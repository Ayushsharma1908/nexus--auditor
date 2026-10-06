package com.nexuscomply.cyber.report;

/**
 * Service interface for read-only aggregation of compliance posture, fleet summaries,
 * drift histories, and audit reports.
 *
 * <p>Strictly read-only; performs zero writes or mutations to operational collections.
 */
public interface DashboardReportingService {

    /**
     * Aggregates compliance and security posture for a single device based on its latest audit.
     *
     * @param deviceId ID of device
     * @return Per-device posture response
     */
    DevicePostureResponse getDevicePosture(String deviceId);

    /**
     * Aggregates fleet-wide compliance posture across all audited devices.
     * Fleet totals strictly equal the sum of device totals.
     *
     * @return Fleet summary response
     */
    FleetSummaryResponse getFleetSummary();

    /**
     * Retrieves historical drift events for a device in reverse chronological order.
     *
     * @param deviceId ID of device
     * @return Device drift history response
     */
    DeviceDriftHistoryResponse getDeviceDriftHistory(String deviceId);

    /**
     * Generates a comprehensive audit report for a specific audit execution.
     * Outputs findings with evidence, remediation plans, pending unknown syntax,
     * coverage, and a Markdown rendering.
     *
     * @param auditId ID of audit
     * @return Audit report response
     */
    AuditReportResponse getAuditReport(String auditId);
}
