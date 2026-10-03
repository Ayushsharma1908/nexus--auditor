package com.nexuscomply.cyber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexuscomply.cyber.detection.VendorDetectionResponse;
import com.nexuscomply.cyber.detection.VendorFingerprintDetectionService;
import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import org.junit.jupiter.api.Test;

public class VerificationRunnerTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void runPart2DetectionVerification() throws Exception {
        VendorFingerprintDetectionService service = new VendorFingerprintDetectionService();

        String inputA = String.join("\n",
                "hostname router1",
                "interface GigabitEthernet0/1",
                "ip ssh version 2",
                "line vty 0 4",
                "enable secret 5 xxxxx",
                "spanning-tree mode rapid-pvst"
        );

        String inputB = String.join("\n",
                "set system host-name router1",
                "set interfaces ge-0/0/0 unit 0 family inet address 10.0.0.1/24",
                "set system services ssh protocol-version v2",
                "set routing-options static route 0.0.0.0/0 next-hop 10.0.0.254"
        );

        String inputC = "";

        String inputD = "the quick brown fox jumps over the lazy dog several times a day";

        System.out.println("=== PART 2C: RAW DETECTION OUTPUTS ===");
        System.out.println("--- INPUT A ---");
        VendorDetectionResponse respA = service.detectVendor(inputA);
        System.out.println(mapper.writeValueAsString(respA));

        System.out.println("--- INPUT B ---");
        VendorDetectionResponse respB = service.detectVendor(inputB);
        System.out.println(mapper.writeValueAsString(respB));

        System.out.println("--- INPUT C ---");
        VendorDetectionResponse respC = service.detectVendor(inputC);
        System.out.println(mapper.writeValueAsString(respC));

        System.out.println("--- INPUT D ---");
        VendorDetectionResponse respD = service.detectVendor(inputD);
        System.out.println(mapper.writeValueAsString(respD));
    }

    @Test
    void runPaloAltoDetectionOutputs() throws Exception {
        VendorFingerprintDetectionService service = new VendorFingerprintDetectionService();

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

        String minifiedXml = xmlConfig.replaceAll(">\\s+<", "><").replaceAll("\\r?\\n", "").trim();

        System.out.println("=== PAN SET FIXTURE DETECTION ===");
        System.out.println(mapper.writeValueAsString(service.detectVendor(setConfig)));

        System.out.println("=== PAN XML FIXTURE DETECTION ===");
        System.out.println(mapper.writeValueAsString(service.detectVendor(xmlConfig)));

        System.out.println("=== PAN MINIFIED XML FIXTURE DETECTION ===");
        System.out.println(mapper.writeValueAsString(service.detectVendor(minifiedXml)));
    }

    @Test
    void runPart3CiscoParserVerification() throws Exception {
        CiscoIosParser parser = new CiscoIosParser();
        String config = String.join("\n",
                "hostname CORE-RTR-01",
                "!",
                "ip ssh version 2",
                "ip ssh authentication-retries 3",
                "!",
                "line vty 0 4",
                " transport input ssh",
                "!",
                "ip http secure-server",
                "!",
                "snmp-server community public RO",
                "snmp-server group ADMIN v3 priv",
                "!",
                "aaa new-model",
                "!",
                "logging host 10.0.0.5",
                "logging buffered 16384",
                "!",
                "ntp server 10.0.0.1",
                "!",
                "interface GigabitEthernet0/0",
                " description WAN",
                " ip address 203.0.113.1 255.255.255.0",
                "!"
        );

        ParserResult result = parser.parse(config);
        System.out.println("=== PART 3: RAW PARSER RESULT ===");
        System.out.println(mapper.writeValueAsString(result));
    }

    @Test
    void runPart4MalformedInputVerification() throws Exception {
        CiscoIosParser parser = new CiscoIosParser();
        String malformedConfig = String.join("\n",
                "interface GigabitEthernet",
                " ip address",
                "!!!",
                "ip ssh version",
                "line vty",
                "   this is not valid cisco syntax at all !!! ### $$$"
        );

        ParserResult result = parser.parse(malformedConfig);
        System.out.println("=== PART 4: RAW MALFORMED RESULT ===");
        System.out.println(mapper.writeValueAsString(result));
    }

    @Test
    void runRuleEngineEvaluationVerification() throws Exception {
        CiscoIosParser parser = new CiscoIosParser();
        String config = "version 17.6\nip ssh version 2\nline vty 0 4\n transport input ssh\n";
        ParserResult parserResult = parser.parse(config);

        com.nexuscomply.cyber.compliance.GenericRuleEvaluator evaluator = new com.nexuscomply.cyber.compliance.GenericRuleEvaluator();

        com.nexuscomply.cyber.compliance.model.ComplianceRule passRule = new com.nexuscomply.cyber.compliance.model.ComplianceRule(
                "rule-101", "ctrl-ssh", "fw-1", "CIS-SSH-V2",
                new com.nexuscomply.cyber.compliance.RuleRequirement("security.ssh.version", "EQUALS", 2),
                "HIGH", java.util.List.of("Cisco"), java.util.List.of("IOS-XE"), "ACTIVE", 1
        );

        com.nexuscomply.cyber.compliance.model.ComplianceRule failRule = new com.nexuscomply.cyber.compliance.model.ComplianceRule(
                "rule-102", "ctrl-telnet", "fw-1", "CIS-TELNET-DISABLED",
                new com.nexuscomply.cyber.compliance.RuleRequirement("security.telnet.enabled", "EQUALS", true),
                "HIGH", java.util.List.of("Cisco"), java.util.List.of(), "ACTIVE", 1
        );

        com.nexuscomply.cyber.compliance.model.ComplianceRule absentRule = new com.nexuscomply.cyber.compliance.model.ComplianceRule(
                "rule-103", "ctrl-crypto", "fw-1", "CIS-IKE-V2",
                new com.nexuscomply.cyber.compliance.RuleRequirement("security.crypto.ikeVersion", "EQUALS", 2),
                "MEDIUM", java.util.List.of(), java.util.List.of(), "ACTIVE", 1
        );

        System.out.println("=== COMPLIANCE EVALUATION: RAW RESULTS ===");
        System.out.println("--- PASS EVALUATION ---");
        System.out.println(mapper.writeValueAsString(evaluator.evaluate(parserResult.getCanonical(), passRule)));

        System.out.println("--- FAIL EVALUATION ---");
        System.out.println(mapper.writeValueAsString(evaluator.evaluate(parserResult.getCanonical(), failRule)));

        System.out.println("--- UNKNOWN EVALUATION (ABSENT FIELD) ---");
        System.out.println(mapper.writeValueAsString(evaluator.evaluate(parserResult.getCanonical(), absentRule)));
    }
}
