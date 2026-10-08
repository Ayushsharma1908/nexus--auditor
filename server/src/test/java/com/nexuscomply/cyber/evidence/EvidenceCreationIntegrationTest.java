package com.nexuscomply.cyber.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClients;
import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.compliance.GenericRuleEvaluator;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.RuleResultStatus;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.model.Control;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleDocument;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.compliance.persistence.ControlDocument;
import com.nexuscomply.cyber.compliance.persistence.ControlRepository;
import com.nexuscomply.cyber.compliance.persistence.FrameworkDocument;
import com.nexuscomply.cyber.compliance.persistence.FrameworkRepository;
import com.nexuscomply.cyber.compliance.rules.cis.CisCiscoIosXeRuleSeeder;
import com.nexuscomply.cyber.evidence.persistence.EvidenceDocument;
import com.nexuscomply.cyber.evidence.persistence.EvidenceRepository;
import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.finding.FindingContext;
import com.nexuscomply.cyber.finding.FindingCreationService;
import com.nexuscomply.cyber.finding.FindingCreationServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceCreationIntegrationTest {

    private static MongoServer mongoServer;
    private static MongoTemplate mongoTemplate;

    private FrameworkRepository frameworkRepository;
    private ControlRepository controlRepository;
    private ComplianceRuleRepository ruleRepository;
    private NormalizedConfigurationRepository normalizedConfigRepository;
    private FindingRepository findingRepository;
    private EvidenceRepository evidenceRepository;

    private CisCiscoIosXeRuleSeeder cisSeeder;
    private NormalizationService normalizationService;
    private GenericRuleEvaluator evaluator;
    private EvidenceCreationService evidenceCreationService;
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
        mongoTemplate.dropCollection(EvidenceDocument.class);

        MongoRepositoryFactory repositoryFactory = new MongoRepositoryFactory(mongoTemplate);
        frameworkRepository = repositoryFactory.getRepository(FrameworkRepository.class);
        controlRepository = repositoryFactory.getRepository(ControlRepository.class);
        ruleRepository = repositoryFactory.getRepository(ComplianceRuleRepository.class);
        normalizedConfigRepository = repositoryFactory.getRepository(NormalizedConfigurationRepository.class);
        findingRepository = repositoryFactory.getRepository(FindingRepository.class);
        evidenceRepository = repositoryFactory.getRepository(EvidenceRepository.class);

        cisSeeder = new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository);

        ParserService parserService = new ParserServiceImpl(List.of(new CiscoIosParser()));
        normalizationService = new NormalizationServiceImpl(parserService, normalizedConfigRepository);
        evaluator = new GenericRuleEvaluator();

        evidenceCreationService = new EvidenceCreationServiceImpl(evidenceRepository);
        findingCreationService = new FindingCreationServiceImpl(
                findingRepository,
                ruleRepository,
                controlRepository,
                evidenceCreationService,
                normalizedConfigRepository
        );

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Integration Test: Full pipeline creates and links Evidence document matching parser SourceMapEntry side-by-side")
    void testFullPipelineEvidenceCreationAndLinkage() throws Exception {
        cisSeeder.seed();

        // Deliberate non-compliant configuration where telnet IS enabled (transport input telnet ssh)
        String failConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-TELNET-FAIL",
                "!",
                "line vty 0 4",
                " transport input telnet ssh",
                "!"
        );

        // 1. Parser + Normalization
        NormalizedConfigurationDocument normalizedDoc = normalizationService.normalizeAndPersist(
                failConfig, "dev-cisco-01", "cfg-evidence-001", "ver-evidence-001", "Cisco", "IOS-XE", "17.6"
        );
        CanonicalSecurityModel canonical = normalizedDoc.getCanonical();
        List<SourceMapEntry> sourceMap = normalizedDoc.getSourceMap();
        assertThat(sourceMap).isNotEmpty();

        // Locate raw parser SourceMapEntry for security.telnet.enabled
        SourceMapEntry parserSourceMapEntry = sourceMap.stream()
                .filter(e -> "security.telnet.enabled".equals(e.getCanonicalField()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("security.telnet.enabled not found in parser source map"));

        System.out.println("=== RAW PARSER SOURCEMAP ENTRY ===");
        System.out.println("canonicalField: " + parserSourceMapEntry.getCanonicalField());
        System.out.println("sourceLine:     " + parserSourceMapEntry.getSourceLine());
        System.out.println("rawText:        " + parserSourceMapEntry.getRawText());

        // 2. Rule Evaluation
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

        // 3. Finding + Evidence Creation via FindingCreationServiceImpl
        FindingContext context = new FindingContext(
                "audit-evidence-100",
                "dev-cisco-01",
                "cfg-evidence-001",
                "ver-evidence-001",
                sourceMap
        );

        Control control = new Control();
        control.setId(cis122Ctrl.getId());
        control.setControlId(cis122Ctrl.getControlId());

        Optional<Finding> findingOpt = findingCreationService.createFinding(result, cis122Rule, control, context);
        assertThat(findingOpt).isPresent();
        Finding finding = findingOpt.get();

        // 4. Verify Finding is persisted with non-empty evidenceIds
        assertThat(finding.getEvidenceIds()).isNotEmpty();
        assertThat(finding.getEvidenceIds()).hasSize(1);
        String evidenceId = finding.getEvidenceIds().get(0);

        // Fetch raw documents directly from MongoDB
        Document rawFinding = mongoTemplate.getCollection("findings").find(new Document("_id", finding.getId())).first();
        Document rawEvidence = mongoTemplate.getCollection("evidence").find(new Document("_id", evidenceId)).first();

        assertThat(rawFinding).isNotNull();
        assertThat(rawEvidence).isNotNull();

        System.out.println("=== RAW PERSISTED FINDING DOCUMENT ===");
        System.out.println(rawFinding.toJson());

        System.out.println("=== RAW PERSISTED EVIDENCE DOCUMENT ===");
        System.out.println(rawEvidence.toJson());

        // Acceptance Criteria: Field-by-field check against schema1.md section 11
        assertThat(rawEvidence.getString("_id")).isEqualTo(evidenceId);
        assertThat(rawEvidence.getString("findingId")).isEqualTo(finding.getId());
        assertThat(rawEvidence.getString("auditId")).isEqualTo("audit-evidence-100");
        assertThat(rawEvidence.getString("configurationId")).isEqualTo("cfg-evidence-001");
        assertThat(rawEvidence.getString("versionId")).isEqualTo("ver-evidence-001");
        assertThat(rawEvidence.getString("reason")).isEqualTo("Configuration explicitly permits Telnet.");
        assertThat(rawEvidence.getDate("createdAt")).isNotNull();

        // Source nested object
        Document sourceObj = (Document) rawEvidence.get("source");
        assertThat(sourceObj).isNotNull();
        assertThat(sourceObj.getInteger("lineNumber")).isEqualTo(parserSourceMapEntry.getSourceLine());
        assertThat(sourceObj.getString("rawText")).isEqualTo(parserSourceMapEntry.getRawText());
        assertThat(sourceObj.getString("sourceType")).isEqualTo("CONFIGURATION");

        // Canonical nested object
        Document canonicalObj = (Document) rawEvidence.get("canonical");
        assertThat(canonicalObj).isNotNull();
        assertThat(canonicalObj.getString("field")).isEqualTo("security.telnet.enabled");
        assertThat(canonicalObj.get("value")).isEqualTo(true);

        // Cross-link verification: finding.evidenceIds contains evidence._id
        List<String> findingEvidenceIds = rawFinding.getList("evidenceIds", String.class);
        assertThat(findingEvidenceIds).containsExactly(evidenceId);

        // Traceability check: versionId preserved
        assertThat(rawEvidence.getString("versionId")).isEqualTo("ver-evidence-001");

        // Side-by-side comparison check
        assertThat(sourceObj.getInteger("lineNumber")).isEqualTo(5);
        assertThat(sourceObj.getString("rawText")).isEqualTo(" transport input telnet ssh");
        assertThat(sourceObj.getString("rawText")).isEqualTo(parserSourceMapEntry.getRawText());
    }

    @Test
    @DisplayName("Acceptance Criterion: PASS rule evaluation produces ZERO findings and ZERO evidence")
    void testPassResultProducesZeroEvidenceAndZeroFindings() {
        cisSeeder.seed();

        // Compliant configuration
        String passConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-TELNET-PASS",
                "!",
                "line vty 0 4",
                " transport input ssh",
                "!"
        );

        NormalizedConfigurationDocument normalizedDoc = normalizationService.normalizeAndPersist(
                passConfig, "dev-cisco-02", "cfg-pass-001", "ver-pass-001", "Cisco", "IOS-XE", "17.6"
        );

        ComplianceRuleDocument cis122Doc = ruleRepository.findByRuleCode("CIS-1.2.2").orElseThrow();
        ComplianceRule cis122Rule = new ComplianceRule();
        cis122Rule.setId(cis122Doc.getId());
        cis122Rule.setRuleCode(cis122Doc.getRuleCode());
        cis122Rule.setRequirement(cis122Doc.getExpression());
        cis122Rule.setSeverity(cis122Doc.getSeverity());

        RuleEvaluationResult result = evaluator.evaluate(normalizedDoc.getCanonical(), cis122Rule);
        assertThat(result.getStatus()).isEqualTo(RuleResultStatus.PASS);

        FindingContext context = new FindingContext(
                "audit-pass-01", "dev-cisco-02", "cfg-pass-001", "ver-pass-001", normalizedDoc.getSourceMap()
        );

        Optional<Finding> findingOpt = findingCreationService.createFinding(result, cis122Rule, null, context);

        assertThat(findingOpt).isEmpty();
        assertThat(findingRepository.count()).isEqualTo(0);
        assertThat(evidenceRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Acceptance Criterion: Missing SourceMapEntry edge case produces valid Evidence document without throwing")
    void testMissingSourceMapEntryEdgeCasePersistsDefensively() {
        // Construct FAIL result on a canonical field that has no entry in sourceMap
        RuleEvaluationResult result = RuleEvaluationResult.fail(
                "CTRL-MISSING",
                "RULE-MISSING-TEST",
                true,
                false,
                "MEDIUM",
                "custom.unmapped.field",
                "Custom check failed"
        );

        FindingContext context = new FindingContext(
                "audit-defensive", "dev-cisco-def", "cfg-def-001", "ver-def-001", List.of()
        );

        Optional<Finding> findingOpt = findingCreationService.createFinding(result, context);
        assertThat(findingOpt).isPresent();

        Finding finding = findingOpt.get();
        assertThat(finding.getEvidenceIds()).hasSize(1);
        String evidenceId = finding.getEvidenceIds().get(0);

        Document rawEvidence = mongoTemplate.getCollection("evidence").find(new Document("_id", evidenceId)).first();
        assertThat(rawEvidence).isNotNull();

        Document sourceObj = (Document) rawEvidence.get("source");
        assertThat(sourceObj.get("lineNumber")).isNull();
        assertThat(sourceObj.getString("rawText")).contains("No explicit configuration line observed");
        assertThat(sourceObj.getString("sourceType")).isEqualTo("CONFIGURATION");

        Document canonicalObj = (Document) rawEvidence.get("canonical");
        assertThat(canonicalObj.getString("field")).isEqualTo("custom.unmapped.field");
        assertThat(canonicalObj.get("value")).isEqualTo(false);
    }
}
