package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.SystemConfigurationRequest;
import com.cms.dto.SystemConfigurationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.SystemConfiguration;
import com.cms.model.enums.ConfigDataType;
import com.cms.repository.SystemConfigurationRepository;

@ExtendWith(MockitoExtension.class)
class SystemConfigurationServiceTest {

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;

    private SystemConfigurationService systemConfigurationService;

    @BeforeEach
    void setUp() {
        systemConfigurationService = new SystemConfigurationService(systemConfigurationRepository);
    }

    @Test
    void shouldCreateSystemConfiguration() {
        SystemConfigurationRequest request = new SystemConfigurationRequest(
            "penalty.daily_rate", "100", "Daily late fee penalty rate",
            ConfigDataType.DECIMAL, "PENALTY", true
        );

        SystemConfiguration saved = createConfig(1L, "penalty.daily_rate", "100",
            "Daily late fee penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationRepository.save(any(SystemConfiguration.class))).thenReturn(saved);

        SystemConfigurationResponse response = systemConfigurationService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.configKey()).isEqualTo("penalty.daily_rate");
        assertThat(response.configValue()).isEqualTo("100");
        assertThat(response.dataType()).isEqualTo(ConfigDataType.DECIMAL);
        assertThat(response.category()).isEqualTo("PENALTY");
        verify(systemConfigurationRepository).save(any(SystemConfiguration.class));
    }

    @Test
    void shouldFindAllSystemConfigurations() {
        SystemConfiguration config = createConfig(1L, "penalty.daily_rate", "100",
            "Daily late fee penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationRepository.findAll()).thenReturn(List.of(config));

        List<SystemConfigurationResponse> responses = systemConfigurationService.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).configKey()).isEqualTo("penalty.daily_rate");
        verify(systemConfigurationRepository).findAll();
    }

    @Test
    void shouldFindById() {
        SystemConfiguration config = createConfig(1L, "penalty.daily_rate", "100",
            "Daily late fee penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationRepository.findById(1L)).thenReturn(Optional.of(config));

        SystemConfigurationResponse response = systemConfigurationService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.configKey()).isEqualTo("penalty.daily_rate");
        verify(systemConfigurationRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(systemConfigurationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemConfigurationService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("System configuration not found with id: 999");

        verify(systemConfigurationRepository).findById(999L);
    }

    @Test
    void shouldFindByKey() {
        SystemConfiguration config = createConfig(1L, "penalty.daily_rate", "100",
            "Daily late fee penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationRepository.findByConfigKey("penalty.daily_rate"))
            .thenReturn(Optional.of(config));

        Optional<SystemConfigurationResponse> response =
            systemConfigurationService.findByKey("penalty.daily_rate");

        assertThat(response).isPresent();
        assertThat(response.get().configKey()).isEqualTo("penalty.daily_rate");
        verify(systemConfigurationRepository).findByConfigKey("penalty.daily_rate");
    }

    @Test
    void shouldReturnEmptyWhenKeyNotFound() {
        when(systemConfigurationRepository.findByConfigKey("nonexistent"))
            .thenReturn(Optional.empty());

        Optional<SystemConfigurationResponse> response =
            systemConfigurationService.findByKey("nonexistent");

        assertThat(response).isEmpty();
        verify(systemConfigurationRepository).findByConfigKey("nonexistent");
    }

    @Test
    void shouldFindByCategory() {
        SystemConfiguration config1 = createConfig(1L, "college.name", "SKS College",
            "College name", ConfigDataType.STRING, "BRANDING", true);
        SystemConfiguration config2 = createConfig(2L, "college.address", "Salem",
            "College address", ConfigDataType.STRING, "BRANDING", true);

        when(systemConfigurationRepository.findByCategory("BRANDING"))
            .thenReturn(List.of(config1, config2));

        List<SystemConfigurationResponse> responses =
            systemConfigurationService.findByCategory("BRANDING");

        assertThat(responses).hasSize(2);
        verify(systemConfigurationRepository).findByCategory("BRANDING");
    }

    @Test
    void shouldGetValueByKey() {
        SystemConfiguration config = createConfig(1L, "penalty.daily_rate", "100",
            "Daily late fee penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationRepository.findByConfigKey("penalty.daily_rate"))
            .thenReturn(Optional.of(config));

        String value = systemConfigurationService.getValueByKey("penalty.daily_rate");

        assertThat(value).isEqualTo("100");
        verify(systemConfigurationRepository).findByConfigKey("penalty.daily_rate");
    }

    @Test
    void shouldThrowWhenGetValueByKeyNotFound() {
        when(systemConfigurationRepository.findByConfigKey("nonexistent"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemConfigurationService.getValueByKey("nonexistent"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("System configuration not found with key: nonexistent");

        verify(systemConfigurationRepository).findByConfigKey("nonexistent");
    }

    @Test
    void shouldUpdateSystemConfiguration() {
        SystemConfiguration existing = createConfig(1L, "penalty.daily_rate", "100",
            "Daily late fee penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        SystemConfigurationRequest updateRequest = new SystemConfigurationRequest(
            "penalty.daily_rate", "200", "Updated penalty rate",
            ConfigDataType.DECIMAL, "PENALTY", true
        );

        SystemConfiguration updated = createConfig(1L, "penalty.daily_rate", "200",
            "Updated penalty rate", ConfigDataType.DECIMAL, "PENALTY", true);

        when(systemConfigurationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(systemConfigurationRepository.save(any(SystemConfiguration.class))).thenReturn(updated);

        SystemConfigurationResponse response = systemConfigurationService.update(1L, updateRequest);

        assertThat(response.configValue()).isEqualTo("200");
        assertThat(response.description()).isEqualTo("Updated penalty rate");
        verify(systemConfigurationRepository).findById(1L);
        verify(systemConfigurationRepository).save(any(SystemConfiguration.class));
    }

    @Test
    void shouldThrowWhenUpdateNotFound() {
        SystemConfigurationRequest request = new SystemConfigurationRequest(
            "penalty.daily_rate", "200", "Updated penalty rate",
            ConfigDataType.DECIMAL, "PENALTY", true
        );

        when(systemConfigurationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemConfigurationService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("System configuration not found with id: 999");

        verify(systemConfigurationRepository).findById(999L);
        verify(systemConfigurationRepository, never()).save(any());
    }

    @Test
    void shouldDeleteSystemConfiguration() {
        when(systemConfigurationRepository.existsById(1L)).thenReturn(true);

        systemConfigurationService.delete(1L);

        verify(systemConfigurationRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeleteNotFound() {
        when(systemConfigurationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> systemConfigurationService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("System configuration not found with id: 999");

        verify(systemConfigurationRepository, never()).deleteById(any());
    }

    private SystemConfiguration createConfig(Long id, String configKey, String configValue,
                                              String description, ConfigDataType dataType,
                                              String category, Boolean isEditable) {
        SystemConfiguration config = new SystemConfiguration(
            configKey, configValue, description, dataType, category, isEditable
        );
        config.setId(id);
        Instant now = Instant.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        return config;
    }
}
