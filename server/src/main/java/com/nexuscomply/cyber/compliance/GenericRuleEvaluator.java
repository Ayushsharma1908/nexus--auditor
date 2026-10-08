package com.nexuscomply.cyber.compliance;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GenericRuleEvaluator implements RuleEvaluator {

    @Override
    public RuleEvaluationResult evaluate(CanonicalSecurityModel canonical, ComplianceRule rule) {
        if (rule == null) {
            return RuleEvaluationResult.error(null, null, "HIGH", "Null compliance rule provided for evaluation");
        }

        String controlId = rule.getControlId();
        String ruleId = rule.getRuleCode() != null ? rule.getRuleCode() : rule.getId();
        String severity = rule.getSeverity() != null ? rule.getSeverity() : "HIGH";

        if (canonical == null) {
            return RuleEvaluationResult.error(controlId, ruleId, severity, "Canonical security model is null");
        }

        RuleRequirement requirement = rule.getRequirement();
        if (requirement == null) {
            return RuleEvaluationResult.error(controlId, ruleId, severity, "Compliance rule has no requirement defined");
        }

        String canonicalField = requirement.getCanonicalField();
        String operator = requirement.getOperator() != null ? requirement.getOperator().toUpperCase().trim() : "EQUALS";
        Object expectedValue = requirement.getExpectedValue();

        if (canonicalField == null || canonicalField.trim().isEmpty()) {
            return RuleEvaluationResult.error(controlId, ruleId, severity, "Rule requirement field path is empty");
        }

        try {
            FieldResolution resolution = resolveFieldValue(canonical, canonicalField.trim());

            // Operator: NOT_EXISTS
            if ("NOT_EXISTS".equals(operator)) {
                if (!resolution.isFound() || resolution.getValue() == null) {
                    return RuleEvaluationResult.pass(controlId, ruleId, "FIELD_NOT_PRESENT", "NOT_PRESENT", severity, canonicalField);
                } else {
                    return RuleEvaluationResult.fail(controlId, ruleId, "FIELD_NOT_PRESENT", resolution.getValue(), severity, canonicalField,
                            "Expected field [" + canonicalField + "] to NOT exist, but it was found with value: " + resolution.getValue());
                }
            }

            // If field is missing/never set in CanonicalSecurityModel
            if (!resolution.isFound() || resolution.getValue() == null) {
                if ("EXISTS".equals(operator)) {
                    return RuleEvaluationResult.fail(controlId, ruleId, "EXISTS", "NOT_FOUND", severity, canonicalField,
                            "Expected field [" + canonicalField + "] to exist, but it was absent from configuration");
                }
                // Mandatory rule: missing evidence returns UNKNOWN, never a default PASS or FAIL
                return RuleEvaluationResult.unknown(controlId, ruleId, expectedValue, severity, canonicalField,
                        "Canonical field [" + canonicalField + "] was not observed or set in the configuration");
            }

            Object actualValue = resolution.getValue();

            // Operator: EXISTS
            if ("EXISTS".equals(operator)) {
                return RuleEvaluationResult.pass(controlId, ruleId, "EXISTS", actualValue, severity, canonicalField);
            }

            // Operator: EQUALS
            if ("EQUALS".equals(operator)) {
                if (areEqual(actualValue, expectedValue)) {
                    return RuleEvaluationResult.pass(controlId, ruleId, expectedValue, actualValue, severity, canonicalField);
                } else {
                    return RuleEvaluationResult.fail(controlId, ruleId, expectedValue, actualValue, severity, canonicalField,
                            "Expected [" + expectedValue + "] but found [" + actualValue + "]");
                }
            }

            // Operator: NOT_EQUALS
            if ("NOT_EQUALS".equals(operator)) {
                if (!areEqual(actualValue, expectedValue)) {
                    return RuleEvaluationResult.pass(controlId, ruleId, "NOT " + expectedValue, actualValue, severity, canonicalField);
                } else {
                    return RuleEvaluationResult.fail(controlId, ruleId, "NOT " + expectedValue, actualValue, severity, canonicalField,
                            "Expected value NOT to equal [" + expectedValue + "], but matched exactly");
                }
            }

            // Operator: GREATER_THAN_OR_EQUAL
            if ("GREATER_THAN_OR_EQUAL".equals(operator)) {
                double actualNum = Double.parseDouble(String.valueOf(actualValue));
                double expectedNum = Double.parseDouble(String.valueOf(expectedValue));
                if (actualNum >= expectedNum) {
                    return RuleEvaluationResult.pass(controlId, ruleId, ">= " + expectedNum, actualNum, severity, canonicalField);
                } else {
                    return RuleEvaluationResult.fail(controlId, ruleId, ">= " + expectedNum, actualNum, severity, canonicalField,
                            "Expected value >= [" + expectedNum + "] but found [" + actualNum + "]");
                }
            }

            return RuleEvaluationResult.error(controlId, ruleId, severity, "Unsupported operator [" + operator + "]");
        } catch (NumberFormatException nfe) {
            return RuleEvaluationResult.error(controlId, ruleId, severity, "Type mismatch during numeric evaluation: " + nfe.getMessage());
        } catch (Exception ex) {
            return RuleEvaluationResult.error(controlId, ruleId, severity, "Error executing compliance check: " + ex.getMessage());
        }
    }

    private boolean areEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;

        if (a.equals(b)) return true;

        if (a instanceof Boolean || b instanceof Boolean) {
            return Boolean.valueOf(String.valueOf(a)).equals(Boolean.valueOf(String.valueOf(b)));
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
        String path = fieldPath;
        if (path.startsWith("security.")) {
            return resolveInMap(canonical.getSecurity(), path.substring("security.".length()));
        } else if (path.startsWith("authentication.")) {
            FieldResolution r = resolveInMap(canonical.getAuthentication(), path.substring("authentication.".length()));
            if (r.isFound()) return r;
            if ("authentication.aaaEnabled".equalsIgnoreCase(path)) {
                return resolveInMap(canonical.getAuthentication(), "aaa");
            }
            return r;
        } else if (path.startsWith("logging.")) {
            FieldResolution r = resolveInMap(canonical.getLogging(), path.substring("logging.".length()));
            if (r.isFound()) return r;
            if ("logging.remoteSyslog.enabled".equalsIgnoreCase(path)) {
                return resolveInMap(canonical.getLogging(), "syslog");
            }
            return r;
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
        } else if (path.startsWith("management.")) {
            String sub = path.substring("management.".length());
            FieldResolution r = resolveInMap(canonical.getSecurity(), sub);
            if (r.isFound()) return r;
            r = resolveInMap(canonical.getManagementAccess(), sub);
            if (r.isFound()) return r;
        } else if (path.startsWith("monitoring.")) {
            String sub = path.substring("monitoring.".length());
            FieldResolution r = resolveInMap(canonical.getSecurity(), sub);
            if (r.isFound()) return r;
            if ("monitoring.snmp.v1Enabled".equalsIgnoreCase(path)) {
                FieldResolution snmp = resolveInMap(canonical.getSecurity(), "snmp.version");
                if (snmp.isFound()) {
                    boolean isV1 = "1".equals(String.valueOf(snmp.getValue())) || "v1".equalsIgnoreCase(String.valueOf(snmp.getValue()));
                    return new FieldResolution(true, isV1);
                }
            }
        } else if (path.startsWith("system.")) {
            String sub = path.substring("system.".length());
            if ("system.ntp.enabled".equalsIgnoreCase(path)) {
                FieldResolution r = resolveInMap(canonical.getNtp(), "configured");
                if (r.isFound()) return r;
                r = resolveInMap(canonical.getNtp(), "enabled");
                if (r.isFound()) return r;
            }
            FieldResolution r = resolveInMap(canonical.getServices(), sub);
            if (r.isFound()) return r;
            r = resolveInMap(canonical.getSecurity(), sub);
            if (r.isFound()) return r;
        }

        // Try direct lookup inside security map first (e.g. "ssh.version")
        FieldResolution res = resolveInMap(canonical.getSecurity(), path);
        if (res.isFound()) return res;

        // Try other domains
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
