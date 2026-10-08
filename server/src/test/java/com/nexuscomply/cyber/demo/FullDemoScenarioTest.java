package com.nexuscomply.cyber.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import com.nexuscomply.cyber.detection.VendorDetectionService;
import com.nexuscomply.cyber.detection.VendorFingerprintDetectionService;
import com.nexuscomply.cyber.drift.model.DriftEvent;
import com.nexuscomply.cyber.drift.persistence.DriftEventRepository;
import com.nexuscomply.cyber.drift.service.DriftDetectionService;
import com.nexuscomply.cyber.drift.service.DriftDetectionServiceImpl;
import com.nexuscomply.cyber.evidence.EvidenceCreationServiceImpl;
import com.nexuscomply.cyber.evidence.persistence.EvidenceRepository;
import com.nexuscomply.cyber.finding.FindingCreationService;
import com.nexuscomply.cyber.finding.FindingCreationServiceImpl;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.normalization.NormalizationService;
import com.nexuscomply.cyber.normalization.NormalizationServiceImpl;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.parser.ParserService;
import com.nexuscomply.cyber.parser.ParserServiceImpl;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import com.nexuscomply.cyber.parser.fortinet.FortinetFortiOSParser;
import com.nexuscomply.cyber.parser.juniper.JuniperJunosParser;
import com.nexuscomply.cyber.parser.paloalto.PaloAltoPanOsParser;
import com.nexuscomply.cyber.remediation.model.RemediationPlan;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanRepository;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateRepository;
import com.nexuscomply.cyber.remediation.seeder.RemediationTemplateSeeder;
import com.nexuscomply.cyber.remediation.service.RemediationPlanService;
import com.nexuscomply.cyber.remediation.service.RemediationPlanServiceImpl;
import com.nexuscomply.cyber.report.AuditReportResponse;
import com.nexuscomply.cyber.report.DashboardReportingService;
import com.nexuscomply.cyber.report.DashboardReportingServiceImpl;
import com.nexuscomply.cyber.report.DevicePostureResponse;
import com.nexuscomply.cyber.report.FleetSummaryResponse;
import com.nexuscomply.cyber.risk.RiskCalculationService;
import com.nexuscomply.cyber.risk.RiskCalculationServiceImpl;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentRepository;
import com.nexuscomply.cyber.simulation.WhatIfSimulationRepository;
import com.nexuscomply.cyber.simulation.WhatIfSimulationRequest;
import com.nexuscomply.cyber.simulation.WhatIfSimulationResult;
import com.nexuscomply.cyber.simulation.WhatIfSimulationService;
import com.nexuscomply.cyber.simulation.WhatIfSimulationServiceImpl;
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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 2.14: End-to-End "Golden Path" Demo Scenario Test.
 * Simulates the complete demonstration lifecycle across multi-vendor ingestion,
 * compliance posture calculation, automated remediation planning, pre-deployment
 * What-If impact simulation, configuration change deployment, drift analysis,
 * self-learning AI unknown-syntax resolution loop, and executive report export.
 *
 * Exports JSON and Markdown artifacts directly to disk at D:\auditor\demo\output\.
 */
public class FullDemoScenarioTest {

    private static final String CISCO_DEVICE_ID = "demo-cisco-01";
    private static final String JUNIPER_DEVICE_ID = "demo-juniper-01";
    private static final String FORTINET_DEVICE_ID = "demo-fortinet-01";
    private static final String PALOALTO_DEVICE_ID = "demo-paloalto-01";

    private static final File OUTPUT_DIR = new File("demo/output");

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
    private RemediationTemplateRepository templateRepository;
    private RemediationPlanRepository remediationPlanRepository;
    private AiMappingRepository aiMappingRepository;
    private AiJobRepository aiJobRepository;
    private WhatIfSimulationRepository simulationRepository;

    private DeterministicStubSuggestionProvider stubProvider;
    private ParserService parserService;
    private NormalizationService normalizationService;
    private VendorDetectionService vendorDetectionService;
    private RuleApplicabilityChecker applicabilityChecker;
    private RuleEvaluator ruleEvaluator;
    private FindingCreationService findingCreationService;
    private RiskCalculationService riskCalculationService;
    private RemediationPlanService remediationPlanService;
    private AiMappingService aiMappingService;
    private AuditOrchestrationService auditOrchestrationService;
    private DashboardReportingService dashboardReportingService;
    private DriftDetectionService driftDetectionService;
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
        templateRepository = factory.getRepository(RemediationTemplateRepository.class);
        remediationPlanRepository = factory.getRepository(RemediationPlanRepository.class);
        aiMappingRepository = factory.getRepository(AiMappingRepository.class);
        aiJobRepository = factory.getRepository(AiJobRepository.class);
        simulationRepository = factory.getRepository(WhatIfSimulationRepository.class);

        // Seed compliance rules and remediation templates
        new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository).seed();
        new NistSp80053RuleSeeder(frameworkRepository, controlRepository, ruleRepository).seed();
        new Iso27001RuleSeeder(frameworkRepository, controlRepository, ruleRepository).seed();
        new RemediationTemplateSeeder(templateRepository).seed();

        stubProvider = new DeterministicStubSuggestionProvider();
        parserService = new ParserServiceImpl(
                List.of(new CiscoIosParser(), new JuniperJunosParser(), new FortinetFortiOSParser(), new PaloAltoPanOsParser()),
                aiMappingRepository
        );

        normalizationService = new NormalizationServiceImpl(parserService, normalizedConfigRepository);
        vendorDetectionService = new VendorFingerprintDetectionService();
        applicabilityChecker = new DefaultRuleApplicabilityChecker();
        ruleEvaluator = new GenericRuleEvaluator();
        findingCreationService = new FindingCreationServiceImpl(findingRepository, ruleRepository, controlRepository, new EvidenceCreationServiceImpl(evidenceRepository), normalizedConfigRepository);
        riskCalculationService = new RiskCalculationServiceImpl(riskRepository);

        remediationPlanService = new RemediationPlanServiceImpl(
                remediationPlanRepository,
                templateRepository,
                normalizedConfigRepository,
                findingRepository
        );

        aiMappingService = new AiMappingServiceImpl(aiMappingRepository, aiJobRepository, stubProvider);

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

        dashboardReportingService = new DashboardReportingServiceImpl(
                auditRepository,
                normalizedConfigRepository,
                findingRepository,
                evidenceRepository,
                riskRepository,
                driftEventRepository,
                remediationPlanRepository,
                aiMappingRepository,
                frameworkRepository,
                templateRepository
        );

        driftDetectionService = new DriftDetectionServiceImpl(
                driftEventRepository,
                normalizedConfigRepository,
                ruleRepository,
                findingRepository,
                riskCalculationService
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

        // Ensure target export directory exists
        OUTPUT_DIR.mkdirs();
    }

    private String readFixture(String filename) throws IOException {
        File file = new File("fixtures", filename);
        if (!file.exists()) {
            file = new File("src/test/resources/fixtures", filename);
        }
        assertThat(file).exists();
        return Files.readString(file.toPath());
    }

    @Test
    @DisplayName("End-to-End Live Demo Scenario: Golden Path Execution & Artifact Export")
    void testEndToEndDemoScenario() throws Exception {
        System.out.println("================================================================================");
        System.out.println("   NEXUS-COMPLY CYBER LAYER — END-TO-END DEMO SCENARIO (GOLDEN PATH)");
        System.out.println("================================================================================");

        // -----------------------------------------------------------------------------------------
        // STEP 1: INITIAL INGESTION (Four Baseline Bad Fixtures)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[DEMO STEP 1] Ingesting 4 baseline non-compliant vendor fixtures...");

        String ciscoBadCfg = readFixture("cisco-ios-xe-bad.cfg");
        String juniperBadCfg = readFixture("juniper-junos-bad.cfg");
        String fortinetBadCfg = readFixture("fortinet-fortios-bad.cfg");
        String paloaltoBadCfg = readFixture("paloalto-panos-bad.cfg");

        Audit ciscoAuditBad = auditOrchestrationService.startAudit(CISCO_DEVICE_ID, "cfg-cisco-bad-01", "v1.0", ciscoBadCfg);
        Audit juniperAuditBad = auditOrchestrationService.startAudit(JUNIPER_DEVICE_ID, "cfg-juniper-bad-01", "v1.0", juniperBadCfg);
        Audit fortinetAuditBad = auditOrchestrationService.startAudit(FORTINET_DEVICE_ID, "cfg-fortinet-bad-01", "v1.0", fortinetBadCfg);
        Audit paloaltoAuditBad = auditOrchestrationService.startAudit(PALOALTO_DEVICE_ID, "cfg-paloalto-bad-01", "v1.0", paloaltoBadCfg);

        assertThat(ciscoAuditBad.getStatus()).isEqualTo("COMPLETED");
        assertThat(juniperAuditBad.getStatus()).isEqualTo("COMPLETED");
        assertThat(fortinetAuditBad.getStatus()).isEqualTo("COMPLETED");
        assertThat(paloaltoAuditBad.getStatus()).isEqualTo("COMPLETED");

        System.out.println(" -> Audits completed for 4 devices across Cisco, Juniper, Fortinet, and Palo Alto.");

        // -----------------------------------------------------------------------------------------
        // STEP 2: INITIAL POSTURE (Fleet Summary & Cisco Device Posture)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[DEMO STEP 2] Fetching initial Fleet Summary and Cisco Device Posture...");

        FleetSummaryResponse fleetSummaryInitial = dashboardReportingService.getFleetSummary();
        DevicePostureResponse ciscoPostureInitial = dashboardReportingService.getDevicePosture(CISCO_DEVICE_ID);

        assertThat(fleetSummaryInitial.getTotalDevices()).isEqualTo(4);
        assertThat(fleetSummaryInitial.getTotalAudits()).isEqualTo(4);
        assertThat(fleetSummaryInitial.getTotalOpenFindings()).isGreaterThan(0);

        assertThat(ciscoPostureInitial.getDeviceId()).isEqualTo(CISCO_DEVICE_ID);
        assertThat(ciscoPostureInitial.getVendor()).isEqualTo("Cisco");
        assertThat(ciscoPostureInitial.getPlatform()).isEqualTo("IOS-XE");
        assertThat(ciscoPostureInitial.getComplianceScore()).isEqualTo(0.0);
        assertThat(ciscoPostureInitial.getOpenFindingsCount()).isEqualTo(3); // Telnet enabled violates CIS, NIST, ISO

        // Export Fleet Summary JSON
        File fleetSummaryFile = new File(OUTPUT_DIR, "fleet_summary_initial.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(fleetSummaryFile, fleetSummaryInitial);
        System.out.println(" -> Exported Fleet Summary JSON to: " + fleetSummaryFile.getAbsolutePath());

        // Export Cisco Posture JSON
        File ciscoPostureFile = new File(OUTPUT_DIR, "cisco_posture_initial.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(ciscoPostureFile, ciscoPostureInitial);
        System.out.println(" -> Exported Cisco Device Posture JSON to: " + ciscoPostureFile.getAbsolutePath());

        // -----------------------------------------------------------------------------------------
        // STEP 3: REMEDIATION GENERATION (Cisco Telnet Fix Plan)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[DEMO STEP 3] Generating automated remediation plan for Cisco Telnet finding...");

        List<FindingDocument> ciscoFindings = findingRepository.findByAuditId(ciscoAuditBad.getId());
        FindingDocument telnetFinding = ciscoFindings.stream()
                .filter(f -> "security.telnet.enabled".equals(f.getCanonicalField()))
                .findFirst()
                .orElseThrow();

        RemediationPlan ciscoRemediationPlan = remediationPlanService.createPlanForFindingId(telnetFinding.getId());
        assertThat(ciscoRemediationPlan).isNotNull();
        assertThat(ciscoRemediationPlan.getStatus()).isEqualTo("PLANNED"); // Never auto-approved
        assertThat(ciscoRemediationPlan.getSteps()).isNotEmpty();

        File remediationPlanFile = new File(OUTPUT_DIR, "cisco_remediation_plan.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(remediationPlanFile, ciscoRemediationPlan);
        System.out.println(" -> Exported Cisco Remediation Plan JSON to: " + remediationPlanFile.getAbsolutePath());

        // -----------------------------------------------------------------------------------------
        // STEP 4: SIMULATION (What-If Impact Analysis)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[DEMO STEP 4] Running What-If Simulation to pre-flight risk delta before deployment...");

        WhatIfSimulationRequest simRequest = new WhatIfSimulationRequest(CISCO_DEVICE_ID, "v1.0");
        simRequest.setName("Demo: Simulate Disabling Telnet on Cisco Router");
        simRequest.setDescription("Evaluate compliance impact of disabling Telnet across CIS, NIST, and ISO controls prior to change execution.");
        simRequest.setCanonicalOverrides(Map.of("security.telnet.enabled", false));
        simRequest.setPersist(true);

        WhatIfSimulationResult simResult = whatIfSimulationService.simulate(simRequest);

        assertThat(simResult.getStatus()).isEqualTo("COMPLETED");
        assertThat(simResult.getBefore().getComplianceScore()).isEqualTo(0.0);
        assertThat(simResult.getAfter().getComplianceScore()).isEqualTo(14.3);
        assertThat(simResult.getBefore().getFailedControls()).isEqualTo(3);
        assertThat(simResult.getAfter().getFailedControls()).isEqualTo(0);
        assertThat(simResult.getFindingDelta()).isEqualTo(-3); // All 3 telnet findings resolved
        assertThat(simResult.getRiskDelta()).isEqualTo(-70);   // Risk reduced from 70 to 0
        assertThat(simResult.getImpact()).isEqualTo("DECREASED");

        File simulationFile = new File(OUTPUT_DIR, "what_if_simulation.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(simulationFile, simResult);
        System.out.println(" -> Exported What-If Simulation JSON to: " + simulationFile.getAbsolutePath());

        // -----------------------------------------------------------------------------------------
        // STEP 5: APPLY FIX & RE-AUDIT (Cisco Good Configuration)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[DEMO STEP 5] Deploying remediation fix and executing re-audit against cisco-ios-xe-good.cfg...");

        String ciscoGoodCfg = readFixture("cisco-ios-xe-good.cfg");
        Audit ciscoAuditGood = auditOrchestrationService.startAudit(CISCO_DEVICE_ID, "cfg-cisco-good-02", "v2.0", ciscoGoodCfg);

        assertThat(ciscoAuditGood.getStatus()).isEqualTo("COMPLETED");
        assertThat(ciscoAuditGood.getSummary().getFailed()).isEqualTo(0);
        assertThat(ciscoAuditGood.getComplianceScore()).isEqualTo(28.6);

        DevicePostureResponse ciscoPostureGood = dashboardReportingService.getDevicePosture(CISCO_DEVICE_ID);
        assertThat(ciscoPostureGood.getOpenFindingsCount()).isEqualTo(0);
        assertThat(ciscoPostureGood.getComplianceScore()).isEqualTo(28.6);
        System.out.println(" -> Re-audit succeeded: Open Findings = 0, Compliance Score = 28.6%");

        // -----------------------------------------------------------------------------------------
        // STEP 6: DRIFT DETECTION (Telemetry between v1.0 and v2.0)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[DEMO STEP 6] Detecting configuration drift between Bad (v1.0) and Good (v2.0)...");

        DriftEvent driftEvent = driftDetectionService.detectDriftByDocumentIds(
                ciscoAuditBad.getNormalizedConfigurationId(),
                ciscoAuditGood.getNormalizedConfigurationId()
        );

        assertThat(driftEvent).isNotNull();
        assertThat(driftEvent.getRiskBefore()).isEqualTo(70);
        assertThat(driftEvent.getRiskAfter()).isEqualTo(0);
        assertThat(driftEvent.getImpact()).isEqualTo("DECREASED");
        assertThat(driftEvent.getChanges()).anyMatch(c ->
                "security.telnet.enabled".equals(c.getCanonicalField()) &&
                "IMPROVED".equals(c.getClassification())
        );

        File driftEventFile = new File(OUTPUT_DIR, "cisco_drift_event.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(driftEventFile, driftEvent);
        System.out.println(" -> Exported Cisco Drift Event JSON to: " + driftEventFile.getAbsolutePath());

        // -----------------------------------------------------------------------------------------
        // STEP 7: AI UNKNOWN-SYNTAX LOOP (Self-Learning Loop)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[DEMO STEP 7] Demonstrating AI Unknown-Syntax capture, suggestion, approval, and learning...");

        String customCommand = "custom-security-feature enable";

        // Register stub for the custom command mapping to security.telnet.enabled = false
        stubProvider.registerStub(
                customCommand,
                new SuggestionResult("security.telnet.enabled", false, 0.95, "Custom security macro disabling legacy management protocols")
        );

        // 1. Unrecognized syntax captured in review queue
        AiMappingDocument pendingMapping = aiMappingService.recordUnknownSyntax("Cisco", "IOS-XE", customCommand);
        assertThat(pendingMapping).isNotNull();
        assertThat(pendingMapping.getStatus()).isEqualTo("PENDING_REVIEW");

        // 2. AI suggests mapping
        AiMappingDocument suggestedMapping = aiMappingService.requestSuggestion(pendingMapping.getId());
        assertThat(suggestedMapping.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(suggestedMapping.getSuggestedBy()).isEqualTo("AI");
        assertThat(suggestedMapping.getCanonicalField()).isEqualTo("security.telnet.enabled");

        // 3. Human reviewer approves mapping
        AiMappingDocument approvedMapping = aiMappingService.approve(suggestedMapping.getId(), "demo-security-admin");
        assertThat(approvedMapping.getStatus()).isEqualTo("APPROVED");
        assertThat(approvedMapping.getReview().getReviewerId()).isEqualTo("demo-security-admin");

        // 4. Re-audit with the custom syntax to prove the parser now understands it
        String ciscoConfigWithLearnedSyntax = String.join("\n",
                "version 17.6",
                "hostname RTR-C",
                customCommand,
                "ip ssh version 2",
                "line vty 0 4",
                " transport input ssh"
        );
        Audit ciscoAuditLearned = auditOrchestrationService.startAudit(CISCO_DEVICE_ID, "cfg-cisco-learned-03", "v3.0", ciscoConfigWithLearnedSyntax);
        assertThat(ciscoAuditLearned.getStatus()).isEqualTo("COMPLETED");
        assertThat(ciscoAuditLearned.getSummary().getFailed()).isEqualTo(0);
        assertThat(ciscoAuditLearned.getComplianceScore()).isEqualTo(28.6);
        System.out.println(" -> AI Loop confirmed: Learned unknown command with explicit human approval.");

        // -----------------------------------------------------------------------------------------
        // STEP 8: FINAL REPORTING (Executive Markdown Report)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[DEMO STEP 8] Generating and exporting final executive Markdown audit report...");

        AuditReportResponse finalAuditReport = dashboardReportingService.getAuditReport(ciscoAuditGood.getId());
        assertThat(finalAuditReport).isNotNull();
        assertThat(finalAuditReport.getMarkdownReport()).isNotEmpty();
        assertThat(finalAuditReport.getMarkdownReport()).contains("# NEXUS-COMPLY Security & Compliance Audit Report");
        assertThat(finalAuditReport.getMarkdownReport()).contains("Executive Posture Summary");
        assertThat(finalAuditReport.getMarkdownReport()).contains("Coverage");
        assertThat(finalAuditReport.getMarkdownReport()).contains("Note on UNKNOWN Controls");

        File reportFile = new File(OUTPUT_DIR, "cisco_final_audit_report.md");
        Files.writeString(reportFile.toPath(), finalAuditReport.getMarkdownReport());
        System.out.println(" -> Exported Final Cisco Audit Report Markdown to: " + reportFile.getAbsolutePath());

        // -----------------------------------------------------------------------------------------
        // SUMMARY ASSERTIONS FOR ALL EXPORTED FILES
        // -----------------------------------------------------------------------------------------
        assertThat(fleetSummaryFile).exists().isNotEmpty();
        assertThat(ciscoPostureFile).exists().isNotEmpty();
        assertThat(remediationPlanFile).exists().isNotEmpty();
        assertThat(simulationFile).exists().isNotEmpty();
        assertThat(driftEventFile).exists().isNotEmpty();
        assertThat(reportFile).exists().isNotEmpty();

        System.out.println("\n================================================================================");
        System.out.println("   ALL 6 DEMO OUTPUT ARTIFACTS EXPORTED SUCCESSFULLY TO: " + OUTPUT_DIR.getAbsolutePath());
        System.out.println("================================================================================");
    }
}
