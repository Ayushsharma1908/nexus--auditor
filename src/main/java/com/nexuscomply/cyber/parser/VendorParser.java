package com.nexuscomply.cyber.parser;

public interface VendorParser {
    ParserResult parse(String rawConfig);
    boolean supports(String vendor, String platform);
}
