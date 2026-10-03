package com.nexuscomply.cyber.parser.paloalto;

import com.nexuscomply.cyber.canonical.CanonicalSecurityModel;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import com.nexuscomply.cyber.detection.VendorDetectionResponse;
import com.nexuscomply.cyber.detection.VendorDetectionStatus;
import com.nexuscomply.cyber.detection.VendorFingerprintDetectionService;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.VendorParser;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import com.nexuscomply.cyber.parser.fortinet.FortinetFortiOSParser;
import com.nexuscomply.cyber.parser.juniper.JuniperJunosParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PaloAltoPanOsParserTest {

    private PaloAltoPanOsParser parser;
    private VendorFingerprintDetectionService detectionService;

    @BeforeEach
    void setUp() {
        parser = new PaloAltoPanOsParser();
        detectionService = new VendorFingerprintDetectionService();
    }

    // =========================================================================
    // PART 2: SUPPORTS CONTRACT MATRIX
    // =========================================================================

    @Test
    @DisplayName("supports() contract matrix: All four parsers x (Cisco, Juniper, Fortinet, Palo Alto, null, blank, unknown)")
    void testSupportsContractMatrix() {
        List<VendorParser> allParsers = List.of(
                new CiscoIosParser(),
                new JuniperJunosParser(),
                new FortinetFortiOSParser(),
                new PaloAltoPanOsParser()
        );

        record TestCase(String vendor, String platform, Class<? extends VendorParser> expectedParser) {}

        List<TestCase> knownPairs = List.of(
                new TestCase("Cisco", "IOS", CiscoIosParser.class),
                new TestCase("Cisco", "IOS-XE", CiscoIosParser.class),
                new TestCase("Juniper", "JUNOS", JuniperJunosParser.class),
                new TestCase("Fortinet", "FortiOS", FortinetFortiOSParser.class),
                new TestCase("Palo Alto", "PAN-OS", PaloAltoPanOsParser.class)
        );

        for (TestCase tc : knownPairs) {
            int claimCount = 0;
            VendorParser claimant = null;
            for (VendorParser p : allParsers) {
                if (p.supports(tc.vendor, tc.platform)) {
                    claimCount++;
                    claimant = p;
                }
            }
            assertThat(claimCount)
                    .as("Exactly one parser must claim pair: vendor=%s, platform=%s", tc.vendor, tc.platform)
                    .isEqualTo(1);
            assertThat(claimant)
                    .as("Claimant for %s/%s must be %s", tc.vendor, tc.platform, tc.expectedParser.getSimpleName())
                    .isInstanceOf(tc.expectedParser);
        }

        // None claim null, blank, or unknown pairs
        List<TestCase> negativePairs = List.of(
                new TestCase(null, null, null),
                new TestCase("", "", null),
                new TestCase("   ", "   ", null),
                new TestCase("UNKNOWN", "UNKNOWN", null),
                new TestCase("UnknownVendor", "UnknownPlatform", null)
        );

        for (TestCase tc : negativePairs) {
            int claimCount = 0;
            for (VendorParser p : allParsers) {
                if (p.supports(tc.vendor, tc.platform)) {
                    claimCount++;
                }
            }
            assertThat(claimCount)
                    .as("No parser should claim invalid/mismatched pair: vendor=%s, platform=%s", tc.vendor, tc.platform)
                    .isEqualTo(0);
        }
    }

    // =========================================================================
    // PART 1 & 4: SAFE ERROR HANDLING & XXE DEFENSE
    // =========================================================================

    @Test
    @DisplayName("Safe error handling: null, empty, or blank input returns FAILED without throwing")
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
    @DisplayName("Safe XML error handling: Malformed XML returns FAILED without throwing")
    void testMalformedXmlDoesNotThrow() {
        String malformedXml = "<config version=\"10.1.0\"><devices><entry name=\"localhost.localdomain\"><unclosed>";
        assertThatCode(() -> {
            ParserResult result = parser.parse(malformedXml);
            assertThat(result.getStatus()).isEqualTo("FAILED");
            assertThat(result.getUnknowns()).isNotEmpty();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("XXE Defense: External entities are not resolved, no network/file access, no exception thrown")
    void testXxeProtection() {
        String xxePayload = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [
                  <!ELEMENT foo ANY >
                  <!ENTITY xxe SYSTEM "file:///etc/passwd" >]>
                <config version="10.1.0">
                  <devices>
                    <entry name="localhost.localdomain">
                      <foo>&xxe;</foo>
                    </entry>
                  </devices>
                </config>
                """;

        assertThatCode(() -> {
            ParserResult result = parser.parse(xxePayload);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("FAILED");
            assertThat(result.getUnknowns()).isNotEmpty();
            assertThat(result.getUnknowns().get(0).getReason()).contains("XML parsing error");
        }).doesNotThrowAnyException();
    }

    // =========================================================================
    // PART 4: FORMAT PARITY (SET vs XML)
    // =========================================================================

    @Test
    @DisplayName("Format Parity: Equivalent set and XML configurations produce identical canonical security model")
    void testFormatParity_EquivalentSetAndXmlProduceIdenticalCanonical() {
        String setConfig = String.join("\n",
                "set deviceconfig system service disable-telnet yes",
                "set network profiles interface-management-profile mgt-profile ssh yes",
                "set network profiles interface-management-profile mgt-profile https yes",
                "set network interface ethernet ethernet1/1 layer3 interface-management-profile mgt-profile",
                "set deviceconfig system snmp-setting access-setting version v2c snmp-community-string public",
                "set shared log-settings syslog SIEM-Profile server SIEM-Collector server 10.10.10.50",
                "set shared log-settings system match-list Forward-System send-syslog SIEM-Profile",
                "set deviceconfig system ntp-servers primary-ntp-server ntp-server-address 10.0.0.1",
                "set shared authentication-profile RADIUS-AUTH method radius",
                "set deviceconfig system authentication-profile RADIUS-AUTH"
        );

        String xmlConfig = """
                <config version="10.1.0" urldb="paloaltonetworks">
                  <devices>
                    <entry name="localhost.localdomain">
                      <deviceconfig>
                        <system>
                          <service>
                            <disable-telnet>yes</disable-telnet>
                          </service>
                          <ntp-servers>
                            <primary-ntp-server>
                              <ntp-server-address>10.0.0.1</ntp-server-address>
                            </primary-ntp-server>
                          </ntp-servers>
                          <authentication-profile>RADIUS-AUTH</authentication-profile>
                          <snmp-setting>
                            <access-setting>
                              <version>
                                <v2c>
                                  <snmp-community-string>public</snmp-community-string>
                                </v2c>
                              </version>
                            </access-setting>
                          </snmp-setting>
                        </system>
                      </deviceconfig>
                      <network>
                        <profiles>
                          <interface-management-profile>
                            <entry name="mgt-profile">
                              <ssh>yes</ssh>
                              <https>yes</https>
                            </entry>
                          </interface-management-profile>
                        </profiles>
                        <interface>
                          <ethernet>
                            <entry name="ethernet1/1">
                              <layer3>
                                <interface-management-profile>mgt-profile</interface-management-profile>
                              </layer3>
                            </entry>
                          </ethernet>
                        </interface>
                      </network>
                    </entry>
                  </devices>
                  <shared>
                    <log-settings>
                      <syslog>
                        <entry name="SIEM-Profile">
                          <server>
                            <entry name="SIEM-Collector">
                              <server>10.10.10.50</server>
                            </entry>
                          </server>
                        </entry>
                      </syslog>
                      <system>
                        <match-list>
                          <entry name="Forward-System">
                            <send-syslog>
                              <member>SIEM-Profile</member>
                            </send-syslog>
                          </entry>
                        </match-list>
                      </system>
                    </log-settings>
                    <authentication-profile>
                      <entry name="RADIUS-AUTH">
                        <method>
                          <radius/>
                        </method>
                      </entry>
                    </authentication-profile>
                  </shared>
                </config>
                """;

        ParserResult setResult = parser.parse(setConfig);
        ParserResult xmlResult = parser.parse(xmlConfig);

        assertThat(setResult.getStatus()).isEqualTo("COMPLETED");
        assertThat(xmlResult.getStatus()).isEqualTo("COMPLETED");

        CanonicalSecurityModel setModel = setResult.getCanonical();
        CanonicalSecurityModel xmlModel = xmlResult.getCanonical();

        // Compare canonical models field by field
        assertThat(setModel.getSecurity().get("ssh")).isEqualTo(xmlModel.getSecurity().get("ssh"));
        assertThat(setModel.getSecurity().get("telnet")).isEqualTo(xmlModel.getSecurity().get("telnet"));
        assertThat(setModel.getSecurity().get("https")).isEqualTo(xmlModel.getSecurity().get("https"));
        assertThat(setModel.getSecurity().get("snmp")).isEqualTo(xmlModel.getSecurity().get("snmp"));
        assertThat(setModel.getLogging().get("syslog")).isEqualTo(xmlModel.getLogging().get("syslog"));
        assertThat(setModel.getLogging().get("localLogging")).isEqualTo(xmlModel.getLogging().get("localLogging"));
        assertThat(setModel.getNtp().get("configured")).isEqualTo(xmlModel.getNtp().get("configured"));
        assertThat(setModel.getAuthentication().get("aaa")).isEqualTo(xmlModel.getAuthentication().get("aaa"));

        // Exact values verified
        Map<String, Object> ssh = (Map<String, Object>) setModel.getSecurity().get("ssh");
        assertThat(ssh.get("enabled")).isEqualTo(true);
        assertThat(ssh.get("version")).isNull();

        Map<String, Object> telnet = (Map<String, Object>) setModel.getSecurity().get("telnet");
        assertThat(telnet.get("enabled")).isEqualTo(false);

        Map<String, Object> https = (Map<String, Object>) setModel.getSecurity().get("https");
        assertThat(https.get("enabled")).isEqualTo(true);

        Map<String, Object> snmp = (Map<String, Object>) setModel.getSecurity().get("snmp");
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isNull();

        assertThat(setModel.getLogging().get("syslog")).isEqualTo(true);
        assertThat(setModel.getLogging().get("localLogging")).isNull();
        assertThat(setModel.getNtp().get("configured")).isEqualTo(true);
        assertThat(setModel.getAuthentication().get("aaa")).isEqualTo(true);
    }

    // =========================================================================
    // PART 3: EXTRACTION RULES TESTS
    // =========================================================================

    @Test
    @DisplayName("Rule: Attached profile enables SSH and HTTPS (order-independent two-pass)")
    void testAttachedProfileEnablesSshAndHttps() {
        // Attachment comes before profile definition in CLI
        String config = String.join("\n",
                "set network interface ethernet ethernet1/2 layer3 interface-management-profile sec-profile",
                "set network profiles interface-management-profile sec-profile ssh yes",
                "set network profiles interface-management-profile sec-profile https yes"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel model = result.getCanonical();
        Map<String, Object> ssh = (Map<String, Object>) model.getSecurity().get("ssh");
        assertThat(ssh).isNotNull();
        assertThat(ssh.get("enabled")).isEqualTo(true);
        assertThat(ssh.get("version")).isNull();

        Map<String, Object> https = (Map<String, Object>) model.getSecurity().get("https");
        assertThat(https).isNotNull();
        assertThat(https.get("enabled")).isEqualTo(true);
    }

    @Test
    @DisplayName("Rule: Defined-but-unattached profile leaves services UNSET")
    void testDefinedButUnattachedProfileLeavesServicesUnset() {
        String config = String.join("\n",
                "set network profiles interface-management-profile unattached-prof ssh yes",
                "set network profiles interface-management-profile unattached-prof telnet yes",
                "set network profiles interface-management-profile unattached-prof https yes"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel model = result.getCanonical();
        assertThat(model.getSecurity().get("ssh")).isNull();
        assertThat(model.getSecurity().get("telnet")).isNull();
        assertThat(model.getSecurity().get("https")).isNull();
    }

    @Test
    @DisplayName("Telnet Asymmetry: Affirmative disable statement produces false")
    void testTelnetAsymmetry_AffirmativeDisableProducesFalse() {
        String config = "set deviceconfig system service disable-telnet yes";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        Map<String, Object> telnet = (Map<String, Object>) result.getCanonical().getSecurity().get("telnet");
        assertThat(telnet).isNotNull();
        assertThat(telnet.get("enabled")).isEqualTo(false);
    }

    @Test
    @DisplayName("Telnet Asymmetry: Absence of any telnet statement leaves telnet UNSET, not false")
    void testTelnetAsymmetry_AbsenceLeavesTelnetUnsetNotFalse() {
        String config = String.join("\n",
                "set deviceconfig system ntp-servers primary-ntp-server ntp-server-address 1.1.1.1",
                "set network profiles interface-management-profile p1 ssh yes"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getSecurity().get("telnet")).isNull();
    }

    @Test
    @DisplayName("Telnet Asymmetry: Attached profile enabling telnet overrides disable-telnet yes")
    void testTelnetAsymmetry_AttachedProfileOverridesAffirmativeDisable() {
        String config = String.join("\n",
                "set deviceconfig system service disable-telnet yes",
                "set network profiles interface-management-profile p1 telnet yes",
                "set network interface ethernet ethernet1/1 layer3 interface-management-profile p1"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        Map<String, Object> telnet = (Map<String, Object>) result.getCanonical().getSecurity().get("telnet");
        assertThat(telnet).isNotNull();
        assertThat(telnet.get("enabled")).isEqualTo(true);
    }

    @Test
    @DisplayName("SSH Version: Explicit protocol-version extracts version number")
    void testSshVersionExplicit() {
        String config = String.join("\n",
                "set network profiles interface-management-profile p1 ssh yes",
                "set network interface ethernet ethernet1/1 layer3 interface-management-profile p1",
                "set deviceconfig system ssh-service version 2"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        Map<String, Object> ssh = (Map<String, Object>) result.getCanonical().getSecurity().get("ssh");
        assertThat(ssh).isNotNull();
        assertThat(ssh.get("enabled")).isEqualTo(true);
        assertThat(ssh.get("version")).isEqualTo(2);

        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .contains("security.ssh.enabled", "security.ssh.version");
    }

    @Test
    @DisplayName("SNMP: v2c community sets enabled=true and version=null")
    void testSnmp_V2cCommunityLeavesVersionUnset() {
        String config = "set deviceconfig system snmp-setting access-setting version v2c snmp-community-string monitor";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        Map<String, Object> snmp = (Map<String, Object>) result.getCanonical().getSecurity().get("snmp");
        assertThat(snmp).isNotNull();
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isNull();
    }

    @Test
    @DisplayName("SNMP: v3 user sets enabled=true and version='3'")
    void testSnmp_V3UserSetsVersion3() {
        String config = "set deviceconfig system snmp-setting access-setting version v3 users admin authpwd secret privpwd secret";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        Map<String, Object> snmp = (Map<String, Object>) result.getCanonical().getSecurity().get("snmp");
        assertThat(snmp).isNotNull();
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isEqualTo("3");

        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .contains("security.snmp.enabled", "security.snmp.version");
    }

    @Test
    @DisplayName("SNMP: Coexistence of community and v3 user leaves version UNSET (null)")
    void testSnmp_CommunityAndV3CoexistenceLeavesVersionUnset() {
        String config = String.join("\n",
                "set deviceconfig system snmp-setting access-setting version v2c snmp-community-string read-only",
                "set deviceconfig system snmp-setting access-setting version v3 users secuser authpwd secret"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        Map<String, Object> snmp = (Map<String, Object>) result.getCanonical().getSecurity().get("snmp");
        assertThat(snmp).isNotNull();
        assertThat(snmp.get("enabled")).isEqualTo(true);
        assertThat(snmp.get("version")).isNull();
    }

    @Test
    @DisplayName("Syslog: Profile with >= 1 server AND referenced from log settings sets logging.syslog=true")
    void testSyslog_ReferencedProfileWithServerSetsSyslogTrue() {
        String config = String.join("\n",
                "set shared log-settings syslog Central-Syslog server Splunk-Collector server 10.20.30.40",
                "set shared log-settings config match-list Config-Audit send-syslog Central-Syslog"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getLogging().get("syslog")).isEqualTo(true);
        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField).contains("logging.syslog");
    }

    @Test
    @DisplayName("Syslog: Unreferenced profile leaves logging.syslog UNSET")
    void testSyslog_UnreferencedProfileLeavesSyslogUnset() {
        String config = "set shared log-settings syslog Unused-Profile server Local-Collector server 10.0.0.99";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getLogging().get("syslog")).isNull();
    }

    @Test
    @DisplayName("Syslog: Referenced profile without configured servers leaves logging.syslog UNSET")
    void testSyslog_ReferencedProfileWithoutServersLeavesSyslogUnset() {
        String config = "set shared log-settings system match-list Forward-All send-syslog Empty-Profile";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getLogging().get("syslog")).isNull();
    }

    @Test
    @DisplayName("Local Logging: Deliberately left UNSET per platform rule")
    void testLocalLoggingRemainsUnset() {
        String config = String.join("\n",
                "set shared log-settings syslog S1 server Srv1 server 10.1.1.1",
                "set shared log-settings system match-list M1 send-syslog S1"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getLogging().get("localLogging")).isNull();
    }

    @Test
    @DisplayName("NTP: Configured NTP server address sets ntp.configured=true")
    void testNtpConfigured() {
        String config = "set deviceconfig system ntp-servers secondary-ntp-server ntp-server-address pool.ntp.org";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getNtp().get("configured")).isEqualTo(true);
        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField).contains("ntp.configured");
    }

    @Test
    @DisplayName("AAA: External auth profile referenced for administrator auth sets authentication.aaa=true")
    void testAaa_ExternalProfileUsedForAdminSetsAaaTrue() {
        String config = String.join("\n",
                "set shared authentication-profile TACACS-ADMIN method tacplus",
                "set mgt-config users secadmin authentication-profile TACACS-ADMIN"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getAuthentication().get("aaa")).isEqualTo(true);
        assertThat(result.getSourceMap()).extracting(SourceMapEntry::getCanonicalField).contains("authentication.aaa");
    }

    @Test
    @DisplayName("AAA: Local-only admin accounts leave authentication.aaa UNSET")
    void testAaa_LocalOnlyAdminsLeavesAaaUnset() {
        String config = String.join("\n",
                "set mgt-config users admin phash $1$secret12345",
                "set mgt-config users admin permissions role-based superuser yes"
        );
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getAuthentication().get("aaa")).isNull();
    }

    @Test
    @DisplayName("AAA: External profile defined but not used for admin auth leaves authentication.aaa UNSET")
    void testAaa_ExternalProfileNotUsedForAdminLeavesAaaUnset() {
        String config = "set shared authentication-profile VPN-RADIUS method radius";
        ParserResult result = parser.parse(config);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCanonical().getAuthentication().get("aaa")).isNull();
    }

    @Test
    @DisplayName("Context Isolation: Same keywords under different contexts are isolated")
    void testContextIsolation() {
        // XML config where 'server' appears under dns-setting and 'ssh' appears in an unattached profile
        String xml = """
                <config version="10.1.0" urldb="paloaltonetworks">
                  <devices>
                    <entry name="localhost.localdomain">
                      <deviceconfig>
                        <system>
                          <dns-setting>
                            <servers>
                              <primary>8.8.8.8</primary>
                            </servers>
                          </dns-setting>
                        </system>
                      </deviceconfig>
                      <network>
                        <profiles>
                          <interface-management-profile>
                            <entry name="unattached-profile">
                              <ssh>yes</ssh>
                              <telnet>yes</telnet>
                            </entry>
                          </interface-management-profile>
                        </profiles>
                      </network>
                    </entry>
                  </devices>
                </config>
                """;
        ParserResult result = parser.parse(xml);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        CanonicalSecurityModel model = result.getCanonical();
        assertThat(model.getSecurity().get("ssh")).isNull();
        assertThat(model.getSecurity().get("telnet")).isNull();
        assertThat(model.getLogging().get("syslog")).isNull();
        assertThat(model.getNtp().get("configured")).isNull();
        assertThat(model.getAuthentication().get("aaa")).isNull();
    }

    // =========================================================================
    // DETECTION TESTS & CROSS-VENDOR NEGATIVES
    // =========================================================================

    @Test
    @DisplayName("Detection: Both set and XML formats score >= 0.70 for Palo Alto")
    void testDetection_PanOsScoresAboveThreshold() {
        String setConfig = String.join("\n",
                "set deviceconfig system service disable-telnet yes",
                "set network profiles interface-management-profile mgt-prof ssh yes",
                "set network interface ethernet ethernet1/1 layer3 interface-management-profile mgt-prof",
                "set zone trust network layer3 ethernet1/1",
                "set shared log-settings syslog SIEM server S1 server 10.0.0.1"
        );

        String xmlConfig = """
                <config version="10.1.0" urldb="paloaltonetworks">
                  <devices>
                    <entry name="localhost.localdomain">
                      <deviceconfig>
                        <system>
                          <service>
                            <disable-telnet>yes</disable-telnet>
                          </service>
                        </system>
                      </deviceconfig>
                      <network>
                        <profiles>
                          <interface-management-profile>
                            <entry name="mgt-prof">
                              <ssh>yes</ssh>
                            </entry>
                          </interface-management-profile>
                        </profiles>
                      </network>
                    </entry>
                  </devices>
                </config>
                """;

        VendorDetectionResponse setResp = detectionService.detectVendor(setConfig);
        assertThat(setResp.getVendor()).isEqualTo("Palo Alto");
        assertThat(setResp.getPlatform()).isEqualTo("PAN-OS");
        assertThat(setResp.getConfidence()).isGreaterThanOrEqualTo(0.70);
        assertThat(setResp.getStatus()).isEqualTo(VendorDetectionStatus.DETECTED);

        VendorDetectionResponse xmlResp = detectionService.detectVendor(xmlConfig);
        assertThat(xmlResp.getVendor()).isEqualTo("Palo Alto");
        assertThat(xmlResp.getPlatform()).isEqualTo("PAN-OS");
        assertThat(xmlResp.getConfidence()).isGreaterThanOrEqualTo(0.70);
        assertThat(xmlResp.getStatus()).isEqualTo(VendorDetectionStatus.DETECTED);
    }

    @Test
    @DisplayName("Detection Cross-Vendor Negatives: Cisco, Juniper, Fortinet fixtures do not score Palo Alto >= 0.70")
    void testDetection_CrossVendorNegative() {
        String ciscoFixture = String.join("\n",
                "version 17.6",
                "hostname CORE-ROUTER",
                "ip ssh version 2",
                "line vty 0 4",
                " transport input ssh",
                "interface GigabitEthernet0/0",
                " aaa new-model"
        );

        String juniperFixture = String.join("\n",
                "set system services ssh protocol-version v2",
                "set interfaces ge-0/0/0 unit 0 family inet address 10.0.0.1/24",
                "set protocols bgp group internal type internal",
                "set system syslog host 10.0.0.5 any any"
        );

        String fortinetFixture = String.join("\n",
                "config system interface",
                "    edit port1",
                "        set allowaccess ssh https",
                "    next",
                "end",
                "config log syslogd setting",
                "    set status enable",
                "    set server 10.0.0.10",
                "end"
        );

        VendorDetectionResponse ciscoResp = detectionService.detectVendor(ciscoFixture);
        assertThat(ciscoResp.getVendor()).isEqualTo("Cisco");
        assertThat(ciscoResp.getPlatform()).isEqualTo("IOS-XE");

        VendorDetectionResponse juniperResp = detectionService.detectVendor(juniperFixture);
        assertThat(juniperResp.getVendor()).isEqualTo("Juniper");
        assertThat(juniperResp.getPlatform()).isEqualTo("JUNOS");

        VendorDetectionResponse fortinetResp = detectionService.detectVendor(fortinetFixture);
        assertThat(fortinetResp.getVendor()).isEqualTo("Fortinet");
        assertThat(fortinetResp.getPlatform()).isEqualTo("FortiOS");
    }

    @Test
    @DisplayName("Detection: Minified single-line XML export scores >= 0.70")
    void testDetection_MinifiedSingleLineXml() {
        String xmlConfig = """
                <config version="10.1.0" urldb="paloaltonetworks">
                  <devices>
                    <entry name="localhost.localdomain">
                      <deviceconfig>
                        <system>
                          <service>
                            <disable-telnet>yes</disable-telnet>
                          </service>
                        </system>
                      </deviceconfig>
                      <network>
                        <profiles>
                          <interface-management-profile>
                            <entry name="mgt-prof">
                              <ssh>yes</ssh>
                            </entry>
                          </interface-management-profile>
                        </profiles>
                      </network>
                    </entry>
                  </devices>
                </config>
                """;
        String minifiedXml = xmlConfig.replaceAll(">\\s+<", "><").replaceAll("\\r?\\n", "").trim();

        VendorDetectionResponse resp = detectionService.detectVendor(minifiedXml);
        assertThat(resp.getVendor()).isEqualTo("Palo Alto");
        assertThat(resp.getPlatform()).isEqualTo("PAN-OS");
        assertThat(resp.getConfidence()).isGreaterThanOrEqualTo(0.70);
        assertThat(resp.getStatus()).isEqualTo(VendorDetectionStatus.DETECTED);
    }

    @Test
    @DisplayName("Tie-break: A tie between vendors produces UNCERTAIN, not the first vendor")
    void testTieBreak_ProducesUncertain() {
        // Equal matches for Cisco (2 matches: hostname, aaa new-model) and Juniper (2 matches: set protocols, apply-groups)
        String tiedConfig = String.join("\n",
                "hostname router1",
                "aaa new-model",
                "set protocols bgp",
                "apply-groups group1"
        );

        VendorDetectionResponse resp = detectionService.detectVendor(tiedConfig);
        assertThat(resp.getStatus()).isEqualTo(VendorDetectionStatus.UNCERTAIN);
        assertThat(resp.getVendor()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Brace format: PAN-OS brace format returns UNKNOWN vendor and visible parse failure")
    void testBraceFormat_ReturnsUnknownVendorAndVisibleParseFailure() {
        String braceConfig = """
                deviceconfig {
                    system {
                        service {
                            disable-telnet yes;
                            disable-ssh no;
                        }
                    }
                }
                """;

        VendorDetectionResponse detResp = detectionService.detectVendor(braceConfig);
        assertThat(detResp.getStatus()).isEqualTo(VendorDetectionStatus.UNKNOWN);
        assertThat(detResp.getVendor()).isEqualTo("UNKNOWN");

        ParserResult parseResult = parser.parse(braceConfig);
        assertThat(parseResult.getStatus()).isEqualTo("FAILED");
        assertThat(parseResult.getUnknowns()).isNotEmpty();
        assertThat(parseResult.getUnknowns().get(0).getReason()).contains("Hierarchical brace-format PAN-OS configuration is not supported");
    }

    @Test
    @DisplayName("HTTPS: disable-https yes with no enabling profile produces security.https.enabled = false")
    void testHttpsExplicitDisable_SetsHttpsFalse() {
        String setConfig = "set deviceconfig system service disable-https yes\n";
        ParserResult setResult = parser.parse(setConfig);

        assertThat(setResult.getStatus()).isEqualTo("COMPLETED");
        Map<String, Object> setHttps = (Map<String, Object>) setResult.getCanonical().getSecurity().get("https");
        assertThat(setHttps).isNotNull();
        assertThat(setHttps.get("enabled")).isEqualTo(false);
        assertThat(setResult.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .contains("security.https.enabled");

        String xmlConfig = """
                <config version="10.1.0">
                  <devices>
                    <entry name="localhost.localdomain">
                      <deviceconfig>
                        <system>
                          <service>
                            <disable-https>yes</disable-https>
                          </service>
                        </system>
                      </deviceconfig>
                    </entry>
                  </devices>
                </config>
                """;
        ParserResult xmlResult = parser.parse(xmlConfig);

        assertThat(xmlResult.getStatus()).isEqualTo("COMPLETED");
        Map<String, Object> xmlHttps = (Map<String, Object>) xmlResult.getCanonical().getSecurity().get("https");
        assertThat(xmlHttps).isNotNull();
        assertThat(xmlHttps.get("enabled")).isEqualTo(false);
        assertThat(xmlResult.getSourceMap()).extracting(SourceMapEntry::getCanonicalField)
                .contains("security.https.enabled");
    }
}
