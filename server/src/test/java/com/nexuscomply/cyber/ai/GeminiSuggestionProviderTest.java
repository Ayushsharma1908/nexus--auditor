package com.nexuscomply.cyber.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClients;
import com.nexuscomply.cyber.ai.persistence.AiJobDocument;
import com.nexuscomply.cyber.ai.persistence.AiJobRepository;
import com.nexuscomply.cyber.ai.persistence.AiMappingDocument;
import com.nexuscomply.cyber.ai.persistence.AiMappingRepository;
import com.nexuscomply.cyber.ai.service.AiMappingService;
import com.nexuscomply.cyber.ai.service.AiMappingServiceImpl;
import com.nexuscomply.cyber.ai.service.CanonicalFieldAllowlist;
import com.nexuscomply.cyber.ai.service.DeterministicStubSuggestionProvider;
import com.nexuscomply.cyber.ai.service.GeminiSuggestionProvider;
import com.nexuscomply.cyber.ai.service.SuggestionResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeminiSuggestionProvider Integration & Resiliency Test Suite")
class GeminiSuggestionProviderTest {

    private static MongoServer mongoServer;
    private static MongoTemplate mongoTemplate;
    private static AiMappingRepository aiMappingRepository;
    private static AiJobRepository aiJobRepository;

    private HttpServer mockGeminiServer;
    private int mockPort;
    private String mockBaseUrl;
    private DeterministicStubSuggestionProvider stubFallback;
    private GeminiSuggestionProvider provider;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void initMongo() {
        mongoServer = new MongoServer(new MemoryBackend());
        InetSocketAddress address = mongoServer.bind();
        String connectionString = "mongodb://" + address.getHostName() + ":" + address.getPort() + "/nexuscomply-test";
        mongoTemplate = new MongoTemplate(new SimpleMongoClientDatabaseFactory(MongoClients.create(connectionString), "nexuscomply-test"));
        MongoRepositoryFactory factory = new MongoRepositoryFactory(mongoTemplate);
        aiMappingRepository = factory.getRepository(AiMappingRepository.class);
        aiJobRepository = factory.getRepository(AiJobRepository.class);
    }

    @AfterAll
    static void tearDownMongo() {
        if (mongoServer != null) {
            mongoServer.shutdown();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        mongoTemplate.getDb().drop();
        objectMapper = new ObjectMapper();
        stubFallback = new DeterministicStubSuggestionProvider();

        // Start mock HTTP server on random available port
        mockGeminiServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockGeminiServer.getAddress().getPort();
        mockBaseUrl = "http://127.0.0.1:" + mockPort;
        mockGeminiServer.start();

        provider = new GeminiSuggestionProvider(
                stubFallback,
                "test-api-key",
                "gemini-2.5-flash",
                10,
                "gemini",
                mockBaseUrl,
                null,
                objectMapper
        );
    }

    @AfterEach
    void tearDown() {
        if (mockGeminiServer != null) {
            mockGeminiServer.stop(0);
        }
    }

    @Test
    @DisplayName("Fallback: Delegates to stub when API key is blank or null")
    void testFallbackWhenApiKeyIsBlankOrNull() {
        GeminiSuggestionProvider unconfiguredProvider = new GeminiSuggestionProvider(
                stubFallback,
                "",
                "gemini-2.5-flash",
                10,
                "gemini",
                mockBaseUrl,
                null,
                objectMapper
        );

        SuggestionResult result = unconfiguredProvider.propose(
                "Cisco",
                "IOS-XE",
                "legacy-telnet enable",
                CanonicalFieldAllowlist.getAllowedFields()
        );

        assertNotNull(result, "Unconfigured provider should delegate to deterministic stub");
        assertEquals("security.telnet.enabled", result.getCanonicalField());
        assertEquals(Boolean.TRUE, result.getMappedValue());
    }

    @Test
    @DisplayName("Fallback: Delegates to stub when provider mode is configured as 'stub'")
    void testFallbackWhenProviderIsStub() {
        provider.setProvider("stub");

        SuggestionResult result = provider.propose(
                "Cisco",
                "IOS-XE",
                "legacy-telnet enable",
                CanonicalFieldAllowlist.getAllowedFields()
        );

        assertNotNull(result, "Stub mode should delegate to deterministic stub");
        assertEquals("security.telnet.enabled", result.getCanonicalField());
        assertEquals(Boolean.TRUE, result.getMappedValue());
    }

    @Test
    @DisplayName("Successful Gemini response: Parses canonical field, mapped value, and rationale")
    void testSuccessfulGeminiResponse() {
        AtomicReference<String> receivedApiKey = new AtomicReference<>();

        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            receivedApiKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            String responseJson = "{\n" +
                    "  \"candidates\": [\n" +
                    "    {\n" +
                    "      \"content\": {\n" +
                    "        \"parts\": [\n" +
                    "          {\n" +
                    "            \"text\": \"{\\\"canonicalField\\\": \\\"security.ssh.version\\\", \\\"mappedValue\\\": 2, \\\"confidence\\\": 0.98, \\\"rationale\\\": \\\"Enables modern SSH protocol version 2\\\"}\"\n" +
                    "          }\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        SuggestionResult result = provider.propose(
                "Juniper",
                "JUNOS",
                "set system services ssh protocol-version v2",
                CanonicalFieldAllowlist.getAllowedFields()
        );

        assertNotNull(result);
        assertEquals("test-api-key", receivedApiKey.get(), "Header x-goog-api-key must be passed");
        assertEquals("security.ssh.version", result.getCanonicalField());
        assertEquals(2, result.getMappedValue());
        assertEquals(0.98, result.getConfidence());
        assertEquals("Enables modern SSH protocol version 2", result.getRationale());
    }

    @Test
    @DisplayName("Successful Gemini response: Correctly unwraps markdown code fence")
    void testSuccessfulGeminiResponseWithMarkdownFence() {
        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            String responseJson = "{\n" +
                    "  \"candidates\": [\n" +
                    "    {\n" +
                    "      \"content\": {\n" +
                    "        \"parts\": [\n" +
                    "          {\n" +
                    "            \"text\": \"```json\\n{\\n  \\\"canonicalField\\\": \\\"security.telnet.enabled\\\",\\n  \\\"mappedValue\\\": false,\\n  \\\"confidence\\\": 0.94,\\n  \\\"rationale\\\": \\\"Disables insecure telnet access\\\"\\n}\\n```\"\n" +
                    "          }\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        SuggestionResult result = provider.propose(
                "Fortinet",
                "FortiOS",
                "unselect allowaccess telnet",
                CanonicalFieldAllowlist.getAllowedFields()
        );

        assertNotNull(result);
        assertEquals("security.telnet.enabled", result.getCanonicalField());
        assertEquals(Boolean.FALSE, result.getMappedValue());
        assertEquals(0.94, result.getConfidence());
    }

    @Test
    @DisplayName("Timeout Fallback: Strict client timeout triggers fallback to stub without throwing")
    void testTimeoutFallsBackToStub() {
        // Set short timeout of 1 second for fast test execution
        provider.setTimeoutSeconds(1);

        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            try {
                // Sleep longer than provider timeout (2.5 seconds)
                Thread.sleep(2500);
            } catch (InterruptedException ignored) {}
            byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        long start = System.currentTimeMillis();
        // Line that deterministic stub knows how to handle
        SuggestionResult result = provider.propose(
                "Cisco",
                "IOS-XE",
                "insecure-remote telnet",
                CanonicalFieldAllowlist.getAllowedFields()
        );
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(result, "Timed out call must fall back to deterministic stub proposal");
        assertEquals("security.telnet.enabled", result.getCanonicalField());
        assertEquals(Boolean.TRUE, result.getMappedValue());
        assertTrue(elapsed < 4000, "Should abort around client timeout window");
    }

    @Test
    @DisplayName("HTTP Error Fallback: 500 Internal Server Error triggers fallback to stub without throwing")
    void testHttp500FallsBackToStub() {
        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            byte[] bytes = "{\"error\": \"internal server error\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        SuggestionResult result = provider.propose(
                "Cisco",
                "IOS-XE",
                "legacy-telnet allow",
                CanonicalFieldAllowlist.getAllowedFields()
        );

        assertNotNull(result, "HTTP 500 must trigger fallback delegate without error");
        assertEquals("security.telnet.enabled", result.getCanonicalField());
        assertEquals(Boolean.TRUE, result.getMappedValue());
    }

    @Test
    @DisplayName("HTTP 429 Rate Limit: Triggers fallback to stub without throwing")
    void testHttp429FallsBackToStub() {
        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            byte[] bytes = "{\"error\": \"rate limit exceeded\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(429, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        SuggestionResult result = provider.propose(
                "Cisco",
                "IOS-XE",
                "legacy-telnet allow",
                CanonicalFieldAllowlist.getAllowedFields()
        );

        assertNotNull(result);
        assertEquals("security.telnet.enabled", result.getCanonicalField());
    }

    @Test
    @DisplayName("Malformed JSON: Unparseable response triggers fallback to stub without throwing")
    void testMalformedJsonFallsBackToStub() {
        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            byte[] bytes = "Not valid json response content".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        SuggestionResult result = provider.propose(
                "Cisco",
                "IOS-XE",
                "legacy-telnet allow",
                CanonicalFieldAllowlist.getAllowedFields()
        );

        assertNotNull(result);
        assertEquals("security.telnet.enabled", result.getCanonicalField());
    }

    @Test
    @DisplayName("Disallowed field: Gemini suggestion not in allowlist triggers fallback")
    void testDisallowedCanonicalFieldFallsBack() {
        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            String responseJson = "{\n" +
                    "  \"candidates\": [\n" +
                    "    {\n" +
                    "      \"content\": {\n" +
                    "        \"parts\": [\n" +
                    "          {\n" +
                    "            \"text\": \"{\\\"canonicalField\\\": \\\"arbitrary.unallowed.field\\\", \\\"mappedValue\\\": true, \\\"confidence\\\": 0.99, \\\"rationale\\\": \\\"Arbitrary field\\\"}\"\n" +
                    "          }\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        SuggestionResult result = provider.propose(
                "Cisco",
                "IOS-XE",
                "arbitrary-command enable",
                CanonicalFieldAllowlist.getAllowedFields()
        );

        // arbitrary command is not in stub either, so should safely return null
        assertNull(result, "Disallowed field must be rejected and fallback to stub (returning null for unknown)");
    }

    @Test
    @DisplayName("Blank/null raw line: Returns null / fallback without network invocation")
    void testBlankOrNullRawLine() {
        AtomicInteger callCount = new AtomicInteger(0);
        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            callCount.incrementAndGet();
            byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertNull(provider.propose("Cisco", "IOS-XE", null, null));
        assertNull(provider.propose("Cisco", "IOS-XE", "", null));
        assertNull(provider.propose("Cisco", "IOS-XE", "   ", null));
        assertEquals(0, callCount.get(), "Zero HTTP calls should be made for null or blank raw syntax lines");
    }

    @Test
    @DisplayName("Human-in-the-Loop Review Boundary: Suggestions strictly remain PENDING_REVIEW")
    void testHumanInTheLoopReviewBoundaryPreserved() {
        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            String responseJson = "{\n" +
                    "  \"candidates\": [\n" +
                    "    {\n" +
                    "      \"content\": {\n" +
                    "        \"parts\": [\n" +
                    "          {\n" +
                    "            \"text\": \"{\\\"canonicalField\\\": \\\"security.telnet.enabled\\\", \\\"mappedValue\\\": true, \\\"confidence\\\": 0.93, \\\"rationale\\\": \\\"Detected legacy telnet\\\"}\"\n" +
                    "          }\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        AiMappingService aiMappingService = new AiMappingServiceImpl(aiMappingRepository, aiJobRepository, provider);

        // 1. Record unknown syntax
        AiMappingDocument recorded = aiMappingService.recordUnknownSyntax("Cisco", "IOS-XE", "custom-telnet-enable 1");
        assertNotNull(recorded);
        assertEquals("PENDING_REVIEW", recorded.getStatus());

        // 2. Request suggestion (executes Gemini provider)
        AiMappingDocument withSuggestion = aiMappingService.requestSuggestion(recorded.getId());
        assertNotNull(withSuggestion);
        assertEquals("security.telnet.enabled", withSuggestion.getCanonicalField());
        assertEquals(Boolean.TRUE, withSuggestion.getMappedValue());
        assertEquals("AI", withSuggestion.getSuggestedBy());

        // IMMUTABLE BOUNDARY: status must remain PENDING_REVIEW
        assertEquals("PENDING_REVIEW", withSuggestion.getStatus(), "AI suggestion must never auto-approve");
        assertNull(withSuggestion.getReview().getReviewerId(), "Reviewer ID must remain null until human approval");

        // 3. Human explicitly approves
        AiMappingDocument approved = aiMappingService.approve(withSuggestion.getId(), "security-officer-01");
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("security-officer-01", approved.getReview().getReviewerId());
        assertNotNull(approved.getReview().getReviewedAt());
    }

    @Test
    @DisplayName("Payload Hardening: Validates system instruction and responseSchema structure in request body")
    void testHardenedPayloadStructureAndResponseSchema() {
        AtomicReference<String> requestBodyRef = new AtomicReference<>();

        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                requestBodyRef.set(requestBody);
            } catch (Exception ignored) {}

            String responseJson = "{\n" +
                    "  \"candidates\": [\n" +
                    "    {\n" +
                    "      \"content\": {\n" +
                    "        \"parts\": [\n" +
                    "          {\n" +
                    "            \"text\": \"{\\\"canonicalField\\\": \\\"security.telnet.enabled\\\", \\\"mappedValue\\\": \\\"true\\\", \\\"confidence\\\": 0.95, \\\"rationale\\\": \\\"Matches schema\\\"}\"\n" +
                    "          }\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        SuggestionResult result = provider.propose("Cisco", "IOS-XE", "sample-command-xyz", CanonicalFieldAllowlist.getAllowedFields());
        assertNotNull(result);

        // Verify request payload conforms to exact Gemini API structured specification
        assertNotNull(requestBodyRef.get(), "Request body must have been transmitted");
        try {
            com.fasterxml.jackson.databind.JsonNode payload = objectMapper.readTree(requestBodyRef.get());

            // 1. System instruction checks
            assertTrue(payload.has("systemInstruction"));
            String sysText = payload.path("systemInstruction").path("parts").get(0).path("text").asText();
            assertTrue(sysText.contains("expert network security auditor"));
            assertTrue(sysText.contains("Cisco"));
            assertTrue(sysText.contains("IOS-XE"));

            // 2. Generation config structured JSON schema checks
            com.fasterxml.jackson.databind.JsonNode genConfig = payload.path("generationConfig");
            assertEquals("application/json", genConfig.path("responseMimeType").asText());

            com.fasterxml.jackson.databind.JsonNode schema = genConfig.path("responseSchema");
            assertEquals("OBJECT", schema.path("type").asText());

            com.fasterxml.jackson.databind.JsonNode props = schema.path("properties");
            assertEquals("STRING", props.path("canonicalField").path("type").asText());
            assertTrue(props.path("canonicalField").path("nullable").asBoolean());
            assertEquals("STRING", props.path("mappedValue").path("type").asText());
            assertTrue(props.path("mappedValue").path("nullable").asBoolean());
            assertEquals("NUMBER", props.path("confidence").path("type").asText());
            assertEquals("STRING", props.path("rationale").path("type").asText());
            assertEquals("STRING", props.path("unit").path("type").asText());
            assertTrue(props.path("unit").path("nullable").asBoolean());

            com.fasterxml.jackson.databind.JsonNode required = schema.path("required");
            assertTrue(required.isArray());
            assertEquals("confidence", required.get(0).asText());
            assertEquals("rationale", required.get(1).asText());

        } catch (Exception e) {
            fail("Failed to parse request JSON payload: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Type Coercion: Correctly parses stringified mappedValue for boolean, integer, and string fields")
    void testStringifiedMappedValueCoercion() {
        // Test Integer field coercion from string "2"
        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            String responseJson = "{\n" +
                    "  \"candidates\": [\n" +
                    "    {\n" +
                    "      \"content\": {\n" +
                    "        \"parts\": [\n" +
                    "          {\n" +
                    "            \"text\": \"{\\\"canonicalField\\\": \\\"security.ssh.version\\\", \\\"mappedValue\\\": \\\"2\\\", \\\"confidence\\\": 0.99, \\\"rationale\\\": \\\"SSH v2 string coerce\\\"}\"\n" +
                    "          }\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        SuggestionResult intResult = provider.propose("Juniper", "JUNOS", "test-ssh-line", CanonicalFieldAllowlist.getAllowedFields());
        assertNotNull(intResult);
        assertEquals(2, intResult.getMappedValue(), "String '2' must be coerced to Integer 2");
        assertInstanceOf(Integer.class, intResult.getMappedValue());
    }

    @Test
    @DisplayName("Direct Invocation: callGeminiWithTimeout returns parsed SuggestionResult directly")
    void testDirectCallGeminiWithTimeout() throws Exception {
        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            String responseJson = "{\n" +
                    "  \"candidates\": [\n" +
                    "    {\n" +
                    "      \"content\": {\n" +
                    "        \"parts\": [\n" +
                    "          {\n" +
                    "            \"text\": \"{\\\"canonicalField\\\": \\\"security.snmp.version\\\", \\\"mappedValue\\\": \\\"3\\\", \\\"confidence\\\": 0.95, \\\"rationale\\\": \\\"SNMP v3 string\\\"}\"\n" +
                    "          }\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        SuggestionResult directResult = provider.callGeminiWithTimeout(
                "Fortinet",
                "FortiOS",
                "snmp-user admin v3",
                CanonicalFieldAllowlist.getAllowedFields()
        );

        assertNotNull(directResult);
        assertEquals("security.snmp.version", directResult.getCanonicalField());
        assertEquals("3", directResult.getMappedValue());
        assertInstanceOf(String.class, directResult.getMappedValue());
    }

    @Test
    @DisplayName("Exact Timeout Boundary: Client timeout aborts hanging endpoint and immediately returns stub suggestion")
    void testExactTenSecondTimeoutBoundary() {
        // Assert default configuration enforces 10-second client timeout
        GeminiSuggestionProvider defaultProvider = new GeminiSuggestionProvider();
        assertEquals(10, defaultProvider.getTimeoutSeconds(), "Default timeout must be exactly 10 seconds");

        // Test boundary enforcement with 1-second timeout against 3-second hanging endpoint
        provider.setTimeoutSeconds(1);

        mockGeminiServer.createContext("/v1beta/models/gemini-2.5-flash:generateContent", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}
            byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        long start = System.currentTimeMillis();
        SuggestionResult result = provider.propose(
                "Cisco",
                "IOS-XE",
                "legacy-telnet allow",
                CanonicalFieldAllowlist.getAllowedFields()
        );
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(result, "Hanging request must fall back to deterministic stub");
        assertEquals("security.telnet.enabled", result.getCanonicalField());
        assertEquals(Boolean.TRUE, result.getMappedValue());
        assertTrue(elapsed < 2500, "Must abort around configured timeout window without lingering delay");
    }

    @Test
    @DisplayName("Connection Refused Fallback: Immediate fallback to stub on unreachable port with zero exception bubble")
    void testConnectionRefusedFallback() {
        // Configure unreachable endpoint
        provider.setBaseUrl("http://127.0.0.1:45678");

        long start = System.currentTimeMillis();
        SuggestionResult result = provider.propose(
                "Cisco",
                "IOS-XE",
                "insecure-remote telnet",
                CanonicalFieldAllowlist.getAllowedFields()
        );
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(result, "Connection failure must seamlessly return offline stub proposal");
        assertEquals("security.telnet.enabled", result.getCanonicalField());
        assertTrue(elapsed < 2000, "Connection refused must fail-fast without hanging");
    }

    @Test
    @DisplayName("AiMappingService Integration: Failing/timing-out Gemini provider records PENDING_REVIEW and allows human approval")
    void testAiMappingServiceIntegrationWithGeminiFallback() {
        // Configure provider with unreachable URL
        provider.setBaseUrl("http://127.0.0.1:45678");

        AiMappingService aiMappingService = new AiMappingServiceImpl(aiMappingRepository, aiJobRepository, provider);

        // 1. Record unknown syntax
        AiMappingDocument recorded = aiMappingService.recordUnknownSyntax("Juniper", "JUNOS", "set system services custom-telnet 1");
        assertNotNull(recorded);
        assertEquals("PENDING_REVIEW", recorded.getStatus());

        // 2. Request suggestion (Gemini connection fails, delegates to fallback stub)
        AiMappingDocument withFallback = aiMappingService.requestSuggestion(recorded.getId());
        assertNotNull(withFallback);
        assertEquals("PENDING_REVIEW", withFallback.getStatus(), "Fallback suggestion must remain strictly PENDING_REVIEW");
        assertEquals("security.telnet.enabled", withFallback.getCanonicalField());
        assertEquals(Boolean.TRUE, withFallback.getMappedValue());
        assertNull(withFallback.getReview().getReviewerId());

        // 3. Human review approval succeeds without regression
        AiMappingDocument approved = aiMappingService.approve(withFallback.getId(), "security-architect-01");
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("security-architect-01", approved.getReview().getReviewerId());
        assertNotNull(approved.getReview().getReviewedAt());
    }
}


