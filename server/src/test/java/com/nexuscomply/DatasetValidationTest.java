package com.nexuscomply;

import com.nexuscomply.framework.model.ComplianceRule;
import com.nexuscomply.framework.model.Control;
import com.nexuscomply.framework.model.DatasetRelease;
import com.nexuscomply.framework.model.EvaluationCase;
import com.nexuscomply.framework.model.FrameworkMapping;
import com.nexuscomply.framework.model.VendorKnowledge;
import com.nexuscomply.framework.repository.ComplianceRuleRepository;
import com.nexuscomply.framework.repository.ControlRepository;
import com.nexuscomply.framework.repository.DatasetReleaseRepository;
import com.nexuscomply.framework.repository.EvaluationCaseRepository;
import com.nexuscomply.framework.repository.FrameworkMappingRepository;
import com.nexuscomply.framework.repository.VendorKnowledgeRepository;
import com.nexuscomply.framework.service.DatasetImportService;
import com.nexuscomply.framework.service.DatasetValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = MongoTestInitializer.class)
class DatasetValidationTest {

    @Autowired
    private DatasetImportService importService;

    @Autowired
    private ControlRepository controlRepo;

    @Autowired
    private ComplianceRuleRepository ruleRepo;

    @Autowired
    private FrameworkMappingRepository mappingRepo;

    @Autowired
    private VendorKnowledgeRepository vendorKnowledgeRepo;

    @Autowired
    private EvaluationCaseRepository evalRepo;

    @Autowired
    private DatasetReleaseRepository releaseRepo;

    @BeforeEach
    void setUp() {
        importService.validateAndImport();
    }

    @Test
    @DisplayName("Section 12: Pipeline is repeatable and idempotent without errors")
    void testRepeatableIdempotentImport() {
        DatasetValidationResult result = importService.validateAndImport();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getInsertedCount()).isGreaterThan(50);
    }

    @Test
    @DisplayName("Section 13.1: No duplicate control identifiers within same framework/version")
    void testNoDuplicateControlIdentifiers() {
        List<Control> controls = controlRepo.findAll();
        assertThat(controls).isNotEmpty();

        Set<String> seen = new HashSet<>();
        for (Control c : controls) {
            String key = c.getFrameworkId() + ":" + (c.getVersion() != null ? c.getVersion() : "") + ":" + c.getControlId();
            assertThat(seen.add(key))
                    .withFailMessage("Duplicate control key found: " + key)
                    .isTrue();
            assertThat(c.getTitle()).isNotBlank();
            assertThat(c.getFamily()).isNotBlank();
            assertThat(c.getSourceReference()).isNotBlank();
            assertThat(c.getProvenance()).isNotBlank();
        }
    }

    @Test
    @DisplayName("Section 13.2 & 13.3: Rules have canonicalField, provenance, and operator")
    void testComplianceRuleQuality() {
        List<ComplianceRule> rules = ruleRepo.findAll();
        assertThat(rules).isNotEmpty();

        for (ComplianceRule r : rules) {
            if ("ACTIVE".equalsIgnoreCase(r.getStatus())) {
                assertThat(r.getCanonicalField())
                        .withFailMessage("Active rule missing canonicalField: " + r.getId())
                        .isNotBlank();
            }
            if ("AUTHORITATIVE".equalsIgnoreCase(r.getSourceType())) {
                assertThat(r.getProvenance())
                        .withFailMessage("Authoritative rule missing provenance: " + r.getId())
                        .isNotBlank();
            }
            assertThat(r.getOperator()).isNotBlank();
            assertThat(r.getExpectedValue()).isNotNull();
            assertThat(r.getSeverity()).isNotBlank();
            assertThat(r.getRuleVersion()).isNotBlank();
        }
    }

    @Test
    @DisplayName("Section 13.4: Framework mappings have valid source/target and reviewStatus")
    void testFrameworkMappingsQuality() {
        List<FrameworkMapping> mappings = mappingRepo.findAll();
        assertThat(mappings).isNotEmpty();

        for (FrameworkMapping m : mappings) {
            assertThat(m.getSourceFramework()).isNotBlank();
            assertThat(m.getSourceControl()).isNotBlank();
            assertThat(m.getTargetFramework()).isNotBlank();
            assertThat(m.getTargetControl()).isNotBlank();
            assertThat(m.getSourceReference()).isNotBlank();
            assertThat(m.getReviewStatus()).isEqualTo("VALIDATED");
            assertThat(m.getMappingType()).isIn("DIRECT", "RELATED", "PARTIAL", "SUPPORTING_EVIDENCE");
        }
    }

    @Test
    @DisplayName("Section 13.5: Vendor knowledge contains vendor, platform, and source across 4 vendors")
    void testVendorKnowledgeQuality() {
        List<VendorKnowledge> vk = vendorKnowledgeRepo.findAll();
        assertThat(vk).isNotEmpty();
        assertThat(vk.size()).isGreaterThanOrEqualTo(40);

        Set<String> vendors = new HashSet<>();
        for (VendorKnowledge v : vk) {
            assertThat(v.getVendor()).isNotBlank();
            assertThat(v.getPlatform()).isNotBlank();
            assertThat(v.getSourceReference()).isNotBlank();
            assertThat(v.getCanonicalField()).isNotBlank();
            assertThat(v.getRawSyntaxPattern()).isNotBlank();
            vendors.add(v.getVendor());
        }

        assertThat(vendors).contains("Cisco", "Juniper", "Fortinet", "Palo Alto");
    }

    @Test
    @DisplayName("Section 13.6 & 13.7: Evaluation fixtures have real configuration text and valid facts")
    void testEvaluationCasesQuality() {
        List<EvaluationCase> evals = evalRepo.findAll();
        assertThat(evals).isNotEmpty();
        assertThat(evals.size()).isGreaterThanOrEqualTo(12);

        for (EvaluationCase ec : evals) {
            assertThat(ec.getExpectedResult()).isIn("PASS", "FAIL", "UNKNOWN");
            assertThat(ec.getRawConfigurationFixture())
                    .withFailMessage("Evaluation case missing real configuration text: " + ec.getId())
                    .isNotBlank();
            assertThat(ec.getConfigurationFixtureRef()).isNotBlank();

            if ("PASS".equalsIgnoreCase(ec.getExpectedResult())) {
                assertThat(ec.getCaseClass()).isEqualTo("GOOD");
            } else if ("FAIL".equalsIgnoreCase(ec.getExpectedResult())) {
                assertThat(ec.getCaseClass()).isIn("BAD", "MIXED");
            } else if ("UNKNOWN".equalsIgnoreCase(ec.getExpectedResult())) {
                assertThat(ec.getCaseClass()).isEqualTo("UNKNOWN");
            }
        }
    }

    @Test
    @DisplayName("Section 10: Dataset release record and manifest hash are verified")
    void testDatasetReleaseProvenance() {
        Optional<DatasetRelease> releaseOpt = releaseRepo.findByDatasetVersion("2.1.0-SIH26155");
        assertThat(releaseOpt).isPresent();

        DatasetRelease release = releaseOpt.get();
        assertThat(release.getSourceManifestHash()).isEqualTo("7c0aeddfa101727d515e06e3c4c12d6d2635206cff4fb8127b4c8c78c2001cee");
        assertThat(release.getStatus()).isEqualTo("VALIDATED");
        assertThat(release.getFrameworkCount()).isGreaterThanOrEqualTo(4);
        assertThat(release.getControlCount()).isGreaterThanOrEqualTo(20);
        assertThat(release.getVendorKnowledgeCount()).isGreaterThanOrEqualTo(40);
        assertThat(release.getEvaluationCaseCount()).isGreaterThanOrEqualTo(12);
    }
}
