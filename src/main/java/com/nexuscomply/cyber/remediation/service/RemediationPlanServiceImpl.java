package com.nexuscomply.cyber.remediation.service;

import com.nexuscomply.cyber.finding.Finding;
import com.nexuscomply.cyber.finding.persistence.FindingDocument;
import com.nexuscomply.cyber.finding.persistence.FindingRepository;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationDocument;
import com.nexuscomply.cyber.normalization.NormalizedConfigurationRepository;
import com.nexuscomply.cyber.remediation.model.CommandType;
import com.nexuscomply.cyber.remediation.model.PlanStep;
import com.nexuscomply.cyber.remediation.model.PlanValidation;
import com.nexuscomply.cyber.remediation.model.PlanVerification;
import com.nexuscomply.cyber.remediation.model.RemediationPlan;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanDocument;
import com.nexuscomply.cyber.remediation.persistence.RemediationPlanRepository;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateDocument;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RemediationPlanServiceImpl implements RemediationPlanService {

    private final RemediationPlanRepository planRepository;
    private final RemediationTemplateRepository templateRepository;
    private final NormalizedConfigurationRepository normalizedConfigRepository;
    private final FindingRepository findingRepository;

    public RemediationPlanServiceImpl(
            RemediationPlanRepository planRepository,
            RemediationTemplateRepository templateRepository,
            NormalizedConfigurationRepository normalizedConfigRepository,
            FindingRepository findingRepository) {
        this.planRepository = planRepository;
        this.templateRepository = templateRepository;
        this.normalizedConfigRepository = normalizedConfigRepository;
        this.findingRepository = findingRepository;
    }

    @Override
    public List<RemediationTemplateDocument> findTemplatesForFinding(Finding finding) {
        if (finding == null) {
            throw new IllegalArgumentException("Finding must not be null");
        }
        if (finding.getCanonicalField() == null || finding.getCanonicalField().isBlank()) {
            throw new IllegalArgumentException("Finding must have a canonicalField defined");
        }

        NormalizedConfigurationDocument normDoc = resolveNormalizedConfig(finding);
        String vendor = normDoc.getVendor();
        String platform = normDoc.getPlatform();
        String canonicalField = finding.getCanonicalField();

        Optional<RemediationTemplateDocument> templateOpt = templateRepository.findByVendorAndPlatformAndCanonicalField(
                vendor, platform, canonicalField
        );

        // Fallback for Cisco IOS vs IOS-XE platform variance
        if (templateOpt.isEmpty() && "Cisco".equalsIgnoreCase(vendor)) {
            if ("IOS".equalsIgnoreCase(platform)) {
                templateOpt = templateRepository.findByVendorAndPlatformAndCanonicalField("Cisco", "IOS-XE", canonicalField);
            } else if ("IOS-XE".equalsIgnoreCase(platform)) {
                templateOpt = templateRepository.findByVendorAndPlatformAndCanonicalField("Cisco", "IOS", canonicalField);
            }
        }

        // Fallback for Palo Alto Networks naming variance
        if (templateOpt.isEmpty() && (vendor != null && vendor.toLowerCase().contains("palo alto"))) {
            templateOpt = templateRepository.findByVendorAndPlatformAndCanonicalField("Palo Alto", "PAN-OS", canonicalField);
        }

        return templateOpt.map(List::of).orElseGet(List::of);
    }

    @Override
    public RemediationPlan createPlanForFinding(Finding finding) {
        if (finding == null) {
            throw new IllegalArgumentException("Finding must not be null");
        }

        List<RemediationTemplateDocument> matchedTemplates = findTemplatesForFinding(finding);
        if (matchedTemplates.isEmpty()) {
            throw new IllegalStateException("No matching remediation template found for finding " + finding.getId()
                    + " with canonical field [" + finding.getCanonicalField() + "]");
        }

        RemediationTemplateDocument template = matchedTemplates.get(0);

        RemediationPlanDocument planDoc = new RemediationPlanDocument();
        planDoc.setId(UUID.randomUUID().toString());
        planDoc.setFindingId(finding.getId());
        planDoc.setDeviceId(finding.getDeviceId());
        planDoc.setTemplateId(template.getId());

        // ABSOLUTE RULE 6: Creating a plan must NEVER auto-approve it. Starts strictly as "PLANNED".
        planDoc.setStatus("PLANNED");

        List<PlanStep> steps = new ArrayList<>();
        int order = 1;
        if (template.getCommandType() == CommandType.PLATFORM_GAP) {
            steps.add(new PlanStep(order++, "PLATFORM_GAP_NOTICE", "NO_COMMAND", template.getGapExplanation()));
        } else {
            for (String cmd : template.getCommands()) {
                steps.add(new PlanStep(order++, "EXECUTE_COMMAND", cmd, template.getTitle()));
            }
        }
        planDoc.setSteps(steps);

        PlanValidation validation = new PlanValidation();
        validation.setStatus("PENDING");
        validation.setMessages(new ArrayList<>());
        planDoc.setValidation(validation);

        PlanVerification verification = new PlanVerification();
        verification.setStatus("PENDING");
        verification.setMessages(new ArrayList<>());
        planDoc.setVerification(verification);

        planDoc.setCreatedBy("SYSTEM_REMEDIATION_ENGINE");
        Instant now = Instant.now();
        planDoc.setCreatedAt(now);
        planDoc.setUpdatedAt(now);

        RemediationPlanDocument saved = planRepository.save(planDoc);
        return toDomain(saved);
    }

    @Override
    public RemediationPlan createPlanForFindingId(String findingId) {
        if (findingId == null || findingId.isBlank()) {
            throw new IllegalArgumentException("findingId must not be null or blank");
        }
        FindingDocument findingDoc = findingRepository.findById(findingId)
                .orElseThrow(() -> new IllegalArgumentException("Finding not found for id: " + findingId));
        return createPlanForFinding(toFindingDomain(findingDoc));
    }

    @Override
    public Optional<RemediationPlan> getPlanById(String planId) {
        return planRepository.findById(planId).map(this::toDomain);
    }

    @Override
    public List<RemediationPlan> getPlansByFindingId(String findingId) {
        return planRepository.findByFindingId(findingId).stream()
                .map(this::toDomain)
                .toList();
    }

    private NormalizedConfigurationDocument resolveNormalizedConfig(Finding finding) {
        String configurationId = finding.getConfigurationId();
        if (configurationId != null && !configurationId.isBlank()) {
            Optional<NormalizedConfigurationDocument> docOpt = normalizedConfigRepository.findByConfigurationId(configurationId);
            if (docOpt.isPresent()) {
                return docOpt.get();
            }
        }

        String deviceId = finding.getDeviceId();
        if (deviceId != null && !deviceId.isBlank()) {
            List<NormalizedConfigurationDocument> docs = normalizedConfigRepository.findByDeviceId(deviceId);
            if (docs != null && !docs.isEmpty()) {
                return docs.get(docs.size() - 1);
            }
        }

        throw new IllegalStateException("Unable to trace Finding [id=" + finding.getId() + "] to NormalizedConfigurationDocument via configurationId ["
                + configurationId + "] or deviceId [" + deviceId + "]");
    }

    private RemediationPlan toDomain(RemediationPlanDocument doc) {
        RemediationPlan plan = new RemediationPlan();
        plan.setId(doc.getId());
        plan.setFindingId(doc.getFindingId());
        plan.setDeviceId(doc.getDeviceId());
        plan.setTemplateId(doc.getTemplateId());
        plan.setStatus(doc.getStatus());
        plan.setSteps(doc.getSteps());
        plan.setValidation(doc.getValidation());
        plan.setVerification(doc.getVerification());
        plan.setCreatedBy(doc.getCreatedBy());
        plan.setCreatedAt(doc.getCreatedAt());
        plan.setUpdatedAt(doc.getUpdatedAt());
        return plan;
    }

    private Finding toFindingDomain(FindingDocument doc) {
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
