package com.nexuscomply.cyber.remediation.persistence;

import com.nexuscomply.cyber.remediation.model.CommandType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "remediation_templates")
@CompoundIndexes({
        @CompoundIndex(name = "ctrl_vendor_plat_idx", def = "{'controlId': 1, 'vendor': 1, 'platform': 1}"),
        @CompoundIndex(name = "vendor_plat_field_idx", def = "{'vendor': 1, 'platform': 1, 'canonicalField': 1}", unique = true)
})
public class RemediationTemplateDocument {

    @Id
    private String id;

    @Indexed
    private String controlId;

    @Indexed
    private String ruleId;

    @Indexed
    private String vendor;

    @Indexed
    private String platform;

    @Indexed
    private String canonicalField;

    private String title;
    private List<String> commands = new ArrayList<>();
    private String description;

    private List<String> preconditions = new ArrayList<>();
    private List<String> verification = new ArrayList<>();

    private String risk = "LOW";

    @Indexed
    private String status = "ACTIVE";

    private int version = 1;

    private CommandType commandType = CommandType.DERIVABLE_REGEX;
    private String gapExplanation;

    @Indexed
    private String confirmationStatus = "UNCONFIRMED";

    private Instant createdAt;
    private Instant updatedAt;

    public RemediationTemplateDocument() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
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

    public String getCanonicalField() {
        return canonicalField;
    }

    public void setCanonicalField(String canonicalField) {
        this.canonicalField = canonicalField;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands != null ? commands : new ArrayList<>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(List<String> preconditions) {
        this.preconditions = preconditions != null ? preconditions : new ArrayList<>();
    }

    public List<String> getVerification() {
        return verification;
    }

    public void setVerification(List<String> verification) {
        this.verification = verification != null ? verification : new ArrayList<>();
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }

    public String getGapExplanation() {
        return gapExplanation;
    }

    public void setGapExplanation(String gapExplanation) {
        this.gapExplanation = gapExplanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getConfirmationStatus() {
        return confirmationStatus != null ? confirmationStatus : "UNCONFIRMED";
    }

    public void setConfirmationStatus(String confirmationStatus) {
        this.confirmationStatus = confirmationStatus;
    }
}
