package com.nexuscomply.cyber.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fleet-wide compliance summary aggregated across all devices.
 * Computed on read; fleet totals equal the exact sum of individual device totals.
 */
public class FleetSummaryResponse {

    private int totalDevices;
    private Map<String, Long> devicesByVendor = new LinkedHashMap<>();
    private Double fleetComplianceScore;
    private int totalAudits;
    private int totalOpenFindings;
    private Map<String, Long> findingsBySeverity = new LinkedHashMap<>();
    private Map<String, Long> findingsByFramework = new LinkedHashMap<>();
    private List<DevicePostureResponse> devicePostures = new ArrayList<>();

    public FleetSummaryResponse() {}

    public int getTotalDevices() {
        return totalDevices;
    }

    public void setTotalDevices(int totalDevices) {
        this.totalDevices = totalDevices;
    }

    public Map<String, Long> getDevicesByVendor() {
        return devicesByVendor;
    }

    public void setDevicesByVendor(Map<String, Long> devicesByVendor) {
        this.devicesByVendor = devicesByVendor != null ? devicesByVendor : new LinkedHashMap<>();
    }

    public Double getFleetComplianceScore() {
        return fleetComplianceScore;
    }

    public void setFleetComplianceScore(Double fleetComplianceScore) {
        this.fleetComplianceScore = fleetComplianceScore;
    }

    public int getTotalAudits() {
        return totalAudits;
    }

    public void setTotalAudits(int totalAudits) {
        this.totalAudits = totalAudits;
    }

    public int getTotalOpenFindings() {
        return totalOpenFindings;
    }

    public void setTotalOpenFindings(int totalOpenFindings) {
        this.totalOpenFindings = totalOpenFindings;
    }

    public Map<String, Long> getFindingsBySeverity() {
        return findingsBySeverity;
    }

    public void setFindingsBySeverity(Map<String, Long> findingsBySeverity) {
        this.findingsBySeverity = findingsBySeverity != null ? findingsBySeverity : new LinkedHashMap<>();
    }

    public Map<String, Long> getFindingsByFramework() {
        return findingsByFramework;
    }

    public void setFindingsByFramework(Map<String, Long> findingsByFramework) {
        this.findingsByFramework = findingsByFramework != null ? findingsByFramework : new LinkedHashMap<>();
    }

    public List<DevicePostureResponse> getDevicePostures() {
        return devicePostures;
    }

    public void setDevicePostures(List<DevicePostureResponse> devicePostures) {
        this.devicePostures = devicePostures != null ? devicePostures : new ArrayList<>();
    }
}
