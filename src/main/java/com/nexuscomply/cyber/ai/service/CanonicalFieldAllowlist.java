package com.nexuscomply.cyber.ai.service;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of allowlisted canonical fields and their expected data types,
 * derived strictly from {@link CanonicalSecurityModel}.
 */
public final class CanonicalFieldAllowlist {

    private static final Map<String, Class<?>> ALLOWED_FIELDS;

    static {
        Map<String, Class<?>> map = new LinkedHashMap<>();
        map.put("security.telnet.enabled", Boolean.class);
        map.put("security.ssh.enabled", Boolean.class);
        map.put("security.ssh.version", Integer.class);
        map.put("security.https.enabled", Boolean.class);
        map.put("security.snmp.enabled", Boolean.class);
        map.put("security.snmp.version", String.class);
        map.put("authentication.aaa", Boolean.class);
        map.put("logging.syslog", Boolean.class);
        map.put("logging.localLogging", Boolean.class);
        map.put("ntp.configured", Boolean.class);
        ALLOWED_FIELDS = Collections.unmodifiableMap(map);
    }

    private CanonicalFieldAllowlist() {}

    public static Set<String> getAllowedFields() {
        return ALLOWED_FIELDS.keySet();
    }

    public static boolean isAllowed(String canonicalField) {
        return canonicalField != null && ALLOWED_FIELDS.containsKey(canonicalField);
    }

    public static Class<?> getExpectedType(String canonicalField) {
        return ALLOWED_FIELDS.get(canonicalField);
    }

    public static void validateFieldAndValue(String field, Object value) {
        if (field == null || !ALLOWED_FIELDS.containsKey(field)) {
            throw new IllegalArgumentException("Field [" + field + "] is not in the canonical security model allowlist");
        }
        if (value == null) {
            throw new IllegalArgumentException("Mapped value cannot be null for field [" + field + "]");
        }
        Class<?> expectedType = ALLOWED_FIELDS.get(field);
        if (expectedType == Integer.class && value instanceof Number) {
            return;
        }
        if (!expectedType.isInstance(value)) {
            throw new IllegalArgumentException("Invalid value type [" + value.getClass().getSimpleName()
                    + "] for field [" + field + "], expected [" + expectedType.getSimpleName() + "]");
        }
    }

    /**
     * Applies a validated canonical field and value to a CanonicalSecurityModel instance.
     */
    public static void applyToCanonical(CanonicalSecurityModel canonical, String field, Object value) {
        if (canonical == null || field == null || value == null) {
            return;
        }
        switch (field) {
            case "security.telnet.enabled" -> canonical.setTelnet((Boolean) value);
            case "security.ssh.enabled" -> canonical.setSsh((Boolean) value, null);
            case "security.ssh.version" -> {
                int ver = (value instanceof Number num) ? num.intValue() : Integer.parseInt(value.toString());
                canonical.setSsh(null, ver);
            }
            case "security.https.enabled" -> canonical.setHttps((Boolean) value);
            case "security.snmp.enabled" -> canonical.setSnmp((Boolean) value, null);
            case "security.snmp.version" -> canonical.setSnmp(null, value.toString());
            case "authentication.aaa" -> canonical.setAaa((Boolean) value);
            case "logging.syslog" -> canonical.setSyslog((Boolean) value);
            case "logging.localLogging" -> canonical.setLocalLogging((Boolean) value);
            case "ntp.configured" -> canonical.setNtpConfigured((Boolean) value);
            default -> throw new IllegalArgumentException("Unsupported canonical field: " + field);
        }
    }
}
