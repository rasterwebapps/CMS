package com.cms.module;

import java.util.List;

/**
 * A toggleable feature module — a named bundle of nav groups/items, routes, and permission
 * codes that a deployment can enable or disable as a unit (e.g. "SKSCON gets everything,
 * another org gets only Core Infrastructure + Inventory Management").
 *
 * <p>{@code ownedPermissionPrefixes} lists the permission-code prefixes (or, for a handful of
 * standalone codes like {@code "FEE_FINALIZE"}, exact codes) that belong to this module — see
 * {@link ModuleRegistry#resolveModuleForPermissionCode(String)}. A permission code with no
 * matching entry in any module (e.g. {@code USER_VIEW}, {@code SETTINGS_MANAGE}) is treated as
 * "core" — always available, never gated by module enablement.
 *
 * <p>{@code dependsOn} lists module codes that must also be enabled whenever this module is —
 * enforced at startup by {@link ModuleConfig}, per the module-architecture decision log.
 */
public record ModuleDefinition(
    String code,
    String displayName,
    List<String> ownedPermissionPrefixes,
    List<String> dependsOn
) {
    public ModuleDefinition {
        ownedPermissionPrefixes = List.copyOf(ownedPermissionPrefixes);
        dependsOn = List.copyOf(dependsOn);
    }
}
