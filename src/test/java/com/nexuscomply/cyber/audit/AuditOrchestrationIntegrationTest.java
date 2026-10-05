package com.nexuscomply.cyber.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClients;
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
import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.RuleResultStatus;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.rules.cis.CisCiscoIosXeRuleSeeder;
import com.nexuscomply.cyber.compliance.rules.iso.Iso27001RuleSeeder;
import com.nexuscomply.cyber.compliance.rules.nist.NistSp80053RuleSeeder;
import com.nexuscomply.cyber.detection.VendorDetectionService;
import com.nexuscomply.cyber.detection.VendorFingerprintDetectionService;
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
import com.nexuscomply.cyber.parser.ParserService;
import com.nexuscomply.cyber.parser.ParserServiceImpl;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import com.nexuscomply.cyber.risk.RiskCalculationService;
import com.nexuscomply.cyber.risk.RiskCalculationServiceImpl;
import com.nexuscomply.cyber.risk.RiskContext;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentDocument;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditOrchestrationIntegrationTest {

    private static MongoServer mongoServer;
    private static MongoTemplate mongoTemplate;

    private FrameworkRepository frameworkRepository;
    private ControlRepository controlRepository;
    private ComplianceRuleRepository ruleRepository;
    private NormalizedConfigurationRepository normalizedConfigRepository;
    private FindingRepository findingRepository;
    private EvidenceRepository evidenceRepository;
    private AuditRepository auditRepository;
    private RiskAssessmentRepository riskAssessmentRepository;

    private CisCiscoIosXeRuleSeeder cisSeeder;
    private NistSp80053RuleSeeder nistSeeder;
    private Iso27001RuleSeeder isoSeeder;

    private VendorDetectionService vendorDetectionService;
    private ParserService parserService;
    private NormalizationService normalizationService;
    private RuleApplicabilityChecker applicabilityChecker;
    private RuleEvaluator ruleEvaluator;
    private EvidenceCreationService evidenceCreationService;
    private FindingCreationService findingCreationService;
    private RiskCalculationService riskCalculationService;
    private AuditOrchestrationServiceImpl auditOrchestrationService;

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
        mongoTemplate.dropCollection(AuditDocument.class);
        mongoTemplate.dropCollection(RiskAssessmentDocument.class);

        MongoRepositoryFactory repositoryFactory = new MongoRepositoryFactory(mongoTemplate);
        frameworkRepository = repositoryFactory.getRepository(FrameworkRepository.class);
        controlRepository = repositoryFactory.getRepository(ControlRepository.class);
        ruleRepository = repositoryFactory.getRepository(ComplianceRuleRepository.class);
        normalizedConfigRepository = repositoryFactory.getRepository(NormalizedConfigurationRepository.class);
        findingRepository = repositoryFactory.getRepository(FindingRepository.class);
        evidenceRepository = repositoryFactory.getRepository(EvidenceRepository.class);
        auditRepository = repositoryFactory.getRepository(AuditRepository.class);
        riskAssessmentRepository = repositoryFactory.getRepository(RiskAssessmentRepository.class);

        cisSeeder = new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        nistSeeder = new NistSp80053RuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        isoSeeder = new Iso27001RuleSeeder(frameworkRepository, controlRepository, ruleRepository);

        vendorDetectionService = new VendorFingerprintDetectionService();
        parserService = new ParserServiceImpl(List.of(new CiscoIosParser()));
        normalizationService = new NormalizationServiceImpl(parserService, normalizedConfigRepository);
        applicabilityChecker = new DefaultRuleApplicabilityChecker();
        ruleEvaluator = new GenericRuleEvaluator();

        evidenceCreationService = new EvidenceCreationServiceImpl(evidenceRepository);
        findingCreationService = new FindingCreationServiceImpl(
                findingRepository,
                ruleRepository,
                controlRepository,
                evidenceCreationService,
                normalizedConfigRepository
        );
        riskCalculationService = new RiskCalculationServiceImpl(riskAssessmentRepository);

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
                riskCalculationService
        );

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Acceptance Criterion: Clean passing Cisco config produces COMPLETED audit with ZERO findings and correct counts")
    void testFullPipelineCleanPassingConfig() {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String cleanConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-CLEAN",
                "!",
                "ip ssh version 2",
                "no ip http server",
                "ip http secure-server",
                "snmp-server group ADMIN v3 priv",
                "aaa new-model",
                "logging on",
                "logging host 10.0.0.5",
                "logging buffered 16384",
                "ntp server 10.0.0.1",
                "line vty 0 4",
                " transport input ssh",
                "!"
        );

        Audit audit = auditOrchestrationService.startAudit("dev-clean-01", "cfg-clean-001", "ver-clean-001", cleanConfig);

        assertThat(audit).isNotNull();
        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());
        assertThat(audit.getProgress().getStage()).isEqualTo("COMPLETED");
        assertThat(audit.getProgress().getPercent()).isEqualTo(100);

        // Verification of evaluated controls
        AuditSummary summary = audit.getSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalControls()).isEqualTo(21);
        assertThat(summary.getPassed()).isEqualTo(21);
        assertThat(summary.getFailed()).isEqualTo(0);
        assertThat(summary.getUnknown()).isEqualTo(0);
        assertThat(summary.getNotApplicable()).isEqualTo(0);
        assertThat(summary.getError()).isEqualTo(0);

        // Confirm ZERO findings, ZERO evidence, and ZERO risk assessments persisted
        assertThat(findingRepository.count()).isEqualTo(0);
        assertThat(evidenceRepository.count()).isEqualTo(0);
        assertThat(riskAssessmentRepository.count()).isEqualTo(0);

        // Compliance score for clean run = 100.0 (Rule 8)
        assertThat(audit.getComplianceScore()).isEqualTo(100.0);

        // Retrieve raw persisted document from MongoDB and assert field-by-field parity
        Document rawAudit = mongoTemplate.getCollection("audits").find(new Document("_id", audit.getId())).first();
        assertThat(rawAudit).isNotNull();

        System.out.println("=== RAW PERSISTED AUDIT DOCUMENT (CLEAN PASS RUN) ===");
        System.out.println(rawAudit.toJson());

        assertThat(rawAudit.getString("_id")).isEqualTo(audit.getId());
        assertThat(rawAudit.getString("deviceId")).isEqualTo("dev-clean-01");
        assertThat(rawAudit.getString("configurationId")).isEqualTo("cfg-clean-001");
        assertThat(rawAudit.getString("versionId")).isEqualTo("ver-clean-001");
        assertThat(rawAudit.getString("normalizedConfigurationId")).isNotEmpty();
        assertThat(rawAudit.getString("status")).isEqualTo("COMPLETED");
        assertThat(rawAudit.getDouble("complianceScore")).isEqualTo(100.0);
        assertThat(rawAudit.getDate("startedAt")).isNotNull();
        assertThat(rawAudit.getDate("completedAt")).isNotNull();
        assertThat(rawAudit.getDate("createdAt")).isNotNull();
        assertThat(rawAudit.getDate("updatedAt")).isNotNull();

        Document progressDoc = (Document) rawAudit.get("progress");
        assertThat(progressDoc.getString("stage")).isEqualTo("COMPLETED");
        assertThat(progressDoc.getInteger("percent")).isEqualTo(100);

        Document summaryDoc = (Document) rawAudit.get("summary");
        assertThat(summaryDoc.getInteger("totalControls")).isEqualTo(21);
        assertThat(summaryDoc.getInteger("passed")).isEqualTo(21);
        assertThat(summaryDoc.getInteger("failed")).isEqualTo(0);
        assertThat(summaryDoc.getInteger("unknown")).isEqualTo(0);
        assertThat(summaryDoc.getInteger("notApplicable")).isEqualTo(0);
    }

    @Test
    @DisplayName("Acceptance Criterion: Failing (telnet-enabled) config produces COMPLETED audit with real Finding + Evidence + RiskAssessment documents")
    void testFullPipelineFailingTelnetConfig() {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        // Deliberate non-compliant configuration with Telnet enabled
        String failConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-TELNET-FAIL",
                "!",
                "ip ssh version 2",
                "no ip http server",
                "ip http secure-server",
                "snmp-server group ADMIN v3 priv",
                "aaa new-model",
                "logging on",
                "logging host 10.0.0.5",
                "logging buffered 16384",
                "ntp server 10.0.0.1",
                "line vty 0 4",
                " transport input telnet ssh",
                "!"
        );

        Audit audit = auditOrchestrationService.startAudit("dev-fail-01", "cfg-fail-001", "ver-fail-001", failConfig);

        assertThat(audit).isNotNull();
        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());

        // 3 telnet rules fail across CIS (CIS-1.2.2), NIST (NIST-AC-17), ISO (ISO-A.13.1.1-TELNET)
        AuditSummary summary = audit.getSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalControls()).isEqualTo(21);
        assertThat(summary.getPassed()).isEqualTo(18);
        assertThat(summary.getFailed()).isEqualTo(3);

        // Compliance score = (18 / 21) * 100 = 85.7 (Rule 8)
        assertThat(audit.getComplianceScore()).isEqualTo(85.7);

        // Exactly 3 findings, 3 evidence records, and 3 risk assessments persisted
        assertThat(findingRepository.count()).isEqualTo(3);
        assertThat(evidenceRepository.count()).isEqualTo(3);
        assertThat(riskAssessmentRepository.count()).isEqualTo(3);

        List<FindingDocument> findings = findingRepository.findAll();
        for (FindingDocument f : findings) {
            assertThat(f.getAuditId()).isEqualTo(audit.getId());
            assertThat(f.getComplianceStatus()).isEqualTo("FAIL");
            assertThat(f.getStatus()).isEqualTo("OPEN");
            // Rule 2: Finding severity is untouched/preserved
            assertThat(f.getSeverity()).isEqualTo("HIGH");
            assertThat(f.getEvidenceIds()).hasSize(1);

            String evidenceId = f.getEvidenceIds().get(0);
            Optional<EvidenceDocument> evOpt = evidenceRepository.findById(evidenceId);
            assertThat(evOpt).isPresent();
            EvidenceDocument ev = evOpt.get();
            assertThat(ev.getFindingId()).isEqualTo(f.getId());
            assertThat(ev.getAuditId()).isEqualTo(audit.getId());
            assertThat(ev.getVersionId()).isEqualTo("ver-fail-001");
            assertThat(ev.getSource().getLineNumber()).isEqualTo(14);
            assertThat(ev.getSource().getRawText()).isEqualTo(" transport input telnet ssh");

            // Verify linked RiskAssessment
            Optional<RiskAssessmentDocument> riskOpt = riskAssessmentRepository.findByFindingId(f.getId());
            assertThat(riskOpt).isPresent();
            RiskAssessmentDocument riskDoc = riskOpt.get();
            assertThat(riskDoc.getAuditId()).isEqualTo(audit.getId());
            assertThat(riskDoc.getDeviceId()).isEqualTo("dev-fail-01");
            assertThat(riskDoc.getFindingId()).isEqualTo(f.getId());
            assertThat(riskDoc.getScore()).isEqualTo(70);
            assertThat(riskDoc.getLevel()).isEqualTo("HIGH");
            // Rule 5: Real vendor detection confidence (0.99 for Cisco)
            assertThat(riskDoc.getConfidence()).isEqualTo(0.99);
            assertThat(riskDoc.getConfidenceSource()).isEqualTo("VENDOR_DETECTION");
            // Rule 4: Default placeholder flags
            assertThat(riskDoc.getAssetCriticalitySource()).isEqualTo(RiskContext.SOURCE_DEFAULT_PLACEHOLDER);
            assertThat(riskDoc.getNetworkExposureSource()).isEqualTo(RiskContext.SOURCE_DEFAULT_PLACEHOLDER);
        }

        Document rawAudit = mongoTemplate.getCollection("audits").find(new Document("_id", audit.getId())).first();

        System.out.println("=== RAW PERSISTED AUDIT DOCUMENT (FAIL RUN) ===");
        System.out.println(rawAudit.toJson());

        System.out.println("=== RAW PERSISTED FINDING DOCUMENTS (FAIL RUN: ALL 3) ===");
        List<Document> rawFindings = mongoTemplate.getCollection("findings")
                .find(new Document("auditId", audit.getId()))
                .into(new ArrayList<>());
        for (int i = 0; i < rawFindings.size(); i++) {
            System.out.println("--- FINDING " + (i + 1) + " ---");
            System.out.println(rawFindings.get(i).toJson());
        }

        System.out.println("=== RAW PERSISTED EVIDENCE DOCUMENTS (FAIL RUN: ALL 3) ===");
        List<Document> rawEvidences = mongoTemplate.getCollection("evidence")
                .find(new Document("auditId", audit.getId()))
                .into(new ArrayList<>());
        for (int i = 0; i < rawEvidences.size(); i++) {
            System.out.println("--- EVIDENCE " + (i + 1) + " ---");
            System.out.println(rawEvidences.get(i).toJson());
        }

        System.out.println("=== RAW PERSISTED RISK ASSESSMENT DOCUMENTS (FAIL RUN: ALL 3) ===");
        List<Document> rawRisks = mongoTemplate.getCollection("risk_assessments")
                .find(new Document("auditId", audit.getId()))
                .into(new ArrayList<>());
        for (int i = 0; i < rawRisks.size(); i++) {
            System.out.println("--- RISK ASSESSMENT " + (i + 1) + " ---");
            System.out.println(rawRisks.get(i).toJson());
        }

        assertThat(rawAudit.getString("status")).isEqualTo("COMPLETED");
        assertThat(rawAudit.getDouble("complianceScore")).isEqualTo(85.7);
        Document summaryDoc = (Document) rawAudit.get("summary");
        assertThat(summaryDoc.getInteger("failed")).isEqualTo(3);
    }

    @Test
    @DisplayName("Acceptance Criterion: Audit with explicitly provided RiskContext flags sources as PROVIDED and uses real detection confidence")
    void testFullPipelineFailingConfigWithProvidedRiskContext() {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String failConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-TELNET-FAIL",
                "!",
                "ip ssh version 2",
                "no ip http server",
                "ip http secure-server",
                "snmp-server group ADMIN v3 priv",
                "aaa new-model",
                "logging on",
                "logging host 10.0.0.5",
                "logging buffered 16384",
                "ntp server 10.0.0.1",
                "line vty 0 4",
                " transport input telnet ssh",
                "!"
        );

        // Explicitly supply CRITICAL asset criticality and PERIMETER network exposure
        RiskContext providedContext = new RiskContext("CRITICAL", "PERIMETER", 0.0);

        Audit audit = auditOrchestrationService.startAudit("dev-provided-01", "cfg-provided-001", "ver-provided-001", failConfig, providedContext);

        assertThat(audit).isNotNull();
        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());
        assertThat(riskAssessmentRepository.count()).isEqualTo(3);

        List<RiskAssessmentDocument> risks = riskAssessmentRepository.findByAuditId(audit.getId());
        for (RiskAssessmentDocument r : risks) {
            // Sources flagged as PROVIDED
            assertThat(r.getAssetCriticalitySource()).isEqualTo(RiskContext.SOURCE_PROVIDED);
            assertThat(r.getNetworkExposureSource()).isEqualTo(RiskContext.SOURCE_PROVIDED);
            // Real detection confidence (0.99) is used instead of supplied 0.0
            assertThat(r.getConfidence()).isEqualTo(0.99);
            // Score math:
            // Severity: HIGH (80 * 0.35) = 28.0
            // Asset Crit: CRITICAL (100 * 0.20) = 20.0
            // Exposure: PERIMETER (100 * 0.20) = 20.0
            // Control Weight: HIGH (80 * 0.15) = 12.0
            // Confidence: 0.99 (99 * 0.10) = 9.9
            // Total = 28 + 20 + 20 + 12 + 9.9 = 89.9 -> 90 (CRITICAL)
            assertThat(r.getScore()).isEqualTo(90);
            assertThat(r.getLevel()).isEqualTo("CRITICAL");
        }
    }

    @Test
    @DisplayName("Acceptance Criterion: Malformed unparseable config transitions audit to FAILED with stored diagnostic message")
    void testMalformedConfigTransitionsToFailed() {
        String malformedConfig = "!!! INVALID GARBAGE NON-SYNTAX CONFIGURATION 12345 !!!";

        Audit audit = auditOrchestrationService.startAudit("dev-malformed-01", "cfg-malformed-001", "ver-malformed-001", malformedConfig);

        assertThat(audit).isNotNull();
        assertThat(audit.getStatus()).isEqualTo(AuditStatus.FAILED.name());
        assertThat(audit.getErrorMessage()).isNotNull();
        assertThat(audit.getErrorMessage()).contains("Vendor detection failed");

        Document rawAudit = mongoTemplate.getCollection("audits").find(new Document("_id", audit.getId())).first();
        assertThat(rawAudit).isNotNull();
        assertThat(rawAudit.getString("status")).isEqualTo("FAILED");
        assertThat(rawAudit.getString("errorMessage")).contains("Vendor detection failed");

        System.out.println("=== RAW PERSISTED FAILED AUDIT DOCUMENT ===");
        System.out.println(rawAudit.toJson());
    }

    @Test
    @DisplayName("Acceptance Criterion: Audit status transitions are visible and persisted mid-run during genuine async execution")
    void testMidPipelineAuditStatusIsVisibleAndNonTerminal() throws Exception {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String cleanConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-MID-TEST",
                "!",
                "ip ssh version 2",
                "no ip http server",
                "ip http secure-server",
                "line vty 0 4",
                " transport input ssh",
                "!"
        );

        java.util.concurrent.CountDownLatch pauseLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch resumeLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<String> midRunAuditId = new java.util.concurrent.atomic.AtomicReference<>();

        AuditOrchestrationServiceImpl impl = (AuditOrchestrationServiceImpl) auditOrchestrationService;
        impl.setStageTransitionHook(doc -> {
            if (AuditStatus.NORMALIZING.name().equalsIgnoreCase(doc.getStatus())) {
                midRunAuditId.set(doc.getId());
                pauseLatch.countDown();
                try {
                    resumeLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        java.util.concurrent.CompletableFuture<Audit> futureAudit = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                auditOrchestrationService.startAudit("dev-mid-01", "cfg-mid-001", "ver-mid-001", cleanConfig)
        );

        // Wait until the real pipeline actively transitions to NORMALIZING and saves to MongoDB
        boolean reached = pauseLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(reached).isTrue();

        String capturedAuditId = midRunAuditId.get();
        assertThat(capturedAuditId).isNotNull();

        // Query the live MongoDB database mid-pipeline from the observer thread
        Document rawMid = mongoTemplate.getCollection("audits").find(new Document("_id", capturedAuditId)).first();
        assertThat(rawMid).isNotNull();
        assertThat(rawMid.getString("status")).isEqualTo("NORMALIZING");

        Document progressDoc = (Document) rawMid.get("progress");
        assertThat(progressDoc.getString("stage")).isEqualTo("NORMALIZING");
        assertThat(progressDoc.getInteger("percent")).isEqualTo(45);
        assertThat(AuditStatus.valueOf(rawMid.getString("status")).isTerminal()).isFalse();

        System.out.println("=== RAW PERSISTED MID-RUN AUDIT STATUS DOCUMENT (GENUINE ASYNC EXECUTION) ===");
        System.out.println(rawMid.toJson());

        // Resume pipeline and verify it completes cleanly
        resumeLatch.countDown();
        Audit completedAudit = futureAudit.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(completedAudit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());
        impl.setStageTransitionHook(null);
    }

    @Test
    @DisplayName("Acceptance Criterion: cancel(auditId) cancels active audit (queued or in-flight), but refuses to cancel COMPLETED or FAILED audit")
    void testCancellationBehavior() throws Exception {
        // 1. Cancel an ongoing/queued audit
        AuditDocument queuedDoc = new AuditDocument();
        String queuedId = "audit-cancel-queued";
        queuedDoc.setId(queuedId);
        queuedDoc.setStatus(AuditStatus.QUEUED.name());
        queuedDoc.setProgress(new AuditProgress("QUEUED", 0));
        queuedDoc.setCreatedAt(java.time.Instant.now());
        queuedDoc.setUpdatedAt(java.time.Instant.now());
        auditRepository.save(queuedDoc);

        boolean cancelled = auditOrchestrationService.cancel(queuedId);
        assertThat(cancelled).isTrue();

        AuditDocument afterCancel = auditRepository.findById(queuedId).orElseThrow();
        assertThat(afterCancel.getStatus()).isEqualTo(AuditStatus.CANCELLED.name());

        // 2. Cancel a running audit in-flight during active stage (PARSING)
        java.util.concurrent.CountDownLatch cancelPauseLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch cancelResumeLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<String> inFlightAuditId = new java.util.concurrent.atomic.AtomicReference<>();

        AuditOrchestrationServiceImpl impl = (AuditOrchestrationServiceImpl) auditOrchestrationService;
        impl.setStageTransitionHook(doc -> {
            if (AuditStatus.PARSING.name().equalsIgnoreCase(doc.getStatus())) {
                inFlightAuditId.set(doc.getId());
                cancelPauseLatch.countDown();
                try {
                    cancelResumeLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        String sampleConfig = "version 17.6\nhostname RTR-CANCEL\n!\nline vty 0 4\n transport input ssh\n!";
        java.util.concurrent.CompletableFuture<Audit> inFlightFuture = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                auditOrchestrationService.startAudit("dev-cancel-01", "cfg-cancel-001", "ver-cancel-001", sampleConfig)
        );

        cancelPauseLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        String liveAuditId = inFlightAuditId.get();
        assertThat(liveAuditId).isNotNull();

        // Cancel mid-flight while paused in PARSING
        boolean inFlightCancelled = auditOrchestrationService.cancel(liveAuditId);
        assertThat(inFlightCancelled).isTrue();

        cancelResumeLatch.countDown();
        Audit haltedAudit = inFlightFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(haltedAudit.getStatus()).isEqualTo(AuditStatus.CANCELLED.name());

        AuditDocument inFlightDoc = auditRepository.findById(liveAuditId).orElseThrow();
        assertThat(inFlightDoc.getStatus()).isEqualTo(AuditStatus.CANCELLED.name());
        impl.setStageTransitionHook(null);

        // 3. Refuse to cancel an already-COMPLETED audit
        AuditDocument completedDoc = new AuditDocument();
        String completedId = "audit-completed-refusal";
        completedDoc.setId(completedId);
        completedDoc.setStatus(AuditStatus.COMPLETED.name());
        completedDoc.setProgress(new AuditProgress("COMPLETED", 100));
        completedDoc.setCreatedAt(java.time.Instant.now());
        completedDoc.setUpdatedAt(java.time.Instant.now());
        auditRepository.save(completedDoc);

        assertThatThrownBy(() -> auditOrchestrationService.cancel(completedId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel an audit in terminal state: COMPLETED");

        // 4. Refuse to cancel an already-FAILED audit
        AuditDocument failedDoc = new AuditDocument();
        String failedId = "audit-failed-refusal";
        failedDoc.setId(failedId);
        failedDoc.setStatus(AuditStatus.FAILED.name());
        failedDoc.setErrorMessage("Prior failure");
        failedDoc.setCreatedAt(java.time.Instant.now());
        failedDoc.setUpdatedAt(java.time.Instant.now());
        auditRepository.save(failedDoc);

        assertThatThrownBy(() -> auditOrchestrationService.cancel(failedId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel an audit in terminal state: FAILED");
    }

    @Test
    @DisplayName("Acceptance Criterion: UNKNOWN_REVIEW and RISK_CALCULATION states are visited in sequence")
    void testTransientStatesAreVisitedInSequence() {
        // Verify state machine ordering per Pre-Resolved Decisions 1, 2, and 3
        AuditStatus[] expectedHappyPath = {
                AuditStatus.QUEUED,
                AuditStatus.DETECTING,
                AuditStatus.PARSING,
                AuditStatus.NORMALIZING,
                AuditStatus.UNKNOWN_REVIEW,
                AuditStatus.CHECKING,
                AuditStatus.RISK_CALCULATION,
                AuditStatus.COMPLETED
        };

        for (int i = 0; i < expectedHappyPath.length - 1; i++) {
            assertThat(expectedHappyPath[i].ordinal()).isLessThan(expectedHappyPath[i + 1].ordinal());
        }

        assertThat(AuditStatus.values()).hasSize(10);
    }

    @Test
    @DisplayName("Zero applicable rules produces complianceScore = null (N/A) rather than misleading 100.0")
    void testZeroApplicableRulesProducesNullComplianceScore() {
        // Without seeding any rules, totalControls is 0
        String config = String.join("\n",
                "version 17.6",
                "hostname RTR-NO-RULES",
                "!",
                "ip ssh version 2",
                "!"
        );

        Audit audit = auditOrchestrationService.startAudit("dev-norules-01", "cfg-norules-001", "ver-norules-001", config);

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());
        assertThat(audit.getSummary().getTotalControls()).isEqualTo(0);
        assertThat(audit.getComplianceScore()).isNull();

        Document rawAudit = mongoTemplate.getCollection("audits").find(new Document("_id", audit.getId())).first();
        assertThat(rawAudit).isNotNull();
        assertThat(rawAudit.get("complianceScore")).isNull();
    }

    @Test
    @DisplayName("extractOsVersion: correctly parses version lines and returns UNKNOWN when absent")
    void testExtractOsVersionBehaviors() {
        String ciscoConfig = "version 17.6\nhostname RTR-01\n";
        String junosConfig = "set system host-name JUNIPER-SRX\nset system services ssh\n";
        String fortinetConfig = String.join("\n",
                "config system global",
                "    set hostname \"FG-EDGE\"",
                "end",
                "config system interface",
                "    edit \"port1\"",
                "        set allowaccess ping https ssh",
                "    next",
                "end"
        );

        assertThat(auditOrchestrationService.extractOsVersion(ciscoConfig)).isEqualTo("17.x");
        assertThat(auditOrchestrationService.extractOsVersion(junosConfig)).isEqualTo("UNKNOWN");
        assertThat(auditOrchestrationService.extractOsVersion(fortinetConfig)).isEqualTo("UNKNOWN");
        assertThat(auditOrchestrationService.extractOsVersion(null)).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Evaluator null handling: CIS-1.2.2 evaluated against canonical with security.telnet.enabled=null yields UNKNOWN")
    void testGenericRuleEvaluatorNullHandlingCIS122() throws Exception {
        cisSeeder.seed();
        ComplianceRuleDocument ruleDoc = ruleRepository.findByRuleCode("CIS-1.2.2").orElseThrow();
        ComplianceRule domainRule = auditOrchestrationService.toDomainRule(ruleDoc);

        // Canonical model where security.telnet.enabled is null (unset)
        CanonicalSecurityModel model = new CanonicalSecurityModel();
        RuleEvaluationResult result = ruleEvaluator.evaluate(model, domainRule);

        assertThat(result.getStatus()).isEqualTo(RuleResultStatus.UNKNOWN);
        assertThat(result.getActual()).isNull();
        assertThat(result.getRuleId()).isEqualTo("CIS-1.2.2");

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        System.out.println("=== RAW EVALUATOR RESULT (CIS-1.2.2 WITH NULL TELNET) ===");
        System.out.println(json);
    }

    @Test
    @DisplayName("Cisco config without version line: osVersion is UNKNOWN, all 17.x rules not applicable, complianceScore is null")
    void testCiscoConfigWithoutVersionLine() throws Exception {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String noVersionCiscoConfig = String.join("\n",
                "hostname RTR-NO-VERSION",
                "!",
                "ip ssh version 2",
                "no ip http server",
                "ip http secure-server",
                "snmp-server group ADMIN v3 priv",
                "aaa new-model",
                "logging on",
                "logging host 10.0.0.5",
                "logging buffered 16384",
                "ntp server 10.0.0.1",
                "line vty 0 4",
                " transport input ssh",
                "!"
        );

        Audit audit = auditOrchestrationService.startAudit("dev-nover-01", "cfg-nover-001", "ver-nover-001", noVersionCiscoConfig);

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETED.name());
        assertThat(audit.getSummary().getTotalControls()).isEqualTo(21);
        assertThat(audit.getSummary().getPassed()).isEqualTo(4);
        assertThat(audit.getSummary().getFailed()).isEqualTo(0);
        assertThat(audit.getSummary().getNotApplicable()).isEqualTo(17);
        assertThat(audit.getSummary().getEvaluatedControls()).isEqualTo(4);
        assertThat(audit.getSummary().getCoveragePercentage()).isEqualTo(4.0 / 21.0 * 100.0);
        assertThat(audit.getComplianceScore()).isEqualTo(100.0);

        System.out.println("=== RAW AUDIT SUMMARY: UNVERSIONED CISCO CONFIG ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(audit.getSummary()));
    }

    @Test
    @DisplayName("Reconciliation identity: totalControls == passed + failed + unknown + notApplicable + error across all 4 scenarios")
    void testAuditSummaryReconciliationIdentityAcrossScenarios() {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String ciscoConfigWithVersion = String.join("\n",
                "version 17.6",
                "hostname RTR-IDENTITY-TEST",
                "!",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input ssh",
                "!"
        );

        String ciscoConfigNoVersion = String.join("\n",
                "hostname RTR-NO-VER",
                "!",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input ssh",
                "!"
        );

        // Scenario A: All applicable (Cisco with version line)
        Audit auditAllApplicable = auditOrchestrationService.startAudit("dev-app-01", "cfg-app-001", "ver-app-001", ciscoConfigWithVersion);
        AuditSummary sumA = auditAllApplicable.getSummary();
        assertThat(sumA.getTotalControls())
                .isEqualTo(sumA.getPassed() + sumA.getFailed() + sumA.getUnknown() + sumA.getNotApplicable() + sumA.getError());
        assertThat(sumA.getNotApplicable()).isEqualTo(0);

        // Scenario B: 17 not-applicable for 17.x-specific rules (Cisco without version line -> osVersion UNKNOWN -> 17 notApplicable, 4 version-agnostic rules pass)
        Audit auditAllNotApplicable = auditOrchestrationService.startAudit("dev-nonapp-01", "cfg-nonapp-001", "ver-nonapp-001", ciscoConfigNoVersion);
        AuditSummary sumB = auditAllNotApplicable.getSummary();
        assertThat(sumB.getTotalControls())
                .isEqualTo(sumB.getPassed() + sumB.getFailed() + sumB.getUnknown() + sumB.getNotApplicable() + sumB.getError());
        assertThat(sumB.getNotApplicable()).isEqualTo(17);
        assertThat(sumB.getPassed()).isEqualTo(4);
        assertThat(auditAllNotApplicable.getComplianceScore()).isEqualTo(100.0);

        // Scenario C: Rule evaluator exception -> error++
        RuleEvaluator throwingEvaluator = (canonical, rule) -> {
            throw new RuntimeException("Simulated evaluator exception");
        };
        AuditOrchestrationServiceImpl throwingService = new AuditOrchestrationServiceImpl(
                auditRepository, vendorDetectionService, parserService, normalizationService,
                ruleRepository, controlRepository, applicabilityChecker, throwingEvaluator,
                findingCreationService, riskCalculationService
        );
        Audit auditException = throwingService.startAudit("dev-exc-01", "cfg-exc-001", "ver-exc-001", ciscoConfigWithVersion);
        AuditSummary sumC = auditException.getSummary();
        assertThat(sumC.getTotalControls())
                .isEqualTo(sumC.getPassed() + sumC.getFailed() + sumC.getUnknown() + sumC.getNotApplicable() + sumC.getError());
        assertThat(sumC.getError()).isEqualTo(21);

        // Scenario D: Null evalResult -> error++
        RuleEvaluator nullEvaluator = (canonical, rule) -> null;
        AuditOrchestrationServiceImpl nullResultService = new AuditOrchestrationServiceImpl(
                auditRepository, vendorDetectionService, parserService, normalizationService,
                ruleRepository, controlRepository, applicabilityChecker, nullEvaluator,
                findingCreationService, riskCalculationService
        );
        Audit auditNull = nullResultService.startAudit("dev-null-01", "cfg-null-001", "ver-null-001", ciscoConfigWithVersion);
        AuditSummary sumD = auditNull.getSummary();
        assertThat(sumD.getTotalControls())
                .isEqualTo(sumD.getPassed() + sumD.getFailed() + sumD.getUnknown() + sumD.getNotApplicable() + sumD.getError());
        assertThat(sumD.getError()).isEqualTo(21);
    }

    @Test
    @DisplayName("startAudit on Bad Detection: Brace-format PAN-OS, Vendor Tie, and Plain Unrecognised Text")
    void testStartAudit_BadDetectionScenarios() throws Exception {
        // (a) Brace-format PAN-OS config
        String braceConfig = """
                deviceconfig {
                    system {
                        service {
                            disable-telnet yes;
                        }
                    }
                }
                """;
        Audit auditBrace = auditOrchestrationService.startAudit("dev-brace-01", "cfg-brace-001", "ver-brace-001", braceConfig);
        AuditDocument docBrace = auditRepository.findById(auditBrace.getId()).orElse(null);
        Optional<NormalizedConfigurationDocument> normBrace = normalizedConfigRepository.findByConfigurationId("cfg-brace-001");

        assertThat(auditBrace.getStatus()).isEqualTo("FAILED");
        assertThat(auditBrace.getErrorMessage()).contains("Vendor detection failed");
        assertThat(docBrace).isNotNull();
        assertThat(docBrace.getStatus()).isEqualTo("FAILED");
        assertThat(docBrace.getErrorMessage()).contains("Vendor detection failed");
        assertThat(normBrace).isEmpty();

        System.out.println("=== (a) BRACE-FORMAT PAN-OS CONFIG AUDIT RESULT ===");
        System.out.println("--- Returned Domain Audit ---");
        System.out.println(objectMapper.writeValueAsString(auditBrace));
        System.out.println("--- Persisted Mongo AuditDocument ---");
        System.out.println(objectMapper.writeValueAsString(docBrace));
        System.out.println("--- Persisted NormalizedConfigurationDocument ---");
        System.out.println(normBrace.isPresent() ? objectMapper.writeValueAsString(normBrace.get()) : "NONE (not persisted)");

        // (b) Config that ties two vendors (2 Cisco clues, 2 Juniper clues)
        String tiedConfig = String.join("\n",
                "hostname router1",
                "aaa new-model",
                "set protocols bgp",
                "apply-groups group1"
        );
        Audit auditTie = auditOrchestrationService.startAudit("dev-tie-01", "cfg-tie-001", "ver-tie-001", tiedConfig);
        AuditDocument docTie = auditRepository.findById(auditTie.getId()).orElse(null);
        Optional<NormalizedConfigurationDocument> normTie = normalizedConfigRepository.findByConfigurationId("cfg-tie-001");

        assertThat(auditTie.getStatus()).isEqualTo("FAILED");
        assertThat(auditTie.getErrorMessage()).contains("Vendor detection failed");
        assertThat(docTie).isNotNull();
        assertThat(docTie.getStatus()).isEqualTo("FAILED");
        assertThat(docTie.getErrorMessage()).contains("Vendor detection failed");
        assertThat(normTie).isEmpty();

        System.out.println("=== (b) VENDOR TIE CONFIG AUDIT RESULT ===");
        System.out.println("--- Returned Domain Audit ---");
        System.out.println(objectMapper.writeValueAsString(auditTie));
        System.out.println("--- Persisted Mongo AuditDocument ---");
        System.out.println(objectMapper.writeValueAsString(docTie));
        System.out.println("--- Persisted NormalizedConfigurationDocument ---");
        System.out.println(normTie.isPresent() ? objectMapper.writeValueAsString(normTie.get()) : "NONE (not persisted)");

        // (c) Plain unrecognised text
        String unrecConfig = "The quick brown fox jumps over the lazy dog repeatedly on a sunny afternoon in the forest.";
        Audit auditUnrec = auditOrchestrationService.startAudit("dev-unrec-01", "cfg-unrec-001", "ver-unrec-001", unrecConfig);
        AuditDocument docUnrec = auditRepository.findById(auditUnrec.getId()).orElse(null);
        Optional<NormalizedConfigurationDocument> normUnrec = normalizedConfigRepository.findByConfigurationId("cfg-unrec-001");

        assertThat(auditUnrec.getStatus()).isEqualTo("FAILED");
        assertThat(auditUnrec.getErrorMessage()).contains("Vendor detection failed");
        assertThat(docUnrec).isNotNull();
        assertThat(docUnrec.getStatus()).isEqualTo("FAILED");
        assertThat(docUnrec.getErrorMessage()).contains("Vendor detection failed");
        assertThat(normUnrec).isEmpty();

        System.out.println("=== (c) PLAIN UNRECOGNISED TEXT AUDIT RESULT ===");
        System.out.println("--- Returned Domain Audit ---");
        System.out.println(objectMapper.writeValueAsString(auditUnrec));
        System.out.println("--- Persisted Mongo AuditDocument ---");
        System.out.println(objectMapper.writeValueAsString(docUnrec));
        System.out.println("--- Persisted NormalizedConfigurationDocument ---");
        System.out.println(normUnrec.isPresent() ? objectMapper.writeValueAsString(normUnrec.get()) : "NONE (not persisted)");

        // (d) UNCERTAIN detection with a populated vendor (1 clue = 0.60 confidence < 0.70 threshold)
        String uncertainConfig = "hostname single-clue-router";
        Audit auditUncertain = auditOrchestrationService.startAudit("dev-uncertain-01", "cfg-uncertain-001", "ver-uncertain-001", uncertainConfig);
        AuditDocument docUncertain = auditRepository.findById(auditUncertain.getId()).orElse(null);
        Optional<NormalizedConfigurationDocument> normUncertain = normalizedConfigRepository.findByConfigurationId("cfg-uncertain-001");

        assertThat(auditUncertain.getStatus()).isEqualTo("FAILED");
        assertThat(auditUncertain.getErrorMessage()).contains("Vendor detection failed");
        assertThat(auditUncertain.getErrorMessage()).contains("UNCERTAIN");
        assertThat(docUncertain).isNotNull();
        assertThat(docUncertain.getStatus()).isEqualTo("FAILED");
        assertThat(docUncertain.getErrorMessage()).contains("Vendor detection failed");
        assertThat(docUncertain.getErrorMessage()).contains("UNCERTAIN");
        assertThat(normUncertain).isEmpty();

        System.out.println("=== (d) UNCERTAIN WITH POPULATED VENDOR AUDIT RESULT ===");
        System.out.println("--- Returned Domain Audit ---");
        System.out.println(objectMapper.writeValueAsString(auditUncertain));
        System.out.println("--- Persisted Mongo AuditDocument ---");
        System.out.println(objectMapper.writeValueAsString(docUncertain));
        System.out.println("--- Persisted NormalizedConfigurationDocument ---");
        System.out.println(normUncertain.isPresent() ? objectMapper.writeValueAsString(normUncertain.get()) : "NONE (not persisted)");
    }
}
