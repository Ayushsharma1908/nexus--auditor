package com.nexuscomply.cyber.compliance.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClients;
import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.compliance.GenericRuleEvaluator;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.RuleRequirement;
import com.nexuscomply.cyber.compliance.RuleResultStatus;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleDocument;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.compliance.persistence.ControlDocument;
import com.nexuscomply.cyber.compliance.persistence.ControlRepository;
import com.nexuscomply.cyber.compliance.persistence.FrameworkDocument;
import com.nexuscomply.cyber.compliance.persistence.FrameworkRepository;
import com.nexuscomply.cyber.compliance.rules.cis.CisCiscoIosXeRuleSeeder;
import com.nexuscomply.cyber.compliance.rules.iso.Iso27001RuleSeeder;
import com.nexuscomply.cyber.compliance.rules.nist.NistSp80053RuleSeeder;
import com.nexuscomply.cyber.normalization.NormalizationService;
import com.nexuscomply.cyber.normalization.NormalizationServiceImpl;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.parser.ParserService;
import com.nexuscomply.cyber.parser.ParserServiceImpl;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class NistAndIsoCrossFrameworkRuleSeederIntegrationTest {

    private static MongoServer mongoServer;
    private static MongoTemplate mongoTemplate;

    private FrameworkRepository frameworkRepository;
    private ControlRepository controlRepository;
    private ComplianceRuleRepository ruleRepository;
    private NormalizedConfigurationRepository normalizedConfigRepository;

    private CisCiscoIosXeRuleSeeder cisSeeder;
    private NistSp80053RuleSeeder nistSeeder;
    private Iso27001RuleSeeder isoSeeder;

    private NormalizationService normalizationService;
    private GenericRuleEvaluator evaluator;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startInMemoryMongo() {
        mongoServer = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = mongoServer.bind();
        String connectionString = "mongodb://" + serverAddress.getHostString() + ":" + serverAddress.getPort() + "/testdb";
        SimpleMongoClientDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(MongoClients.create(connectionString), "testdb");
        mongoTemplate = new MongoTemplate(factory);
    }

    @AfterAll
    static void stopInMemoryMongo() {
        if (mongoServer != null) {
            mongoServer.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(FrameworkDocument.class);
        mongoTemplate.dropCollection(ControlDocument.class);
        mongoTemplate.dropCollection(ComplianceRuleDocument.class);
        mongoTemplate.dropCollection(NormalizedConfigurationDocument.class);

        MongoRepositoryFactory repositoryFactory = new MongoRepositoryFactory(mongoTemplate);
        frameworkRepository = repositoryFactory.getRepository(FrameworkRepository.class);
        controlRepository = repositoryFactory.getRepository(ControlRepository.class);
        ruleRepository = repositoryFactory.getRepository(ComplianceRuleRepository.class);
        normalizedConfigRepository = repositoryFactory.getRepository(NormalizedConfigurationRepository.class);

        cisSeeder = new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        nistSeeder = new NistSp80053RuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        isoSeeder = new Iso27001RuleSeeder(frameworkRepository, controlRepository, ruleRepository);

        ParserService parserService = new ParserServiceImpl(List.of(new CiscoIosParser()));
        normalizationService = new NormalizationServiceImpl(parserService, normalizedConfigRepository);

        evaluator = new GenericRuleEvaluator();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Idempotency: Running NIST and ISO seeders twice produces exactly 1 Framework, 7 Controls, and 7 Rules each")
    void testIdempotencyForNistAndIsoSeeders() {
        // Run 1: NIST
        NistSp80053RuleSeeder.SeedResult nistRes1 = nistSeeder.seed();
        assertThat(nistRes1.getFramework()).isNotNull();
        assertThat(nistRes1.getControls()).hasSize(7);
        assertThat(nistRes1.getRules()).hasSize(7);

        // Run 2: NIST (idempotent)
        NistSp80053RuleSeeder.SeedResult nistRes2 = nistSeeder.seed();
        assertThat(nistRes2.getFramework().getId()).isEqualTo(nistRes1.getFramework().getId());

        // Run 1: ISO
        Iso27001RuleSeeder.SeedResult isoRes1 = isoSeeder.seed();
        assertThat(isoRes1.getFramework()).isNotNull();
        assertThat(isoRes1.getControls()).hasSize(6);
        assertThat(isoRes1.getRules()).hasSize(7);

        // Run 2: ISO (idempotent)
        Iso27001RuleSeeder.SeedResult isoRes2 = isoSeeder.seed();
        assertThat(isoRes2.getFramework().getId()).isEqualTo(isoRes1.getFramework().getId());

        // Assert counts in MongoDB: exactly 2 frameworks (NIST, ISO), 13 controls (7 NIST + 6 ISO), 14 rules (7 NIST + 7 ISO)
        assertThat(frameworkRepository.count()).isEqualTo(2);
        assertThat(controlRepository.count()).isEqualTo(13);
        assertThat(ruleRepository.count()).isEqualTo(14);

        // Verify NIST Framework
        FrameworkDocument nistFw = frameworkRepository.findByCode(NistSp80053RuleSeeder.FRAMEWORK_CODE).orElseThrow();
        assertThat(nistFw.getName()).isEqualTo("NIST SP 800-53 Revision 5");
        assertThat(nistFw.getVersion()).isEqualTo("Rev 5");
        assertThat(nistFw.getMetadata()).containsEntry("severitySource", "internal, not NIST-assigned");

        // Verify ISO Framework and ResultLabelMap metadata
        FrameworkDocument isoFw = frameworkRepository.findByCode(Iso27001RuleSeeder.FRAMEWORK_CODE).orElseThrow();
        assertThat(isoFw.getName()).isEqualTo("ISO/IEC 27001:2013");
        assertThat(isoFw.getVersion()).isEqualTo("2013");
        assertThat(isoFw.getMetadata()).containsEntry("severitySource", "internal, not ISO-assigned");
        assertThat(isoFw.getMetadata()).containsKey("resultLabelMap");

        @SuppressWarnings("unchecked")
        Map<String, String> labelMap = (Map<String, String>) isoFw.getMetadata().get("resultLabelMap");
        assertThat(labelMap).containsEntry("PASS", "Technical evidence available");
        assertThat(labelMap).containsEntry("FAIL", "Technical evidence missing");
        assertThat(labelMap).containsEntry("UNKNOWN", "Partial/contextual");
        assertThat(labelMap).containsEntry("NOT_APPLICABLE", "Not assessable");

        // Verify that ISO-A.13.1.1-TELNET and ISO-A.13.1.1-SNMP reference the SAME Control document _id
        ControlDocument a1311Ctrl = controlRepository.findByFrameworkIdAndControlId(isoFw.getId(), "A.13.1.1").orElseThrow();
        ComplianceRuleDocument telnetRule = ruleRepository.findByRuleCode("ISO-A.13.1.1-TELNET").orElseThrow();
        ComplianceRuleDocument snmpRule = ruleRepository.findByRuleCode("ISO-A.13.1.1-SNMP").orElseThrow();

        assertThat(telnetRule.getControlId()).isEqualTo(a1311Ctrl.getId());
        assertThat(snmpRule.getControlId()).isEqualTo(a1311Ctrl.getId());
        assertThat(telnetRule.getControlId()).isEqualTo(snmpRule.getControlId());

        // Verify NIST-IA-2 baselines
        ComplianceRuleDocument nistIa2Rule = ruleRepository.findByRuleCode("NIST-IA-2").orElseThrow();
        ControlDocument nistIa2Ctrl = controlRepository.findByFrameworkIdAndControlId(nistFw.getId(), "IA-2").orElseThrow();
        assertThat(nistIa2Rule.getBaselines()).containsExactly("LOW", "MODERATE", "HIGH");
        assertThat(nistIa2Ctrl.getBaselines()).containsExactly("LOW", "MODERATE", "HIGH");

        // Print raw MongoDB BSON documents for user confirmation
        org.bson.Document rawNistIa2Rule = mongoTemplate.getCollection("compliance_rules").find(new org.bson.Document("ruleCode", "NIST-IA-2")).first();
        org.bson.Document rawNistIa2Ctrl = mongoTemplate.getCollection("controls").find(new org.bson.Document("controlId", "IA-2")).first();
        org.bson.Document rawIsoFw = mongoTemplate.getCollection("frameworks").find(new org.bson.Document("code", Iso27001RuleSeeder.FRAMEWORK_CODE)).first();
        org.bson.Document rawTelnetRule = mongoTemplate.getCollection("compliance_rules").find(new org.bson.Document("ruleCode", "ISO-A.13.1.1-TELNET")).first();
        org.bson.Document rawSnmpRule = mongoTemplate.getCollection("compliance_rules").find(new org.bson.Document("ruleCode", "ISO-A.13.1.1-SNMP")).first();

        System.out.println("=== RAW PERSISTED NIST-IA-2 COMPLIANCE_RULE DOCUMENT ===");
        System.out.println(rawNistIa2Rule != null ? rawNistIa2Rule.toJson() : "null");
        System.out.println("=== RAW PERSISTED NIST IA-2 CONTROL DOCUMENT ===");
        System.out.println(rawNistIa2Ctrl != null ? rawNistIa2Ctrl.toJson() : "null");
        System.out.println("=== RAW PERSISTED ISO FRAMEWORK DOCUMENT ===");
        System.out.println(rawIsoFw != null ? rawIsoFw.toJson() : "null");
        System.out.println("=== RAW PERSISTED ISO-A.13.1.1-TELNET RULE ===");
        System.out.println(rawTelnetRule != null ? rawTelnetRule.toJson() : "null");
        System.out.println("=== RAW PERSISTED ISO-A.13.1.1-SNMP RULE ===");
        System.out.println(rawSnmpRule != null ? rawSnmpRule.toJson() : "null");

        // Verify no STIG framework, control, or rule exists
        List<FrameworkDocument> allFws = frameworkRepository.findAll();
        assertThat(allFws.stream().noneMatch(f -> f.getCode().contains("STIG"))).isTrue();

        List<ComplianceRuleDocument> allRules = ruleRepository.findAll();
        assertThat(allRules.stream().noneMatch(r -> r.getRuleCode().contains("STIG"))).isTrue();
    }

    @Test
    @DisplayName("Cross-Framework Expression Equivalence: NIST and ISO rules reuse exact same RuleRequirements as CIS")
    void testCrossFrameworkExpressionEquivalence() {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        // 1. AAA Equivalence: CIS 1.1.1 vs NIST IA-2 vs ISO A.9.4.2
        ComplianceRuleDocument cisAaa = ruleRepository.findByRuleCode("CIS-1.1.1").orElseThrow();
        ComplianceRuleDocument nistAaa = ruleRepository.findByRuleCode("NIST-IA-2").orElseThrow();
        ComplianceRuleDocument isoAaa = ruleRepository.findByRuleCode("ISO-A.9.4.2").orElseThrow();

        assertThat(nistAaa.getExpression().getCanonicalField()).isEqualTo(cisAaa.getExpression().getCanonicalField()).isEqualTo("authentication.aaa");
        assertThat(nistAaa.getExpression().getOperator()).isEqualTo(cisAaa.getExpression().getOperator()).isEqualTo("EQUALS");
        assertThat(nistAaa.getExpression().getExpectedValue()).isEqualTo(cisAaa.getExpression().getExpectedValue()).isEqualTo(true);

        assertThat(isoAaa.getExpression().getCanonicalField()).isEqualTo(cisAaa.getExpression().getCanonicalField()).isEqualTo("authentication.aaa");
        assertThat(isoAaa.getExpression().getOperator()).isEqualTo(cisAaa.getExpression().getOperator()).isEqualTo("EQUALS");
        assertThat(isoAaa.getExpression().getExpectedValue()).isEqualTo(cisAaa.getExpression().getExpectedValue()).isEqualTo(true);

        // 2. SSH Version 2 Equivalence: CIS 2.1.1.2 vs NIST SC-8 vs ISO A.10.1.1
        ComplianceRuleDocument cisSsh = ruleRepository.findByRuleCode("CIS-2.1.1.2").orElseThrow();
        ComplianceRuleDocument nistSsh = ruleRepository.findByRuleCode("NIST-SC-8").orElseThrow();
        ComplianceRuleDocument isoSsh = ruleRepository.findByRuleCode("ISO-A.10.1.1").orElseThrow();

        assertThat(nistSsh.getExpression().getCanonicalField()).isEqualTo(cisSsh.getExpression().getCanonicalField()).isEqualTo("security.ssh.version");
        assertThat(nistSsh.getExpression().getOperator()).isEqualTo(cisSsh.getExpression().getOperator()).isEqualTo("EQUALS");
        assertThat(nistSsh.getExpression().getExpectedValue()).isEqualTo(cisSsh.getExpression().getExpectedValue()).isEqualTo(2);

        assertThat(isoSsh.getExpression().getCanonicalField()).isEqualTo(cisSsh.getExpression().getCanonicalField()).isEqualTo("security.ssh.version");
        assertThat(isoSsh.getExpression().getOperator()).isEqualTo(cisSsh.getExpression().getOperator()).isEqualTo("EQUALS");
        assertThat(isoSsh.getExpression().getExpectedValue()).isEqualTo(cisSsh.getExpression().getExpectedValue()).isEqualTo(2);

        // 3. Telnet Disabled Equivalence: CIS 1.2.2 vs NIST AC-17 vs ISO A.13.1.1-TELNET
        ComplianceRuleDocument cisTelnet = ruleRepository.findByRuleCode("CIS-1.2.2").orElseThrow();
        ComplianceRuleDocument nistTelnet = ruleRepository.findByRuleCode("NIST-AC-17").orElseThrow();
        ComplianceRuleDocument isoTelnet = ruleRepository.findByRuleCode("ISO-A.13.1.1-TELNET").orElseThrow();

        assertThat(nistTelnet.getExpression().getCanonicalField()).isEqualTo(cisTelnet.getExpression().getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(nistTelnet.getExpression().getExpectedValue()).isEqualTo(cisTelnet.getExpression().getExpectedValue()).isEqualTo(false);
        assertThat(isoTelnet.getExpression().getCanonicalField()).isEqualTo(cisTelnet.getExpression().getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(isoTelnet.getExpression().getExpectedValue()).isEqualTo(cisTelnet.getExpression().getExpectedValue()).isEqualTo(false);
    }

    @Test
    @DisplayName("Live Evaluation: Real Cisco acceptance configuration evaluates all 14 NIST and ISO rules to PASS")
    void testRealAcceptanceConfigEvaluatesAll14NistAndIsoRules() throws Exception {
        NistSp80053RuleSeeder.SeedResult nistResult = nistSeeder.seed();
        Iso27001RuleSeeder.SeedResult isoResult = isoSeeder.seed();

        List<ComplianceRule> nistDomainRules = nistResult.toDomainRules();
        List<ComplianceRule> isoDomainRules = isoResult.toDomainRules();

        assertThat(nistDomainRules).hasSize(7);
        assertThat(isoDomainRules).hasSize(7);

        // Real Cisco acceptance configuration (identical to prior verified tasks)
        String realConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-01",
                "!",
                "ip ssh version 2",
                "no ip http server",
                "ip http secure-server",
                "line vty 0 4",
                " transport input ssh",
                "snmp-server community private RO version 3",
                "aaa new-model",
                "logging on",
                "logging host 10.10.10.5",
                "logging buffered 16384",
                "ntp server 10.0.0.1",
                "!",
                "interface GigabitEthernet0/0/0",
                " ip address 10.0.0.1 255.255.255.0",
                "!"
        );

        NormalizedConfigurationDocument normalizedDoc = normalizationService.normalizeAndPersist(
                realConfig, "dev-rtr-01", "cfg-test-001", "ver-001", "Cisco", "IOS-XE", "17.6"
        );
        CanonicalSecurityModel canonical = normalizedDoc.getCanonical();
        assertThat(canonical).isNotNull();

        System.out.println("=== TASK 1.3: EVALUATION RESULTS FOR 7 NIST SP 800-53 REV 5 RULES ===");
        for (ComplianceRule rule : nistDomainRules) {
            RuleEvaluationResult result = evaluator.evaluate(canonical, rule);
            String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            System.out.println("--- RULE: " + rule.getRuleCode() + " (" + rule.getName() + ") ---");
            System.out.println(resultJson);
            assertThat(result.getStatus()).isEqualTo(RuleResultStatus.PASS);
        }

        System.out.println("=== TASK 1.3: EVALUATION RESULTS FOR 7 ISO/IEC 27001:2013 RULES ===");
        for (ComplianceRule rule : isoDomainRules) {
            RuleEvaluationResult result = evaluator.evaluate(canonical, rule);
            String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            System.out.println("--- RULE: " + rule.getRuleCode() + " (" + rule.getName() + ") ---");
            System.out.println(resultJson);
            assertThat(result.getStatus()).isEqualTo(RuleResultStatus.PASS);
        }
    }

    @Test
    @DisplayName("Task 2.15: Multi-vendor rule applicability expanded across all 4 supported vendors for NIST and ISO rules")
    void testMultiVendorRuleApplicabilityForNistAndIso() {
        NistSp80053RuleSeeder.SeedResult nistResult = nistSeeder.seed();
        Iso27001RuleSeeder.SeedResult isoResult = isoSeeder.seed();

        List<ComplianceRuleDocument> nistRules = nistResult.getRules();
        List<ComplianceRuleDocument> isoRules = isoResult.getRules();

        assertThat(nistRules).hasSize(7);
        assertThat(isoRules).hasSize(7);

        List<String> expectedVendors = List.of("Cisco", "Juniper", "Fortinet", "Palo Alto");
        List<String> expectedPlatforms = List.of("IOS", "IOS-XE", "JUNOS", "FortiOS", "PAN-OS");

        for (ComplianceRuleDocument rule : nistRules) {
            assertThat(rule.getApplicableVendors())
                    .as("NIST rule %s applicableVendors", rule.getRuleCode())
                    .containsExactlyElementsOf(expectedVendors);
            assertThat(rule.getApplicablePlatforms())
                    .as("NIST rule %s applicablePlatforms", rule.getRuleCode())
                    .containsExactlyElementsOf(expectedPlatforms);
            assertThat(rule.getApplicableOsVersions())
                    .as("NIST rule %s applicableOsVersions", rule.getRuleCode())
                    .isEmpty();
        }

        for (ComplianceRuleDocument rule : isoRules) {
            assertThat(rule.getApplicableVendors())
                    .as("ISO rule %s applicableVendors", rule.getRuleCode())
                    .containsExactlyElementsOf(expectedVendors);
            assertThat(rule.getApplicablePlatforms())
                    .as("ISO rule %s applicablePlatforms", rule.getRuleCode())
                    .containsExactlyElementsOf(expectedPlatforms);
            assertThat(rule.getApplicableOsVersions())
                    .as("ISO rule %s applicableOsVersions", rule.getRuleCode())
                    .isEmpty();
        }
    }
}
