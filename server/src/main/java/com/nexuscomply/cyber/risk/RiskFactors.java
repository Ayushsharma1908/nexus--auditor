package com.nexuscomply.cyber.risk;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Detailed representation of the 5 risk factors from cyberlayer.pdf section 18 and schema1.md section 12.
 *
 * <p>Field names:
 * <ul>
 *   <li>{@code severity}: finding severity contribution/scale.</li>
 *   <li>{@code assetCriticality}: asset criticality contribution/scale.</li>
 *   <li>{@code exposure}: network exposure contribution/scale.</li>
 *   <li>{@code exploitability}: matches schema1.md section 12 literally. Conceptually in this implementation,
 *       this holds the Control Importance / Weight factor from cyberlayer.pdf section 18 (derived via proxy
 *       from ComplianceRule.severity), as vulnerability exploitability metrics (CVSS/EPSS) do not exist in
 *       raw network configuration files.</li>
 *   <li>{@code confidence}: vendor detection confidence factor.</li>
 * </ul>
 *
 * <p>Preserves scaled factor values (1-10 scale), raw inputs, and explicit point contributions to the final score.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiskFactors {

    // Scaled factor values (1-10 scale per schema1.md example style)
    private Double severity;
    private Double assetCriticality;
    private Double exposure;
    private Double exploitability;
    private Double confidence;

    // Raw input factor values preserved explicitly
    private Map<String, Object> rawInputs = new LinkedHashMap<>();

    // Point contribution of each factor to the final 0-100 score
    private Map<String, Double> contributions = new LinkedHashMap<>();

    public RiskFactors() {}

    public RiskFactors(Double severity, Double assetCriticality, Double exposure, Double exploitability, Double confidence) {
        this.severity = severity;
        this.assetCriticality = assetCriticality;
        this.exposure = exposure;
        this.exploitability = exploitability;
        this.confidence = confidence;
    }

    public Double getSeverity() {
        return severity;
    }

    public void setSeverity(Double severity) {
        this.severity = severity;
    }

    public Double getAssetCriticality() {
        return assetCriticality;
    }

    public void setAssetCriticality(Double assetCriticality) {
        this.assetCriticality = assetCriticality;
    }

    public Double getExposure() {
        return exposure;
    }

    public void setExposure(Double exposure) {
        this.exposure = exposure;
    }

    public Double getExploitability() {
        return exploitability;
    }

    public void setExploitability(Double exploitability) {
        this.exploitability = exploitability;
    }

    // Alias for callers referring to the conceptual factor (control weight)
    public Double getControlWeight() {
        return exploitability;
    }

    public void setControlWeight(Double controlWeight) {
        this.exploitability = controlWeight;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Object> getRawInputs() {
        return rawInputs;
    }

    public void setRawInputs(Map<String, Object> rawInputs) {
        this.rawInputs = rawInputs;
    }

    public Map<String, Double> getContributions() {
        return contributions;
    }

    public void setContributions(Map<String, Double> contributions) {
        this.contributions = contributions;
    }
}
