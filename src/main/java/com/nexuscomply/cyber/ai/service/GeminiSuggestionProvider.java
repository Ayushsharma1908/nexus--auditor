package com.nexuscomply.cyber.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resilient Google Gemini AI {@link SuggestionProvider} implementation.
 * <p>
 * Connects to Google Gemini (default: {@code gemini-2.5-flash}) to analyze unrecognized configuration
 * syntax and propose canonical security mappings.
 * </p>
 * <p>
 * Employs structured JSON output schema enforcement via {@code responseSchema}, ensuring
 * type safety, deterministic parsing, and strict adherence to {@link CanonicalFieldAllowlist}.
 * </p>
 * <p>
 * Enforces a strict 10-second client timeout window and resiliently delegates to
 * {@link DeterministicStubSuggestionProvider} if:
 * <ul>
 *   <li>The Gemini API key is missing or blank</li>
 *   <li>The provider mode is configured as "stub"</li>
 *   <li>The Gemini HTTP call times out (> 10s)</li>
 *   <li>The Gemini API returns an HTTP error (4xx/5xx)</li>
 *   <li>The Gemini response is malformed, unparseable, or contains disallowed fields</li>
 * </ul>
 * Zero exceptions are surfaced to callers, ensuring audits complete normally.
 * All proposed mappings remain strictly {@code PENDING_REVIEW} until explicit human approval.
 * </p>
 */
@Component
@Primary
public class GeminiSuggestionProvider implements SuggestionProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiSuggestionProvider.class);

    private final DeterministicStubSuggestionProvider fallbackDelegate;
    private volatile String apiKey;
    private volatile String model;
    private volatile int timeoutSeconds;
    private volatile String provider;
    private volatile String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiSuggestionProvider(
            DeterministicStubSuggestionProvider fallbackDelegate,
            @Value("${gemini.api.key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.api.model:gemini-2.5-flash}") String model,
            @Value("${gemini.api.timeout-seconds:10}") int timeoutSeconds,
            @Value("${nexus.ai.provider:${nexuscomply.ai.provider:gemini}}") String provider,
            @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            ObjectMapper objectMapper) {
        this(
                fallbackDelegate,
                apiKey,
                model,
                timeoutSeconds,
                provider,
                baseUrl,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 10)).build(),
                objectMapper != null ? objectMapper : new ObjectMapper()
        );
    }

    public GeminiSuggestionProvider(
            DeterministicStubSuggestionProvider fallbackDelegate,
            String apiKey,
            String model,
            int timeoutSeconds,
            String provider,
            String baseUrl,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.fallbackDelegate = fallbackDelegate != null ? fallbackDelegate : new DeterministicStubSuggestionProvider();
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gemini-2.5-flash";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 10;
        this.provider = (provider != null && !provider.isBlank()) ? provider.trim() : "gemini";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.trim() : "https://generativelanguage.googleapis.com";
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(this.timeoutSeconds)).build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public GeminiSuggestionProvider() {
        this(new DeterministicStubSuggestionProvider(), "", "gemini-2.5-flash", 10, "gemini", "https://generativelanguage.googleapis.com", null, new ObjectMapper());
    }

    @Override
    public SuggestionResult propose(String vendor, String platform, String rawLine, Set<String> allowedCanonicalFields) {
        if (rawLine == null || rawLine.isBlank()) {
            return fallbackDelegate.propose(vendor, platform, rawLine, allowedCanonicalFields);
        }

        // 1. Dual-mode check: if provider is configured as "stub", immediately delegate to offline stub
        if ("stub".equalsIgnoreCase(provider)) {
            log.debug("Provider configured as 'stub'; delegating to DeterministicStubSuggestionProvider.");
            return fallbackDelegate.propose(vendor, platform, rawLine, allowedCanonicalFields);
        }

        // 2. Unconfigured API key check: immediately delegate to fallback
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Gemini API key is not configured; falling back to DeterministicStubSuggestionProvider.");
            return fallbackDelegate.propose(vendor, platform, rawLine, allowedCanonicalFields);
        }

        // 3. Attempt Gemini API call bounded by strict timeout
        try {
            SuggestionResult suggestion = callGeminiWithTimeout(vendor, platform, rawLine, allowedCanonicalFields);
            if (suggestion != null) {
                return suggestion;
            }

            // If Gemini returned a null or unmapped suggestion, allow fallback delegate to evaluate
            return fallbackDelegate.propose(vendor, platform, rawLine, allowedCanonicalFields);

        } catch (HttpTimeoutException timeoutEx) {
            log.warn("Gemini suggestion timed out after {} seconds for line [{}]. Falling back to DeterministicStubSuggestionProvider.",
                    timeoutSeconds, rawLine);
            return fallbackDelegate.propose(vendor, platform, rawLine, allowedCanonicalFields);
        } catch (Exception ex) {
            log.warn("Gemini suggestion request failed for line [{}] ({}). Falling back to DeterministicStubSuggestionProvider.",
                    rawLine, ex.getMessage());
            return fallbackDelegate.propose(vendor, platform, rawLine, allowedCanonicalFields);
        }
    }

    /**
     * Executes the HTTP request to the Google Gemini API with a client timeout.
     * Constructs the structured JSON request with system instructions and response schema.
     *
     * @param vendor the detected device vendor
     * @param platform the detected device platform
     * @param rawLine the unrecognized raw configuration line
     * @param allowedCanonicalFields the allowlist of valid canonical fields
     * @return parsed SuggestionResult, or null if unmapped or HTTP error
     * @throws Exception if transport or timeout error occurs
     */
    public SuggestionResult callGeminiWithTimeout(String vendor, String platform, String rawLine, Set<String> allowedCanonicalFields) throws Exception {
        String requestPayload = buildGeminiRequestPayload(vendor, platform, rawLine, allowedCanonicalFields);
        String endpointUrl = buildEndpointUrl();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(requestPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            log.warn("Gemini API returned HTTP status {}: {}. Falling back to DeterministicStubSuggestionProvider.",
                    response.statusCode(), response.body());
            return null;
        }

        return parseGeminiResponse(response.body(), allowedCanonicalFields);
    }

    private String buildEndpointUrl() {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/v1beta/models/" + model + ":generateContent";
    }

    private String buildGeminiRequestPayload(String vendor, String platform, String rawLine, Set<String> allowedCanonicalFields) throws Exception {
        String allowedFieldsStr = (allowedCanonicalFields != null && !allowedCanonicalFields.isEmpty())
                ? String.join(", ", allowedCanonicalFields)
                : String.join(", ", CanonicalFieldAllowlist.getAllowedFields());

        // System Instructions: expert network security auditor persona
        String systemInstructionText = "You are an expert network security auditor for NEXUS-COMPLY analyzing unknown configuration syntax for vendor "
                + (vendor != null ? vendor : "UNKNOWN") + " and platform " + (platform != null ? platform : "UNKNOWN")
                + ". You MUST choose the best fitting canonicalField from the strictly provided allowedFields list, or set canonicalField to null if no valid mapping exists.";

        Map<String, Object> systemPart = Map.of("text", systemInstructionText);
        Map<String, Object> systemInstruction = Map.of("parts", List.of(systemPart));

        // User Content Prompt
        String userPrompt = "Unrecognized Configuration Line: " + rawLine + "\n\n"
                + "Allowed Canonical Fields:\n" + allowedFieldsStr + "\n\n"
                + "Evaluate if the unrecognized syntax configures any of the allowed canonical fields.\n"
                + "If mapped, return the exact canonicalField, its mappedValue as string, confidence (0.0-1.0), and a concise rationale.";

        Map<String, Object> userPart = Map.of("text", userPrompt);
        Map<String, Object> contentObj = Map.of("parts", List.of(userPart));

        // Generation Config: Structured JSON Response Schema
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("canonicalField", Map.of("type", "STRING", "nullable", true));
        properties.put("mappedValue", Map.of("type", "STRING", "nullable", true));
        properties.put("confidence", Map.of("type", "NUMBER"));
        properties.put("rationale", Map.of("type", "STRING"));
        properties.put("unit", Map.of("type", "STRING", "nullable", true));

        Map<String, Object> responseSchema = new LinkedHashMap<>();
        responseSchema.put("type", "OBJECT");
        responseSchema.put("properties", properties);
        responseSchema.put("required", List.of("confidence", "rationale"));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", responseSchema);

        Map<String, Object> rootPayload = new LinkedHashMap<>();
        rootPayload.put("systemInstruction", systemInstruction);
        rootPayload.put("contents", List.of(contentObj));
        rootPayload.put("generationConfig", generationConfig);

        return objectMapper.writeValueAsString(rootPayload);
    }

    private SuggestionResult parseGeminiResponse(String responseBody, Set<String> allowedCanonicalFields) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                log.warn("Gemini response contained no candidates.");
                return null;
            }

            JsonNode firstCandidate = candidates.get(0);
            JsonNode parts = firstCandidate.path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                log.warn("Gemini candidate content contained no parts.");
                return null;
            }

            String text = parts.get(0).path("text").asText("");
            if (text.isBlank()) {
                return null;
            }

            String jsonText = cleanJsonFence(text);
            JsonNode suggestionNode = objectMapper.readTree(jsonText);

            if (!suggestionNode.hasNonNull("canonicalField")) {
                return null;
            }

            String canonicalField = suggestionNode.get("canonicalField").asText().trim();
            if (canonicalField.isEmpty() || "null".equalsIgnoreCase(canonicalField)) {
                return null;
            }

            // Check against allowed canonical fields
            if (allowedCanonicalFields != null && !allowedCanonicalFields.contains(canonicalField)) {
                log.warn("Gemini suggested canonical field [{}] not in allowed set.", canonicalField);
                return null;
            }

            if (!CanonicalFieldAllowlist.isAllowed(canonicalField)) {
                log.warn("Gemini suggested canonical field [{}] not in CanonicalFieldAllowlist.", canonicalField);
                return null;
            }

            JsonNode valNode = suggestionNode.get("mappedValue");
            Object mappedValue = parseMappedValue(canonicalField, valNode);
            if (mappedValue == null) {
                return null;
            }

            // Validate field and mapped value according to CanonicalSecurityModel types
            CanonicalFieldAllowlist.validateFieldAndValue(canonicalField, mappedValue);

            double confidence = suggestionNode.hasNonNull("confidence")
                    ? suggestionNode.get("confidence").asDouble(0.90)
                    : 0.90;

            String rationale = suggestionNode.hasNonNull("rationale")
                    ? suggestionNode.get("rationale").asText()
                    : (suggestionNode.hasNonNull("reason") ? suggestionNode.get("reason").asText() : "Suggested by Gemini AI");

            String unit = (suggestionNode.hasNonNull("unit") && !suggestionNode.get("unit").isNull())
                    ? suggestionNode.get("unit").asText()
                    : null;

            return new SuggestionResult(canonicalField, mappedValue, unit, confidence, rationale);

        } catch (Exception e) {
            log.warn("Failed to parse Gemini suggestion JSON: {}", e.getMessage());
            return null;
        }
    }

    private Object parseMappedValue(String canonicalField, JsonNode valNode) {
        if (valNode == null || valNode.isNull()) {
            return null;
        }

        Class<?> expectedType = CanonicalFieldAllowlist.getExpectedType(canonicalField);
        if (expectedType == null) {
            return null;
        }

        if (expectedType == Boolean.class) {
            if (valNode.isBoolean()) {
                return valNode.asBoolean();
            }
            String text = valNode.asText().trim().toLowerCase();
            if ("true".equals(text) || "1".equals(text) || "enabled".equals(text) || "yes".equals(text) || "enable".equals(text)) {
                return Boolean.TRUE;
            }
            if ("false".equals(text) || "0".equals(text) || "disabled".equals(text) || "no".equals(text) || "disable".equals(text)) {
                return Boolean.FALSE;
            }
            return Boolean.parseBoolean(text);
        } else if (expectedType == Integer.class) {
            if (valNode.isNumber()) {
                return valNode.asInt();
            }
            try {
                String clean = valNode.asText().replaceAll("[^0-9-]", "");
                return clean.isEmpty() ? null : Integer.parseInt(clean);
            } catch (Exception ex) {
                return null;
            }
        } else if (expectedType == String.class) {
            return valNode.asText().trim();
        }

        return valNode.asText();
    }

    private String cleanJsonFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    public DeterministicStubSuggestionProvider getFallbackDelegate() {
        return fallbackDelegate;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model != null ? model.trim() : "gemini-2.5-flash";
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 10;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider != null ? provider.trim() : "gemini";
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "https://generativelanguage.googleapis.com";
    }
}
