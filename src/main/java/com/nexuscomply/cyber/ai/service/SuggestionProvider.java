package com.nexuscomply.cyber.ai.service;

import java.util.Set;

/**
 * Contract for proposing canonical mappings for unrecognized syntax constructs.
 */
public interface SuggestionProvider {

    /**
     * Proposes a canonical field mapping for an unknown configuration line.
     *
     * @param vendor the detected device vendor (e.g. "Juniper", "Cisco")
     * @param platform the detected device platform (e.g. "JUNOS", "IOS-XE")
     * @param rawLine the unrecognized raw configuration line
     * @param allowedCanonicalFields the allowlist of valid canonical fields
     * @return proposed canonical field, value, and rationale, or null if no suggestion could be derived
     */
    SuggestionResult propose(String vendor, String platform, String rawLine, Set<String> allowedCanonicalFields);
}
