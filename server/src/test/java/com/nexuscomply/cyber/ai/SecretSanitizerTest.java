package com.nexuscomply.cyber.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretSanitizerTest {

    @Test
    @DisplayName("1. SNMP community string is sanitized with trailing options preserved")
    void testSnmpCommunitySanitization() {
        String input = "snmp-server community SECRET123 RO";
        String sanitized = SecretSanitizer.sanitize(input);
        assertThat(sanitized).isEqualTo("snmp-server community <REDACTED> RO");
        assertThat(sanitized).doesNotContain("SECRET123");
    }

    @Test
    @DisplayName("2. User secret with type number and hash is sanitized")
    void testUsernameSecretSanitization() {
        String input = "username admin secret 5 $1$abc";
        String sanitized = SecretSanitizer.sanitize(input);
        assertThat(sanitized).isEqualTo("username admin secret <REDACTED>");
        assertThat(sanitized).doesNotContain("$1$abc");
    }

    @Test
    @DisplayName("3. User secret with privilege level is sanitized")
    void testUsernamePrivilegeSecretSanitization() {
        String input = "username admin privilege 15 secret 5 $1$xyz987";
        String sanitized = SecretSanitizer.sanitize(input);
        assertThat(sanitized).isEqualTo("username admin privilege 15 secret <REDACTED>");
        assertThat(sanitized).doesNotContain("$1$xyz987");
    }

    @Test
    @DisplayName("4. Enable secret and password are sanitized")
    void testEnableSecretSanitization() {
        String input1 = "enable secret 5 $1$supersecret";
        assertThat(SecretSanitizer.sanitize(input1)).isEqualTo("enable secret <REDACTED>");
        assertThat(SecretSanitizer.sanitize(input1)).doesNotContain("supersecret");

        String input2 = "enable password ciscopass";
        assertThat(SecretSanitizer.sanitize(input2)).isEqualTo("enable password <REDACTED>");
        assertThat(SecretSanitizer.sanitize(input2)).doesNotContain("ciscopass");
    }

    @Test
    @DisplayName("5. Pre-shared keys are sanitized")
    void testPreSharedKeySanitization() {
        String input1 = "pre-shared-key ascii MySecretPSK123";
        assertThat(SecretSanitizer.sanitize(input1)).isEqualTo("pre-shared-key <REDACTED>");
        assertThat(SecretSanitizer.sanitize(input1)).doesNotContain("MySecretPSK123");

        String input2 = "psk hex 0123456789abcdef";
        assertThat(SecretSanitizer.sanitize(input2)).isEqualTo("psk <REDACTED>");
        assertThat(SecretSanitizer.sanitize(input2)).doesNotContain("0123456789abcdef");
    }

    @Test
    @DisplayName("6. Generic password and secret statements are sanitized")
    void testGenericPasswordSanitization() {
        String input1 = "password 7 0822455D0A16";
        assertThat(SecretSanitizer.sanitize(input1)).isEqualTo("password <REDACTED>");
        assertThat(SecretSanitizer.sanitize(input1)).doesNotContain("0822455D0A16");

        String input2 = "secret 5 $1$hash";
        assertThat(SecretSanitizer.sanitize(input2)).isEqualTo("secret <REDACTED>");
        assertThat(SecretSanitizer.sanitize(input2)).doesNotContain("hash");
    }

    @Test
    @DisplayName("7. Multi-line configuration block sanitizes each secret line")
    void testMultilineSanitization() {
        String multiline = String.join("\n",
                "hostname RTR-CORE",
                "snmp-server community SECRET123 RO",
                "username admin secret 5 $1$abc",
                "interface GigabitEthernet0/0/0"
        );

        String result = SecretSanitizer.sanitize(multiline);
        assertThat(result).contains("snmp-server community <REDACTED> RO");
        assertThat(result).contains("username admin secret <REDACTED>");
        assertThat(result).doesNotContain("SECRET123");
        assertThat(result).doesNotContain("$1$abc");
        assertThat(result).contains("hostname RTR-CORE");
    }
}
