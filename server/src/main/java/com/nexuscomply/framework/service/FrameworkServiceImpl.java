package com.nexuscomply.framework.service;

import com.nexuscomply.common.exception.ResourceNotFoundException;
import com.nexuscomply.framework.model.ComplianceRule;
import com.nexuscomply.framework.model.Control;
import com.nexuscomply.framework.model.Framework;
import com.nexuscomply.framework.model.FrameworkMapping;
import com.nexuscomply.framework.repository.ComplianceRuleRepository;
import com.nexuscomply.framework.repository.ControlRepository;
import com.nexuscomply.framework.repository.FrameworkMappingRepository;
import com.nexuscomply.framework.repository.FrameworkRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FrameworkServiceImpl implements FrameworkService {

    private final FrameworkRepository frameworkRepo;
    private final ControlRepository controlRepo;
    private final ComplianceRuleRepository ruleRepo;
    private final FrameworkMappingRepository mappingRepo;

    public FrameworkServiceImpl(FrameworkRepository frameworkRepo,
                                ControlRepository controlRepo,
                                ComplianceRuleRepository ruleRepo,
                                FrameworkMappingRepository mappingRepo) {
        this.frameworkRepo = frameworkRepo;
        this.controlRepo = controlRepo;
        this.ruleRepo = ruleRepo;
        this.mappingRepo = mappingRepo;
    }

    @Override
    public List<Framework> getAllFrameworks() {
        return frameworkRepo.findAll();
    }

    @Override
    public Framework getFrameworkById(String id) {
        return frameworkRepo.findById(id)
                .or(() -> frameworkRepo.findByCode(id))
                .orElseThrow(() -> new ResourceNotFoundException("Framework", id));
    }

    @Override
    public List<Control> getControlsByFramework(String frameworkId) {
        getFrameworkById(frameworkId); // validate framework exists
        return controlRepo.findByFrameworkId(frameworkId);
    }

    @Override
    public Control getControlById(String id) {
        return controlRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Control", id));
    }

    @Override
    public List<ComplianceRule> getRulesByControl(String controlId) {
        getControlById(controlId); // validate control exists
        return ruleRepo.findByControlId(controlId);
    }

    @Override
    public ComplianceRule getRuleById(String id) {
        return ruleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", id));
    }

    @Override
    public List<FrameworkMapping> getAllMappings() {
        return mappingRepo.findAll();
    }

    @Override
    public FrameworkMapping getMappingById(String id) {
        return mappingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Framework mapping", id));
    }
}
