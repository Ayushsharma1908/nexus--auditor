package com.nexuscomply.cyber.evidence.persistence;

import com.nexuscomply.cyber.evidence.EvidenceCanonical;
import com.nexuscomply.cyber.evidence.EvidenceSource;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representation of technical evidence supporting findings.
 * Collection: "evidence" per schema1.md section 11.
 *
 * <p>Indexed fields per schema1.md section 11:
 * findingId, auditId, configurationId, versionId.
 */
@Document(collection = "evidence")
public class EvidenceDocument {

    @Id
    private String id;

    @Indexed
    private String findingId;

    @Indexed
    private String auditId;

    @Indexed
    private String configurationId;

    @Indexed
    private String versionId;

    private EvidenceSource source;

    private EvidenceCanonical canonical;

    private String reason;

    private Instant createdAt;

    public EvidenceDocument() {}

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
