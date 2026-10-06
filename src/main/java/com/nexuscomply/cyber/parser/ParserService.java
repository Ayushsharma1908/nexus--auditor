package com.nexuscomply.cyber.parser;

public interface ParserService {
    default ParserResult parse(String rawConfig, String vendor, String platform) {
        return parse(rawConfig, vendor, platform, false);
    }

    ParserResult parse(String rawConfig, String vendor, String platform, boolean readOnly);
}
