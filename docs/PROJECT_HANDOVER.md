# OneCMS / SKSCMS — Developer Handover Guide

> **Audience:** a developer taking over this codebase who has never worked on it before.
> **Purpose:** one document that explains what this system is, how it's built, how to run it, what order data/screens must be set up in (from a blank database to a live college), and where to go for the detail this document doesn't cover.
>
> This document does not replace the docs listed in [Section 10](#10-where-the-real-detail-lives). It is the map that tells you which of those ~250 other files to open for a given question, plus the one thing none of them fully spell out end-to-end: **the order to do things in.**

---

## 1. What this actually is

- **Product name:** OneCMS, internally also called the College Management System (CMS). **Company:** Raster / Raster Images Pvt. Ltd. **First (reference) client:** SKSCON / SKS College of Nursing. Use these three names consistently — see `docs/module-architecture/` and `CLAUDE.md`'s naming-conventions rule.
- **What it does today:** a single-tenant (one deployment = one organization) web app covering admissions/enquiry intake, student lifecycle, fee/finance, academics + a full in-house timetable/scheduling engine, library circulation, core physical infrastructure (campus/room hierarchy), hostel room allocation, a full standalone inventory/procurement/asset system, and reports.
- **Important correction about the root `README.md`:** it describes an aspirational 21-module / ~212-feature vision written early in the project. **It is not an accurate picture of what's built.** The authoritative list of what actually exists and is toggleable per deployment is `docs/module-architecture/MODULE_REGISTRY.md` — 9 toggleable modules plus a small always-on core (see [Section 6](#6-modules--what-a-deployment-can-turn-onoff)). Don't plan work off the root README's module list.
- **Scale, concretely:** 569 Flyway migrations, ~40+ frontend nav screens, a full RBAC/permission-tier system, and a working greedy-algorithm timetable auto-scheduler — this is a mature, continuously-shipped system, not a prototype.

---

## 2. Architecture at a glance

| Layer | Technology | Notes |
|---|---|---|
| Frontend | Angular 21, standalone components, Signals, new `@if`/`@for` control flow | Angular Material 3 + Tailwind + SCSS. Folder-by-feature under `frontend/src/app/`. |
| Backend | Spring Boot 3.4.5 / Java 21 | Gradle (Kotlin DSL) — **never `./mvnw`**, this project uses `./gradlew`. |
| Database | PostgreSQL 17 | Schema managed entirely via **Flyway** migrations in `backend/src/main/resources/db/migration/`. |
| Auth | Keycloak 26 (OAuth2/OIDC) | Keycloak only owns *identity* (who is this user). Roles/permissions are **100% DB-driven** (see [Section 7](#7-authentication--rbac-model)), not Keycloak realm roles. |
| File storage | MinIO (S3-compatible) | **Hard dependency** — the backend will not boot without a reachable MinIO endpoint (`MinioStorageService` is `@PostConstruct`). See gotcha in [Section 4](#4-running-it-locally). |
| Local AI (optional) | Ollama | Powers two local-LLM R&D features (AI Smart Search on Students, AI Document Search/RAG on Admission + Enquiry documents) — never calls an external AI API, everything stays on the local box. |
| Deployment | Docker Compose, Nginx, Ubuntu | See [Section 12](#12-environments--deployment). |

**Single-tenant model:** one deployment serves one organization. A non-college org (pure inventory/asset tracking client) can run the same codebase with most modules disabled — see [Section 6](#6-modules--what-a-deployment-can-turn-onoff).

### Repository map

```
SKSCMS/
├── CLAUDE.md                 ← READ THIS FULLY. The living rulebook — hard gates, conventions, non-negotiables.
├── README.md                 ← Aspirational/historical vision doc. Don't treat as current spec (see Section 1).
├── CONTRIBUTING.md           ← Git workflow, coding standards, testing requirements
├── DEPLOYMENT.md             ← Test-server (243) deploy mechanics
├── CHANGELOG.md              ← Partially maintained; cross-check against `git log`, don't trust alone
├── docker-compose.yml        ← Local/dev-style full stack: postgres, keycloak, ollama, backend, frontend
├── backend/                  ← Spring Boot app
│   └── src/main/resources/db/migration/   ← 569 Flyway migrations — the real schema history
├── frontend/                 ← Angular app
│   └── src/app/core/nav/nav-config.ts     ← Canonical screen/menu list — read this to see every screen that exists
├── scripts/                  ← jira.sh, go_live_wipe.sh, deploy-209.sh, deploy-243.sh, seed_demo_data.py, etc.
├── infrastructure/           ← Keycloak realm JSON, other infra config
├── e2e/                      ← Playwright end-to-end suite (paused mid-rollout — see e2e/README.md)
└── docs/                     ← Everything else. See Section 10 — this is where the real functional detail lives.
```

---

## 3. Prerequisites

- Java 21 (JDK), Node.js 20+ & Angular CLI, Docker & Docker Compose, PostgreSQL client (`psql`), Git.
- `scripts/.jira.env` for the JIRA CLI (`scripts/jira.sh`) — ask the previous team/the project owner for credentials.

---

## 4. Running it locally

There are two real modes. **Use Postgres mode for any meaningful work** — H2 mode is a quick smoke-test only.

### 4a. Quick smoke-test (H2, no persistence, no Flyway)
```bash
cd backend && ./gradlew bootRun        # defaults to 'local' profile → in-memory H2, Flyway disabled
cd frontend && npm install && ng serve
```
Data seeded by `DataLoader`/`LocalDataSeeder` is created fresh every run and lost on restart. Useful only for a first look at the UI.

### 4b. Real local dev (PostgreSQL + Flyway, persistent)
```bash
docker run -d --name cms-postgres -e POSTGRES_DB=cmsdb -e POSTGRES_USER=cms -e POSTGRES_PASSWORD=cms -p 5435:5432 postgres:17
docker compose up -d keycloak     # or the full docker-compose.yml stack

cd backend
DB_URL=jdbc:postgresql://localhost:5435/cmsdb DB_USERNAME=cms DB_PASSWORD=cms \
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```
Full details, verification queries, and the H2 console: `docs/DATABASE_CONFIG_GUIDE.md`.

### Gotchas that will cost you real time if you don't know them up front

1. **MinIO is mandatory, not optional.** The backend fails to boot entirely (not just file-upload features) without a reachable MinIO endpoint. There is **no `minio` service in the root `docker-compose.yml`** — this is a known, still-unfixed gap. Point `MINIO_ENDPOINT` at the shared test server's MinIO (`http://172.17.1.243:9000`) rather than trying to stand up a local instance; get real credentials from that server, not by guessing. **Never upload throwaway test documents to that endpoint** — it's shared by other devs/QA.
2. **Local dev machines are typically RAM-constrained** (as little as ~150MB free / 16GB swap on some dev boxes). The two local-LLM features (AI Smart Search, AI Document RAG via Ollama) only work with small 1.5–3B models — never attempt a 7B+ model locally.
3. **LAN access from another machine** (not `localhost`) requires HTTPS on both frontend (`ng serve --ssl`) and backend (self-signed `backend/local-dev.p12`), plus matching Keycloak `allowed-issuers` and redirect-URI entries for that LAN IP. Chrome disables `window.crypto.subtle` (needed for Keycloak PKCE) on any non-HTTPS, non-localhost origin — this is the real root cause behind "login works on my machine, fails from another," not a firewall issue. See `docs/manual-test-cases/` and ask for the full checklist if you hit this.
4. **No backend hot-reload.** After any backend code change, you must kill and restart `bootRun` — there's no devtools auto-restart configured.
5. **Backend test suite uses `create-drop`, not Flyway.** `./gradlew test` passing proves nothing about whether your migration actually runs cleanly against a real migrated schema. Boot the backend against a real Postgres+Flyway-migrated local DB before calling a migration "done."

---

## 5. Database & migrations (Flyway) — the hard gates

The schema is 100% Flyway-managed, 569 migrations deep. A few rules are **non-negotiable** and have caused real production incidents when violated — see `CLAUDE.md` for the full list, but the two most important:

1. **A shipped migration file is frozen forever.** Never edit a migration that has already run anywhere (even dev/test). Flyway checksums the file; editing it crash-loops the backend on every environment that already applied it. If a past migration's data needs correcting, write a **new** forward migration with an idempotent `WHERE` guard. (Real incident: editing V37_1/V45 after they'd shipped silently crash-looped a server for over a week.)
2. **Never guess a column name.** Before writing an `INSERT` in a new migration, grep the existing migrations for the real column names of the target table. Two prior migrations failed from guessing.

Any migration that **inserts permissions** must end with the DEV_ADMIN/SUPPORT_ADMIN catch-all sync block (pattern established in V129, V172) — those two roles always get every permission automatically.

---

## 6. Modules — what a deployment can turn on/off

The canonical mirror of `backend/.../module/ModuleRegistry.java` and `frontend/.../core/nav/nav-config.ts` is **`docs/module-architecture/MODULE_REGISTRY.md`** — read that file, not this summary, before making any module-gating change. In short:

| Module code | Covers |
|---|---|
| `ADMISSIONS` | Enquiry → document submission/verification → fee finalize → admission → retro-admit; Agents, Institutions, Referral Types, Staff Referrers |
| `STUDENT_MGMT` | Student Explorer, AI Smart Search, Roll Numbers, Scholarship Applications, Data Import |
| `FINANCE` | Fee Explorer, Receipts, Refunds, Commissions, Fee Structures *(depends on `ACADEMICS`)* |
| `ACADEMICS` | The whole Academics nav group (curriculum, course offerings, the timetable engine, attendance, exams, promotion) — kept as one module, too interdependent to split |
| `LIBRARY` | Library circulation |
| `CORE_INFRA` | Campus/building/room hierarchy, Room Purpose Categories, Room Sub-Types |
| `INVENTORY` | Stock Management, Purchasing & Suppliers, Equipment & Asset Mgmt, Budgets & Approvals, Gate Pass & Service Requests, Maintenance |
| `HOSTEL` | Hostel Room Types, Room Preferences, Room Allocation *(depends on `CORE_INFRA`)* |
| `REPORTS` | General + Fee reports |

Always-on regardless of module config: Dashboard, My Profile, User Management, and the shared/core Preferences masters (Designations, Location Master, Number Sequences, Settings).

Configured via `app.modules.enabled` (or `APP_MODULES_ENABLED` env var) — **unset = every module enabled** (this is SKSCON's config; it needs no setup). A non-college org (e.g. inventory-only client) would set `app.modules.enabled: CORE_INFRA,INVENTORY`. Full worked example: `docs/module-architecture/OPS_CONFIG_GUIDE.md`. The app **fails to start** if you enable a module without its dependency (`HOSTEL`→`CORE_INFRA`, `FINANCE`→`ACADEMICS`).

---

## 7. Authentication & RBAC model

- Keycloak is **identity-only**: it authenticates the user and issues a JWT. It does **not** carry roles/permissions.
- Every authorization decision is DB-driven: `app_roles` → `role_permissions` → `permissions`, resolved per request. This is BR-24 in `BUSINESS_REQUIREMENTS.md`.
- **Role management (who has which role) is a DB-only operation**, done through the in-app Role Management module — never hardcode a role check anywhere in code.
- **Permission Tiers (BR-39)** let a permission be scoped to a functional tier rather than being all-or-nothing per screen.
- **Operation-wise permissions (hard gate):** every distinct button/operation on a screen (View, Manage, Export, Import, Transfer, Delete...) gets its own dedicated permission code, named `<MODULE>_<SCREEN>_<OPERATION>` — never shared "for simplicity." See `CLAUDE.md`.
- The three **platform bootstrap roles** that exist outside any college's own org structure: `DEV_ADMIN` (Raster dev/infra team, full access to everything, auto-synced to every new permission), `SUPPORT_ADMIN` (Raster support team, same auto-sync), and `collegeadmin` (the actual college's own admin — scoped, see go-live section below). These three, and only these three, survive a go-live wipe.

---

## 8. Data creation order — from a blank database to a live college

This is the part no single existing doc spells out end-to-end. It has three layers: **(a)** what Flyway inserts automatically on first boot, **(b)** what the go-live wipe script does when retiring demo data for a real client, and **(c)** the actual screen-by-screen order a College Admin must follow afterward. `frontend/src/app/core/nav/nav-config.ts` itself contains the authoritative ordering rationale in its comments — read it alongside this section.

### 8a. What ships automatically — no action needed

On every fresh boot against a real Postgres database, Flyway runs all 569 migrations in order. Among them, these are **seed/default-data migrations** (not schema), and they run unconditionally:

- **RBAC bootstrap** (V87 creates the tables, V88 seeds roles & permissions, V89 fixes gaps; V123 adds the `collegeadmin` role; V172 locks down College Admin's permission set for go-live). Every subsequent permission-adding migration re-syncs `DEV_ADMIN`/`SUPPORT_ADMIN` to hold it automatically.
- **Reference/lookup masters:** India country row, Communities (SC/ST/BC/MBC/DNC/OC/EWS/Others), Blood Groups (all 8), Referral Types (Walk-In, Phone, Online, Agent Referral, Staff, Alumni, Parent, Advertisement, Student, Faculty), Scholarship Types (First Graduate, SC/ST/OBC/BC govt, EWS, Merit, Sports), Fee States (Tamil Nadu = default, Other State = fallback — V165, BR-30).
- **System configuration keys** — `college.*`, `receipt.*`, `trust.*`, branding keys.
- **Dashboard widget defaults** per role (BR-40).

In addition, on a **local/H2 "local" profile only**, `DataLoader`/`LocalDataSeeder` also drops in a full demo dataset (nursing-college specialities, programs, courses, academic years, 8 faculty, 10 students, labs, equipment, fee structures, exams, 7 enquiries, 3 agents) purely for UI smoke-testing — see `docs/manual-test-cases/data-seeding.md`. **This demo data is not what you build a real client's data on top of** — that's what Section 8b is for.

### 8b. Retiring demo data for a real go-live: `scripts/go_live_wipe.sh`

Run against a Postgres database that already has every migration applied. Preserves the reference masters above (re-upserting any missing rows as an idempotent repair), **wipes every transactional/structural table** (students, enquiries, admissions, fees, specialities, programs, courses, academic years, labs, exams, attendance, everything), and **recreates exactly three users/roles**: `devadmin` (DEV_ADMIN), `supportadmin` (SUPPORT_ADMIN), `collegeadmin` (College Admin, deliberately narrow permission set — can configure admission/academic/finance masters and run the admission flow, but cannot see or assign Dev/Support Admin roles). Always `--dry-run` first; always take a `pg_dump` backup before running for real — see `CLAUDE.md`'s production-data-safety rules, which apply to this script without exception.

### 8c. The real setup order after a go-live wipe (or for a brand-new client)

The script's own completion message lists a 10-step skeleton, but it only covers Admissions/Finance — it predates Academics' full timetable engine, Core Infrastructure, Hostel, and Library. Here is the **complete** order, phase by phase. Within each phase, follow the listed order exactly — later screens have hard FK/UI dependencies on earlier ones.

**Phase 0 — Platform (one-time, DEV_ADMIN/SUPPORT_ADMIN only)**
Nothing to do here day-to-day; these two roles and their Keycloak users already exist from the wipe/bootstrap.

**Phase 1 — Organization-wide core masters** *(Preferences, core/shared items)*
1. **Settings** — college name, address, email, phone, branding
2. **Location Master** (India country already seeded; add States/Districts if not already present)
3. **Designations**
4. **Number Sequences** — verify/configure before anything that auto-numbers (admissions, receipts)

**Phase 2 — Academic foundation** *(Preferences → Academics masters, in this exact order — each depends on the one before)*
5. **Specialities** (Preferences → Specialities)
6. **Programs** (depend on Specialities)
7. **Courses** (depend on Programs)
8. **Academic Years** → mark the current one (Preferences → Academic Years)
9. **Holiday Templates**, **Periods**, **Classrooms**, **Clinical Venues**, **Labs** — all one-time institution-wide configuration, order among these five doesn't matter, but all must exist before building any timetable

**Phase 3 — Curriculum** *(Academics nav group)*
10. **Curriculum Versions** first — Syllabus rows, Attendance-Threshold rows, and Elective Groups are all children of a curriculum version
11. **Subjects**
12. **Syllabus**
13. **Experiments** (hang off Subject)
14. **CO/PO Mapping** (hangs off Experiments)

**Phase 4 — People**
15. **Faculty** (+ **Faculty Doc Config** if document requirements need adjusting)
16. **Agents**, **Staff Referrers**, **Referral Types**, **Institutions** — admissions-side people/partners, needed before taking real enquiries

**Phase 5 — Finance setup**
17. **Fee Structures** — configured manually via the multi-dimension Combination Picker (program × course × quota × fee state × gender × student type). There is **no bulk/auto seed for fee structures** by design (removed in BR-30) — this must be done by the College Admin through the UI.
18. **Scholarship Types** — review the seeded defaults, add institution-specific ones if needed

**Phase 6 — Begin taking admissions** *(the enquiry→student pipeline, in this order per screen)*
19. **Enquiries** → **Submit Documents** → **Verify Documents** → **Finalize Fee** → **Collect Payment** → **Complete Admission** (creates the Student + Admission records together)
20. **Assign Roll Numbers** once admission is complete

**Phase 7 — Term operations** *(once students exist and a term needs a live timetable — order matters, and is documented directly in `nav-config.ts`'s comments)*
21. **Course Offerings** for the term
22. **Elective Assignment** — only after this do you know which elective options have real enrolled students
23. **Capacity Auto-Plan** — decides whether a cohort's Theory splits into multiple sections/rooms; must run **before** Assign Faculty, because per-section faculty assignment only becomes possible once sections exist
24. **Assign Faculty**
25. **Recurring Unavailability** (faculty standing commitments) — gates Staffing
26. **Timetable Builder** → generate/edit the draft grid → **Draft Review** → Publish
27. From here on, day-to-day term operations: **Attendance**, **Faculty Absence**/substitution, **Manage Exams** → **Exam Results**, **Progress Report**
28. **Student Promotion** at term close

**Phase 8 — Other modules, as enabled, independent of the above**
- **Library:** Racks & Shelves → Book/Journal Explorer (catalogue) → Library Settings → then Issue Books day-to-day
- **Core Infrastructure / Hostel** (if enabled): Room Purpose Categories + Room Sub-Types → Campus Infrastructure (building/floor/zone/room hierarchy, via Campus Setup's floor-plan import) → Hostel Room Types → Room Preferences → Room Allocation
- **Inventory** (if enabled, and it's the one module built to be usable standalone by a non-college org too): Categories/UOM/Brands/Locations/Racks masters → Suppliers/Rate Contracts → Purchase Requisitions → Purchase Orders → Goods Receipts → Stock Balance is now live → day-to-day Stock Indents/Transfers/Issues, Asset Register, Budgets, Gate Passes as needed

**Phase 9 — Go live.** Begin taking real enquiries/admissions in production.

---

## 9. Screen-by-screen functional map

`frontend/src/app/core/nav/nav-config.ts` is the single source of truth for every screen that exists, its route, its permission codes, and — critically — its own inline comments explaining *why* screens are ordered the way they are in the nav (several of which directly justify the setup order in Section 8c). Treat it as a living index, not this document.

High-level groupings (nav order):

1. **Overview** — Dashboard, My Profile, role-specific My Timetable/My Dashboard/My Fees/My Wards views (student/parent self-service)
2. **Admission Management** — Enquiries, Finalize Fee, Collect Payment, Submit/Verify Documents, Complete Admission, Admission Explorer, AI Document Search, Retro Admit
3. **Student Management** — Student Explorer, AI Smart Search, Roll Numbers, Scholarship Applications, Data Import
4. **Finance** — Fee Explorer, Receipts, Refunds, Commissions
5. **Academics** — the largest group by far: Curriculum Versions/Syllabus/Experiments/CO-PO Mapping, Course Offerings, Elective Assignment, Capacity Auto-Plan, Assign Faculty, Recurring Unavailability, Timetable Builder, Timetable, Resource Timetable, Class Schedules, Faculty Absence, Special Class requests/approvals, Escort Duties, Attendance, Progress Report, Manage Exams, Exam Results, Student Promotion
6. **Library** — Issue Books, Issue Explorer, Overdue Books, Book/Journal Explorer, My Library, Fines, Racks & Shelves, Import, Library Settings
7. **Core Infrastructure** — Campus Infrastructure, Room Purpose Categories, Room Sub-Types
8. **Maintenance** (legacy repair tickets) / **Stock Management** / **Purchasing & Suppliers** / **Equipment & Asset Management** / **Budgets & Approvals** / **Gate Pass & Service Requests** — the full Inventory module, split into these six nav groups
9. **Hostel Management** — Room Types, Room Preferences, Room Allocation
10. **Reports & Analytics** — General Reports, Fee Reports
11. **Preferences** — every module's own masters, clustered but shown as one nav group
12. **User Management** — Users, Roles & Permissions, Permission Tiers

For what each screen actually *does* (business rules, edge cases, API endpoints), don't try to reverse-engineer it from the frontend alone — go to the docs in Section 10 first.

---

## 10. Where the real detail lives

This project documents itself extensively. Don't duplicate this effort — find the existing doc first.

| Question | Go to |
|---|---|
| "What are the business rules for X?" | `docs/BUSINESS_REQUIREMENTS.md` — BR-1 through BR-62 (a couple of numbers skipped), covers fee logic, RBAC, admissions workflow, timetable engine, inventory, module config, etc. **This is the single source of truth for business logic** and must be updated for any business/workflow change. |
| "What screens/API/data-model does module X have, and is it fully built?" | `docs/requirements/<module>/{SRS,BRD,FRD}.md` — per-module formal specs reverse-engineered from shipped code, organized by delivery release/milestone (see `docs/requirements/README.md` for the full index). **Frozen as of 2026-09-24** — see Section 11 for what's shipped since. Partially-built modules say so explicitly in a "Known Gaps" section. |
| "What issues were found while writing the requirements docs?" | `docs/requirements/FINDINGS.md` |
| "How do I use screen X as an end user?" (role-oriented walkthroughs) | `docs/user-guides/` — Cashier, College Admin, Front Office guides (.md and pre-rendered .html) |
| "What test cases exist/are expected for feature X?" | `docs/manual-test-cases/` — one file per feature area, required for every completed task |
| "What's the milestone/phase roadmap and what's done?" | `docs/RELEASE_1_MILESTONES.md`, `RELEASE_2_MILESTONES.md`, `RELEASE_3_MILESTONES.md`, `docs/DEVELOPMENT_PLAN.md` |
| "How is the module-toggle system implemented and why?" | `docs/module-architecture/` — README, MODULE_REGISTRY, OPS_CONFIG_GUIDE, DECISION_LOG |
| "What's the Inventory module's original business-team SRS and gap analysis vs. what got built?" | `docs/inventory-management/` |
| "What coding/architecture standards apply?" | `docs/TECHNICAL_STANDARDS.md` |
| "How do I write a Flyway migration / Angular component / Spring controller the way this codebase expects?" | `docs/skills/*.md` |
| "What are the hard gates, non-negotiable rules, and recurring bug patterns to check for?" | **`CLAUDE.md` at the repo root — read this in full, it's not optional.** Covers production-data safety, migration rules, permission patterns, badge/CSS audit checklists, list-screen structural requirements, and more, each with a real incident behind it. |

---

## 11. What's shipped since the requirements docs were frozen (2026-09-24 → present)

`docs/requirements/` is explicitly a snapshot, not a living doc. Real work has continued since. From `git log` and recent delivery, the notable items not reflected there:

- **Timetable engine refinements (OC-271–275 and more):** Class Schedules rebuilt as a date-wise occurrence browser with reschedule/swap; Cohort filter added; Faculty Absence converted to a filterable list screen; redundant Staff Session Swap screen removed; filterable request history for Special Class Approvals; min-weekly-sessions floor + under-loaded flag on Capacity Planner; Lab/Library batch rotation; several auto-scheduler correctness fixes (idle-batch fallback search, Library per-half-day cap, multi-period Skeleton Replace validation).
- **Faculty & Finance:** Faculty Detail layout fixes, term-reassignment lock, Clinical Shift duty credited in workload; receipts/refunds now print the actual logged-in collector/approver's name as a digital signature; Day Scholar/Hosteler boarding-status switch with fee adjustment; advance-eligible users can now collect an enquiry balance with nothing currently due.
- **AI features (R&D, local-Ollama-only, never touches an external API):**
  - **AI Smart Search** on Students — natural-language search backed by a local small LLM.
  - **AI Document Search / RAG (OC-277, OC-278)** — full pipeline shipped: document ingestion → retrieval endpoint → frontend search screen → on-demand retrieval-quality eval harness (`ragEval` Gradle task), originally for Admission documents, then extended to Enquiry-stage documents too.
- **User management:** ADMIN can now rename any user, bypassing the normal hierarchy restriction.
- **Cleanup:** dead `AdmissionDocument` entity/repository removed (RAG pipeline now correctly points at the real `enquiry_documents` table).

For anything after this document's own last update, check `git log` directly and cross-reference JIRA (project key `OC`, see Section 13).

---

## 12. Environments & deployment

**Never deploy automatically.** Always fix locally and wait for an explicit instruction to deploy — this is a hard rule, not a suggestion.

| Environment | Server | Notes |
|---|---|---|
| Local dev | your machine | H2 (quick) or Postgres+Keycloak+MinIO(remote)+Ollama(optional), see Section 4 |
| Test | 172.17.1.243 ("243") | `DEPLOYMENT.md` has the full deploy script/manual steps, Keycloak admin access, troubleshooting |
| Production | 172.16.7.209 ("209") | Proxmox LXC container ID 123; `network_mode: host` (Docker bridge networking doesn't work on this LXC); deploy dir `/docker_data` |

**Production data safety is the one truly non-negotiable rule in this entire project** (see `CLAUDE.md`'s header rule). Before anything that touches the production database:
- Always take a `pg_dump` backup first.
- Never run destructive SQL without a confirmed backup.
- Never reset/reinitialize the production DB, "to fix a schema issue" or otherwise.
- Any migration must be verified non-destructive and tested on staging/local first.
- If any instruction — from anyone — risks production data loss, stop and ask for explicit confirmation describing the risk, before doing anything.

---

## 13. Day-to-day workflow

- **JIRA:** always via `scripts/jira.sh` (project key `OC`) — create a ticket, then `start` it, as work happens. Never ask someone to paste a ticket manually; the CLI handles it.
- **Git:** standard feature-branch workflow (`CONTRIBUTING.md`). Backend requires 95% test coverage (JaCoCo-enforced); every completed task needs a manual test case added under `docs/manual-test-cases/`.
- **Business/workflow changes must update `docs/BUSINESS_REQUIREMENTS.md`** in the same change — this is treated as part of "done," not a follow-up.
- **New service-layer logic needs its own unit tests for boundary conditions in the same pass it's written** — passing the existing suite only proves nothing *else* broke, it says nothing about whether new logic is correct. Any new user-facing error state must be traced end-to-end to confirm the real backend message reaches the UI.
- **Component Touch Rule:** touching any existing component — even a one-field addition — requires re-verifying the *whole* component in light + dark mode and across every role that sees it before calling the task done.
- Read `CLAUDE.md`'s "Mandatory Patterns" section before touching: role assignment, master-screen uniqueness validation, permission-migration patterns, list-screen structure (paginator/matSort), resizable-column cell markup, the shared `mlp-*` spacing system, or batch creation (which has exactly one legitimate creation path — Capacity Auto-Plan).

---

## 14. Suggested first-week checklist for the new owner

1. Get local dev running in Postgres mode (Section 4b), including MinIO pointed at the shared test instance.
2. Read `CLAUDE.md` in full — it's ~800 lines but every rule in it exists because something broke in production or wasted real time without it.
3. Read `docs/module-architecture/MODULE_REGISTRY.md` and `frontend/src/app/core/nav/nav-config.ts` side by side to build a mental map of every screen that exists.
4. Skim `docs/BUSINESS_REQUIREMENTS.md`'s table of contents (BR-1 to BR-62) to know what's been formally decided and where.
5. Pick one module you'll likely touch first and read its `docs/requirements/<module>/FRD.md` plus the matching `docs/manual-test-cases/` files.
6. Run `scripts/go_live_wipe.sh --dry-run` once (no DB connection needed) to see exactly what a real go-live looks like at the SQL level.
7. Get `scripts/jira.sh` working against the live JIRA instance and look at recently closed `OC-*` tickets to see real recent change patterns.
8. Ask the outgoing developer specifically about: the e2e Playwright suite's paused state (`e2e/README.md`), the Hostel module's known gaps (mess/attendance/fee wiring not built), and the Inventory module's `InventoryItem` migration (R3-M8, not started).
