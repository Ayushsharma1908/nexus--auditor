package com.nexuscomply.cyber.parser.paloalto;

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
import com.nexuscomply.cyber.parser.fortinet.FortinetFortiOSParser;
import com.nexuscomply.cyber.parser.juniper.JuniperJunosParser;
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

import static org.assertj.core.api.Assertions.assertThat;

class PaloAltoPipelineIntegrationTest {

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
        parserService = new ParserServiceImpl(List.of(
                new CiscoIosParser(),
                new JuniperJunosParser(),
                new FortinetFortiOSParser(),
                new PaloAltoPanOsParser()
        ));
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
    @DisplayName("Pipeline Integration: PAN-OS set-format fixture with all 3 frameworks seeded -> 7 notApplicable, 8 passed, 6 unknown, complianceScore 57.1")
    void testPipelineIntegration_PanOsSetFormatFixture() throws Exception {
        // Seed all 21 rules across CIS, NIST, ISO
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String setConfig = String.join("\n",
                "set deviceconfig system service disable-telnet yes",
                "set network profiles interface-management-profile mgt-profile ssh yes",
                "set network profiles interface-management-profile mgt-profile https yes",
                "set network interface ethernet ethernet1/1 layer3 interface-management-profile mgt-profile",
                "set deviceconfig system snmp-setting access-setting version v2c snmp-community-string public",
                "set shared log-settings syslog SIEM-Profile server SIEM-Collector server 10.10.10.50",
                "set shared log-settings system match-list Forward-System send-syslog SIEM-Profile",
                "set deviceconfig system ntp-servers primary-ntp-server ntp-server-address 10.0.0.1",
                "set shared authentication-profile RADIUS-AUTH method radius",
                "set deviceconfig system authentication-profile RADIUS-AUTH"
        );

        Audit audit = auditOrchestrationService.startAudit(
                "device-pa-01",
                "cfg-pa-01",
                "v1.0",
                setConfig
        );

        assertThat(audit).isNotNull();
        assertThat(audit.getStatus()).isEqualTo("COMPLETED");

        AuditDocument auditDoc = auditRepository.findById(audit.getId()).orElseThrow();
        NormalizedConfigurationDocument normDoc = normalizedConfigRepository.findById(auditDoc.getNormalizedConfigurationId()).orElseThrow();

        System.out.println("=== PAN-OS SET-FORMAT AUDIT DOCUMENT ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(auditDoc));
        System.out.println("=== PAN-OS SET-FORMAT NORMALIZED CONFIGURATION DOCUMENT ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normDoc));

        assertThat(auditDoc.getSummary().getTotalControls()).isEqualTo(21);
        assertThat(auditDoc.getSummary().getNotApplicable()).isEqualTo(7);
        assertThat(auditDoc.getSummary().getPassed()).isEqualTo(8);
        assertThat(auditDoc.getSummary().getFailed()).isEqualTo(0);
        assertThat(auditDoc.getSummary().getUnknown()).isEqualTo(6);
        assertThat(auditDoc.getSummary().getError()).isEqualTo(0);
        assertThat(auditDoc.getComplianceScore()).isEqualTo(57.1);
        List<String> expectedFrameworkIds = frameworkRepository.findAll().stream()
                .map(FrameworkDocument::getId)
                .toList();
        assertThat(expectedFrameworkIds).hasSize(3);
        assertThat(auditDoc.getFrameworkIds()).containsExactlyInAnyOrderElementsOf(expectedFrameworkIds);
    }

    @Test
    @DisplayName("Pipeline Integration: PAN-OS XML running-config export fixture with all 3 frameworks seeded -> 7 notApplicable, 8 passed, 6 unknown")
    void testPipelineIntegration_PanOsXmlFormatFixture() throws Exception {
        // Seed all 21 rules across CIS, NIST, ISO
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String xmlConfig = """
                <config version="10.1.0" urldb="paloaltonetworks">
                  <devices>
                    <entry name="localhost.localdomain">
                      <deviceconfig>
                        <system>
                          <service>
                            <disable-telnet>yes</disable-telnet>
                          </service>
                          <ntp-servers>
                            <primary-ntp-server>
                              <ntp-server-address>10.0.0.1</ntp-server-address>
                            </primary-ntp-server>
                          </ntp-servers>
                          <authentication-profile>RADIUS-AUTH</authentication-profile>
                          <snmp-setting>
                            <access-setting>
                              <version>
                                <v2c>
                                  <snmp-community-string>public</snmp-community-string>
                                </v2c>
                              </version>
                            </access-setting>
                          </snmp-setting>
                        </system>
                      </deviceconfig>
                      <network>
                        <profiles>
                          <interface-management-profile>
                            <entry name="mgt-profile">
                              <ssh>yes</ssh>
                              <https>yes</https>
                            </entry>
                          </interface-management-profile>
                        </profiles>
                        <interface>
                          <ethernet>
                            <entry name="ethernet1/1">
                              <layer3>
                                <interface-management-profile>mgt-profile</interface-management-profile>
                              </layer3>
                            </entry>
                          </ethernet>
                        </interface>
                      </network>
                    </entry>
                  </devices>
                  <shared>
                    <log-settings>
                      <syslog>
                        <entry name="SIEM-Profile">
                          <server>
                            <entry name="SIEM-Collector">
                              <server>10.10.10.50</server>
                            </entry>
                          </server>
                        </entry>
                      </syslog>
                      <system>
                        <match-list>
                          <entry name="Forward-System">
                            <send-syslog>
                              <member>SIEM-Profile</member>
                            </send-syslog>
                          </entry>
                        </match-list>
                      </system>
                    </log-settings>
                    <authentication-profile>
                      <entry name="RADIUS-AUTH">
                        <method>
                          <radius/>
                        </method>
                      </entry>
                    </authentication-profile>
                  </shared>
                </config>
                """;

        Audit audit = auditOrchestrationService.startAudit(
                "device-pa-02",
                "cfg-pa-02",
                "v1.0",
                xmlConfig
        );

        assertThat(audit).isNotNull();
        assertThat(audit.getStatus()).isEqualTo("COMPLETED");

        AuditDocument auditDoc = auditRepository.findById(audit.getId()).orElseThrow();
        NormalizedConfigurationDocument normDoc = normalizedConfigRepository.findById(auditDoc.getNormalizedConfigurationId()).orElseThrow();

        System.out.println("=== PAN-OS XML-FORMAT AUDIT DOCUMENT ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(auditDoc));
        System.out.println("=== PAN-OS XML-FORMAT NORMALIZED CONFIGURATION DOCUMENT ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normDoc));

        assertThat(auditDoc.getSummary().getTotalControls()).isEqualTo(21);
        assertThat(auditDoc.getSummary().getNotApplicable()).isEqualTo(7);
        assertThat(auditDoc.getSummary().getPassed()).isEqualTo(8);
        assertThat(auditDoc.getSummary().getFailed()).isEqualTo(0);
        assertThat(auditDoc.getSummary().getUnknown()).isEqualTo(6);
        assertThat(auditDoc.getSummary().getError()).isEqualTo(0);
        assertThat(auditDoc.getComplianceScore()).isEqualTo(57.1);
        List<String> expectedFrameworkIds = frameworkRepository.findAll().stream()
                .map(FrameworkDocument::getId)
                .toList();
        assertThat(expectedFrameworkIds).hasSize(3);
        assertThat(auditDoc.getFrameworkIds()).containsExactlyInAnyOrderElementsOf(expectedFrameworkIds);
    }

    @Test
    @DisplayName("Palo Alto deliberate-violation config (telnet enabled) -> FAIL findings on security.telnet.enabled, remediation plan generated")
    void testPaloAltoDeliberateViolationConfig() throws Exception {
        cisSeeder.seed();
        nistSeeder.seed();
        isoSeeder.seed();

        String violationConfig = String.join("\n",
                "set deviceconfig system service disable-telnet no",
                "set network profiles interface-management-profile mgt-profile ssh yes",
                "set network profiles interface-management-profile mgt-profile https yes",
                "set network interface ethernet ethernet1/1 layer3 interface-management-profile mgt-profile",
                "set deviceconfig system snmp-setting access-setting version v2c snmp-community-string public",
                "set shared log-settings syslog SIEM-Profile server SIEM-Collector server 10.10.10.50",
                "set shared log-settings system match-list Forward-System send-syslog SIEM-Profile",
                "set deviceconfig system ntp-servers primary-ntp-server ntp-server-address 10.0.0.1",
                "set shared authentication-profile RADIUS-AUTH method radius",
                "set deviceconfig system authentication-profile RADIUS-AUTH"
        );

        String deviceId = "device-pa-viol";
        String configurationId = "cfg-pa-viol";

        Audit audit = auditOrchestrationService.startAudit(
                deviceId,
                configurationId,
                "v1.0",
                violationConfig
        );

        assertThat(audit).isNotNull();
        assertThat(audit.getStatus()).isEqualTo("COMPLETED");

        AuditDocument auditDoc = auditRepository.findById(audit.getId()).orElseThrow();
        assertThat(auditDoc.getSummary().getTotalControls()).isEqualTo(21);
        assertThat(auditDoc.getSummary().getNotApplicable()).isEqualTo(7);
        assertThat(auditDoc.getSummary().getPassed()).isEqualTo(6);
        assertThat(auditDoc.getSummary().getFailed()).isEqualTo(2);
        assertThat(auditDoc.getSummary().getUnknown()).isEqualTo(6);
        assertThat(auditDoc.getSummary().getError()).isEqualTo(0);
        assertThat(auditDoc.getSummary().getPassed() + auditDoc.getSummary().getFailed() + auditDoc.getSummary().getUnknown() + auditDoc.getSummary().getNotApplicable() + auditDoc.getSummary().getError()).isEqualTo(21);
        assertThat(auditDoc.getComplianceScore()).isEqualTo(42.9);

        List<FindingDocument> findings = findingRepository.findByDeviceId(deviceId);
        assertThat(findings).hasSize(2);
        assertThat(findings).allMatch(f -> "security.telnet.enabled".equals(f.getCanonicalField()));
        assertThat(findings).allMatch(f -> Boolean.FALSE.equals(f.getExpected()));
        assertThat(findings).allMatch(f -> Boolean.TRUE.equals(f.getActual()));

        // Confirm Evidence exists for each finding
        System.out.println("=== RAW PERSISTED PALO ALTO DELIBERATE-VIOLATION EVIDENCE ===");
        for (FindingDocument f : findings) {
            assertThat(f.getEvidenceIds()).isNotEmpty();
            for (String evId : f.getEvidenceIds()) {
                EvidenceDocument ev = evidenceRepository.findById(evId).orElse(null);
                assertThat(ev).isNotNull();
                assertThat(ev.getSource().getRawText()).contains("disable-telnet no");
                org.bson.Document rawEvidence = mongoTemplate.getCollection("evidence")
                        .find(new org.bson.Document("_id", evId)).first();
                System.out.println(rawEvidence != null ? rawEvidence.toJson() : "null");
            }
        }

        System.out.println("=== RAW PERSISTED PALO ALTO DELIBERATE-VIOLATION FINDINGS ===");
        for (FindingDocument f : findings) {
            org.bson.Document rawFinding = mongoTemplate.getCollection("findings")
                    .find(new org.bson.Document("_id", f.getId())).first();
            System.out.println(rawFinding != null ? rawFinding.toJson() : "null");
        }

        System.out.println("=== RAW PERSISTED PALO ALTO DELIBERATE-VIOLATION AUDIT DOCUMENT ===");
        org.bson.Document rawAuditDoc = mongoTemplate.getCollection("audits")
                .find(new org.bson.Document("_id", audit.getId())).first();
        System.out.println(rawAuditDoc != null ? rawAuditDoc.toJson() : "null");

        // Create remediation plan for Palo Alto telnet finding
        com.nexuscomply.cyber.remediation.persistence.RemediationTemplateRepository tplRepo =
                new MongoRepositoryFactory(mongoTemplate).getRepository(com.nexuscomply.cyber.remediation.persistence.RemediationTemplateRepository.class);
        com.nexuscomply.cyber.remediation.persistence.RemediationPlanRepository planRepo =
                new MongoRepositoryFactory(mongoTemplate).getRepository(com.nexuscomply.cyber.remediation.persistence.RemediationPlanRepository.class);
        com.nexuscomply.cyber.remediation.seeder.RemediationTemplateSeeder tplSeeder =
                new com.nexuscomply.cyber.remediation.seeder.RemediationTemplateSeeder(tplRepo);
        tplSeeder.seed();

        com.nexuscomply.cyber.remediation.service.RemediationPlanServiceImpl planService =
                new com.nexuscomply.cyber.remediation.service.RemediationPlanServiceImpl(planRepo, tplRepo, normalizedConfigRepository, findingRepository);

        com.nexuscomply.cyber.finding.Finding telnetFinding = new com.nexuscomply.cyber.finding.Finding();
        telnetFinding.setId(findings.get(0).getId());
        telnetFinding.setDeviceId(deviceId);
        telnetFinding.setConfigurationId(configurationId);
        telnetFinding.setCanonicalField("security.telnet.enabled");
        telnetFinding.setExpected(false);
        telnetFinding.setActual(true);

        com.nexuscomply.cyber.remediation.model.RemediationPlan telnetPlan = planService.createPlanForFinding(telnetFinding);
        assertThat(telnetPlan).isNotNull();
        assertThat(telnetPlan.getStatus()).isEqualTo("PLANNED");
        assertThat(telnetPlan.getSteps().get(0).getCommand()).isEqualTo("set deviceconfig system service disable-telnet yes");

        org.bson.Document rawPlan = mongoTemplate.getCollection("remediation_plans")
                .find(new org.bson.Document("findingId", telnetFinding.getId())).first();
        System.out.println("=== RAW PERSISTED PALO ALTO TELNET REMEDIATION PLAN ===");
        System.out.println(rawPlan != null ? rawPlan.toJson() : "null");
    }
}
