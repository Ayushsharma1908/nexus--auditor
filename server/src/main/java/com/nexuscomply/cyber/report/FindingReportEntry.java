package com.nexuscomply.cyber.report;

import com.nexuscomply.cyber.evidence.persistence.EvidenceDocument;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Audit report entry associating a finding with its evidence and matching remediation plan.
 */
public class FindingReportEntry {

    private FindingDocument finding;
    private List<EvidenceDocument> evidence = new ArrayList<>();
    private RemediationPlanDocument remediationPlan;

    public FindingReportEntry() {}

    public FindingReportEntry(FindingDocument finding, List<EvidenceDocument> evidence, RemediationPlanDocument remediationPlan) {
        this.finding = finding;
        this.evidence = evidence != null ? evidence : new ArrayList<>();
        this.remediationPlan = remediationPlan;
    }

    public FindingDocument getFinding() {
        return finding;
    }

    public void setFinding(FindingDocument finding) {
        this.finding = finding;
    }

    public List<EvidenceDocument> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<EvidenceDocument> evidence) {
        this.evidence = evidence != null ? evidence : new ArrayList<>();
    }

    public RemediationPlanDocument getRemediationPlan() {
        return remediationPlan;
    }

    public void setRemediationPlan(RemediationPlanDocument remediationPlan) {
        this.remediationPlan = remediationPlan;
    }
}
