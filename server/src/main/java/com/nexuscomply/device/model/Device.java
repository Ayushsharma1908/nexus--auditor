package com.nexuscomply.device.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "devices")
public class Device {

    @Id
    private String id;

    @Indexed
    private String hostname;

    private String vendor;
    private String platform;
    private String ip;
    private String model;
    private String osVersion;
    private String environment;
    private String location;
    private String status = "Healthy"; // Healthy, At risk, Critical, Unknown
    private String lastAudit;
    private Integer risk = 0;
    private Integer compliance = 100;
    private String criticality = "Medium"; // Critical, High, Medium, Low
    private Integer findings = 0;

    public Device() {}

    public Device(String id, String hostname, String vendor, String platform, String ip,
                  String model, String osVersion, String environment, String location,
                  String status, String lastAudit, Integer risk, Integer compliance,
                  String criticality, Integer findings) {
        this.id = id;
        this.hostname = hostname;
        this.vendor = vendor;
        this.platform = platform;
        this.ip = ip;
        this.model = model;
        this.osVersion = osVersion;
        this.environment = environment;
        this.location = location;
        this.status = status;
        this.lastAudit = lastAudit;
        this.risk = risk;
        this.compliance = compliance;
        this.criticality = criticality;
        this.findings = findings;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastAudit() {
        return lastAudit;
    }

    public void setLastAudit(String lastAudit) {
        this.lastAudit = lastAudit;
    }

    public Integer getRisk() {
        return risk;
    }

    public void setRisk(Integer risk) {
        this.risk = risk;
    }

    public Integer getCompliance() {
        return compliance;
    }

    public void setCompliance(Integer compliance) {
        this.compliance = compliance;
    }

    public String getCriticality() {
        return criticality;
    }

    public void setCriticality(String criticality) {
        this.criticality = criticality;
    }

    public Integer getFindings() {
        return findings;
    }

    public void setFindings(Integer findings) {
        this.findings = findings;
    }
}
