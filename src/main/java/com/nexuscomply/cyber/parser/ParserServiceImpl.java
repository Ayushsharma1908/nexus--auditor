package com.nexuscomply.cyber.parser;

import com.nexuscomply.cyber.ai.persistence.AiMappingDocument;
import com.nexuscomply.cyber.ai.persistence.AiMappingRepository;
import com.nexuscomply.cyber.ai.service.CanonicalFieldAllowlist;
import com.nexuscomply.cyber.canonical.SourceMapEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ParserServiceImpl implements ParserService {

    private final List<VendorParser> vendorParsers;
    private final AiMappingRepository aiMappingRepository;

    @Autowired
    public ParserServiceImpl(
            List<VendorParser> vendorParsers,
            @Autowired(required = false) AiMappingRepository aiMappingRepository) {
        this.vendorParsers = vendorParsers;
        this.aiMappingRepository = aiMappingRepository;
    }

    public ParserServiceImpl(List<VendorParser> vendorParsers) {
        this(vendorParsers, null);
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
                ParserResult result = parser.parse(rawConfig);
                applyApprovedMappings(result, vendor, platform);
                return result;
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

    private void applyApprovedMappings(ParserResult result, String vendor, String platform) {
        if (aiMappingRepository == null || result == null || result.getUnknowns() == null || result.getUnknowns().isEmpty()) {
            return;
        }

        List<UnknownConstruct> remainingUnknowns = new ArrayList<>();
        for (UnknownConstruct unknown : result.getUnknowns()) {
            if (unknown != null && unknown.getRawText() != null && !unknown.getRawText().isBlank()) {
                Optional<AiMappingDocument> mappingOpt = aiMappingRepository.findByVendorAndPlatformAndRawSyntax(
                        vendor, platform, unknown.getRawText().trim()
                );
                if (mappingOpt.isPresent() && "APPROVED".equalsIgnoreCase(mappingOpt.get().getStatus())) {
                    AiMappingDocument mapping = mappingOpt.get();
                    if (mapping.getCanonicalField() != null && mapping.getMappedValue() != null) {
                        // Apply to canonical model
                        CanonicalFieldAllowlist.applyToCanonical(
                                result.getCanonical(),
                                mapping.getCanonicalField(),
                                mapping.getMappedValue()
                        );

                        // Record source map entry with APPROVED_MAPPING source type
                        result.getSourceMap().add(new SourceMapEntry(
                                mapping.getCanonicalField(),
                                unknown.getSourceLine(),
                                unknown.getRawText(),
                                "APPROVED_MAPPING"
                        ));

                        // Track usage count
                        mapping.setUsageCount(mapping.getUsageCount() + 1);
                        aiMappingRepository.save(mapping);
                        continue;
                    }
                }
            }
            remainingUnknowns.add(unknown);
        }
        result.setUnknowns(remainingUnknowns);
    }
}
