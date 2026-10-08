package com.nexuscomply;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import org.springframework.test.context.ContextConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = MongoTestInitializer.class)
class FullApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Frameworks B-009: GET /api/v1/frameworks")
    void testFrameworksList() throws Exception {
        mockMvc.perform(get("/api/v1/frameworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Test
    @DisplayName("Audits B-018: GET /api/v1/audits")
    void testAuditsList() throws Exception {
        mockMvc.perform(get("/api/v1/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("Compliance B-032: GET /api/v1/compliance/results/audit-001/summary")
    void testComplianceSummary() throws Exception {
        mockMvc.perform(get("/api/v1/compliance/results/audit-001/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditId").value("audit-001"));
    }

    @Test
    @DisplayName("Findings B-034: GET /api/v1/findings")
    void testFindingsList() throws Exception {
        mockMvc.perform(get("/api/v1/findings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("Risk B-046: GET /api/v1/risk")
    void testRiskList() throws Exception {
        mockMvc.perform(get("/api/v1/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("Drift B-053: GET /api/v1/drift")
    void testDriftList() throws Exception {
        mockMvc.perform(get("/api/v1/drift"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("Simulations B-059 & B-060: POST and GET /api/v1/simulations")
    void testSimulationLifecycle() throws Exception {
        Map<String, String> body = Map.of("name", "Test Sim 1");
        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Test Sim 1"))
                .andExpect(jsonPath("$.data.status").value("QUEUED"));

        mockMvc.perform(get("/api/v1/simulations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("Remediation B-064: GET /api/v1/remediation/templates")
    void testRemediationTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/remediation/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("AI B-071: POST /api/v1/ai/analyze (Returns 202 Accepted)")
    void testAiAnalyze() throws Exception {
        Map<String, Object> body = Map.of(
                "jobType", "MAPPING_SUGGESTION",
                "targetFramework", "cis-cisco-ios-xe",
                "targetId", "tgt-001"
        );
        mockMvc.perform(post("/api/v1/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.status").value("QUEUED"));
    }

    @Test
    @DisplayName("Dashboard B-080: GET /api/v1/dashboard/summary")
    void testDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRiskAssessments", notNullValue()))
                .andExpect(jsonPath("$.data.totalDriftEvents", notNullValue()))
                .andExpect(jsonPath("$.data.totalReports", notNullValue()))
                .andExpect(jsonPath("$.data.totalSimulations", notNullValue()));
    }

    @Test
    @DisplayName("Reports B-088: GET /api/v1/reports")
    void testReportsList() throws Exception {
        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }
}
