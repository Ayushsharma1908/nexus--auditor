package com.nexuscomply.cyber.parser;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParserServiceImpl implements ParserService {

    private final List<VendorParser> vendorParsers;

    public ParserServiceImpl(List<VendorParser> vendorParsers) {
        this.vendorParsers = vendorParsers;
    }

    @Override
    public ParserResult parse(String rawConfig, String vendor, String platform) {
        if (rawConfig == null || rawConfig.trim().isEmpty()) {
            ParserResult emptyResult = new ParserResult();
            emptyResult.setStatus("FAILED");
            emptyResult.getUnknowns().add(new UnknownConstruct("", 0, "Configuration content is empty or null"));
            return emptyResult;
        }

        for (VendorParser parser : vendorParsers) {
            if (parser.supports(vendor, platform)) {
                return parser.parse(rawConfig);
            }
        }

        ParserResult unsupportedResult = new ParserResult();
        unsupportedResult.setStatus("FAILED");
        unsupportedResult.getUnknowns().add(new UnknownConstruct(
                "Vendor: " + vendor + ", Platform: " + platform,
                0,
                "No registered parser found for vendor [" + vendor + "] and platform [" + platform + "]"
        ));
        return unsupportedResult;
    }
}
