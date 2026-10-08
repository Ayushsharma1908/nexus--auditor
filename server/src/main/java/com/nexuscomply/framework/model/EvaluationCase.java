package com.nexuscomply.framework.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "evaluation_cases")
public class EvaluationCase {
    @Id
    @JsonProperty("_id")
    @JsonAlias({"id", "_id"})
    private String id;

    private String vendor;
    private String platform;

    @JsonProperty("caseClass")
    @JsonAlias({"caseClass", "case_class"})
    private String caseClass;

    private String summary;

    @JsonProperty("expectedResult")
    @JsonAlias({"expectedResult", "expected_result"})
    private String expectedResult;

    private String status = "VALIDATED";

    @JsonProperty("configurationFixtureRef")
    @JsonAlias({"configurationFixtureRef", "configuration_fixture_ref", "fixtureRef"})
    private String configurationFixtureRef;

    @JsonProperty("rawConfigurationFixture")
    @JsonAlias({"rawConfigurationFixture", "raw_configuration_fixture", "rawConfig", "rawContent"})
    private String rawConfigurationFixture;

    @JsonProperty("expectedVendorDetection")
    @JsonAlias({"expectedVendorDetection", "expected_vendor_detection"})
    private String expectedVendorDetection;

    @JsonProperty("expectedPlatformDetection")
    @JsonAlias({"expectedPlatformDetection", "expected_platform_detection"})
    private String expectedPlatformDetection;

    @JsonProperty("expectedParsedFacts")
    @JsonAlias({"expectedParsedFacts", "expected_parsed_facts"})
    private Map<String, Object> expectedParsedFacts = new HashMap<>();

    @JsonProperty("expectedCanonicalFacts")
    @JsonAlias({"expectedCanonicalFacts", "expected_canonical_facts"})
    private Map<String, Object> expectedCanonicalFacts = new HashMap<>();

    @JsonProperty("expectedAffectedRuleIds")
    @JsonAlias({"expectedAffectedRuleIds", "expected_affected_rule_ids"})
    private List<String> expectedAffectedRuleIds = new ArrayList<>();

    public EvaluationCase() {}

    public EvaluationCase(String id, String vendor, String platform, String caseClass, String summary, String expectedResult, String status) {
        this.id = id;
        this.vendor = vendor;
        this.platform = platform;
        this.caseClass = caseClass;
        this.summary = summary;
        this.expectedResult = expectedResult;
        this.status = status;
        this.expectedVendorDetection = vendor;
        this.expectedPlatformDetection = platform;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getCaseClass() {
        return caseClass;
    }

    public void setCaseClass(String caseClass) {
        this.caseClass = caseClass;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfigurationFixtureRef() {
        return configurationFixtureRef;
    }

    public void setConfigurationFixtureRef(String configurationFixtureRef) {
        this.configurationFixtureRef = configurationFixtureRef;
    }

    public String getRawConfigurationFixture() {
        return rawConfigurationFixture;
    }

    public void setRawConfigurationFixture(String rawConfigurationFixture) {
        this.rawConfigurationFixture = rawConfigurationFixture;
    }

    public String getExpectedVendorDetection() {
        return expectedVendorDetection != null ? expectedVendorDetection : vendor;
    }

    public void setExpectedVendorDetection(String expectedVendorDetection) {
        this.expectedVendorDetection = expectedVendorDetection;
    }

    public String getExpectedPlatformDetection() {
        return expectedPlatformDetection != null ? expectedPlatformDetection : platform;
    }

    public void setExpectedPlatformDetection(String expectedPlatformDetection) {
        this.expectedPlatformDetection = expectedPlatformDetection;
    }

    public Map<String, Object> getExpectedParsedFacts() {
        return expectedParsedFacts;
    }

    public void setExpectedParsedFacts(Map<String, Object> expectedParsedFacts) {
        this.expectedParsedFacts = expectedParsedFacts != null ? expectedParsedFacts : new HashMap<>();
    }

    public Map<String, Object> getExpectedCanonicalFacts() {
        return expectedCanonicalFacts;
    }

    public void setExpectedCanonicalFacts(Map<String, Object> expectedCanonicalFacts) {
        this.expectedCanonicalFacts = expectedCanonicalFacts != null ? expectedCanonicalFacts : new HashMap<>();
    }

    public List<String> getExpectedAffectedRuleIds() {
        return expectedAffectedRuleIds;
    }

    public void setExpectedAffectedRuleIds(List<String> expectedAffectedRuleIds) {
        this.expectedAffectedRuleIds = expectedAffectedRuleIds != null ? expectedAffectedRuleIds : new ArrayList<>();
    }
}
