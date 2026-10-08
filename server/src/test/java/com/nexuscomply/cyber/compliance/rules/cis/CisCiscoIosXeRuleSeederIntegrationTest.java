package com.nexuscomply.cyber.compliance.rules.cis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClients;
import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.compliance.GenericRuleEvaluator;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.RuleResultStatus;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleDocument;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.compliance.persistence.ControlDocument;
import com.nexuscomply.cyber.compliance.persistence.ControlRepository;
import com.nexuscomply.cyber.compliance.persistence.FrameworkDocument;
import com.nexuscomply.cyber.compliance.persistence.FrameworkRepository;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CisCiscoIosXeRuleSeederIntegrationTest {

    private static MongoServer mongoServer;
    private static MongoTemplate mongoTemplate;

    private FrameworkRepository frameworkRepository;
    private ControlRepository controlRepository;
    private ComplianceRuleRepository ruleRepository;
    private NormalizedConfigurationRepository normalizedConfigRepository;

    private CisCiscoIosXeRuleSeeder seeder;
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

        seeder = new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository);

        ParserService parserService = new ParserServiceImpl(List.of(new CiscoIosParser()));
        normalizationService = new NormalizationServiceImpl(parserService, normalizedConfigRepository);

        evaluator = new GenericRuleEvaluator();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Idempotency: Running seeder twice produces exactly 1 Framework, 7 Controls, and 7 Rules")
    void testIdempotentSeeding() throws Exception {
        // Run 1
        CisCiscoIosXeRuleSeeder.SeedResult result1 = seeder.seed();
        assertThat(result1.getFramework()).isNotNull();
        assertThat(result1.getControls()).hasSize(7);
        assertThat(result1.getRules()).hasSize(7);

        assertThat(frameworkRepository.count()).isEqualTo(1);
        assertThat(controlRepository.count()).isEqualTo(7);
        assertThat(ruleRepository.count()).isEqualTo(7);

        // Run 2 (verify idempotence)
        CisCiscoIosXeRuleSeeder.SeedResult result2 = seeder.seed();
        assertThat(result2.getFramework().getId()).isEqualTo(result1.getFramework().getId());
        assertThat(result2.getControls()).hasSize(7);
        assertThat(result2.getRules()).hasSize(7);

        // Counts must remain identical -- no duplicate documents
        assertThat(frameworkRepository.count()).isEqualTo(1);
        assertThat(controlRepository.count()).isEqualTo(7);
        assertThat(ruleRepository.count()).isEqualTo(7);

        // Verify Framework fields
        FrameworkDocument fw = frameworkRepository.findByCode(CisCiscoIosXeRuleSeeder.FRAMEWORK_CODE).orElseThrow();
        assertThat(fw.getName()).isEqualTo("CIS Cisco IOS XE 17.x Benchmark");
        assertThat(fw.getVersion()).isEqualTo("2.2.1");
        assertThat(fw.getCategory()).isEqualTo("NETWORK_DEVICE");
        assertThat(fw.getStatus()).isEqualTo("ACTIVE");
        assertThat(fw.getControlCount()).isEqualTo(7);
        assertThat(fw.getMetadata()).containsEntry("severitySource", "internal, not CIS-assigned");

        // Verify Control IDs match the real CIS numbers exactly
        List<ControlDocument> allControls = controlRepository.findAll();
        Set<String> controlIds = allControls.stream().map(ControlDocument::getControlId).collect(Collectors.toSet());
        assertThat(controlIds).containsExactlyInAnyOrder("1.1.1", "1.2.2", "2.1.1.2", "1.5.9", "2.2.1", "2.2.2", "2.3.2");

        // Verify Rule codes match the real CIS numbers exactly
        List<ComplianceRuleDocument> allRules = ruleRepository.findAll();
        Set<String> ruleCodes = allRules.stream().map(ComplianceRuleDocument::getRuleCode).collect(Collectors.toSet());
        assertThat(ruleCodes).containsExactlyInAnyOrder(
                "CIS-1.1.1", "CIS-1.2.2", "CIS-2.1.1.2", "CIS-1.5.9", "CIS-2.2.1", "CIS-2.2.2", "CIS-2.3.2"
        );

        // Verify NO HTTPS rule is present in the rule collection
        boolean hasHttpsRule = allRules.stream().anyMatch(r ->
                r.getRuleCode().toUpperCase().contains("HTTP") ||
                r.getName().toUpperCase().contains("HTTPS") ||
                (r.getExpression() != null && "security.https.enabled".equals(r.getExpression().getCanonicalField()))
        );
        assertThat(hasHttpsRule).isFalse();

        // Verify each rule's description marks severity as internal, not CIS-assigned
        for (ComplianceRuleDocument rule : allRules) {
            assertThat(rule.getDescription()).contains("Severity source: internal, not CIS-assigned");
            assertThat(rule.getFrameworkIds()).contains(fw.getId());
            assertThat(rule.getApplicableVendors()).contains("Cisco");
            assertThat(rule.getApplicablePlatforms()).contains("IOS-XE");
        }

        // Verify schema1.md section 7 & 8 JSON structure roundtrip
        ControlDocument sampleCtrl = allControls.get(0);
        String ctrlJson = objectMapper.writeValueAsString(sampleCtrl);
        @SuppressWarnings("unchecked")
        Map<String, Object> ctrlMap = objectMapper.readValue(ctrlJson, Map.class);
        assertThat(ctrlMap).containsKeys("id", "frameworkId", "controlId", "title", "description", "category", "severity", "status", "requirements", "createdAt", "updatedAt");

        ComplianceRuleDocument sampleRule = allRules.get(0);
        String ruleJson = objectMapper.writeValueAsString(sampleRule);
        @SuppressWarnings("unchecked")
        Map<String, Object> ruleMap = objectMapper.readValue(ruleJson, Map.class);
        assertThat(ruleMap).containsKeys("id", "ruleCode", "controlId", "name", "description", "expression", "severity", "frameworkIds", "applicableVendors", "applicablePlatforms", "applicableOsVersions", "status", "version", "createdAt", "updatedAt");

        // Print raw MongoDB documents for CIS 1.1.1
        ControlDocument cis111Ctrl = controlRepository.findByFrameworkIdAndControlId(fw.getId(), "1.1.1").orElseThrow();
        ComplianceRuleDocument cis111Rule = ruleRepository.findByRuleCode("CIS-1.1.1").orElseThrow();

        org.bson.Document rawBsonCtrl = mongoTemplate.getCollection("controls").find(new org.bson.Document("controlId", "1.1.1")).first();
        org.bson.Document rawBsonRule = mongoTemplate.getCollection("compliance_rules").find(new org.bson.Document("ruleCode", "CIS-1.1.1")).first();

        System.out.println("=== RAW PERSISTED MONGODB DOCUMENT (BSON COLLECTION): CONTROL CIS 1.1.1 ===");
        System.out.println(rawBsonCtrl != null ? rawBsonCtrl.toJson() : "null");
        System.out.println("=== RAW PERSISTED MONGODB DOCUMENT (BSON COLLECTION): COMPLIANCE RULE CIS-1.1.1 ===");
        System.out.println(rawBsonRule != null ? rawBsonRule.toJson() : "null");

        org.bson.Document rawBsonRuleTelnet = mongoTemplate.getCollection("compliance_rules").find(new org.bson.Document("ruleCode", "CIS-1.2.2")).first();
        System.out.println("=== RAW PERSISTED MONGODB DOCUMENT (BSON COLLECTION): COMPLIANCE RULE CIS-1.2.2 (TELNET) ===");
        System.out.println(rawBsonRuleTelnet != null ? rawBsonRuleTelnet.toJson() : "null");

        org.bson.Document rawBsonRuleSsh = mongoTemplate.getCollection("compliance_rules").find(new org.bson.Document("ruleCode", "CIS-2.1.1.2")).first();
        System.out.println("=== RAW PERSISTED MONGODB DOCUMENT (BSON COLLECTION): COMPLIANCE RULE CIS-2.1.1.2 (SSH VERSION) ===");
        System.out.println(rawBsonRuleSsh != null ? rawBsonRuleSsh.toJson() : "null");
    }

    @Test
    @DisplayName("Real acceptance config from CiscoIosParserTest evaluates all 7 rules to PASS with actual values")
    void testRealAcceptanceConfigEvaluatesAllSevenRules() throws Exception {
        // 1. Seed the rules
        CisCiscoIosXeRuleSeeder.SeedResult seedResult = seeder.seed();
        List<ComplianceRule> domainRules = seedResult.toDomainRules();
        assertThat(domainRules).hasSize(7);

        // 2. Real Cisco acceptance configuration (identical to CiscoIosParserTest)
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

        // 3. Pipeline: CiscoIosParser -> NormalizationService -> MongoDB -> CanonicalSecurityModel
        NormalizedConfigurationDocument normalizedDoc = normalizationService.normalizeAndPersist(
                realConfig, "dev-rtr-01", "cfg-cis-001", "ver-001", "Cisco", "IOS-XE", "17.6"
        );
        assertThat(normalizedDoc).isNotNull();
        CanonicalSecurityModel canonical = normalizedDoc.getCanonical();
        assertThat(canonical).isNotNull();

        // 4. Evaluate each seeded rule against the canonical model
        System.out.println("=== TASK 1.2: EVALUATION RESULTS FOR REAL CISCO ACCEPTANCE CONFIG ===");
        for (ComplianceRule rule : domainRules) {
            RuleEvaluationResult result = evaluator.evaluate(canonical, rule);
            String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            System.out.println("--- RULE: " + rule.getRuleCode() + " (" + rule.getName() + ") ---");
            System.out.println(resultJson);

            // Assert deterministic PASS on every rule
            assertThat(result.getStatus())
                    .as("Rule %s should PASS on real acceptance config", rule.getRuleCode())
                    .isEqualTo(RuleResultStatus.PASS);
        }

        // Specific field-level assertions
        // RULE 1: CIS-1.1.1 - AAA
        ComplianceRule rule1 = findRule(domainRules, "CIS-1.1.1");
        RuleEvaluationResult res1 = evaluator.evaluate(canonical, rule1);
        assertThat(res1.getActual()).isEqualTo(true);
        assertThat(res1.getExpected()).isEqualTo(true);
        assertThat(res1.getEvidenceSourceField()).isEqualTo("authentication.aaa");

        // RULE 2: CIS-1.2.2 - Telnet disabled
        ComplianceRule rule2 = findRule(domainRules, "CIS-1.2.2");
        RuleEvaluationResult res2 = evaluator.evaluate(canonical, rule2);
        assertThat(res2.getActual()).isEqualTo(false);
        assertThat(res2.getExpected()).isEqualTo(false);
        assertThat(res2.getEvidenceSourceField()).isEqualTo("security.telnet.enabled");

        // RULE 3: CIS-2.1.1.2 - SSH version 2
        ComplianceRule rule3 = findRule(domainRules, "CIS-2.1.1.2");
        RuleEvaluationResult res3 = evaluator.evaluate(canonical, rule3);
        assertThat(res3.getActual()).isEqualTo(2);
        assertThat(res3.getExpected()).isEqualTo(2);
        assertThat(res3.getEvidenceSourceField()).isEqualTo("security.ssh.version");

        // RULE 4: CIS-1.5.9 - SNMPv3 version
        ComplianceRule rule4 = findRule(domainRules, "CIS-1.5.9");
        RuleEvaluationResult res4 = evaluator.evaluate(canonical, rule4);
        assertThat(res4.getActual()).isEqualTo("3");
        assertThat(res4.getExpected()).isEqualTo("3");
        assertThat(res4.getEvidenceSourceField()).isEqualTo("security.snmp.version");

        // RULE 5: CIS-2.2.1 - Syslog host
        ComplianceRule rule5 = findRule(domainRules, "CIS-2.2.1");
        RuleEvaluationResult res5 = evaluator.evaluate(canonical, rule5);
        assertThat(res5.getActual()).isEqualTo(true);
        assertThat(res5.getExpected()).isEqualTo(true);
        assertThat(res5.getEvidenceSourceField()).isEqualTo("logging.syslog");

        // RULE 6: CIS-2.2.2 - Local logging
        ComplianceRule rule6 = findRule(domainRules, "CIS-2.2.2");
        RuleEvaluationResult res6 = evaluator.evaluate(canonical, rule6);
        assertThat(res6.getActual()).isEqualTo(true);
        assertThat(res6.getExpected()).isEqualTo(true);
        assertThat(res6.getEvidenceSourceField()).isEqualTo("logging.localLogging");

        // RULE 7: CIS-2.3.2 - NTP
        ComplianceRule rule7 = findRule(domainRules, "CIS-2.3.2");
        RuleEvaluationResult res7 = evaluator.evaluate(canonical, rule7);
        assertThat(res7.getActual()).isEqualTo(true);
        assertThat(res7.getExpected()).isEqualTo(true);
        assertThat(res7.getEvidenceSourceField()).isEqualTo("ntp.configured");
    }

    @Test
    @DisplayName("Non-compliant Cisco config produces FAIL results on violated rules")
    void testNonCompliantCiscoConfigProducesFailures() {
        CisCiscoIosXeRuleSeeder.SeedResult seedResult = seeder.seed();
        List<ComplianceRule> domainRules = seedResult.toDomainRules();

        // Non-compliant config: Telnet allowed, SSH v1, SNMP v2c, no AAA, no logging, no NTP
        String nonCompliantConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-INSECURE",
                "!",
                "ip ssh version 1",
                "line vty 0 4",
                " transport input telnet",
                "snmp-server community public RO",
                "no aaa new-model",
                "no logging on",
                "!"
        );

        NormalizedConfigurationDocument doc = normalizationService.normalizeAndPersist(
                nonCompliantConfig, "dev-rtr-02", "cfg-insecure-001", "ver-002", "Cisco", "IOS-XE", "17.6"
        );
        CanonicalSecurityModel canonical = doc.getCanonical();

        // Rule 1: AAA not configured / unobserved -> UNKNOWN (per rule 5: unobserved fields must produce UNKNOWN)
        ComplianceRule rule1 = findRule(domainRules, "CIS-1.1.1");
        RuleEvaluationResult res1 = evaluator.evaluate(canonical, rule1);
        assertThat(res1.getStatus()).isEqualTo(RuleResultStatus.UNKNOWN);
        assertThat(res1.getMessage()).contains("was not observed or set");

        // Rule 2: Telnet enabled -> FAIL (actual true, expected false)
        ComplianceRule rule2 = findRule(domainRules, "CIS-1.2.2");
        RuleEvaluationResult res2 = evaluator.evaluate(canonical, rule2);
        assertThat(res2.getStatus()).isEqualTo(RuleResultStatus.FAIL);
        assertThat(res2.getActual()).isEqualTo(true);

        // Rule 3: SSH version 1 -> FAIL (actual 1, expected 2)
        ComplianceRule rule3 = findRule(domainRules, "CIS-2.1.1.2");
        RuleEvaluationResult res3 = evaluator.evaluate(canonical, rule3);
        assertThat(res3.getStatus()).isEqualTo(RuleResultStatus.FAIL);
        assertThat(res3.getActual()).isEqualTo(1);

        // Rule 7: NTP unconfigured -> UNKNOWN (field ntp.configured not present in canonical)
        ComplianceRule rule7 = findRule(domainRules, "CIS-2.3.2");
        RuleEvaluationResult res7 = evaluator.evaluate(canonical, rule7);
        assertThat(res7.getStatus()).isEqualTo(RuleResultStatus.UNKNOWN);
        assertThat(res7.getMessage()).contains("was not observed or set");
    }

    private ComplianceRule findRule(List<ComplianceRule> rules, String ruleCode) {
        return rules.stream()
                .filter(r -> ruleCode.equals(r.getRuleCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Rule with code " + ruleCode + " not found"));
    }
}
