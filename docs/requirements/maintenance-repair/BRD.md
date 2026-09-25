# Business Requirements Document — Maintenance & Repair

**Module:** Maintenance & Repair (Module 7.7) · **App:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing · **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective
Give lab/equipment staff a place to log a maintenance or repair need against a specific piece of lab equipment, track who requested it and who it's assigned to, and follow it through to completion — while automatically reflecting the equipment's own availability status so other screens (e.g. equipment lists, lab scheduling) know a unit is out of service.

## 2. Stakeholders
- **Lab/equipment staff** — raise requests, view status.
- **Maintenance coordinator / admin (`MAINTENANCE_MANAGE` holders)** — create, assign, update, delete requests.
- **Faculty** — appear as `requestedBy`/`assignedTo` on the backend data model (both FK to `Faculty`), i.e. the backend models this as a faculty-to-faculty workflow even though the frontend form currently collects free text instead (see Known Gaps).

## 3. Business Rules

> No BR-N entry in `docs/BUSINESS_REQUIREMENTS.md` covers this feature — it predates the BR numbering convention or was never formally written up. The rules below (BR-MAINT-#) are inferred from the milestone tracker (R1-M4.3) and the shipped code, not from a pre-existing business-requirements section.

| ID | Rule | Rationale |
|---|---|---|
| BR-MAINT-1 | A maintenance request must always reference exactly one `Equipment` record (`equipment_id NOT NULL`). | Requests are meaningless without a target asset. |
| BR-MAINT-2 | A request has a `MaintenanceType` (`PREVENTIVE`, `CORRECTIVE`, `EMERGENCY`, `ROUTINE`) and a `MaintenancePriority` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), both mandatory. | Lets staff triage and plan around urgency and cause. |
| BR-MAINT-3 | A request's `status` drives the linked equipment's availability: `IN_PROGRESS` forces `Equipment.status = UNDER_MAINTENANCE`; `COMPLETED` forces it back to `AVAILABLE`. | Keeps equipment availability in sync with active repairs without a separate manual step. |
| BR-MAINT-4 | Estimated cost and actual cost are tracked separately (`estimated_cost`, `actual_cost`), both optional `BigDecimal(10,2)`. | Supports budget-vs-actual comparison for repairs. |
| BR-MAINT-5 | Only `MAINTENANCE_MANAGE` holders may create, edit, or delete requests; any authenticated user can currently read them (no distinct enforced view-only gate at the API layer — see FRD §7). | Matches the coarse-permission pattern used at the time this module was built, before the later operation-wise permission mandate. |

## 4. Business Process / Workflow

1. **Request raised.** A user (intended: on behalf of / by a `Faculty` member — see Known Gaps on how this is actually collected) selects the affected equipment, describes the issue, sets type/priority, and an initial status (frontend defaults to `REQUESTED`).
2. **Assignment.** The request can be updated to associate an `assignedTo` faculty member (backend) — the frontend instead collects a free-text "Assigned Technician" name (see Known Gaps).
3. **In progress.** Status is moved to `IN_PROGRESS`; the linked equipment is automatically marked `UNDER_MAINTENANCE`.
4. **Completion.** Status is moved to `COMPLETED`, `completion_date`/`actual_cost`/`resolution_notes` are recorded; the linked equipment is automatically marked `AVAILABLE` again.
5. **Cancellation.** Status can be set to `CANCELLED` — no equipment-status side effect is coded for this transition (only `IN_PROGRESS` and `COMPLETED` trigger equipment-status changes).
6. **Deletion.** A request can be hard-deleted at any status — no soft-delete/archival was found.

No calculation formulas exist beyond simple cost fields (no automatic total/variance computation between estimated and actual cost).

## 5. Success Criteria
Not formally defined — inferred from feature completeness (a working create/list/assign/complete loop that keeps equipment status accurate). Given the contract mismatches noted below, this criterion may not currently be met end-to-end for create/edit via the UI.

## 6. Assumptions & Constraints
- The backend models requester/assignee as `Faculty` FKs — students, non-faculty staff, or external vendors cannot be recorded as requester/assignee in the current schema.
- No workflow enforcement prevents skipping states (e.g., going straight from `REQUESTED` to `COMPLETED`) — the `status` field accepts any enum value on update.
- No notification/alerting on request creation, assignment, or completion was found in this module (distinct from the general Notification & Alert Preferences system, BR-28, which was not checked for a maintenance-specific hook).

## 7. Known Gaps / Deferred
- **Requester/technician identity mismatch:** the backend expects `requestedById`/`assignedToId` (Faculty FKs); the shipped frontend form instead collects `requestedBy`/`assignedTechnician` as free-text strings and never sends the FK fields, nor the required `title`/`maintenanceType`. This is a strong indication the Add/Edit screens do not function as shipped — see the FRD for the exact field-level diff.
- **No export feature**, despite `MAINTENANCE_EXPORT` existing as a seeded permission.
- **No granular create/edit/delete permission enforcement** — everything funnels through `MAINTENANCE_MANAGE`, despite `MAINTENANCE_CREATE`/`EDIT`/`DELETE` existing as seeded, unused permissions.
- **No formal BR-N documentation** exists for this module; this BRD's business rules are inferred, not sourced from `docs/BUSINESS_REQUIREMENTS.md`.
