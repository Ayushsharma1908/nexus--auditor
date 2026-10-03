package com.nexuscomply.cyber.canonical;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CanonicalSecurityModel {

    private String schemaVersion = "1.0";
    private Map<String, Object> security = new LinkedHashMap<>();
    private Map<String, Object> authentication = new LinkedHashMap<>();
    private Map<String, Object> logging = new LinkedHashMap<>();
    private Map<String, Object> ntp = new LinkedHashMap<>();
    private Map<String, Object> managementAccess = new LinkedHashMap<>();
    private Map<String, Object> acl = new LinkedHashMap<>();
    private Map<String, Object> crypto = new LinkedHashMap<>();
    private Map<String, Object> services = new LinkedHashMap<>();

    public CanonicalSecurityModel() {
        // Initialize default empty maps for required schema domains
        initializeDomains();
    }

    private void initializeDomains() {
        if (security == null) security = new LinkedHashMap<>();
        if (authentication == null) authentication = new LinkedHashMap<>();
        if (logging == null) logging = new LinkedHashMap<>();
        if (ntp == null) ntp = new LinkedHashMap<>();
        if (managementAccess == null) managementAccess = new LinkedHashMap<>();
        if (acl == null) acl = new LinkedHashMap<>();
        if (crypto == null) crypto = new LinkedHashMap<>();
        if (services == null) services = new LinkedHashMap<>();
    }

    // Convenience setters for standard domain fields
    @SuppressWarnings("unchecked")
    public void setSsh(Boolean enabled, Integer version) {
        Map<String, Object> ssh = (Map<String, Object>) security.computeIfAbsent("ssh", k -> new LinkedHashMap<String, Object>());
        if (enabled != null) ssh.put("enabled", enabled);
        if (version != null) ssh.put("version", version);
    }

    @SuppressWarnings("unchecked")
    public void setTelnet(Boolean enabled) {
        Map<String, Object> telnet = (Map<String, Object>) security.computeIfAbsent("telnet", k -> new LinkedHashMap<String, Object>());
        if (enabled != null) telnet.put("enabled", enabled);
    }

    @SuppressWarnings("unchecked")
    public void setHttps(Boolean enabled) {
        Map<String, Object> https = (Map<String, Object>) security.computeIfAbsent("https", k -> new LinkedHashMap<String, Object>());
        if (enabled != null) https.put("enabled", enabled);
    }

    @SuppressWarnings("unchecked")
    public void setSnmp(Boolean enabled, String version) {
        Map<String, Object> snmp = (Map<String, Object>) security.computeIfAbsent("snmp", k -> new LinkedHashMap<String, Object>());
        if (enabled != null) snmp.put("enabled", enabled);
        if (version != null) snmp.put("version", version);
    }

    public void setAaa(Boolean aaa) {
        if (aaa != null) authentication.put("aaa", aaa);
    }

    public void setSyslog(Boolean syslog) {
        if (syslog != null) logging.put("syslog", syslog);
    }

    public void setLocalLogging(Boolean localLogging) {
        if (localLogging != null) logging.put("localLogging", localLogging);
    }

    public void setNtpConfigured(Boolean configured) {
        if (configured != null) ntp.put("configured", configured);
    }

    // Getters and Setters
    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Map<String, Object> getSecurity() {
        return security;
    }

    public void setSecurity(Map<String, Object> security) {
        this.security = security != null ? security : new LinkedHashMap<>();
    }

    public Map<String, Object> getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Map<String, Object> authentication) {
        this.authentication = authentication != null ? authentication : new LinkedHashMap<>();
    }

    public Map<String, Object> getLogging() {
        return logging;
    }

    public void setLogging(Map<String, Object> logging) {
        this.logging = logging != null ? logging : new LinkedHashMap<>();
    }

    public Map<String, Object> getNtp() {
        return ntp;
    }

    public void setNtp(Map<String, Object> ntp) {
        this.ntp = ntp != null ? ntp : new LinkedHashMap<>();
    }

    public Map<String, Object> getManagementAccess() {
        return managementAccess;
    }

    public void setManagementAccess(Map<String, Object> managementAccess) {
        this.managementAccess = managementAccess != null ? managementAccess : new LinkedHashMap<>();
    }

    public Map<String, Object> getAcl() {
        return acl;
    }

    public void setAcl(Map<String, Object> acl) {
        this.acl = acl != null ? acl : new LinkedHashMap<>();
    }

    public Map<String, Object> getCrypto() {
        return crypto;
    }

    public void setCrypto(Map<String, Object> crypto) {
        this.crypto = crypto != null ? crypto : new LinkedHashMap<>();
    }

    public Map<String, Object> getServices() {
        return services;
    }

    public void setServices(Map<String, Object> services) {
        this.services = services != null ? services : new LinkedHashMap<>();
    }
}
