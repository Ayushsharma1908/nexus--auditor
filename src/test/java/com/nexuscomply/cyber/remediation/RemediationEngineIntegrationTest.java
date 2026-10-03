package com.nexuscomply.cyber.remediation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClients;
import com.nexuscomply.cyber.audit.Audit;
import com.nexuscomply.cyber.audit.AuditOrchestrationServiceImpl;
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
import com.nexuscomply.cyber.detection.VendorDetectionService;
import com.nexuscomply.cyber.detection.VendorFingerprintDetectionService;
import com.nexuscomply.cyber.evidence.EvidenceCreationService;
import com.nexuscomply.cyber.evidence.EvidenceCreationServiceImpl;
import com.nexuscomply.cyber.evidence.persistence.EvidenceDocument;
import com.nexuscomply.cyber.evidence.persistence.EvidenceRepository;
import com.nexuscomply.cyber.finding.Finding;
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
import com.nexuscomply.cyber.remediation.model.CommandType;
import com.nexuscomply.cyber.remediation.model.RemediationPlan;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanDocument;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanRepository;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateDocument;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateRepository;
import com.nexuscomply.cyber.remediation.seeder.RemediationTemplateSeeder;
import com.nexuscomply.cyber.remediation.service.RemediationPlanService;
import com.nexuscomply.cyber.remediation.service.RemediationPlanServiceImpl;
import com.nexuscomply.cyber.remediation.service.RemediationVerificationResult;
import com.nexuscomply.cyber.remediation.service.RemediationVerificationService;
import com.nexuscomply.cyber.remediation.service.RemediationVerificationServiceImpl;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RemediationEngineIntegrationTest {

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

    private RemediationTemplateRepository templateRepository;
    private RemediationPlanRepository planRepository;

    private RemediationTemplateSeeder templateSeeder;
    private RemediationPlanService planService;
    private RemediationVerificationService verificationService;

    private CisCiscoIosXeRuleSeeder cisSeeder;
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
        mongoTemplate.dropCollection(RemediationTemplateDocument.class);
        mongoTemplate.dropCollection(RemediationPlanDocument.class);

        MongoRepositoryFactory repositoryFactory = new MongoRepositoryFactory(mongoTemplate);
        frameworkRepository = repositoryFactory.getRepository(FrameworkRepository.class);
        controlRepository = repositoryFactory.getRepository(ControlRepository.class);
        ruleRepository = repositoryFactory.getRepository(ComplianceRuleRepository.class);
        normalizedConfigRepository = repositoryFactory.getRepository(NormalizedConfigurationRepository.class);
        findingRepository = repositoryFactory.getRepository(FindingRepository.class);
        evidenceRepository = repositoryFactory.getRepository(EvidenceRepository.class);
        auditRepository = repositoryFactory.getRepository(AuditRepository.class);
        riskAssessmentRepository = repositoryFactory.getRepository(RiskAssessmentRepository.class);

        templateRepository = repositoryFactory.getRepository(RemediationTemplateRepository.class);
        planRepository = repositoryFactory.getRepository(RemediationPlanRepository.class);

        templateSeeder = new RemediationTemplateSeeder(templateRepository);
        planService = new RemediationPlanServiceImpl(planRepository, templateRepository, normalizedConfigRepository, findingRepository);
        verificationService = new RemediationVerificationServiceImpl();

        cisSeeder = new CisCiscoIosXeRuleSeeder(frameworkRepository, controlRepository, ruleRepository);

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
    @DisplayName("Seeds exactly 28 templates across 4 vendors x 7 canonical fields with correct platform gaps and syntax grounding")
    void testSeeder_SeedsExactly28TemplatesWithPlatformGapsAndCommandTypeDistinctions() {
        List<RemediationTemplateDocument> seeded = templateSeeder.seed();
        assertThat(seeded).hasSize(28);
        assertThat(templateRepository.count()).isEqualTo(28);

        Set<String> vendors = seeded.stream().map(RemediationTemplateDocument::getVendor).collect(Collectors.toSet());
        assertThat(vendors).containsExactlyInAnyOrder("Cisco", "Juniper", "Fortinet", "Palo Alto");

        List<String> canonicalFields = List.of(
                "authentication.aaa",
                "security.telnet.enabled",
                "security.ssh.version",
                "security.snmp.version",
                "logging.syslog",
                "logging.localLogging",
                "ntp.configured"
        );

        for (String vendor : vendors) {
            List<RemediationTemplateDocument> vendorTemplates = seeded.stream()
                    .filter(t -> t.getVendor().equals(vendor))
                    .toList();
            assertThat(vendorTemplates).hasSize(7);
            Set<String> fields = vendorTemplates.stream().map(RemediationTemplateDocument::getCanonicalField).collect(Collectors.toSet());
            assertThat(fields).containsExactlyInAnyOrderElementsOf(canonicalFields);
        }

        // Platform Gap 1: Fortinet security.ssh.version
        RemediationTemplateDocument fortinetSsh = templateRepository
                .findByVendorAndPlatformAndCanonicalField("Fortinet", "FortiOS", "security.ssh.version")
                .orElseThrow();
        assertThat(fortinetSsh.getCommandType()).isEqualTo(CommandType.PLATFORM_GAP);
        assertThat(fortinetSsh.getCommands()).isEmpty();
        assertThat(fortinetSsh.getGapExplanation()).isEqualTo("not configurable on this platform");

        // Platform Gap 2: Palo Alto logging.localLogging
        RemediationTemplateDocument paloAltoLocalLogging = templateRepository
                .findByVendorAndPlatformAndCanonicalField("Palo Alto", "PAN-OS", "logging.localLogging")
                .orElseThrow();
        assertThat(paloAltoLocalLogging.getCommandType()).isEqualTo(CommandType.PLATFORM_GAP);
        assertThat(paloAltoLocalLogging.getCommands()).isEmpty();
        assertThat(paloAltoLocalLogging.getGapExplanation()).isEqualTo("not determinable from configuration evidence");

        // Syntax grounding: (a)-type regex derivable vs (b)-type representative example
        RemediationTemplateDocument ciscoSsh = templateRepository
                .findByVendorAndPlatformAndCanonicalField("Cisco", "IOS-XE", "security.ssh.version")
                .orElseThrow();
        assertThat(ciscoSsh.getCommandType()).isEqualTo(CommandType.DERIVABLE_REGEX);
        assertThat(ciscoSsh.getCommands()).containsExactly("ip ssh version 2");

        RemediationTemplateDocument ciscoSnmp = templateRepository
                .findByVendorAndPlatformAndCanonicalField("Cisco", "IOS-XE", "security.snmp.version")
                .orElseThrow();
        assertThat(ciscoSnmp.getCommandType()).isEqualTo(CommandType.REPRESENTATIVE_EXAMPLE);
        assertThat(ciscoSnmp.getCommands()).containsExactly("snmp-server group <name> v3 priv");
    }

    @Test
    @DisplayName("Seeder is fully idempotent: running twice preserves 28 count and document IDs")
    void testSeeder_IsIdempotentOnSubsequentRuns() {
        List<RemediationTemplateDocument> firstRun = templateSeeder.seed();
        assertThat(firstRun).hasSize(28);
        assertThat(templateRepository.count()).isEqualTo(28);

        Map<String, String> firstRunIds = firstRun.stream()
                .collect(Collectors.toMap(t -> t.getVendor() + "|" + t.getCanonicalField(), RemediationTemplateDocument::getId));

        List<RemediationTemplateDocument> secondRun = templateSeeder.seed();
        assertThat(secondRun).hasSize(28);
        assertThat(templateRepository.count()).isEqualTo(28);

        for (RemediationTemplateDocument doc : secondRun) {
            String key = doc.getVendor() + "|" + doc.getCanonicalField();
            assertThat(doc.getId()).isEqualTo(firstRunIds.get(key));
        }
    }

    @Test
    @DisplayName("Persisted documents conform field-by-field to schema1.md Section 15 and Section 16")
    void testRawDocumentFieldParityWithSchema1() throws Exception {
        templateSeeder.seed();
        RemediationTemplateDocument tplDoc = templateRepository
                .findByVendorAndPlatformAndCanonicalField("Cisco", "IOS-XE", "security.ssh.version")
                .orElseThrow();

        // Raw MongoDB document check for Section 15 (remediation_templates)
        Document rawTpl = mongoTemplate.getCollection("remediation_templates").find().first();
        assertThat(rawTpl).isNotNull();
        assertThat(rawTpl.containsKey("_id")).isTrue();
        assertThat(rawTpl.containsKey("vendor")).isTrue();
        assertThat(rawTpl.containsKey("platform")).isTrue();
        assertThat(rawTpl.containsKey("title")).isTrue();
        assertThat(rawTpl.containsKey("commands")).isTrue();
        assertThat(rawTpl.containsKey("description")).isTrue();
        assertThat(rawTpl.containsKey("preconditions")).isTrue();
        assertThat(rawTpl.containsKey("verification")).isTrue();
        assertThat(rawTpl.containsKey("risk")).isTrue();
        assertThat(rawTpl.containsKey("status")).isTrue();
        assertThat(rawTpl.containsKey("version")).isTrue();
        assertThat(rawTpl.containsKey("createdAt")).isTrue();
        assertThat(rawTpl.containsKey("updatedAt")).isTrue();

        // Create a plan and inspect raw MongoDB document for Section 16 (remediation_plans)
        Finding finding = new Finding();
        finding.setId("find-cisco-telnet-01");
        finding.setDeviceId("dev-cisco-01");
        finding.setConfigurationId("cfg-cisco-01");
        finding.setCanonicalField("security.telnet.enabled");
        finding.setExpected(false);
        finding.setActual(true);

        NormalizedConfigurationDocument normDoc = new NormalizedConfigurationDocument();
        normDoc.setId("norm-01");
        normDoc.setConfigurationId("cfg-cisco-01");
        normDoc.setDeviceId("dev-cisco-01");
        normDoc.setVendor("Cisco");
        normDoc.setPlatform("IOS-XE");
        normalizedConfigRepository.save(normDoc);

        RemediationPlan plan = planService.createPlanForFinding(finding);
        assertThat(plan).isNotNull();

        Document rawPlan = mongoTemplate.getCollection("remediation_plans").find().first();
        assertThat(rawPlan).isNotNull();
        assertThat(rawPlan.containsKey("_id")).isTrue();
        assertThat(rawPlan.containsKey("findingId")).isTrue();
        assertThat(rawPlan.containsKey("deviceId")).isTrue();
        assertThat(rawPlan.containsKey("templateId")).isTrue();
        assertThat(rawPlan.containsKey("status")).isTrue();
        assertThat(rawPlan.getString("status")).isEqualTo("PLANNED"); // NON-APPROVED
        assertThat(rawPlan.containsKey("steps")).isTrue();
        assertThat(rawPlan.containsKey("validation")).isTrue();
        assertThat(rawPlan.containsKey("verification")).isTrue();
        assertThat(rawPlan.containsKey("createdBy")).isTrue();
        assertThat(rawPlan.containsKey("createdAt")).isTrue();
        assertThat(rawPlan.containsKey("updatedAt")).isTrue();

        System.out.println("=== RAW PERSISTED TEMPLATE (schema1.md Sec 15) ===");
        System.out.println(rawTpl.toJson());
        System.out.println("=== RAW PERSISTED PLAN (schema1.md Sec 16) ===");
        System.out.println(rawPlan.toJson());
    }

    @Test
    @DisplayName("End-to-end proof: Cisco Telnet Finding -> Plan created in PLANNED non-approved state -> Verification positive and negative")
    void testEndToEndCiscoTelnetRemediationAndVerification() {
        templateSeeder.seed();
        cisSeeder.seed();

        // 1. Audit failing Cisco config (Telnet enabled on VTY)
        String failingConfig = String.join("\n",
                "hostname cisco-edge-01",
                "version 17.6",
                "aaa new-model",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input telnet",
                "logging buffered 16384",
                "ntp server 10.0.0.1"
        );

        String deviceId = "dev-cisco-telnet-fail";
        String configurationId = "cfg-cisco-fail-001";
        String versionId = "ver-cisco-fail-001";

        Audit failingAudit = auditOrchestrationService.startAudit(deviceId, configurationId, versionId, failingConfig);
        assertThat(failingAudit).isNotNull();

        // 2. Fetch the real Finding for security.telnet.enabled
        List<FindingDocument> findingDocs = findingRepository.findByDeviceId(deviceId);
        FindingDocument telnetFindingDoc = findingDocs.stream()
                .filter(f -> "security.telnet.enabled".equals(f.getCanonicalField()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected finding for security.telnet.enabled"));

        assertThat(telnetFindingDoc.getExpected()).isEqualTo(false);
        assertThat(telnetFindingDoc.getActual()).isEqualTo(true);
        assertThat(telnetFindingDoc.getControlCode()).isEqualTo("CIS-1.2.2");

        Finding telnetFinding = new Finding();
        telnetFinding.setId(telnetFindingDoc.getId());
        telnetFinding.setDeviceId(telnetFindingDoc.getDeviceId());
        telnetFinding.setConfigurationId(telnetFindingDoc.getConfigurationId());
        telnetFinding.setCanonicalField(telnetFindingDoc.getCanonicalField());
        telnetFinding.setExpected(telnetFindingDoc.getExpected());
        telnetFinding.setActual(telnetFindingDoc.getActual());

        // 3. Create RemediationPlan via RemediationPlanService
        RemediationPlan plan = planService.createPlanForFinding(telnetFinding);

        // Verify linkage and non-approved initial state
        assertThat(plan).isNotNull();
        assertThat(plan.getFindingId()).isEqualTo(telnetFinding.getId());
        assertThat(plan.getDeviceId()).isEqualTo(telnetFinding.getDeviceId());
        assertThat(plan.getStatus()).isEqualTo("PLANNED"); // Absolute Rule 6: NEVER auto-approved
        assertThat(plan.getValidation().getStatus()).isEqualTo("PENDING");
        assertThat(plan.getVerification().getStatus()).isEqualTo("PENDING");

        // Verify matched Cisco template real steps: "line vty 0 4" then "transport input ssh"
        assertThat(plan.getSteps()).hasSize(2);
        assertThat(plan.getSteps().get(0).getOrder()).isEqualTo(1);
        assertThat(plan.getSteps().get(0).getCommand()).isEqualTo("line vty 0 4");
        assertThat(plan.getSteps().get(1).getOrder()).isEqualTo(2);
        assertThat(plan.getSteps().get(1).getCommand()).isEqualTo("transport input ssh");

        // 4. Positive verification: Construct remediated config (telnet disabled, ssh enforced)
        String remediatedConfig = String.join("\n",
                "hostname cisco-edge-01",
                "version 17.6",
                "aaa new-model",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input ssh",
                "logging buffered 16384",
                "ntp server 10.0.0.1"
        );

        NormalizedConfigurationDocument remediatedDoc = normalizationService.normalizeAndPersist(
                remediatedConfig, deviceId, "cfg-cisco-remed-002", "ver-cisco-remed-002", "Cisco", "IOS-XE", "17.6"
        );

        RemediationVerificationResult positiveResult = verificationService.verifyFinding(telnetFinding, remediatedDoc);
        assertThat(positiveResult.isResolved()).isTrue();
        assertThat(positiveResult.getCurrentActualValue()).isEqualTo(false);
        assertThat(positiveResult.getExpectedValue()).isEqualTo(false);
        assertThat(positiveResult.getMessage()).contains("Violation resolved");

        // 5. Negative verification: Construct config where telnet is STILL enabled
        String stillFailingConfig = String.join("\n",
                "hostname cisco-edge-01",
                "version 17.6",
                "aaa new-model",
                "line vty 0 4",
                " transport input telnet ssh"
        );

        NormalizedConfigurationDocument stillFailingDoc = normalizationService.normalizeAndPersist(
                stillFailingConfig, deviceId, "cfg-cisco-unremed-003", "ver-cisco-unremed-003", "Cisco", "IOS-XE", "17.6"
        );

        RemediationVerificationResult negativeResult = verificationService.verifyFinding(telnetFinding, stillFailingDoc);
        assertThat(negativeResult.isResolved()).isFalse();
        assertThat(negativeResult.getCurrentActualValue()).isEqualTo(true);
        assertThat(negativeResult.getExpectedValue()).isEqualTo(false);
        assertThat(negativeResult.getMessage()).contains("Violation unresolved");
    }

    @Test
    @DisplayName("Platform gap templates create plans with PLATFORM_GAP_NOTICE steps and non-approved status")
    void testPlatformGapPlanCreation_ProducesPlatformGapNoticeSteps() {
        templateSeeder.seed();

        // 1. Fortinet SSH version gap
        NormalizedConfigurationDocument fortiDoc = new NormalizedConfigurationDocument();
        fortiDoc.setId("norm-forti-01");
        fortiDoc.setConfigurationId("cfg-forti-01");
        fortiDoc.setDeviceId("dev-forti-01");
        fortiDoc.setVendor("Fortinet");
        fortiDoc.setPlatform("FortiOS");
        normalizedConfigRepository.save(fortiDoc);

        Finding fortiFinding = new Finding();
        fortiFinding.setId("find-forti-ssh");
        fortiFinding.setDeviceId("dev-forti-01");
        fortiFinding.setConfigurationId("cfg-forti-01");
        fortiFinding.setCanonicalField("security.ssh.version");
        fortiFinding.setExpected(2);
        fortiFinding.setActual(null);

        RemediationPlan fortiPlan = planService.createPlanForFinding(fortiFinding);
        assertThat(fortiPlan).isNotNull();
        assertThat(fortiPlan.getStatus()).isEqualTo("PLANNED");
        assertThat(fortiPlan.getSteps()).hasSize(1);
        assertThat(fortiPlan.getSteps().get(0).getAction()).isEqualTo("PLATFORM_GAP_NOTICE");
        assertThat(fortiPlan.getSteps().get(0).getCommand()).isEqualTo("NO_COMMAND");
        assertThat(fortiPlan.getSteps().get(0).getDescription()).isEqualTo("not configurable on this platform");

        // 2. Palo Alto localLogging gap
        NormalizedConfigurationDocument panDoc = new NormalizedConfigurationDocument();
        panDoc.setId("norm-pan-01");
        panDoc.setConfigurationId("cfg-pan-01");
        panDoc.setDeviceId("dev-pan-01");
        panDoc.setVendor("Palo Alto");
        panDoc.setPlatform("PAN-OS");
        normalizedConfigRepository.save(panDoc);

        Finding panFinding = new Finding();
        panFinding.setId("find-pan-locallog");
        panFinding.setDeviceId("dev-pan-01");
        panFinding.setConfigurationId("cfg-pan-01");
        panFinding.setCanonicalField("logging.localLogging");
        panFinding.setExpected(true);
        panFinding.setActual(null);

        RemediationPlan panPlan = planService.createPlanForFinding(panFinding);
        assertThat(panPlan).isNotNull();
        assertThat(panPlan.getStatus()).isEqualTo("PLANNED");
        assertThat(panPlan.getSteps()).hasSize(1);
        assertThat(panPlan.getSteps().get(0).getAction()).isEqualTo("PLATFORM_GAP_NOTICE");
        assertThat(panPlan.getSteps().get(0).getCommand()).isEqualTo("NO_COMMAND");
        assertThat(panPlan.getSteps().get(0).getDescription()).isEqualTo("not determinable from configuration evidence");
    }

    @Test
    @DisplayName("Service validation handling: null finding, missing config, and repository queries")
    void testServiceValidationAndRepositoryQueries() {
        templateSeeder.seed();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> planService.createPlanForFinding(null))
                .isInstanceOf(IllegalArgumentException.class);

        Finding orphanFinding = new Finding();
        orphanFinding.setId("orphan-1");
        orphanFinding.setCanonicalField("security.telnet.enabled");
        orphanFinding.setConfigurationId("non-existent-cfg");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> planService.createPlanForFinding(orphanFinding))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to trace Finding");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> verificationService.verifyFinding(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Task 2.9d Item 3: Platform detection for Cisco configs (a, b, c) and IOS audit remediation")
    void testPlatformDetectionAndIosAuditRemediation() {
        cisSeeder.seed();
        templateSeeder.seed();

        // (a) Cisco config with no version line and IP address 10.17.1.1
        String configA = String.join("\n",
                "hostname cisco-edge-01",
                "interface GigabitEthernet0/1",
                " ip address 10.17.1.1 255.255.255.0",
                "line vty 0 4",
                " transport input telnet"
        );
        com.nexuscomply.cyber.detection.VendorDetectionResponse respA = vendorDetectionService.detectVendor(configA);
        System.out.println("=== CONFIG A (NO VERSION, IP 10.17.1.1) DETECTION ===");
        System.out.println("Vendor: " + respA.getVendor() + ", Platform: " + respA.getPlatform() + ", Confidence: " + respA.getConfidence());
        assertThat(respA.getVendor()).isEqualTo("Cisco");
        assertThat(respA.getPlatform()).isEqualTo("IOS");

        // (b) Cisco config with version 17.6
        String configB = String.join("\n",
                "hostname cisco-edge-02",
                "version 17.6",
                "interface GigabitEthernet0/1",
                " ip address 10.0.0.1 255.255.255.0",
                "line vty 0 4",
                " transport input telnet"
        );
        com.nexuscomply.cyber.detection.VendorDetectionResponse respB = vendorDetectionService.detectVendor(configB);
        System.out.println("=== CONFIG B (VERSION 17.6) DETECTION ===");
        System.out.println("Vendor: " + respB.getVendor() + ", Platform: " + respB.getPlatform() + ", Confidence: " + respB.getConfidence());
        assertThat(respB.getVendor()).isEqualTo("Cisco");
        assertThat(respB.getPlatform()).isEqualTo("IOS-XE");

        // (c) Cisco config with version 16.12 (detects as IOS-XE)
        String configC = String.join("\n",
                "hostname cisco-edge-03",
                "version 16.12",
                "interface GigabitEthernet0/1",
                " ip address 10.0.0.1 255.255.255.0",
                "line vty 0 4",
                " transport input telnet"
        );
        com.nexuscomply.cyber.detection.VendorDetectionResponse respC = vendorDetectionService.detectVendor(configC);
        System.out.println("=== CONFIG C (VERSION 16.12) DETECTION ===");
        System.out.println("Vendor: " + respC.getVendor() + ", Platform: " + respC.getPlatform() + ", Confidence: " + respC.getConfidence());
        assertThat(respC.getVendor()).isEqualTo("Cisco");
        assertThat(respC.getPlatform()).isEqualTo("IOS-XE");

        // (d) Cisco config with version 15.2 (classical IOS)
        String configD = String.join("\n",
                "hostname cisco-edge-04",
                "version 15.2",
                "interface GigabitEthernet0/1",
                " ip address 10.0.0.1 255.255.255.0",
                "line vty 0 4",
                " transport input telnet"
        );
        com.nexuscomply.cyber.detection.VendorDetectionResponse respD = vendorDetectionService.detectVendor(configD);
        System.out.println("=== CONFIG D (VERSION 15.2) DETECTION ===");
        System.out.println("Vendor: " + respD.getVendor() + ", Platform: " + respD.getPlatform() + ", Confidence: " + respD.getConfidence());
        assertThat(respD.getVendor()).isEqualTo("Cisco");
        assertThat(respD.getPlatform()).isEqualTo("IOS");

        // Allow CIS telnet rule to apply to IOS configs regardless of OS version
        ComplianceRuleDocument telnetRule = ruleRepository.findByRuleCode("CIS-1.2.2").orElseThrow();
        telnetRule.setApplicableOsVersions(java.util.Collections.emptyList());
        ruleRepository.save(telnetRule);

        // Run startAudit on Cisco config that detects as "IOS" (configA)
        Audit audit = auditOrchestrationService.startAudit("dev-cisco-ios-audit", "cfg-ios-01", "ver-ios-01", configA);
        assertThat(audit.getStatus()).isEqualTo("COMPLETED");

        List<FindingDocument> findingDocs = findingRepository.findByAuditId(audit.getId());
        assertThat(findingDocs).isNotEmpty();
        FindingDocument telnetDoc = findingDocs.stream()
                .filter(f -> "security.telnet.enabled".equals(f.getCanonicalField()))
                .findFirst().orElseThrow();

        // Check direct repository lookup for platform "IOS"
        java.util.Optional<RemediationTemplateDocument> directTemplate = templateRepository
                .findByVendorAndPlatformAndCanonicalField("Cisco", "IOS", "security.telnet.enabled");
        System.out.println("=== DIRECT REPOSITORY LOOKUP FOR (Cisco, IOS, security.telnet.enabled) ===");
        System.out.println("Direct template found: " + directTemplate.isPresent());

        // Create plan via planService
        Finding finding = new Finding();
        finding.setId(telnetDoc.getId());
        finding.setAuditId(telnetDoc.getAuditId());
        finding.setDeviceId(telnetDoc.getDeviceId());
        finding.setConfigurationId(telnetDoc.getConfigurationId());
        finding.setCanonicalField(telnetDoc.getCanonicalField());
        finding.setExpected(telnetDoc.getExpected());
        finding.setActual(telnetDoc.getActual());

        RemediationPlan plan = planService.createPlanForFinding(finding);
        assertThat(plan).isNotNull();
        assertThat(plan.getStatus()).isEqualTo("PLANNED");

        Document rawPlan = mongoTemplate.getCollection("remediation_plans").find(new Document("_id", plan.getId())).first();
        System.out.println("=== PERSISTED REMEDIATION PLAN FOR IOS FINDING ===");
        System.out.println(rawPlan != null ? rawPlan.toJson() : "null");
    }
}

