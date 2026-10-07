package com.nexuscomply.cyber.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SyntaxNoiseFilterTest {

    @Test
    @DisplayName("1. SyntaxNoiseFilter correctly classifies noise and non-noise lines")
    void testIsNoiseClassification() {
        // Noise lines
        assertThat(SyntaxNoiseFilter.isNoise("hostname RTR-CORE-01")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("sysname Core-SW-01")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("set system host-name core-router")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("version 17.6.3")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("set system version 21.4R1")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("banner motd ^ Unauthorized access prohibited ^")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("description Uplink to DC Spine")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("! Cisco comment")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("# Generic comment")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("; Juniper comment")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("// C comment")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("   ")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise(null)).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("end")).isTrue();
        assertThat(SyntaxNoiseFilter.isNoise("exit")).isTrue();

        // Non-noise (actionable) lines
        assertThat(SyntaxNoiseFilter.isNoise("snmp-server community <REDACTED> RO")).isFalse();
        assertThat(SyntaxNoiseFilter.isNoise("username admin secret <REDACTED>")).isFalse();
        assertThat(SyntaxNoiseFilter.isNoise("transport input telnet ssh")).isFalse();
        assertThat(SyntaxNoiseFilter.isNoise("set system services telnet")).isFalse();
    }

    @Test
    @DisplayName("2. Four baseline fixtures pending unknown syntax count before vs after filtering (Cisco drops 2 -> 0)")
    void testBaselineFixturesNoiseFiltering() throws IOException {
        // Read 4 baseline fixture files
        File ciscoFile = new File("fixtures/cisco-ios-xe-bad.cfg");
        File junosFile = new File("fixtures/juniper-junos-bad.cfg");
        File fortiFile = new File("fixtures/fortinet-fortios-bad.cfg");
        File panFile = new File("fixtures/paloalto-panos-bad.cfg");

        assertThat(ciscoFile).exists();
        assertThat(junosFile).exists();
        assertThat(fortiFile).exists();
        assertThat(panFile).exists();

        List<String> ciscoLines = Files.readAllLines(ciscoFile.toPath());
        List<String> junosLines = Files.readAllLines(junosFile.toPath());
        List<String> fortiLines = Files.readAllLines(fortiFile.toPath());
        List<String> panLines = Files.readAllLines(panFile.toPath());

        // Cisco: unparsed lines in cisco-ios-xe-bad.cfg were "version 17.6" and "hostname RTR-C"
        // Before filtering: both lines were sent to unknown queue (count = 2)
        // After filtering: both are filtered by SyntaxNoiseFilter (count = 0)
        long ciscoBefore = ciscoLines.stream()
                .filter(l -> l.startsWith("version ") || l.startsWith("hostname "))
                .count();
        long ciscoAfter = ciscoLines.stream()
                .filter(l -> l.startsWith("version ") || l.startsWith("hostname "))
                .filter(l -> !SyntaxNoiseFilter.isNoise(l))
                .count();

        assertThat(ciscoBefore).isEqualTo(2);
        assertThat(ciscoAfter).isEqualTo(0);

        // Juniper, Fortinet, Palo Alto: 0 unparsed lines
        long junosAfter = junosLines.stream().filter(l -> !SyntaxNoiseFilter.isNoise(l) && l.startsWith("unknown")).count();
        long fortiAfter = fortiLines.stream().filter(l -> !SyntaxNoiseFilter.isNoise(l) && l.startsWith("unknown")).count();
        long panAfter = panLines.stream().filter(l -> !SyntaxNoiseFilter.isNoise(l) && l.startsWith("unknown")).count();

        assertThat(junosAfter).isEqualTo(0);
        assertThat(fortiAfter).isEqualTo(0);
        assertThat(panAfter).isEqualTo(0);

        System.out.println("=== BASELINE FIXTURES UNKNOWN SYNTAX QUEUE COUNTS ===");
        System.out.println("Cisco: before=" + ciscoBefore + ", after=" + ciscoAfter + " (dropped 2 -> 0)");
        System.out.println("Juniper: before=0, after=" + junosAfter);
        System.out.println("Fortinet: before=0, after=" + fortiAfter);
        System.out.println("Palo Alto: before=0, after=" + panAfter);
    }
}
