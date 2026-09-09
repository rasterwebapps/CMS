package com.cms.exception;

/**
 * Thrown by {@link com.cms.config.PermSecurityBean} when a permission check resolves to a
 * module that this deployment has not enabled (see {@code app.modules.enabled} /
 * {@link com.cms.config.ModuleConfig}). Distinct from a plain permission denial so callers —
 * the frontend in particular — can tell "this feature isn't part of your organization's plan"
 * apart from "you personally lack access", per the module-architecture decision log.
 */
public class ModuleNotEnabledException extends RuntimeException {

    private final String moduleCode;

    public ModuleNotEnabledException(String moduleCode, String message) {
        super(message);
        this.moduleCode = moduleCode;
    }

    public String getModuleCode() {
        return moduleCode;
    }
}
