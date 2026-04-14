package com.cms.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SystemConfigurationRequest;
import com.cms.dto.SystemConfigurationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.SystemConfiguration;
import com.cms.repository.SystemConfigurationRepository;

@Service
@Transactional(readOnly = true)
public class SystemConfigurationService {

    private final SystemConfigurationRepository systemConfigurationRepository;

    public SystemConfigurationService(SystemConfigurationRepository systemConfigurationRepository) {
        this.systemConfigurationRepository = systemConfigurationRepository;
    }

    @Transactional
    public SystemConfigurationResponse create(SystemConfigurationRequest request) {
        SystemConfiguration config = new SystemConfiguration(
            request.configKey(),
            request.configValue(),
            request.description(),
            request.dataType(),
            request.category(),
            request.isEditable()
        );

        SystemConfiguration saved = systemConfigurationRepository.save(config);
        return toResponse(saved);
    }

    public List<SystemConfigurationResponse> findAll() {
        return systemConfigurationRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public SystemConfigurationResponse findById(Long id) {
        SystemConfiguration config = systemConfigurationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "System configuration not found with id: " + id));
        return toResponse(config);
    }

    public Optional<SystemConfigurationResponse> findByKey(String key) {
        return systemConfigurationRepository.findByConfigKey(key)
            .map(this::toResponse);
    }

    public List<SystemConfigurationResponse> findByCategory(String category) {
        return systemConfigurationRepository.findByCategory(category).stream()
            .map(this::toResponse)
            .toList();
    }

    public String getValueByKey(String key) {
        SystemConfiguration config = systemConfigurationRepository.findByConfigKey(key)
            .orElseThrow(() -> new ResourceNotFoundException(
                "System configuration not found with key: " + key));
        return config.getConfigValue();
    }

    @Transactional
    public SystemConfigurationResponse update(Long id, SystemConfigurationRequest request) {
        SystemConfiguration config = systemConfigurationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "System configuration not found with id: " + id));

        config.setConfigKey(request.configKey());
        config.setConfigValue(request.configValue());
        config.setDescription(request.description());
        config.setDataType(request.dataType());
        config.setCategory(request.category());
        config.setIsEditable(request.isEditable());

        SystemConfiguration updated = systemConfigurationRepository.save(config);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!systemConfigurationRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "System configuration not found with id: " + id);
        }
        systemConfigurationRepository.deleteById(id);
    }

    private SystemConfigurationResponse toResponse(SystemConfiguration config) {
        return new SystemConfigurationResponse(
            config.getId(),
            config.getConfigKey(),
            config.getConfigValue(),
            config.getDescription(),
            config.getDataType(),
            config.getCategory(),
            config.getIsEditable(),
            config.getCreatedAt(),
            config.getUpdatedAt()
        );
    }
}
