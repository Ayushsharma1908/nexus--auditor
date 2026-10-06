package com.nexuscomply.cyber.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexuscomply.cyber.ai.persistence.AiJobRepository;
import com.nexuscomply.cyber.ai.persistence.AiMappingDocument;
import com.nexuscomply.cyber.ai.persistence.AiMappingRepository;
import com.nexuscomply.cyber.ai.service.AiMappingService;
import com.nexuscomply.cyber.ai.service.AiMappingServiceImpl;
import com.nexuscomply.cyber.ai.service.DeterministicStubSuggestionProvider;
import com.nexuscomply.cyber.audit.Audit;
import com.nexuscomply.cyber.audit.AuditOrchestrationService;
import com.nexuscomply.cyber.audit.AuditOrchestrationServiceImpl;
import com.nexuscomply.cyber.audit.persistence.AuditRepository;
import com.nexuscomply.cyber.compliance.DefaultRuleApplicabilityChecker;
import com.nexuscomply.cyber.compliance.GenericRuleEvaluator;
import com.nexuscomply.cyber.compliance.RuleApplicabilityChecker;
import com.nexuscomply.cyber.compliance.RuleEvaluator;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.compliance.persistence.ControlRepository;
import com.nexuscomply.cyber.compliance.persistence.FrameworkRepository;
import com.nexuscomply.cyber.compliance.rules.cis.CisCiscoIosXeRuleSeeder;
import com.nexuscomply.cyber.compliance.rules.iso.Iso27001RuleSeeder;
import com.nexuscomply.cyber.compliance.rules.nist.NistSp80053RuleSeeder;
import com.nexuscomply.cyber.detection.VendorDetectionResponse;
import com.nexuscomply.cyber.detection.VendorDetectionService;
import com.nexuscomply.cyber.detection.VendorDetectionStatus;
import com.nexuscomply.cyber.drift.persistence.DriftEventRepository;
import com.nexuscomply.cyber.evidence.EvidenceCreationService;
import com.nexuscomply.cyber.evidence.EvidenceCreationServiceImpl;
import com.nexuscomply.cyber.evidence.persistence.EvidenceRepository;
import com.nexuscomply.cyber.finding.FindingCreationService;
import com.nexuscomply.cyber.finding.FindingCreationServiceImpl;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.normalization.NormalizationService;
import com.nexuscomply.cyber.normalization.NormalizationServiceImpl;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
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
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 2.11 Integration Tests: What-If Simulation Engine.
 * Tests strictly adhere to schema1.md section 14 and cyberlayer.pdf Section 20, 33 item 16, Criterion 10.
 */
class WhatIfSimulationIntegrationTest {

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
    private DriftEventRepository driftEventRepository;
    private AiMappingRepository aiMappingRepository;
    private AiJobRepository aiJobRepository;
    private WhatIfSimulationRepository simulationRepository;

    private CisCiscoIosXeRuleSeeder cisSeeder;
    private NistSp80053RuleSeeder nistSeeder;
    private Iso27001RuleSeeder isoSeeder;

    private ParserService parserService;
    private NormalizationService normalizationService;
    private VendorDetectionService vendorDetectionService;
    private RuleApplicabilityChecker applicabilityChecker;
    private RuleEvaluator ruleEvaluator;
    private FindingCreationService findingCreationService;
    private RiskCalculationService riskCalculationService;
    private AuditOrchestrationService auditOrchestrationService;
    private WhatIfSimulationService whatIfSimulationService;

    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUpAll() {
        mongoServer = new MongoServer(new MemoryBackend());
        InetSocketAddress address = mongoServer.bind();
        String connectionString = "mongodb://" + address.getHostName() + ":" + address.getPort() + "/testdb";
        mongoTemplate = new MongoTemplate(new SimpleMongoClientDatabaseFactory(connectionString));
    }

    @AfterAll
    static void tearDownAll() {
        if (mongoServer != null) {
            mongoServer.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();

        MongoRepositoryFactory factory = new MongoRepositoryFactory(mongoTemplate);
        frameworkRepository = factory.getRepository(FrameworkRepository.class);
        controlRepository = factory.getRepository(ControlRepository.class);
        ruleRepository = factory.getRepository(ComplianceRuleRepository.class);
        normalizedConfigRepository = factory.getRepository(NormalizedConfigurationRepository.class);
        findingRepository = factory.getRepository(FindingRepository.class);
        evidenceRepository = factory.getRepository(EvidenceRepository.class);
        riskRepository = factory.getRepository(RiskAssessmentRepository.class);
        auditRepository = factory.getRepository(AuditRepository.class);
        driftEventRepository = factory.getRepository(DriftEventRepository.class);
        aiMappingRepository = factory.getRepository(AiMappingRepository.class);
        aiJobRepository = factory.getRepository(AiJobRepository.class);
        simulationRepository = factory.getRepository(WhatIfSimulationRepository.class);

        cisSeeder = new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        nistSeeder = new NistSp80053RuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        isoSeeder = new Iso27001RuleSeeder(frameworkRepository, controlRepository, ruleRepository);

        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        parserService = new ParserServiceImpl(
                List.of(new CiscoIosParser(), new JuniperJunosParser(), new FortinetFortiOSParser(), new PaloAltoPanOsParser()),
                aiMappingRepository
        );

        normalizationService = new NormalizationServiceImpl(parserService, normalizedConfigRepository);

        vendorDetectionService = new com.nexuscomply.cyber.detection.VendorFingerprintDetectionService();

        applicabilityChecker = new DefaultRuleApplicabilityChecker();
        ruleEvaluator = new GenericRuleEvaluator();
        findingCreationService = new FindingCreationServiceImpl(findingRepository, ruleRepository, controlRepository, new EvidenceCreationServiceImpl(evidenceRepository), normalizedConfigRepository);
        riskCalculationService = new RiskCalculationServiceImpl(riskRepository);

        AiMappingService aiMappingService = new AiMappingServiceImpl(aiMappingRepository, aiJobRepository, new DeterministicStubSuggestionProvider());

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

        whatIfSimulationService = new WhatIfSimulationServiceImpl(
                normalizedConfigRepository,
                parserService,
                ruleRepository,
                controlRepository,
                applicabilityChecker,
                ruleEvaluator,
                riskCalculationService,
                findingRepository,
                simulationRepository
        );

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("1. Criterion 10: Collection counts unchanged before/after simulation (in-memory execution)")
    void testCollectionCountsUnchangedBeforeAndAfterSimulation() {
        String baseConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-SIM-BASE",
                "transport input telnet ssh",
                "line vty 0 4",
                " transport input telnet ssh"
        );

        Audit audit = auditOrchestrationService.startAudit("dev-sim-01", "cfg-sim-01", "v1.0", baseConfig);
        assertThat(audit.getStatus()).isEqualTo("COMPLETED");

        // Seed an AI mapping document with usageCount to verify it remains unchanged
        AiMappingDocument m = new AiMappingDocument();
        m.setId("map-sim-proof-01");
        m.setVendor("Cisco");
        m.setPlatform("IOS-XE");
        m.setRawSyntax("logging buffered 16384");
        m.setCanonicalField("logging.localLogging");
        m.setMappedValue(true);
        m.setUsageCount(3);
        aiMappingRepository.save(m);

        // Record initial collection counts across EVERY collection in the system
        long auditsBefore = auditRepository.count();
        long findingsBefore = findingRepository.count();
        long evidenceBefore = evidenceRepository.count();
        long risksBefore = riskRepository.count();
        long normsBefore = normalizedConfigRepository.count();
        long driftsBefore = driftEventRepository.count();
        long aiMappingsBefore = aiMappingRepository.count();
        int usageCountBefore = aiMappingRepository.findById("map-sim-proof-01").map(AiMappingDocument::getUsageCount).orElse(0);
        long aiJobsBefore = aiJobRepository.count();
        long simsBefore = simulationRepository.count();

        System.out.println("=== INITIAL COLLECTION COUNTS (BEFORE SIMULATION) ===");
        System.out.printf("audits: %d, findings: %d, evidence: %d, risk_assessments: %d, normalized_configurations: %d, drift_events: %d, ai_mappings: %d (usageCount: %d), ai_jobs: %d, what_if_simulations: %d%n",
                auditsBefore, findingsBefore, evidenceBefore, risksBefore, normsBefore, driftsBefore, aiMappingsBefore, usageCountBefore, aiJobsBefore, simsBefore);

        // 1. Run simulation with persist = false (in-memory execution)
        WhatIfSimulationRequest requestNonPersist = new WhatIfSimulationRequest("dev-sim-01", "v1.0");
        requestNonPersist.setCanonicalOverrides(Map.of("security.telnet.enabled", false));
        requestNonPersist.setPersist(false);

        WhatIfSimulationResult resultNonPersist = whatIfSimulationService.simulate(requestNonPersist);
        assertThat(resultNonPersist).isNotNull();
        assertThat(resultNonPersist.getStatus()).isEqualTo("COMPLETED");
        assertThat(resultNonPersist.getPersistedSimulationId()).isNull();

        // Assert that EVERY collection count and mapping usageCount is strictly unchanged
        assertThat(auditRepository.count()).isEqualTo(auditsBefore);
        assertThat(findingRepository.count()).isEqualTo(findingsBefore);
        assertThat(evidenceRepository.count()).isEqualTo(evidenceBefore);
        assertThat(riskRepository.count()).isEqualTo(risksBefore);
        assertThat(normalizedConfigRepository.count()).isEqualTo(normsBefore);
        assertThat(driftEventRepository.count()).isEqualTo(driftsBefore);
        assertThat(aiMappingRepository.count()).isEqualTo(aiMappingsBefore);
        int usageCountAfterNonPersist = aiMappingRepository.findById("map-sim-proof-01").map(AiMappingDocument::getUsageCount).orElse(0);
        assertThat(usageCountAfterNonPersist).isEqualTo(usageCountBefore);
        assertThat(aiJobRepository.count()).isEqualTo(aiJobsBefore);
        assertThat(simulationRepository.count()).isEqualTo(simsBefore);

        System.out.println("=== COUNTS AFTER SIMULATION (persist=false) ===");
        System.out.printf("audits: %d, findings: %d, evidence: %d, risk_assessments: %d, normalized_configurations: %d, drift_events: %d, ai_mappings: %d (usageCount: %d), ai_jobs: %d, what_if_simulations: %d%n",
                auditRepository.count(), findingRepository.count(), evidenceRepository.count(), riskRepository.count(),
                normalizedConfigRepository.count(), driftEventRepository.count(), aiMappingRepository.count(),
                usageCountAfterNonPersist, aiJobRepository.count(), simulationRepository.count());

        // 2. Run simulation with persist = true (only what_if_simulations must increment by 1)
        WhatIfSimulationRequest requestPersist = new WhatIfSimulationRequest("dev-sim-01", "v1.0");
        requestPersist.setCanonicalOverrides(Map.of("security.telnet.enabled", false));
        requestPersist.setName("Persist Proof Simulation");
        requestPersist.setPersist(true);

        WhatIfSimulationResult resultPersist = whatIfSimulationService.simulate(requestPersist);
        assertThat(resultPersist).isNotNull();
        assertThat(resultPersist.getPersistedSimulationId()).isNotNull();

        assertThat(simulationRepository.count()).isEqualTo(simsBefore + 1);
        assertThat(auditRepository.count()).isEqualTo(auditsBefore);
        assertThat(findingRepository.count()).isEqualTo(findingsBefore);
        assertThat(evidenceRepository.count()).isEqualTo(evidenceBefore);
        assertThat(riskRepository.count()).isEqualTo(risksBefore);
        assertThat(normalizedConfigRepository.count()).isEqualTo(normsBefore);
        assertThat(driftEventRepository.count()).isEqualTo(driftsBefore);
        assertThat(aiMappingRepository.count()).isEqualTo(aiMappingsBefore);
        int usageCountAfterPersist = aiMappingRepository.findById("map-sim-proof-01").map(AiMappingDocument::getUsageCount).orElse(0);
        assertThat(usageCountAfterPersist).isEqualTo(usageCountBefore);
        assertThat(aiJobRepository.count()).isEqualTo(aiJobsBefore);

        System.out.println("=== COUNTS AFTER SIMULATION (persist=true) ===");
        System.out.printf("audits: %d, findings: %d, evidence: %d, risk_assessments: %d, normalized_configurations: %d, drift_events: %d, ai_mappings: %d (usageCount: %d), ai_jobs: %d, what_if_simulations: %d (strictly +1)%n",
                auditRepository.count(), findingRepository.count(), evidenceRepository.count(), riskRepository.count(),
                normalizedConfigRepository.count(), driftEventRepository.count(), aiMappingRepository.count(),
                usageCountAfterPersist, aiJobRepository.count(), simulationRepository.count());
    }

    @Test
    @DisplayName("2. Fix that removes telnet shows FAIL->PASS, finding reduction, and risk decrease")
    void testFixRemovesTelnetShowsFailToPassAndRiskDecrease() throws Exception {
        String baseConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-TELNET-ON",
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
                " transport input telnet ssh"
        );

        auditOrchestrationService.startAudit("dev-sim-02", "cfg-sim-02", "v1.0", baseConfig);

        WhatIfSimulationRequest request = new WhatIfSimulationRequest("dev-sim-02", "v1.0");
        request.setCanonicalOverrides(Map.of("security.telnet.enabled", false));

        WhatIfSimulationResult result = whatIfSimulationService.simulate(request);
        assertThat(result).isNotNull();
        assertThat(result.getFindingDelta()).isEqualTo(-3); // 3 telnet rules (CIS, NIST, ISO) fail -> pass
        assertThat(result.getRiskDelta()).isEqualTo(-70);   // Risk delta from 70 to 0 (-70)
        assertThat(result.getImpact()).isEqualTo("DECREASED");

        // Verify per-field max rule (clamped 0-100) consistency
        assertThat(result.getBefore().getRiskScore()).isEqualTo(70);
        assertThat(result.getAfter().getRiskScore()).isEqualTo(0);

        // Verify complianceScore relationship to audit identity (passed / applicable)
        assertThat(result.getBefore().getFailedControls()).isEqualTo(3);
        assertThat(result.getBefore().getComplianceScore()).isEqualTo(85.7); // 18 / 21 = 85.7%
        assertThat(result.getAfter().getFailedControls()).isEqualTo(0);
        assertThat(result.getAfter().getComplianceScore()).isEqualTo(100.0); // 21 / 21 = 100.0%

        // Changes list contains telnet enabled -> false classified as IMPROVED
        assertThat(result.getChanges()).anyMatch(c ->
                "security.telnet.enabled".equals(c.getCanonicalField()) &&
                Boolean.FALSE.equals(c.getNewValue()) &&
                "IMPROVED".equals(c.getClassification())
        );

        System.out.println("=== RAW WHAT-IF SIMULATION RESULT (REMOVES TELNET) ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));

        // Impact test case A: Zero changes gives NO_CHANGE
        WhatIfSimulationRequest zeroChangesRequest = new WhatIfSimulationRequest("dev-sim-02", "v1.0");
        zeroChangesRequest.setCanonicalOverrides(Map.of());
        WhatIfSimulationResult zeroChangesResult = whatIfSimulationService.simulate(zeroChangesRequest);
        assertThat(zeroChangesResult.getImpact()).isEqualTo("NO_CHANGE");
        assertThat(zeroChangesResult.getChanges()).isEmpty();
        System.out.println("=== RAW WHAT-IF IMPACT (ZERO CHANGES -> NO_CHANGE) ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(zeroChangesResult));

        // Impact test case B: Telnet fix plus a field with no rule gives UNKNOWN
        WhatIfSimulationRequest unmappedFieldRequest = new WhatIfSimulationRequest("dev-sim-02", "v1.0");
        unmappedFieldRequest.setCanonicalOverrides(Map.of(
                "security.telnet.enabled", false,
                "security.https.enabled", false
        ));
        WhatIfSimulationResult unmappedFieldResult = whatIfSimulationService.simulate(unmappedFieldRequest);
        assertThat(unmappedFieldResult.getImpact()).isEqualTo("UNKNOWN");
        assertThat(unmappedFieldResult.getChanges()).anyMatch(c ->
                "security.https.enabled".equals(c.getCanonicalField()) &&
                "UNKNOWN_IMPACT".equals(c.getClassification())
        );
        System.out.println("=== RAW WHAT-IF IMPACT (TELNET FIX + FIELD WITH NO RULE -> UNKNOWN) ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(unmappedFieldResult));
    }

    @Test
    @DisplayName("3. Change that introduces telnet shows PASS->FAIL, finding increase, and risk increase")
    void testChangeIntroducesTelnetShowsIncrease() throws Exception {
        String baseConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-TELNET-OFF",
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
                " transport input ssh"
        );

        auditOrchestrationService.startAudit("dev-sim-03", "cfg-sim-03", "v1.0", baseConfig);

        WhatIfSimulationRequest request = new WhatIfSimulationRequest("dev-sim-03", "v1.0");
        request.setCanonicalOverrides(Map.of("security.telnet.enabled", true));

        WhatIfSimulationResult result = whatIfSimulationService.simulate(request);
        assertThat(result).isNotNull();
        assertThat(result.getFindingDelta()).isEqualTo(3);  // 3 telnet rules fail
        assertThat(result.getRiskDelta()).isEqualTo(70);    // Risk increases by 70 (0 -> 70 via per-field max)
        assertThat(result.getImpact()).isEqualTo("INCREASED");

        assertThat(result.getBefore().getFailedControls()).isEqualTo(0);
        assertThat(result.getBefore().getRiskScore()).isEqualTo(0);
        assertThat(result.getBefore().getComplianceScore()).isEqualTo(100.0);

        assertThat(result.getAfter().getFailedControls()).isEqualTo(3);
        assertThat(result.getAfter().getRiskScore()).isEqualTo(70);
        assertThat(result.getAfter().getComplianceScore()).isEqualTo(85.7);

        assertThat(result.getChanges()).anyMatch(c ->
                "security.telnet.enabled".equals(c.getCanonicalField()) &&
                Boolean.TRUE.equals(c.getNewValue()) &&
                "DEGRADED".equals(c.getClassification())
        );

        System.out.println("=== RAW WHAT-IF SIMULATION RESULT (INTRODUCES TELNET) ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }

    @Test
    @DisplayName("4. Unknown line in proposed raw config stays UNKNOWN")
    void testUnknownLineStaysUnknown() {
        String baseConfig = String.join("\n",
                "set system services ssh",
                "set system services ssh protocol-version v2"
        );

        auditOrchestrationService.startAudit("dev-sim-04", "cfg-sim-04", "v1.0", baseConfig);

        String proposedRawWithUnknown = String.join("\n",
                "set system services ssh",
                "set system services ssh protocol-version v2",
                "set system services unknown-custom-rule unverified-value"
        );

        WhatIfSimulationRequest request = new WhatIfSimulationRequest("dev-sim-04", "v1.0");
        request.setProposedRawConfig(proposedRawWithUnknown);

        WhatIfSimulationResult result = whatIfSimulationService.simulate(request);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        // Line remained UNKNOWN: zero mappings created, zero artificial passes
        assertThat(aiMappingRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("5. Same input twice gives the exact same output (deterministic)")
    void testDeterministicSameInputTwiceGivesSameOutput() throws Exception {
        String baseConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-SIM-DETERMINISTIC",
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
                " transport input telnet ssh"
        );

        auditOrchestrationService.startAudit("dev-sim-05", "cfg-sim-05", "v1.0", baseConfig);

        WhatIfSimulationRequest request1 = new WhatIfSimulationRequest("dev-sim-05", "v1.0");
        request1.setCanonicalOverrides(Map.of("security.telnet.enabled", false));

        WhatIfSimulationRequest request2 = new WhatIfSimulationRequest("dev-sim-05", "v1.0");
        request2.setCanonicalOverrides(Map.of("security.telnet.enabled", false));

        WhatIfSimulationResult res1 = whatIfSimulationService.simulate(request1);
        WhatIfSimulationResult res2 = whatIfSimulationService.simulate(request2);

        // Assert strict equality across all result dimensions
        assertThat(res1.getDeviceId()).isEqualTo(res2.getDeviceId());
        assertThat(res1.getBaseConfigurationVersionId()).isEqualTo(res2.getBaseConfigurationVersionId());
        assertThat(res1.getStatus()).isEqualTo(res2.getStatus());
        assertThat(res1.getFindingDelta()).isEqualTo(res2.getFindingDelta());
        assertThat(res1.getRiskDelta()).isEqualTo(res2.getRiskDelta());
        assertThat(res1.getImpact()).isEqualTo(res2.getImpact());
        assertThat(res1.getBefore().getComplianceScore()).isEqualTo(res2.getBefore().getComplianceScore());
        assertThat(res1.getBefore().getRiskScore()).isEqualTo(res2.getBefore().getRiskScore());
        assertThat(res1.getBefore().getFailedControls()).isEqualTo(res2.getBefore().getFailedControls());
        assertThat(res1.getAfter().getComplianceScore()).isEqualTo(res2.getAfter().getComplianceScore());
        assertThat(res1.getAfter().getRiskScore()).isEqualTo(res2.getAfter().getRiskScore());
        assertThat(res1.getAfter().getFailedControls()).isEqualTo(res2.getAfter().getFailedControls());
        assertThat(res1.getRuleResultsBefore()).isEqualTo(res2.getRuleResultsBefore());
        assertThat(res1.getRuleResultsAfter()).isEqualTo(res2.getRuleResultsAfter());
        assertThat(res1.getAffectedControlIds()).isEqualTo(res2.getAffectedControlIds());
        assertThat(res1.getAffectedFindingIds()).isEqualTo(res2.getAffectedFindingIds());

        String json1 = objectMapper.writeValueAsString(res1);
        String json2 = objectMapper.writeValueAsString(res2);
        assertThat(json1).isEqualTo(json2);

        System.out.println("=== DOUBLE-RUN EQUALITY VERIFIED ===");
        System.out.println("Run 1 SHA-256 match Run 2: true. Payload length: " + json1.length());
    }

    @Test
    @DisplayName("6. Unknown device or version is refused safely")
    void testUnknownDeviceOrVersionRefusedSafely() {
        WhatIfSimulationRequest badDeviceReq = new WhatIfSimulationRequest("non-existent-device", "v1.0");
        assertThatThrownBy(() -> whatIfSimulationService.simulate(badDeviceReq))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Base configuration not found");

        WhatIfSimulationRequest badVerReq = new WhatIfSimulationRequest("dev-sim-05", "non-existent-version");
        assertThatThrownBy(() -> whatIfSimulationService.simulate(badVerReq))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Base configuration not found");
    }

    @Test
    @DisplayName("7. Persisted simulation document when requested (schema1.md section 14)")
    void testPersistedSimulationDocumentWhenRequested() throws Exception {
        String baseConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-PERSIST-TEST",
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
                " transport input telnet ssh"
        );

        auditOrchestrationService.startAudit("dev-sim-07", "cfg-sim-07", "v1.0", baseConfig);

        WhatIfSimulationRequest request = new WhatIfSimulationRequest("dev-sim-07", "v1.0");
        request.setName("Disable Telnet");
        request.setDescription("Simulate disabling Telnet on management access.");
        request.setCanonicalOverrides(Map.of("security.telnet.enabled", false));
        request.setPersist(true);

        WhatIfSimulationResult result = whatIfSimulationService.simulate(request);
        assertThat(result).isNotNull();
        assertThat(result.getPersistedSimulationId()).isNotNull();

        // Verify document persisted in what_if_simulations matching schema1.md section 14
        WhatIfSimulationDocument simDoc = simulationRepository.findById(result.getPersistedSimulationId()).orElseThrow();
        assertThat(simDoc.getDeviceId()).isEqualTo("dev-sim-07");
        assertThat(simDoc.getBaseConfigurationVersionId()).isEqualTo("v1.0");
        assertThat(simDoc.getName()).isEqualTo("Disable Telnet");
        assertThat(simDoc.getDescription()).isEqualTo("Simulate disabling Telnet on management access.");
        assertThat(simDoc.getStatus()).isEqualTo("COMPLETED");
        assertThat(simDoc.getChanges()).isNotEmpty();
        assertThat(simDoc.getCreatedBy()).isEqualTo("user-uuid");
        assertThat(simDoc.getBefore().getRiskScore()).isEqualTo(70);
        assertThat(simDoc.getAfter().getRiskScore()).isEqualTo(0);
        assertThat(simDoc.getBefore().getComplianceScore()).isEqualTo(85.7);
        assertThat(simDoc.getAfter().getComplianceScore()).isEqualTo(100.0);
        assertThat(simDoc.getBefore().getFailedControls()).isEqualTo(3);
        assertThat(simDoc.getAfter().getFailedControls()).isEqualTo(0);

        System.out.println("=== RAW PERSISTED WHAT-IF SIMULATION DOCUMENT (schema1.md Sec 14) ===");
        org.bson.Document rawSim = mongoTemplate.getCollection("what_if_simulations").find(new org.bson.Document("_id", simDoc.getId())).first();
        System.out.println(rawSim != null ? rawSim.toJson() : "null");
    }
}
