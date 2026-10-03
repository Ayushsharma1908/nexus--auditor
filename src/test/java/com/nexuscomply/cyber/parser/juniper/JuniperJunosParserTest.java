package com.nexuscomply.cyber.parser.juniper;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.parser.ParserResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JuniperJunosParserTest {

    private JuniperJunosParser parser;

    @BeforeEach
    void setUp() {
        parser = new JuniperJunosParser();
    }

    @Test
    @DisplayName("supports() contract: true for Juniper/JUNOS, false for Cisco/Fortinet/Palo Alto")
    void testSupportsContract() {
        assertThat(parser.supports("Juniper", "JUNOS")).isTrue();
        assertThat(parser.supports("JUNIPER", "Junos")).isTrue();
        assertThat(parser.supports(null, "JUNOS")).isTrue();

        assertThat(parser.supports("Cisco", "IOS")).isFalse();
        assertThat(parser.supports("Cisco", "IOS-XE")).isFalse();
        assertThat(parser.supports("Fortinet", "FortiOS")).isFalse();
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
    @DisplayName("Rule 1: 'set system services ssh protocol-version v2' extracts ssh.enabled=true and ssh.version=2")
    void testSshWithProtocolVersionV2() {
        String config = "set system services ssh protocol-version v2";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();
        Map<String, Object> sec = (Map<String, Object>) canonical.getSecurity().get("ssh");
        assertThat(sec).isNotNull();
        assertThat(sec.get("enabled")).isEqualTo(true);
        assertThat(sec.get("version")).isEqualTo(2);

        assertThat(result.getSourceMap()).hasSize(2);
        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .containsExactlyInAnyOrder("security.ssh.version", "security.ssh.enabled");
    }

    @Test
    @DisplayName("Rule 2: 'set system services ssh' without explicit protocol-version leaves version unset (not assumed)")
    void testSshWithoutExplicitVersionLeavesVersionUnset() {
        String config = String.join("\n",
                "set system services ssh",
                "set system services ssh root-login deny"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();
        Map<String, Object> sec = (Map<String, Object>) canonical.getSecurity().get("ssh");
        assertThat(sec).isNotNull();
        assertThat(sec.get("enabled")).isEqualTo(true);
        // Version must NOT be assumed without explicit evidence
        assertThat(sec.get("version")).isNull();
    }

    @Test
    @DisplayName("Rule 3: 'set system services telnet' extracts security.telnet.enabled = true")
    void testTelnetEnabledWhenLinePresent() {
        String config = "set system services telnet";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();
        Map<String, Object> telnet = (Map<String, Object>) canonical.getSecurity().get("telnet");
        assertThat(telnet).isNotNull();
        assertThat(telnet.get("enabled")).isEqualTo(true);

        assertThat(result.getSourceMap()).hasSize(1);
        SourceMapEntry entry = result.getSourceMap().get(0);
        assertThat(entry.getCanonicalField()).isEqualTo("security.telnet.enabled");
        assertThat(entry.getSourceLine()).isEqualTo(1);
        assertThat(entry.getRawText()).isEqualTo("set system services telnet");
    }

    @Test
    @DisplayName("Asymmetry Test: When telnet line is absent, security.telnet.enabled is UNSET (null), not false")
    void testTelnetAbsentLeavesSecurityTelnetUnsetNotFalse() {
        String config = String.join("\n",
                "set system services ssh",
                "set system syslog host 10.0.0.1",
                "set system ntp server 10.0.0.2"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();
        // In Junos, absence of telnet does not prove explicit disablement
        Map<String, Object> telnet = (Map<String, Object>) canonical.getSecurity().get("telnet");
        if (telnet != null) {
            assertThat(telnet.get("enabled")).isNull();
        } else {
            assertThat(canonical.getSecurity()).doesNotContainKey("telnet");
        }
    }

    @Test
    @DisplayName("Rule 4: 'set system services web-management https' extracts security.https.enabled = true")
    void testWebManagementHttps() {
        String config = "set system services web-management https";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();
        Map<String, Object> https = (Map<String, Object>) canonical.getSecurity().get("https");
        assertThat(https).isNotNull();
        assertThat(https.get("enabled")).isEqualTo(true);
    }

    @Test
    @DisplayName("Rule 5: 'set snmp v3 ...' extracts snmp.enabled=true and snmp.version='3'")
    void testSnmpV3() {
        String config = "set snmp v3 usm local-user admin authentication-md5 authentication-password secret";
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
    @DisplayName("Rule 6: 'set snmp community ...' extracts snmp.enabled=true and leaves version unset")
    void testSnmpCommunity() {
        String config = "set snmp community public authorization read-only";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel canonical = result.getCanonical();
        Map<String, Object> snmp = (Map<String, Object>) canonical.getSecurity().get("snmp");
        assertThat(snmp).isNotNull();
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isNull();
    }

    @Test
    @DisplayName("Rule 7: 'set system authentication-order' with radius/tacplus sets authentication.aaa = true")
    void testAuthenticationOrderWithExternalAuth() {
        String config1 = "set system authentication-order [ radius password ]";
        ParserResult res1 = parser.parse(config1);
        assertThat(res1.getCanonical().getAuthentication().get("aaa")).isEqualTo(true);

        String config2 = "set system authentication-order tacplus";
        ParserResult res2 = parser.parse(config2);
        assertThat(res2.getCanonical().getAuthentication().get("aaa")).isEqualTo(true);
    }

    @Test
    @DisplayName("Rule 7 Negative: 'set system authentication-order password' does not set authentication.aaa")
    void testAuthenticationOrderPasswordOnlyDoesNotSetAaa() {
        String config = "set system authentication-order password";
        ParserResult res = parser.parse(config);
        assertThat(res.getCanonical().getAuthentication().get("aaa")).isNull();
    }

    @Test
    @DisplayName("Rules 8 & 9: Syslog host and file set logging.syslog and logging.localLogging")
    void testSyslogHostAndFile() {
        String config = String.join("\n",
                "set system syslog host 192.168.1.50 any any",
                "set system syslog file messages any notice"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getLogging().get("syslog")).isEqualTo(true);
        assertThat(result.getCanonical().getLogging().get("localLogging")).isEqualTo(true);

        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .containsExactlyInAnyOrder("logging.syslog", "logging.localLogging");
    }

    @Test
    @DisplayName("Rule 10: 'set system ntp server ...' sets ntp.configured = true")
    void testNtpServer() {
        String config = "set system ntp server 10.0.0.1";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getNtp().get("configured")).isEqualTo(true);
    }

    @Test
    @DisplayName("Comments are ignored and unrecognized lines are recorded in unknowns with line numbers")
    void testCommentsAndUnknowns() {
        String config = String.join("\n",
                "# This is a Junos comment",
                "set system services ssh",
                "/* Block comment */",
                "set interfaces ge-0/0/0 unit 0 family inet address 10.0.0.1/24",
                "set protocols lldp interface all"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        // Comments should not be in unknowns
        assertThat(result.getUnknowns()).hasSize(2);
        assertThat(result.getUnknowns().get(0).getSourceLine()).isEqualTo(4);
        assertThat(result.getUnknowns().get(0).getRawText()).isEqualTo("set interfaces ge-0/0/0 unit 0 family inet address 10.0.0.1/24");
        assertThat(result.getUnknowns().get(1).getSourceLine()).isEqualTo(5);
        assertThat(result.getUnknowns().get(1).getRawText()).isEqualTo("set protocols lldp interface all");
    }
}
