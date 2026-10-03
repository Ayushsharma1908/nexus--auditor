package com.nexuscomply.cyber.parser;

import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParserServiceTest {

    private ParserService parserService;

    @BeforeEach
    void setUp() {
        VendorParser ciscoParser = new CiscoIosParser();
        parserService = new ParserServiceImpl(List.of(ciscoParser));
    }

    @Test
    @DisplayName("Routes correctly to Cisco parser when vendor is Cisco")
    void testRoutesToCiscoParser() {
        String config = "ip ssh version 2";
        ParserResult result = parserService.parse(config, "Cisco", "IOS-XE");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSourceMap()).anyMatch(sm -> sm.getCanonicalField().equals("security.ssh.version"));
    }

    @Test
    @DisplayName("Returns safe FAILED result when vendor is unsupported")
    void testUnsupportedVendorFailsSafely() {
        String config = "set system services ssh";
        ParserResult result = parserService.parse(config, "Juniper", "JUNOS");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getUnknowns()).anyMatch(u -> u.getReason().contains("No registered parser found"));
    }

    @Test
    @DisplayName("Returns safe FAILED result when rawConfig is null or empty")
    void testEmptyConfigFailsSafely() {
        ParserResult result = parserService.parse("", "Cisco", "IOS-XE");
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }
}
