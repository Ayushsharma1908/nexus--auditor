package com.nexuscomply.cyber.drift;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClients;
import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.audit.Audit;
import com.nexuscomply.cyber.audit.AuditOrchestrationServiceImpl;
import com.nexuscomply.cyber.audit.persistence.AuditDocument;
import com.nexuscomply.cyber.audit.persistence.AuditRepository;
import com.nexuscomply.cyber.compliance.DefaultRuleApplicabilityChecker;
import com.nexuscomply.cyber.compliance.GenericRuleEvaluator;
import com.nexuscomply.cyber.compliance.RuleApplicabilityChecker;
import com.nexuscomply.cyber.compliance.RuleEvaluator;
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
import com.nexuscomply.cyber.detection.VendorDetectionService;
import com.nexuscomply.cyber.detection.VendorFingerprintDetectionService;
import com.nexuscomply.cyber.drift.model.DriftChange;
import com.nexuscomply.cyber.drift.model.DriftClassification;
import com.nexuscomply.cyber.drift.model.DriftEvent;
import com.nexuscomply.cyber.drift.persistence.DriftEventDocument;
import com.nexuscomply.cyber.drift.persistence.DriftEventRepository;
import com.nexuscomply.cyber.drift.service.DriftDetectionService;
import com.nexuscomply.cyber.drift.service.DriftDetectionServiceImpl;
import com.nexuscomply.cyber.evidence.EvidenceCreationService;
import com.nexuscomply.cyber.evidence.EvidenceCreationServiceImpl;
import com.nexuscomply.cyber.evidence.persistence.EvidenceDocument;
import com.nexuscomply.cyber.evidence.persistence.EvidenceRepository;
import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.finding.FindingCreationService;
import com.nexuscomply.cyber.finding.FindingCreationServiceImpl;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.risk.RiskAssessment;
import com.nexuscomply.cyber.risk.RiskContext;
import com.nexuscomply.cyber.normalization.NormalizationService;
import com.nexuscomply.cyber.normalization.NormalizationServiceImpl;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.parser.UnknownConstruct;
import com.nexuscomply.cyber.parser.ParserService;
import com.nexuscomply.cyber.parser.ParserServiceImpl;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import com.nexuscomply.cyber.risk.RiskCalculationService;
import com.nexuscomply.cyber.risk.RiskCalculationServiceImpl;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriftDetectionIntegrationTest {

    private static MongoServer mongoServer;
    private static MongoTemplate mongoTemplate;

    private FrameworkRepository frameworkRepository;
    private ControlRepository controlRepository;
    private ComplianceRuleRepository ruleRepository;
    private NormalizedConfigurationRepository normalizedConfigRepository;
    private FindingRepository findingRepository;
    private RiskAssessmentRepository riskAssessmentRepository;
    private DriftEventRepository driftEventRepository;

    private CisCiscoIosXeRuleSeeder cisSeeder;
    private NistSp80053RuleSeeder nistSeeder;
    private Iso27001RuleSeeder isoSeeder;
    private ParserService parserService;
    private NormalizationService normalizationService;
    private RiskCalculationService riskCalculationService;
    private DriftDetectionService driftDetectionService;

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

    private AuditRepository auditRepository;
    private EvidenceRepository evidenceRepository;
    private AuditOrchestrationServiceImpl auditOrchestrationService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(FrameworkDocument.class);
        mongoTemplate.dropCollection(ControlDocument.class);
        mongoTemplate.dropCollection(ComplianceRuleDocument.class);
        mongoTemplate.dropCollection(NormalizedConfigurationDocument.class);
        mongoTemplate.dropCollection(FindingDocument.class);
        mongoTemplate.dropCollection(RiskAssessmentDocument.class);
        mongoTemplate.dropCollection(DriftEventDocument.class);
        mongoTemplate.dropCollection(AuditDocument.class);
        mongoTemplate.dropCollection(EvidenceDocument.class);

        MongoRepositoryFactory repositoryFactory = new MongoRepositoryFactory(mongoTemplate);
        frameworkRepository = repositoryFactory.getRepository(FrameworkRepository.class);
        controlRepository = repositoryFactory.getRepository(ControlRepository.class);
        ruleRepository = repositoryFactory.getRepository(ComplianceRuleRepository.class);
        normalizedConfigRepository = repositoryFactory.getRepository(NormalizedConfigurationRepository.class);
        findingRepository = repositoryFactory.getRepository(FindingRepository.class);
        riskAssessmentRepository = repositoryFactory.getRepository(RiskAssessmentRepository.class);
        driftEventRepository = repositoryFactory.getRepository(DriftEventRepository.class);
        auditRepository = repositoryFactory.getRepository(AuditRepository.class);
        evidenceRepository = repositoryFactory.getRepository(EvidenceRepository.class);

        cisSeeder = new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        nistSeeder = new NistSp80053RuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        isoSeeder = new Iso27001RuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        parserService = new ParserServiceImpl(List.of(new CiscoIosParser()));
        normalizationService = new NormalizationServiceImpl(parserService, normalizedConfigRepository);
        riskCalculationService = new RiskCalculationServiceImpl(riskAssessmentRepository);

        VendorDetectionService vendorDetectionService = new VendorFingerprintDetectionService();
        RuleApplicabilityChecker applicabilityChecker = new DefaultRuleApplicabilityChecker();
        RuleEvaluator ruleEvaluator = new GenericRuleEvaluator();
        EvidenceCreationService evidenceCreationService = new EvidenceCreationServiceImpl(evidenceRepository);
        FindingCreationService findingCreationService = new FindingCreationServiceImpl(
                findingRepository, ruleRepository, controlRepository, evidenceCreationService, normalizedConfigRepository
        );
        auditOrchestrationService = new AuditOrchestrationServiceImpl(
                auditRepository, vendorDetectionService, parserService, normalizationService,
                ruleRepository, controlRepository, applicabilityChecker, ruleEvaluator,
                findingCreationService, riskCalculationService
        );

        driftDetectionService = new DriftDetectionServiceImpl(
                driftEventRepository,
                normalizedConfigRepository,
                ruleRepository,
                findingRepository,
                riskCalculationService
        );

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Persisted DriftEvent document conforms field-by-field to schema1.md Section 13")
    void testRawDocumentFieldParityWithSchema1() {
        cisSeeder.seed();

        NormalizedConfigurationDocument docA = createDoc("dev-field-parity", "cfg-1", "ver-1", "17.6", true, "transport input telnet");
        NormalizedConfigurationDocument docB = createDoc("dev-field-parity", "cfg-2", "ver-2", "17.6", false, "transport input ssh");

        DriftEvent event = driftDetectionService.detectDrift(docA, docB);
        assertThat(event).isNotNull();

        Document rawDoc = mongoTemplate.getCollection("drift_events").find().first();
        assertThat(rawDoc).isNotNull();

        // Field-by-field parity check against schema1.md Section 13
        assertThat(rawDoc.containsKey("_id")).isTrue();
        assertThat(rawDoc.containsKey("deviceId")).isTrue();
        assertThat(rawDoc.containsKey("fromVersionId")).isTrue();
        assertThat(rawDoc.containsKey("toVersionId")).isTrue();
        assertThat(rawDoc.containsKey("fromVersion")).isTrue();
        assertThat(rawDoc.containsKey("toVersion")).isTrue();
        assertThat(rawDoc.containsKey("changes")).isTrue();
        assertThat(rawDoc.containsKey("affectedControlIds")).isTrue();
        assertThat(rawDoc.containsKey("affectedFindingIds")).isTrue();
        assertThat(rawDoc.containsKey("riskBefore")).isTrue();
        assertThat(rawDoc.containsKey("riskAfter")).isTrue();
        assertThat(rawDoc.containsKey("impact")).isTrue();
        assertThat(rawDoc.containsKey("detectedAt")).isTrue();
        assertThat(rawDoc.containsKey("createdAt")).isTrue();
        assertThat(rawDoc.containsKey("updatedAt")).isTrue();

        List<Document> rawChanges = rawDoc.getList("changes", Document.class);
        assertThat(rawChanges).isNotEmpty();
        Document rawChange = rawChanges.get(0);
        assertThat(rawChange.containsKey("canonicalField")).isTrue();
        assertThat(rawChange.containsKey("before")).isTrue();
        assertThat(rawChange.containsKey("after")).isTrue();
        assertThat(rawChange.containsKey("changeType")).isTrue();
        assertThat(rawChange.containsKey("sourceBefore")).isTrue();
        assertThat(rawChange.containsKey("sourceAfter")).isTrue();

        System.out.println("=== RAW PERSISTED DRIFT EVENT (schema1.md Sec 13) ===");
        System.out.println(rawDoc.toJson());
    }

    @Test
    @DisplayName("Absolute Rule 2: Cosmetic-only changes (unknowns differing, canonical identical) produce zero drift changes")
    void testCosmeticNoiseFiltering_UnknownsDifferingProducesZeroChanges() {
        NormalizedConfigurationDocument docA = new NormalizedConfigurationDocument();
        docA.setId("norm-noise-1");
        docA.setDeviceId("dev-noise-01");
        docA.setConfigurationId("cfg-noise-1");
        docA.setVersionId("ver-noise-1");
        docA.setVendor("Cisco");
        docA.setPlatform("IOS-XE");
        CanonicalSecurityModel canonA = new CanonicalSecurityModel();
        canonA.setTelnet(false);
        canonA.setSsh(true, 2);
        docA.setCanonical(canonA);
        docA.setUnknowns(List.of(new UnknownConstruct("banner motd # Authorized Access Only #", 1, "banner text")));
        normalizedConfigRepository.save(docA);

        NormalizedConfigurationDocument docB = new NormalizedConfigurationDocument();
        docB.setId("norm-noise-2");
        docB.setDeviceId("dev-noise-01");
        docB.setConfigurationId("cfg-noise-2");
        docB.setVersionId("ver-noise-2");
        docB.setVendor("Cisco");
        docB.setPlatform("IOS-XE");
        CanonicalSecurityModel canonB = new CanonicalSecurityModel();
        canonB.setTelnet(false);
        canonB.setSsh(true, 2);
        docB.setCanonical(canonB);
        docB.setUnknowns(List.of(new UnknownConstruct("! Modified maintenance comments 2026-10-03", 2, "comment line")));
        normalizedConfigRepository.save(docB);

        DriftEvent event = driftDetectionService.detectDrift(docA, docB);
        assertThat(event).isNotNull();
        assertThat(event.getChanges()).isEmpty();
        assertThat(event.getAffectedControlIds()).isEmpty();
        assertThat(event.getRiskBefore()).isEqualTo(0);
        assertThat(event.getRiskAfter()).isEqualTo(0);
        assertThat(event.getImpact()).isEqualTo("NO_CHANGE");
    }

    @Test
    @DisplayName("End-to-end Cisco telnet remediation drift: IMPROVED classification and real risk delta computation")
    void testCiscoTelnetDrift_ImprovedClassificationAndRealRiskDelta() {
        cisSeeder.seed();

        String deviceId = "dev-cisco-drift-01";

        // Version 1 (Before): Telnet enabled (violating CIS-1.2.2)
        String configBefore = String.join("\n",
                "hostname cisco-edge-01",
                "version 17.6",
                "aaa new-model",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input telnet",
                "logging buffered 16384",
                "ntp server 10.0.0.1"
        );
        NormalizedConfigurationDocument docBefore = normalizationService.normalizeAndPersist(
                configBefore, deviceId, "cfg-v1", "ver-1", "Cisco", "IOS-XE", "17.6"
        );

        // Version 2 (After): Telnet disabled, SSH enforced (compliant with CIS-1.2.2)
        String configAfter = String.join("\n",
                "hostname cisco-edge-01",
                "version 17.6",
                "aaa new-model",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input ssh",
                "logging buffered 16384",
                "ntp server 10.0.0.1"
        );
        NormalizedConfigurationDocument docAfter = normalizationService.normalizeAndPersist(
                configAfter, deviceId, "cfg-v2", "ver-2", "Cisco", "IOS-XE", "17.6"
        );

        DriftEvent driftEvent = driftDetectionService.detectDrift(docBefore, docAfter);

        assertThat(driftEvent).isNotNull();
        assertThat(driftEvent.getDeviceId()).isEqualTo(deviceId);
        assertThat(driftEvent.getFromVersionId()).isEqualTo("ver-1");
        assertThat(driftEvent.getToVersionId()).isEqualTo("ver-2");

        // Verify detected change on security.telnet.enabled
        assertThat(driftEvent.getChanges()).hasSize(1);
        DriftChange change = driftEvent.getChanges().get(0);
        assertThat(change.getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(change.getBefore()).isEqualTo(true);
        assertThat(change.getAfter()).isEqualTo(false);
        assertThat(change.getChangeType()).isEqualTo("MODIFIED");
        assertThat(change.getSourceBefore()).isEqualTo(" transport input telnet");
        assertThat(change.getSourceAfter()).isEqualTo(" transport input ssh");
        assertThat(change.getClassification()).isEqualTo(DriftClassification.IMPROVED.name());

        // Verify affected controls
        assertThat(driftEvent.getAffectedControlIds()).isNotEmpty();

        // Verify real risk delta
        assertThat(driftEvent.getRiskBefore()).isGreaterThan(0);
        assertThat(driftEvent.getRiskAfter()).isEqualTo(0);
        assertThat(driftEvent.getImpact()).isEqualTo("DECREASED");

        System.out.println("=== REAL RISK DELTA CALCULATION ===");
        System.out.printf("Before (telnet=true, CIS-1.2.2 violated): Risk Score = %d%n", driftEvent.getRiskBefore());
        System.out.printf("After (telnet=false, CIS-1.2.2 satisfied): Risk Score = %d%n", driftEvent.getRiskAfter());
        System.out.printf("Risk Delta: %d -> %d (Impact = %s)%n", driftEvent.getRiskBefore(), driftEvent.getRiskAfter(), driftEvent.getImpact());

        Document rawDoc = mongoTemplate.getCollection("drift_events").find(new Document("deviceId", deviceId)).first();
        System.out.println("=== MONGO QUERY PERSISTED DRIFT EVENT (REAL TELNET TEST) ===");
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");

        ComplianceRuleDocument cisRuleDoc = ruleRepository.findByRuleCode("CIS-1.2.2").orElseThrow();
        Finding beforeFinding = new Finding();
        beforeFinding.setId("drift-eval-before-sample");
        beforeFinding.setDeviceId(deviceId);
        beforeFinding.setConfigurationId("cfg-v1");
        beforeFinding.setControlId(cisRuleDoc.getControlId());
        beforeFinding.setRuleId(cisRuleDoc.getId());
        beforeFinding.setControlCode(cisRuleDoc.getRuleCode());
        beforeFinding.setCanonicalField("security.telnet.enabled");
        beforeFinding.setSeverity(cisRuleDoc.getSeverity());
        beforeFinding.setExpected(false);
        beforeFinding.setActual(true);

        ComplianceRule domainRule = new ComplianceRule();
        domainRule.setId(cisRuleDoc.getId());
        domainRule.setControlId(cisRuleDoc.getControlId());
        domainRule.setRuleCode(cisRuleDoc.getRuleCode());
        domainRule.setRequirement(cisRuleDoc.getExpression());
        domainRule.setSeverity(cisRuleDoc.getSeverity());
        domainRule.setApplicableVendors(cisRuleDoc.getApplicableVendors());
        domainRule.setApplicablePlatforms(cisRuleDoc.getApplicablePlatforms());

        RiskAssessment beforeAssessment = riskCalculationService.calculateRisk(beforeFinding, domainRule, new RiskContext(1.0));
        System.out.println("=== BEFORE STATE RISK ASSESSMENT OBJECT ===");
        try {
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(beforeAssessment));
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("A3: Telnet drift with CIS + NIST + ISO all seeded")
    void testCiscoTelnetDrift_WithCisNistIsoSeeded() throws Exception {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String deviceId = "dev-cisco-drift-all-frameworks";
        String configBefore = String.join("\n",
                "hostname cisco-edge-01",
                "version 17.6",
                "aaa new-model",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input telnet",
                "logging buffered 16384",
                "ntp server 10.0.0.1"
        );
        NormalizedConfigurationDocument docBefore = normalizationService.normalizeAndPersist(
                configBefore, deviceId, "cfg-v1-all", "ver-1", "Cisco", "IOS-XE", "17.6"
        );

        String configAfter = String.join("\n",
                "hostname cisco-edge-01",
                "version 17.6",
                "aaa new-model",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input ssh",
                "logging buffered 16384",
                "ntp server 10.0.0.1"
        );
        NormalizedConfigurationDocument docAfter = normalizationService.normalizeAndPersist(
                configAfter, deviceId, "cfg-v2-all", "ver-2", "Cisco", "IOS-XE", "17.6"
        );

        DriftEvent driftEvent = driftDetectionService.detectDrift(docBefore, docAfter);
        Document rawDoc = mongoTemplate.getCollection("drift_events").find(new Document("deviceId", deviceId)).first();
        System.out.println("=== CIS + NIST + ISO SEEDED PERSISTED DRIFT EVENT ===");
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");
        System.out.println("Affected Control IDs: " + driftEvent.getAffectedControlIds());
        System.out.println("Risk Before: " + driftEvent.getRiskBefore());
        System.out.println("Risk After: " + driftEvent.getRiskAfter());

        assertThat(driftEvent.getAffectedControlIds()).hasSize(3);
        assertThat(driftEvent.getRiskBefore()).isEqualTo(70); // max over matching CIS, NIST, ISO rules (not 210)
        assertThat(driftEvent.getRiskAfter()).isEqualTo(0);
        assertThat(driftEvent.getImpact()).isEqualTo("DECREASED");
    }

    @Test
    @DisplayName("Negative direction: Posture degradation when clean config is modified to enable telnet")
    void testCiscoTelnetDrift_DegradedClassificationAndRiskIncrease() {
        cisSeeder.seed();

        String deviceId = "dev-cisco-drift-02";

        NormalizedConfigurationDocument docBefore = createDoc(deviceId, "cfg-clean", "ver-10", "17.6", false, "transport input ssh");
        NormalizedConfigurationDocument docAfter = createDoc(deviceId, "cfg-dirty", "ver-11", "17.6", true, "transport input telnet ssh");

        DriftEvent driftEvent = driftDetectionService.detectDrift(docBefore, docAfter);

        assertThat(driftEvent.getChanges()).hasSize(1);
        DriftChange change = driftEvent.getChanges().get(0);
        assertThat(change.getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(change.getBefore()).isEqualTo(false);
        assertThat(change.getAfter()).isEqualTo(true);
        assertThat(change.getClassification()).isEqualTo(DriftClassification.DEGRADED.name());

        assertThat(driftEvent.getRiskBefore()).isEqualTo(0);
        assertThat(driftEvent.getRiskAfter()).isGreaterThan(0);
        assertThat(driftEvent.getImpact()).isEqualTo("INCREASED");
    }

    @Test
    @DisplayName("Absolute Rule 5: A field change with no applicable rule is classified UNKNOWN_IMPACT without guessing")
    void testFieldChangeWithNoApplicableRule_ClassifiedAsUnknownImpact() {
        cisSeeder.seed(); // Only seeds rules for 7 fields (security.https has NO rule)

        String deviceId = "dev-cisco-unknown-impact";

        NormalizedConfigurationDocument docA = new NormalizedConfigurationDocument();
        docA.setId("doc-https-1");
        docA.setDeviceId(deviceId);
        docA.setConfigurationId("cfg-h-1");
        docA.setVersionId("ver-h-1");
        docA.setVendor("Cisco");
        docA.setPlatform("IOS-XE");
        CanonicalSecurityModel canonA = new CanonicalSecurityModel();
        canonA.setHttps(false);
        docA.setCanonical(canonA);
        normalizedConfigRepository.save(docA);

        NormalizedConfigurationDocument docB = new NormalizedConfigurationDocument();
        docB.setId("doc-https-2");
        docB.setDeviceId(deviceId);
        docB.setConfigurationId("cfg-h-2");
        docB.setVersionId("ver-h-2");
        docB.setVendor("Cisco");
        docB.setPlatform("IOS-XE");
        CanonicalSecurityModel canonB = new CanonicalSecurityModel();
        canonB.setHttps(true);
        docB.setCanonical(canonB);
        normalizedConfigRepository.save(docB);

        DriftEvent driftEvent = driftDetectionService.detectDrift(docA, docB);

        assertThat(driftEvent.getChanges()).hasSize(1);
        DriftChange change = driftEvent.getChanges().get(0);
        assertThat(change.getCanonicalField()).isEqualTo("security.https.enabled");
        assertThat(change.getBefore()).isEqualTo(false);
        assertThat(change.getAfter()).isEqualTo(true);
        assertThat(change.getClassification()).isEqualTo(DriftClassification.UNKNOWN_IMPACT.name());

        assertThat(driftEvent.getRiskBefore()).isEqualTo(0);
        assertThat(driftEvent.getRiskAfter()).isEqualTo(0);
        assertThat(driftEvent.getImpact()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Absolute Rule 6: Read-only integrity -- neither input NormalizedConfigurationDocument is mutated")
    void testReadonlyIntegrity_NeitherInputDocumentIsMutated() {
        cisSeeder.seed();

        String deviceId = "dev-cisco-readonly";
        NormalizedConfigurationDocument docBefore = createDoc(deviceId, "cfg-r1", "ver-r1", "17.6", true, "transport input telnet");
        NormalizedConfigurationDocument docAfter = createDoc(deviceId, "cfg-r2", "ver-r2", "17.6", false, "transport input ssh");

        Instant beforeUpdatedAt = docBefore.getUpdatedAt();
        Instant afterUpdatedAt = docAfter.getUpdatedAt();
        Map<String, Object> beforeTelnet = (Map<String, Object>) docBefore.getCanonical().getSecurity().get("telnet");
        int beforeSourceMapSize = docBefore.getSourceMap().size();

        driftDetectionService.detectDrift(docBefore, docAfter);

        // Re-read both from MongoDB and prove unchanged
        NormalizedConfigurationDocument freshBefore = normalizedConfigRepository.findById(docBefore.getId()).orElseThrow();
        NormalizedConfigurationDocument freshAfter = normalizedConfigRepository.findById(docAfter.getId()).orElseThrow();

        assertThat(freshBefore.getUpdatedAt().toEpochMilli()).isEqualTo(beforeUpdatedAt.toEpochMilli());
        assertThat(freshAfter.getUpdatedAt().toEpochMilli()).isEqualTo(afterUpdatedAt.toEpochMilli());
        assertThat(freshBefore.getSourceMap()).hasSize(beforeSourceMapSize);
        assertThat(freshBefore.getCanonical().getSecurity()).isEqualTo(docBefore.getCanonical().getSecurity());
        assertThat(freshAfter.getCanonical().getSecurity()).isEqualTo(docAfter.getCanonical().getSecurity());
    }

    @Test
    @DisplayName("Absolute Rule 1 validation: Rejects cross-device comparison or null inputs")
    void testValidation_RejectsCrossDeviceOrNullInputs() {
        NormalizedConfigurationDocument docDev1 = createDoc("dev-01", "cfg-1", "ver-1", "17.6", true, "transport input telnet");
        NormalizedConfigurationDocument docDev2 = createDoc("dev-02", "cfg-2", "ver-2", "17.6", false, "transport input ssh");

        assertThatThrownBy(() -> driftDetectionService.detectDrift(null, docDev2))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> driftDetectionService.detectDrift(docDev1, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> driftDetectionService.detectDrift(docDev1, docDev2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same deviceId");
    }

    @Test
    @DisplayName("A5.1: Drift on telnet null -> true (ADDED changeType, violation persists)")
    void testTelnetDrift_NullToTrue() {
        cisSeeder.seed();

        String deviceId = "dev-drift-null-to-true";
        NormalizedConfigurationDocument docA = new NormalizedConfigurationDocument();
        docA.setId("norm-null-1");
        docA.setDeviceId(deviceId);
        docA.setConfigurationId("cfg-null-1");
        docA.setVersionId("ver-1");
        docA.setVendor("Cisco");
        docA.setPlatform("IOS-XE");
        CanonicalSecurityModel canonA = new CanonicalSecurityModel();
        // telnet left null
        docA.setCanonical(canonA);
        normalizedConfigRepository.save(docA);

        NormalizedConfigurationDocument docB = new NormalizedConfigurationDocument();
        docB.setId("norm-null-2");
        docB.setDeviceId(deviceId);
        docB.setConfigurationId("cfg-null-2");
        docB.setVersionId("ver-2");
        docB.setVendor("Cisco");
        docB.setPlatform("IOS-XE");
        CanonicalSecurityModel canonB = new CanonicalSecurityModel();
        canonB.setTelnet(true);
        docB.setCanonical(canonB);
        normalizedConfigRepository.save(docB);

        DriftEvent event = driftDetectionService.detectDrift(docA, docB);
        assertThat(event.getChanges()).hasSize(1);
        DriftChange change = event.getChanges().get(0);
        assertThat(change.getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(change.getBefore()).isNull();
        assertThat(change.getAfter()).isEqualTo(true);
        assertThat(change.getChangeType()).isEqualTo("ADDED");
        assertThat(change.getClassification()).isEqualTo(DriftClassification.DEGRADED.name());
        assertThat(event.getRiskBefore()).isEqualTo(0);
        assertThat(event.getRiskAfter()).isEqualTo(70);
        assertThat(event.getImpact()).isEqualTo("INCREASED");

        Document rawDoc = mongoTemplate.getCollection("drift_events").find(new Document("deviceId", deviceId)).first();
        System.out.println("=== NULL TO TRUE PERSISTED DRIFT EVENT ===");
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");
    }

    @Test
    @DisplayName("A5.2: Drift on telnet true -> null (REMOVED changeType, UNKNOWN_IMPACT, risk decreased 70->0, impact UNKNOWN)")
    void testTelnetDrift_TrueToNull() {
        cisSeeder.seed();

        String deviceId = "dev-drift-true-to-null";
        NormalizedConfigurationDocument docA = new NormalizedConfigurationDocument();
        docA.setId("norm-tn-1");
        docA.setDeviceId(deviceId);
        docA.setConfigurationId("cfg-tn-1");
        docA.setVersionId("ver-1");
        docA.setVendor("Cisco");
        docA.setPlatform("IOS-XE");
        CanonicalSecurityModel canonA = new CanonicalSecurityModel();
        canonA.setTelnet(true);
        docA.setCanonical(canonA);
        normalizedConfigRepository.save(docA);

        NormalizedConfigurationDocument docB = new NormalizedConfigurationDocument();
        docB.setId("norm-tn-2");
        docB.setDeviceId(deviceId);
        docB.setConfigurationId("cfg-tn-2");
        docB.setVersionId("ver-2");
        docB.setVendor("Cisco");
        docB.setPlatform("IOS-XE");
        CanonicalSecurityModel canonB = new CanonicalSecurityModel();
        // telnet left null
        docB.setCanonical(canonB);
        normalizedConfigRepository.save(docB);

        DriftEvent event = driftDetectionService.detectDrift(docA, docB);
        assertThat(event.getChanges()).hasSize(1);
        DriftChange change = event.getChanges().get(0);
        assertThat(change.getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(change.getBefore()).isEqualTo(true);
        assertThat(change.getAfter()).isNull();
        assertThat(change.getChangeType()).isEqualTo("REMOVED");
        assertThat(change.getClassification()).isEqualTo(DriftClassification.UNKNOWN_IMPACT.name());
        assertThat(event.getRiskBefore()).isEqualTo(70);
        assertThat(event.getRiskAfter()).isEqualTo(0);
        assertThat(event.getImpact()).isEqualTo("UNKNOWN");

        Document rawDoc = mongoTemplate.getCollection("drift_events").find(new Document("deviceId", deviceId)).first();
        System.out.println("=== TRUE TO NULL PERSISTED DRIFT EVENT ===");
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");
    }

    @Test
    @DisplayName("A5.2b: Telnet true->false (IMPROVED) plus field with no rule (security.https.enabled) yields impact UNKNOWN despite risk decrease 70->0")
    void testTelnetImproved_PlusFieldWithNoRule_YieldsUnknownImpact() {
        cisSeeder.seed();

        String deviceId = "dev-drift-improved-plus-norule";
        NormalizedConfigurationDocument docA = new NormalizedConfigurationDocument();
        docA.setId("norm-ipnr-1");
        docA.setDeviceId(deviceId);
        docA.setConfigurationId("cfg-ipnr-1");
        docA.setVersionId("ver-ipnr-1");
        docA.setVendor("Cisco");
        docA.setPlatform("IOS-XE");
        CanonicalSecurityModel canonA = new CanonicalSecurityModel();
        canonA.setTelnet(true);
        canonA.setHttps(false);
        docA.setCanonical(canonA);
        normalizedConfigRepository.save(docA);

        NormalizedConfigurationDocument docB = new NormalizedConfigurationDocument();
        docB.setId("norm-ipnr-2");
        docB.setDeviceId(deviceId);
        docB.setConfigurationId("cfg-ipnr-2");
        docB.setVersionId("ver-ipnr-2");
        docB.setVendor("Cisco");
        docB.setPlatform("IOS-XE");
        CanonicalSecurityModel canonB = new CanonicalSecurityModel();
        canonB.setTelnet(false);
        canonB.setHttps(true);
        docB.setCanonical(canonB);
        normalizedConfigRepository.save(docB);

        DriftEvent event = driftDetectionService.detectDrift(docA, docB);
        assertThat(event.getChanges()).hasSize(2);

        DriftChange telnetChange = event.getChanges().stream()
                .filter(c -> "security.telnet.enabled".equals(c.getCanonicalField()))
                .findFirst().orElseThrow();
        assertThat(telnetChange.getClassification()).isEqualTo(DriftClassification.IMPROVED.name());

        DriftChange httpsChange = event.getChanges().stream()
                .filter(c -> "security.https.enabled".equals(c.getCanonicalField()))
                .findFirst().orElseThrow();
        assertThat(httpsChange.getClassification()).isEqualTo(DriftClassification.UNKNOWN_IMPACT.name());

        assertThat(event.getRiskBefore()).isEqualTo(70);
        assertThat(event.getRiskAfter()).isEqualTo(0);
        assertThat(event.getImpact()).isEqualTo("UNKNOWN");

        Document rawDoc = mongoTemplate.getCollection("drift_events").find(new Document("deviceId", deviceId)).first();
        System.out.println("=== TELNET IMPROVED PLUS NO-RULE FIELD PERSISTED DRIFT EVENT ===");
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");
    }

    @Test
    @DisplayName("A5.3: Numeric vs String equivalence on ssh.version (2 vs \"2\") produces zero changes")
    void testSshVersion_NumericVsStringEquivalence_ProducesZeroChanges() {
        String deviceId = "dev-drift-ssh-equiv";
        NormalizedConfigurationDocument docA = new NormalizedConfigurationDocument();
        docA.setId("norm-ssh-1");
        docA.setDeviceId(deviceId);
        docA.setConfigurationId("cfg-s-1");
        docA.setVersionId("ver-1");
        docA.setVendor("Cisco");
        docA.setPlatform("IOS-XE");
        CanonicalSecurityModel canonA = new CanonicalSecurityModel();
        canonA.setSsh(true, 2); // Integer 2
        docA.setCanonical(canonA);
        normalizedConfigRepository.save(docA);

        NormalizedConfigurationDocument docB = new NormalizedConfigurationDocument();
        docB.setId("norm-ssh-2");
        docB.setDeviceId(deviceId);
        docB.setConfigurationId("cfg-s-2");
        docB.setVersionId("ver-2");
        docB.setVendor("Cisco");
        docB.setPlatform("IOS-XE");
        CanonicalSecurityModel canonB = new CanonicalSecurityModel();
        Map<String, Object> sshMap = new java.util.LinkedHashMap<>();
        sshMap.put("enabled", true);
        sshMap.put("version", "2"); // String "2"
        canonB.getSecurity().put("ssh", sshMap);
        docB.setCanonical(canonB);
        normalizedConfigRepository.save(docB);

        DriftEvent event = driftDetectionService.detectDrift(docA, docB);
        assertThat(event.getChanges()).isEmpty();
        assertThat(event.getImpact()).isEqualTo("NO_CHANGE");
        assertThat(event.getRiskBefore()).isEqualTo(0);
        assertThat(event.getRiskAfter()).isEqualTo(0);
    }

    @Test
    @DisplayName("A5.4: Two fields changed at once (one improved, one degraded) with equal risk yields MIXED impact")
    void testTwoFieldsChangedAtOnce_OneImprovedOneDegraded_ImpactDetermination() {
        cisSeeder.seed();

        String deviceId = "dev-drift-multi-change";
        // Before: telnet=true (violates CIS-1.2.2, risk 70), snmp.version="3" (complies with CIS-1.5.9)
        NormalizedConfigurationDocument docA = new NormalizedConfigurationDocument();
        docA.setId("norm-m-1");
        docA.setDeviceId(deviceId);
        docA.setConfigurationId("cfg-m-1");
        docA.setVersionId("ver-1");
        docA.setVendor("Cisco");
        docA.setPlatform("IOS-XE");
        CanonicalSecurityModel canonA = new CanonicalSecurityModel();
        canonA.setTelnet(true);
        canonA.setSnmp(true, "3");
        docA.setCanonical(canonA);
        normalizedConfigRepository.save(docA);

        // After: telnet=false (complies with CIS-1.2.2, IMPROVED, risk 0), snmp.version="2c" (violates CIS-1.5.9, DEGRADED, risk 70)
        NormalizedConfigurationDocument docB = new NormalizedConfigurationDocument();
        docB.setId("norm-m-2");
        docB.setDeviceId(deviceId);
        docB.setConfigurationId("cfg-m-2");
        docB.setVersionId("ver-2");
        docB.setVendor("Cisco");
        docB.setPlatform("IOS-XE");
        CanonicalSecurityModel canonB = new CanonicalSecurityModel();
        canonB.setTelnet(false);
        canonB.setSnmp(true, "2c");
        docB.setCanonical(canonB);
        normalizedConfigRepository.save(docB);

        DriftEvent event = driftDetectionService.detectDrift(docA, docB);
        assertThat(event.getChanges()).hasSize(2);

        DriftChange telnetChange = event.getChanges().stream()
                .filter(c -> "security.telnet.enabled".equals(c.getCanonicalField()))
                .findFirst().orElseThrow();
        assertThat(telnetChange.getClassification()).isEqualTo(DriftClassification.IMPROVED.name());

        DriftChange snmpChange = event.getChanges().stream()
                .filter(c -> "security.snmp.version".equals(c.getCanonicalField()))
                .findFirst().orElseThrow();
        assertThat(snmpChange.getClassification()).isEqualTo(DriftClassification.DEGRADED.name());

        // Both risks equal 70, contains both IMPROVED and DEGRADED -> MIXED impact
        assertThat(event.getRiskBefore()).isEqualTo(70);
        assertThat(event.getRiskAfter()).isEqualTo(70);
        assertThat(event.getImpact()).isEqualTo("MIXED");

        Document rawDoc = mongoTemplate.getCollection("drift_events").find(new Document("deviceId", deviceId)).first();
        System.out.println("=== MIXED EVENT PERSISTED DRIFT EVENT ===");
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");
    }

    @Test
    @DisplayName("A5.5: Source-map-only difference produces zero drift changes")
    void testSourceMapOnlyDifference_ZeroChanges() {
        String deviceId = "dev-drift-sm-only";
        NormalizedConfigurationDocument docA = new NormalizedConfigurationDocument();
        docA.setId("norm-sm-1");
        docA.setDeviceId(deviceId);
        docA.setConfigurationId("cfg-sm-1");
        docA.setVersionId("ver-1");
        docA.setVendor("Cisco");
        docA.setPlatform("IOS-XE");
        CanonicalSecurityModel canonA = new CanonicalSecurityModel();
        canonA.setTelnet(false);
        docA.setCanonical(canonA);
        docA.setSourceMap(List.of(new SourceMapEntry("security.telnet.enabled", 10, "transport input ssh")));
        normalizedConfigRepository.save(docA);

        NormalizedConfigurationDocument docB = new NormalizedConfigurationDocument();
        docB.setId("norm-sm-2");
        docB.setDeviceId(deviceId);
        docB.setConfigurationId("cfg-sm-2");
        docB.setVersionId("ver-2");
        docB.setVendor("Cisco");
        docB.setPlatform("IOS-XE");
        CanonicalSecurityModel canonB = new CanonicalSecurityModel();
        canonB.setTelnet(false);
        docB.setCanonical(canonB);
        docB.setSourceMap(List.of(new SourceMapEntry("security.telnet.enabled", 15, "line vty 0 4\n transport input ssh")));
        normalizedConfigRepository.save(docB);

        DriftEvent event = driftDetectionService.detectDrift(docA, docB);
        assertThat(event.getChanges()).isEmpty();
        assertThat(event.getImpact()).isEqualTo("NO_CHANGE");
    }

    @Test
    @DisplayName("4a: Reject identical fromVersionId/toVersionId or identical document ids")
    void testIdenticalVersionIdOrDocId_ThrowsIllegalArgumentException() {
        String deviceId = "dev-drift-same-ver";
        NormalizedConfigurationDocument docA = createDoc(deviceId, "cfg-sv-1", "ver-1", "17.6", true, "transport input telnet");
        NormalizedConfigurationDocument docB = createDoc(deviceId, "cfg-sv-2", "ver-1", "17.6", false, "transport input ssh");

        assertThatThrownBy(() -> driftDetectionService.detectDrift(docA, docB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identical versionId");

        assertThatThrownBy(() -> driftDetectionService.detectDrift(docA, docA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identical configuration document");

        assertThatThrownBy(() -> driftDetectionService.detectDriftByVersionIds(deviceId, "ver-1", "ver-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identical versionId");

        assertThatThrownBy(() -> driftDetectionService.detectDriftByDocumentIds(docA.getId(), docA.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identical document id");
    }

    @Test
    @DisplayName("4b: affectedFindingIds only includes Findings tied to before document configurationId")
    void testAffectedFindingIds_OnlyIncludesFindingsTiedToBeforeDoc() {
        cisSeeder.seed();

        String deviceId = "dev-cisco-drift-audit-link";
        String configBefore = String.join("\n",
                "hostname cisco-edge-01",
                "version 17.6",
                "aaa new-model",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input telnet",
                "logging buffered 16384",
                "ntp server 10.0.0.1"
        );

        // Run real audit on before config to produce real persisted FindingDocument
        Audit auditBefore = auditOrchestrationService.startAudit(deviceId, "cfg-v1-audit", "ver-1", configBefore);
        assertThat(auditBefore.getStatus()).isEqualTo("COMPLETED");

        List<FindingDocument> beforeFindings = findingRepository.findByDeviceId(deviceId);
        assertThat(beforeFindings).isNotEmpty();
        FindingDocument telnetFindingBefore = beforeFindings.stream()
                .filter(f -> "security.telnet.enabled".equals(f.getCanonicalField()))
                .findFirst().orElseThrow();
        assertThat(telnetFindingBefore.getConfigurationId()).isEqualTo("cfg-v1-audit");

        // Inject a finding belonging to another version (cfg-other-999) on the same device and field
        FindingDocument foreignFinding = new FindingDocument();
        foreignFinding.setId("find-foreign-999");
        foreignFinding.setDeviceId(deviceId);
        foreignFinding.setConfigurationId("cfg-other-999");
        foreignFinding.setCanonicalField("security.telnet.enabled");
        foreignFinding.setStatus("OPEN");
        findingRepository.save(foreignFinding);

        // Version 2 (After): Telnet disabled
        String configAfter = String.join("\n",
                "hostname cisco-edge-01",
                "version 17.6",
                "aaa new-model",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input ssh",
                "logging buffered 16384",
                "ntp server 10.0.0.1"
        );
        NormalizedConfigurationDocument docAfter = normalizationService.normalizeAndPersist(
                configAfter, deviceId, "cfg-v2-audit", "ver-2", "Cisco", "IOS-XE", "17.6"
        );
        NormalizedConfigurationDocument docBefore = normalizedConfigRepository.findByConfigurationId("cfg-v1-audit").orElseThrow();

        DriftEvent event = driftDetectionService.detectDrift(docBefore, docAfter);
        assertThat(event.getAffectedFindingIds()).isNotEmpty();
        assertThat(event.getAffectedFindingIds()).contains(telnetFindingBefore.getId());
        assertThat(event.getAffectedFindingIds()).doesNotContain("find-foreign-999");

        Document rawDoc = mongoTemplate.getCollection("drift_events").find(new Document("deviceId", deviceId)).first();
        System.out.println("=== 4b PERSISTED DRIFT EVENT WITH REAL AUDIT FINDINGS ===");
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");
    }

    @Test
    @DisplayName("A5.7: Missing document ID throws IllegalArgumentException")
    void testMissingDocumentId_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> driftDetectionService.detectDriftByDocumentIds("non-existent-doc-1", "non-existent-doc-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Normalized configuration not found for id: non-existent-doc-1");
    }

    @Test
    @DisplayName("A5.8: Vendor-neutral rules evaluated against Juniper device yield DEGRADED and INCREASED impact")
    void testCiscoRulesOnJuniperDevice_NowRuleCovered() {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String deviceId = "dev-juniper-drift-01";
        NormalizedConfigurationDocument docA = new NormalizedConfigurationDocument();
        docA.setId("norm-j-1");
        docA.setDeviceId(deviceId);
        docA.setConfigurationId("cfg-j-1");
        docA.setVersionId("ver-j-1");
        docA.setVendor("Juniper");
        docA.setPlatform("JUNOS");
        CanonicalSecurityModel canonA = new CanonicalSecurityModel();
        canonA.setTelnet(false);
        docA.setCanonical(canonA);
        normalizedConfigRepository.save(docA);

        NormalizedConfigurationDocument docB = new NormalizedConfigurationDocument();
        docB.setId("norm-j-2");
        docB.setDeviceId(deviceId);
        docB.setConfigurationId("cfg-j-2");
        docB.setVersionId("ver-j-2");
        docB.setVendor("Juniper");
        docB.setPlatform("JUNOS");
        CanonicalSecurityModel canonB = new CanonicalSecurityModel();
        canonB.setTelnet(true);
        docB.setCanonical(canonB);
        normalizedConfigRepository.save(docB);

        DriftEvent event = driftDetectionService.detectDrift(docA, docB);
        assertThat(event.getChanges()).hasSize(1);
        DriftChange change = event.getChanges().get(0);
        assertThat(change.getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(change.getClassification()).isEqualTo(DriftClassification.DEGRADED.name());
        assertThat(event.getRiskBefore()).isEqualTo(0);
        assertThat(event.getRiskAfter()).isEqualTo(70);
        assertThat(event.getImpact()).isEqualTo("INCREASED");

        Document rawDoc = mongoTemplate.getCollection("drift_events").find(new Document("deviceId", deviceId)).first();
        System.out.println("=== JUNIPER TELNET PERSISTED DRIFT EVENT ===");
        System.out.println(rawDoc != null ? rawDoc.toJson() : "null");
    }

    @Test
    @DisplayName("A4 verification: calculateRisk does not persist documents to risk_assessments collection")
    void testRiskAssessmentsCollection_UnmutatedCount() {
        cisSeeder.seed();
        assertThat(riskAssessmentRepository.count()).isEqualTo(0);

        String deviceId = "dev-drift-risk-count";
        NormalizedConfigurationDocument docBefore = createDoc(deviceId, "cfg-rc-1", "ver-1", "17.6", true, "transport input telnet");
        NormalizedConfigurationDocument docAfter = createDoc(deviceId, "cfg-rc-2", "ver-2", "17.6", false, "transport input ssh");

        long countBefore = riskAssessmentRepository.count();
        DriftEvent event = driftDetectionService.detectDrift(docBefore, docAfter);
        long countAfter = riskAssessmentRepository.count();

        assertThat(countBefore).isEqualTo(0);
        assertThat(countAfter).isEqualTo(0);
        assertThat(event.getRiskBefore()).isEqualTo(70);
        assertThat(event.getRiskAfter()).isEqualTo(0);
    }

    @Test
    @DisplayName("A5.19: RiskBefore clamped at 100 with further degrade yields equal risk 100->100 and INCREASED impact")
    void testClampedRiskAt100_FurtherDegrade_YieldsIncreasedImpact() {
        cisSeeder.seed();

        String deviceId = "dev-drift-clamped-risk";

        // docA has two failing fields (ssh version 1 -> 70, snmp version "1" -> 70) and telnet passing (false -> 0)
        // Total risk before: 70 + 70 + 0 = 140 -> clamped to 100
        NormalizedConfigurationDocument docA = new NormalizedConfigurationDocument();
        docA.setId("norm-clamp-1");
        docA.setDeviceId(deviceId);
        docA.setConfigurationId("cfg-clamp-1");
        docA.setVersionId("ver-1");
        docA.setVendor("Cisco");
        docA.setPlatform("IOS-XE");
        docA.setOsVersion("17.6");
        CanonicalSecurityModel canonA = new CanonicalSecurityModel();
        canonA.setSsh(true, 1);
        canonA.setSnmp(true, "1");
        canonA.setTelnet(false);
        docA.setCanonical(canonA);
        docA.setSourceMap(List.of(
                new SourceMapEntry("security.ssh.version", 10, "ip ssh version 1"),
                new SourceMapEntry("security.snmp.version", 11, "snmp-server community public RO"),
                new SourceMapEntry("security.telnet.enabled", 12, "transport input ssh")
        ));
        normalizedConfigRepository.save(docA);

        // docB changes ssh to 0 (still fails -> NO_SECURITY_IMPACT, risk 70),
        // snmp to "2c" (still fails -> NO_SECURITY_IMPACT, risk 70),
        // and telnet from false to true (DEGRADED, risk 70)
        // Total risk after: 70 + 70 + 70 = 210 -> clamped to 100
        NormalizedConfigurationDocument docB = new NormalizedConfigurationDocument();
        docB.setId("norm-clamp-2");
        docB.setDeviceId(deviceId);
        docB.setConfigurationId("cfg-clamp-2");
        docB.setVersionId("ver-2");
        docB.setVendor("Cisco");
        docB.setPlatform("IOS-XE");
        docB.setOsVersion("17.6");
        CanonicalSecurityModel canonB = new CanonicalSecurityModel();
        canonB.setSsh(true, 0);
        canonB.setSnmp(true, "2c");
        canonB.setTelnet(true);
        docB.setCanonical(canonB);
        docB.setSourceMap(List.of(
                new SourceMapEntry("security.ssh.version", 10, "no ip ssh"),
                new SourceMapEntry("security.snmp.version", 11, "snmp-server community private RW"),
                new SourceMapEntry("security.telnet.enabled", 12, "transport input telnet")
        ));
        normalizedConfigRepository.save(docB);

        DriftEvent event = driftDetectionService.detectDrift(docA, docB);

        assertThat(event.getChanges()).hasSize(3);

        DriftChange telnetChange = event.getChanges().stream()
                .filter(c -> "security.telnet.enabled".equals(c.getCanonicalField()))
                .findFirst().orElseThrow();
        assertThat(telnetChange.getClassification()).isEqualTo(DriftClassification.DEGRADED.name());

        // Verify riskBefore and riskAfter are both clamped at 100
        assertThat(event.getRiskBefore()).isEqualTo(100);
        assertThat(event.getRiskAfter()).isEqualTo(100);

        // Equal risk with only DEGRADED change must yield INCREASED impact
        assertThat(event.getImpact()).isEqualTo("INCREASED");
    }

    private NormalizedConfigurationDocument createDoc(
            String deviceId,
            String configId,
            String versionId,
            String osVersion,
            boolean telnetEnabled,
            String rawLine) {
        NormalizedConfigurationDocument doc = new NormalizedConfigurationDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setDeviceId(deviceId);
        doc.setConfigurationId(configId);
        doc.setVersionId(versionId);
        doc.setOsVersion(osVersion);
        doc.setVendor("Cisco");
        doc.setPlatform("IOS-XE");

        CanonicalSecurityModel canon = new CanonicalSecurityModel();
        canon.setTelnet(telnetEnabled);
        doc.setCanonical(canon);

        doc.setSourceMap(List.of(new SourceMapEntry("security.telnet.enabled", 10, rawLine)));
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        return normalizedConfigRepository.save(doc);
    }
}
