# Module Configuration Guide

How to configure which feature modules a deployment has enabled. See `MODULE_REGISTRY.md` for the full list of module codes and what each one covers.

## Summary

| What | Where | Default |
|---|---|---|
| Enabled module list | `app.modules.enabled` in that deployment's `application.yml` (or the `APP_MODULES_ENABLED` env var) | Unset = every module enabled |

> Not user-editable in-app by design — an org's module set is a deployment-time decision made once by whoever stands the deployment up, not a runtime toggle any admin can flip. See `DECISION_LOG.md`.

---

## Configuring a new deployment

### Step 1 — Decide the module codes

Pick from the module codes in `MODULE_REGISTRY.md` (`ADMISSIONS`, `STUDENT_MGMT`, `FINANCE`, `ACADEMICS`, `LIBRARY`, `CORE_INFRA`, `INVENTORY`, `HOSTEL`, `REPORTS`). Core screens (Overview, User Management, shared Preferences masters) are always available and don't need to be listed.

Check the Depends On column — `HOSTEL` requires `CORE_INFRA`, `FINANCE` requires `ACADEMICS`. The app **fails to start** if you enable a module without its dependency, so get this right before deploying.

### Step 2 — Set the property

Either in `application.yml`:
```yaml
app:
  modules:
    enabled: CORE_INFRA,INVENTORY
```

Or via environment variable (matches this deployment's existing `${ENV_VAR:default}` convention, e.g. alongside `CORS_ALLOWED_ORIGINS`):
```bash
APP_MODULES_ENABLED=CORE_INFRA,INVENTORY
```

### Step 3 — Verify at startup

Check the logs on boot. An unknown module code or a missing dependency throws `IllegalStateException` and the app won't start — the message names exactly which code or dependency is wrong:
```
app.modules.enabled enables 'HOSTEL' but not its required dependency 'CORE_INFRA'.
Enable 'CORE_INFRA' too, or disable 'HOSTEL'.
```

### Step 4 — Verify in the app

Log in and confirm the nav only shows the enabled modules' screens (plus Overview, User Management, and shared Preferences masters, which are always visible). Confirm a direct API call to a disabled module's endpoint returns `403` with `"code": "MODULE_NOT_ENABLED"` rather than succeeding.

---

## Worked example: a non-college, infra + inventory only org

```yaml
app:
  modules:
    enabled: CORE_INFRA,INVENTORY
```

This org sees: Overview, Core Infrastructure, Inventory Management (legacy asset tracking + Stock Management + Purchasing & Suppliers, all one module), the shared/core Preferences masters (Designations, Location Master, Number Sequences, Settings) plus Inventory's own Preferences item (Equipment), and User Management. Every other screen (Admissions, Academics, Finance, Library, Hostel, Reports) is absent from nav and blocked at the API.

---

## SKSCON (every module)

Leave `app.modules.enabled` unset. This is the default and requires no configuration — existing deployments predating this feature are unaffected.

---

## Future direction (not built)

A per-org frontend build that physically excludes a disabled module's code from the compiled bundle (rather than only hiding it at runtime) is a possible future addition — see `DECISION_LOG.md`'s "Bundle exclusion vs. backend-served config" entry. Not needed until an org specifically wants a leaner build artifact; the runtime gating described above is fully functional (both nav-hidden and API-blocked) on its own.
