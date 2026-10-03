package com.nexuscomply.cyber.finding;

import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.compliance.RuleEvaluationResult;
import com.nexuscomply.cyber.compliance.RuleResultStatus;
import com.nexuscomply.cyber.compliance.model.ComplianceRule;
import com.nexuscomply.cyber.compliance.model.Control;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleDocument;
import com.nexuscomply.cyber.compliance.persistence.ComplianceRuleRepository;
import com.nexuscomply.cyber.compliance.persistence.ControlDocument;
import com.nexuscomply.cyber.compliance.persistence.ControlRepository;
import com.nexuscomply.cyber.evidence.Evidence;
import com.nexuscomply.cyber.evidence.EvidenceCreationService;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link FindingCreationService}.
 *
 * <p>Enforces the core rule: Only rule evaluation results with status FAIL produce findings.
 * Finding severity comes directly from the originating rule evaluation result without recalculation.
 *
 * <p>Content Quality Rules:
 * <ul>
 *   <li>Title describes the actual violation found (e.g. "Telnet enabled"), derived from canonical field + observed value.</li>
 *   <li>Impact describes the security consequence (why this matters), not CLI commands or audit syntax.</li>
 *   <li>Control code uses the framework-prefixed convention (e.g. "CIS-1.2.2") matching schema1.md section 10.</li>
 * </ul>
 *
 * <p>Evidence Integration (Task 2.2):
 * <ul>
 *   <li>Invokes {@link EvidenceCreationService} to generate and persist line-level evidence for FAIL findings.</li>
 *   <li>Populates finding.evidenceIds with the real evidence ID.</li>
 * </ul>
 */
@Service
public class FindingCreationServiceImpl implements FindingCreationService {

    private final FindingRepository findingRepository;
    private final ComplianceRuleRepository complianceRuleRepository;
    private final ControlRepository controlRepository;
    private final EvidenceCreationService evidenceCreationService;
    private final NormalizedConfigurationRepository normalizedConfigurationRepository;

    @Autowired
    public FindingCreationServiceImpl(
            FindingRepository findingRepository,
            ComplianceRuleRepository complianceRuleRepository,
            ControlRepository controlRepository,
            @Autowired(required = false) EvidenceCreationService evidenceCreationService,
            @Autowired(required = false) NormalizedConfigurationRepository normalizedConfigurationRepository) {
        this.findingRepository = findingRepository;
        this.complianceRuleRepository = complianceRuleRepository;
        this.controlRepository = controlRepository;
        this.evidenceCreationService = evidenceCreationService;
        this.normalizedConfigurationRepository = normalizedConfigurationRepository;
    }

    public FindingCreationServiceImpl(
            FindingRepository findingRepository,
            ComplianceRuleRepository complianceRuleRepository,
            ControlRepository controlRepository,
            EvidenceCreationService evidenceCreationService) {
        this(findingRepository, complianceRuleRepository, controlRepository, evidenceCreationService, null);
    }

    public FindingCreationServiceImpl(
            FindingRepository findingRepository,
            ComplianceRuleRepository complianceRuleRepository,
            ControlRepository controlRepository) {
        this(findingRepository, complianceRuleRepository, controlRepository, null, null);
    }

    public FindingCreationServiceImpl(FindingRepository findingRepository) {
        this(findingRepository, null, null, null, null);
    }

    @Override
    public Optional<Finding> createFinding(RuleEvaluationResult result, ComplianceRule rule, Control control, FindingContext context) {
        // Absolute Rule 1: A Finding is created ONLY when rule evaluation result has status FAIL.
        if (result == null || result.getStatus() != RuleResultStatus.FAIL) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        FindingDocument doc = new FindingDocument();
        doc.setId(UUID.randomUUID().toString());

        if (context != null) {
            doc.setAuditId(context.auditId());
            doc.setDeviceId(context.deviceId());
            doc.setConfigurationId(context.configurationId());
        }

        // Traceability references per absolute rule 3
        String ruleId = (rule != null && rule.getId() != null) ? rule.getId() : result.getRuleId();
        String controlId = (rule != null && rule.getControlId() != null) ? rule.getControlId() : result.getControlId();
        String controlCode = deriveControlCode(control, rule, controlId);

        doc.setRuleId(ruleId);
        doc.setControlId(controlId);
        doc.setControlCode(controlCode);

        // Titles derived from actual violation per schema1.md example style ("Telnet enabled")
        String title = deriveViolationTitle(result.getEvidenceSourceField(), result.getActual(), result.getExpected(), rule != null ? rule.getName() : null);
        doc.setTitle(title);

        // Description describes the observed non-compliant state
        String description = deriveDescription(result.getEvidenceSourceField(), result.getActual(), result.getExpected(), result.getMessage());
        doc.setDescription(description);

        // Lifecycle & compliance status (starts at OPEN on creation per absolute rule 4)
        doc.setStatus(FindingStatus.OPEN.name());
        doc.setComplianceStatus(result.getStatus().name());

        // Severity comes DIRECTLY from RuleEvaluationResult/ComplianceRule (never recalculated per absolute rule 2)
        String severity = (result.getSeverity() != null && !result.getSeverity().isBlank())
                ? result.getSeverity()
                : (rule != null && rule.getSeverity() != null ? rule.getSeverity() : "HIGH");
        doc.setSeverity(severity);

        // Framework IDs
        List<String> frameworkIds = (rule != null && rule.getFrameworkIds() != null)
                ? new ArrayList<>(rule.getFrameworkIds())
                : new ArrayList<>();
        doc.setFrameworkIds(frameworkIds);

        // Canonical expression details
        doc.setCanonicalField(result.getEvidenceSourceField());
        doc.setExpected(result.getExpected());
        doc.setActual(result.getActual());

        // Plain-language security consequence (why this matters), excluding CLI / remediation text
        String impact = deriveSecurityImpact(result.getEvidenceSourceField(), result.getActual(), result.getExpected());
        doc.setImpact(impact);

        // Task 2.2: Line-Level Evidence creation & linking
        List<String> evidenceIds = new ArrayList<>();
        if (evidenceCreationService != null) {
            List<SourceMapEntry> sourceMap = resolveSourceMap(context);
            Optional<Evidence> evidenceOpt = evidenceCreationService.createEvidence(result, sourceMap, context, doc.getId());
            evidenceOpt.ifPresent(evidence -> evidenceIds.add(evidence.getId()));
        }
        doc.setEvidenceIds(evidenceIds);
        doc.setRemediationAvailable(true);

        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        FindingDocument saved = findingRepository.save(doc);
        return Optional.of(toDomain(saved));
    }

    @Override
    public Optional<Finding> createFinding(RuleEvaluationResult result, FindingContext context) {
        if (result == null || result.getStatus() != RuleResultStatus.FAIL) {
            return Optional.empty();
        }

        ComplianceRule rule = null;
        Control control = null;

        if (complianceRuleRepository != null && result.getRuleId() != null) {
            Optional<ComplianceRuleDocument> ruleDocOpt = complianceRuleRepository.findById(result.getRuleId());
            if (ruleDocOpt.isEmpty()) {
                ruleDocOpt = complianceRuleRepository.findByRuleCode(result.getRuleId());
            }
            if (ruleDocOpt.isPresent()) {
                ComplianceRuleDocument ruleDoc = ruleDocOpt.get();
                rule = new ComplianceRule();
                rule.setId(ruleDoc.getId());
                rule.setRuleCode(ruleDoc.getRuleCode());
                rule.setControlId(ruleDoc.getControlId());
                rule.setName(ruleDoc.getName());
                rule.setDescription(ruleDoc.getDescription());
                rule.setSeverity(ruleDoc.getSeverity());
                rule.setFrameworkIds(ruleDoc.getFrameworkIds());
                rule.setStatus(ruleDoc.getStatus());

                if (controlRepository != null && ruleDoc.getControlId() != null) {
                    Optional<ControlDocument> ctrlDocOpt = controlRepository.findById(ruleDoc.getControlId());
                    if (ctrlDocOpt.isPresent()) {
                        ControlDocument ctrlDoc = ctrlDocOpt.get();
                        control = new Control();
                        control.setId(ctrlDoc.getId());
                        control.setControlId(ctrlDoc.getControlId());
                        control.setTitle(ctrlDoc.getTitle());
                        control.setDescription(ctrlDoc.getDescription());
                        control.setCategory(ctrlDoc.getCategory());
                        control.setSeverity(ctrlDoc.getSeverity());
                    }
                }
            }
        }

        return createFinding(result, rule, control, context);
    }

    /**
     * Resolves the configuration source map from context or normalized repository lookup.
     */
    private List<SourceMapEntry> resolveSourceMap(FindingContext context) {
        if (context != null && context.sourceMap() != null && !context.sourceMap().isEmpty()) {
            return context.sourceMap();
        }

        if (normalizedConfigurationRepository != null && context != null) {
            if (context.versionId() != null) {
                Optional<NormalizedConfigurationDocument> normDocOpt =
                        normalizedConfigurationRepository.findByVersionId(context.versionId());
                if (normDocOpt.isPresent() && normDocOpt.get().getSourceMap() != null) {
                    return normDocOpt.get().getSourceMap();
                }
            }
            if (context.configurationId() != null) {
                Optional<NormalizedConfigurationDocument> normDocOpt =
                        normalizedConfigurationRepository.findByConfigurationId(context.configurationId());
                if (normDocOpt.isPresent() && normDocOpt.get().getSourceMap() != null) {
                    return normDocOpt.get().getSourceMap();
                }
            }
        }

        return List.of();
    }

    /**
     * Derives a problem-focused violation title matching schema1.md example style ("Telnet enabled").
     */
    private String deriveViolationTitle(String canonicalField, Object actual, Object expected, String fallback) {
        if (canonicalField == null) {
            return fallback != null ? fallback : "Compliance check failed";
        }

        return switch (canonicalField) {
            case "security.telnet.enabled" -> Boolean.TRUE.equals(actual) ? "Telnet enabled" : "Telnet disabled";
            case "authentication.aaa" -> Boolean.FALSE.equals(actual) ? "AAA authentication disabled" : "AAA authentication not enabled";
            case "security.ssh.version" -> "Insecure SSH version (" + actual + ") in use";
            case "security.snmp.version" -> "Insecure SNMP version (" + actual + ") in use";
            case "logging.syslog" -> Boolean.FALSE.equals(actual) ? "Remote syslog logging not configured" : "Remote syslog disabled";
            case "logging.localLogging" -> Boolean.FALSE.equals(actual) ? "Local audit logging not configured" : "Local logging disabled";
            case "ntp.configured" -> Boolean.FALSE.equals(actual) ? "Network Time Protocol (NTP) not configured" : "NTP disabled";
            default -> deriveGenericViolationTitle(canonicalField, actual);
        };
    }

    private String deriveGenericViolationTitle(String canonicalField, Object actual) {
        String lastSegment = canonicalField.contains(".")
                ? canonicalField.substring(canonicalField.lastIndexOf('.') + 1)
                : canonicalField;

        String readable = lastSegment.replaceAll("([a-z])([A-Z])", "$1 $2");
        if (readable.endsWith(" enabled") && Boolean.TRUE.equals(actual)) {
            return capitalize(readable);
        }
        if (Boolean.TRUE.equals(actual)) {
            return capitalize(readable) + " enabled";
        }
        if (Boolean.FALSE.equals(actual)) {
            return capitalize(readable) + " disabled";
        }
        return capitalize(readable) + " non-compliant (observed: " + actual + ")";
    }

    /**
     * Derives a plain-language security consequence (why this matters) without CLI commands or remediation syntax.
     */
    private String deriveSecurityImpact(String canonicalField, Object actual, Object expected) {
        if (canonicalField == null) {
            return "Non-compliant security configuration detected, potentially exposing the device to unauthorized access or operational compromise.";
        }

        return switch (canonicalField) {
            case "security.telnet.enabled" ->
                    "Insecure remote management protocol is enabled. Administrative credentials and session traffic are transmitted in cleartext, exposing the device to credential interception, eavesdropping, and unauthorized administrative access.";
            case "authentication.aaa" ->
                    "Centralized authentication, authorization, and accounting (AAA) is not enforced. Device administration relies on local fallbacks or shared credentials without central access revocation, increasing the risk of unauthenticated administrative compromise.";
            case "security.ssh.version" ->
                    "Legacy or insecure SSH protocol version is in use. SSH protocol versions prior to 2 contain known cryptographic vulnerabilities susceptible to man-in-the-middle attacks and session hijacking.";
            case "security.snmp.version" ->
                    "Insecure SNMP management protocol is configured. SNMP versions prior to v3 lack cryptographic encryption and user authentication, exposing community strings and device telemetry in cleartext across the network.";
            case "logging.syslog" ->
                    "Security events and audit records are not forwarded to a central syslog server. This prevents real-time security monitoring, automated alert generation, and post-incident forensic investigation.";
            case "logging.localLogging" ->
                    "Local operational and security event logging is not configured, resulting in loss of forensic evidence and diagnostic history during network disruptions.";
            case "ntp.configured" ->
                    "Network Time Protocol (NTP) synchronization is not configured. Unsynchronized system clocks compromise the integrity of audit record timestamps, preventing accurate log correlation across the enterprise.";
            default ->
                    "Security baseline requirement for [" + canonicalField + "] is not satisfied, potentially exposing the device to unauthorized access, configuration tampering, or operational risk.";
        };
    }

    /**
     * Derives a concise description of the finding matching schema1.md section 10 ("Telnet is enabled for management access.").
     */
    private String deriveDescription(String canonicalField, Object actual, Object expected, String message) {
        if ("security.telnet.enabled".equals(canonicalField) && Boolean.TRUE.equals(actual)) {
            return "Telnet is enabled for management access.";
        }
        if (canonicalField != null) {
            return switch (canonicalField) {
                case "authentication.aaa" -> "Centralized AAA authentication is not enabled on the device.";
                case "security.ssh.version" -> "SSH version " + actual + " is configured instead of required version " + expected + ".";
                case "security.snmp.version" -> "SNMP version " + actual + " is configured instead of required version " + expected + ".";
                case "logging.syslog" -> "Remote syslog logging is not configured to forward security events.";
                case "logging.localLogging" -> "Local buffered or console logging is not configured.";
                case "ntp.configured" -> "Authoritative NTP server synchronization is not configured.";
                default -> message != null && !message.isBlank() ? message : "Compliance check failed for " + canonicalField;
            };
        }
        return message != null && !message.isBlank() ? message : "Compliance rule requirement failed";
    }

    /**
     * Derives framework-prefixed controlCode (e.g. "CIS-1.2.2") matching schema1.md section 10 ("CIS-5.1").
     */
    private String deriveControlCode(Control control, ComplianceRule rule, String controlDbId) {
        String baseCode = (control != null && control.getControlId() != null)
                ? control.getControlId()
                : (controlDbId != null ? controlDbId : "UNKNOWN-CONTROL");

        if (rule != null && rule.getRuleCode() != null && rule.getRuleCode().contains("-")) {
            String prefix = rule.getRuleCode().substring(0, rule.getRuleCode().indexOf('-'));
            if (!baseCode.startsWith(prefix)) {
                return prefix + "-" + baseCode;
            }
        }
        return baseCode;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private Finding toDomain(FindingDocument doc) {
        Finding finding = new Finding();
        finding.setId(doc.getId());
        finding.setAuditId(doc.getAuditId());
        finding.setDeviceId(doc.getDeviceId());
        finding.setConfigurationId(doc.getConfigurationId());
        finding.setControlId(doc.getControlId());
        finding.setRuleId(doc.getRuleId());
        finding.setControlCode(doc.getControlCode());
        finding.setTitle(doc.getTitle());
        finding.setDescription(doc.getDescription());
        finding.setStatus(doc.getStatus());
        finding.setComplianceStatus(doc.getComplianceStatus());
        finding.setSeverity(doc.getSeverity());
        finding.setFrameworkIds(doc.getFrameworkIds());
        finding.setCanonicalField(doc.getCanonicalField());
        finding.setExpected(doc.getExpected());
        finding.setActual(doc.getActual());
        finding.setImpact(doc.getImpact());
        finding.setEvidenceIds(doc.getEvidenceIds());
        finding.setRemediationAvailable(doc.isRemediationAvailable());
        finding.setCreatedAt(doc.getCreatedAt());
        finding.setUpdatedAt(doc.getUpdatedAt());
        return finding;
    }
}
