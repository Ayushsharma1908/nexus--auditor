package com.nexuscomply.cyber.remediation.seeder;

import com.nexuscomply.cyber.remediation.model.CommandType;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateDocument;
import com.nexuscomply.cyber.remediation.persistence.RemediationTemplateRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Seeds curated remediation templates for all 4 vendors x 7 canonical fields (28 combinations).
 * Fully idempotent, safe to run multiple times without duplicating data.
 * All commands are strictly grounded in validated parser extraction patterns or explicitly
 * flagged as representative examples / platform gaps per Absolute Rules 3 and 4.
 */
@Component
public class RemediationTemplateSeeder {

    private final RemediationTemplateRepository repository;

    public RemediationTemplateSeeder(RemediationTemplateRepository repository) {
        this.repository = repository;
    }

    public static class TemplateDefinition {
        private final String vendor;
        private final String platform;
        private final String canonicalField;
        private final String title;
        private final List<String> commands;
        private final String description;
        private final List<String> preconditions;
        private final List<String> verification;
        private final String risk;
        private final CommandType commandType;
        private final String gapExplanation;

        public TemplateDefinition(
                String vendor,
                String platform,
                String canonicalField,
                String title,
                List<String> commands,
                String description,
                List<String> preconditions,
                List<String> verification,
                String risk,
                CommandType commandType,
                String gapExplanation) {
            this.vendor = vendor;
            this.platform = platform;
            this.canonicalField = canonicalField;
            this.title = title;
            this.commands = commands;
            this.description = description;
            this.preconditions = preconditions;
            this.verification = verification;
            this.risk = risk;
            this.commandType = commandType;
            this.gapExplanation = gapExplanation;
        }

        public String getVendor() { return vendor; }
        public String getPlatform() { return platform; }
        public String getCanonicalField() { return canonicalField; }
        public String getTitle() { return title; }
        public List<String> getCommands() { return commands; }
        public String getDescription() { return description; }
        public List<String> getPreconditions() { return preconditions; }
        public List<String> getVerification() { return verification; }
        public String getRisk() { return risk; }
        public CommandType getCommandType() { return commandType; }
        public String getGapExplanation() { return gapExplanation; }
    }

    public static final List<TemplateDefinition> DEFINITIONS = List.of(
            // ==========================================
            // CISCO (IOS-XE) - 7 Canonical Fields
            // ==========================================
            new TemplateDefinition(
                    "Cisco", "IOS-XE", "authentication.aaa",
                    "Enable AAA Service (Cisco)",
                    List.of("aaa new-model"),
                    "Enable AAA model on Cisco IOS-XE to mandate centralized authentication, authorization, and accounting.",
                    List.of("Privileged EXEC mode access"),
                    List.of("show running-config | include aaa new-model"),
                    "MEDIUM", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Cisco", "IOS-XE", "security.telnet.enabled",
                    "Disable Telnet on VTY Lines (Cisco)",
                    List.of("line vty 0 4", "transport input ssh"),
                    "Enforce SSH-only transport on VTY lines, disabling unencrypted Telnet communication.",
                    List.of("SSH must be configured with domain name and crypto key before disabling Telnet"),
                    List.of("show running-config | section line vty"),
                    "MEDIUM", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Cisco", "IOS-XE", "security.ssh.version",
                    "Enable SSH Version 2 (Cisco)",
                    List.of("ip ssh version 2"),
                    "Configure Cisco IOS-XE to enforce SSH version 2 protocol.",
                    List.of("Crypto key generate rsa completed", "ip domain-name configured"),
                    List.of("show ip ssh"),
                    "LOW", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Cisco", "IOS-XE", "security.snmp.version",
                    "Configure SNMPv3 Privacy Group (Cisco)",
                    List.of("snmp-server group <name> v3 priv"),
                    "Representative example beyond parser regex: Configure SNMPv3 group with privacy (authPriv encryption). Full real-world configuration requires auth/priv passwords and users.",
                    List.of("SNMP community strings should be decommissioned"),
                    List.of("show snmp group"),
                    "MEDIUM", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),
            new TemplateDefinition(
                    "Cisco", "IOS-XE", "logging.syslog",
                    "Configure Remote Syslog Logging Host (Cisco)",
                    List.of("logging host <ip>"),
                    "Representative example beyond parser regex: Forward system logging messages to centralized syslog server.",
                    List.of("Target syslog host must be IP-reachable"),
                    List.of("show logging"),
                    "LOW", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),
            new TemplateDefinition(
                    "Cisco", "IOS-XE", "logging.localLogging",
                    "Configure Local Buffered Logging (Cisco)",
                    List.of("logging buffered <size>"),
                    "[UNCONFIRMED] Representative example beyond parser regex: Allocate internal memory buffer to store local logging entries.",
                    List.of("Sufficient free device RAM"),
                    List.of("show logging"),
                    "LOW", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),
            new TemplateDefinition(
                    "Cisco", "IOS-XE", "ntp.configured",
                    "Configure NTP Server (Cisco)",
                    List.of("ntp server <ip>"),
                    "Representative example beyond parser regex: Configure authoritative NTP server for accurate time synchronization.",
                    List.of("NTP server reachable on UDP port 123"),
                    List.of("show ntp status", "show ntp associations"),
                    "LOW", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),

            // ==========================================
            // JUNIPER (JUNOS) - 7 Canonical Fields (all UNCONFIRMED)
            // ==========================================
            new TemplateDefinition(
                    "Juniper", "JUNOS", "authentication.aaa",
                    "Configure System Authentication Order (Juniper)",
                    List.of("set system authentication-order [ radius password ]"),
                    "[UNCONFIRMED] Representative example: Configure RADIUS fallback to local password. Requires configured radius-server block.",
                    List.of("system radius-server configured with IP and secret"),
                    List.of("show configuration system authentication-order"),
                    "MEDIUM", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),
            new TemplateDefinition(
                    "Juniper", "JUNOS", "security.telnet.enabled",
                    "Disable Telnet Service (Juniper)",
                    List.of("delete system services telnet"),
                    "[UNCONFIRMED] Disable Telnet daemon under system services on Junos.",
                    List.of("SSH service must remain operational under system services"),
                    List.of("show configuration system services telnet"),
                    "LOW", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Juniper", "JUNOS", "security.ssh.version",
                    "Enforce SSH Protocol Version 2 (Juniper)",
                    List.of("set system services ssh protocol-version v2"),
                    "[UNCONFIRMED] Enforce SSH protocol version 2 on Junos.",
                    List.of("system services ssh configured"),
                    List.of("show configuration system services ssh"),
                    "LOW", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Juniper", "JUNOS", "security.snmp.version",
                    "Configure SNMPv3 USM Local User (Juniper)",
                    List.of("set snmp v3 usm local-user <name> ..."),
                    "[UNCONFIRMED] Representative example beyond parser regex: Configure SNMPv3 USM user with authentication and privacy credentials.",
                    List.of("SNMP service enabled"),
                    List.of("show configuration snmp v3"),
                    "MEDIUM", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),
            new TemplateDefinition(
                    "Juniper", "JUNOS", "logging.syslog",
                    "Configure Remote Syslog Host (Juniper)",
                    List.of("set system syslog host <ip> any any"),
                    "[UNCONFIRMED] Representative example beyond parser regex: Send system logs to remote syslog server.",
                    List.of("Syslog destination reachable"),
                    List.of("show configuration system syslog host"),
                    "LOW", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),
            new TemplateDefinition(
                    "Juniper", "JUNOS", "logging.localLogging",
                    "Configure Local Syslog File Logging (Juniper)",
                    List.of("set system syslog file messages any notice"),
                    "[UNCONFIRMED] Configure system messages log file for local event recording on Junos.",
                    List.of("Sufficient disk space under /var/log"),
                    List.of("show configuration system syslog file messages"),
                    "LOW", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Juniper", "JUNOS", "ntp.configured",
                    "Configure NTP Server (Juniper)",
                    List.of("set system ntp server <ip>"),
                    "[UNCONFIRMED] Representative example beyond parser regex: Configure external NTP server for clock synchronization on Junos.",
                    List.of("NTP server reachable on UDP port 123"),
                    List.of("show ntp status", "show ntp associations"),
                    "LOW", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),

            // ==========================================
            // FORTINET (FortiOS) - 7 Canonical Fields (6 UNCONFIRMED, 1 PLATFORM_GAP)
            // ==========================================
            new TemplateDefinition(
                    "Fortinet", "FortiOS", "authentication.aaa",
                    "Configure RADIUS AAA Server (Fortinet)",
                    List.of("config user radius", "edit <name>", "set server <ip>", "set secret <key>", "next", "end"),
                    "[UNCONFIRMED] Representative multi-step example beyond parser regex: Configure RADIUS server profile on FortiOS.",
                    List.of("RADIUS server IP and pre-shared secret ready"),
                    List.of("show user radius"),
                    "MEDIUM", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),
            new TemplateDefinition(
                    "Fortinet", "FortiOS", "security.telnet.enabled",
                    "Disable Telnet on Interface Allowaccess (Fortinet)",
                    List.of("config system interface", "edit <port>", "unselect allowaccess telnet", "next", "end"),
                    "[UNCONFIRMED] Remove telnet from allowaccess list on administrative interfaces without removing other permitted protocols via unselect.",
                    List.of("HTTPS or SSH administrative connectivity confirmed"),
                    List.of("show system interface"),
                    "MEDIUM", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Fortinet", "FortiOS", "security.ssh.version",
                    "SSH Version Configuration Gap (Fortinet)",
                    List.of(),
                    "not configurable on this platform",
                    List.of(),
                    List.of(),
                    "LOW", CommandType.PLATFORM_GAP, "not configurable on this platform"
            ),
            new TemplateDefinition(
                    "Fortinet", "FortiOS", "security.snmp.version",
                    "Configure SNMPv3 User (Fortinet)",
                    List.of("config system snmp user", "edit <name>", "set security-level auth-priv", "next", "end"),
                    "[UNCONFIRMED] Representative example beyond parser regex: Configure SNMPv3 user with auth-priv security level.",
                    List.of("SNMP agent enabled under config system snmp sysinfo"),
                    List.of("show system snmp user"),
                    "MEDIUM", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),
            new TemplateDefinition(
                    "Fortinet", "FortiOS", "logging.syslog",
                    "Configure Remote Syslog Logging (Fortinet)",
                    List.of("config log syslogd setting", "set status enable", "set server <ip>", "end"),
                    "[UNCONFIRMED] Representative example beyond parser regex: Enable remote syslog daemon forwarding.",
                    List.of("Syslog server reachable"),
                    List.of("get log syslogd setting"),
                    "LOW", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),
            new TemplateDefinition(
                    "Fortinet", "FortiOS", "logging.localLogging",
                    "Enable Local Memory Logging (Fortinet)",
                    List.of("config log memory setting", "set status enable", "end"),
                    "[UNCONFIRMED] Enable internal memory buffer logging on FortiOS.",
                    List.of("Device has sufficient available memory"),
                    List.of("get log memory setting"),
                    "LOW", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Fortinet", "FortiOS", "ntp.configured",
                    "Enable NTP Synchronization (Fortinet)",
                    List.of("config system ntp", "set ntpsync enable", "end"),
                    "[UNCONFIRMED] Representative example beyond parser regex: Enable NTP synchronization on FortiOS. Full config requires ntpserver sub-block.",
                    List.of("NTP server or FortiGuard NTP connectivity available"),
                    List.of("show system ntp"),
                    "LOW", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),

            // ==========================================
            // PALO ALTO (PAN-OS) - 7 Canonical Fields (5 UNCONFIRMED, 1 CONFIRMED, 1 PLATFORM_GAP)
            // ==========================================
            new TemplateDefinition(
                    "Palo Alto", "PAN-OS", "authentication.aaa",
                    "Configure RADIUS Authentication Profile (Palo Alto)",
                    List.of("set shared authentication-profile <name> method radius", "set mgt-config users <admin> authentication-profile <name>"),
                    "[UNCONFIRMED] Two-step configuration directly matching parser detection: Configure shared RADIUS profile and assign to management user.",
                    List.of("RADIUS server profile defined under shared radius"),
                    List.of("show shared authentication-profile"),
                    "MEDIUM", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Palo Alto", "PAN-OS", "security.telnet.enabled",
                    "Disable Telnet Service (Palo Alto)",
                    List.of("set deviceconfig system service disable-telnet yes"),
                    "Explicitly disable telnet management service under deviceconfig system service (confirmed via Palo Alto KB kA10g000000CltrCAC).",
                    List.of("SSH or Web UI management access confirmed"),
                    List.of("show deviceconfig system service"),
                    "LOW", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Palo Alto", "PAN-OS", "security.ssh.version",
                    "Enforce SSH Version 2 (Palo Alto)",
                    List.of("set deviceconfig system ssh-service version 2"),
                    "[UNCONFIRMED] Configure PAN-OS ssh-service to enforce protocol version 2.",
                    List.of("SSH service operational"),
                    List.of("show deviceconfig system ssh-service"),
                    "LOW", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Palo Alto", "PAN-OS", "security.snmp.version",
                    "Configure SNMPv3 User Access (Palo Alto)",
                    List.of("set deviceconfig system snmp-setting access-setting version v3 users <user> ..."),
                    "[UNCONFIRMED] Representative example beyond parser regex: Configure SNMPv3 access setting with user credentials.",
                    List.of("SNMP views configured"),
                    List.of("show deviceconfig system snmp-setting"),
                    "MEDIUM", CommandType.REPRESENTATIVE_EXAMPLE, null
            ),
            new TemplateDefinition(
                    "Palo Alto", "PAN-OS", "logging.syslog",
                    "Configure Syslog Server and Forwarding (Palo Alto)",
                    List.of("set shared log-settings syslog <profile> server <name> server <ip>", "set shared log-settings system match-list <name> send-syslog <profile>"),
                    "[UNCONFIRMED] Two-step configuration directly matching parser detection: Define syslog server profile and bind to log match-list.",
                    List.of("Syslog destination reachable"),
                    List.of("show shared log-settings syslog"),
                    "LOW", CommandType.DERIVABLE_REGEX, null
            ),
            new TemplateDefinition(
                    "Palo Alto", "PAN-OS", "logging.localLogging",
                    "Local Logging Configuration Gap (Palo Alto)",
                    List.of(),
                    "not determinable from configuration evidence",
                    List.of(),
                    List.of(),
                    "LOW", CommandType.PLATFORM_GAP, "not determinable from configuration evidence"
            ),
            new TemplateDefinition(
                    "Palo Alto", "PAN-OS", "ntp.configured",
                    "Configure Primary NTP Server (Palo Alto)",
                    List.of("set deviceconfig system ntp-servers primary-ntp-server ntp-server-address <ip>"),
                    "[UNCONFIRMED] Configure primary NTP server IP address under deviceconfig system ntp-servers.",
                    List.of("NTP server reachable on UDP port 123"),
                    List.of("show deviceconfig system ntp-servers"),
                    "LOW", CommandType.DERIVABLE_REGEX, null
            )
    );

    /**
     * Seeds all 28 remediation templates idempotently.
     */
    public List<RemediationTemplateDocument> seed() {
        Instant now = Instant.now();
        List<RemediationTemplateDocument> results = new ArrayList<>();

        for (TemplateDefinition def : DEFINITIONS) {
            Optional<RemediationTemplateDocument> existingOpt = repository.findByVendorAndPlatformAndCanonicalField(
                    def.getVendor(), def.getPlatform(), def.getCanonicalField()
            );

            RemediationTemplateDocument doc = existingOpt.orElseGet(RemediationTemplateDocument::new);
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID().toString());
                doc.setCreatedAt(now);
            }
            doc.setVendor(def.getVendor());
            doc.setPlatform(def.getPlatform());
            doc.setCanonicalField(def.getCanonicalField());
            doc.setTitle(def.getTitle());
            doc.setCommands(def.getCommands());
            doc.setDescription(def.getDescription());
            doc.setPreconditions(def.getPreconditions());
            doc.setVerification(def.getVerification());
            doc.setRisk(def.getRisk());
            doc.setStatus("ACTIVE");
            doc.setVersion(1);
            doc.setCommandType(def.getCommandType());
            doc.setGapExplanation(def.getGapExplanation());

            String confStatus = "CONFIRMED";
            if (def.getDescription() != null && def.getDescription().contains("[UNCONFIRMED]")) {
                confStatus = "UNCONFIRMED";
            } else if ("Juniper".equalsIgnoreCase(def.getVendor())) {
                confStatus = "UNCONFIRMED";
            } else if ("Fortinet".equalsIgnoreCase(def.getVendor()) && def.getCommandType() != CommandType.PLATFORM_GAP) {
                confStatus = "UNCONFIRMED";
            } else if ("Palo Alto".equalsIgnoreCase(def.getVendor()) && !"security.telnet.enabled".equalsIgnoreCase(def.getCanonicalField()) && def.getCommandType() != CommandType.PLATFORM_GAP) {
                confStatus = "UNCONFIRMED";
            }
            doc.setConfirmationStatus(confStatus);

            doc.setUpdatedAt(now);

            RemediationTemplateDocument saved = repository.save(doc);
            results.add(saved);
        }

        return results;
    }
}
