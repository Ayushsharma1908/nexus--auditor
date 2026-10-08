package com.nexuscomply.framework.service;

import com.nexuscomply.framework.model.ComplianceRule;
import com.nexuscomply.framework.model.Control;
import com.nexuscomply.framework.model.Framework;
import com.nexuscomply.framework.model.FrameworkMapping;

import java.util.List;

public interface FrameworkService {
    List<Framework> getAllFrameworks();
    Framework getFrameworkById(String id);
    List<Control> getControlsByFramework(String frameworkId);
    Control getControlById(String id);
    List<ComplianceRule> getRulesByControl(String controlId);
    ComplianceRule getRuleById(String id);
    List<FrameworkMapping> getAllMappings();
    FrameworkMapping getMappingById(String id);
}
