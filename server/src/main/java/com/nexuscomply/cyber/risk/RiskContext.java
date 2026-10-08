package com.nexuscomply.cyber.risk;

/**
 * Contextual metadata supplied for risk assessment calculation.
 *
 * <p>Asset criticality and network exposure originate from Package A's device inventory
 * (outside this workspace). When not supplied, documented default placeholders are used:
 * assetCriticality = "MEDIUM", networkExposure = "INTERNAL".
 *
 * <p>Every defaulted input is explicitly flagged with source "DEFAULT_PLACEHOLDER" vs "PROVIDED".
 *
 * <p>Vendor detection confidence is the real confidence value produced during the DETECTING stage.
 */
public class RiskContext {

    public static final String DEFAULT_ASSET_CRITICALITY = "MEDIUM";
    public static final String DEFAULT_NETWORK_EXPOSURE = "INTERNAL";

    public static final String SOURCE_DEFAULT_PLACEHOLDER = "DEFAULT_PLACEHOLDER";
    public static final String SOURCE_PROVIDED = "PROVIDED";

    private String assetCriticality;
    private String networkExposure;
    private double detectionConfidence;

    private String assetCriticalitySource;
    private String networkExposureSource;

    public RiskContext() {
        this(1.0);
    }

    public RiskContext(double detectionConfidence) {
        this.assetCriticality = DEFAULT_ASSET_CRITICALITY;
        this.assetCriticalitySource = SOURCE_DEFAULT_PLACEHOLDER;
        this.networkExposure = DEFAULT_NETWORK_EXPOSURE;
        this.networkExposureSource = SOURCE_DEFAULT_PLACEHOLDER;
        this.detectionConfidence = detectionConfidence;
    }

    public RiskContext(String assetCriticality, String networkExposure, double detectionConfidence) {
        if (assetCriticality != null && !assetCriticality.isBlank()) {
            this.assetCriticality = assetCriticality.toUpperCase();
            this.assetCriticalitySource = SOURCE_PROVIDED;
        } else {
            this.assetCriticality = DEFAULT_ASSET_CRITICALITY;
            this.assetCriticalitySource = SOURCE_DEFAULT_PLACEHOLDER;
        }

        if (networkExposure != null && !networkExposure.isBlank()) {
            this.networkExposure = networkExposure.toUpperCase();
            this.networkExposureSource = SOURCE_PROVIDED;
        } else {
            this.networkExposure = DEFAULT_NETWORK_EXPOSURE;
            this.networkExposureSource = SOURCE_DEFAULT_PLACEHOLDER;
        }

        this.detectionConfidence = detectionConfidence;
    }

    public String getAssetCriticality() {
        return assetCriticality;
    }

    public void setAssetCriticality(String assetCriticality) {
        if (assetCriticality != null && !assetCriticality.isBlank()) {
            this.assetCriticality = assetCriticality.toUpperCase();
            this.assetCriticalitySource = SOURCE_PROVIDED;
        } else {
            this.assetCriticality = DEFAULT_ASSET_CRITICALITY;
            this.assetCriticalitySource = SOURCE_DEFAULT_PLACEHOLDER;
        }
    }

    public String getNetworkExposure() {
        return networkExposure;
    }

    public void setNetworkExposure(String networkExposure) {
        if (networkExposure != null && !networkExposure.isBlank()) {
            this.networkExposure = networkExposure.toUpperCase();
            this.networkExposureSource = SOURCE_PROVIDED;
        } else {
            this.networkExposure = DEFAULT_NETWORK_EXPOSURE;
            this.networkExposureSource = SOURCE_DEFAULT_PLACEHOLDER;
        }
    }

    public double getDetectionConfidence() {
        return detectionConfidence;
    }

    public void setDetectionConfidence(double detectionConfidence) {
        this.detectionConfidence = detectionConfidence;
    }

    public String getAssetCriticalitySource() {
        return assetCriticalitySource;
    }

    public String getNetworkExposureSource() {
        return networkExposureSource;
    }
}
