package com.nexuscomply.cyber.detection;

public class VendorDetectionRequest {
    private String rawConfig;
    private String hintVendor;
    private String hintPlatform;

    public VendorDetectionRequest() {}

    public VendorDetectionRequest(String rawConfig) {
        this.rawConfig = rawConfig;
    }

    public VendorDetectionRequest(String rawConfig, String hintVendor, String hintPlatform) {
        this.rawConfig = rawConfig;
        this.hintVendor = hintVendor;
        this.hintPlatform = hintPlatform;
    }

    public String getRawConfig() {
        return rawConfig;
    }

    public void setRawConfig(String rawConfig) {
        this.rawConfig = rawConfig;
    }

    public String getHintVendor() {
        return hintVendor;
    }

    public void setHintVendor(String hintVendor) {
        this.hintVendor = hintVendor;
    }

    public String getHintPlatform() {
        return hintPlatform;
    }

    public void setHintPlatform(String hintPlatform) {
        this.hintPlatform = hintPlatform;
    }
}
