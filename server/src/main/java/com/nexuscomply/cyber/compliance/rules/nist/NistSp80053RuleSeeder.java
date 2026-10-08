package com.nexuscomply.cyber.compliance.rules.nist;

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
import java.util.Set;
import java.util.UUID;

/**
 * Seed component for NIST SP 800-53 Revision 5 rule set.
 *
 * <p>Idempotently creates or updates:
 * <ul>
 *   <li>1 Framework: NIST-SP-800-53-R5 (Rev 5)</li>
 *   <li>7 Controls matching real NIST SP 800-53 Rev 5 control identifiers</li>
 *   <li>7 ComplianceRules wired to CanonicalSecurityModel fields reusing identical RuleRequirements</li>
 * </ul>
 *
 * <p>Notes on Severity and Baselines:
 * <ul>
 *   <li>NIST baselines (LOW, MODERATE, HIGH) define control allocation baselines and are NOT severity levels.
 *       Baseline applicability is stored separately in control/rule metadata and descriptions.</li>
 *   <li>Severity ratings (HIGH/MEDIUM) represent internal engineering classifications, not NIST-assigned.</li>
 * </ul>
 */
@Component
public class NistSp80053RuleSeeder {

    public static final String FRAMEWORK_CODE = "NIST-SP-800-53-R5";
    public static final String FRAMEWORK_NAME = "NIST SP 800-53 Revision 5";
    public static final String FRAMEWORK_VERSION = "Rev 5";
    public static final String FRAMEWORK_CATEGORY = "SECURITY_CONTROLS";

    private final FrameworkRepository frameworkRepository;
    private final ControlRepository controlRepository;
    private final ComplianceRuleRepository complianceRuleRepository;

    public NistSp80053RuleSeeder(
            FrameworkRepository frameworkRepository,
            ControlRepository controlRepository,
            ComplianceRuleRepository complianceRuleRepository) {
        this.frameworkRepository = frameworkRepository;
        this.controlRepository = controlRepository;
        this.complianceRuleRepository = complianceRuleRepository;
    }

    public static class NistRuleDefinition {
        private final String controlId;
        private final String controlTitle;
        private final String controlFamily;
        private final List<String> baselines;
        private final String ruleCode;
        private final String ruleName;
        private final String canonicalField;
        private final String operator;
        private final Object expectedValue;
        private final String severity; // Internal engineering judgment, not NIST-assigned
        private final String controlSummary;

        public NistRuleDefinition(
                String controlId,
                String controlTitle,
                String controlFamily,
                List<String> baselines,
                String ruleCode,
                String ruleName,
                String canonicalField,
                String operator,
                Object expectedValue,
                String severity,
                String controlSummary) {
            this.controlId = controlId;
            this.controlTitle = controlTitle;
            this.controlFamily = controlFamily;
            this.baselines = baselines != null ? baselines : Collections.emptyList();
            this.ruleCode = ruleCode;
            this.ruleName = ruleName;
            this.canonicalField = canonicalField;
            this.operator = operator;
            this.expectedValue = expectedValue;
            this.severity = severity;
            this.controlSummary = controlSummary;
        }

        public String getControlId() { return controlId; }
        public String getControlTitle() { return controlTitle; }
        public String getControlFamily() { return controlFamily; }
        public List<String> getBaselines() { return baselines; }
        public String getRuleCode() { return ruleCode; }
        public String getRuleName() { return ruleName; }
        public String getCanonicalField() { return canonicalField; }
        public String getOperator() { return operator; }
        public Object getExpectedValue() { return expectedValue; }
        public String getSeverity() { return severity; }
        public String getControlSummary() { return controlSummary; }

        public String buildControlDescription() {
            return String.format(
                    "NIST SP 800-53 Rev 5 Control %s: %s. Family: %s. Baselines: %s. %s",
                    controlId, controlTitle, controlFamily, baselines, controlSummary
            );
        }

        public String buildRuleDescription() {
            return String.format(
                    "NIST SP 800-53 Rev 5 %s (%s) | Baselines: %s | Requirement: %s %s %s | Severity source: internal, not NIST-assigned",
                    controlId, controlTitle, baselines, canonicalField, operator, expectedValue
            );
        }
    }

    public static final List<NistRuleDefinition> DEFINITIONS = List.of(
            // RULE 1 — NIST IA-2 — Identification and Authentication (Organizational Users)
            new NistRuleDefinition(
                    "IA-2",
                    "Identification and Authentication (Organizational Users)",
                    "Identification and Authentication",
                    List.of("LOW", "MODERATE", "HIGH"),
                    "NIST-IA-2",
                    "Identification and Authentication - Organizational Users (NIST IA-2)",
                    "authentication.aaa",
                    "EQUALS",
                    true,
                    "HIGH",
                    "Requires unique identification and authentication of organizational users before granting network access."
            ),
            // RULE 2 — NIST AC-17 — Remote Access
            new NistRuleDefinition(
                    "AC-17",
                    "Remote Access",
                    "Access Control",
                    List.of("LOW", "MODERATE", "HIGH"),
                    "NIST-AC-17",
                    "Remote Access - Unencrypted Management Disabled (NIST AC-17)",
                    "security.telnet.enabled",
                    "EQUALS",
                    false,
                    "HIGH",
                    "Requires documented restrictions and authorization for remote access; unencrypted management protocols violate requirements."
            ),
            // RULE 3 — NIST SC-8 — Transmission Confidentiality and Integrity
            new NistRuleDefinition(
                    "SC-8",
                    "Transmission Confidentiality and Integrity",
                    "System and Communications Protection",
                    List.of("MODERATE", "HIGH"),
                    "NIST-SC-8",
                    "Transmission Confidentiality and Integrity - SSHv2 (NIST SC-8)",
                    "security.ssh.version",
                    "EQUALS",
                    2,
                    "HIGH",
                    "Protects confidentiality and integrity of transmitted data; mandates secure encrypted transport (SSH version 2)."
            ),
            // RULE 4 — NIST CM-6 — Configuration Settings
            new NistRuleDefinition(
                    "CM-6",
                    "Configuration Settings",
                    "Configuration Management",
                    List.of("LOW", "MODERATE", "HIGH"),
                    "NIST-CM-6",
                    "Configuration Settings - Restrictive SNMPv3 (NIST CM-6)",
                    "security.snmp.version",
                    "EQUALS",
                    "3",
                    "HIGH",
                    "Mandates implementation of restrictive security configuration settings; weak SNMP versions violate baseline."
            ),
            // RULE 5 — NIST AU-2 — Event Logging
            new NistRuleDefinition(
                    "AU-2",
                    "Event Logging",
                    "Audit and Accountability",
                    List.of("LOW", "MODERATE", "HIGH"),
                    "NIST-AU-2",
                    "Event Logging - Remote Syslog Generation (NIST AU-2)",
                    "logging.syslog",
                    "EQUALS",
                    true,
                    "MEDIUM",
                    "Requires identification and generation of security-relevant event types forwarded to remote logging infrastructure."
            ),
            // RULE 6 — NIST AU-12 — Audit Record Generation
            new NistRuleDefinition(
                    "AU-12",
                    "Audit Record Generation",
                    "Audit and Accountability",
                    List.of("LOW", "MODERATE", "HIGH"),
                    "NIST-AU-12",
                    "Audit Record Generation - Local Logging Enabled (NIST AU-12)",
                    "logging.localLogging",
                    "EQUALS",
                    true,
                    "MEDIUM",
                    "Requires active audit record generation and local buffer protection across administrative sessions."
            ),
            // RULE 7 — NIST AU-8 — Time Stamps
            new NistRuleDefinition(
                    "AU-8",
                    "Time Stamps",
                    "Audit and Accountability",
                    List.of("LOW", "MODERATE", "HIGH"),
                    "NIST-AU-8",
                    "Time Stamps - Network Clock Synchronization (NIST AU-8)",
                    "ntp.configured",
                    "EQUALS",
                    true,
                    "MEDIUM",
                    "Requires internal system clocks and audit timestamps to synchronize with an authoritative time source (NTP)."
            )
    );

    public static final Set<String> MULTI_VENDOR_CANONICAL_FIELDS = Set.of(
            "security.telnet.enabled",
            "security.ssh.version",
            "logging.syslog",
            "logging.localLogging",
            "ntp.configured",
            "authentication.aaa",
            "security.snmp.enabled",
            "security.snmp.version"
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

        public FrameworkDocument getFramework() { return framework; }
        public List<ControlDocument> getControls() { return controls; }
        public List<ComplianceRuleDocument> getRules() { return rules; }

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
                rule.setBaselines(doc.getBaselines());
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
     * Executes idempotent seeding of the NIST SP 800-53 Rev 5 Framework, Controls, and ComplianceRules.
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
        fw.setDescription("NIST Special Publication 800-53 Revision 5: Security and Privacy Controls for Information Systems and Organizations");
        fw.setStatus("ACTIVE");
        fw.setControlCount(DEFINITIONS.size());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("severitySource", "internal, not NIST-assigned");
        metadata.put("baselineNotice", "NIST baselines (LOW, MODERATE, HIGH) define control allocation baselines and are not severity levels");
        metadata.put("standard", "NIST SP 800-53 Rev 5");
        fw.setMetadata(metadata);
        fw.setUpdatedAt(now);

        fw = frameworkRepository.save(fw);

        // 2. Upsert Controls
        List<ControlDocument> savedControls = new ArrayList<>();
        Map<String, ControlDocument> controlMap = new LinkedHashMap<>();

        for (NistRuleDefinition def : DEFINITIONS) {
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
            ctrl.setCategory(def.getControlFamily());
            ctrl.setSeverity(def.getSeverity());
            ctrl.setStatus("ACTIVE");
            ctrl.setBaselines(def.getBaselines());
            ctrl.setRequirements(List.of(new ControlRequirement(def.getCanonicalField(), def.getOperator(), def.getExpectedValue())));
            ctrl.setUpdatedAt(now);

            ctrl = controlRepository.save(ctrl);
            savedControls.add(ctrl);
            controlMap.put(def.getControlId(), ctrl);
        }

        // 3. Upsert ComplianceRules
        List<ComplianceRuleDocument> savedRules = new ArrayList<>();

        for (NistRuleDefinition def : DEFINITIONS) {
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
            // Reuses identical RuleRequirement (field, operator, value)
            rule.setExpression(new RuleRequirement(def.getCanonicalField(), def.getOperator(), def.getExpectedValue()));
            rule.setSeverity(def.getSeverity());
            rule.setFrameworkIds(List.of(fw.getId()));
            rule.setBaselines(def.getBaselines());
            if (MULTI_VENDOR_CANONICAL_FIELDS.contains(def.getCanonicalField())) {
                rule.setApplicableVendors(List.of("Cisco", "Juniper", "Fortinet", "Palo Alto"));
                rule.setApplicablePlatforms(List.of("IOS", "IOS-XE", "JUNOS", "FortiOS", "PAN-OS"));
                rule.setApplicableOsVersions(List.of());
            } else {
                rule.setApplicableVendors(List.of("Cisco"));
                rule.setApplicablePlatforms(List.of("IOS", "IOS-XE"));
                rule.setApplicableOsVersions(List.of("17.x"));
            }
            rule.setStatus("ACTIVE");
            rule.setVersion(1);
            rule.setUpdatedAt(now);

            rule = complianceRuleRepository.save(rule);
            savedRules.add(rule);
        }

        return new SeedResult(fw, savedControls, savedRules);
    }
}
