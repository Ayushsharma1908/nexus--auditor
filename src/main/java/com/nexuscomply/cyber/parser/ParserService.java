package com.nexuscomply.cyber.parser;

public interface ParserService {
    ParserResult parse(String rawConfig, String vendor, String platform);
}
