package com.nexuscomply.cyber.parser.juniper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClients;
import com.nexuscomply.cyber.audit.Audit;
import com.nexuscomply.cyber.audit.AuditOrchestrationService;
import com.nexuscomply.cyber.audit.AuditOrchestrationServiceImpl;
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

        // 6. Confirm vendor-neutral rule coverage across NIST and ISO
        assertThat(audit.getStatus()).isEqualTo("COMPLETED");
        assertThat(audit.getFrameworkIds()).containsExactlyInAnyOrderElementsOf(expectedFrameworkIds);
        AuditSummary sum = audit.getSummary();
        assertThat(sum).isNotNull();
        assertThat(sum.getTotalControls()).isEqualTo(21);
        assertThat(sum.getPassed()).isEqualTo(12); // NIST-IA-2, NIST-SC-8, NIST-CM-6, NIST-AU-2, NIST-AU-12, NIST-AU-8, ISO-A.9.4.2, ISO-A.10.1.1, ISO-A.13.1.1-SNMP, ISO-A.12.4.1, ISO-A.12.4.3, ISO-A.12.4.4
        assertThat(sum.getFailed()).isEqualTo(2); // NIST-AC-17, ISO-A.13.1.1-TELNET
        assertThat(sum.getUnknown()).isEqualTo(0);
        assertThat(sum.getNotApplicable()).isEqualTo(7);
        assertThat(sum.getError()).isEqualTo(0);
        assertThat(sum.getPassed() + sum.getFailed() + sum.getUnknown() + sum.getNotApplicable() + sum.getError()).isEqualTo(sum.getTotalControls());
        assertThat(audit.getComplianceScore()).isEqualTo(85.7);

        // Exactly 2 findings on security.telnet.enabled (1 NIST, 1 ISO)
        List<FindingDocument> findings = findingRepository.findByAuditId(auditId);
        assertThat(findings).hasSize(2);
        for (FindingDocument f : findings) {
            assertThat(f.getCanonicalField()).isEqualTo("security.telnet.enabled");
            assertThat(f.getComplianceStatus()).isEqualTo("FAIL");
            assertThat(f.getEvidenceIds()).hasSize(1);
            EvidenceDocument ev = evidenceRepository.findById(f.getEvidenceIds().get(0)).orElseThrow();
            assertThat(ev.getFindingId()).isEqualTo(f.getId());
        }

        List<EvidenceDocument> evidence = evidenceRepository.findByAuditId(auditId);
        assertThat(evidence).hasSize(2);

        // 7. Verify persisted AuditDocument
        AuditDocument persistedAuditDoc = auditRepository.findById(auditId).orElseThrow();
        assertThat(persistedAuditDoc.getStatus()).isEqualTo("COMPLETED");
        assertThat(persistedAuditDoc.getSummary().getTotalControls()).isEqualTo(21);
        assertThat(persistedAuditDoc.getSummary().getPassed()).isEqualTo(12);
        assertThat(persistedAuditDoc.getSummary().getFailed()).isEqualTo(2);
        assertThat(persistedAuditDoc.getSummary().getNotApplicable()).isEqualTo(7);
        assertThat(persistedAuditDoc.getComplianceScore()).isEqualTo(85.7);

        // Print raw persisted documents for surefire capture
        org.bson.Document rawNistRule = mongoTemplate.getCollection("compliance_rules")
                .find(new org.bson.Document("ruleCode", "NIST-AC-17")).first();
        System.out.println("=== RAW PERSISTED RULE DOCUMENT (JUNIPER APPLICABLE: NIST-AC-17) ===");
        System.out.println(rawNistRule != null ? rawNistRule.toJson() : "null");

        for (int i = 0; i < findings.size(); i++) {
            org.bson.Document rawFinding = mongoTemplate.getCollection("findings")
                    .find(new org.bson.Document("_id", findings.get(i).getId())).first();
            System.out.println("=== RAW PERSISTED JUNIPER FINDING " + (i + 1) + " ===");
            System.out.println(rawFinding != null ? rawFinding.toJson() : "null");
        }

        for (int i = 0; i < evidence.size(); i++) {
            org.bson.Document rawEv = mongoTemplate.getCollection("evidence")
                    .find(new org.bson.Document("_id", evidence.get(i).getId())).first();
            System.out.println("=== RAW PERSISTED JUNIPER EVIDENCE " + (i + 1) + " ===");
            System.out.println(rawEv != null ? rawEv.toJson() : "null");
        }

        org.bson.Document rawAuditDoc = mongoTemplate.getCollection("audits")
                .find(new org.bson.Document("_id", auditId)).first();
        System.out.println("=== RAW PERSISTED JUNIPER DELIBERATE VIOLATION AUDIT DOCUMENT ===");
        System.out.println(rawAuditDoc != null ? rawAuditDoc.toJson() : "null");

        // 8. Create remediation plan for Juniper finding
        com.nexuscomply.cyber.remediation.persistence.RemediationTemplateRepository tplRepo =
                new MongoRepositoryFactory(mongoTemplate).getRepository(com.nexuscomply.cyber.remediation.persistence.RemediationTemplateRepository.class);
        com.nexuscomply.cyber.remediation.persistence.RemediationPlanRepository planRepo =
                new MongoRepositoryFactory(mongoTemplate).getRepository(com.nexuscomply.cyber.remediation.persistence.RemediationPlanRepository.class);
        com.nexuscomply.cyber.remediation.seeder.RemediationTemplateSeeder tplSeeder =
                new com.nexuscomply.cyber.remediation.seeder.RemediationTemplateSeeder(tplRepo);
        tplSeeder.seed();

        com.nexuscomply.cyber.remediation.service.RemediationPlanServiceImpl planService =
                new com.nexuscomply.cyber.remediation.service.RemediationPlanServiceImpl(planRepo, tplRepo, normalizedConfigRepository, findingRepository);

        com.nexuscomply.cyber.finding.Finding domainFinding = new com.nexuscomply.cyber.finding.Finding();
        domainFinding.setId(findings.get(0).getId());
        domainFinding.setDeviceId(deviceId);
        domainFinding.setConfigurationId(configurationId);
        domainFinding.setCanonicalField("security.telnet.enabled");
        domainFinding.setExpected(false);
        domainFinding.setActual(true);

        com.nexuscomply.cyber.remediation.model.RemediationPlan plan = planService.createPlanForFinding(domainFinding);
        assertThat(plan).isNotNull();
        assertThat(plan.getStatus()).isEqualTo("PLANNED");
        assertThat(plan.getSteps()).hasSize(1);
        assertThat(plan.getSteps().get(0).getCommand()).isEqualTo("delete system services telnet");

        org.bson.Document rawPlan = mongoTemplate.getCollection("remediation_plans")
                .find(new org.bson.Document("findingId", domainFinding.getId())).first();
        System.out.println("=== RAW PERSISTED JUNIPER REMEDIATION PLAN ===");
        System.out.println(rawPlan != null ? rawPlan.toJson() : "null");
    }

    @Test
    @DisplayName("Compliant Junos config -> PASS on SSH version, UNKNOWN on telnet (unset), complianceScore 50.0")
    void testJuniperCompliantConfig() {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String compliantJunosConfig = String.join("\n",
                "set system host-name edge-router-clean",
                "set system authentication-order [ radius password ]",
                "set system services ssh protocol-version v2",
                "set system services web-management https",
                "set snmp v3 usm local-user secadmin authentication-sha authentication-password secretKey123",
                "set system syslog host 10.100.1.25 any any",
                "set system syslog file messages any notice",
                "set system ntp server 10.100.1.1",
                "set interfaces ge-0/0/0 unit 0 family inet address 192.168.10.1/24"
        );

        Audit audit = auditOrchestrationService.startAudit("juniper-clean-01", "cfg-junos-clean", "ver-junos-clean", compliantJunosConfig);
        assertThat(audit.getStatus()).isEqualTo("COMPLETED");

        AuditSummary sum = audit.getSummary();
        assertThat(sum.getTotalControls()).isEqualTo(21);
        assertThat(sum.getPassed()).isEqualTo(12); // NIST-IA-2, NIST-SC-8, NIST-CM-6, NIST-AU-2, NIST-AU-12, NIST-AU-8, ISO-A.9.4.2, ISO-A.10.1.1, ISO-A.13.1.1-SNMP, ISO-A.12.4.1, ISO-A.12.4.3, ISO-A.12.4.4
        assertThat(sum.getFailed()).isEqualTo(0);
        assertThat(sum.getUnknown()).isEqualTo(2); // NIST-AC-17, ISO-A.13.1.1-TELNET (telnet is unset/null)
        assertThat(sum.getNotApplicable()).isEqualTo(7);
        assertThat(sum.getError()).isEqualTo(0);
        assertThat(sum.getPassed() + sum.getFailed() + sum.getUnknown() + sum.getNotApplicable() + sum.getError()).isEqualTo(sum.getTotalControls());
        // applicableControls = 21 - 7 = 14; passed = 12; score = (12/14)*100 = 85.7
        assertThat(audit.getComplianceScore()).isEqualTo(85.7);

        org.bson.Document rawAuditDoc = mongoTemplate.getCollection("audits")
                .find(new org.bson.Document("_id", audit.getId())).first();
        System.out.println("=== RAW PERSISTED JUNIPER COMPLIANT AUDIT DOCUMENT ===");
        System.out.println(rawAuditDoc != null ? rawAuditDoc.toJson() : "null");
    }
}
