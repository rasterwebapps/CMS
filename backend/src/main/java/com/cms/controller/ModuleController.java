package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.config.ModuleConfig;
import com.cms.dto.EnabledModulesResponse;
import com.cms.dto.ModuleDto;
import com.cms.module.ModuleDefinition;
import com.cms.module.ModuleRegistry;

/**
 * Serves this deployment's enabled module set — the frontend fetches this at app init (alongside
 * {@code GET /permissions/my}) to build its nav/routes. Requires only authentication (like
 * {@code GET /permissions/my}); every logged-in user needs this to render their own nav, so it
 * carries no additional {@code @PreAuthorize} check.
 */
@RestController
@RequestMapping("/modules")
public class ModuleController {

    private final ModuleConfig moduleConfig;

    public ModuleController(ModuleConfig moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    @GetMapping("/enabled")
    public ResponseEntity<EnabledModulesResponse> getEnabledModules() {
        List<ModuleDto> enabled = ModuleRegistry.all().stream()
            .filter(m -> moduleConfig.isEnabled(m.code()))
            .map(m -> new ModuleDto(m.code(), m.displayName()))
            .toList();
        return ResponseEntity.ok(new EnabledModulesResponse(enabled));
    }
}
