package com.nexuscomply.cyber.remediation.service;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class RemediationVerificationServiceImpl implements RemediationVerificationService {

    @Override
    public RemediationVerificationResult verifyFinding(Finding finding, NormalizedConfigurationDocument laterNormalizedDoc) {
        if (finding == null) {
            throw new IllegalArgumentException("Finding must not be null for verification");
        }
        if (laterNormalizedDoc == null) {
            throw new IllegalArgumentException("Later NormalizedConfigurationDocument must not be null for verification");
        }

        String canonicalField = finding.getCanonicalField();
        Object expected = finding.getExpected();
        Object originalActual = finding.getActual();

        CanonicalSecurityModel canonical = laterNormalizedDoc.getCanonical();
        FieldResolution resolution = resolveFieldValue(canonical, canonicalField);

        Object currentActual = resolution.isFound() ? resolution.getValue() : null;

        boolean resolved = false;
        if (resolution.isFound() && currentActual != null) {
            resolved = areEqual(currentActual, expected);
        } else if (expected == null && !resolution.isFound()) {
            resolved = true;
        }

        Instant now = Instant.now();
        String message;
        if (resolved) {
            message = String.format("Violation resolved: canonical field [%s] value is now [%s], matching expected [%s].",
                    canonicalField, currentActual, expected);
        } else {
            message = String.format("Violation unresolved: canonical field [%s] current value is [%s], still violates expected [%s] (original actual: [%s]).",
                    canonicalField, currentActual, expected, originalActual);
        }

        return new RemediationVerificationResult(
                finding.getId(),
                canonicalField,
                expected,
                originalActual,
                currentActual,
                resolved,
                message,
                now
        );
    }

    private boolean areEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;

        if (a instanceof Boolean && b instanceof Boolean) {
            return a.equals(b);
        }

        if (isNumeric(a) && isNumeric(b)) {
            try {
                return Double.parseDouble(String.valueOf(a)) == Double.parseDouble(String.valueOf(b));
            } catch (NumberFormatException ignored) {}
        }

        return String.valueOf(a).trim().equalsIgnoreCase(String.valueOf(b).trim());
    }

    private boolean isNumeric(Object obj) {
        if (obj instanceof Number) return true;
        try {
            Double.parseDouble(String.valueOf(obj));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private FieldResolution resolveFieldValue(CanonicalSecurityModel canonical, String fieldPath) {
        if (canonical == null || fieldPath == null || fieldPath.isBlank()) {
            return new FieldResolution(false, null);
        }

        String path = fieldPath.trim();
        if (path.startsWith("security.")) {
            return resolveInMap(canonical.getSecurity(), path.substring("security.".length()));
        } else if (path.startsWith("authentication.")) {
            return resolveInMap(canonical.getAuthentication(), path.substring("authentication.".length()));
        } else if (path.startsWith("logging.")) {
            return resolveInMap(canonical.getLogging(), path.substring("logging.".length()));
        } else if (path.startsWith("ntp.")) {
            return resolveInMap(canonical.getNtp(), path.substring("ntp.".length()));
        } else if (path.startsWith("managementAccess.")) {
            return resolveInMap(canonical.getManagementAccess(), path.substring("managementAccess.".length()));
        } else if (path.startsWith("acl.")) {
            return resolveInMap(canonical.getAcl(), path.substring("acl.".length()));
        } else if (path.startsWith("crypto.")) {
            return resolveInMap(canonical.getCrypto(), path.substring("crypto.".length()));
        } else if (path.startsWith("services.")) {
            return resolveInMap(canonical.getServices(), path.substring("services.".length()));
        }

        FieldResolution res = resolveInMap(canonical.getSecurity(), path);
        if (res.isFound()) return res;

        res = resolveInMap(canonical.getAuthentication(), path);
        if (res.isFound()) return res;

        res = resolveInMap(canonical.getLogging(), path);
        if (res.isFound()) return res;

        res = resolveInMap(canonical.getNtp(), path);
        if (res.isFound()) return res;

        return new FieldResolution(false, null);
    }

    @SuppressWarnings("unchecked")
    private FieldResolution resolveInMap(Map<String, Object> map, String path) {
        if (map == null || map.isEmpty()) {
            return new FieldResolution(false, null);
        }

        String[] parts = path.split("\\.", 2);
        String currentKey = parts[0];

        if (!map.containsKey(currentKey)) {
            return new FieldResolution(false, null);
        }

        Object currentVal = map.get(currentKey);

        if (parts.length == 1) {
            return new FieldResolution(true, currentVal);
        }

        if (currentVal instanceof Map) {
            return resolveInMap((Map<String, Object>) currentVal, parts[1]);
        }

        return new FieldResolution(false, null);
    }

    private static class FieldResolution {
        private final boolean found;
        private final Object value;

        public FieldResolution(boolean found, Object value) {
            this.found = found;
            this.value = value;
        }

        public boolean isFound() {
            return found;
        }

        public Object getValue() {
            return value;
        }
    }
}
