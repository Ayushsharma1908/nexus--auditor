package com.nexuscomply.parser.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuscomply.parser.dto.ParseRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.context.ContextConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = com.nexuscomply.MongoTestInitializer.class)
class ParserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("B-001: POST /api/v1/cyber/parse - Happy path with raw configuration")
    void testParseConfigurationHappyPath() throws Exception {
        ParseRequest request = new ParseRequest();
        request.setRawContent("hostname router-core-01\ninterface GigabitEthernet0/0/0\n ip address 10.0.0.1 255.255.255.0\n");
        request.setVendor("Cisco");
        request.setPlatform("IOS-XE");

        mockMvc.perform(post("/api/v1/cyber/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.jobId").isNotEmpty())
                .andExpect(jsonPath("$.data.jobType").value("PARSER"))
                .andExpect(jsonPath("$.data.vendor").value("Cisco"))
                .andExpect(jsonPath("$.data.platform").value("IOS-XE"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("B-001: POST /api/v1/cyber/parse - Happy path with configurationId")
    void testParseConfigurationWithConfigurationId() throws Exception {
        ParseRequest request = new ParseRequest();
        request.setConfigurationId("550e8400-e29b-41d4-a716-446655440000");
        request.setDeviceId("device-101");

        mockMvc.perform(post("/api/v1/cyber/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.jobId").isNotEmpty())
                .andExpect(jsonPath("$.data.configurationId").value("550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.data.deviceId").value("device-101"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("B-001: POST /api/v1/cyber/parse - Validation failure when missing both configurationId and rawContent")
    void testParseConfigurationValidationFailure() throws Exception {
        ParseRequest request = new ParseRequest();

        mockMvc.perform(post("/api/v1/cyber/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message").value(containsString("Either configurationId or rawContent must be provided")))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("B-001: POST /api/v1/cyber/parse - Malformed JSON body returns 400")
    void testParseConfigurationMalformedBody() throws Exception {
        mockMvc.perform(post("/api/v1/cyber/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("B-002: GET /api/v1/cyber/parse-jobs/{jobId} - Not found error")
    void testGetParseJobNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/cyber/parse-jobs/nonexistent-job-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }
}
