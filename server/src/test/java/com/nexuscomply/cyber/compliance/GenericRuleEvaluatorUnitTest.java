package com.nexuscomply.cyber.compliance;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenericRuleEvaluatorUnitTest {

    private GenericRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new GenericRuleEvaluator();
    }

    @Test
    @DisplayName("RuleResultStatus has exactly 5 states")
    void testRuleResultStatusEnum() {
        assertThat(RuleResultStatus.values()).containsExactly(
                RuleResultStatus.PASS,
                RuleResultStatus.FAIL,
                RuleResultStatus.UNKNOWN,
                RuleResultStatus.NOT_APPLICABLE,
                RuleResultStatus.ERROR
        );
    }

    @Test
    @DisplayName("Operator EQUALS: evaluates to PASS when matched and FAIL when mismatched")
    void testEqualsOperator() {
        CanonicalSecurityModel model = new CanonicalSecurityModel();
        model.setSsh(true, 2);

        ComplianceRule passRule = new ComplianceRule("r1", "c1", "f1", "SSH-V2",
                new RuleRequirement("security.ssh.version", "EQUALS", 2),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        ComplianceRule failRule = new ComplianceRule("r2", "c1", "f1", "SSH-V1",
                new RuleRequirement("security.ssh.version", "EQUALS", 1),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        RuleEvaluationResult passRes = evaluator.evaluate(model, passRule);
        assertThat(passRes.getStatus()).isEqualTo(RuleResultStatus.PASS);
        assertThat(passRes.getActual()).isEqualTo(2);

        RuleEvaluationResult failRes = evaluator.evaluate(model, failRule);
        assertThat(failRes.getStatus()).isEqualTo(RuleResultStatus.FAIL);
    }

    @Test
    @DisplayName("Operator NOT_EQUALS: evaluates to PASS when different and FAIL when equal")
    void testNotEqualsOperator() {
        CanonicalSecurityModel model = new CanonicalSecurityModel();
        model.setTelnet(false);

        ComplianceRule passRule = new ComplianceRule("r1", "c1", "f1", "NO-TELNET",
                new RuleRequirement("security.telnet.enabled", "NOT_EQUALS", true),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        ComplianceRule failRule = new ComplianceRule("r2", "c1", "f1", "TELNET-ALLOWED",
                new RuleRequirement("security.telnet.enabled", "NOT_EQUALS", false),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        assertThat(evaluator.evaluate(model, passRule).getStatus()).isEqualTo(RuleResultStatus.PASS);
        assertThat(evaluator.evaluate(model, failRule).getStatus()).isEqualTo(RuleResultStatus.FAIL);
    }

    @Test
    @DisplayName("Operator GREATER_THAN_OR_EQUAL: evaluates numerical comparison correctly")
    void testGreaterThanOrEqualOperator() {
        CanonicalSecurityModel model = new CanonicalSecurityModel();
        model.setSsh(true, 2);

        ComplianceRule passRule = new ComplianceRule("r1", "c1", "f1", "SSH-MIN-V2",
                new RuleRequirement("security.ssh.version", "GREATER_THAN_OR_EQUAL", 2),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        ComplianceRule failRule = new ComplianceRule("r2", "c1", "f1", "SSH-MIN-V3",
                new RuleRequirement("security.ssh.version", "GREATER_THAN_OR_EQUAL", 3),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        assertThat(evaluator.evaluate(model, passRule).getStatus()).isEqualTo(RuleResultStatus.PASS);
        assertThat(evaluator.evaluate(model, failRule).getStatus()).isEqualTo(RuleResultStatus.FAIL);
    }

    @Test
    @DisplayName("Operator EXISTS: evaluates to PASS if present, FAIL if absent")
    void testExistsOperator() {
        CanonicalSecurityModel model = new CanonicalSecurityModel();
        model.setAaa(true);

        ComplianceRule passRule = new ComplianceRule("r1", "c1", "f1", "AAA-EXISTS",
                new RuleRequirement("authentication.aaa", "EXISTS", null),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        ComplianceRule failRule = new ComplianceRule("r2", "c1", "f1", "SNMP-EXISTS",
                new RuleRequirement("security.snmp.version", "EXISTS", null),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        assertThat(evaluator.evaluate(model, passRule).getStatus()).isEqualTo(RuleResultStatus.PASS);
        assertThat(evaluator.evaluate(model, failRule).getStatus()).isEqualTo(RuleResultStatus.FAIL);
    }

    @Test
    @DisplayName("Operator NOT_EXISTS: evaluates to PASS if absent, FAIL if present")
    void testNotExistsOperator() {
        CanonicalSecurityModel model = new CanonicalSecurityModel();
        model.setTelnet(true);

        ComplianceRule failRule = new ComplianceRule("r1", "c1", "f1", "NO-TELNET-CONFIG",
                new RuleRequirement("security.telnet.enabled", "NOT_EXISTS", null),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        ComplianceRule passRule = new ComplianceRule("r2", "c1", "f1", "NO-LEGACY-CRYPTO",
                new RuleRequirement("crypto.des56", "NOT_EXISTS", null),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        assertThat(evaluator.evaluate(model, failRule).getStatus()).isEqualTo(RuleResultStatus.FAIL);
        assertThat(evaluator.evaluate(model, passRule).getStatus()).isEqualTo(RuleResultStatus.PASS);
    }

    @Test
    @DisplayName("Absent canonical field produces UNKNOWN, never a default PASS or FAIL")
    void testAbsentFieldReturnsUnknown() {
        CanonicalSecurityModel model = new CanonicalSecurityModel();
        // Model has no NTP field set at all

        ComplianceRule rule = new ComplianceRule("r1", "c1", "f1", "NTP-CONFIGURED",
                new RuleRequirement("ntp.configured", "EQUALS", true),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);

        RuleEvaluationResult result = evaluator.evaluate(model, rule);

        assertThat(result.getStatus()).isEqualTo(RuleResultStatus.UNKNOWN);
        assertThat(result.getMessage()).contains("was not observed or set");
    }

    @Test
    @DisplayName("Malformed/Null parameters return ERROR, never PASS")
    void testErrorHandlingNeverPasses() {
        CanonicalSecurityModel model = new CanonicalSecurityModel();

        // Null rule
        RuleEvaluationResult r1 = evaluator.evaluate(model, null);
        assertThat(r1.getStatus()).isEqualTo(RuleResultStatus.ERROR);

        // Null requirement
        ComplianceRule ruleNoReq = new ComplianceRule();
        RuleEvaluationResult r2 = evaluator.evaluate(model, ruleNoReq);
        assertThat(r2.getStatus()).isEqualTo(RuleResultStatus.ERROR);

        // Numeric parsing error returns ERROR
        model.getSecurity().put("customField", "not-a-number");
        ComplianceRule numRule = new ComplianceRule("r3", "c1", "f1", "NUM-RULE",
                new RuleRequirement("security.customField", "GREATER_THAN_OR_EQUAL", 5),
                "HIGH", List.of(), List.of(), "ACTIVE", 1);
        RuleEvaluationResult r3 = evaluator.evaluate(model, numRule);
        assertThat(r3.getStatus()).isEqualTo(RuleResultStatus.ERROR);
    }
}
