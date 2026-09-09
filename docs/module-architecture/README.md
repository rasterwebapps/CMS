# Module Architecture — Documentation Index

This folder documents the module-configuration system: how OneCMS's screens/menus are grouped into toggleable feature modules, and how a deployment (one org = one deployment, per the existing single-tenant architecture) is configured with only the subset of modules it needs — e.g. SKSCON gets everything, a non-college org gets only Core Infrastructure + Inventory Management. Every new document about this initiative goes here and gets listed below, mirroring the convention already established in `docs/inventory-management/`.

## Document Index

| Document | Purpose | Status | Last updated |
|---|---|---|---|
| [DECISION_LOG.md](DECISION_LOG.md) | Append-only chronological record of the @Partner specialist round and every scope/architecture decision, with rationale | Living document | 2026-09-08 |
| [MODULE_REGISTRY.md](MODULE_REGISTRY.md) | The full module ⇄ nav-group/item ⇄ permission-code-prefix table — the human-readable mirror of `ModuleRegistry.java` and `nav-config.ts`'s `modules` tags | Living document | 2026-09-08 |
| [OPS_CONFIG_GUIDE.md](OPS_CONFIG_GUIDE.md) | How to configure a deployment's enabled module set (`app.modules.enabled` in `application.yml`), with a worked example | Living document | 2026-09-08 |

## Conventions for This Folder

1. **Every new document about module architecture is added here and listed in the index above.** A document that exists but isn't listed is out of date by definition.
2. **Decisions are logged, not just implied by an edit.** Any scope or architecture decision goes into `DECISION_LOG.md` (append-only) and is reflected in `MODULE_REGISTRY.md` if it changes the module taxonomy.
3. **`MODULE_REGISTRY.md` must stay in sync with `backend/.../module/ModuleRegistry.java` and `frontend/.../core/nav/nav-config.ts`'s `modules` tags.** These three are independently maintained (backend permission-code mapping, frontend nav-route mapping, this doc) but describe the same taxonomy — any change to one should be checked against the other two in the same pull request.
4. **This is a sub-tree of `docs/`** and follows the same rules as the rest of the repo's documentation (see `docs/README.md` and root `CLAUDE.md`).
