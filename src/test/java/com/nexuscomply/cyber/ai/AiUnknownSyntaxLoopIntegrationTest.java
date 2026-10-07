package com.nexuscomply.cyber.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexuscomply.cyber.ai.persistence.AiJobDocument;
import com.nexuscomply.cyber.ai.persistence.AiJobRepository;
import com.nexuscomply.cyber.ai.persistence.AiMappingDocument;
import com.nexuscomply.cyber.ai.persistence.AiMappingRepository;
import com.nexuscomply.cyber.ai.service.AiMappingService;
import com.nexuscomply.cyber.ai.service.AiMappingServiceImpl;
import com.nexuscomply.cyber.ai.service.DeterministicStubSuggestionProvider;
import com.nexuscomply.cyber.ai.service.SuggestionResult;
import com.nexuscomply.cyber.audit.Audit;
import com.nexuscomply.cyber.audit.AuditOrchestrationService;
import com.nexuscomply.cyber.audit.AuditOrchestrationServiceImpl;
import com.nexuscomply.cyber.audit.AuditStatus;
import com.nexuscomply.cyber.audit.AuditSummary;
import com.nexuscomply.cyber.audit.persistence.AuditDocument;
import com.nexuscomply.cyber.audit.persistence.AuditRepository;
import com.nexuscomply.cyber.compliance.DefaultRuleApplicabilityChecker;
import com.nexuscomply.cyber.compliance.GenericRuleEvaluator;
import com.nexuscomply.cyber.compliance.RuleApplicabilityChecker;
import com.nexuscomply.cyber.compliance.RuleEvaluator;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleDocument;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.compliance.persistence.ControlDocument;
import com.nexuscomply.cyber.compliance.persistence.ControlRepository;
import com.nexuscomply.cyber.compliance.persistence.FrameworkDocument;
import com.nexuscomply.cyber.compliance.persistence.FrameworkRepository;
import com.nexuscomply.cyber.compliance.rules.cis.CisCiscoIosXeRuleSeeder;
import com.nexuscomply.cyber.compliance.rules.iso.Iso27001RuleSeeder;
import com.nexuscomply.cyber.compliance.rules.nist.NistSp80053RuleSeeder;
import com.nexuscomply.cyber.detection.VendorDetectionResponse;
import com.nexuscomply.cyber.detection.VendorDetectionService;
import com.nexuscomply.cyber.detection.VendorDetectionStatus;
import com.nexuscomply.cyber.evidence.EvidenceCreationService;
import com.nexuscomply.cyber.evidence.EvidenceCreationServiceImpl;
import com.nexuscomply.cyber.evidence.persistence.EvidenceDocument;
import com.nexuscomply.cyber.evidence.persistence.EvidenceRepository;
import com.nexuscomply.cyber.finding.FindingCreationService;
import com.nexuscomply.cyber.finding.FindingCreationServiceImpl;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.normalization.NormalizationService;
import com.nexuscomply.cyber.normalization.NormalizationServiceImpl;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.ParserService;
import com.nexuscomply.cyber.parser.ParserServiceImpl;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import com.nexuscomply.cyber.parser.fortinet.FortinetFortiOSParser;
import com.nexuscomply.cyber.parser.juniper.JuniperJunosParser;
import com.nexuscomply.cyber.parser.paloalto.PaloAltoPanOsParser;
import com.nexuscomply.cyber.risk.RiskCalculationService;
import com.nexuscomply.cyber.risk.RiskCalculationServiceImpl;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentRepository;
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
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 2.10b Integration Tests: Minimal AI Unknown-Syntax Feedback Loop.
 * Tests strictly adhere to schema1.md section 17 & 18 and cyberlayer.pdf Section 8, 28, 34 Criterion 4.
 */
class AiUnknownSyntaxLoopIntegrationTest {

    private static MongoServer mongoServer;
    private static MongoTemplate mongoTemplate;

    private FrameworkRepository frameworkRepository;
    private ControlRepository controlRepository;
    private ComplianceRuleRepository ruleRepository;
    private NormalizedConfigurationRepository normalizedConfigRepository;
    private FindingRepository findingRepository;
    private EvidenceRepository evidenceRepository;
    private RiskAssessmentRepository riskRepository;
    private AuditRepository auditRepository;
    private AiMappingRepository aiMappingRepository;
    private AiJobRepository aiJobRepository;

    private CisCiscoIosXeRuleSeeder cisSeeder;
    private NistSp80053RuleSeeder nistSeeder;
    private Iso27001RuleSeeder isoSeeder;

    private ParserService parserService;
    private NormalizationService normalizationService;
    private DeterministicStubSuggestionProvider suggestionProvider;
    private AiMappingService aiMappingService;
    private AuditOrchestrationService auditOrchestrationService;
    private EvidenceCreationService evidenceCreationService;
    private FindingCreationService findingCreationService;
    private RiskCalculationService riskCalculationService;
    private VendorDetectionService vendorDetectionService;
    private RuleApplicabilityChecker applicabilityChecker;
    private RuleEvaluator ruleEvaluator;

    private ObjectMapper objectMapper;

    @BeforeAll
    static void startInMemoryMongo() {
        mongoServer = new MongoServer(new MemoryBackend());
        InetSocketAddress address = mongoServer.bind();
        String connectionString = "mongodb://" + address.getHostName() + ":" + address.getPort() + "/testdb";
        mongoTemplate = new MongoTemplate(new SimpleMongoClientDatabaseFactory(connectionString));
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
        mongoTemplate.dropCollection("risk_assessments");
        mongoTemplate.dropCollection(AuditDocument.class);
        mongoTemplate.dropCollection(AiMappingDocument.class);
        mongoTemplate.dropCollection(AiJobDocument.class);

        MongoRepositoryFactory repositoryFactory = new MongoRepositoryFactory(mongoTemplate);
        frameworkRepository = repositoryFactory.getRepository(FrameworkRepository.class);
        controlRepository = repositoryFactory.getRepository(ControlRepository.class);
        ruleRepository = repositoryFactory.getRepository(ComplianceRuleRepository.class);
        normalizedConfigRepository = repositoryFactory.getRepository(NormalizedConfigurationRepository.class);
        findingRepository = repositoryFactory.getRepository(FindingRepository.class);
        evidenceRepository = repositoryFactory.getRepository(EvidenceRepository.class);
        riskRepository = repositoryFactory.getRepository(RiskAssessmentRepository.class);
        auditRepository = repositoryFactory.getRepository(AuditRepository.class);
        aiMappingRepository = repositoryFactory.getRepository(AiMappingRepository.class);
        aiJobRepository = repositoryFactory.getRepository(AiJobRepository.class);

        cisSeeder = new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        nistSeeder = new NistSp80053RuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        isoSeeder = new Iso27001RuleSeeder(frameworkRepository, controlRepository, ruleRepository);

        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        suggestionProvider = new DeterministicStubSuggestionProvider();
        aiMappingService = new AiMappingServiceImpl(aiMappingRepository, aiJobRepository, suggestionProvider);

        parserService = new ParserServiceImpl(List.of(
                new CiscoIosParser(),
                new JuniperJunosParser(),
                new FortinetFortiOSParser(),
                new PaloAltoPanOsParser()
        ), aiMappingRepository);

        normalizationService = new NormalizationServiceImpl(parserService, normalizedConfigRepository);
        evidenceCreationService = new EvidenceCreationServiceImpl(evidenceRepository);
        findingCreationService = new FindingCreationServiceImpl(
                findingRepository,
                ruleRepository,
                controlRepository,
                evidenceCreationService,
                normalizedConfigRepository
        );
        riskCalculationService = new RiskCalculationServiceImpl(riskRepository);

        vendorDetectionService = new com.nexuscomply.cyber.detection.VendorFingerprintDetectionService();

        applicabilityChecker = new DefaultRuleApplicabilityChecker();
        ruleEvaluator = new GenericRuleEvaluator();

        auditOrchestrationService = new AuditOrchestrationServiceImpl(
                auditRepository,
                vendorDetectionService,
                parserService,
                normalizationService,
                ruleRepository,
                controlRepository,
                applicabilityChecker,
                ruleEvaluator,
                findingCreationService,
                riskCalculationService,
                aiMappingService
        );

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("1. Pending Capture: Unknown lines are stored as PENDING_REVIEW; no provider call happens during audit")
    void testPendingCapture() throws Exception {
        String junosConfigWithUnknown = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services legacy-telnet active"
        );

        Audit audit = auditOrchestrationService.startAudit("dev-junos-01", "cfg-01", "v1.0", junosConfigWithUnknown);
        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());

        // Verify stored in ai_mappings with PENDING_REVIEW
        AiMappingDocument mapping = aiMappingRepository.findByVendorAndPlatformAndRawSyntax(
                "Juniper", "JUNOS", "set system services legacy-telnet active"
        ).orElseThrow();

        assertThat(mapping.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(mapping.getSuggestedBy()).isNull();
        assertThat(mapping.getCanonicalField()).isNull();
        assertThat(mapping.getMappedValue()).isNull();
        assertThat(mapping.getUsageCount()).isEqualTo(0);
        assertThat(mapping.getReview().getReviewerId()).isNull();

        // Verify zero AI jobs triggered during audit
        assertThat(aiJobRepository.findAll()).isEmpty();

        System.out.println("=== RAW PERSISTED AI MAPPING DOCUMENT (PENDING CAPTURE) ===");
        org.bson.Document rawMapping = mongoTemplate.getCollection("ai_mappings").find(new org.bson.Document("_id", mapping.getId())).first();
        System.out.println(rawMapping != null ? rawMapping.toJson() : "null");
    }

    @Test
    @DisplayName("2. Approve then Re-audit changes the canonical fact, creates finding & evidence with APPROVED_MAPPING source type")
    void testApproveThenReauditChangesCanonicalFact() throws Exception {
        String junosConfig = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services legacy-telnet active"
        );

        // Run 1: Unknown syntax not mapped
        Audit audit1 = auditOrchestrationService.startAudit("dev-junos-02", "cfg-02", "v1.0", junosConfig);
        assertThat(audit1.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());
        // In audit 1, telnet was unknown, so 0 telnet findings
        List<FindingDocument> findings1 = findingRepository.findByDeviceId("dev-junos-02");
        assertThat(findings1).isEmpty();

        AiMappingDocument pendingMapping = aiMappingRepository.findByVendorAndPlatformAndRawSyntax(
                "Juniper", "JUNOS", "set system services legacy-telnet active"
        ).orElseThrow();

        // Human requests suggestion
        AiMappingDocument withSuggestion = aiMappingService.requestSuggestion(pendingMapping.getId());
        assertThat(withSuggestion.getStatus()).isEqualTo("PENDING_REVIEW"); // Still pending review
        assertThat(withSuggestion.getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(withSuggestion.getMappedValue()).isEqualTo(true);
        assertThat(withSuggestion.getSuggestedBy()).isEqualTo("AI");

        // Human approves mapping
        AiMappingDocument approvedMapping = aiMappingService.approve(pendingMapping.getId(), "sec-admin-101");
        assertThat(approvedMapping.getStatus()).isEqualTo("APPROVED");
        assertThat(approvedMapping.getReview().getReviewerId()).isEqualTo("sec-admin-101");
        assertThat(approvedMapping.getReview().getReviewedAt()).isNotNull();

        // Run 2: Re-audit on the same configuration
        Audit audit2 = auditOrchestrationService.startAudit("dev-junos-02", "cfg-02", "v2.0", junosConfig);
        assertThat(audit2.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());

        // Normalized configuration now contains mapped canonical fact
        NormalizedConfigurationDocument normDoc = normalizedConfigRepository.findById(audit2.getNormalizedConfigurationId()).orElseThrow();
        assertThat(normDoc.getCanonical().getSecurity()).containsEntry("telnet", java.util.Map.of("enabled", true));

        // Finding is created because telnet is now known to be enabled (deliberate violation of NIST/ISO rules)
        List<FindingDocument> findings2 = findingRepository.findByDeviceId("dev-junos-02");
        assertThat(findings2).hasSize(2);
        assertThat(findings2).allMatch(f -> "security.telnet.enabled".equals(f.getCanonicalField()));
        assertThat(findings2).allMatch(f -> f.getDescription().contains("[Source: Approved AI Mapping]"));

        // Traceable Evidence has sourceType = APPROVED_MAPPING
        for (FindingDocument f : findings2) {
            assertThat(f.getEvidenceIds()).isNotEmpty();
            for (String evId : f.getEvidenceIds()) {
                EvidenceDocument ev = evidenceRepository.findById(evId).orElseThrow();
                assertThat(ev.getSource().getSourceType()).isEqualTo("APPROVED_MAPPING");
                assertThat(ev.getSource().getRawText()).isEqualTo("set system services legacy-telnet active");
            }
        }

        // Mapping usage count incremented exactly once per audit (moved to orchestration layer)
        AiMappingDocument refreshedMapping = aiMappingRepository.findById(pendingMapping.getId()).orElseThrow();
        assertThat(refreshedMapping.getUsageCount()).isEqualTo(1);

        // Parsing with read-only flag performs zero DB writes and leaves usageCount unchanged
        long countBefore = aiMappingRepository.count();
        ParserResult readOnlyResult = parserService.parse(junosConfig, "Juniper", "JUNOS", true);
        assertThat(readOnlyResult).isNotNull();
        assertThat(aiMappingRepository.count()).isEqualTo(countBefore);
        assertThat(aiMappingRepository.findById(pendingMapping.getId()).orElseThrow().getUsageCount()).isEqualTo(1);

        System.out.println("=== RAW PERSISTED AI MAPPING DOCUMENT (APPROVED) ===");
        org.bson.Document rawAppr = mongoTemplate.getCollection("ai_mappings").find(new org.bson.Document("_id", approvedMapping.getId())).first();
        System.out.println(rawAppr != null ? rawAppr.toJson() : "null");

        System.out.println("=== RAW PERSISTED FINDING FROM APPROVED MAPPING ===");
        org.bson.Document rawFinding = mongoTemplate.getCollection("findings").find(new org.bson.Document("_id", findings2.get(0).getId())).first();
        System.out.println(rawFinding != null ? rawFinding.toJson() : "null");

        System.out.println("=== RAW PERSISTED EVIDENCE FROM APPROVED MAPPING ===");
        org.bson.Document rawEv = mongoTemplate.getCollection("evidence").find(new org.bson.Document("_id", findings2.get(0).getEvidenceIds().get(0))).first();
        System.out.println(rawEv != null ? rawEv.toJson() : "null");
    }

    @Test
    @DisplayName("3. Reject leaves the line UNKNOWN; next audit remains unchanged")
    void testRejectLeavesLineUnknown() {
        String junosConfig = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services legacy-telnet active"
        );

        auditOrchestrationService.startAudit("dev-junos-03", "cfg-03", "v1.0", junosConfig);
        AiMappingDocument pendingMapping = aiMappingRepository.findByVendorAndPlatformAndRawSyntax(
                "Juniper", "JUNOS", "set system services legacy-telnet active"
        ).orElseThrow();

        aiMappingService.requestSuggestion(pendingMapping.getId());

        // Human rejects mapping
        AiMappingDocument rejectedMapping = aiMappingService.reject(pendingMapping.getId(), "sec-admin-102", "Unrecognized or dangerous command");
        assertThat(rejectedMapping.getStatus()).isEqualTo("REJECTED");
        assertThat(rejectedMapping.getReview().getComment()).isEqualTo("Unrecognized or dangerous command");

        // Next audit: line remains UNKNOWN
        Audit auditNext = auditOrchestrationService.startAudit("dev-junos-03", "cfg-03", "v2.0", junosConfig);
        assertThat(auditNext.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());
        List<FindingDocument> findings = findingRepository.findByDeviceId("dev-junos-03");
        assertThat(findings).isEmpty(); // Line was not mapped to telnet.enabled
    }

    @Test
    @DisplayName("4. Blank reviewerId is refused on approve and reject")
    void testBlankReviewerIdRefused() {
        AiMappingDocument doc = aiMappingService.recordUnknownSyntax("Juniper", "JUNOS", "set dummy syntax");
        aiMappingService.requestSuggestion(doc.getId());

        assertThatThrownBy(() -> aiMappingService.approve(doc.getId(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reviewer ID must be non-blank");

        assertThatThrownBy(() -> aiMappingService.approve(doc.getId(), "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reviewer ID must be non-blank");

        assertThatThrownBy(() -> aiMappingService.reject(doc.getId(), null, "some reason"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reviewer ID must be non-blank");

        assertThatThrownBy(() -> aiMappingService.reject(doc.getId(), "  ", "some reason"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reviewer ID must be non-blank");

        // Verify status remained PENDING_REVIEW
        AiMappingDocument unchanged = aiMappingRepository.findById(doc.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    @DisplayName("5. Not-allowlisted field or invalid type is strictly refused")
    void testNotAllowlistedFieldRefused() {
        AiMappingDocument doc = aiMappingService.recordUnknownSyntax("Juniper", "JUNOS", "set invalid field line");

        // Register stub returning a non-allowlisted field
        suggestionProvider.registerStub("set invalid field line", new SuggestionResult(
                "system.admin.password", "admin123", 0.99, "Proposes forbidden field"
        ));

        assertThatThrownBy(() -> aiMappingService.requestSuggestion(doc.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in the canonical security model allowlist");

        // Register stub returning invalid type for valid field (String instead of Integer for ssh.version)
        suggestionProvider.registerStub("set invalid field line", new SuggestionResult(
                "security.ssh.version", "version-two", 0.99, "Invalid type"
        ));

        assertThatThrownBy(() -> aiMappingService.requestSuggestion(doc.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid value type");
    }

    @Test
    @DisplayName("6. Provider exception leaves audit COMPLETED (Fail-Safe)")
    void testProviderExceptionLeavesAuditCompleted() {
        suggestionProvider.setSimulateException(true, "AI API connection timed out");

        String junosConfig = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services legacy-telnet active"
        );

        // Audit succeeds completely without invoking external AI
        Audit audit = auditOrchestrationService.startAudit("dev-junos-06", "cfg-06", "v1.0", junosConfig);
        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());
        assertThat(audit.getSummary().getTotalControls()).isEqualTo(21);

        // Explicit requestSuggestion fails safely and records failed AiJobDocument
        AiMappingDocument doc = aiMappingRepository.findByVendorAndPlatformAndRawSyntax(
                "Juniper", "JUNOS", "set system services legacy-telnet active"
        ).orElseThrow();

        assertThatThrownBy(() -> aiMappingService.requestSuggestion(doc.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI API connection timed out");

        List<AiJobDocument> failedJobs = aiJobRepository.findByStatus("FAILED");
        assertThat(failedJobs).isNotEmpty();
        assertThat(failedJobs.get(0).getError()).contains("AI API connection timed out");
    }

    @Test
    @DisplayName("7. Deterministic double run: Re-running audit on same config gives identical results")
    void testDeterministicDoubleRun() {
        String junosConfig = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services legacy-telnet active"
        );

        // Capture, suggest, and approve
        auditOrchestrationService.startAudit("dev-junos-07", "cfg-07", "v1.0", junosConfig);
        AiMappingDocument mapping = aiMappingRepository.findByVendorAndPlatformAndRawSyntax(
                "Juniper", "JUNOS", "set system services legacy-telnet active"
        ).orElseThrow();
        aiMappingService.requestSuggestion(mapping.getId());
        aiMappingService.approve(mapping.getId(), "sec-admin-107");

        // Run 1
        Audit run1 = auditOrchestrationService.startAudit("dev-junos-07", "cfg-07", "v2.0", junosConfig);
        // Run 2
        Audit run2 = auditOrchestrationService.startAudit("dev-junos-07", "cfg-07", "v3.0", junosConfig);

        AuditSummary sum1 = run1.getSummary();
        AuditSummary sum2 = run2.getSummary();

        assertThat(sum1.getTotalControls()).isEqualTo(sum2.getTotalControls()).isEqualTo(21);
        assertThat(sum1.getPassed()).isEqualTo(sum2.getPassed()).isEqualTo(2); // SSH version passes on NIST and ISO
        assertThat(sum1.getFailed()).isEqualTo(sum2.getFailed()).isEqualTo(2); // Telnet enabled fails on NIST and ISO
        assertThat(sum1.getUnknown()).isEqualTo(sum2.getUnknown()).isEqualTo(10);
        assertThat(sum1.getNotApplicable()).isEqualTo(sum2.getNotApplicable()).isEqualTo(7);
        assertThat(sum1.getError()).isEqualTo(sum2.getError()).isEqualTo(0);
        assertThat(run1.getComplianceScore()).isEqualTo(run2.getComplianceScore()).isEqualTo(14.3);
    }

    @Test
    @DisplayName("8. Mapping scoped to wrong vendor does not apply")
    void testMappingScopedToWrongVendorDoesNotApply() {
        // Approve a mapping scoped strictly to Juniper / JUNOS
        AiMappingDocument doc = aiMappingService.recordUnknownSyntax("Juniper", "JUNOS", "custom-telnet enable");
        suggestionProvider.registerStub("custom-telnet enable", new SuggestionResult(
                "security.telnet.enabled", true, 0.95, "Custom telnet directive"
        ));
        aiMappingService.requestSuggestion(doc.getId());
        aiMappingService.approve(doc.getId(), "sec-admin-108");

        // Audit a Cisco configuration that has the exact same text line
        String ciscoConfig = String.join("\n",
                "version 17.6",
                "hostname CISCO-RTR",
                "custom-telnet enable"
        );

        Audit ciscoAudit = auditOrchestrationService.startAudit("dev-cisco-08", "cfg-cisco-08", "v1.0", ciscoConfig);
        assertThat(ciscoAudit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());

        NormalizedConfigurationDocument ciscoNorm = normalizedConfigRepository.findById(ciscoAudit.getNormalizedConfigurationId()).orElseThrow();
        // Cisco canonical telnet MUST NOT be set by the Juniper mapping
        assertThat(ciscoNorm.getCanonical().getSecurity().get("telnet")).isNull();
    }

    @Test
    @DisplayName("9. Section 28 End-to-End Acceptance Scenario: Unknown command -> parser UNKNOWN -> AI suggestion -> human approval -> mapping stored -> normalization -> deterministic re-audit")
    void testSection28EndToEndScenario() throws Exception {
        String vendor = "Juniper";
        String platform = "JUNOS";
        String configText = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services legacy-telnet active"
        );

        // Step 1: Unknown command -> parser UNKNOWN
        Audit audit1 = auditOrchestrationService.startAudit("device-s28-01", "cfg-s28-01", "v1.0", configText);
        assertThat(audit1.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());

        // Line was captured as PENDING_REVIEW in ai_mappings
        AiMappingDocument captured = aiMappingRepository.findByVendorAndPlatformAndRawSyntax(vendor, platform, "set system services legacy-telnet active").orElseThrow();
        assertThat(captured.getStatus()).isEqualTo("PENDING_REVIEW");

        System.out.println("=== SECTION 28 PRE-APPROVAL AUDIT DOCUMENT ===");
        org.bson.Document rawAudit1 = mongoTemplate.getCollection("audits").find(new org.bson.Document("_id", audit1.getId())).first();
        System.out.println(rawAudit1 != null ? rawAudit1.toJson() : "null");

        System.out.println("=== SECTION 28 PRE-APPROVAL PENDING AI MAPPING DOCUMENT ===");
        org.bson.Document rawPending = mongoTemplate.getCollection("ai_mappings").find(new org.bson.Document("_id", captured.getId())).first();
        System.out.println(rawPending != null ? rawPending.toJson() : "null");

        // Step 2: AI suggestion
        AiMappingDocument suggested = aiMappingService.requestSuggestion(captured.getId());
        assertThat(suggested.getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(suggested.getMappedValue()).isEqualTo(true);
        assertThat(suggested.getStatus()).isEqualTo("PENDING_REVIEW"); // Not auto-approved

        // Step 3: Human approval -> mapping stored
        AiMappingDocument approved = aiMappingService.approve(suggested.getId(), "human-auditor-99");
        assertThat(approved.getStatus()).isEqualTo("APPROVED");

        // Step 4: Normalization -> deterministic re-audit
        Audit audit2 = auditOrchestrationService.startAudit("device-s28-01", "cfg-s28-01", "v2.0", configText);
        assertThat(audit2.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());

        NormalizedConfigurationDocument normDoc = normalizedConfigRepository.findById(audit2.getNormalizedConfigurationId()).orElseThrow();
        assertThat(normDoc.getCanonical().getSecurity()).containsEntry("telnet", java.util.Map.of("enabled", true));

        List<FindingDocument> findings = findingRepository.findByDeviceId("device-s28-01");
        assertThat(findings).hasSize(2);
        assertThat(findings).allMatch(f -> "security.telnet.enabled".equals(f.getCanonicalField()));
        assertThat(findings).allMatch(f -> f.getDescription().contains("[Source: Approved AI Mapping]"));

        EvidenceDocument ev = evidenceRepository.findById(findings.get(0).getEvidenceIds().get(0)).orElseThrow();
        assertThat(ev.getSource().getSourceType()).isEqualTo("APPROVED_MAPPING");

        // Print raw MongoDB documents for Section 28 verification
        System.out.println("=== SECTION 28 RAW PERSISTED AI MAPPING DOCUMENT ===");
        org.bson.Document rawAiMapping = mongoTemplate.getCollection("ai_mappings").find(new org.bson.Document("_id", approved.getId())).first();
        System.out.println(rawAiMapping != null ? rawAiMapping.toJson() : "null");

        System.out.println("=== SECTION 28 RAW PERSISTED AI JOB DOCUMENT ===");
        org.bson.Document rawAiJob = mongoTemplate.getCollection("ai_jobs").find().first();
        System.out.println(rawAiJob != null ? rawAiJob.toJson() : "null");

        System.out.println("=== SECTION 28 RAW PERSISTED FINDING DOCUMENT ===");
        org.bson.Document rawFinding = mongoTemplate.getCollection("findings").find(new org.bson.Document("_id", findings.get(0).getId())).first();
        System.out.println(rawFinding != null ? rawFinding.toJson() : "null");

        System.out.println("=== SECTION 28 RAW PERSISTED EVIDENCE DOCUMENT ===");
        org.bson.Document rawEv = mongoTemplate.getCollection("evidence").find(new org.bson.Document("_id", ev.getId())).first();
        System.out.println(rawEv != null ? rawEv.toJson() : "null");

        System.out.println("=== SECTION 28 RAW PERSISTED AUDIT DOCUMENT ===");
        org.bson.Document rawAudit = mongoTemplate.getCollection("audits").find(new org.bson.Document("_id", audit2.getId())).first();
        System.out.println(rawAudit != null ? rawAudit.toJson() : "null");
    }

    @Test
    @DisplayName("10. Fail-Safe: ai_mappings read throws in parser -> audit remains COMPLETED")
    void testFailSafe_AiMappingsReadThrowsInParserLeavesAuditCompleted() {
        AiMappingRepository mockRepo = org.mockito.Mockito.mock(AiMappingRepository.class);
        org.mockito.Mockito.when(mockRepo.findByVendorAndPlatformAndRawSyntax(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())
        ).thenThrow(new RuntimeException("Simulated Mongo read timeout / connection failure"));

        ParserService failingParser = new ParserServiceImpl(
                List.of(new CiscoIosParser(), new JuniperJunosParser(), new FortinetFortiOSParser(), new PaloAltoPanOsParser()),
                mockRepo
        );

        NormalizationService normService = new NormalizationServiceImpl(failingParser, normalizedConfigRepository);

        AuditOrchestrationServiceImpl customOrch = new AuditOrchestrationServiceImpl(
                auditRepository,
                vendorDetectionService,
                failingParser,
                normService,
                ruleRepository,
                controlRepository,
                applicabilityChecker,
                ruleEvaluator,
                findingCreationService,
                riskCalculationService,
                aiMappingService
        );

        String config = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services legacy-telnet active"
        );

        Audit audit = customOrch.startAudit("dev-fs-read-01", "cfg-fs-01", "v1.0", config);
        assertThat(audit).isNotNull();
        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());
        assertThat(audit.getSummary().getTotalControls()).isEqualTo(21);

        System.out.println("=== FAIL-SAFE AUDIT RESULT (AI MAPPINGS READ THROWS) ===");
        org.bson.Document rawAudit = mongoTemplate.getCollection("audits").find(new org.bson.Document("_id", audit.getId())).first();
        System.out.println(rawAudit != null ? rawAudit.toJson() : "null");
    }

    @Test
    @DisplayName("11. Fail-Safe: ai_mappings write throws in pending capture -> audit remains COMPLETED")
    void testFailSafe_AiMappingsWriteThrowsInPendingCaptureLeavesAuditCompleted() {
        AiMappingService mockAiService = org.mockito.Mockito.mock(AiMappingService.class);
        org.mockito.Mockito.when(mockAiService.recordUnknownSyntax(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())
        ).thenThrow(new RuntimeException("Simulated Mongo write concern timeout / connection error"));

        AuditOrchestrationServiceImpl customOrch = new AuditOrchestrationServiceImpl(
                auditRepository,
                vendorDetectionService,
                parserService,
                normalizationService,
                ruleRepository,
                controlRepository,
                applicabilityChecker,
                ruleEvaluator,
                findingCreationService,
                riskCalculationService,
                mockAiService
        );

        String config = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services legacy-telnet active"
        );

        Audit audit = customOrch.startAudit("dev-fs-write-01", "cfg-fs-02", "v1.0", config);
        assertThat(audit).isNotNull();
        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());
        assertThat(audit.getSummary().getTotalControls()).isEqualTo(21);

        System.out.println("=== FAIL-SAFE AUDIT RESULT (AI MAPPINGS WRITE THROWS) ===");
        org.bson.Document rawAudit = mongoTemplate.getCollection("audits").find(new org.bson.Document("_id", audit.getId())).first();
        System.out.println(rawAudit != null ? rawAudit.toJson() : "null");
    }

    @Test
    @DisplayName("12. Edge Case: Audit twice -> exactly one pending doc in ai_mappings")
    void testEdgeCase_AuditTwiceYieldsOnePendingDoc() {
        String config = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services duplicate-unknown test"
        );

        // Run 1
        Audit audit1 = auditOrchestrationService.startAudit("dev-edge-01", "cfg-edge-01", "v1.0", config);
        assertThat(audit1.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());

        // Run 2 on same config
        Audit audit2 = auditOrchestrationService.startAudit("dev-edge-01", "cfg-edge-01", "v2.0", config);
        assertThat(audit2.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());

        List<AiMappingDocument> matches = aiMappingRepository.findAll().stream()
                .filter(m -> "set system services duplicate-unknown test".equals(m.getRawSyntax()))
                .toList();

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getStatus()).isEqualTo("PENDING_REVIEW");

        System.out.println("=== RAW PERSISTED SINGLE PENDING MAPPING AFTER TWO AUDITS ===");
        org.bson.Document rawDoc = mongoTemplate.getCollection("ai_mappings").find(new org.bson.Document("_id", matches.get(0).getId())).first();
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");
    }

    @Test
    @DisplayName("13. Edge Case: Reject then re-audit -> no new pending doc and line remains UNKNOWN")
    void testEdgeCase_RejectThenReauditYieldsNoNewPendingDocAndLineUnknown() {
        String config = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services rejected-unknown test"
        );

        // Audit 1: captures pending mapping
        auditOrchestrationService.startAudit("dev-edge-02", "cfg-edge-02", "v1.0", config);
        AiMappingDocument pending = aiMappingRepository.findByVendorAndPlatformAndRawSyntax(
                "Juniper", "JUNOS", "set system services rejected-unknown test"
        ).orElseThrow();

        // Reject mapping
        AiMappingDocument rejected = aiMappingService.reject(pending.getId(), "reviewer-sec-01", "Denied explicitly");
        assertThat(rejected.getStatus()).isEqualTo("REJECTED");

        // Audit 2: Re-audit on same config
        Audit audit2 = auditOrchestrationService.startAudit("dev-edge-02", "cfg-edge-02", "v2.0", config);
        assertThat(audit2.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());

        List<AiMappingDocument> matches = aiMappingRepository.findAll().stream()
                .filter(m -> "set system services rejected-unknown test".equals(m.getRawSyntax()))
                .toList();

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getStatus()).isEqualTo("REJECTED");

        // Line was not converted to findings or mapped facts
        List<FindingDocument> findings = findingRepository.findByDeviceId("dev-edge-02");
        assertThat(findings).isEmpty();

        System.out.println("=== RAW PERSISTED REJECTED MAPPING AFTER RE-AUDIT ===");
        org.bson.Document rawDoc = mongoTemplate.getCollection("ai_mappings").find(new org.bson.Document("_id", matches.get(0).getId())).first();
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");
    }

    @Test
    @DisplayName("14. Edge Case: Approve with no suggestion is refused")
    void testEdgeCase_ApproveWithNoSuggestionRefused() {
        AiMappingDocument pending = aiMappingService.recordUnknownSyntax("Juniper", "JUNOS", "set unanalyzed command");
        assertThat(pending.getCanonicalField()).isNull();
        assertThat(pending.getMappedValue()).isNull();

        assertThatThrownBy(() -> aiMappingService.approve(pending.getId(), "admin-01"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot approve mapping without valid proposed suggestion");
    }

    @Test
    @DisplayName("15. Edge Case: Approve after reject is refused")
    void testEdgeCase_ApproveAfterRejectRefused() {
        AiMappingDocument pending = aiMappingService.recordUnknownSyntax("Juniper", "JUNOS", "set rejected then approve");
        aiMappingService.requestSuggestion(pending.getId());
        aiMappingService.reject(pending.getId(), "admin-01", "Wrong directive");

        assertThatThrownBy(() -> aiMappingService.approve(pending.getId(), "admin-02"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot approve a rejected mapping");
    }

    @Test
    @DisplayName("16. Edge Case: Double approve is refused")
    void testEdgeCase_DoubleApproveRefused() {
        AiMappingDocument pending = aiMappingService.recordUnknownSyntax("Juniper", "JUNOS", "set system services legacy-telnet active");
        aiMappingService.requestSuggestion(pending.getId());
        aiMappingService.approve(pending.getId(), "admin-01");

        assertThatThrownBy(() -> aiMappingService.approve(pending.getId(), "admin-02"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mapping is already approved");
    }

    @Test
    @DisplayName("17. Orchestration Layer: One re-audit increments mapping usageCount by exactly 1")
    void testUsageCountIncrementedOncePerAuditOnReaudit() {
        AiMappingDocument pending = aiMappingService.recordUnknownSyntax("Juniper", "JUNOS", "set system services custom-telnet-flag on");
        aiMappingService.requestSuggestion(pending.getId());
        AiMappingDocument approved = aiMappingService.approve(pending.getId(), "sec-admin-01");
        assertThat(approved.getUsageCount()).isEqualTo(0);

        String config = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services custom-telnet-flag on"
        );

        // Audit 1
        auditOrchestrationService.startAudit("dev-usage-01", "cfg-usage-01", "v1.0", config);
        AiMappingDocument afterAudit1 = aiMappingRepository.findById(approved.getId()).orElseThrow();
        assertThat(afterAudit1.getUsageCount()).isEqualTo(1);

        // Re-audit (Audit 2)
        auditOrchestrationService.startAudit("dev-usage-01", "cfg-usage-01", "v2.0", config);
        AiMappingDocument afterAudit2 = aiMappingRepository.findById(approved.getId()).orElseThrow();
        assertThat(afterAudit2.getUsageCount()).isEqualTo(2);

        System.out.println("=== USAGE COUNT AFTER RE-AUDIT: INITIAL=0, AUDIT1=1, AUDIT2=2 ===");
        org.bson.Document rawDoc = mongoTemplate.getCollection("ai_mappings").find(new org.bson.Document("_id", approved.getId())).first();
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");
    }

    @Test
    @DisplayName("18. Parser Layer: Parse with read-only flag performs zero database writes")
    void testParserReadOnlyFlagPerformsNoDbWrites() {
        AiMappingDocument pending = aiMappingService.recordUnknownSyntax("Juniper", "JUNOS", "set system services read-only-telnet flag");
        aiMappingService.requestSuggestion(pending.getId());
        AiMappingDocument approved = aiMappingService.approve(pending.getId(), "sec-admin-01");
        assertThat(approved.getUsageCount()).isEqualTo(0);

        long mappingCountBefore = aiMappingRepository.count();
        long jobCountBefore = aiJobRepository.count();

        String rawConfig = String.join("\n",
                "set system services ssh protocol-version v2",
                "set system services read-only-telnet flag",
                "set system services new-unknown-in-readonly line"
        );

        // Call parser directly in read-only mode
        ParserResult result = parserService.parse(rawConfig, "Juniper", "JUNOS", true);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getAppliedMappingIds()).contains(approved.getId());

        // Zero database writes occurred
        assertThat(aiMappingRepository.count()).isEqualTo(mappingCountBefore);
        assertThat(aiJobRepository.count()).isEqualTo(jobCountBefore);
        AiMappingDocument docAfter = aiMappingRepository.findById(approved.getId()).orElseThrow();
        assertThat(docAfter.getUsageCount()).isEqualTo(0);

        System.out.println("=== PARSER READ-ONLY TEST: NO WRITES TO AI_MAPPINGS OR AI_JOBS, USAGE COUNT UNCHANGED ===");
        System.out.println("ai_mappings count: " + aiMappingRepository.count() + " (unchanged), usageCount: " + docAfter.getUsageCount());
    }
}

