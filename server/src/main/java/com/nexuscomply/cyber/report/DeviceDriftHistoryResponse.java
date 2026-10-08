package com.nexuscomply.cyber.report;

import java.util.ArrayList;
import java.util.List;

/**
 * Historical drift sequence for a single device with directional impact classifications.
 */
public class DeviceDriftHistoryResponse {

    private String deviceId;
    private int totalEvents;
    private List<DriftEventSummary> events = new ArrayList<>();

    public DeviceDriftHistoryResponse() {}

    public DeviceDriftHistoryResponse(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public List<DriftEventSummary> getEvents() {
        return events;
    }

    public void setEvents(List<DriftEventSummary> events) {
        this.events = events != null ? events : new ArrayList<>();
    }
}
