package com.cms.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cms.module.ModuleDefinition;
import com.cms.module.ModuleRegistry;

import jakarta.annotation.PostConstruct;

/**
 * Ops-level, per-deployment configuration of which feature modules are enabled — see
 * docs/module-architecture/OPS_CONFIG_GUIDE.md. Deliberately not user-editable in-app: an org's
 * module set is set once, in that deployment's {@code application.yml}, by whoever stands the
 * deployment up (per the module-architecture decision log).
 *
 * <p>Leaving {@code app.modules.enabled} unset defaults to every module in {@link ModuleRegistry}
 * being enabled, so existing deployments (which predate this property) keep behaving exactly as
 * they do today until an ops person deliberately narrows the list for a new org.
 *
 * <p>Validated at startup (fail fast, per the module-architecture decision log): an unknown module
 * code, or a module enabled without a dependency it declares via {@link ModuleDefinition#dependsOn()},
 * throws from {@link #init()} and prevents the application context from starting.
 */
@Component
public class ModuleConfig {

    @Value("${app.modules.enabled:}")
    private String enabledModulesRaw;

    private Set<String> enabledModules;

    @PostConstruct
    void init() {
        if (enabledModulesRaw == null || enabledModulesRaw.isBlank()) {
            enabledModules = ModuleRegistry.all().stream()
                .map(ModuleDefinition::code)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            enabledModules = Arrays.stream(enabledModulesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        validate();
    }

    private void validate() {
        String knownCodes = ModuleRegistry.all().stream()
            .map(ModuleDefinition::code)
            .collect(Collectors.joining(", "));

        for (String code : enabledModules) {
            if (ModuleRegistry.findByCode(code).isEmpty()) {
                throw new IllegalStateException(
                    "app.modules.enabled lists unknown module code '" + code
                        + "'. Known module codes: " + knownCodes);
            }
        }
        for (ModuleDefinition module : ModuleRegistry.all()) {
            if (!enabledModules.contains(module.code())) {
                continue;
            }
            for (String dependency : module.dependsOn()) {
                if (!enabledModules.contains(dependency)) {
                    throw new IllegalStateException(
                        "app.modules.enabled enables '" + module.code() + "' but not its required "
                            + "dependency '" + dependency + "'. Enable '" + dependency
                            + "' too, or disable '" + module.code() + "'.");
                }
            }
        }
    }

    public boolean isEnabled(String moduleCode) {
        return enabledModules.contains(moduleCode);
    }

    public Set<String> getEnabledModules() {
        return Collections.unmodifiableSet(enabledModules);
    }
}
