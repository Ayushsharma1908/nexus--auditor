package com.nexuscomply.framework.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "dataset_releases")
public class DatasetRelease {

    @Id
    @JsonProperty("id")
    @JsonAlias({"id", "_id"})
    private String id;

    @JsonProperty("datasetVersion")
    @JsonAlias({"dataset_version", "datasetVersion"})
    private String datasetVersion;

    @JsonProperty("releaseDate")
    @JsonAlias({"release_date", "releaseDate"})
    private String releaseDate;

    @JsonProperty("sourceManifestHash")
    @JsonAlias({"source_manifest_hash", "sourceManifestHash"})
    private String sourceManifestHash;

    @JsonProperty("importedAt")
    @JsonAlias({"imported_at", "importedAt"})
    private String importedAt;

    private int sourceCount;
    private int frameworkCount;
    private int controlCount;
    private int ruleCount;
    private int vendorKnowledgeCount;
    private int frameworkMappingCount;
    private int evaluationCaseCount;

    private String status = "VALIDATED";
    private String notes;

    public DatasetRelease() {}

    public DatasetRelease(String id, String datasetVersion, String releaseDate, String sourceManifestHash,
                          int sourceCount, int frameworkCount, int controlCount, int ruleCount,
                          int vendorKnowledgeCount, int frameworkMappingCount, int evaluationCaseCount,
                          String status, String notes) {
        this.id = id;
        this.datasetVersion = datasetVersion;
        this.releaseDate = releaseDate;
        this.sourceManifestHash = sourceManifestHash;
        this.importedAt = Instant.now().toString();
        this.sourceCount = sourceCount;
        this.frameworkCount = frameworkCount;
        this.controlCount = controlCount;
        this.ruleCount = ruleCount;
        this.vendorKnowledgeCount = vendorKnowledgeCount;
        this.frameworkMappingCount = frameworkMappingCount;
        this.evaluationCaseCount = evaluationCaseCount;
        this.status = status;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(String datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getSourceManifestHash() {
        return sourceManifestHash;
    }

    public void setSourceManifestHash(String sourceManifestHash) {
        this.sourceManifestHash = sourceManifestHash;
    }

    public String getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(String importedAt) {
        this.importedAt = importedAt;
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }

    public int getFrameworkCount() {
        return frameworkCount;
    }

    public void setFrameworkCount(int frameworkCount) {
        this.frameworkCount = frameworkCount;
    }

    public int getControlCount() {
        return controlCount;
    }

    public void setControlCount(int controlCount) {
        this.controlCount = controlCount;
    }

    public int getRuleCount() {
        return ruleCount;
    }

    public void setRuleCount(int ruleCount) {
        this.ruleCount = ruleCount;
    }

    public int getVendorKnowledgeCount() {
        return vendorKnowledgeCount;
    }

    public void setVendorKnowledgeCount(int vendorKnowledgeCount) {
        this.vendorKnowledgeCount = vendorKnowledgeCount;
    }

    public int getFrameworkMappingCount() {
        return frameworkMappingCount;
    }

    public void setFrameworkMappingCount(int frameworkMappingCount) {
        this.frameworkMappingCount = frameworkMappingCount;
    }

    public int getEvaluationCaseCount() {
        return evaluationCaseCount;
    }

    public void setEvaluationCaseCount(int evaluationCaseCount) {
        this.evaluationCaseCount = evaluationCaseCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
