package com.nexuscomply.cyber.compliance.rules.cis;

import com.nexuscomply.cyber.compliance.RuleRequirement;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.model.ControlRequirement;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleDocument;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.compliance.persistence.ControlDocument;
import com.nexuscomply.cyber.compliance.persistence.ControlRepository;
import com.nexuscomply.cyber.compliance.persistence.FrameworkDocument;
import com.nexuscomply.cyber.compliance.persistence.FrameworkRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Seed component for CIS Cisco IOS XE 17.x Benchmark v2.2.1 rule set.
 *
 * <p>Idempotently creates or updates:
 * <ul>
 *   <li>1 Framework: CIS-CISCO-IOS-XE-17X (v2.2.1)</li>
 *   <li>7 Controls matching real CIS control numbers</li>
 *   <li>7 ComplianceRules wired to CanonicalSecurityModel fields via RuleRequirement</li>
 * </ul>
 *
 * <p>Notes on Severity and Posture:
 * <ul>
 *   <li>Severity ratings (HIGH/MEDIUM) represent internal engineering judgment, not CIS-assigned
 *       (CIS assigns Profile Level 1/2 and Automated/Manual).</li>
 *   <li>HTTPS (ip http secure-server) is intentionally excluded: CIS Cisco IOS XE 17.x Benchmark
 *       favors disabling web management interfaces and does not mandate HTTPS activation as a PASS rule.</li>
 *   <li>SNMPv3 rule (CIS 1.5.9) checks {@code security.snmp.version == "3"}. Verifying the {@code priv}
 *       keyword specifically is a known limitation of the current CanonicalSecurityModel.</li>
 * </ul>
 */
@Component
public class CisCiscoIosXeRuleSeeder {

    public static final String FRAMEWORK_CODE = "CIS-CISCO-IOS-XE-17X";
    public static final String FRAMEWORK_NAME = "CIS Cisco IOS XE 17.x Benchmark";
    public static final String FRAMEWORK_VERSION = "2.2.1";
    public static final String FRAMEWORK_CATEGORY = "NETWORK_DEVICE";

    private final FrameworkRepository frameworkRepository;
    private final ControlRepository controlRepository;
    private final ComplianceRuleRepository complianceRuleRepository;

    public CisCiscoIosXeRuleSeeder(
            FrameworkRepository frameworkRepository,
            ControlRepository controlRepository,
            ComplianceRuleRepository complianceRuleRepository) {
        this.frameworkRepository = frameworkRepository;
        this.controlRepository = controlRepository;
        this.complianceRuleRepository = complianceRuleRepository;
    }

    public static class CisRuleDefinition {
        private final String controlId;
        private final String controlTitle;
        private final String controlCategory;
        private final String ruleCode;
        private final String ruleName;
        private final String canonicalField;
        private final String operator;
        private final Object expectedValue;
        private final String severity; // Internal engineering judgment, not CIS-assigned
        private final String auditRef;
        private final String remediationRef;
        private final String limitationOrNote;

        public CisRuleDefinition(
                String controlId,
                String controlTitle,
                String controlCategory,
                String ruleCode,
                String ruleName,
                String canonicalField,
                String operator,
                Object expectedValue,
                String severity,
                String auditRef,
                String remediationRef,
                String limitationOrNote) {
            this.controlId = controlId;
            this.controlTitle = controlTitle;
            this.controlCategory = controlCategory;
            this.ruleCode = ruleCode;
            this.ruleName = ruleName;
            this.canonicalField = canonicalField;
            this.operator = operator;
            this.expectedValue = expectedValue;
            this.severity = severity;
            this.auditRef = auditRef;
            this.remediationRef = remediationRef;
            this.limitationOrNote = limitationOrNote;
        }

        public String getControlId() { return controlId; }
        public String getControlTitle() { return controlTitle; }
        public String getControlCategory() { return controlCategory; }
        public String getRuleCode() { return ruleCode; }
        public String getRuleName() { return ruleName; }
        public String getCanonicalField() { return canonicalField; }
        public String getOperator() { return operator; }
        public Object getExpectedValue() { return expectedValue; }
        public String getSeverity() { return severity; }
        public String getAuditRef() { return auditRef; }
        public String getRemediationRef() { return remediationRef; }
        public String getLimitationOrNote() { return limitationOrNote; }

        public String buildControlDescription() {
            return String.format(
                    "CIS Cisco IOS XE 17.x Control %s: %s. Audit: %s. Remediation: %s.",
                    controlId, controlTitle, auditRef, remediationRef
            );
        }

        public String buildRuleDescription() {
            StringBuilder sb = new StringBuilder();
            sb.append("Audit: ").append(auditRef)
              .append(" | Remediation: ").append(remediationRef)
              .append(" | Severity source: internal, not CIS-assigned");
            if (limitationOrNote != null && !limitationOrNote.isBlank()) {
                sb.append(" | Note: ").append(limitationOrNote);
            }
            return sb.toString();
        }
    }

    public static final List<CisRuleDefinition> DEFINITIONS = List.of(
            // RULE 1 — CIS 1.1.1
            new CisRuleDefinition(
                    "1.1.1",
                    "Ensure Authentication, Authorization, and Accounting (AAA) is enabled",
                    "Local Authentication, Authorization and Accounting",
                    "CIS-1.1.1",
                    "AAA enabled (CIS 1.1.1)",
                    "authentication.aaa",
                    "EQUALS",
                    true,
                    "HIGH",
                    "show running-config | inc aaa new-model",
                    "aaa new-model",
                    null
            ),
            // RULE 2 — CIS 1.2.2
            new CisRuleDefinition(
                    "1.2.2",
                    "Ensure Telnet is disabled on VTY (SSH-only transport)",
                    "Access Control",
                    "CIS-1.2.2",
                    "Telnet disabled on VTY (CIS 1.2.2)",
                    "security.telnet.enabled",
                    "EQUALS",
                    false,
                    "HIGH",
                    "show running-config | sec vty",
                    "line vty <range> then transport input ssh",
                    null
            ),
            // RULE 3 — CIS 2.1.1.2
            new CisRuleDefinition(
                    "2.1.1.2",
                    "Ensure SSH version 2 is configured",
                    "Network Services",
                    "CIS-2.1.1.2",
                    "SSH version 2 (CIS 2.1.1.2)",
                    "security.ssh.version",
                    "EQUALS",
                    2,
                    "HIGH",
                    "sh ip ssh",
                    "ip ssh version 2",
                    null
            ),
            // RULE 4 — CIS 1.5.9
            new CisRuleDefinition(
                    "1.5.9",
                    "Ensure SNMPv3 privacy (encryption) is required",
                    "Network Protocols",
                    "CIS-1.5.9",
                    "SNMPv3 privacy required (CIS 1.5.9)",
                    "security.snmp.version",
                    "EQUALS",
                    "3",
                    "HIGH",
                    "show snmp group",
                    "snmp-server group {group_name} v3 priv",
                    "Evaluates security.snmp.version == '3'; priv keyword verification is not yet modeled in CanonicalSecurityModel"
            ),
            // RULE 5 — CIS 2.2.1 + 2.2.4
            new CisRuleDefinition(
                    "2.2.1",
                    "Ensure logging is enabled with remote syslog host configured",
                    "System Logging",
                    "CIS-2.2.1",
                    "Logging enabled with remote syslog host (CIS 2.2.1 / 2.2.4)",
                    "logging.syslog",
                    "EQUALS",
                    true,
                    "MEDIUM",
                    "show run | incl logging host (2.2.4) plus logging enable state (2.2.1)",
                    "logging enable / logging host {ip}",
                    null
            ),
            // RULE 6 — CIS 2.2.2 / 2.2.3
            new CisRuleDefinition(
                    "2.2.2",
                    "Ensure local logging is configured (buffered/console)",
                    "System Logging",
                    "CIS-2.2.2",
                    "Local logging configured (CIS 2.2.2 / 2.2.3)",
                    "logging.localLogging",
                    "EQUALS",
                    true,
                    "MEDIUM",
                    "show run | incl logging buffered / show run | incl logging console",
                    "logging buffered {size} / logging console {level}",
                    null
            ),
            // RULE 7 — CIS 2.3.2
            new CisRuleDefinition(
                    "2.3.2",
                    "Ensure authoritative NTP server is configured",
                    "System Clock and Time Services",
                    "CIS-2.3.2",
                    "NTP server configured (CIS 2.3.2)",
                    "ntp.configured",
                    "EQUALS",
                    true,
                    "MEDIUM",
                    "sh ntp associations",
                    "ntp server {ip_address}",
                    null
            )
    );

    public static class SeedResult {
        private final FrameworkDocument framework;
        private final List<ControlDocument> controls;
        private final List<ComplianceRuleDocument> rules;

        public SeedResult(FrameworkDocument framework, List<ControlDocument> controls, List<ComplianceRuleDocument> rules) {
            this.framework = framework;
            this.controls = controls != null ? controls : Collections.emptyList();
            this.rules = rules != null ? rules : Collections.emptyList();
        }

        public FrameworkDocument getFramework() {
            return framework;
        }

        public List<ControlDocument> getControls() {
            return controls;
        }

        public List<ComplianceRuleDocument> getRules() {
            return rules;
        }

        public List<ComplianceRule> toDomainRules() {
            List<ComplianceRule> domainRules = new ArrayList<>();
            for (ComplianceRuleDocument doc : rules) {
                ComplianceRule rule = new ComplianceRule();
                rule.setId(doc.getId());
                rule.setRuleCode(doc.getRuleCode());
                rule.setControlId(doc.getControlId());
                rule.setFrameworkIds(doc.getFrameworkIds());
                if (doc.getFrameworkIds() != null && !doc.getFrameworkIds().isEmpty()) {
                    rule.setFrameworkId(doc.getFrameworkIds().get(0));
                }
                rule.setName(doc.getName());
                rule.setDescription(doc.getDescription());
                rule.setRequirement(doc.getExpression());
                rule.setSeverity(doc.getSeverity());
                rule.setApplicableVendors(doc.getApplicableVendors());
                rule.setApplicablePlatforms(doc.getApplicablePlatforms());
                rule.setApplicableOsVersions(doc.getApplicableOsVersions());
                rule.setStatus(doc.getStatus());
                rule.setVersion(doc.getVersion());
                domainRules.add(rule);
            }
            return domainRules;
        }
    }

    /**
     * Executes idempotent seeding of the CIS Cisco IOS XE 17.x Framework, Controls, and ComplianceRules.
     * Safe to invoke multiple times without generating duplicates.
     */
    public SeedResult seed() {
        Instant now = Instant.now();

        // 1. Upsert Framework
        Optional<FrameworkDocument> existingFwOpt = frameworkRepository.findByCode(FRAMEWORK_CODE);
        FrameworkDocument fw = existingFwOpt.orElseGet(FrameworkDocument::new);
        if (fw.getId() == null) {
            fw.setId(UUID.randomUUID().toString());
            fw.setCreatedAt(now);
        }
        fw.setCode(FRAMEWORK_CODE);
        fw.setName(FRAMEWORK_NAME);
        fw.setVersion(FRAMEWORK_VERSION);
        fw.setCategory(FRAMEWORK_CATEGORY);
        fw.setDescription("CIS Cisco IOS XE 17.x Benchmark v2.2.1 baseline security profile for network devices");
        fw.setStatus("ACTIVE");
        fw.setControlCount(DEFINITIONS.size());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("severitySource", "internal, not CIS-assigned");
        metadata.put("benchmarkVersion", FRAMEWORK_VERSION);
        metadata.put("targetPlatform", "Cisco IOS-XE");
        metadata.put("source", "CIS Cisco IOS XE 17.x Benchmark v2.2.1");
        fw.setMetadata(metadata);
        fw.setUpdatedAt(now);

        fw = frameworkRepository.save(fw);

        // 2. Upsert Controls
        List<ControlDocument> savedControls = new ArrayList<>();
        Map<String, ControlDocument> controlMap = new LinkedHashMap<>();

        for (CisRuleDefinition def : DEFINITIONS) {
            Optional<ControlDocument> existingCtrlOpt = controlRepository.findByFrameworkIdAndControlId(fw.getId(), def.getControlId());
            ControlDocument ctrl = existingCtrlOpt.orElseGet(ControlDocument::new);
            if (ctrl.getId() == null) {
                ctrl.setId(UUID.randomUUID().toString());
                ctrl.setCreatedAt(now);
            }
            ctrl.setFrameworkId(fw.getId());
            ctrl.setControlId(def.getControlId());
            ctrl.setTitle(def.getControlTitle());
            ctrl.setDescription(def.buildControlDescription());
            ctrl.setCategory(def.getControlCategory());
            ctrl.setSeverity(def.getSeverity());
            ctrl.setStatus("ACTIVE");
            ctrl.setRequirements(List.of(new ControlRequirement(def.getCanonicalField(), def.getOperator(), def.getExpectedValue())));
            ctrl.setUpdatedAt(now);

            ctrl = controlRepository.save(ctrl);
            savedControls.add(ctrl);
            controlMap.put(def.getControlId(), ctrl);
        }

        // 3. Upsert ComplianceRules
        List<ComplianceRuleDocument> savedRules = new ArrayList<>();

        for (CisRuleDefinition def : DEFINITIONS) {
            ControlDocument parentCtrl = controlMap.get(def.getControlId());
            String controlDbId = parentCtrl != null ? parentCtrl.getId() : def.getControlId();

            Optional<ComplianceRuleDocument> existingRuleOpt = complianceRuleRepository.findByRuleCode(def.getRuleCode());
            ComplianceRuleDocument rule = existingRuleOpt.orElseGet(ComplianceRuleDocument::new);
            if (rule.getId() == null) {
                rule.setId(UUID.randomUUID().toString());
                rule.setCreatedAt(now);
            }
            rule.setRuleCode(def.getRuleCode());
            rule.setControlId(controlDbId);
            rule.setName(def.getRuleName());
            rule.setDescription(def.buildRuleDescription());
            rule.setExpression(new RuleRequirement(def.getCanonicalField(), def.getOperator(), def.getExpectedValue()));
            rule.setSeverity(def.getSeverity());
            rule.setFrameworkIds(List.of(fw.getId()));
            rule.setApplicableVendors(List.of("Cisco"));
            rule.setApplicablePlatforms(List.of("IOS", "IOS-XE"));
            rule.setApplicableOsVersions(List.of("17.x"));
            rule.setStatus("ACTIVE");
            rule.setVersion(1);
            rule.setUpdatedAt(now);

            rule = complianceRuleRepository.save(rule);
            savedRules.add(rule);
        }

        return new SeedResult(fw, savedControls, savedRules);
    }
}
