package com.nexuscomply.cyber.parser.fortinet;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.parser.ParserResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FortinetFortiOSParserTest {

    private FortinetFortiOSParser parser;

    @BeforeEach
    void setUp() {
        parser = new FortinetFortiOSParser();
    }

    @Test
    @DisplayName("supports() contract: true for Fortinet/FortiOS, false for Cisco/Juniper/Palo Alto")
    void testSupportsContract() {
        assertThat(parser.supports("Fortinet", "FortiOS")).isTrue();
        assertThat(parser.supports("FORTINET", "FortiOS")).isTrue();
        assertThat(parser.supports("Fortigate", "FortiOS")).isTrue();
        assertThat(parser.supports(null, "FortiOS")).isTrue();

        assertThat(parser.supports("Cisco", "IOS")).isFalse();
        assertThat(parser.supports("Cisco", "IOS-XE")).isFalse();
        assertThat(parser.supports("Juniper", "JUNOS")).isFalse();
        assertThat(parser.supports("Palo Alto", "PAN-OS")).isFalse();
        assertThat(parser.supports(null, null)).isFalse();
    }

    @Test
    @DisplayName("Safe error handling: null, empty, or whitespace-only input returns FAILED without throwing")
    void testMalformedAndNullInput() {
        ParserResult nullResult = parser.parse(null);
        assertThat(nullResult.getStatus()).isEqualTo("FAILED");
        assertThat(nullResult.getUnknowns()).isNotEmpty();

        ParserResult emptyResult = parser.parse("");
        assertThat(emptyResult.getStatus()).isEqualTo("FAILED");
        assertThat(emptyResult.getUnknowns()).isNotEmpty();

        ParserResult blankResult = parser.parse("   \n\t  \r\n  ");
        assertThat(blankResult.getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Block-context tracking: Same 'set status enable' command behaves differently in different blocks")
    void testBlockContextTrackingWithSameCommandInDifferentBlocks() {
        String config = String.join("\n",
                "config system snmp sysinfo",
                "    set status enable",
                "end",
                "config log memory setting",
                "    set status enable",
                "end",
                "config system ntp",
                "    set status enable",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();

        // 1. In system snmp sysinfo -> sets security.snmp.enabled = true
        Map<String, Object> snmp = (Map<String, Object>) canonical.getSecurity().get("snmp");
        assertThat(snmp).isNotNull();
        assertThat(snmp.get("enabled")).isEqualTo(true);

        // 2. In log memory setting -> sets logging.localLogging = true
        assertThat(canonical.getLogging().get("localLogging")).isEqualTo(true);

        // 3. In system ntp -> 'set status enable' is not a valid NTP trigger (expects 'set ntpsync enable'),
        // so ntp.configured is NOT set and the line is placed in unknowns
        assertThat(canonical.getNtp().get("configured")).isNull();
        assertThat(result.getUnknowns()).extracting(u -> u.getSourceLine()).contains(8);
    }

    @Test
    @DisplayName("Block-context tracking: 'set allowaccess' in non-interface block is ignored and placed in unknowns")
    void testAllowAccessIgnoredOutsideInterfaceBlock() {
        String config = String.join("\n",
                "config firewall address",
                "    edit \"internal-net\"",
                "        set allowaccess ping https ssh",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();

        // Security model must not have SSH or HTTPS enabled from firewall address block
        assertThat(canonical.getSecurity()).doesNotContainKey("ssh");
        assertThat(canonical.getSecurity()).doesNotContainKey("https");

        // The line must appear in unknowns
        assertThat(result.getUnknowns()).anyMatch(u -> u.getRawText().contains("set allowaccess ping https ssh"));
    }

    @Test
    @DisplayName("Rule 1: 'set allowaccess ping https ssh' enables SSH and HTTPS, leaves SSH version UNSET")
    void testInterfaceAllowAccessSshAndHttps() {
        String config = String.join("\n",
                "config system interface",
                "    edit \"port1\"",
                "        set allowaccess ping https ssh",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();

        Map<String, Object> ssh = (Map<String, Object>) canonical.getSecurity().get("ssh");
        assertThat(ssh).isNotNull();
        assertThat(ssh.get("enabled")).isEqualTo(true);
        // Explicitly test that ssh.version remains unset (platform capability gap)
        assertThat(ssh.get("version")).isNull();

        Map<String, Object> https = (Map<String, Object>) canonical.getSecurity().get("https");
        assertThat(https).isNotNull();
        assertThat(https.get("enabled")).isEqualTo(true);

        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .containsExactlyInAnyOrder("security.ssh.enabled", "security.https.enabled");
    }

    @Test
    @DisplayName("Rule 1 Telnet: 'set allowaccess telnet' extracts security.telnet.enabled = true")
    void testInterfaceAllowAccessTelnet() {
        String config = String.join("\n",
                "config system interface",
                "    edit \"mgmt0\"",
                "        set allowaccess ping telnet",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();

        Map<String, Object> telnet = (Map<String, Object>) canonical.getSecurity().get("telnet");
        assertThat(telnet).isNotNull();
        assertThat(telnet.get("enabled")).isEqualTo(true);

        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .contains("security.telnet.enabled");
    }

    @Test
    @DisplayName("Telnet Asymmetry Test: When telnet is absent from all allowaccess lists, security.telnet.enabled is UNSET, not false")
    void testTelnetAbsentLeavesSecurityTelnetUnsetNotFalse() {
        String config = String.join("\n",
                "config system interface",
                "    edit \"port1\"",
                "        set allowaccess ping https ssh",
                "    next",
                "    edit \"port2\"",
                "        set allowaccess ping",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();

        // In FortiOS, absence of telnet does not prove explicit disablement
        Map<String, Object> telnet = (Map<String, Object>) canonical.getSecurity().get("telnet");
        if (telnet != null) {
            assertThat(telnet.get("enabled")).isNull();
        } else {
            assertThat(canonical.getSecurity()).doesNotContainKey("telnet");
        }
    }

    @Test
    @DisplayName("Rule 2: 'config system snmp sysinfo' with 'set status enable' extracts snmp.enabled = true, version unset")
    void testSnmpSysinfo() {
        String config = String.join("\n",
                "config system snmp sysinfo",
                "    set status enable",
                "    set description \"FortiGate-60F\"",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();

        Map<String, Object> snmp = (Map<String, Object>) canonical.getSecurity().get("snmp");
        assertThat(snmp).isNotNull();
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isNull();

        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .contains("security.snmp.enabled");
    }

    @Test
    @DisplayName("Rule 3: 'config system snmp user' with 'set security-level auth-priv' extracts snmp.enabled = true, snmp.version = '3'")
    void testSnmpV3User() {
        String config = String.join("\n",
                "config system snmp user",
                "    edit \"snmp3-admin\"",
                "        set security-level auth-priv",
                "        set auth-pwd \"secretAuth123\"",
                "        set priv-pwd \"secretPriv123\"",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();

        Map<String, Object> snmp = (Map<String, Object>) canonical.getSecurity().get("snmp");
        assertThat(snmp).isNotNull();
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isEqualTo("3");

        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .containsExactlyInAnyOrder("security.snmp.enabled", "security.snmp.version");
    }

    @Test
    @DisplayName("Syslog Case A: 'status enable' but no server leaves logging.syslog UNSET")
    void testSyslogStatusEnableNoServerLeavesSyslogUnset() {
        String config = String.join("\n",
                "config log syslogd setting",
                "    set status enable",
                "end"
        );
        ParserResult result = parser.parse(config);
        assertThat(result.getCanonical().getLogging().get("syslog")).isNull();
    }

    @Test
    @DisplayName("Syslog Case B: server set but 'status disable' leaves logging.syslog UNSET")
    void testSyslogServerSetStatusDisableLeavesSyslogUnset() {
        String config = String.join("\n",
                "config log syslogd setting",
                "    set status disable",
                "    set server \"192.168.1.100\"",
                "end"
        );
        ParserResult result = parser.parse(config);
        assertThat(result.getCanonical().getLogging().get("syslog")).isNull();
    }

    @Test
    @DisplayName("Syslog Case C: 'status enable' AND server set extracts logging.syslog = true")
    void testSyslogStatusEnableAndServerSet() {
        String config = String.join("\n",
                "config log syslogd setting",
                "    set status enable",
                "    set server \"192.168.1.100\"",
                "end"
        );
        ParserResult result = parser.parse(config);
        assertThat(result.getCanonical().getLogging().get("syslog")).isEqualTo(true);
        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .contains("logging.syslog");
    }

    @Test
    @DisplayName("Syslog ordering: 'set server' configured BEFORE 'set status enable' extracts logging.syslog = true")
    void testSyslogServerBeforeStatusEnable() {
        String config = String.join("\n",
                "config log syslogd setting",
                "    set server \"192.168.1.100\"",
                "    set status enable",
                "end"
        );
        ParserResult result = parser.parse(config);
        assertThat(result.getCanonical().getLogging().get("syslog")).isEqualTo(true);
        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .contains("logging.syslog");
    }

    @Test
    @DisplayName("SNMP Case A: community-only (v2c) extracts snmp.enabled = true, snmp.version = null")
    void testSnmpCommunityOnly() {
        String config = String.join("\n",
                "config system snmp community",
                "    edit 1",
                "        set name \"public\"",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);
        Map<String, Object> snmp = (Map<String, Object>) result.getCanonical().getSecurity().get("snmp");
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isNull();
    }

    @Test
    @DisplayName("SNMP Case B: v2c community + v3 user extracts snmp.enabled = true, snmp.version = null (NOT '3')")
    void testSnmpCommunityPlusV3UserLeavesVersionUnset() {
        String config = String.join("\n",
                "config system snmp community",
                "    edit 1",
                "        set name \"public\"",
                "    next",
                "end",
                "config system snmp user",
                "    edit \"v3user\"",
                "        set security-level auth-priv",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);
        Map<String, Object> snmp = (Map<String, Object>) result.getCanonical().getSecurity().get("snmp");
        assertThat(snmp.get("enabled")).isEqualTo(true);
        // Mixed mode: must NOT report "3"
        assertThat(snmp.get("version")).isNull();
    }

    @Test
    @DisplayName("SNMP Case C: v3 user only extracts snmp.enabled = true, snmp.version = '3'")
    void testSnmpV3UserOnly() {
        String config = String.join("\n",
                "config system snmp user",
                "    edit \"v3user\"",
                "        set security-level auth-priv",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);
        Map<String, Object> snmp = (Map<String, Object>) result.getCanonical().getSecurity().get("snmp");
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isEqualTo("3");
    }

    @Test
    @DisplayName("Nesting snippet from specification Item 5")
    void testNestingSnippetFromSpecItem5() {
        String config = String.join("\n",
                "config system admin",
                "    edit \"admin\"",
                "        set accprofile super_admin",
                "    next",
                "end",
                "config system interface",
                "    edit \"port1\"",
                "        set allowaccess ping https ssh",
                "    next",
                "end",
                "config system interface",
                "    edit \"port2\"",
                "        set allowaccess telnet",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        Map<String, Object> sec = result.getCanonical().getSecurity();
        Map<String, Object> ssh = (Map<String, Object>) sec.get("ssh");
        assertThat(ssh.get("enabled")).isEqualTo(true);
        assertThat(ssh.get("version")).isNull();

        Map<String, Object> telnet = (Map<String, Object>) sec.get("telnet");
        assertThat(telnet.get("enabled")).isEqualTo(true);

        Map<String, Object> https = (Map<String, Object>) sec.get("https");
        assertThat(https.get("enabled")).isEqualTo(true);

        // Local admin leaves AAA unset
        assertThat(result.getCanonical().getAuthentication().get("aaa")).isNull();

        // Unknowns has set accprofile super_admin
        assertThat(result.getUnknowns()).hasSize(1);
        assertThat(result.getUnknowns().get(0).getRawText().trim()).isEqualTo("set accprofile super_admin");
    }

    @Test
    @DisplayName("Rule 5: 'config log memory setting' and 'config log disk setting' extract logging.localLogging = true")
    void testLocalLogging() {
        String configMemory = String.join("\n",
                "config log memory setting",
                "    set status enable",
                "end"
        );
        ParserResult res1 = parser.parse(configMemory);
        assertThat(res1.getCanonical().getLogging().get("localLogging")).isEqualTo(true);

        String configDisk = String.join("\n",
                "config log disk setting",
                "    set status enable",
                "end"
        );
        ParserResult res2 = parser.parse(configDisk);
        assertThat(res2.getCanonical().getLogging().get("localLogging")).isEqualTo(true);
    }

    @Test
    @DisplayName("Rule 6: 'config system ntp' with 'set ntpsync enable' extracts ntp.configured = true")
    void testNtpSyncEnable() {
        String config = String.join("\n",
                "config system ntp",
                "    set ntpsync enable",
                "    set type custom",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getNtp().get("configured")).isEqualTo(true);

        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .contains("ntp.configured");
    }

    @Test
    @DisplayName("AAA Interpretation: Local admin accounts under 'config system admin' leave authentication.aaa UNSET")
    void testLocalAdminLeavesAaaUnset() {
        String config = String.join("\n",
                "config system admin",
                "    edit \"secadmin\"",
                "        set accprofile \"super_admin\"",
                "        set password ENC xxxxxxxx",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        // Local accounts are not centralized AAA; must be left unset
        assertThat(result.getCanonical().getAuthentication().get("aaa")).isNull();
    }

    @Test
    @DisplayName("AAA Interpretation: Centralized external authentication ('config user radius') sets authentication.aaa = true")
    void testCentralizedRadiusSetsAaaTrue() {
        String config = String.join("\n",
                "config user radius",
                "    edit \"radius-server\"",
                "        set server \"10.0.0.50\"",
                "        set secret ENC xxxxxxxx",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getAuthentication().get("aaa")).isEqualTo(true);
        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .contains("authentication.aaa");
    }

    @Test
    @DisplayName("Comments are ignored and unrecognized lines are recorded in unknowns with line numbers")
    void testCommentsAndUnknowns() {
        String config = String.join("\n",
                "# FortiGate FortiOS 7.2 Configuration",
                "config system interface",
                "    edit \"port1\"",
                "        set allowaccess ping https ssh",
                "        set ip 192.168.1.99 255.255.255.0",
                "    next",
                "end"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        // Comments should not be in unknowns
        assertThat(result.getUnknowns()).hasSize(1);
        assertThat(result.getUnknowns().get(0).getSourceLine()).isEqualTo(5);
        assertThat(result.getUnknowns().get(0).getRawText().trim()).isEqualTo("set ip 192.168.1.99 255.255.255.0");
    }
}
