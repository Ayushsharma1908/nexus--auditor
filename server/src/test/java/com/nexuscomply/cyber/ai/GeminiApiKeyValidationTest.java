package com.nexuscomply.cyber.ai;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Utility test case to verify whether GEMINI_API_KEY is valid and active
 * against the Google Gemini list models endpoint.
 */
public class GeminiApiKeyValidationTest {

    @Test
    public void testGeminiApiKeyIsValid() throws Exception {
        String apiKey = resolveApiKey();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            fail("GEMINI_API_KEY environment variable is not set.");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey.trim()))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(),
                "API Key is invalid or rate-limited. HTTP " + response.statusCode() + " Response: " + response.body());

        System.out.println(">>> GEMINI API KEY IS VALID AND ACTIVE! <<<");
        System.out.println("Models endpoint returned HTTP 200 OK.");
    }

    private String resolveApiKey() {
        // 1. Environment variable
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return sanitize(envKey);
        }

        // 2. System property (-DGEMINI_API_KEY=... or -Dgemini.api.key=...)
        String propKey = System.getProperty("GEMINI_API_KEY");
        if (propKey != null && !propKey.trim().isEmpty()) {
            return sanitize(propKey);
        }
        String sysPropKey = System.getProperty("gemini.api.key");
        if (sysPropKey != null && !sysPropKey.trim().isEmpty()) {
            return sanitize(sysPropKey);
        }

        // 3. Optional local .env file in project root
        try {
            Path envFile = Path.of("../.env");
            if (!Files.exists(envFile)) {
                envFile = Path.of(".env");
            }
            if (Files.exists(envFile)) {
                for (String line : Files.readAllLines(envFile)) {
                    line = line.trim();
                    if (line.startsWith("GEMINI_API_KEY=") && line.length() > 15) {
                        String val = line.substring("GEMINI_API_KEY=".length()).trim();
                        if (!val.isEmpty()) {
                            return sanitize(val);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 4. Optional application.properties in classpath
        try {
            File propFile = new File("src/main/resources/application.properties");
            if (propFile.exists()) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(propFile)) {
                    props.load(fis);
                    String val = props.getProperty("gemini.api.key");
                    if (val != null && !val.trim().isEmpty() && !val.contains("${")) {
                        return sanitize(val);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return sanitize(null);
    }

    private static String sanitize(String key) {
        if (key == null) return null;
        String trimmed = key.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}
