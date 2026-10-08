package com.nexuscomply.normalization.service;

import com.nexuscomply.normalization.dto.NormalizeRequest;
import com.nexuscomply.normalization.model.NormalizedConfiguration;
import com.nexuscomply.normalization.model.SourceMapEntry;

import java.util.List;

public interface NormalizationService {
    NormalizedConfiguration normalizeConfiguration(NormalizeRequest request);
    NormalizedConfiguration getById(String id);
    NormalizedConfiguration getByVersionId(String versionId);
    List<SourceMapEntry> getSourceMap(String id);
}
