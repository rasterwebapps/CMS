package com.cms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cms.module.ModuleRegistry;

class ModuleConfigTest {

    private ModuleConfig configWithRaw(String raw) {
        ModuleConfig config = new ModuleConfig();
        ReflectionTestUtils.setField(config, "enabledModulesRaw", raw);
        ReflectionTestUtils.invokeMethod(config, "init");
        return config;
    }

    @Test
    void unsetPropertyDefaultsToEveryModuleEnabled() {
        ModuleConfig config = configWithRaw("");

        for (var module : ModuleRegistry.all()) {
            assertThat(config.isEnabled(module.code())).isTrue();
        }
    }

    @Test
    void narrowedListOnlyEnablesTheListedModulesAndTheirDependencies() {
        ModuleConfig config = configWithRaw("CORE_INFRA,INVENTORY");

        assertThat(config.isEnabled(ModuleRegistry.CORE_INFRA)).isTrue();
        assertThat(config.isEnabled(ModuleRegistry.INVENTORY)).isTrue();
        assertThat(config.isEnabled(ModuleRegistry.ACADEMICS)).isFalse();
        assertThat(config.isEnabled(ModuleRegistry.HOSTEL)).isFalse();
    }

    @Test
    void unknownModuleCodeFailsFastAtStartup() {
        assertThatThrownBy(() -> configWithRaw("NOT_A_REAL_MODULE"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NOT_A_REAL_MODULE");
    }

    @Test
    void enablingAModuleWithoutItsRequiredDependencyFailsFastAtStartup() {
        // HOSTEL depends on CORE_INFRA
        assertThatThrownBy(() -> configWithRaw("HOSTEL"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(ModuleRegistry.HOSTEL)
            .hasMessageContaining(ModuleRegistry.CORE_INFRA);
    }

    @Test
    void enablingAModuleWithItsDependencyStartsCleanly() {
        ModuleConfig config = configWithRaw("HOSTEL,CORE_INFRA");

        assertThat(config.isEnabled(ModuleRegistry.HOSTEL)).isTrue();
        assertThat(config.isEnabled(ModuleRegistry.CORE_INFRA)).isTrue();
    }
}
