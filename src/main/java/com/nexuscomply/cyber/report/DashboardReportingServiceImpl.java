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
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanDocument;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanRepository;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentDocument;
import com.nexuscomply.cyber.risk.persistence.RiskAssessmentRepository;
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

    public DashboardReportingServiceImpl(
            AuditRepository auditRepository,
            NormalizedConfigurationRepository normalizedConfigRepository,
            FindingRepository findingRepository,
            EvidenceRepository evidenceRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            DriftEventRepository driftEventRepository,
            RemediationPlanRepository remediationPlanRepository,
            AiMappingRepository aiMappingRepository) {
        this.auditRepository = auditRepository;
        this.normalizedConfigRepository = normalizedConfigRepository;
        this.findingRepository = findingRepository;
        this.evidenceRepository = evidenceRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.driftEventRepository = driftEventRepository;
        this.remediationPlanRepository = remediationPlanRepository;
        this.aiMappingRepository = aiMappingRepository;
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
        if (!audits.isEmpty()) {
            AuditDocument latestAudit = audits.stream()
                    .max(Comparator.comparing(a -> a.getCompletedAt() != null ? a.getCompletedAt()
                            : (a.getStartedAt() != null ? a.getStartedAt() : a.getCreatedAt())))
                    .orElse(audits.get(0));

            posture.setLatestAuditSummary(latestAudit.getSummary());
            posture.setComplianceScore(latestAudit.getComplianceScore());
            if (latestAudit.getSummary() != null) {
                posture.setCoverage(latestAudit.getSummary().getCoverage());
            }
        }

        // Open findings by severity and framework
        List<FindingDocument> openFindings = findingRepository.findByDeviceIdAndStatus(deviceId, "OPEN");
        if (openFindings.isEmpty()) {
            openFindings = findingRepository.findByDeviceId(deviceId).stream()
                    .filter(f -> "OPEN".equalsIgnoreCase(f.getStatus()))
                    .toList();
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
            if (f.getFrameworkIds() != null) {
                for (String fwId : f.getFrameworkIds()) {
                    byFramework.put(fwId, byFramework.getOrDefault(fwId, 0L) + 1L);
                }
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
            report.setCoverage(summary.getCoverage());
            report.setUnknownCount(summary.getUnknown());
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
            if (plan != null && !plans.contains(plan)) {
                plans.add(plan);
            }

            entries.add(new FindingReportEntry(f, evidence, plan));
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
            sb.append("| **UNKNOWN Controls (Pending Review)** | ").append(s.getUnknown()).append(" |\n");
            sb.append("| **Not Applicable Controls** | ").append(s.getNotApplicable()).append(" |\n");
            sb.append("| **Evaluated Controls** | ").append(s.getEvaluated()).append(" |\n");
            sb.append("| **Control Coverage** | ").append(String.format("%.1f%%", s.getCoverage() * 100.0)).append(" |\n");
            sb.append("| **Compliance Score** | ").append(r.getComplianceScore() != null ? String.format("%.1f%%", r.getComplianceScore()) : "N/A").append(" |\n\n");
        } else {
            sb.append("| Summary | None |\n\n");
        }

        sb.append("> [!IMPORTANT]\n");
        sb.append("> **Policy on UNKNOWN Evaluated Controls:** Controls with UNKNOWN evaluation status (count: ")
                .append(r.getUnknownCount())
                .append(") are strictly isolated and **NEVER** treated as PASS. An UNKNOWN evaluation reflects unmapped syntax or unreviewed platform state requiring human review or approved mapping before credit is awarded.\n\n");

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
                        sb.append("  - Line ").append(ev.getSource() != null ? ev.getSource().getLineNumber() : "?")
                                .append(": `").append(ev.getSource() != null ? ev.getSource().getRawText() : "N/A").append("`")
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
                sb.append("- `").append(mapping.getRawSyntax()).append("` (Status: `").append(mapping.getStatus()).append("`)\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
