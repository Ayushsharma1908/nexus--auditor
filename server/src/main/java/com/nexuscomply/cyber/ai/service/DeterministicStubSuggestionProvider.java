package com.nexuscomply.cyber.ai.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic stub implementation of {@link SuggestionProvider}.
 * Provides predictable, reproducible suggestions without invoking external AI networks/APIs.
 */
@Component
public class DeterministicStubSuggestionProvider implements SuggestionProvider {

    private final Map<String, SuggestionResult> customRules = new ConcurrentHashMap<>();
    private volatile boolean throwException = false;
    private volatile String exceptionMessage = "Simulated AI provider failure/timeout";

    public DeterministicStubSuggestionProvider() {}

    public void registerStub(String rawSyntax, SuggestionResult result) {
        customRules.put(rawSyntax.trim(), result);
    }

    public void clearStubs() {
        customRules.clear();
        throwException = false;
    }

    public void setSimulateException(boolean simulateException) {
        this.throwException = simulateException;
    }

    public void setSimulateException(boolean simulateException, String message) {
        this.throwException = simulateException;
        this.exceptionMessage = message;
    }

    @Override
    public SuggestionResult propose(String vendor, String platform, String rawLine, Set<String> allowedCanonicalFields) {
        if (throwException) {
            throw new RuntimeException(exceptionMessage);
        }

        if (rawLine == null || rawLine.isBlank()) {
            return null;
        }

        String trimmed = rawLine.trim();

        // 1. Check custom registered stubs
        if (customRules.containsKey(trimmed)) {
            return customRules.get(trimmed);
        }

        // 2. Built-in deterministic heuristics for known testing patterns
        String lower = trimmed.toLowerCase();

        // Section 28 scenario: Unknown vendor/command for Telnet
        if (lower.contains("telnet") || lower.contains("legacy-telnet") || lower.contains("insecure-remote")) {
            boolean enabled = !lower.contains("disable") && !lower.contains("deny") && !lower.contains("no");
            if (allowedCanonicalFields == null || allowedCanonicalFields.contains("security.telnet.enabled")) {
                return new SuggestionResult(
                        "security.telnet.enabled",
                        enabled,
                        0.92,
                        "Heuristic match: maps unrecognized command containing 'telnet' to security.telnet.enabled"
                );
            }
        }

        // SSH version patterns
        if (lower.contains("ssh") && (lower.contains("v2") || lower.contains("version 2") || lower.contains("protocol-version 2"))) {
            if (allowedCanonicalFields == null || allowedCanonicalFields.contains("security.ssh.version")) {
                return new SuggestionResult(
                        "security.ssh.version",
                        2,
                        0.95,
                        "Heuristic match: maps unrecognized command specifying SSH v2 to security.ssh.version = 2"
                );
            }
        }

        return null;
    }
}
