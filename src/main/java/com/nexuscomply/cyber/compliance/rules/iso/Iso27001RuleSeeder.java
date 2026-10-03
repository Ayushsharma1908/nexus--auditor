package com.nexuscomply.cyber.compliance.rules.iso;

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
 * Seed component for ISO/IEC 27001:2013 Annex A technical evidence rule set.
 *
 * <p>Compliance and Intellectual Property Notice:
 * <ul>
 *   <li>ISO/IEC 27001 is a proprietary standard. No copyrighted ISO standard text or verbatim wording
 *       is reproduced in this code, comments, or seed data. Only widely-known, public Annex A control
 *       identifiers are used with original technical descriptions.</li>
 *   <li>Confidence Note: Annex A numbering reflects publicly-cited industry mappings (as cited in
 *       cyberlayer.pdf), not verified against a purchased primary copy of the ISO/IEC 27001 standard.</li>
 *   <li>Evaluation results represent technical evidence availability only, NOT an ISO/IEC 27001 certification.</li>
 *   <li>User-facing evaluation label mappings (e.g. PASS -> "Technical evidence available") are stored
 *       in static Framework metadata per specification, without altering core evaluator logic.</li>
 * </ul>
 *
 * <p>Relational Mapping Note:
 * Control {@code A.13.1.1} ("Network controls") is a single Annex A control that encompasses both
 * unencrypted management transport prohibition and secure SNMP management. Both
 * {@code ISO-A.13.1.1-TELNET} and {@code ISO-A.13.1.1-SNMP} compliance rules reference the SAME
 * underlying {@link ControlDocument} {@code _id} for {@code A.13.1.1}.
 */
@Component
public class Iso27001RuleSeeder {

    public static final String FRAMEWORK_CODE = "ISO-IEC-27001-2013";
    public static final String FRAMEWORK_NAME = "ISO/IEC 27001:2013";
    public static final String FRAMEWORK_VERSION = "2013";
    public static final String FRAMEWORK_CATEGORY = "INTERNATIONAL_STANDARD";

    private final FrameworkRepository frameworkRepository;
    private final ControlRepository controlRepository;
    private final ComplianceRuleRepository complianceRuleRepository;

    public Iso27001RuleSeeder(
            FrameworkRepository frameworkRepository,
            ControlRepository controlRepository,
            ComplianceRuleRepository complianceRuleRepository) {
        this.frameworkRepository = frameworkRepository;
        this.controlRepository = controlRepository;
        this.complianceRuleRepository = complianceRuleRepository;
    }

    public static class IsoControlDefinition {
        private final String controlId;
        private final String title;
        private final String category;
        private final String severity;
        private final String description;
        private final List<ControlRequirement> requirements;

        public IsoControlDefinition(
                String controlId,
                String title,
                String category,
                String severity,
                String description,
                List<ControlRequirement> requirements) {
            this.controlId = controlId;
            this.title = title;
            this.category = category;
            this.severity = severity;
            this.description = description;
            this.requirements = requirements != null ? requirements : Collections.emptyList();
        }

        public String getControlId() { return controlId; }
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public List<ControlRequirement> getRequirements() { return requirements; }
    }

    public static class IsoRuleDefinition {
        private final String controlId;
        private final String ruleCode;
        private final String ruleName;
        private final String canonicalField;
        private final String operator;
        private final Object expectedValue;
        private final String severity; // Internal engineering judgment, not ISO-assigned
        private final String technicalDescription;

        public IsoRuleDefinition(
                String controlId,
                String ruleCode,
                String ruleName,
                String canonicalField,
                String operator,
                Object expectedValue,
                String severity,
                String technicalDescription) {
            this.controlId = controlId;
            this.ruleCode = ruleCode;
            this.ruleName = ruleName;
            this.canonicalField = canonicalField;
            this.operator = operator;
            this.expectedValue = expectedValue;
            this.severity = severity;
            this.technicalDescription = technicalDescription;
        }

        public String getControlId() { return controlId; }
        public String getRuleCode() { return ruleCode; }
        public String getRuleName() { return ruleName; }
        public String getCanonicalField() { return canonicalField; }
        public String getOperator() { return operator; }
        public Object getExpectedValue() { return expectedValue; }
        public String getSeverity() { return severity; }
        public String getTechnicalDescription() { return technicalDescription; }

        public String buildRuleDescription() {
            return String.format(
                    "Technical evidence mapping for ISO/IEC 27001 Annex A %s | %s | Severity source: internal, not ISO-assigned",
                    controlId, technicalDescription
            );
        }
    }

    public static final List<IsoControlDefinition> CONTROL_DEFINITIONS = List.of(
            // CONTROL 1 — ISO A.9.4.2 — Secure log-on procedures
            new IsoControlDefinition(
                    "A.9.4.2",
                    "Secure log-on procedures",
                    "A.9 Access Control",
                    "HIGH",
                    "ISO/IEC 27001:2013 Annex A Control A.9.4.2 (Secure log-on procedures). Category: A.9 Access Control. Technical verification: Access to network systems controlled by secure log-on procedures via AAA.",
                    List.of(new ControlRequirement("authentication.aaa", "EQUALS", true))
            ),
            // CONTROL 2 — ISO A.13.1.1 — Network controls (Single control covering Telnet and SNMP)
            new IsoControlDefinition(
                    "A.13.1.1",
                    "Network controls",
                    "A.13 Communications Security",
                    "HIGH",
                    "ISO/IEC 27001:2013 Annex A Control A.13.1.1 (Network controls). Category: A.13 Communications Security. Technical verification: Networks managed and controlled to protect information in transit (unencrypted Telnet prohibited, secure SNMPv3).",
                    List.of(
                            new ControlRequirement("security.telnet.enabled", "EQUALS", false),
                            new ControlRequirement("security.snmp.version", "EQUALS", "3")
                    )
            ),
            // CONTROL 3 — ISO A.10.1.1 — Policy on the use of cryptographic controls
            new IsoControlDefinition(
                    "A.10.1.1",
                    "Policy on the use of cryptographic controls",
                    "A.10 Cryptography",
                    "HIGH",
                    "ISO/IEC 27001:2013 Annex A Control A.10.1.1 (Policy on the use of cryptographic controls). Category: A.10 Cryptography. Technical verification: Technical evidence of strong cryptographic controls applied to management communications (SSHv2).",
                    List.of(new ControlRequirement("security.ssh.version", "EQUALS", 2))
            ),
            // CONTROL 4 — ISO A.12.4.1 — Event logging
            new IsoControlDefinition(
                    "A.12.4.1",
                    "Event logging",
                    "A.12 Operations Security",
                    "MEDIUM",
                    "ISO/IEC 27001:2013 Annex A Control A.12.4.1 (Event logging). Category: A.12 Operations Security. Technical verification: Event logs recording administrative actions and security events produced and forwarded to central syslog.",
                    List.of(new ControlRequirement("logging.syslog", "EQUALS", true))
            ),
            // CONTROL 5 — ISO A.12.4.3 — Administrator and operator logs
            new IsoControlDefinition(
                    "A.12.4.3",
                    "Administrator and operator logs",
                    "A.12 Operations Security",
                    "MEDIUM",
                    "ISO/IEC 27001:2013 Annex A Control A.12.4.3 (Administrator and operator logs). Category: A.12 Operations Security. Technical verification: System administrator and operator activities logged and protected via local buffer configuration.",
                    List.of(new ControlRequirement("logging.localLogging", "EQUALS", true))
            ),
            // CONTROL 6 — ISO A.12.4.4 — Clock synchronisation
            new IsoControlDefinition(
                    "A.12.4.4",
                    "Clock synchronisation",
                    "A.12 Operations Security",
                    "MEDIUM",
                    "ISO/IEC 27001:2013 Annex A Control A.12.4.4 (Clock synchronisation). Category: A.12 Operations Security. Technical verification: Clocks of all information processing systems synchronised to an authoritative reference time source.",
                    List.of(new ControlRequirement("ntp.configured", "EQUALS", true))
            )
    );

    public static final List<IsoRuleDefinition> RULE_DEFINITIONS = List.of(
            // RULE 8 — ISO A.9.4.2 — Secure log-on procedures
            new IsoRuleDefinition(
                    "A.9.4.2",
                    "ISO-A.9.4.2",
                    "Secure log-on procedures (ISO A.9.4.2)",
                    "authentication.aaa",
                    "EQUALS",
                    true,
                    "HIGH",
                    "Access to network systems controlled by secure log-on procedures via AAA"
            ),
            // RULE 9 — ISO A.13.1.1 — Network controls (Management transport)
            new IsoRuleDefinition(
                    "A.13.1.1",
                    "ISO-A.13.1.1-TELNET",
                    "Network controls - Unencrypted transport disabled (ISO A.13.1.1)",
                    "security.telnet.enabled",
                    "EQUALS",
                    false,
                    "HIGH",
                    "Networks managed and controlled to protect information in transit (unencrypted Telnet prohibited)"
            ),
            // RULE 10 — ISO A.10.1.1 — Policy on the use of cryptographic controls
            new IsoRuleDefinition(
                    "A.10.1.1",
                    "ISO-A.10.1.1",
                    "Policy on cryptographic controls - SSHv2 (ISO A.10.1.1)",
                    "security.ssh.version",
                    "EQUALS",
                    2,
                    "HIGH",
                    "Technical evidence of strong cryptographic controls applied to management communications (SSHv2)"
            ),
            // RULE 11 — ISO A.13.1.1 — Network controls (SNMP) — References SAME A.13.1.1 Control
            new IsoRuleDefinition(
                    "A.13.1.1",
                    "ISO-A.13.1.1-SNMP",
                    "Network controls - Secure SNMPv3 (ISO A.13.1.1)",
                    "security.snmp.version",
                    "EQUALS",
                    "3",
                    "HIGH",
                    "Management-plane network protocols secured with encrypted authentication (SNMPv3)"
            ),
            // RULE 12 — ISO A.12.4.1 — Event logging
            new IsoRuleDefinition(
                    "A.12.4.1",
                    "ISO-A.12.4.1",
                    "Event logging - Remote syslog (ISO A.12.4.1)",
                    "logging.syslog",
                    "EQUALS",
                    true,
                    "MEDIUM",
                    "Event logs recording administrative actions and security events produced and forwarded to central syslog"
            ),
            // RULE 13 — ISO A.12.4.3 — Administrator and operator logs
            new IsoRuleDefinition(
                    "A.12.4.3",
                    "ISO-A.12.4.3",
                    "Administrator and operator logs - Local buffering (ISO A.12.4.3)",
                    "logging.localLogging",
                    "EQUALS",
                    true,
                    "MEDIUM",
                    "System administrator and operator activities logged and protected via local buffer configuration"
            ),
            // RULE 14 — ISO A.12.4.4 — Clock synchronisation
            new IsoRuleDefinition(
                    "A.12.4.4",
                    "ISO-A.12.4.4",
                    "Clock synchronisation - NTP (ISO A.12.4.4)",
                    "ntp.configured",
                    "EQUALS",
                    true,
                    "MEDIUM",
                    "Clocks of all information processing systems synchronised to an authoritative reference time source"
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
     * Executes idempotent seeding of the ISO/IEC 27001:2013 Framework, Controls, and ComplianceRules.
     */
    public SeedResult seed() {
        Instant now = Instant.now();

        // 1. Upsert Framework with static resultLabelMap metadata
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
        fw.setDescription("ISO/IEC 27001:2013 Annex A technical security controls baseline for network infrastructure");
        fw.setStatus("ACTIVE");
        fw.setControlCount(CONTROL_DEFINITIONS.size());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("severitySource", "internal, not ISO-assigned");
        metadata.put("confidenceNote", "Publicly-cited Annex A control numbers; not verified against purchased copy of standard");
        metadata.put("disclaimer", "Evaluation results indicate technical evidence coverage only, not ISO/IEC 27001 certification");

        // Static label mapping per specification
        Map<String, String> resultLabelMap = new LinkedHashMap<>();
        resultLabelMap.put("PASS", "Technical evidence available");
        resultLabelMap.put("FAIL", "Technical evidence missing");
        resultLabelMap.put("UNKNOWN", "Partial/contextual");
        resultLabelMap.put("NOT_APPLICABLE", "Not assessable");
        resultLabelMap.put("ERROR", "Evaluation error");
        metadata.put("resultLabelMap", resultLabelMap);

        fw.setMetadata(metadata);
        fw.setUpdatedAt(now);

        fw = frameworkRepository.save(fw);

        // 2. Upsert Controls (6 distinct ISO Annex A controls)
        List<ControlDocument> savedControls = new ArrayList<>();
        Map<String, ControlDocument> controlMap = new LinkedHashMap<>();

        for (IsoControlDefinition def : CONTROL_DEFINITIONS) {
            Optional<ControlDocument> existingCtrlOpt = controlRepository.findByFrameworkIdAndControlId(fw.getId(), def.getControlId());
            ControlDocument ctrl = existingCtrlOpt.orElseGet(ControlDocument::new);
            if (ctrl.getId() == null) {
                ctrl.setId(UUID.randomUUID().toString());
                ctrl.setCreatedAt(now);
            }
            ctrl.setFrameworkId(fw.getId());
            ctrl.setControlId(def.getControlId());
            ctrl.setTitle(def.getTitle());
            ctrl.setDescription(def.getDescription());
            ctrl.setCategory(def.getCategory());
            ctrl.setSeverity(def.getSeverity());
            ctrl.setStatus("ACTIVE");
            ctrl.setRequirements(def.getRequirements());
            ctrl.setUpdatedAt(now);

            ctrl = controlRepository.save(ctrl);
            savedControls.add(ctrl);
            controlMap.put(def.getControlId(), ctrl);
        }

        // 3. Upsert ComplianceRules (7 rules; TELNET and SNMP both link to A.13.1.1's Control _id)
        List<ComplianceRuleDocument> savedRules = new ArrayList<>();

        for (IsoRuleDefinition def : RULE_DEFINITIONS) {
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
