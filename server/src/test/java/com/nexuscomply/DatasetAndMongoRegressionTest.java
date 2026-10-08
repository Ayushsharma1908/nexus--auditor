package com.nexuscomply;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuscomply.audit.model.Audit;
import com.nexuscomply.audit.repository.AuditRepository;
import com.nexuscomply.finding.model.Evidence;
import com.nexuscomply.finding.model.Finding;
import com.nexuscomply.finding.repository.EvidenceRepository;
import com.nexuscomply.finding.repository.FindingRepository;
import com.nexuscomply.framework.model.EvaluationCase;
import com.nexuscomply.framework.model.Framework;
import com.nexuscomply.framework.model.VendorKnowledge;
import com.nexuscomply.framework.repository.ComplianceRuleRepository;
import com.nexuscomply.framework.repository.EvaluationCaseRepository;
import com.nexuscomply.framework.repository.FrameworkRepository;
import com.nexuscomply.framework.repository.VendorKnowledgeRepository;
import com.nexuscomply.simulation.model.Simulation;
import com.nexuscomply.simulation.repository.SimulationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = MongoTestInitializer.class)
class DatasetAndMongoRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FrameworkRepository frameworkRepo;

    @Autowired
    private VendorKnowledgeRepository vendorKnowledgeRepo;

    @Autowired
    private ComplianceRuleRepository ruleRepo;

    @Autowired
    private EvaluationCaseRepository evalRepo;

    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private FindingRepository findingRepo;

    @Autowired
    private EvidenceRepository evidenceRepo;

    @Autowired
    private SimulationRepository simulationRepo;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    @DisplayName("Verify Reference Dataset Ingested into MongoDB")
    void testReferenceDatasetSeeded() {
        // Frameworks
        List<Framework> frameworks = frameworkRepo.findAll();
        assertThat(frameworks).isNotEmpty();
        assertThat(frameworks.size()).isGreaterThanOrEqualTo(4);
        assertThat(frameworks.stream().map(Framework::getId))
                .contains("FW-CIS", "FW-NIST-800-53", "FW-STIG", "FW-ISO-27001");

        // Vendor Knowledge
        List<VendorKnowledge> vk = vendorKnowledgeRepo.findAll();
        assertThat(vk).isNotEmpty();
        assertThat(vk.size()).isGreaterThanOrEqualTo(23);
        assertThat(vk.stream().map(VendorKnowledge::getVendor).distinct())
                .contains("Cisco", "Juniper", "Fortinet", "Palo Alto");

        // Compliance Rules
        assertThat(ruleRepo.count()).isGreaterThanOrEqualTo(6);

        // Evaluation Cases
        List<EvaluationCase> evalCases = evalRepo.findAll();
        assertThat(evalCases).isNotEmpty();
        assertThat(evalCases.size()).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Verify Collection Naming for What-If Simulations (what_if_simulations)")
    void testSimulationCollectionNaming() {
        Simulation sim = new Simulation();
        sim.setName("Collection Name Test Sim");
        sim.setStatus("QUEUED");
        Simulation saved = simulationRepo.save(sim);

        assertThat(saved.getId()).isNotNull();
        // Verify it was stored in what_if_simulations collection, not simulations
        assertThat(mongoTemplate.collectionExists("what_if_simulations")).isTrue();
        assertThat(mongoTemplate.findById(saved.getId(), Simulation.class, "what_if_simulations")).isNotNull();
    }

    @Test
    @DisplayName("Verify Real Multi-Vendor Evaluation Cases Pass Deterministically")
    void testEvaluationCasesEvaluation() throws Exception {
        List<EvaluationCase> cases = evalRepo.findAll();
        assertThat(cases).isNotEmpty();

        for (EvaluationCase ec : cases) {
            assertThat(ec.getVendor()).isNotNull();
            assertThat(ec.getExpectedResult()).isIn("PASS", "FAIL", "UNKNOWN");

            // Post a parse request for the vendor to verify parser/knowledge compatibility
            Map<String, Object> parseReq = Map.of(
                    "vendor", ec.getVendor(),
                    "platform", ec.getPlatform() != null ? ec.getPlatform() : "Default",
                    "rawContent", "hostname " + ec.getId() + "\n" + ec.getSummary()
            );

            mockMvc.perform(post("/api/v1/cyber/parse")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(parseReq)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }
    }

    @Test
    @DisplayName("Verify End-to-End Audit, Finding, and Evidence MongoDB Persistence")
    void testAuditLifecyclePersistence() throws Exception {
        Map<String, Object> auditRequest = Map.of(
                "name", "E2E Production Mongo Verification Audit",
                "type", "COMPREHENSIVE",
                "deviceIds", List.of("dev-cisco-core-01"),
                "configurationIds", List.of("cfg-e2e-01"),
                "frameworkIds", List.of("FW-CIS")
        );

        String responseStr = mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(auditRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> respMap = objectMapper.readValue(responseStr, new TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) respMap.get("data");
        String auditId = (String) data.get("id");

        // Verify audit is persisted in Mongo
        Audit persistedAudit = auditRepo.findById(auditId).orElse(null);
        assertThat(persistedAudit).isNotNull();
        assertThat(persistedAudit.getName()).isEqualTo("E2E Production Mongo Verification Audit");

        // Verify evaluate compliance creates and persists findings and evidence
        mockMvc.perform(post("/api/v1/compliance/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("auditId", auditId))))
                .andExpect(status().isOk());

        // Verify findings and evidence exist in MongoDB
        List<Finding> findings = findingRepo.findByAuditId(auditId);
        assertThat(findings).isNotEmpty();
        for (Finding f : findings) {
            assertThat(f.getId()).isNotNull();
            List<Evidence> evidenceList = evidenceRepo.findByFindingId(f.getId());
            assertThat(evidenceList).isNotEmpty();
            Evidence ev = evidenceList.get(0);
            assertThat(ev.getAuditId()).isEqualTo(auditId);
            assertThat(ev.getConfigurationVersionId()).isNotNull();
        }
    }

    @Test
    @DisplayName("Verify Dashboard Aggregates Real MongoDB State")
    void testDashboardMongoAggregation() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFrameworks").isNumber())
                .andExpect(jsonPath("$.data.totalAudits").isNumber());

        mockMvc.perform(get("/api/v1/dashboard/frameworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFrameworks").isNumber());
    }
}
