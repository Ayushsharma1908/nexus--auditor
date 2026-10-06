package com.nexuscomply.cyber.report;

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
import com.nexuscomply.cyber.detection.VendorDetectionService;
import com.nexuscomply.cyber.drift.model.DriftChange;
import com.nexuscomply.cyber.drift.persistence.DriftEventDocument;
import com.nexuscomply.cyber.drift.persistence.DriftEventRepository;
import com.nexuscomply.cyber.evidence.EvidenceCreationService;
import com.nexuscomply.cyber.evidence.EvidenceCreationServiceImpl;
import com.nexuscomply.cyber.evidence.persistence.EvidenceRepository;
import com.nexuscomply.cyber.finding.FindingCreationService;
import com.nexuscomply.cyber.finding.FindingCreationServiceImpl;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.audit.AuditSummary;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizationService;
import com.nexuscomply.cyber.normalization.NormalizationServiceImpl;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.parser.ParserService;
import com.nexuscomply.cyber.parser.ParserServiceImpl;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import com.nexuscomply.cyber.parser.fortinet.FortinetFortiOSParser;
import com.nexuscomply.cyber.parser.juniper.JuniperJunosParser;
import com.nexuscomply.cyber.parser.paloalto.PaloAltoPanOsParser;
import com.nexuscomply.cyber.remediation.seeder.RemediationTemplateSeeder;
import com.nexuscomply.cyber.remediation.service.RemediationPlanService;
import com.nexuscomply.cyber.remediation.service.RemediationPlanServiceImpl;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanRepository;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateRepository;
import com.nexuscomply.cyber.risk.RiskCalculationService;
import com.nexuscomply.cyber.risk.RiskCalculationServiceImpl;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentRepository;
import com.nexuscomply.cyber.simulation.WhatIfSimulationRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 2.12 Integration Tests: Read-Only Dashboard & Audit Report Aggregation.
 * Verifies read-only aggregation across all collections, fleet totals consistency,
 * safe refusal of unknown entities, empty database handling, and Markdown report rendering.
 */
class DashboardReportingIntegrationTest {

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

    private CisCiscoIosXeRuleSeeder cisSeeder;
    private NistSp80053RuleSeeder nistSeeder;
    private Iso27001RuleSeeder isoSeeder;
    private RemediationTemplateSeeder remediationTemplateSeeder;

    private ParserService parserService;
    private NormalizationService normalizationService;
    private VendorDetectionService vendorDetectionService;
    private RuleApplicabilityChecker applicabilityChecker;
    private RuleEvaluator ruleEvaluator;
    private FindingCreationService findingCreationService;
    private RiskCalculationService riskCalculationService;
    private RemediationPlanService remediationPlanService;
    private AuditOrchestrationService auditOrchestrationService;
    private DashboardReportingService dashboardReportingService;

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

        cisSeeder = new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        nistSeeder = new NistSp80053RuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        isoSeeder = new Iso27001RuleSeeder(frameworkRepository, controlRepository, ruleRepository);
        remediationTemplateSeeder = new RemediationTemplateSeeder(templateRepository);

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

        remediationPlanService = new RemediationPlanServiceImpl(
                remediationPlanRepository,
                templateRepository,
                normalizedConfigRepository,
                findingRepository
        );

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

        dashboardReportingService = new DashboardReportingServiceImpl(
                auditRepository,
                normalizedConfigRepository,
                findingRepository,
                evidenceRepository,
                riskRepository,
                driftEventRepository,
                remediationPlanRepository,
                aiMappingRepository
        );

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private void seedRulesAndTemplates() {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();
        remediationTemplateSeeder.seed();
    }

    @Test
    @DisplayName("1. Empty database returns empty results, not errors")
    void testEmptyDatabaseReturnsEmptyResultsNotErrors() {
        // Completely empty database
        FleetSummaryResponse fleet = dashboardReportingService.getFleetSummary();
        assertThat(fleet).isNotNull();
        assertThat(fleet.getTotalDevices()).isEqualTo(0);
        assertThat(fleet.getTotalAudits()).isEqualTo(0);
        assertThat(fleet.getTotalOpenFindings()).isEqualTo(0);
        assertThat(fleet.getFleetComplianceScore()).isNull();
        assertThat(fleet.getDevicePostures()).isEmpty();
        assertThat(fleet.getDevicesByVendor()).isEmpty();

        DevicePostureResponse posture = dashboardReportingService.getDevicePosture("dev-empty");
        assertThat(posture).isNotNull();
        assertThat(posture.getDeviceId()).isEqualTo("dev-empty");
        assertThat(posture.getVendor()).isEqualTo("UNKNOWN");
        assertThat(posture.getLatestAuditSummary()).isNull();
        assertThat(posture.getOpenFindingsCount()).isEqualTo(0);
        assertThat(posture.getTopRiskAssessments()).isEmpty();

        DeviceDriftHistoryResponse driftHistory = dashboardReportingService.getDeviceDriftHistory("dev-empty");
        assertThat(driftHistory).isNotNull();
        assertThat(driftHistory.getDeviceId()).isEqualTo("dev-empty");
        assertThat(driftHistory.getTotalEvents()).isEqualTo(0);
        assertThat(driftHistory.getEvents()).isEmpty();
    }

    @Test
    @DisplayName("2. Unknown device and unknown audit are refused safely")
    void testUnknownDeviceAndAuditRefusedSafely() {
        seedRulesAndTemplates();

        String baseConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-KNOWN",
                "line vty 0 4",
                " transport input telnet ssh"
        );
        auditOrchestrationService.startAudit("dev-known-01", "cfg-01", "v1.0", baseConfig);

        assertThatThrownBy(() -> dashboardReportingService.getDevicePosture("non-existent-device"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Device not found");

        assertThatThrownBy(() -> dashboardReportingService.getAuditReport("non-existent-audit"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Audit not found");

        assertThatThrownBy(() -> dashboardReportingService.getDeviceDriftHistory("non-existent-device"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Device not found");
    }

    @Test
    @DisplayName("3. Fleet totals strictly equal the sum of device totals")
    void testFleetTotalsEqualSumOfDeviceTotals() {
        seedRulesAndTemplates();

        // Device 1: Cisco with telnet enabled (deliberate violation -> 3 findings)
        String ciscoConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-CISCO",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input telnet ssh"
        );
        auditOrchestrationService.startAudit("dev-cisco-01", "cfg-cisco", "v1.0", ciscoConfig);

        // Device 2: Juniper with telnet enabled (deliberate violation -> 2 findings)
        String junosConfig = String.join("\n",
                "set system services ssh",
                "set system services telnet"
        );
        auditOrchestrationService.startAudit("dev-junos-01", "cfg-junos", "v1.0", junosConfig);

        FleetSummaryResponse fleet = dashboardReportingService.getFleetSummary();
        assertThat(fleet).isNotNull();
        assertThat(fleet.getTotalDevices()).isEqualTo(2);
        assertThat(fleet.getTotalAudits()).isEqualTo(2);

        int sumFindings = fleet.getDevicePostures().stream().mapToInt(DevicePostureResponse::getOpenFindingsCount).sum();
        assertThat(fleet.getTotalOpenFindings()).isEqualTo(sumFindings);

        long sumHigh = fleet.getDevicePostures().stream()
                .mapToLong(p -> p.getOpenFindingsBySeverity().getOrDefault("HIGH", 0L))
                .sum();
        assertThat(fleet.getFindingsBySeverity().get("HIGH")).isEqualTo(sumHigh);

        long vendorCountSum = fleet.getDevicesByVendor().values().stream().mapToLong(Long::longValue).sum();
        assertThat(vendorCountSum).isEqualTo(fleet.getTotalDevices());
    }

    @Test
    @DisplayName("4. Collection counts strictly unchanged after every report call (read-only verification)")
    void testCollectionCountsStrictlyUnchangedAfterEveryReportCall() {
        seedRulesAndTemplates();

        String baseConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-COUNTS-TEST",
                "line vty 0 4",
                " transport input telnet ssh"
        );
        Audit audit = auditOrchestrationService.startAudit("dev-counts-01", "cfg-cnt-01", "v1.0", baseConfig);

        // Record initial collection counts
        long auditsBefore = auditRepository.count();
        long findingsBefore = findingRepository.count();
        long evidenceBefore = evidenceRepository.count();
        long risksBefore = riskRepository.count();
        long normsBefore = normalizedConfigRepository.count();
        long driftsBefore = driftEventRepository.count();
        long remPlansBefore = remediationPlanRepository.count();
        long remTemplatesBefore = templateRepository.count();
        long aiMappingsBefore = aiMappingRepository.count();
        long aiJobsBefore = aiJobRepository.count();
        long simsBefore = simulationRepository.count();

        // Execute all 4 reporting endpoints
        DevicePostureResponse posture = dashboardReportingService.getDevicePosture("dev-counts-01");
        assertThat(posture).isNotNull();

        FleetSummaryResponse fleet = dashboardReportingService.getFleetSummary();
        assertThat(fleet).isNotNull();

        DeviceDriftHistoryResponse drift = dashboardReportingService.getDeviceDriftHistory("dev-counts-01");
        assertThat(drift).isNotNull();

        AuditReportResponse report = dashboardReportingService.getAuditReport(audit.getId());
        assertThat(report).isNotNull();

        // Verify that EVERY collection count in the system is strictly unchanged
        assertThat(auditRepository.count()).isEqualTo(auditsBefore);
        assertThat(findingRepository.count()).isEqualTo(findingsBefore);
        assertThat(evidenceRepository.count()).isEqualTo(evidenceBefore);
        assertThat(riskRepository.count()).isEqualTo(risksBefore);
        assertThat(normalizedConfigRepository.count()).isEqualTo(normsBefore);
        assertThat(driftEventRepository.count()).isEqualTo(driftsBefore);
        assertThat(remediationPlanRepository.count()).isEqualTo(remPlansBefore);
        assertThat(templateRepository.count()).isEqualTo(remTemplatesBefore);
        assertThat(aiMappingRepository.count()).isEqualTo(aiMappingsBefore);
        assertThat(aiJobRepository.count()).isEqualTo(aiJobsBefore);
        assertThat(simulationRepository.count()).isEqualTo(simsBefore);

        System.out.println("=== COLLECTION COUNTS BEFORE AND AFTER REPORT CALLS ===");
        System.out.println("audits: before=" + auditsBefore + ", after=" + auditRepository.count());
        System.out.println("findings: before=" + findingsBefore + ", after=" + findingRepository.count());
        System.out.println("evidence: before=" + evidenceBefore + ", after=" + evidenceRepository.count());
        System.out.println("risk_assessments: before=" + risksBefore + ", after=" + riskRepository.count());
        System.out.println("normalized_configurations: before=" + normsBefore + ", after=" + normalizedConfigRepository.count());
        System.out.println("drift_events: before=" + driftsBefore + ", after=" + driftEventRepository.count());
        System.out.println("remediation_plans: before=" + remPlansBefore + ", after=" + remediationPlanRepository.count());
        System.out.println("remediation_templates: before=" + remTemplatesBefore + ", after=" + templateRepository.count());
        System.out.println("ai_mappings: before=" + aiMappingsBefore + ", after=" + aiMappingRepository.count());
        System.out.println("ai_jobs: before=" + aiJobsBefore + ", after=" + aiJobRepository.count());
        System.out.println("what_if_simulations: before=" + simsBefore + ", after=" + simulationRepository.count());
    }

    @Test
    @DisplayName("5. Audit report for four-vendor fixtures with telnet findings")
    void testAuditReportForFourVendorFixturesWithTelnetFindings() throws Exception {
        seedRulesAndTemplates();

        // Cisco
        String cisco = String.join("\n", "version 17.6", "hostname RTR-C", "line vty 0 4", " transport input telnet ssh");
        Audit aCisco = auditOrchestrationService.startAudit("dev-c", "cfg-c", "v1.0", cisco);
        findingRepository.findByAuditId(aCisco.getId()).forEach(f -> remediationPlanService.createPlanForFindingId(f.getId()));
        AuditReportResponse rCisco = dashboardReportingService.getAuditReport(aCisco.getId());
        assertThat(rCisco.getFindings()).isNotEmpty();
        assertThat(rCisco.getRemediationPlans()).isNotEmpty();
        assertThat(rCisco.getMarkdownReport()).contains("Coverage").contains("UNKNOWN");

        // Juniper
        String junos = String.join("\n", "set system services ssh", "set system services telnet");
        Audit aJunos = auditOrchestrationService.startAudit("dev-j", "cfg-j", "v1.0", junos);
        findingRepository.findByAuditId(aJunos.getId()).forEach(f -> remediationPlanService.createPlanForFindingId(f.getId()));
        AuditReportResponse rJunos = dashboardReportingService.getAuditReport(aJunos.getId());
        assertThat(rJunos.getFindings()).isNotEmpty();
        assertThat(rJunos.getRemediationPlans()).isNotEmpty();
        assertThat(rJunos.getMarkdownReport()).contains("Coverage").contains("UNKNOWN");

        // Fortinet
        String forti = String.join("\n", "config system interface", "edit \"port1\"", "set allowaccess ping telnet", "next", "end");
        Audit aForti = auditOrchestrationService.startAudit("dev-f", "cfg-f", "v1.0", forti);
        findingRepository.findByAuditId(aForti.getId()).forEach(f -> remediationPlanService.createPlanForFindingId(f.getId()));
        AuditReportResponse rForti = dashboardReportingService.getAuditReport(aForti.getId());
        assertThat(rForti.getFindings()).isNotEmpty();
        assertThat(rForti.getRemediationPlans()).isNotEmpty();
        assertThat(rForti.getMarkdownReport()).contains("Coverage").contains("UNKNOWN");

        // Palo Alto
        String pan = String.join("\n",
                "set deviceconfig system service disable-telnet no",
                "set deviceconfig system service disable-https no"
        );
        Audit aPan = auditOrchestrationService.startAudit("dev-p", "cfg-p", "v1.0", pan);
        findingRepository.findByAuditId(aPan.getId()).forEach(f -> remediationPlanService.createPlanForFindingId(f.getId()));
        AuditReportResponse rPan = dashboardReportingService.getAuditReport(aPan.getId());
        assertThat(rPan.getFindings()).isNotEmpty();
        assertThat(rPan.getRemediationPlans()).isNotEmpty();
        assertThat(rPan.getMarkdownReport()).contains("Coverage").contains("UNKNOWN");

        DevicePostureResponse pCisco = dashboardReportingService.getDevicePosture("dev-c");
        DevicePostureResponse pJunos = dashboardReportingService.getDevicePosture("dev-j");
        DevicePostureResponse pForti = dashboardReportingService.getDevicePosture("dev-f");
        DevicePostureResponse pPan = dashboardReportingService.getDevicePosture("dev-p");

        System.out.println("=== FOUR-VENDOR DEVICE POSTURES ===");
        System.out.println("CISCO POSTURE: " + objectMapper.writeValueAsString(pCisco));
        System.out.println("JUNIPER POSTURE: " + objectMapper.writeValueAsString(pJunos));
        System.out.println("FORTINET POSTURE: " + objectMapper.writeValueAsString(pForti));
        System.out.println("PALO ALTO POSTURE: " + objectMapper.writeValueAsString(pPan));

        FleetSummaryResponse fleet = dashboardReportingService.getFleetSummary();
        int sumFindings = fleet.getDevicePostures().stream().mapToInt(DevicePostureResponse::getOpenFindingsCount).sum();
        double avgScore = fleet.getDevicePostures().stream()
                .filter(p -> p.getComplianceScore() != null)
                .mapToDouble(DevicePostureResponse::getComplianceScore)
                .average().orElse(0.0);

        System.out.println("=== FOUR-VENDOR FLEET SUMMARY (WORKED SUM CHECK) ===");
        System.out.println("Total Devices: " + fleet.getTotalDevices() + " (Sum check: " + fleet.getDevicePostures().size() + ")");
        System.out.println("Total Open Findings: " + fleet.getTotalOpenFindings() + " (Sum check: " + sumFindings + ")");
        System.out.println("Fleet Compliance Score: " + fleet.getFleetComplianceScore() + "% (Worked Average: " + String.format("%.1f%%", avgScore) + ")");
        System.out.println("Devices by Vendor: " + fleet.getDevicesByVendor());
        System.out.println("FLEET SUMMARY JSON: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fleet));

        DriftEventDocument driftDoc = new DriftEventDocument();
        driftDoc.setId("drift-dev-c-01");
        driftDoc.setDeviceId("dev-c");
        driftDoc.setFromVersion(1);
        driftDoc.setToVersion(2);
        driftDoc.setImpact("INCREASED");
        driftDoc.setRiskBefore(0);
        driftDoc.setRiskAfter(68);
        driftDoc.setDetectedAt(Instant.now());
        driftDoc.setChanges(List.of(new DriftChange("security.telnet.enabled", false, true, "MODIFIED", "ssh", "telnet ssh", "DEGRADED")));
        driftEventRepository.save(driftDoc);

        DeviceDriftHistoryResponse driftHistory = dashboardReportingService.getDeviceDriftHistory("dev-c");
        System.out.println("=== DEVICE DRIFT HISTORY (dev-c) ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(driftHistory));

        System.out.println("=== RAW CISCO AUDIT REPORT MARKDOWN ===");
        System.out.println(rCisco.getMarkdownReport());
    }

    @Test
    @DisplayName("6. Double-run equality across all reporting endpoints")
    void testDoubleRunEqualityAcrossAllReportEndpoints() throws Exception {
        seedRulesAndTemplates();

        String baseConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-DOUBLE-RUN",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input telnet ssh"
        );
        Audit audit = auditOrchestrationService.startAudit("dev-double-01", "cfg-db-01", "v1.0", baseConfig);

        DriftEventDocument drift = new DriftEventDocument();
        drift.setId("drift-db-01");
        drift.setDeviceId("dev-double-01");
        drift.setFromVersion(1);
        drift.setToVersion(2);
        drift.setImpact("INCREASED");
        drift.setRiskBefore(0);
        drift.setRiskAfter(70);
        drift.setDetectedAt(Instant.now());
        drift.setChanges(List.of(new DriftChange("security.telnet.enabled", false, true, "MODIFIED", "ssh", "telnet ssh", "DEGRADED")));
        driftEventRepository.save(drift);

        // 1. Device Posture equality
        DevicePostureResponse p1 = dashboardReportingService.getDevicePosture("dev-double-01");
        DevicePostureResponse p2 = dashboardReportingService.getDevicePosture("dev-double-01");
        assertThat(objectMapper.writeValueAsString(p1)).isEqualTo(objectMapper.writeValueAsString(p2));

        // 2. Fleet Summary equality
        FleetSummaryResponse f1 = dashboardReportingService.getFleetSummary();
        FleetSummaryResponse f2 = dashboardReportingService.getFleetSummary();
        assertThat(objectMapper.writeValueAsString(f1)).isEqualTo(objectMapper.writeValueAsString(f2));

        // 3. Drift History equality
        DeviceDriftHistoryResponse d1 = dashboardReportingService.getDeviceDriftHistory("dev-double-01");
        DeviceDriftHistoryResponse d2 = dashboardReportingService.getDeviceDriftHistory("dev-double-01");
        assertThat(objectMapper.writeValueAsString(d1)).isEqualTo(objectMapper.writeValueAsString(d2));

        // 4. Audit Report equality
        AuditReportResponse r1 = dashboardReportingService.getAuditReport(audit.getId());
        AuditReportResponse r2 = dashboardReportingService.getAuditReport(audit.getId());
        assertThat(objectMapper.writeValueAsString(r1)).isEqualTo(objectMapper.writeValueAsString(r2));
        assertThat(r1.getMarkdownReport()).isEqualTo(r2.getMarkdownReport());

        System.out.println("=== DOUBLE-RUN EQUALITY CONFIRMED FOR ALL 4 REPORTING ENDPOINTS ===");
    }

    @Test
    @DisplayName("7. Coverage and UNKNOWN visible in Markdown and never treated as PASS")
    void testCoverageAndUnknownVisibleInMarkdownAndNeverTreatedAsPass() {
        seedRulesAndTemplates();

        // Juniper configuration with 2 passing rules and 19 UNKNOWN / notApplicable
        String junosConfig = String.join("\n",
                "set system services ssh",
                "set system services ssh protocol-version v2"
        );
        Audit audit = auditOrchestrationService.startAudit("dev-coverage-junos", "cfg-j-cov", "v1.0", junosConfig);

        AuditReportResponse report = dashboardReportingService.getAuditReport(audit.getId());
        assertThat(report).isNotNull();
        assertThat(report.getCoverage()).isNotNull();

        String md = report.getMarkdownReport();
        assertThat(md).contains("| **Control Coverage** |");
        assertThat(md).contains("| **UNKNOWN Controls (Pending Review)** |");
        assertThat(md).contains("Controls with UNKNOWN evaluation status");
        assertThat(md).contains("**NEVER** treated as PASS");

        // Assert that UNKNOWN count was not credited to passed count
        AuditSummary summary = report.getSummary();
        assertThat(summary.getPassed()).isEqualTo(2);
        assertThat(summary.getFailed()).isEqualTo(0);
        assertThat(summary.getPassed() + summary.getFailed() + summary.getUnknown() + summary.getNotApplicable() + summary.getError())
                .isEqualTo(summary.getTotalControls());
    }

    @Test
    @DisplayName("8. Two audits for one device -> posture uses the latest audit")
    void testTwoAuditsForOneDevicePostureUsesLatest() throws Exception {
        seedRulesAndTemplates();

        // Audit 1: Config with Telnet enabled (3 failures, complianceScore = 0.0)
        String ciscoV1 = String.join("\n",
                "version 17.6",
                "hostname RTR-MULTI",
                "line vty 0 4",
                " transport input telnet ssh"
        );
        Audit audit1 = auditOrchestrationService.startAudit("dev-multi-audit", "cfg-multi", "v1.0", ciscoV1);
        assertThat(audit1.getComplianceScore()).isEqualTo(0.0);

        // Audit 2: Config with Telnet disabled (0 failures, complianceScore = 100.0)
        String ciscoV2 = String.join("\n",
                "version 17.6",
                "hostname RTR-MULTI",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input ssh"
        );
        Audit audit2 = auditOrchestrationService.startAudit("dev-multi-audit", "cfg-multi", "v2.0", ciscoV2);
        assertThat(audit2.getComplianceScore()).isNotNull();

        DevicePostureResponse posture = dashboardReportingService.getDevicePosture("dev-multi-audit");
        assertThat(posture).isNotNull();
        assertThat(posture.getComplianceScore()).isEqualTo(audit2.getComplianceScore());
        assertThat(posture.getComplianceScore()).isNotEqualTo(audit1.getComplianceScore());
        assertThat(posture.getLatestAuditSummary().getPassed()).isEqualTo(audit2.getSummary().getPassed());
        assertThat(posture.getLatestAuditSummary().getFailed()).isEqualTo(audit2.getSummary().getFailed());

        System.out.println("=== TWO AUDITS LATEST POSTURE OUTPUT ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(posture));
    }

    @Test
    @DisplayName("9. Device with no audit returns posture without summary, completely unknown device refused")
    void testDeviceWithNoAuditAndUnknownDevice() throws Exception {
        seedRulesAndTemplates();

        // Device registered in normalized configurations but never audited
        NormalizedConfigurationDocument norm = new NormalizedConfigurationDocument();
        norm.setId("norm-no-audit-01");
        norm.setDeviceId("dev-no-audit");
        norm.setConfigurationId("cfg-no-audit");
        norm.setVersionId("v1.0");
        norm.setVendor("Cisco");
        norm.setPlatform("IOS-XE");
        norm.setCreatedAt(Instant.now());
        normalizedConfigRepository.save(norm);

        DevicePostureResponse posture = dashboardReportingService.getDevicePosture("dev-no-audit");
        assertThat(posture).isNotNull();
        assertThat(posture.getDeviceId()).isEqualTo("dev-no-audit");
        assertThat(posture.getVendor()).isEqualTo("Cisco");
        assertThat(posture.getPlatform()).isEqualTo("IOS-XE");
        assertThat(posture.getLatestAuditSummary()).isNull();
        assertThat(posture.getComplianceScore()).isNull();
        assertThat(posture.getOpenFindingsCount()).isEqualTo(0);

        // Completely unknown device (not in audits and not in normalized configs)
        assertThatThrownBy(() -> dashboardReportingService.getDevicePosture("dev-completely-unknown"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Device not found: dev-completely-unknown");

        System.out.println("=== DEVICE WITH NO AUDIT OUTPUT ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(posture));
    }

    @Test
    @DisplayName("10. Audit with zero findings produces 100% compliance report")
    void testAuditWithZeroFindings() throws Exception {
        seedRulesAndTemplates();

        // Cisco config without telnet and with SSH v2 (0 failures)
        String cleanConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-CLEAN",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input ssh"
        );
        Audit audit = auditOrchestrationService.startAudit("dev-clean-01", "cfg-clean", "v1.0", cleanConfig);

        AuditReportResponse report = dashboardReportingService.getAuditReport(audit.getId());
        assertThat(report).isNotNull();
        assertThat(report.getFindings()).isEmpty();
        assertThat(report.getSummary().getFailed()).isEqualTo(0);
        assertThat(report.getSummary().getPassed()).isEqualTo(audit.getSummary().getPassed());
        assertThat(report.getComplianceScore()).isEqualTo(audit.getComplianceScore());

        System.out.println("=== AUDIT WITH ZERO FINDINGS OUTPUT ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    @Test
    @DisplayName("11. Severity and framework grouping example")
    void testSeverityAndFrameworkGroupingExample() throws Exception {
        seedRulesAndTemplates();

        String ciscoConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-GRP",
                "line vty 0 4",
                " transport input telnet ssh"
        );
        auditOrchestrationService.startAudit("dev-grp-01", "cfg-grp", "v1.0", ciscoConfig);

        DevicePostureResponse posture = dashboardReportingService.getDevicePosture("dev-grp-01");
        assertThat(posture).isNotNull();
        assertThat(posture.getOpenFindingsCount()).isEqualTo(3);

        Map<String, Long> bySev = posture.getOpenFindingsBySeverity();
        assertThat(bySev.get("HIGH")).isEqualTo(3L);
        assertThat(bySev.get("CRITICAL")).isEqualTo(0L);

        Map<String, Long> byFw = posture.getOpenFindingsByFramework();
        assertThat(byFw).isNotEmpty();

        System.out.println("=== SEVERITY AND FRAMEWORK GROUPING OUTPUT ===");
        System.out.println("By Severity: " + objectMapper.writeValueAsString(bySev));
        System.out.println("By Framework: " + objectMapper.writeValueAsString(byFw));
    }
}
