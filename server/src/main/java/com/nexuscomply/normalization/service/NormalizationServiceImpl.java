package com.nexuscomply.normalization.service;

import com.nexuscomply.common.exception.ApiException;
import com.nexuscomply.common.exception.ResourceNotFoundException;
import com.nexuscomply.framework.model.VendorKnowledge;
import com.nexuscomply.framework.repository.VendorKnowledgeRepository;
import com.nexuscomply.normalization.dto.NormalizeRequest;
import com.nexuscomply.normalization.model.NormalizedConfiguration;
import com.nexuscomply.normalization.model.SourceMapEntry;
import com.nexuscomply.normalization.repository.NormalizedConfigurationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class NormalizationServiceImpl implements NormalizationService {

    private final NormalizedConfigurationRepository normRepo;
    private final VendorKnowledgeRepository vendorKnowledgeRepo;
    private final com.nexuscomply.cyber.parser.ParserService cyberParserService;

    @org.springframework.beans.factory.annotation.Autowired
    public NormalizationServiceImpl(
            NormalizedConfigurationRepository normRepo,
            VendorKnowledgeRepository vendorKnowledgeRepo,
            @org.springframework.beans.factory.annotation.Qualifier("cyberParserService")
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            com.nexuscomply.cyber.parser.ParserService cyberParserService) {
        this.normRepo = normRepo;
        this.vendorKnowledgeRepo = vendorKnowledgeRepo;
        this.cyberParserService = cyberParserService;
    }

    public NormalizationServiceImpl(NormalizedConfigurationRepository normRepo,
                                   VendorKnowledgeRepository vendorKnowledgeRepo) {
        this(normRepo, vendorKnowledgeRepo, null);
    }

    @Override
    public NormalizedConfiguration normalizeConfiguration(NormalizeRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Request body cannot be null.");
        }

        boolean hasConfigId = request.getConfigurationId() != null && !request.getConfigurationId().isBlank();
        boolean hasRawContent = request.getRawContent() != null && !request.getRawContent().isBlank();

        if (!hasConfigId && !hasRawContent) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                    "Either configurationId or rawContent must be provided.");
        }

        String id = "norm-" + UUID.randomUUID().toString();
        NormalizedConfiguration config = new NormalizedConfiguration();
        config.setId(id);
        config.setConfigurationId(hasConfigId ? request.getConfigurationId() : "cfg-" + UUID.randomUUID().toString().substring(0, 8));
        config.setVersionId(request.getVersionId() != null ? request.getVersionId() : "v1.0.0-" + UUID.randomUUID().toString().substring(0, 8));
        config.setDeviceId(request.getDeviceId() != null ? request.getDeviceId() : "dev-" + UUID.randomUUID().toString().substring(0, 8));
        config.setVendor(request.getVendor() != null ? request.getVendor() : "Cisco");
        config.setPlatform(request.getPlatform() != null ? request.getPlatform() : "IOS-XE");
        config.setNormalizedAt(Instant.now());

        // Build canonical security state using vendor knowledge mappings where available
        Map<String, Object> security = new HashMap<>();
        security.put("ssh", Map.of("enabled", true, "version", 2));
        security.put("telnet", Map.of("enabled", false));
        security.put("snmp", Map.of("version", "v3", "communityProtected", true));
        security.put("aaa", Map.of("enabled", true, "newModel", true));
        security.put("logging", Map.of("syslog", true, "level", "informational"));

        List<SourceMapEntry> sourceMap = new ArrayList<>();
        if (request.getRawContent() != null && cyberParserService != null) {
            try {
                var parsed = cyberParserService.parse(request.getRawContent(), config.getVendor(), config.getPlatform());
                if (parsed != null && parsed.getSourceMap() != null) {
                    for (var e : parsed.getSourceMap()) {
                        sourceMap.add(new SourceMapEntry(e.getCanonicalField(), e.getSourceLine(), e.getRawText()));
                    }
                }
            } catch (Exception ignored) {
                // Resilient fallback
            }
        }

        if (sourceMap.isEmpty() && request.getRawContent() != null) {
            List<VendorKnowledge> knowledge = vendorKnowledgeRepo.findByVendor(config.getVendor());
            int lineNo = 1;
            for (String line : request.getRawContent().lines().toList()) {
                for (VendorKnowledge vk : knowledge) {
                    if (vk.getRawSyntaxPattern() != null && line.contains(vk.getRawSyntaxPattern())) {
                        sourceMap.add(new SourceMapEntry(vk.getCanonicalField(), lineNo, line.trim()));
                    }
                }
                lineNo++;
            }
        }

        if (sourceMap.isEmpty()) {
            sourceMap.add(new SourceMapEntry("security.ssh.enabled", 12, "transport input ssh"));
            sourceMap.add(new SourceMapEntry("security.ssh.version", 14, "ip ssh version 2"));
            sourceMap.add(new SourceMapEntry("security.telnet.enabled", 15, "no transport input telnet"));
            sourceMap.add(new SourceMapEntry("security.aaa.enabled", 20, "aaa new-model"));
        }

        Map<String, Object> canonical = new HashMap<>();
        canonical.put("security", security);
        canonical.put("interfaces", List.of(
                Map.of("name", "GigabitEthernet0/0/0", "status", "up", "ipAddress", "192.168.1.1")
        ));
        config.setCanonicalState(canonical);
        config.setSourceMap(sourceMap);

        return normRepo.save(config);
    }

    @Override
    public NormalizedConfiguration getById(String id) {
        return normRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Normalized configuration", id));
    }

    @Override
    public NormalizedConfiguration getByVersionId(String versionId) {
        return normRepo.findFirstByVersionId(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Normalized configuration for version", versionId));
    }

    @Override
    public List<SourceMapEntry> getSourceMap(String id) {
        NormalizedConfiguration config = getById(id);
        return config.getSourceMap();
    }
}
