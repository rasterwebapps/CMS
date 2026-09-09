# Module Registry

The canonical list of toggleable feature modules, what each one owns, and how it's implemented in code. This is the human-readable mirror of `backend/src/main/java/com/cms/module/ModuleRegistry.java` (permission-code prefixes, used for API-level enforcement) and `frontend/src/app/core/nav/nav-config.ts`'s `modules` tags (used for nav/route gating). If this table and the code ever disagree, the code is authoritative — fix this doc to match, don't assume the doc is right.

## Core (always on, never a toggleable module)

Every deployment has these regardless of which modules are enabled:

- **Overview** — Dashboard, My Profile (My Timetable within this group is module-gated to `ACADEMICS` individually — see below)
- **User Management** — Users, Roles & Permissions, Permission Tiers
- **Preferences** (shared/core masters only) — Designations, Location Master (India Locations), Number Sequences, Settings

A permission code with no entry in any module below (e.g. `USER_VIEW`, `DESIGNATION_MANAGE`, `SETTINGS_EDIT`) is core and is never blocked by module enablement.

## Toggleable modules

| Module code | Display name | Nav groups it owns | Owned permission-code prefixes | Depends on |
|---|---|---|---|---|
| `ADMISSIONS` | Admission Management | Admission Management, + Preferences: Agents, Institutions, Referral Types, Staff Referrers | `ENQUIRY_`, `FEE_FINALIZE`, `FEE_COLLECT`, `DOCUMENT_SUBMISSION_`, `DOCUMENT_VERIFICATION_`, `ADMISSION_`, `RETRO_ADMIT`, `AGENT_`, `INSTITUTION_`, `REFERRAL_TYPE_`, `STAFF_REFERRER_` | — |
| `STUDENT_MGMT` | Student Management | Student Management, + Preferences: Blood Groups, Communities, Scholarship Types | `STUDENT_VIEW`/`CREATE`/`EDIT`/`DELETE`/`EXPORT`/`MANAGE` (exact codes — NOT a bare `STUDENT_` prefix, to avoid swallowing `STUDENT_FEE_*`/`STUDENT_PROMOTION_*`), `ROLL_NUMBER_`, `SCHOLARSHIP_`, `IMPORT_DATA`, `BLOOD_GROUP_`, `COMMUNITY_` | — |
| `FINANCE` | Finance | Finance, + Preferences: Fee Structures | `STUDENT_FEE_`, `RECEIPT_`, `FEE_REFUND_`, `COMMISSION_`, `FEE_STRUCTURE_` | `ACADEMICS` |
| `ACADEMICS` | Academics | Academics (all 22 items — kept as one module, too interdependent to split), + Preferences: Academic Calendar, Academic Years/Semester, Classrooms, Clinical Venues, Courses, Faculty (+ Doc Config), Holiday Templates, Labs, Periods, Programs, Specialities, Subjects | `CURRICULUM_`, `ATTENDANCE_`, `SYLLABUS_`, `EXPERIMENT_`, `COPO_`, `COURSE_`, `TIMETABLE_`, `LAB_`, `FACULTY_`, `PROGRESS_REPORT_`, `EXAMINATION_`, `EXAM_RESULT_`, `STUDENT_PROMOTION_`, `ACADEMIC_`, `SEMESTER_`, `CLASSROOM_`, `CLINICAL_VENUE_`, `HOLIDAY_TEMPLATE_`, `PERIOD_`, `PROGRAM_`, `DEPT_`, `SUBJECT_` | — |
| `LIBRARY` | Library | Library | `LIBRARY_`, `MY_LIBRARY_` | — |
| `CORE_INFRA` | Core Infrastructure | Core Infrastructure | `CAMPUS_INFRASTRUCTURE_`, `ROOM_` (covers `ROOM_PURPOSE_CATEGORY_`, `ROOM_SUB_TYPE_`), `SPATIAL_` | — |
| `INVENTORY` | Inventory Management | Stock Management (merged 2026-09-09 from what were originally three separate groups — Stock Management, Receiving & Stock Movement, Requests/Issues & Returns — see `docs/inventory-management/DECISION_LOG.md`), Purchasing & Suppliers, Equipment & Asset Management, Budgets & Approvals, Gate Pass & Service Requests, Maintenance (legacy ad-hoc equipment repair tickets — distinct from Equipment & Asset Management's preventive Maintenance Schedules), + Preferences: Equipment. (The legacy per-lab consumables screen this row used to also list was retired entirely 2026-09-09 — see the "Legacy InventoryItem retirement" decision-log entry — not folded into any of the above.) | `INVENTORY_`, `MAINTENANCE_`, `EQUIPMENT_` | — |
| `HOSTEL` | Hostel Management | Hostel Management | `HOSTEL_` | `CORE_INFRA` |
| `REPORTS` | Reports & Analytics | Reports & Analytics | `REPORT_`, `FEE_REPORT_` | — (soft dependency on `FINANCE`/`ACADEMICS`, documented only — its two items keep their own permission gates) |

## Notable near-collisions the prefix choices deliberately avoid

- `STUDENT_FEE_*` → `FINANCE`, **not** `STUDENT_MGMT`, despite sharing the `STUDENT_` word — resolved by using exact `STUDENT_VIEW`/`CREATE`/`EDIT`/`DELETE`/`EXPORT`/`MANAGE` codes for Student Management rather than a bare `STUDENT_` prefix.
- `STUDENT_PROMOTION_*` → `ACADEMICS`, same reasoning.
- `FEE_REPORT_*` → `REPORTS`, **not** `FINANCE` — Finance's Preferences item is `FEE_STRUCTURE_`, not a bare `FEE_` prefix, and Admissions' `FEE_FINALIZE`/`FEE_COLLECT` are exact codes, not a `FEE_` prefix either.
- `HOSTEL_ROOM_*` → `HOSTEL`, **not** `CORE_INFRA`'s bare `ROOM_` prefix, because it starts with `HOSTEL_`, not `ROOM_`.

`backend/src/test/java/com/cms/module/ModuleRegistryTest.java` locks these cases in as regression tests.

## Dependency enforcement

`HOSTEL` requires `CORE_INFRA`; `FINANCE` requires `ACADEMICS`. `ModuleConfig` validates this at startup (fails fast — the app won't boot with an invalid combination) and `ModuleConfigTest.java` covers both the failure and success paths.

## Cross-module nav items

"Campus Infrastructure" (Core Infrastructure group) lists both `CAMPUS_INFRASTRUCTURE_*` and `HOSTEL_ROOM_*` in its permission array, but is gated only by `CORE_INFRA` at the nav-group level — this is safe specifically because `HOSTEL` hard-depends on `CORE_INFRA`, so any deployment with Hostel enabled always has Core Infrastructure enabled too, and the item never needs to be independently reachable under Hostel alone.
