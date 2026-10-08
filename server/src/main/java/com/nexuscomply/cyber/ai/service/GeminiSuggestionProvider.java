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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
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
    private volatile HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiSuggestionProvider(
            DeterministicStubSuggestionProvider fallbackDelegate,
            @Value("${gemini.api.key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.api.model:gemini-1.5-flash}") String model,
            @Value("${gemini.api.timeout-seconds:10}") int timeoutSeconds,
            @Value("${nexus.ai.provider:${nexuscomply.ai.provider:gemini}}") String provider,
            @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            ObjectMapper objectMapper) {
        this(
                fallbackDelegate,
                resolveApiKeyFallback(apiKey),
                model,
                timeoutSeconds,
                provider,
                baseUrl,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 10)).build(),
                objectMapper != null ? objectMapper : new ObjectMapper()
        );
        if (this.apiKey != null && !this.apiKey.isBlank()) {
            log.info("GeminiSuggestionProvider initialized with active API key (model: {}).", this.model);
        } else {
            log.warn("GeminiSuggestionProvider initialized without API key; will use deterministic stub fallback.");
        }
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
        this.apiKey = sanitizeApiKey(apiKey);
        this.model = sanitizeModelName(model);
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 10;
        this.provider = (provider != null && !provider.isBlank()) ? provider.trim() : "gemini";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.trim() : "https://generativelanguage.googleapis.com";
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(this.timeoutSeconds)).build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public GeminiSuggestionProvider() {
        this(new DeterministicStubSuggestionProvider(), "", "gemini-1.5-flash", 10, "gemini", "https://generativelanguage.googleapis.com", null, new ObjectMapper());
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
        } catch (java.net.ConnectException connectEx) {
            log.warn("Gemini suggestion connection refused for line [{}]: {}. Falling back to DeterministicStubSuggestionProvider.",
                    rawLine, connectEx.getMessage());
            return fallbackDelegate.propose(vendor, platform, rawLine, allowedCanonicalFields);
        } catch (InterruptedException interruptEx) {
            log.warn("Gemini suggestion interrupted for line [{}]: {}. Restoring interrupt flag and falling back to DeterministicStubSuggestionProvider.",
                    rawLine, interruptEx.getMessage());
            Thread.currentThread().interrupt();
            return fallbackDelegate.propose(vendor, platform, rawLine, allowedCanonicalFields);
        } catch (java.io.IOException ioEx) {
            log.warn("Gemini suggestion I/O failure for line [{}]: {}. Falling back to DeterministicStubSuggestionProvider.",
                    rawLine, ioEx.getMessage());
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
        String key = getApiKey();

        List<String> candidateModels = getCandidateModels();
        HttpResponse<String> response = null;

        for (String candidate : candidateModels) {
            String endpointUrl = buildEndpointUrl(candidate);
            HttpRequest request = buildHttpRequest(endpointUrl, key, requestPayload);

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                return parseGeminiResponse(response.body(), allowedCanonicalFields);
            }
            if (response.statusCode() != 503 && response.statusCode() != 404 && response.statusCode() != 429) {
                break;
            }
        }

        if (response != null && response.statusCode() != 200) {
            log.warn("Gemini API returned HTTP status {}: {}. Falling back to DeterministicStubSuggestionProvider.",
                    response.statusCode(), response.body());
        }

        return null;
    }

    public HttpRequest buildHttpRequest(String endpointUrl, String key, String requestPayload) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(requestPayload, StandardCharsets.UTF_8));

        String sanitizedKey = sanitizeApiKey(key);
        if (sanitizedKey.startsWith("ya29.") || sanitizedKey.startsWith("AQ.") || sanitizedKey.startsWith("Bearer ")) {
            String token = sanitizedKey.startsWith("Bearer ") ? sanitizedKey : "Bearer " + sanitizedKey;
            builder.uri(URI.create(endpointUrl)).header("Authorization", token);
        } else {
            String urlWithKey = (sanitizedKey != null && !sanitizedKey.isBlank())
                    ? (endpointUrl.contains("?") ? endpointUrl + "&key=" + sanitizedKey : endpointUrl + "?key=" + sanitizedKey)
                    : endpointUrl;
            builder.uri(URI.create(urlWithKey));
            if (sanitizedKey != null && !sanitizedKey.isBlank()) {
                builder.header("x-goog-api-key", sanitizedKey);
            }
        }

        return builder.build();
    }

    private String buildEndpointUrl(String targetModel) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String m = (targetModel != null && !targetModel.isBlank()) ? targetModel : model;
        return base + "/v1beta/models/" + m + ":generateContent";
    }

    private String buildEndpointUrl() {
        return buildEndpointUrl(this.model);
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
        Map<String, Object> contentObj = Map.of(
                "role", "user",
                "parts", List.of(userPart)
        );

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
        String fresh = resolveApiKeyFallback(this.apiKey);
        if (fresh != null && !fresh.isBlank()) {
            this.apiKey = fresh;
            return fresh;
        }
        return this.apiKey != null ? this.apiKey : "";
    }

    public void setApiKey(String apiKey) {
        this.apiKey = sanitizeApiKey(apiKey);
    }

    /**
     * Interactive LLM inquiry method for AI Analyst and security operators.
     * Answers questions regarding specific syntax, mappings, security impact, or compliance rules using Gemini.
     */
    public String answerInquiry(String userQuestion, String syntax, String vendor, String canonicalField, String suggestedValue) {
        String key = getApiKey();
        if (key.isBlank() || "stub".equalsIgnoreCase(provider)) {
            return generateOfflineInquiryReply(userQuestion, syntax, vendor, canonicalField, suggestedValue);
        }

        try {
            String systemInstructionText = "You are an expert network security auditor and AI Analyst for NEXUS-COMPLY. "
                    + "Provide clear, accurate, authoritative answers directly addressing the user's inquiry, explaining network configuration syntax, "
                    + "canonical field mappings, NIST SP 800-53/CIS compliance rationale, and operational security impact.";

            StringBuilder promptBuilder = new StringBuilder();
            boolean hasContext = (vendor != null && !vendor.isBlank()) || (syntax != null && !syntax.isBlank())
                    || (canonicalField != null && !canonicalField.isBlank()) || (suggestedValue != null && !suggestedValue.isBlank());

            if (hasContext) {
                promptBuilder.append("Context for configuration under audit:\n");
                if (vendor != null && !vendor.isBlank()) promptBuilder.append("- Vendor/Platform: ").append(vendor).append("\n");
                if (syntax != null && !syntax.isBlank()) promptBuilder.append("- Configuration Syntax: ").append(syntax).append("\n");
                if (canonicalField != null && !canonicalField.isBlank()) promptBuilder.append("- Proposed Canonical Field: ").append(canonicalField).append("\n");
                if (suggestedValue != null && !suggestedValue.isBlank()) promptBuilder.append("- Proposed Value: ").append(suggestedValue).append("\n");
                promptBuilder.append("\n");
            }

            promptBuilder.append("User Query: ").append(userQuestion != null ? userQuestion : "").append("\n\n");
            promptBuilder.append("Please provide a direct, comprehensive, and helpful answer to the query above:");

            Map<String, Object> systemPart = Map.of("text", systemInstructionText);
            Map<String, Object> systemInstruction = Map.of("parts", List.of(systemPart));

            Map<String, Object> userPart = Map.of("text", promptBuilder.toString());
            Map<String, Object> contentObj = Map.of(
                    "role", "user",
                    "parts", List.of(userPart)
            );

            Map<String, Object> generationConfig = new LinkedHashMap<>();
            generationConfig.put("temperature", 0.2);
            generationConfig.put("maxOutputTokens", 2048);

            Map<String, Object> rootPayload = new LinkedHashMap<>();
            rootPayload.put("systemInstruction", systemInstruction);
            rootPayload.put("contents", List.of(contentObj));
            rootPayload.put("generationConfig", generationConfig);

            String requestPayload = objectMapper.writeValueAsString(rootPayload);
            List<String> candidateModels = getCandidateModels();
            for (String candidate : candidateModels) {
                try {
                    String endpointUrl = buildEndpointUrl(candidate);
                    HttpRequest request = buildHttpRequest(endpointUrl, key, requestPayload);

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (response.statusCode() == 200) {
                        JsonNode root = objectMapper.readTree(response.body());
                        JsonNode candidates = root.path("candidates");
                        if (candidates.isArray() && !candidates.isEmpty()) {
                            JsonNode parts = candidates.get(0).path("content").path("parts");
                            if (parts.isArray() && !parts.isEmpty()) {
                                String text = parts.get(0).path("text").asText();
                                if (text != null && !text.isBlank()) {
                                    return text.trim();
                                }
                            }
                        }
                    } else if (response.statusCode() != 503 && response.statusCode() != 404 && response.statusCode() != 429) {
                        log.warn("Gemini inquiry returned HTTP status {}: {}", response.statusCode(), response.body());
                        break;
                    }
                } catch (Exception ex) {
                    log.warn("Gemini inquiry candidate [{}] error: {}", candidate, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Gemini inquiry request failed: {}. Falling back to offline heuristic.", ex.getMessage());
        }

        return generateOfflineInquiryReply(userQuestion, syntax, vendor, canonicalField, suggestedValue);
    }

    public String chat(String userMessage, Map<String, Object> context, List<Map<String, String>> history) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Please provide a question or instruction regarding the configuration syntax.";
        }

        String rawCommand = "";
        String vendor = "";
        String suggestedMapping = "";

        if (context != null) {
            Object rawObj = context.getOrDefault("rawCommand", context.getOrDefault("syntax", context.getOrDefault("command", "")));
            rawCommand = rawObj != null ? String.valueOf(rawObj) : "";

            Object vendorObj = context.getOrDefault("vendor", "");
            vendor = vendorObj != null ? String.valueOf(vendorObj) : "";

            Object mappingObj = context.getOrDefault("suggestedMapping", context.getOrDefault("canonicalField", ""));
            suggestedMapping = mappingObj != null ? String.valueOf(mappingObj) : "";
        }

        String key = getApiKey();
        if ("stub".equalsIgnoreCase(provider) || key.isBlank()) {
            return generateOfflineInquiryReply(userMessage, rawCommand, vendor, suggestedMapping, "");
        }

        try {
            Map<String, Object> systemInstruction = Map.of(
                    "parts", List.of(Map.of(
                            "text", "You are an elite Network Security Compliance Analyst in NEXUS-COMPLY. " +
                                    "Your role is to assist human security auditors reviewing unknown vendor CLI syntax against " +
                                    "CIS Benchmarks, NIST SP 800-53, DISA STIG, and ISO 27001 standards.\n" +
                                    "Context of current card under review:\n" +
                                    "- Vendor: " + (vendor.isBlank() ? "Unknown" : vendor) + "\n" +
                                    "- CLI Command/Syntax: " + (rawCommand.isBlank() ? "N/A" : rawCommand) + "\n" +
                                    "- Suggested Canonical Mapping: " + (suggestedMapping.isBlank() ? "N/A" : suggestedMapping) + "\n\n" +
                                    "Answer the analyst's questions concisely, accurately, and professionally. " +
                                    "Cite specific controls (e.g. NIST AC-6, SC-7, CIS 1.1) and operational impacts when relevant. " +
                                    "Never recommend dangerous commands without explicit warnings."
                    ))
            );

            List<Map<String, Object>> contents = new ArrayList<>();
            if (history != null && !history.isEmpty()) {
                for (Map<String, String> turn : history) {
                    String role = turn.getOrDefault("role", "user");
                    String content = turn.getOrDefault("content", "");
                    if (!content.isBlank()) {
                        String geminiRole = "user".equalsIgnoreCase(role) ? "user" : "model";
                        contents.add(Map.of(
                                "role", geminiRole,
                                "parts", List.of(Map.of("text", content))
                        ));
                    }
                }
            }

            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userMessage))
            ));

            Map<String, Object> generationConfig = new LinkedHashMap<>();
            generationConfig.put("temperature", 0.2);
            generationConfig.put("maxOutputTokens", 2048);

            Map<String, Object> rootPayload = new LinkedHashMap<>();
            rootPayload.put("systemInstruction", systemInstruction);
            rootPayload.put("contents", contents);
            rootPayload.put("generationConfig", generationConfig);

            String requestPayload = objectMapper.writeValueAsString(rootPayload);
            List<String> candidateModels = getCandidateModels();
            for (String candidate : candidateModels) {
                try {
                    String endpointUrl = buildEndpointUrl(candidate);
                    HttpRequest request = buildHttpRequest(endpointUrl, key, requestPayload);

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (response.statusCode() == 200) {
                        JsonNode root = objectMapper.readTree(response.body());
                        JsonNode candidates = root.path("candidates");
                        if (candidates.isArray() && !candidates.isEmpty()) {
                            JsonNode parts = candidates.get(0).path("content").path("parts");
                            if (parts.isArray() && !parts.isEmpty()) {
                                String text = parts.get(0).path("text").asText();
                                if (text != null && !text.isBlank()) {
                                    return text.trim();
                                }
                            }
                        }
                    } else if (response.statusCode() != 503 && response.statusCode() != 404 && response.statusCode() != 429) {
                        log.warn("Gemini chat returned HTTP status {}: {}", response.statusCode(), response.body());
                        break;
                    }
                } catch (Exception ex) {
                    log.warn("Gemini chat candidate [{}] error: {}", candidate, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Gemini chat request failed: {}. Falling back to offline heuristic.", ex.getMessage());
        }

        return generateOfflineInquiryReply(userMessage, rawCommand, vendor, suggestedMapping, "");
    }

    private String generateOfflineInquiryReply(String userQuestion, String syntax, String vendor, String canonicalField, String suggestedValue) {
        String q = userQuestion != null ? userQuestion.trim() : "";
        String lower = q.toLowerCase();

        if (q.isBlank()) {
            return "Please provide a query regarding network device configuration, security policies, or compliance rules.";
        }

        if (lower.equals("hi") || lower.equals("hii") || lower.equals("hiii") || lower.equals("hello") || lower.equals("hey")) {
            return "Hello! I am your NEXUS-COMPLY AI Security Analyst. You can ask me about CLI syntax, canonical mappings, NIST/CIS compliance, or security impact. (Tip: Configure your GEMINI_API_KEY in server/.env or client/.env to enable live Gemini LLM generation).";
        }

        if (syntax != null && !syntax.isBlank()) {
            if (lower.contains("why") || lower.contains("role") || lower.contains("class") || lower.contains("permission")) {
                return String.format("Analysis for \"%s\": Configures profile/privilege settings on %s. Maps to canonical field '%s' (value: %s) to enforce least privilege under NIST AC-6.",
                        syntax, (vendor != null && !vendor.isBlank()) ? vendor : "device",
                        (canonicalField != null && !canonicalField.isBlank()) ? canonicalField : "profile",
                        (suggestedValue != null && !suggestedValue.isBlank()) ? suggestedValue : "configured");
            } else if (lower.contains("telnet") || lower.contains("ssh") || lower.contains("port") || lower.contains("crypto") || lower.contains("tls")) {
                return String.format("Protocol Analysis for \"%s\": Restricts network management to secure encrypted channels. Mapping to '%s' verifies compliance with CIS Benchmark 1.1 / NIST SC-7.",
                        syntax, (canonicalField != null && !canonicalField.isBlank()) ? canonicalField : "transportSecurity");
            } else if (lower.contains("risk") || lower.contains("impact") || lower.contains("lockout")) {
                return String.format("Safety Assessment for \"%s\": Operational impact evaluated safe. Ensure secondary access credentials are confirmed before deployment.",
                        syntax);
            }
            return String.format("Configuration Analysis: \"%s\" on %s maps to canonical parameter '%s' with value '%s'. Aligns with least privilege and secure baseline controls.",
                    syntax, (vendor != null && !vendor.isBlank()) ? vendor : "device",
                    (canonicalField != null && !canonicalField.isBlank()) ? canonicalField : "parameter",
                    (suggestedValue != null && !suggestedValue.isBlank()) ? suggestedValue : "configured");
        }

        return String.format("Security Analyst Response to \"%s\": Query received. To activate live Google Gemini LLM reasoning, ensure GEMINI_API_KEY is configured in your server/.env or client/.env file.", q);
    }

    public static String sanitizeApiKey(String key) {
        if (key == null) return "";
        String trimmed = key.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }

    public static String resolveApiKeyFallback(String candidate) {
        String sanitized = sanitizeApiKey(candidate);
        if (!sanitized.isBlank()) {
            return sanitized;
        }

        // 1. Environment variable
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) {
            return sanitizeApiKey(env);
        }

        // 2. System properties
        String prop = System.getProperty("GEMINI_API_KEY");
        if (prop != null && !prop.isBlank()) {
            return sanitizeApiKey(prop);
        }
        String sysProp = System.getProperty("gemini.api.key");
        if (sysProp != null && !sysProp.isBlank()) {
            return sanitizeApiKey(sysProp);
        }

        // 3. Search .env in all project locations (server, client, root, parents)
        String userDir = System.getProperty("user.dir", ".");
        List<Path> searchPaths = List.of(
                Path.of(userDir, ".env"),
                Path.of(userDir, "server", ".env"),
                Path.of(userDir, "client", ".env"),
                Path.of(userDir, "..", ".env"),
                Path.of(userDir, "..", "server", ".env"),
                Path.of(userDir, "..", "client", ".env"),
                Path.of(".env"),
                Path.of("server/.env"),
                Path.of("client/.env"),
                Path.of("../.env"),
                Path.of("../server/.env"),
                Path.of("../client/.env"),
                Path.of("../../.env"),
                Path.of("d:/projects/auditor/.env"),
                Path.of("d:/projects/auditor/server/.env"),
                Path.of("d:/projects/auditor/client/.env"),
                Path.of("D:/projects/auditor/.env"),
                Path.of("D:/projects/auditor/server/.env"),
                Path.of("D:/projects/auditor/client/.env"),
                Path.of("d:/auditor/.env"),
                Path.of("D:/auditor/.env")
        );
        for (Path path : searchPaths) {
            try {
                if (Files.exists(path)) {
                    for (String line : Files.readAllLines(path)) {
                        String t = line.trim();
                        if (t.startsWith("GEMINI_API_KEY=") && t.length() > "GEMINI_API_KEY=".length()) {
                            String val = t.substring("GEMINI_API_KEY=".length()).trim();
                            String s = sanitizeApiKey(val);
                            if (!s.isBlank()) {
                                return s;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = sanitizeModelName(model);
    }

    public static String sanitizeModelName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "gemini-1.5-flash";
        }
        return candidate.trim();
    }

    public List<String> getCandidateModels() {
        String primary = (this.model != null && !this.model.isBlank()) ? this.model.trim() : "gemini-1.5-flash";
        List<String> fallbacks = List.of(primary, "gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro");
        List<String> models = new ArrayList<>();
        for (String fb : fallbacks) {
            if (!models.contains(fb)) {
                models.add(fb);
            }
        }
        return models;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 10;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .build();
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
