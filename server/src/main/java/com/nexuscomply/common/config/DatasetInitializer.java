package com.nexuscomply.common.config;

import com.nexuscomply.audit.model.Audit;
import com.nexuscomply.audit.model.AuditScope;
import com.nexuscomply.audit.model.AuditSummary;
import com.nexuscomply.audit.repository.AuditRepository;
import com.nexuscomply.configuration.model.Configuration;
import com.nexuscomply.configuration.repository.ConfigurationRepository;
import com.nexuscomply.device.model.Device;
import com.nexuscomply.device.repository.DeviceRepository;
import com.nexuscomply.drift.model.DriftEvent;
import com.nexuscomply.drift.repository.DriftEventRepository;
import com.nexuscomply.finding.model.Finding;
import com.nexuscomply.finding.repository.FindingRepository;
import com.nexuscomply.framework.service.DatasetImportService;
import com.nexuscomply.framework.service.DatasetValidationResult;
import com.nexuscomply.report.model.Report;
import com.nexuscomply.report.repository.ReportRepository;
import com.nexuscomply.ai.model.AiMapping;
import com.nexuscomply.ai.repository.AiMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatasetInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatasetInitializer.class);

    private final DatasetImportService datasetImportService;
    private final DeviceRepository deviceRepo;
    private final ConfigurationRepository configRepo;
    private final AuditRepository auditRepo;
    private final FindingRepository findingRepo;
    private final DriftEventRepository driftEventRepo;
    private final ReportRepository reportRepo;
    private final AiMappingRepository aiMappingRepo;
    private final MongoTemplate mongoTemplate;

    public DatasetInitializer(DatasetImportService datasetImportService,
                              DeviceRepository deviceRepo,
                              ConfigurationRepository configRepo,
                              AuditRepository auditRepo,
                              FindingRepository findingRepo,
                              DriftEventRepository driftEventRepo,
                              ReportRepository reportRepo,
                              AiMappingRepository aiMappingRepo,
                              MongoTemplate mongoTemplate) {
        this.datasetImportService = datasetImportService;
        this.deviceRepo = deviceRepo;
        this.configRepo = configRepo;
        this.auditRepo = auditRepo;
        this.findingRepo = findingRepo;
        this.driftEventRepo = driftEventRepo;
        this.reportRepo = reportRepo;
        this.aiMappingRepo = aiMappingRepo;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            ensureRuntimeCollectionIndexes();
            DatasetValidationResult result = datasetImportService.validateAndImport();
            log.info("NEXUS-COMPLY reference dataset initialization complete: valid={}, inserted/updated={}, errors={}",
                    result.isValid(), result.getInsertedCount(), result.getErrors().size());

            seedDevices();
            seedConfigurations();
            seedAudits();
            seedFindings();
            seedDriftEvents();
            seedReports();
            seedAiMappings();
        } catch (Exception e) {
            log.error("Error during reference dataset initialization: {}", e.getMessage(), e);
        }
    }

    private void seedDevices() {
        if (deviceRepo.count() == 0) {
            Device d1 = new Device("dev-001", "edge-router-01", "Cisco", "IOS-XE", "10.0.1.1",
                    "Catalyst 8300", "17.06.01a", "Production", "US-East-1",
                    "At risk", "2026-10-07", 48, 82, "Critical", 3);
            Device d2 = new Device("dev-002", "core-switch-01", "Cisco", "IOS-XE", "10.0.1.2",
                    "Catalyst 9300", "17.09.02", "Production", "US-East-1",
                    "Healthy", "2026-10-06", 15, 96, "High", 0);
            Device d3 = new Device("dev-003", "dist-switch-02", "Juniper", "JunOS", "10.0.2.1",
                    "EX4300", "21.4R1", "Staging", "EU-West-1",
                    "Healthy", "2026-10-05", 20, 94, "Medium", 1);
            Device d4 = new Device("dev-004", "sec-firewall-01", "Palo Alto", "PAN-OS", "10.0.0.1",
                    "PA-3220", "10.2.3", "Production", "US-West-2",
                    "At risk", "2026-10-07", 62, 75, "Critical", 3);
            Device d5 = new Device("dev-005", "forti-gw-01", "Fortinet", "FortiOS", "10.0.3.1",
                    "FortiGate 60F", "7.2.1", "Production", "AP-South-1",
                    "Healthy", "2026-10-06", 25, 91, "High", 1);

            deviceRepo.saveAll(List.of(d1, d2, d3, d4, d5));
            log.info("Seeded 5 baseline multi-vendor devices into database.");
        }
    }

    private void seedConfigurations() {
        if (configRepo.count() == 0) {
            Configuration c1 = new Configuration(
                    "cfg-001",
                    "dev-001",
                    1,
                    "edge-router-01.cfg",
                    "admin@nexus-comply.local",
                    "2026-10-07 10:15:00",
                    "42 KB",
                    "Active",
                    "sha256-e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                    List.of(
                            "! Current configuration - edge-router-01",
                            "version 17.6",
                            "service timestamps debug datetime msec",
                            "service timestamps log datetime msec",
                            "no service password-encryption",
                            "hostname edge-router-01",
                            "boot-start-marker",
                            "boot-end-marker",
                            "no aaa new-model",
                            "ip domain name corp.nexus.local",
                            "crypto key generate rsa modulus 2048",
                            "ip ssh version 1",
                            "interface GigabitEthernet0/0/0",
                            " description Uplink to Provider",
                            " ip address 10.0.1.1 255.255.255.0",
                            " negotiation auto",
                            " no shutdown",
                            "line con 0",
                            " stopbits 2",
                            "line vty 0 4",
                            " transport input telnet ssh",
                            " login local",
                            "end"
                    )
            );

            Configuration c2 = new Configuration(
                    "cfg-002",
                    "dev-002",
                    1,
                    "core-switch-01.cfg",
                    "admin@nexus-comply.local",
                    "2026-10-06 09:30:00",
                    "58 KB",
                    "Active",
                    "sha256-872f3e445b2b98fc1c149afbf4c8996fb92427ae41e4649b934ca495991b11a9",
                    List.of(
                            "! Current configuration - core-switch-01",
                            "version 17.9",
                            "hostname core-switch-01",
                            "aaa new-model",
                            "ip ssh version 2",
                            "line vty 0 4",
                            " transport input ssh",
                            "end"
                    )
            );

            Configuration c3 = new Configuration(
                    "cfg-003",
                    "dev-003",
                    1,
                    "dist-switch-02.cfg",
                    "admin@nexus-comply.local",
                    "2026-10-05 14:20:00",
                    "34 KB",
                    "Active",
                    "sha256-a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0",
                    List.of(
                            "system {",
                            "    host-name dist-switch-02;",
                            "    services {",
                            "        ssh {",
                            "            protocol-version v2;",
                            "        }",
                            "    }",
                            "}"
                    )
            );

            Configuration c4 = new Configuration(
                    "cfg-004",
                    "dev-004",
                    1,
                    "sec-firewall-01.cfg",
                    "admin@nexus-comply.local",
                    "2026-10-07 16:45:00",
                    "64 KB",
                    "Active",
                    "sha256-f0e1d2c3b4a5968778695a4b3c2d1e0f0e1d2c3b4a5968778695a4b3c2d1e0f0",
                    List.of(
                            "set deviceconfig system hostname sec-firewall-01",
                            "set deviceconfig system service disable-telnet no",
                            "set deviceconfig system service disable-http yes"
                    )
            );

            Configuration c5 = new Configuration(
                    "cfg-005",
                    "dev-005",
                    1,
                    "forti-gw-01.cfg",
                    "admin@nexus-comply.local",
                    "2026-10-06 18:10:00",
                    "49 KB",
                    "Active",
                    "sha256-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                    List.of(
                            "config system global",
                            "    set hostname forti-gw-01",
                            "    set admin-sport 8443",
                            "end"
                    )
            );

            configRepo.saveAll(List.of(c1, c2, c3, c4, c5));
            log.info("Seeded 5 baseline configurations into database.");
        }
    }

    private void seedAudits() {
        if (auditRepo.count() == 0) {
            Audit a1 = new Audit();
            a1.setId("aud-001");
            a1.setAuditNumber("AUD-2026-001");
            a1.setName("Quarterly Perimeter Audit - edge-router-01");
            a1.setDeviceId("dev-001");
            a1.setConfigurationId("cfg-001");
            a1.setStatus("COMPLETED");
            a1.setDate("2026-10-07");
            a1.setDuration("3.4s");
            AuditSummary s1 = new AuditSummary();
            s1.setTotalFindings(3);
            s1.setCriticalFindings(0);
            s1.setHighFindings(2);
            s1.setMediumFindings(1);
            s1.setLowFindings(0);
            s1.setComplianceScore(82.0);
            s1.setTotalControls(4);
            s1.setPassedControls(1);
            s1.setFailedControls(3);
            a1.setSummary(s1);
            a1.setScope(new AuditScope(List.of("dev-001"), List.of("cfg-001"), List.of("CIS Controls v8", "NIST SP 800-53")));
            a1.setFindingIds(List.of("find-001", "find-002", "find-003"));

            Audit a2 = new Audit();
            a2.setId("aud-002");
            a2.setAuditNumber("AUD-2026-002");
            a2.setName("Core Infrastructure Audit - core-switch-01");
            a2.setDeviceId("dev-002");
            a2.setConfigurationId("cfg-002");
            a2.setStatus("COMPLETED");
            a2.setDate("2026-10-06");
            a2.setDuration("2.1s");
            AuditSummary s2 = new AuditSummary();
            s2.setTotalFindings(0);
            s2.setComplianceScore(96.0);
            s2.setTotalControls(4);
            s2.setPassedControls(4);
            s2.setFailedControls(0);
            a2.setSummary(s2);
            a2.setScope(new AuditScope(List.of("dev-002"), List.of("cfg-002"), List.of("CIS Controls v8")));

            Audit a3 = new Audit();
            a3.setId("aud-003");
            a3.setAuditNumber("AUD-2026-003");
            a3.setName("Distribution Switch Compliance - dist-switch-02");
            a3.setDeviceId("dev-003");
            a3.setConfigurationId("cfg-003");
            a3.setStatus("COMPLETED");
            a3.setDate("2026-10-05");
            a3.setDuration("2.8s");
            AuditSummary s3 = new AuditSummary();
            s3.setTotalFindings(1);
            s3.setComplianceScore(94.0);
            a3.setSummary(s3);
            a3.setScope(new AuditScope(List.of("dev-003"), List.of("cfg-003"), List.of("CIS Controls v8")));
            a3.setFindingIds(List.of("find-007"));

            Audit a4 = new Audit();
            a4.setId("aud-004");
            a4.setAuditNumber("AUD-2026-004");
            a4.setName("Perimeter Firewall Security Audit - sec-firewall-01");
            a4.setDeviceId("dev-004");
            a4.setConfigurationId("cfg-004");
            a4.setStatus("COMPLETED");
            a4.setDate("2026-10-07");
            a4.setDuration("4.1s");
            AuditSummary s4 = new AuditSummary();
            s4.setTotalFindings(3);
            s4.setComplianceScore(75.0);
            a4.setSummary(s4);
            a4.setScope(new AuditScope(List.of("dev-004"), List.of("cfg-004"), List.of("NIST SP 800-53", "PCI-DSS")));
            a4.setFindingIds(List.of("find-004", "find-005", "find-006"));

            auditRepo.saveAll(List.of(a1, a2, a3, a4));
            log.info("Seeded 4 baseline audits into database.");
        }
    }

    private void seedFindings() {
        if (findingRepo.count() == 0) {
            Finding f1 = new Finding();
            f1.setId("find-001");
            f1.setAuditId("aud-001");
            f1.setDeviceId("dev-001");
            f1.setConfigurationId("cfg-001");
            f1.setControl("cis-1-1-3");
            f1.setControlId("cis-1-1-3");
            f1.setRule("RULE-MGMT-TELNET-OFF");
            f1.setRuleId("RULE-MGMT-TELNET-OFF");
            f1.setTitle("Telnet service enabled on VTY lines");
            f1.setDescription("Cleartext Telnet protocol permitted on remote administration lines vty 0 4.");
            f1.setSeverity("High");
            f1.setStatus("OPEN");
            f1.setLine(21);
            f1.setFramework("CIS Controls v8");
            f1.setExpected("transport input ssh");
            f1.setActual("transport input telnet ssh");
            f1.setImpact("Cleartext credentials and traffic subject to eavesdropping.");
            f1.setCanonicalField("management.remote_access.telnet_disabled");
            f1.setRemediation(List.of("configure terminal", "line vty 0 4", "transport input ssh", "end"));

            Finding f2 = new Finding();
            f2.setId("find-002");
            f2.setAuditId("aud-001");
            f2.setDeviceId("dev-001");
            f2.setConfigurationId("cfg-001");
            f2.setControl("cis-1-1-1");
            f2.setControlId("cis-1-1-1");
            f2.setRule("RULE-MGMT-SSH-V2");
            f2.setRuleId("RULE-MGMT-SSH-V2");
            f2.setTitle("Legacy SSH Version 1 permitted");
            f2.setDescription("SSH Version 1 is enabled which has known cryptanalytic vulnerabilities.");
            f2.setSeverity("High");
            f2.setStatus("OPEN");
            f2.setLine(12);
            f2.setFramework("CIS Controls v8");
            f2.setExpected("ip ssh version 2");
            f2.setActual("ip ssh version 1");
            f2.setImpact("Cryptographic degradation allows man-in-the-middle session hijacking.");
            f2.setCanonicalField("management.remote_access.ssh_v2");
            f2.setRemediation(List.of("configure terminal", "ip ssh version 2", "end"));

            Finding f3 = new Finding();
            f3.setId("find-003");
            f3.setAuditId("aud-001");
            f3.setDeviceId("dev-001");
            f3.setConfigurationId("cfg-001");
            f3.setControl("cis-1-1-2");
            f3.setControlId("cis-1-1-2");
            f3.setRule("RULE-AUTH-AAA");
            f3.setRuleId("RULE-AUTH-AAA");
            f3.setTitle("AAA subsystem not initialized");
            f3.setDescription("Centralized AAA authorization and accounting is disabled ('no aaa new-model').");
            f3.setSeverity("Medium");
            f3.setStatus("ACKNOWLEDGED");
            f3.setLine(9);
            f3.setFramework("CIS Controls v8");
            f3.setExpected("aaa new-model");
            f3.setActual("no aaa new-model");
            f3.setImpact("Lack of centralized authentication leads to unmonitored privilege escalation.");
            f3.setCanonicalField("authentication.aaa_enabled");
            f3.setRemediation(List.of("configure terminal", "aaa new-model", "end"));

            Finding f4 = new Finding();
            f4.setId("find-004");
            f4.setAuditId("aud-004");
            f4.setDeviceId("dev-004");
            f4.setConfigurationId("cfg-004");
            f4.setControl("nist-ac-17");
            f4.setControlId("nist-ac-17");
            f4.setRule("RULE-FW-TELNET-CLI");
            f4.setRuleId("RULE-FW-TELNET-CLI");
            f4.setTitle("Plaintext management protocol enabled");
            f4.setDescription("Telnet management service is not disabled on administrative interface.");
            f4.setSeverity("Critical");
            f4.setStatus("OPEN");
            f4.setLine(2);
            f4.setFramework("NIST SP 800-53");
            f4.setExpected("set deviceconfig system service disable-telnet yes");
            f4.setActual("set deviceconfig system service disable-telnet no");
            f4.setImpact("Cleartext credentials exposed on administrative network.");
            f4.setCanonicalField("management.remote_access.telnet_disabled");
            f4.setRemediation(List.of("set deviceconfig system service disable-telnet yes", "commit"));

            Finding f5 = new Finding();
            f5.setId("find-005");
            f5.setAuditId("aud-004");
            f5.setDeviceId("dev-004");
            f5.setConfigurationId("cfg-004");
            f5.setControl("pci-dss-8-2");
            f5.setControlId("pci-dss-8-2");
            f5.setRule("RULE-DEFAULT-ADMIN");
            f5.setRuleId("RULE-DEFAULT-ADMIN");
            f5.setTitle("Default administrative password unchanged");
            f5.setDescription("Default admin account credentials retained in configuration.");
            f5.setSeverity("Critical");
            f5.setStatus("OPEN");
            f5.setLine(5);
            f5.setFramework("PCI-DSS");
            f5.setExpected("Custom administrator hash defined");
            f5.setActual("Default factory credentials");
            f5.setImpact("Unauthorized access and firewall compromise.");
            f5.setCanonicalField("authentication.admin_password_changed");
            f5.setRemediation(List.of("set mgt-config users admin password", "commit"));

            Finding f6 = new Finding();
            f6.setId("find-006");
            f6.setAuditId("aud-004");
            f6.setDeviceId("dev-004");
            f6.setConfigurationId("cfg-004");
            f6.setControl("nist-sc-7");
            f6.setControlId("nist-sc-7");
            f6.setRule("RULE-ZONE-ANY-ANY");
            f6.setRuleId("RULE-ZONE-ANY-ANY");
            f6.setTitle("Overly permissive boundary policy");
            f6.setDescription("Security policy permits any-to-any ingress without inspection.");
            f6.setSeverity("High");
            f6.setStatus("OPEN");
            f6.setLine(8);
            f6.setFramework("NIST SP 800-53");
            f6.setExpected("Explicit service and destination filtering");
            f6.setActual("service any, application any");
            f6.setImpact("Unrestricted lateral network movement.");
            f6.setCanonicalField("firewall.policy.any_any_disabled");
            f6.setRemediation(List.of("delete rulebase security rules allow-all", "commit"));

            Finding f7 = new Finding();
            f7.setId("find-007");
            f7.setAuditId("aud-003");
            f7.setDeviceId("dev-003");
            f7.setConfigurationId("cfg-003");
            f7.setControl("cis-6-1");
            f7.setControlId("cis-6-1");
            f7.setRule("RULE-NTP-AUTH");
            f7.setRuleId("RULE-NTP-AUTH");
            f7.setTitle("NTP synchronization without authentication");
            f7.setDescription("Time synchronization server does not enforce cryptographic authentication.");
            f7.setSeverity("Low");
            f7.setStatus("IN_REVIEW");
            f7.setLine(4);
            f7.setFramework("CIS Controls v8");
            f7.setExpected("authentication-key configured");
            f7.setActual("ntp server unauthenticated");
            f7.setImpact("Time manipulation and audit trail desynchronization.");
            f7.setCanonicalField("system.ntp.auth_enabled");
            f7.setRemediation(List.of("set system ntp server 10.0.0.1 key 1"));

            findingRepo.saveAll(List.of(f1, f2, f3, f4, f5, f6, f7));
            log.info("Seeded 7 baseline findings into database.");
        }
    }

    private void seedDriftEvents() {
        if (driftEventRepo.count() == 0) {
            DriftEvent d1 = new DriftEvent();
            d1.setId("drift-001");
            d1.setDeviceId("dev-001");
            d1.setVersion(2);
            d1.setDate("2026-10-07");
            d1.setChange("Transport protocol downgraded on line vty 0 4");
            d1.setDescription("Line vty transport changed from 'transport input ssh' to 'transport input telnet ssh'.");
            d1.setImpact("Increased");
            d1.setRiskBefore(35);
            d1.setRiskAfter(48);
            d1.setFinding("find-001");
            d1.setControls(List.of("cis-1-1-3"));

            DriftEvent d2 = new DriftEvent();
            d2.setId("drift-002");
            d2.setDeviceId("dev-004");
            d2.setVersion(2);
            d2.setDate("2026-10-06");
            d2.setChange("Permissive any-any rule created on boundary firewall");
            d2.setDescription("New security rule added allowing unauthenticated ingress.");
            d2.setImpact("Increased");
            d2.setRiskBefore(45);
            d2.setRiskAfter(62);
            d2.setFinding("find-006");
            d2.setControls(List.of("nist-sc-7"));

            driftEventRepo.saveAll(List.of(d1, d2));
            log.info("Seeded 2 baseline drift events into database.");
        }
    }

    private void seedReports() {
        if (reportRepo.count() == 0) {
            Report r1 = new Report();
            r1.setId("REP-2026-001");
            r1.setTitle("Core Infrastructure Security Posture Assessment");
            r1.setType("Executive Summary");
            r1.setDeviceId("dev-001");
            r1.setAuditId("aud-001");
            r1.setDate("2026-10-07");
            r1.setStatus("Ready");
            r1.setCompliance(82);

            Report r2 = new Report();
            r2.setId("REP-2026-002");
            r2.setTitle("Perimeter Firewall Security Audit");
            r2.setType("Findings");
            r2.setDeviceId("dev-004");
            r2.setAuditId("aud-004");
            r2.setDate("2026-10-07");
            r2.setStatus("Ready");
            r2.setCompliance(75);

            Report r3 = new Report();
            r3.setId("REP-2026-003");
            r3.setTitle("Distribution Layer Health Evaluation");
            r3.setType("Risk Assessment");
            r3.setDeviceId("dev-003");
            r3.setAuditId("aud-003");
            r3.setDate("2026-10-05");
            r3.setStatus("Ready");
            r3.setCompliance(94);

            reportRepo.saveAll(List.of(r1, r2, r3));
            log.info("Seeded 3 baseline reports into database.");
        }
    }

    private void seedAiMappings() {
        if (aiMappingRepo.count() == 0) {
            AiMapping m1 = new AiMapping();
            m1.setId("AIM-001");
            m1.setSyntax("set system login user audit-operator class read-only");
            m1.setVendor("JUNOS");
            m1.setConfidence(96);
            m1.setCanonicalField("users[audit-operator].role");
            m1.setSuggestedValue("READ_ONLY");
            m1.setReason("Maps Junos read-only class definition to canonical non-privileged auditor role.");
            m1.setStatus("Needs review");
            m1.setReviewer("Unassigned");
            m1.setDate("2026-10-07");

            AiMapping m2 = new AiMapping();
            m2.setId("AIM-002");
            m2.setSyntax("transport input ssh");
            m2.setVendor("CISCO_IOS");
            m2.setConfidence(99);
            m2.setCanonicalField("remoteAccess.vty.transportInbound");
            m2.setSuggestedValue("[\"SSH\"]");
            m2.setReason("Restricts VTY transport inbound access exclusively to encrypted SSH.");
            m2.setStatus("Approved");
            m2.setReviewer("Ayush Sharma");
            m2.setDate("2026-10-06");

            AiMapping m3 = new AiMapping();
            m3.setId("AIM-003");
            m3.setSyntax("config system global set admin-sport 8443");
            m3.setVendor("FORTIOS");
            m3.setConfidence(92);
            m3.setCanonicalField("management.https.port");
            m3.setSuggestedValue("8443");
            m3.setReason("Identifies non-standard secure administrative HTTPS management port.");
            m3.setStatus("Approved");
            m3.setReviewer("Ayush Sharma");
            m3.setDate("2026-10-05");

            aiMappingRepo.saveAll(List.of(m1, m2, m3));
            log.info("Seeded 3 baseline AI mappings into database.");
        }
    }

    private void ensureRuntimeCollectionIndexes() {
        try {
            mongoTemplate.indexOps("audits")
                    .ensureIndex(new Index().on("startedAt", Sort.Direction.DESC));
            mongoTemplate.indexOps("findings")
                    .ensureIndex(new Index().on("auditId", Sort.Direction.ASC).on("deviceId", Sort.Direction.ASC));
            mongoTemplate.indexOps("evidence")
                    .ensureIndex(new Index().on("findingId", Sort.Direction.ASC));
            mongoTemplate.indexOps("normalized_configurations")
                    .ensureIndex(new Index().on("configurationId", Sort.Direction.ASC).on("versionId", Sort.Direction.ASC));
            mongoTemplate.indexOps("what_if_simulations")
                    .ensureIndex(new Index().on("createdAt", Sort.Direction.DESC));
            mongoTemplate.indexOps("drift_events")
                    .ensureIndex(new Index().on("deviceId", Sort.Direction.ASC).on("detectedAt", Sort.Direction.DESC));
            log.info("MongoDB runtime collection indexes ensured.");
        } catch (Exception e) {
            log.warn("Notice when ensuring runtime collection indexes: {}", e.getMessage());
        }
    }
}
