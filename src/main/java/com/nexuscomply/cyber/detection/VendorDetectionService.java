package com.nexuscomply.cyber.detection;

public interface VendorDetectionService {
    VendorDetectionResponse detectVendor(VendorDetectionRequest request);
    VendorDetectionResponse detectVendor(String rawConfig);
}
