package com.nexuscomply.cyber.parser.cisco;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.UnknownConstruct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CiscoIosParserTest {

    private CiscoIosParser parser;

    @BeforeEach
    void setUp() {
        parser = new CiscoIosParser();
    }

    @Test
    @DisplayName("Supports Cisco vendor and IOS/IOS-XE platforms")
    void testSupports() {
        assertThat(parser.supports("Cisco", "IOS-XE")).isTrue();
        assertThat(parser.supports("cisco", "ios")).isTrue();
        assertThat(parser.supports(null, "IOS-XE")).isTrue();
        assertThat(parser.supports("Juniper", "Junos")).isFalse();
        assertThat(parser.supports("Fortinet", "FortiOS")).isFalse();
    }

    @Test
    @DisplayName("Real Cisco acceptance scenario config extracts all facts and tracks source lines")
    void testRealCiscoAcceptanceScenario() {
        String config = String.join("\n",
                "version 17.6",
                "hostname RTR-01",
                "!",
                "ip ssh version 2",
                "no ip http server",
                "ip http secure-server",
                "line vty 0 4",
                " transport input ssh",
                "snmp-server community private RO version 3",
                "aaa new-model",
                "logging on",
                "logging host 10.10.10.5",
                "logging buffered 16384",
                "ntp server 10.0.0.1",
                "!",
                "interface GigabitEthernet0/0/0",
                " ip address 10.0.0.1 255.255.255.0",
                "!"
        );

        ParserResult result = parser.parse(config);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        CanonicalSecurityModel model = result.getCanonical();
        assertThat(model.getSchemaVersion()).isEqualTo("1.0");

        // SSH
        @SuppressWarnings("unchecked")
        Map<String, Object> ssh = (Map<String, Object>) model.getSecurity().get("ssh");
        assertThat(ssh).isNotNull();
        assertThat(ssh.get("enabled")).isEqualTo(true);
        assertThat(ssh.get("version")).isEqualTo(2);

        // Telnet disabled via transport input ssh
        @SuppressWarnings("unchecked")
        Map<String, Object> telnet = (Map<String, Object>) model.getSecurity().get("telnet");
        assertThat(telnet).isNotNull();
        assertThat(telnet.get("enabled")).isEqualTo(false);

        // HTTPS enabled
        @SuppressWarnings("unchecked")
        Map<String, Object> https = (Map<String, Object>) model.getSecurity().get("https");
        assertThat(https).isNotNull();
        assertThat(https.get("enabled")).isEqualTo(true);

        // SNMP v3
        @SuppressWarnings("unchecked")
        Map<String, Object> snmp = (Map<String, Object>) model.getSecurity().get("snmp");
        assertThat(snmp).isNotNull();
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isEqualTo("3");

        // AAA
        assertThat(model.getAuthentication().get("aaa")).isEqualTo(true);

        // Logging
        assertThat(model.getLogging().get("syslog")).isEqualTo(true);
        assertThat(model.getLogging().get("localLogging")).isEqualTo(true);

        // NTP
        assertThat(model.getNtp().get("configured")).isEqualTo(true);

        // Source map tracking
        assertThat(result.getSourceMap()).isNotEmpty();
        assertThat(result.getSourceMap()).anySatisfy(entry -> {
            assertThat(entry.getCanonicalField()).isEqualTo("security.ssh.version");
            assertThat(entry.getSourceLine()).isEqualTo(4);
            assertThat(entry.getRawText()).isEqualTo("ip ssh version 2");
        });
        assertThat(result.getSourceMap()).anySatisfy(entry -> {
            assertThat(entry.getCanonicalField()).isEqualTo("security.https.enabled");
            assertThat(entry.getSourceLine()).isEqualTo(6);
            assertThat(entry.getRawText()).isEqualTo("ip http secure-server");
        });
        assertThat(result.getSourceMap()).anySatisfy(entry -> {
            assertThat(entry.getCanonicalField()).isEqualTo("security.telnet.enabled");
            assertThat(entry.getSourceLine()).isEqualTo(8);
        });

        // Unknowns preservation: lines not recognized like hostname, interface, ip address must be in unknowns
        assertThat(result.getUnknowns()).isNotEmpty();
        assertThat(result.getUnknowns()).extracting(UnknownConstruct::getRawText)
                .contains("hostname RTR-01", "interface GigabitEthernet0/0/0", " ip address 10.0.0.1 255.255.255.0");
    }

    @Test
    @DisplayName("Rule: no ip ssh disables SSH")
    void testNoIpSshRule() {
        String config = "no ip ssh";
        ParserResult result = parser.parse(config);
        @SuppressWarnings("unchecked")
        Map<String, Object> ssh = (Map<String, Object>) result.getCanonical().getSecurity().get("ssh");
        assertThat(ssh.get("enabled")).isEqualTo(false);
        assertThat(result.getSourceMap()).anyMatch(sm -> sm.getCanonicalField().equals("security.ssh.enabled") && sm.getSourceLine() == 1);
    }

    @Test
    @DisplayName("Rule: transport input telnet enables Telnet")
    void testTransportInputTelnetRule() {
        String config = "line vty 0 4\n transport input telnet";
        ParserResult result = parser.parse(config);
        @SuppressWarnings("unchecked")
        Map<String, Object> telnet = (Map<String, Object>) result.getCanonical().getSecurity().get("telnet");
        assertThat(telnet.get("enabled")).isEqualTo(true);
        assertThat(result.getSourceMap()).anyMatch(sm -> sm.getCanonicalField().equals("security.telnet.enabled") && sm.getSourceLine() == 2);
    }

    @Test
    @DisplayName("Rule: transport input ssh disables Telnet")
    void testTransportInputSshRule() {
        String config = "line vty 0 4\n transport input ssh";
        ParserResult result = parser.parse(config);
        @SuppressWarnings("unchecked")
        Map<String, Object> telnet = (Map<String, Object>) result.getCanonical().getSecurity().get("telnet");
        assertThat(telnet.get("enabled")).isEqualTo(false);
    }

    @Test
    @DisplayName("Rule: ip http secure-server enables HTTPS and no ip http server leaves HTTPS untouched")
    void testHttpAndHttpsRules() {
        String config = "no ip http server\nip http secure-server";
        ParserResult result = parser.parse(config);
        @SuppressWarnings("unchecked")
        Map<String, Object> https = (Map<String, Object>) result.getCanonical().getSecurity().get("https");
        assertThat(https.get("enabled")).isEqualTo(true);
        assertThat(result.getSourceMap()).anyMatch(sm -> sm.getCanonicalField().equals("security.https.enabled") && sm.getSourceLine() == 2);
    }

    @Test
    @DisplayName("Rule: snmp-server v3 extraction")
    void testSnmpV3Rule() {
        String config = "snmp-server group SECGROUP v3 priv";
        ParserResult result = parser.parse(config);
        @SuppressWarnings("unchecked")
        Map<String, Object> snmp = (Map<String, Object>) result.getCanonical().getSecurity().get("snmp");
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isEqualTo("3");
        assertThat(result.getSourceMap()).anyMatch(sm -> sm.getCanonicalField().equals("security.snmp.version") && sm.getSourceLine() == 1);
    }

    @Test
    @DisplayName("Rule: aaa new-model extraction")
    void testAaaNewModelRule() {
        String config = "aaa new-model";
        ParserResult result = parser.parse(config);
        assertThat(result.getCanonical().getAuthentication().get("aaa")).isEqualTo(true);
        assertThat(result.getSourceMap()).anyMatch(sm -> sm.getCanonicalField().equals("authentication.aaa") && sm.getSourceLine() == 1);
    }

    @Test
    @DisplayName("Rule: logging syslog and localLogging extraction")
    void testLoggingRules() {
        String config = "logging console informational\nlogging host 192.168.1.10";
        ParserResult result = parser.parse(config);
        assertThat(result.getCanonical().getLogging().get("localLogging")).isEqualTo(true);
        assertThat(result.getCanonical().getLogging().get("syslog")).isEqualTo(true);
        assertThat(result.getSourceMap()).anyMatch(sm -> sm.getCanonicalField().equals("logging.localLogging") && sm.getSourceLine() == 1);
        assertThat(result.getSourceMap()).anyMatch(sm -> sm.getCanonicalField().equals("logging.syslog") && sm.getSourceLine() == 2);
    }

    @Test
    @DisplayName("Rule: ntp server extraction")
    void testNtpServerRule() {
        String config = "ntp server 192.168.1.1 prefer";
        ParserResult result = parser.parse(config);
        assertThat(result.getCanonical().getNtp().get("configured")).isEqualTo(true);
        assertThat(result.getSourceMap()).anyMatch(sm -> sm.getCanonicalField().equals("ntp.configured") && sm.getSourceLine() == 1);
    }

    @Test
    @DisplayName("Malformed/empty/null input fails safely without throwing exceptions")
    void testMalformedInputFailsSafely() {
        // Null test
        ParserResult nullResult = parser.parse(null);
        assertThat(nullResult).isNotNull();
        assertThat(nullResult.getStatus()).isEqualTo("FAILED");
        assertThat(nullResult.getUnknowns()).isNotEmpty();

        // Empty string test
        ParserResult emptyResult = parser.parse("   \n\t  ");
        assertThat(emptyResult).isNotNull();
        assertThat(emptyResult.getStatus()).isEqualTo("FAILED");

        // Corrupted / binary text
        String binaryGarbage = "\u0000\u0001\u0002random corrupted stream\n%%%syntax error###\n";
        ParserResult garbageResult = parser.parse(binaryGarbage);
        assertThat(garbageResult).isNotNull();
        assertThat(garbageResult.getCanonical().getSecurity()).isEmpty();
        assertThat(garbageResult.getUnknowns()).isNotEmpty();
    }
}
