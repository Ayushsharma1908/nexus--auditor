package com.nexuscomply.cyber.remediation.service;

import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.remediation.model.RemediationPlan;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateDocument;

import java.util.List;
import java.util.Optional;

public interface RemediationPlanService {

    /**
     * Resolves matching remediation template(s) for a finding based on the vendor,
     * platform from its associated NormalizedConfigurationDocument, and canonicalField.
     */
    List<RemediationTemplateDocument> findTemplatesForFinding(Finding finding);

    /**
     * Creates a RemediationPlan for a finding in non-approved initial state (status = "PLANNED").
     * Never auto-approves.
     */
    RemediationPlan createPlanForFinding(Finding finding);

    /**
     * Creates a RemediationPlan for a finding looked up by findingId.
     */
    RemediationPlan createPlanForFindingId(String findingId);

    /**
     * Retrieves an existing plan by its ID.
     */
    Optional<RemediationPlan> getPlanById(String planId);

    /**
     * Retrieves all plans associated with a finding ID.
     */
    List<RemediationPlan> getPlansByFindingId(String findingId);
}
