package com.nexuscomply.cyber.normalization;

import com.nexuscomply.cyber.parser.ParserResult;
import com.nexuscomply.cyber.parser.ParserService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service("cyberNormalizationService")
public class NormalizationServiceImpl implements NormalizationService {

    private final ParserService parserService;
    private final NormalizedConfigurationRepository repository;

    public NormalizationServiceImpl(ParserService parserService, NormalizedConfigurationRepository repository) {
        this.parserService = parserService;
        this.repository = repository;
    }

    @Override
    public NormalizedConfigurationDocument normalizeAndPersist(
            String rawConfig,
            String deviceId,
            String configurationId,
            String versionId,
            String vendor,
            String platform,
            String osVersion
    ) {
        ParserResult parserResult = parserService.parse(rawConfig, vendor, platform);

        NormalizedConfigurationDocument doc = new NormalizedConfigurationDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setDeviceId(deviceId);
        doc.setConfigurationId(configurationId);
        doc.setVersionId(versionId);
        doc.setVendor(vendor);
        doc.setPlatform(platform);
        doc.setOsVersion(osVersion);
        doc.setSchemaVersion("1.0");

        doc.setCanonical(parserResult.getCanonical());
        doc.setSourceMap(parserResult.getSourceMap());
        doc.setUnknowns(parserResult.getUnknowns());
        doc.setNormalizationStatus(parserResult.getStatus());

        Instant now = Instant.now();
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        return repository.save(doc);
    }

    @Override
    public Optional<NormalizedConfigurationDocument> getById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<NormalizedConfigurationDocument> getByVersionId(String versionId) {
        return repository.findByVersionId(versionId);
    }
}
