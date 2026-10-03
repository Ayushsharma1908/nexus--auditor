package com.nexuscomply.cyber.parser.juniper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClients;
import com.nexuscomply.cyber.audit.Audit;
import com.nexuscomply.cyber.audit.AuditOrchestrationService;
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
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentDocument;
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

import static org.assertj.core.api.Assertions.assertThat;

class JuniperPipelineIntegrationTest {

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
    private AuditOrchestrationService auditOrchestrationService;

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
        parserService = new ParserServiceImpl(List.of(new CiscoIosParser(), new JuniperJunosParser()));
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
    @DisplayName("End-to-end proof: Vendor detection -> Juniper parsing -> Normalization -> Audit Orchestration with vendor isolation")
    void testEndToEndJuniperPipelineWithVendorIsolation() throws Exception {
        // 1. Seed all 21 CIS, NIST, ISO rules (scoped exclusively to Cisco IOS/IOS-XE)
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();
        assertThat(ruleRepository.count()).isEqualTo(21);

        // 2. Realistic Junos configuration text with deliberate violation (telnet enabled)
        String junosConfig = String.join("\n",
                "set system host-name edge-router-01",
                "set system authentication-order [ radius password ]",
                "set system services ssh protocol-version v2",
                "set system services telnet",
                "set system services web-management https",
                "set snmp v3 usm local-user secadmin authentication-sha authentication-password secretKey123",
                "set system syslog host 10.100.1.25 any any",
                "set system syslog file messages any notice",
                "set system ntp server 10.100.1.1",
                "set interfaces ge-0/0/0 unit 0 family inet address 192.168.10.1/24"
        );

        String deviceId = "juniper-edge-01";
        String configurationId = "cfg-junos-001";
        String versionId = "ver-junos-001";

        // 3. Execute full pipeline
        Audit audit = auditOrchestrationService.startAudit(deviceId, configurationId, versionId, junosConfig);
        String auditId = audit.getId();

        // 4. Confirm vendor detection correctly identifies Juniper / JUNOS
        com.nexuscomply.cyber.detection.VendorDetectionResponse detection = vendorDetectionService.detectVendor(junosConfig);
        assertThat(detection).isNotNull();
        assertThat(detection.getVendor()).isEqualTo("Juniper");
        assertThat(detection.getPlatform()).isEqualTo("JUNOS");
        assertThat(detection.getConfidence()).isGreaterThan(0.0);

        // 5. Confirm normalized configuration is persisted and contains correct canonical facts
        NormalizedConfigurationDocument normDoc = normalizedConfigRepository.findByVersionId(versionId).orElseThrow();
        assertThat(normDoc.getVendor()).isEqualTo("Juniper");
        assertThat(normDoc.getPlatform()).isEqualTo("JUNOS");

        Map<String, Object> sec = normDoc.getCanonical().getSecurity();
        assertThat(sec).isNotNull();

        Map<String, Object> ssh = (Map<String, Object>) sec.get("ssh");
        assertThat(ssh.get("enabled")).isEqualTo(true);
        assertThat(ssh.get("version")).isEqualTo(2);

        Map<String, Object> telnet = (Map<String, Object>) sec.get("telnet");
        assertThat(telnet.get("enabled")).isEqualTo(true);

        Map<String, Object> https = (Map<String, Object>) sec.get("https");
        assertThat(https.get("enabled")).isEqualTo(true);

        Map<String, Object> snmp = (Map<String, Object>) sec.get("snmp");
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isEqualTo("3");

        Map<String, Object> auth = normDoc.getCanonical().getAuthentication();
        assertThat(auth.get("aaa")).isEqualTo(true);

        Map<String, Object> logging = normDoc.getCanonical().getLogging();
        assertThat(logging.get("syslog")).isEqualTo(true);
        assertThat(logging.get("localLogging")).isEqualTo(true);

        Map<String, Object> ntp = normDoc.getCanonical().getNtp();
        assertThat(ntp.get("configured")).isEqualTo(true);

        // Unknowns captured for interfaces and hostname
        assertThat(normDoc.getUnknowns()).isNotEmpty();

        List<String> expectedFrameworkIds = frameworkRepository.findAll().stream()
                .map(FrameworkDocument::getId)
                .toList();
        assertThat(expectedFrameworkIds).hasSize(3);

        // 6. Confirm vendor isolation: audit status COMPLETED, totalControls is 21 (all 21 rules not-applicable)
        assertThat(audit.getStatus()).isEqualTo("COMPLETED");
        assertThat(audit.getFrameworkIds()).containsExactlyInAnyOrderElementsOf(expectedFrameworkIds);
        assertThat(audit.getSummary()).isNotNull();
        assertThat(audit.getSummary().getTotalControls()).isEqualTo(21);
        assertThat(audit.getSummary().getPassed()).isEqualTo(0);
        assertThat(audit.getSummary().getFailed()).isEqualTo(0);
        assertThat(audit.getSummary().getNotApplicable()).isEqualTo(21);
        // Zero applicable controls must produce null complianceScore (N/A), not 100.0,
        // preventing conflation with an actual clean audit pass.
        assertThat(audit.getComplianceScore()).isNull();

        // No findings or evidence should be created because no rules applied to Juniper
        List<FindingDocument> findings = findingRepository.findByAuditId(auditId);
        assertThat(findings).isEmpty();

        List<EvidenceDocument> evidence = evidenceRepository.findByAuditId(auditId);
        assertThat(evidence).isEmpty();

        // 7. Verify persisted AuditDocument
        AuditDocument persistedAuditDoc = auditRepository.findById(auditId).orElseThrow();
        assertThat(persistedAuditDoc.getStatus()).isEqualTo("COMPLETED");
        assertThat(persistedAuditDoc.getFrameworkIds()).containsExactlyInAnyOrderElementsOf(expectedFrameworkIds);
        assertThat(persistedAuditDoc.getSummary().getTotalControls()).isEqualTo(21);
        assertThat(persistedAuditDoc.getSummary().getNotApplicable()).isEqualTo(21);
        assertThat(persistedAuditDoc.getComplianceScore()).isNull();

        // Print raw JSON outputs for verification proof
        String rawNormDocJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normDoc);
        String rawAuditDocJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(persistedAuditDoc);

        System.out.println("=== RAW NORMALIZED CONFIGURATION DOCUMENT ===");
        System.out.println(rawNormDocJson);
        System.out.println("=== RAW AUDIT DOCUMENT ===");
        System.out.println(rawAuditDocJson);
    }
}
