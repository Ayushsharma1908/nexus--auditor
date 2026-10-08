package com.nexuscomply.configuration.service;

import com.nexuscomply.common.exception.ResourceNotFoundException;
import com.nexuscomply.configuration.model.Configuration;
import com.nexuscomply.configuration.repository.ConfigurationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigurationService {

    private final ConfigurationRepository configurationRepository;

    public ConfigurationService(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public List<Configuration> getConfigurations(String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            return configurationRepository.findByDeviceId(deviceId);
        }
        return configurationRepository.findAll();
    }

    public Configuration getConfigurationById(String id) {
        return configurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration with ID '" + id + "' was not found."));
    }

    public Configuration saveConfiguration(Configuration config) {
        return configurationRepository.save(config);
    }
}
