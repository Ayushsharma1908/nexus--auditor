package com.nexuscomply.cyber.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClients;
import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleDocument;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.compliance.persistence.ControlDocument;
import com.nexuscomply.cyber.compliance.persistence.ControlRepository;
import com.nexuscomply.cyber.compliance.persistence.FrameworkDocument;
import com.nexuscomply.cyber.compliance.persistence.FrameworkRepository;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceRuleEngineIntegrationTest {

    private static MongoServer mongoServer;
    private static MongoTemplate mongoTemplate;

    private FrameworkRepository frameworkRepository;
    private ControlRepository controlRepository;
    private ComplianceRuleRepository ruleRepository;

    private CiscoIosParser parser;
    private GenericRuleEvaluator evaluator;
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

        MongoRepositoryFactory repositoryFactory = new MongoRepositoryFactory(mongoTemplate);
        frameworkRepository = repositoryFactory.getRepository(FrameworkRepository.class);
        controlRepository = repositoryFactory.getRepository(ControlRepository.class);
        ruleRepository = repositoryFactory.getRepository(ComplianceRuleRepository.class);

        parser = new CiscoIosParser();
        evaluator = new GenericRuleEvaluator();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Real Cisco parser output evaluated by GenericRuleEvaluator produces correct PASS/FAIL/UNKNOWN")
    void testRealParserOutputThroughEvaluator() {
        String realCiscoConfig = String.join("\n",
                "version 17.6",
                "hostname RTR-01",
                "!",
                "ip ssh version 2",
                "no ip http server",
                "ip http secure-server",
                "line vty 0 4",
                " transport input ssh",
                "snmp-server community private RO version 3",
                "aaa new-model",
                "logging on",
                "logging host 10.10.10.5",
                "logging buffered 16384",
                "ntp server 10.0.0.1",
                "!"
        );

        ParserResult parserResult = parser.parse(realCiscoConfig);
        assertThat(parserResult.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = parserResult.getCanonical();

        // Check 1: Rule for SSH version EQUALS 2 -> PASS
        ComplianceRule sshV2Rule = new ComplianceRule(
                "rule-101", "ctrl-ssh", "fw-1", "TEST-SSH-2",
                new RuleRequirement("security.ssh.version", "EQUALS", 2),
                "HIGH", List.of("Cisco"), List.of("IOS-XE"), "ACTIVE", 1
        );
        RuleEvaluationResult sshResult = evaluator.evaluate(canonical, sshV2Rule);
        assertThat(sshResult.getStatus()).isEqualTo(RuleResultStatus.PASS);
        assertThat(sshResult.getActual()).isEqualTo(2);

        // Check 2: Rule for Telnet enabled EQUALS true -> FAIL (config has transport input ssh, so telnet=false)
        ComplianceRule telnetMustBeTrueRule = new ComplianceRule(
                "rule-102", "ctrl-telnet", "fw-1", "TEST-TELNET-TRUE",
                new RuleRequirement("security.telnet.enabled", "EQUALS", true),
                "HIGH", List.of("Cisco"), List.of(), "ACTIVE", 1
        );
        RuleEvaluationResult telnetResult = evaluator.evaluate(canonical, telnetMustBeTrueRule);
        assertThat(telnetResult.getStatus()).isEqualTo(RuleResultStatus.FAIL);
        assertThat(telnetResult.getActual()).isEqualTo(false);

        // Check 3: Rule for field never set in config (e.g. security.crypto.ikeVersion) -> UNKNOWN
        ComplianceRule absentFieldRule = new ComplianceRule(
                "rule-103", "ctrl-crypto", "fw-1", "TEST-CRYPTO-IKE",
                new RuleRequirement("security.crypto.ikeVersion", "EQUALS", 2),
                "MEDIUM", List.of(), List.of(), "ACTIVE", 1
        );
        RuleEvaluationResult absentResult = evaluator.evaluate(canonical, absentFieldRule);
        assertThat(absentResult.getStatus()).isEqualTo(RuleResultStatus.UNKNOWN);
        assertThat(absentResult.getMessage()).contains("was not observed or set");
    }

    @Test
    @DisplayName("Framework, Control, and ComplianceRule documents persist to MongoDB and match schema1.md sections 6, 7, 8")
    void testFrameworkControlRulePersistenceRoundTrip() throws Exception {
        Instant now = Instant.now();

        // 1. Persist FrameworkDocument (schema1.md Section 6)
        String frameworkId = UUID.randomUUID().toString();
        FrameworkDocument fw = new FrameworkDocument();
        fw.setId(frameworkId);
        fw.setCode("CIS");
        fw.setName("CIS Benchmarks");
        fw.setVersion("2025");
        fw.setCategory("NETWORK_SECURITY");
        fw.setDescription("CIS Benchmark network security specifications");
        fw.setStatus("ACTIVE");
        fw.setControlCount(120);
        fw.getMetadata().put("targetAudience", "Network Engineers");
        fw.setCreatedAt(now);
        fw.setUpdatedAt(now);

        frameworkRepository.save(fw);

        Optional<FrameworkDocument> savedFwOpt = frameworkRepository.findById(frameworkId);
        assertThat(savedFwOpt).isPresent();
        FrameworkDocument savedFw = savedFwOpt.get();

        String fwJson = objectMapper.writeValueAsString(savedFw);
        @SuppressWarnings("unchecked")
        Map<String, Object> fwJsonMap = objectMapper.readValue(fwJson, Map.class);
        assertThat(fwJsonMap).containsKeys("id", "code", "name", "version", "category", "description", "status", "controlCount", "metadata", "createdAt", "updatedAt");

        // 2. Persist ControlDocument (schema1.md Section 7)
        String controlId = UUID.randomUUID().toString();
        ControlDocument ctrl = new ControlDocument();
        ctrl.setId(controlId);
        ctrl.setFrameworkId(frameworkId);
        ctrl.setControlId("CIS-5.1");
        ctrl.setTitle("Disable insecure management protocols");
        ctrl.setDescription("Ensure Telnet service is disabled on network devices.");
        ctrl.setCategory("ACCESS_CONTROL");
        ctrl.setSeverity("HIGH");
        ctrl.setStatus("ACTIVE");
        ctrl.setRequirements(List.of(new com.nexuscomply.cyber.compliance.model.ControlRequirement("management.telnetEnabled", "EQUALS", false)));
        ctrl.setCreatedAt(now);
        ctrl.setUpdatedAt(now);

        controlRepository.save(ctrl);

        Optional<ControlDocument> savedCtrlOpt = controlRepository.findById(controlId);
        assertThat(savedCtrlOpt).isPresent();
        ControlDocument savedCtrl = savedCtrlOpt.get();
        assertThat(savedCtrl.getRequirements()).hasSize(1);
        assertThat(savedCtrl.getRequirements().get(0).getField()).isEqualTo("management.telnetEnabled");

        String ctrlJson = objectMapper.writeValueAsString(savedCtrl);
        @SuppressWarnings("unchecked")
        Map<String, Object> ctrlJsonMap = objectMapper.readValue(ctrlJson, Map.class);
        assertThat(ctrlJsonMap).containsKeys("id", "frameworkId", "controlId", "title", "description", "category", "severity", "status", "requirements", "createdAt", "updatedAt");

        // 3. Persist ComplianceRuleDocument (schema1.md Section 8)
        String ruleId = UUID.randomUUID().toString();
        ComplianceRuleDocument rule = new ComplianceRuleDocument();
        rule.setId(ruleId);
        rule.setRuleCode("CIS-SSH-V2");
        rule.setControlId(controlId);
        rule.setName("SSH version must be 2");
        rule.setDescription("SSH must use version 2.");
        rule.setExpression(new RuleRequirement("security.ssh.version", "EQUALS", 2));
        rule.setSeverity("HIGH");
        rule.setFrameworkIds(List.of(frameworkId));
        rule.setApplicableVendors(List.of("Cisco", "Juniper", "Fortinet"));
        rule.setStatus("ACTIVE");
        rule.setVersion(1);
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);

        ruleRepository.save(rule);

        Optional<ComplianceRuleDocument> savedRuleOpt = ruleRepository.findById(ruleId);
        assertThat(savedRuleOpt).isPresent();
        ComplianceRuleDocument savedRule = savedRuleOpt.get();

        String ruleJson = objectMapper.writeValueAsString(savedRule);
        @SuppressWarnings("unchecked")
        Map<String, Object> ruleJsonMap = objectMapper.readValue(ruleJson, Map.class);
        assertThat(ruleJsonMap).containsKeys("id", "ruleCode", "controlId", "name", "description", "expression", "severity", "frameworkIds", "applicableVendors", "status", "version", "createdAt", "updatedAt");
    }
}
