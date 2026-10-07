package com.nexuscomply.cyber.report;

import com.nexuscomply.cyber.ai.persistence.AiMappingDocument;
import com.nexuscomply.cyber.ai.persistence.AiMappingRepository;
import com.nexuscomply.cyber.audit.AuditSummary;
import com.nexuscomply.cyber.audit.persistence.AuditDocument;
import com.nexuscomply.cyber.audit.persistence.AuditRepository;
import com.nexuscomply.cyber.drift.model.DriftChange;
import com.nexuscomply.cyber.drift.persistence.DriftEventDocument;
import com.nexuscomply.cyber.drift.persistence.DriftEventRepository;
import com.nexuscomply.cyber.evidence.persistence.EvidenceDocument;
import com.nexuscomply.cyber.evidence.persistence.EvidenceRepository;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.compliance.persistence.FrameworkRepository;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.remediation.model.PlanStep;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanDocument;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanRepository;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateDocument;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateRepository;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentDocument;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of DashboardReportingService.
 * Strictly read-only aggregation over existing operational collections.
 * Does not mutate collections or execute database writes.
 */
@Service
public class DashboardReportingServiceImpl implements DashboardReportingService {

    private final AuditRepository auditRepository;
    private final NormalizedConfigurationRepository normalizedConfigRepository;
    private final FindingRepository findingRepository;
    private final EvidenceRepository evidenceRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final DriftEventRepository driftEventRepository;
    private final RemediationPlanRepository remediationPlanRepository;
    private final AiMappingRepository aiMappingRepository;
    private final FrameworkRepository frameworkRepository;
    private final RemediationTemplateRepository remediationTemplateRepository;

    @Autowired
    public DashboardReportingServiceImpl(
            AuditRepository auditRepository,
            NormalizedConfigurationRepository normalizedConfigRepository,
            FindingRepository findingRepository,
            EvidenceRepository evidenceRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            DriftEventRepository driftEventRepository,
            RemediationPlanRepository remediationPlanRepository,
            AiMappingRepository aiMappingRepository,
            @Autowired(required = false) FrameworkRepository frameworkRepository,
            @Autowired(required = false) RemediationTemplateRepository remediationTemplateRepository) {
        this.auditRepository = auditRepository;
        this.normalizedConfigRepository = normalizedConfigRepository;
        this.findingRepository = findingRepository;
        this.evidenceRepository = evidenceRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.driftEventRepository = driftEventRepository;
        this.remediationPlanRepository = remediationPlanRepository;
        this.aiMappingRepository = aiMappingRepository;
        this.frameworkRepository = frameworkRepository;
        this.remediationTemplateRepository = remediationTemplateRepository;
    }

    public DashboardReportingServiceImpl(
            AuditRepository auditRepository,
            NormalizedConfigurationRepository normalizedConfigRepository,
            FindingRepository findingRepository,
            EvidenceRepository evidenceRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            DriftEventRepository driftEventRepository,
            RemediationPlanRepository remediationPlanRepository,
            AiMappingRepository aiMappingRepository) {
        this(auditRepository, normalizedConfigRepository, findingRepository, evidenceRepository,
                riskAssessmentRepository, driftEventRepository, remediationPlanRepository, aiMappingRepository,
                null, null);
    }

    @Override
    public DevicePostureResponse getDevicePosture(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be null or blank");
        }

        // Empty database check: return safe empty response without throwing
        if (auditRepository.count() == 0 && normalizedConfigRepository.count() == 0) {
            DevicePostureResponse empty = new DevicePostureResponse(deviceId);
            empty.setVendor("UNKNOWN");
            empty.setPlatform("UNKNOWN");
            return empty;
        }

        List<AuditDocument> audits = auditRepository.findByDeviceId(deviceId);
        List<NormalizedConfigurationDocument> normDocs = normalizedConfigRepository.findByDeviceId(deviceId);

        if (audits.isEmpty() && normDocs.isEmpty()) {
            throw new NoSuchElementException("Device not found: " + deviceId);
        }

        DevicePostureResponse posture = new DevicePostureResponse(deviceId);

        // Resolve vendor and platform
        String vendor = "UNKNOWN";
        String platform = "UNKNOWN";
        if (!normDocs.isEmpty()) {
            NormalizedConfigurationDocument latestNorm = normDocs.get(normDocs.size() - 1);
            if (latestNorm.getVendor() != null) vendor = latestNorm.getVendor();
            if (latestNorm.getPlatform() != null) platform = latestNorm.getPlatform();
        }
        posture.setVendor(vendor);
        posture.setPlatform(platform);

        // Latest audit summary & compliance score
        AuditDocument latestAudit = null;
        if (!audits.isEmpty()) {
            latestAudit = audits.stream()
                    .max(Comparator.comparing(a -> a.getCompletedAt() != null ? a.getCompletedAt()
                            : (a.getStartedAt() != null ? a.getStartedAt() : a.getCreatedAt())))
                    .orElse(audits.get(0));

            posture.setLatestAuditSummary(latestAudit.getSummary());
            posture.setComplianceScore(latestAudit.getComplianceScore());
            if (latestAudit.getSummary() != null) {
                AuditSummary s = latestAudit.getSummary();
                int denom = s.getTotalControls() - s.getNotApplicable();
                double cov = denom > 0 ? (double) (s.getPassed() + s.getFailed()) / denom : 0.0;
                posture.setCoverage(cov);
                posture.setUnknownCount(s.getUnknown());
                posture.setNotApplicableCount(s.getNotApplicable());
            }
        }

        // Open findings by severity and framework from LATEST audit only
        List<FindingDocument> openFindings = new ArrayList<>();
        if (latestAudit != null) {
            List<FindingDocument> auditFindings = findingRepository.findByAuditIdAndStatus(latestAudit.getId(), "OPEN");
            if (auditFindings.isEmpty()) {
                auditFindings = findingRepository.findByAuditId(latestAudit.getId()).stream()
                        .filter(f -> "OPEN".equalsIgnoreCase(f.getStatus()))
                        .toList();
            }
            openFindings = auditFindings;
        }
        posture.setOpenFindingsCount(openFindings.size());

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        bySeverity.put("CRITICAL", 0L);
        bySeverity.put("HIGH", 0L);
        bySeverity.put("MEDIUM", 0L);
        bySeverity.put("LOW", 0L);
        for (FindingDocument f : openFindings) {
            String sev = f.getSeverity() != null ? f.getSeverity().toUpperCase() : "MEDIUM";
            bySeverity.put(sev, bySeverity.getOrDefault(sev, 0L) + 1L);
        }
        posture.setOpenFindingsBySeverity(bySeverity);

        Map<String, Long> byFramework = new LinkedHashMap<>();
        for (FindingDocument f : openFindings) {
            Set<String> fwKeys = resolveFrameworkKeys(f);
            for (String key : fwKeys) {
                byFramework.put(key, byFramework.getOrDefault(key, 0L) + 1L);
            }
        }
        posture.setOpenFindingsByFramework(byFramework);

        // Top risk assessments (up to 5 sorted by score descending)
        List<RiskAssessmentDocument> allRisks = riskAssessmentRepository.findByDeviceId(deviceId);
        List<RiskAssessmentDocument> topRisks = allRisks.stream()
                .sorted(Comparator.comparingInt(RiskAssessmentDocument::getScore).reversed())
                .limit(5)
                .toList();
        posture.setTopRiskAssessments(topRisks);

        return posture;
    }

    @Override
    public FleetSummaryResponse getFleetSummary() {
        FleetSummaryResponse fleet = new FleetSummaryResponse();

        if (auditRepository.count() == 0 && normalizedConfigRepository.count() == 0) {
            return fleet;
        }

        Set<String> deviceIds = new LinkedHashSet<>();
        List<AuditDocument> allAudits = auditRepository.findAll();
        for (AuditDocument a : allAudits) {
            if (a.getDeviceId() != null) {
                deviceIds.add(a.getDeviceId());
            }
        }
        List<NormalizedConfigurationDocument> allNorms = normalizedConfigRepository.findAll();
        for (NormalizedConfigurationDocument n : allNorms) {
            if (n.getDeviceId() != null) {
                deviceIds.add(n.getDeviceId());
            }
        }

        fleet.setTotalDevices(deviceIds.size());
        fleet.setTotalAudits(allAudits.size());

        List<DevicePostureResponse> postures = new ArrayList<>();
        Map<String, Long> vendorMap = new LinkedHashMap<>();
        Map<String, Long> severityMap = new LinkedHashMap<>();
        severityMap.put("CRITICAL", 0L);
        severityMap.put("HIGH", 0L);
        severityMap.put("MEDIUM", 0L);
        severityMap.put("LOW", 0L);
        Map<String, Long> frameworkMap = new LinkedHashMap<>();

        int totalFindings = 0;
        double sumScore = 0.0;
        int scoredDevicesCount = 0;

        for (String devId : deviceIds) {
            DevicePostureResponse p = getDevicePosture(devId);
            postures.add(p);

            String v = p.getVendor() != null ? p.getVendor() : "UNKNOWN";
            vendorMap.put(v, vendorMap.getOrDefault(v, 0L) + 1L);

            totalFindings += p.getOpenFindingsCount();

            for (Map.Entry<String, Long> e : p.getOpenFindingsBySeverity().entrySet()) {
                severityMap.put(e.getKey(), severityMap.getOrDefault(e.getKey(), 0L) + e.getValue());
            }

            for (Map.Entry<String, Long> e : p.getOpenFindingsByFramework().entrySet()) {
                frameworkMap.put(e.getKey(), frameworkMap.getOrDefault(e.getKey(), 0L) + e.getValue());
            }

            if (p.getComplianceScore() != null) {
                sumScore += p.getComplianceScore();
                scoredDevicesCount++;
            }
        }

        fleet.setDevicesByVendor(vendorMap);
        fleet.setTotalOpenFindings(totalFindings);
        fleet.setFindingsBySeverity(severityMap);
        fleet.setFindingsByFramework(frameworkMap);
        fleet.setDevicePostures(postures);

        if (scoredDevicesCount > 0) {
            double avg = Math.round((sumScore / scoredDevicesCount) * 10.0) / 10.0;
            fleet.setFleetComplianceScore(avg);
        }

        return fleet;
    }

    @Override
    public DeviceDriftHistoryResponse getDeviceDriftHistory(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be null or blank");
        }

        if (driftEventRepository.count() == 0 && auditRepository.count() == 0 && normalizedConfigRepository.count() == 0) {
            return new DeviceDriftHistoryResponse(deviceId);
        }

        List<DriftEventDocument> events = driftEventRepository.findByDeviceIdOrderByDetectedAtDesc(deviceId);
        if (events.isEmpty()) {
            if (auditRepository.findByDeviceId(deviceId).isEmpty()
                    && normalizedConfigRepository.findByDeviceId(deviceId).isEmpty()) {
                throw new NoSuchElementException("Device not found: " + deviceId);
            }
        }

        DeviceDriftHistoryResponse history = new DeviceDriftHistoryResponse(deviceId);
        history.setTotalEvents(events.size());

        List<DriftEventSummary> summaries = new ArrayList<>();
        for (DriftEventDocument doc : events) {
            DriftEventSummary s = new DriftEventSummary();
            s.setEventId(doc.getId());
            s.setFromVersion(doc.getFromVersion());
            s.setToVersion(doc.getToVersion());
            s.setFromVersionId(doc.getFromVersionId());
            s.setToVersionId(doc.getToVersionId());
            s.setDetectedAt(doc.getDetectedAt());
            s.setImpact(doc.getImpact());
            s.setChangesCount(doc.getChanges() != null ? doc.getChanges().size() : 0);
            s.setRiskBefore(doc.getRiskBefore());
            s.setRiskAfter(doc.getRiskAfter());
            s.setRiskDelta(doc.getRiskAfter() - doc.getRiskBefore());
            s.setChanges(doc.getChanges() != null ? doc.getChanges() : new ArrayList<>());
            summaries.add(s);
        }
        history.setEvents(summaries);

        return history;
    }

    @Override
    public AuditReportResponse getAuditReport(String auditId) {
        if (auditId == null || auditId.isBlank()) {
            throw new IllegalArgumentException("auditId must not be null or blank");
        }

        AuditDocument auditDoc = auditRepository.findById(auditId)
                .orElseThrow(() -> new NoSuchElementException("Audit not found: " + auditId));

        AuditReportResponse report = new AuditReportResponse();
        report.setAuditId(auditDoc.getId());
        report.setDeviceId(auditDoc.getDeviceId());
        report.setConfigurationId(auditDoc.getConfigurationId());
        report.setVersionId(auditDoc.getVersionId());
        report.setStatus(auditDoc.getStatus());
        report.setStartedAt(auditDoc.getStartedAt());
        report.setCompletedAt(auditDoc.getCompletedAt());

        AuditSummary summary = auditDoc.getSummary();
        report.setSummary(summary);
        report.setComplianceScore(auditDoc.getComplianceScore());
        if (summary != null) {
            int denom = summary.getTotalControls() - summary.getNotApplicable();
            double cov = denom > 0 ? (double) (summary.getPassed() + summary.getFailed()) / denom : 0.0;
            report.setCoverage(cov);
            report.setUnknownCount(summary.getUnknown());
            report.setNotApplicableCount(summary.getNotApplicable());
        }

        // Vendor & platform resolution
        String vendor = "UNKNOWN";
        String platform = "UNKNOWN";
        if (auditDoc.getNormalizedConfigurationId() != null) {
            NormalizedConfigurationDocument norm = normalizedConfigRepository.findById(auditDoc.getNormalizedConfigurationId()).orElse(null);
            if (norm != null) {
                if (norm.getVendor() != null) vendor = norm.getVendor();
                if (norm.getPlatform() != null) platform = norm.getPlatform();
            }
        }
        report.setVendor(vendor);
        report.setPlatform(platform);

        // Findings with evidence and remediation plans
        List<FindingDocument> findings = findingRepository.findByAuditId(auditId);
        List<FindingReportEntry> entries = new ArrayList<>();
        List<RemediationPlanDocument> plans = new ArrayList<>();

        for (FindingDocument f : findings) {
            List<EvidenceDocument> evidence = new ArrayList<>();
            if (f.getEvidenceIds() != null && !f.getEvidenceIds().isEmpty()) {
                for (String evId : f.getEvidenceIds()) {
                    evidenceRepository.findById(evId).ifPresent(evidence::add);
                }
            }
            if (evidence.isEmpty()) {
                evidence = evidenceRepository.findByFindingId(f.getId());
            }

            RemediationPlanDocument plan = null;
            if (remediationPlanRepository != null) {
                plan = remediationPlanRepository.findFirstByFindingIdOrderByCreatedAtDesc(f.getId()).orElse(null);
            }
            RemediationPlanDocument reportingPlan = null;
            if (plan != null) {
                String confStatus = resolveConfirmationStatus(plan, f, vendor, platform);
                reportingPlan = buildReportingPlan(plan, confStatus);
                if (!plans.contains(reportingPlan)) {
                    plans.add(reportingPlan);
                }
            }

            entries.add(new FindingReportEntry(f, evidence, reportingPlan));
        }

        report.setFindings(entries);
        report.setRemediationPlans(plans);

        // Pending unknown syntax for vendor/platform
        List<AiMappingDocument> unknowns = new ArrayList<>();
        if (aiMappingRepository != null) {
            unknowns = aiMappingRepository.findByVendorAndPlatformAndStatus(vendor, platform, "PENDING_REVIEW");
        }
        report.setPendingUnknownSyntax(unknowns);

        // Render Markdown report
        String md = renderMarkdownReport(report);
        report.setMarkdownReport(md);

        return report;
    }

    private RemediationPlanDocument buildReportingPlan(RemediationPlanDocument plan, String confStatus) {
        RemediationPlanDocument reportingPlan = new RemediationPlanDocument();
        reportingPlan.setId(plan.getId());
        reportingPlan.setFindingId(plan.getFindingId());
        reportingPlan.setDeviceId(plan.getDeviceId());
        reportingPlan.setTemplateId(plan.getTemplateId());
        reportingPlan.setStatus(plan.getStatus());
        reportingPlan.setValidation(plan.getValidation());
        reportingPlan.setVerification(plan.getVerification());
        reportingPlan.setCreatedBy(plan.getCreatedBy());
        reportingPlan.setCreatedAt(plan.getCreatedAt());
        reportingPlan.setUpdatedAt(plan.getUpdatedAt());

        List<PlanStep> reportingSteps = new ArrayList<>();
        if (plan.getSteps() != null) {
            for (PlanStep origStep : plan.getSteps()) {
                String cmd = origStep.getCommand();
                if (cmd != null && !cmd.contains("[CONFIRMED]") && !cmd.contains("[UNCONFIRMED]")) {
                    cmd = cmd + " [" + confStatus + "]";
                }
                reportingSteps.add(new PlanStep(origStep.getOrder(), origStep.getAction(), cmd, origStep.getDescription()));
            }
        }
        reportingPlan.setSteps(reportingSteps);
        return reportingPlan;
    }

    private String resolveConfirmationStatus(RemediationPlanDocument plan, FindingDocument f, String vendor, String platform) {
        RemediationTemplateDocument tmpl = null;
        if (remediationTemplateRepository != null && plan.getTemplateId() != null) {
            tmpl = remediationTemplateRepository.findById(plan.getTemplateId()).orElse(null);
        }
        if (tmpl == null && remediationTemplateRepository != null && f.getCanonicalField() != null) {
            tmpl = remediationTemplateRepository.findByVendorAndPlatformAndCanonicalField(vendor, platform, f.getCanonicalField()).orElse(null);
        }
        if (tmpl != null) {
            if (tmpl.getConfirmationStatus() != null) {
                return tmpl.getConfirmationStatus();
            }
            String desc = tmpl.getDescription() != null ? tmpl.getDescription() : "";
            String title = tmpl.getTitle() != null ? tmpl.getTitle() : "";
            if (desc.contains("[UNCONFIRMED]") || title.contains("[UNCONFIRMED]")) {
                return "UNCONFIRMED";
            }
            if ("Juniper".equalsIgnoreCase(tmpl.getVendor()) || "Juniper".equalsIgnoreCase(vendor)) {
                return "UNCONFIRMED";
            }
            return "CONFIRMED";
        }
        if ("Juniper".equalsIgnoreCase(vendor)) {
            return "UNCONFIRMED";
        }
        return "CONFIRMED";
    }

    private Set<String> resolveFrameworkKeys(FindingDocument f) {
        Set<String> keys = new LinkedHashSet<>();
        if (f.getFrameworkIds() != null && !f.getFrameworkIds().isEmpty()) {
            for (String fwId : f.getFrameworkIds()) {
                String resolved = resolveFrameworkNameOrCode(fwId);
                if (resolved != null) {
                    keys.add(resolved);
                }
            }
        }
        if (keys.isEmpty() && f.getControlCode() != null) {
            String ccUpper = f.getControlCode().toUpperCase();
            if (ccUpper.startsWith("CIS")) keys.add("CIS");
            else if (ccUpper.startsWith("NIST")) keys.add("NIST");
            else if (ccUpper.startsWith("ISO")) keys.add("ISO");
            else keys.add(f.getControlCode());
        }
        if (keys.isEmpty()) {
            keys.add("OTHER");
        }
        return keys;
    }

    private String resolveFrameworkNameOrCode(String fwId) {
        if (fwId == null || fwId.isBlank()) return null;
        String upper = fwId.toUpperCase();
        if (upper.startsWith("CIS")) return "CIS";
        if (upper.startsWith("NIST")) return "NIST";
        if (upper.startsWith("ISO")) return "ISO";
        if (frameworkRepository != null) {
            var opt = frameworkRepository.findById(fwId);
            if (opt.isPresent()) {
                String code = opt.get().getCode() != null ? opt.get().getCode().toUpperCase() : "";
                if (code.startsWith("CIS")) return "CIS";
                if (code.startsWith("NIST")) return "NIST";
                if (code.startsWith("ISO")) return "ISO";
                if (opt.get().getCode() != null && !opt.get().getCode().isBlank()) {
                    return opt.get().getCode();
                }
                if (opt.get().getName() != null && !opt.get().getName().isBlank()) {
                    return opt.get().getName();
                }
            }
        }
        return null;
    }

    private String renderMarkdownReport(AuditReportResponse r) {
        StringBuilder sb = new StringBuilder();
        sb.append("# NEXUS-COMPLY Security & Compliance Audit Report\n\n");
        sb.append("**Audit ID:** `").append(r.getAuditId()).append("`  \n");
        sb.append("**Device ID:** `").append(r.getDeviceId()).append("`  \n");
        sb.append("**Vendor / Platform:** ").append(r.getVendor()).append(" / ").append(r.getPlatform()).append("  \n");
        sb.append("**Configuration ID / Version:** `").append(r.getConfigurationId()).append("` (").append(r.getVersionId()).append(")  \n");
        sb.append("**Audit Status:** ").append(r.getStatus()).append("  \n");
        sb.append("**Completed At:** ").append(r.getCompletedAt() != null ? r.getCompletedAt().toString() : "N/A").append("  \n\n");

        sb.append("## 1. Executive Posture Summary\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("| :--- | :--- |\n");

        AuditSummary s = r.getSummary();
        if (s != null) {
            sb.append("| **Total Controls** | ").append(s.getTotalControls()).append(" |\n");
            sb.append("| **Passed Controls** | ").append(s.getPassed()).append(" |\n");
            sb.append("| **Failed Controls** | ").append(s.getFailed()).append(" |\n");
            sb.append("| **UNKNOWN Controls (Pending Review)** | ").append(r.getUnknownCount()).append(" |\n");
            sb.append("| **Not Applicable Controls** | ").append(r.getNotApplicableCount()).append(" |\n");
            sb.append("| **Evaluated Controls** | ").append(s.getEvaluated()).append(" |\n");
            sb.append("| **Control Coverage** | ").append(String.format("%.1f%%", r.getCoverage() != null ? r.getCoverage() * 100.0 : 0.0)).append(" |\n");
            sb.append("| **Compliance Score** | ").append(r.getComplianceScore() != null ? String.format("%.1f%%", r.getComplianceScore()) : "N/A").append(" |\n\n");
        } else {
            sb.append("| Summary | None |\n\n");
        }

        sb.append("> **Note on UNKNOWN Controls:** UNKNOWN means the field was not observed in the configuration; it is not a pass and may indicate a gap.\n\n");

        sb.append("## 2. Findings & Evidence Traceability (Total: ").append(r.getFindings().size()).append(")\n\n");
        if (r.getFindings().isEmpty()) {
            sb.append("*No compliance findings detected for this audit execution.*\n\n");
        } else {
            for (int i = 0; i < r.getFindings().size(); i++) {
                FindingReportEntry entry = r.getFindings().get(i);
                FindingDocument f = entry.getFinding();
                sb.append("### Finding ").append(i + 1).append(": [").append(f.getControlCode()).append("] ").append(f.getTitle()).append("\n");
                sb.append("- **Severity:** `").append(f.getSeverity()).append("` | **Status:** `").append(f.getStatus()).append("`\n");
                sb.append("- **Canonical Field:** `").append(f.getCanonicalField()).append("`\n");
                sb.append("- **Expected vs Actual:** Expected `").append(f.getExpected()).append("`, got `").append(f.getActual()).append("`\n");
                sb.append("- **Description:** ").append(f.getDescription()).append("\n");
                if (f.getImpact() != null) {
                    sb.append("- **Security Impact:** ").append(f.getImpact()).append("\n");
                }
                if (!entry.getEvidence().isEmpty()) {
                    sb.append("- **Traceable Evidence:**\n");
                    for (EvidenceDocument ev : entry.getEvidence()) {
                        String rawSnippet = ev.getSource() != null ? ev.getSource().getRawText() : "N/A";
                        rawSnippet = com.nexuscomply.cyber.ai.SecretSanitizer.sanitize(rawSnippet);
                        sb.append("  - Line ").append(ev.getSource() != null ? ev.getSource().getLineNumber() : "?")
                                .append(": `").append(rawSnippet).append("`")
                                .append(" (SourceType: `").append(ev.getSource() != null ? ev.getSource().getSourceType() : "N/A").append("`)\n");
                    }
                }
                if (entry.getRemediationPlan() != null) {
                    sb.append("- **Remediation Plan ID:** `").append(entry.getRemediationPlan().getId()).append("` (Status: `").append(entry.getRemediationPlan().getStatus()).append("`)\n");
                }
                sb.append("\n");
            }
        }

        sb.append("## 3. Remediation Plans (Total: ").append(r.getRemediationPlans().size()).append(")\n\n");
        if (r.getRemediationPlans().isEmpty()) {
            sb.append("*No remediation plans generated for this audit execution.*\n\n");
        } else {
            boolean hasUnconfirmed = r.getRemediationPlans().stream()
                    .filter(p -> p.getSteps() != null)
                    .flatMap(p -> p.getSteps().stream())
                    .anyMatch(step -> step.getCommand() != null && step.getCommand().contains("[UNCONFIRMED]"));
            if (hasUnconfirmed) {
                sb.append("> [!WARNING]\n");
                sb.append("> **Unconfirmed Remediation Commands:** One or more remediation commands below are marked as `[UNCONFIRMED]`. These commands represent unconfirmed vendor heuristics and must be validated before production execution.\n\n");
            }
            for (RemediationPlanDocument plan : r.getRemediationPlans()) {
                sb.append("### Plan: `").append(plan.getId()).append("` (Status: `").append(plan.getStatus()).append("`)\n");
                if (plan.getSteps() != null) {
                    for (int sIdx = 0; sIdx < plan.getSteps().size(); sIdx++) {
                        var step = plan.getSteps().get(sIdx);
                        sb.append(sIdx + 1).append(". Action: `").append(step.getAction()).append("` | Command: `").append(step.getCommand()).append("`\n");
                        if (step.getDescription() != null) {
                            sb.append("   - Description: ").append(step.getDescription()).append("\n");
                        }
                    }
                }
                sb.append("\n");
            }
        }

        sb.append("## 4. Unrecognized Syntax Under Review (Total: ").append(r.getPendingUnknownSyntax().size()).append(")\n\n");
        if (r.getPendingUnknownSyntax().isEmpty()) {
            sb.append("*No syntax items currently pending human or AI review for this platform.*\n\n");
        } else {
            for (AiMappingDocument mapping : r.getPendingUnknownSyntax()) {
                String sanitizedSyntax = com.nexuscomply.cyber.ai.SecretSanitizer.sanitize(mapping.getRawSyntax());
                sb.append("- `").append(sanitizedSyntax).append("` (Status: `").append(mapping.getStatus()).append("`)\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
