package com.nexuscomply.cyber.evidence;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Domain model representing technical evidence supporting a compliance finding.
 * Field names match schema1.md section 11 exactly:
 *
 * <pre>
 * {
 *   "_id": "evidence-uuid",
 *   "findingId": "finding-uuid",
 *   "auditId": "audit-uuid",
 *   "configurationId": "configuration-uuid",
 *   "versionId": "version-uuid",
 *   "source": {
 *     "lineNumber": 32,
 *     "rawText": "transport input telnet ssh",
 *     "sourceType": "CONFIGURATION"
 *   },
 *   "canonical": {
 *     "field": "management.telnetEnabled",
 *     "value": true
 *   },
 *   "reason": "Configuration explicitly permits Telnet.",
 *   "createdAt": "2026-09-24T08:02:00Z"
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Evidence {

    private String id;
    private String findingId;
    private String auditId;
    private String configurationId;
    private String versionId;
    private EvidenceSource source;
    private EvidenceCanonical canonical;
    private String reason;
    private Instant createdAt;

    public Evidence() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFindingId() {
        return findingId;
    }

    public void setFindingId(String findingId) {
        this.findingId = findingId;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public EvidenceSource getSource() {
        return source;
    }

    public void setSource(EvidenceSource source) {
        this.source = source;
    }

    public EvidenceCanonical getCanonical() {
        return canonical;
    }

    public void setCanonical(EvidenceCanonical canonical) {
        this.canonical = canonical;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
