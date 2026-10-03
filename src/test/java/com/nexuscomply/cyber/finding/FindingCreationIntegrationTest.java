package com.nexuscomply.cyber.finding;

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
import com.nexuscomply.cyber.compliance.rules.cis.CisCiscoIosXeRuleSeeder;
import com.nexuscomply.cyber.compliance.rules.nist.NistSp80053RuleSeeder;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.normalization.NormalizationService;
import com.nexuscomply.cyber.normalization.NormalizationServiceImpl;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.parser.ParserService;
import com.nexuscomply.cyber.parser.ParserServiceImpl;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
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

import static org.assertj.core.api.Assertions.assertThat;

class FindingCreationIntegrationTest {

    private static MongoServer mongoServer;
    private static MongoTemplate mongoTemplate;

    private FrameworkRepository frameworkRepository;
    private ControlRepository controlRepository;
    private ComplianceRuleRepository ruleRepository;
    private NormalizedConfigurationRepository normalizedConfigRepository;
    private FindingRepository findingRepository;

    private CisCiscoIosXeRuleSeeder cisSeeder;
    private NistSp80053RuleSeeder nistSeeder;
    private NormalizationService normalizationService;
    private GenericRuleEvaluator evaluator;
    private FindingCreationService findingCreationService;
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
        mongoTemplate.dropCollection(FindingDocument.class);

        MongoRepositoryFactory repositoryFactory = new MongoRepositoryFactory(mongoTemplate);
        frameworkRepository = repositoryFactory.getRepository(FrameworkRepository.class);
        controlRepository = repositoryFactory.getRepository(ControlRepository.class);
        ruleRepository = repositoryFactory.getRepository(ComplianceRuleRepository.class);
        normalizedConfigRepository = repositoryFactory.getRepository(NormalizedConfigurationRepository.class);
        findingRepository = repositoryFactory.getRepository(FindingRepository.class);

        cisSeeder = new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        nistSeeder = new NistSp80053RuleSeeder(frameworkRepository, controlRepository, ruleRepository);

        ParserService parserService = new ParserServiceImpl(List.of(new CiscoIosParser()));
        normalizationService = new NormalizationServiceImpl(parserService, normalizedConfigRepository);
        evaluator = new GenericRuleEvaluator();

        findingCreationService = new FindingCreationServiceImpl(findingRepository, ruleRepository, controlRepository);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Acceptance Criterion: A rule evaluation that returns PASS produces ZERO findings in MongoDB")
    void testPassResultProducesZeroPersistedFindings() {
        cisSeeder.seed();

        // Real Cisco acceptance configuration (telnet disabled: transport input ssh)
        String passConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-PASS",
                "!",
                "line vty 0 4",
                " transport input ssh",
                "!"
        );

        NormalizedConfigurationDocument normalizedDoc = normalizationService.normalizeAndPersist(
                passConfig, "dev-pass-01", "cfg-pass-001", "ver-001", "Cisco", "IOS-XE", "17.6"
        );
        CanonicalSecurityModel canonical = normalizedDoc.getCanonical();

        ComplianceRuleDocument cis122Doc = ruleRepository.findByRuleCode("CIS-1.2.2").orElseThrow();
        ComplianceRule cis122Rule = new ComplianceRule();
        cis122Rule.setId(cis122Doc.getId());
        cis122Rule.setRuleCode(cis122Doc.getRuleCode());
        cis122Rule.setControlId(cis122Doc.getControlId());
        cis122Rule.setName(cis122Doc.getName());
        cis122Rule.setDescription(cis122Doc.getDescription());
        cis122Rule.setRequirement(cis122Doc.getExpression());
        cis122Rule.setSeverity(cis122Doc.getSeverity());
        cis122Rule.setFrameworkIds(cis122Doc.getFrameworkIds());

        RuleEvaluationResult result = evaluator.evaluate(canonical, cis122Rule);
        assertThat(result.getStatus()).isEqualTo(RuleResultStatus.PASS);

        FindingContext context = new FindingContext("audit-001", "dev-pass-01", "cfg-pass-001", "ver-001");
        Optional<Finding> findingOpt = findingCreationService.createFinding(result, cis122Rule, null, context);

        assertThat(findingOpt).isEmpty();
        assertThat(findingRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Acceptance Criterion: A rule evaluation returning FAIL produces exactly ONE persisted Finding with status OPEN")
    void testFailResultProducesExactlyOnePersistedFinding() throws Exception {
        cisSeeder.seed();

        // Deliberate non-compliant configuration where telnet IS enabled (transport input telnet ssh)
        String failConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-FAIL",
                "!",
                "line vty 0 4",
                " transport input telnet ssh",
                "!"
        );

        NormalizedConfigurationDocument normalizedDoc = normalizationService.normalizeAndPersist(
                failConfig, "dev-fail-01", "cfg-fail-001", "ver-001", "Cisco", "IOS-XE", "17.6"
        );
        CanonicalSecurityModel canonical = normalizedDoc.getCanonical();
        assertThat(canonical).isNotNull();

        ComplianceRuleDocument cis122Doc = ruleRepository.findByRuleCode("CIS-1.2.2").orElseThrow();
        ControlDocument cis122Ctrl = controlRepository.findById(cis122Doc.getControlId()).orElseThrow();

        ComplianceRule cis122Rule = new ComplianceRule();
        cis122Rule.setId(cis122Doc.getId());
        cis122Rule.setRuleCode(cis122Doc.getRuleCode());
        cis122Rule.setControlId(cis122Doc.getControlId());
        cis122Rule.setName(cis122Doc.getName());
        cis122Rule.setDescription(cis122Doc.getDescription());
        cis122Rule.setRequirement(cis122Doc.getExpression());
        cis122Rule.setSeverity(cis122Doc.getSeverity());
        cis122Rule.setFrameworkIds(cis122Doc.getFrameworkIds());

        RuleEvaluationResult result = evaluator.evaluate(canonical, cis122Rule);
        assertThat(result.getStatus()).isEqualTo(RuleResultStatus.FAIL);
        assertThat(result.getActual()).isEqualTo(true);
        assertThat(result.getExpected()).isEqualTo(false);

        FindingContext context = new FindingContext("audit-test-999", "dev-fail-01", "cfg-fail-001", "ver-001");
        Optional<Finding> findingOpt = findingCreationService.createFinding(result, context);

        assertThat(findingOpt).isPresent();
        assertThat(findingRepository.count()).isEqualTo(1);

        Finding finding = findingOpt.get();
        assertThat(finding.getStatus()).isEqualTo("OPEN");
        assertThat(finding.getComplianceStatus()).isEqualTo("FAIL");
        assertThat(finding.getSeverity()).isEqualTo("HIGH");
        assertThat(finding.getControlId()).isEqualTo(cis122Ctrl.getId());
        assertThat(finding.getRuleId()).isEqualTo(cis122Doc.getId());
        assertThat(finding.getControlCode()).isEqualTo("CIS-1.2.2");
        assertThat(finding.getTitle()).isEqualTo("Telnet enabled");
        assertThat(finding.getDescription()).isEqualTo("Telnet is enabled for management access.");
        assertThat(finding.getImpact()).contains("Insecure remote management protocol is enabled");
        assertThat(finding.getImpact()).doesNotContain("show running-config").doesNotContain("line vty").doesNotContain("Severity source");
        assertThat(finding.getFrameworkIds()).containsExactlyElementsOf(cis122Doc.getFrameworkIds());
        assertThat(finding.getAuditId()).isEqualTo("audit-test-999");
        assertThat(finding.getDeviceId()).isEqualTo("dev-fail-01");
        assertThat(finding.getConfigurationId()).isEqualTo("cfg-fail-001");
        assertThat(finding.getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(finding.getExpected()).isEqualTo(false);
        assertThat(finding.getActual()).isEqualTo(true);

        // Fetch raw BSON document from MongoDB
        Document rawFinding = mongoTemplate.getCollection("findings").find(new Document("ruleId", cis122Doc.getId())).first();
        assertThat(rawFinding).isNotNull();

        System.out.println("=== RAW PERSISTED MONGODB FINDING DOCUMENT ===");
        System.out.println(rawFinding.toJson());

        // Field-by-field verification against schema1.md section 10
        assertThat(rawFinding.getString("_id")).isNotEmpty();
        assertThat(rawFinding.getString("auditId")).isEqualTo("audit-test-999");
        assertThat(rawFinding.getString("deviceId")).isEqualTo("dev-fail-01");
        assertThat(rawFinding.getString("configurationId")).isEqualTo("cfg-fail-001");
        assertThat(rawFinding.getString("controlId")).isEqualTo(cis122Ctrl.getId());
        assertThat(rawFinding.getString("ruleId")).isEqualTo(cis122Doc.getId());
        assertThat(rawFinding.getString("controlCode")).isEqualTo("CIS-1.2.2");
        assertThat(rawFinding.getString("title")).isEqualTo("Telnet enabled");
        assertThat(rawFinding.getString("description")).isEqualTo("Telnet is enabled for management access.");
        assertThat(rawFinding.getString("impact")).contains("Insecure remote management protocol is enabled");
        assertThat(rawFinding.getString("impact")).doesNotContain("show running-config").doesNotContain("line vty").doesNotContain("Severity source");
        assertThat(rawFinding.getString("status")).isEqualTo("OPEN");
        assertThat(rawFinding.getString("complianceStatus")).isEqualTo("FAIL");
        assertThat(rawFinding.getString("severity")).isEqualTo("HIGH");
        assertThat(rawFinding.getList("frameworkIds", String.class)).containsExactlyElementsOf(cis122Doc.getFrameworkIds());
        assertThat(rawFinding.getString("canonicalField")).isEqualTo("security.telnet.enabled");
        assertThat(rawFinding.get("expected")).isEqualTo(false);
        assertThat(rawFinding.get("actual")).isEqualTo(true);
        assertThat(rawFinding.getList("evidenceIds", String.class)).isEmpty();
        assertThat(rawFinding.getBoolean("remediationAvailable")).isTrue();
        assertThat(rawFinding.getDate("createdAt")).isNotNull();
        assertThat(rawFinding.getDate("updatedAt")).isNotNull();

        // Confirm NO risk score field exists in this task
        assertThat(rawFinding.containsKey("risk")).isFalse();
        assertThat(rawFinding.containsKey("score")).isFalse();
    }

    @Test
    @DisplayName("Absolute Rule 5: Cross-framework failures on same canonical field produce separate Finding documents, never merged")
    void testCrossFrameworkFailuresProduceSeparateFindingsWithoutMerging() {
        cisSeeder.seed();
        nistSeeder.seed();

        // Configuration with Telnet enabled -> causes both CIS-1.2.2 and NIST-AC-17 to FAIL
        String failConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-CROSS-FAIL",
                "!",
                "line vty 0 4",
                " transport input telnet ssh",
                "!"
        );

        NormalizedConfigurationDocument normalizedDoc = normalizationService.normalizeAndPersist(
                failConfig, "dev-cross-01", "cfg-cross-001", "ver-001", "Cisco", "IOS-XE", "17.6"
        );
        CanonicalSecurityModel canonical = normalizedDoc.getCanonical();

        // 1. CIS-1.2.2
        ComplianceRuleDocument cisRuleDoc = ruleRepository.findByRuleCode("CIS-1.2.2").orElseThrow();
        ComplianceRule cisRule = new ComplianceRule();
        cisRule.setId(cisRuleDoc.getId());
        cisRule.setRuleCode(cisRuleDoc.getRuleCode());
        cisRule.setControlId(cisRuleDoc.getControlId());
        cisRule.setName(cisRuleDoc.getName());
        cisRule.setDescription(cisRuleDoc.getDescription());
        cisRule.setRequirement(cisRuleDoc.getExpression());
        cisRule.setSeverity(cisRuleDoc.getSeverity());
        cisRule.setFrameworkIds(cisRuleDoc.getFrameworkIds());

        RuleEvaluationResult cisResult = evaluator.evaluate(canonical, cisRule);
        assertThat(cisResult.getStatus()).isEqualTo(RuleResultStatus.FAIL);

        // 2. NIST-AC-17
        ComplianceRuleDocument nistRuleDoc = ruleRepository.findByRuleCode("NIST-AC-17").orElseThrow();
        ComplianceRule nistRule = new ComplianceRule();
        nistRule.setId(nistRuleDoc.getId());
        nistRule.setRuleCode(nistRuleDoc.getRuleCode());
        nistRule.setControlId(nistRuleDoc.getControlId());
        nistRule.setName(nistRuleDoc.getName());
        nistRule.setDescription(nistRuleDoc.getDescription());
        nistRule.setRequirement(nistRuleDoc.getExpression());
        nistRule.setSeverity(nistRuleDoc.getSeverity());
        nistRule.setFrameworkIds(nistRuleDoc.getFrameworkIds());

        RuleEvaluationResult nistResult = evaluator.evaluate(canonical, nistRule);
        assertThat(nistResult.getStatus()).isEqualTo(RuleResultStatus.FAIL);

        FindingContext context = new FindingContext("audit-cross-101", "dev-cross-01", "cfg-cross-001", "ver-001");

        // Create findings for both
        Optional<Finding> cisFindingOpt = findingCreationService.createFinding(cisResult, context);
        Optional<Finding> nistFindingOpt = findingCreationService.createFinding(nistResult, context);

        assertThat(cisFindingOpt).isPresent();
        assertThat(nistFindingOpt).isPresent();

        // Exactly 2 findings in database -- NOT deduplicated or merged
        assertThat(findingRepository.count()).isEqualTo(2);

        Finding cisFinding = cisFindingOpt.get();
        Finding nistFinding = nistFindingOpt.get();

        assertThat(cisFinding.getId()).isNotEqualTo(nistFinding.getId());
        assertThat(cisFinding.getRuleId()).isEqualTo(cisRuleDoc.getId());
        assertThat(nistFinding.getRuleId()).isEqualTo(nistRuleDoc.getId());

        assertThat(cisFinding.getFrameworkIds()).containsExactlyElementsOf(cisRuleDoc.getFrameworkIds());
        assertThat(nistFinding.getFrameworkIds()).containsExactlyElementsOf(nistRuleDoc.getFrameworkIds());
        assertThat(cisFinding.getFrameworkIds()).isNotEqualTo(nistFinding.getFrameworkIds());

        System.out.println("=== CROSS-FRAMEWORK FINDING 1 (CIS): " + cisFinding.getTitle() + " ===");
        System.out.println("Finding ID: " + cisFinding.getId() + " | RuleId: " + cisFinding.getRuleId() + " | FrameworkIds: " + cisFinding.getFrameworkIds());
        System.out.println("=== CROSS-FRAMEWORK FINDING 2 (NIST): " + nistFinding.getTitle() + " ===");
        System.out.println("Finding ID: " + nistFinding.getId() + " | RuleId: " + nistFinding.getRuleId() + " | FrameworkIds: " + nistFinding.getFrameworkIds());
    }
}
