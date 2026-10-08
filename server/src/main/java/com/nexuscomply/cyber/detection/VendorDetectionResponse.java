package com.nexuscomply.cyber.detection;

public class VendorDetectionResponse {
    private String vendor;
    private String platform;
    private double confidence;
    private String detectionMethod;
    private VendorDetectionStatus status;

    public VendorDetectionResponse() {}

    public VendorDetectionResponse(String vendor, String platform, double confidence, String detectionMethod, VendorDetectionStatus status) {
        this.vendor = vendor;
        this.platform = platform;
        this.confidence = confidence;
        this.detectionMethod = detectionMethod;
        this.status = status;
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

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(String detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public VendorDetectionStatus getStatus() {
        return status;
    }

    public void setStatus(VendorDetectionStatus status) {
        this.status = status;
    }
}
