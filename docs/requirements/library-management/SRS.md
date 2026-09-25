# Software Requirements Specification — Library Management

**Product:** OneCMS / College Management System
**Company:** Raster / Raster Images Pvt. Ltd.
**Client:** SKSCON / SKS College Of Nursing
**Module:** Library Management (Release 2, Milestone 3 — R2-M3)
**Status:** Shipped (Complete) — first fully-complete Release 2 module
**Document type:** Reverse-engineered from shipped code and existing project docs (not a pre-build spec)

---

## 1. Introduction

### 1.1 Purpose
This SRS documents the Library Management module of OneCMS as it exists in the shipped codebase. It was written retroactively — after the feature was built, tested, and deployed — by inspecting the backend (Spring Boot), frontend (Angular), and Flyway migrations, cross-checked against `docs/RELEASE_2_MILESTONES.md` and `docs/BUSINESS_REQUIREMENTS.md`.

### 1.2 Scope
The module covers: a book Accession Register (catalogue), a Journals & Periodicals register, circulation (issue/return/renew) for both students and faculty, overdue fine tracking, a Library → Rack → Shelf physical-location hierarchy with book transfer and audit trail, barcode generation and label printing, bulk Excel import for books and periodicals, configurable library settings, and self-service ("My Library") + reporting screens. It does **not** cover digital/e-library resources (see §6).

### 1.3 References
- `docs/RELEASE_2_MILESTONES.md` — R2-M3 delivery record (lines 98–137) and progress tracker (line 424)
- `docs/BUSINESS_REQUIREMENTS.md` — BR-35 (Rack/Shelf & Transfer), BR-37 (Barcode/Printer), BR-38 (base Circulation module), BR-39 (Permission Model V2, referenced for the tiered permission system)
- `docs/manual-test-cases/library-management.md` — 982-line manual test suite
- Backend source: `backend/src/main/java/com/cms/{model,controller,service,repository,dto}/Library*.java`
- Migrations: `backend/src/main/resources/db/migration/V196,V197,V250–V258,V260,V261,V263,V279–V281`

---

## 2. Overall Description

### 2.1 Product Perspective
Library Management is one of nine toggleable feature modules of OneCMS (`LIBRARY` module code, alongside `ADMISSIONS`, `STUDENT_MGMT`, `FINANCE`, `ACADEMICS`, `CORE_INFRA`, `INVENTORY`, `HOSTEL`, `REPORTS`). It is a standalone module that references, but does not modify, the `Student` and `Faculty` entities from Student Management for circulation. It introduced a new role, `LIBRARIAN`, and integrates with the app-wide DB-driven permission system, the shared Excel import framework, and the shared barcode/label-print infrastructure used elsewhere in the app.

### 2.2 Actors / User Classes
| Actor | Description |
|---|---|
| **LIBRARIAN** | New role (hierarchy level 5) created specifically for this module; holds every `LIBRARY_*` permission. |
| **DEV_ADMIN / SUPPORT_ADMIN / ADMIN / COLLEGE_ADMIN** | Full library access via the mandatory catch-all sync applied on every library permission migration. |
| **FACULTY** | Read-only catalogue access + own circulation history ("My Library"); may not manage catalogue, issues, fines, or settings. |
| **STUDENT** | Same as Faculty — read-only catalogue + own circulation history. Bulk export was briefly over-granted to Students (V242 backfill) and explicitly revoked in V258. |

Role-to-permission assignment beyond migration-time defaults is managed entirely through the DB-only Role Management module, per project standing rule — not hardcoded anywhere in this module.

### 2.3 Operating Environment
Angular frontend (`frontend/src/app/features/library/*`), Spring Boot backend (`backend/src/main/java/com/cms/*`), PostgreSQL via Flyway, Keycloak-authenticated, permission checks via `@PreAuthorize("@perm...")` Spring Security expressions. Barcode label printing additionally supports a networked thermal printer (raw ZPL over a socket) or a local USB printer via a Browser-Print-style local agent, alongside the default browser print dialog.

### 2.4 Constraints & Assumptions
- Only one physical `Library` row exists today (seeded "Main Library"); the schema is multi-library-ready but no Library CRUD screen exists because one row has no CRUD need.
- Fine collection has no automatic tie-in to the Finance/cashier module — waiving/collecting a fine is a manual status change on `library_fines` only (`V196` comment: "cashier integration is Phase 2" — still not implemented).
- A physical copy (book or periodical) can only be actively issued to one person at a time — enforced at the database level via a partial unique index, not just application logic.
- The module assumes `Student` and `Faculty` records already exist (created by Student/HR management); it does not create members.

---

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-LIBRARY-1 | Maintain a Book Accession Register (create/edit/delete/search/paginate) with bibliographic fields, call number, source of supply, price, and status. | Must | Student/Faculty modules (none directly); DB permission system |
| FR-LIBRARY-2 | Enforce real-time async uniqueness of a book's accession number and barcode while typing, via a dedicated `-exists` endpoint. | Must | FR-LIBRARY-1 |
| FR-LIBRARY-3 | Prevent deletion of a book that is currently issued or has any issue history. | Must | FR-LIBRARY-1, FR-LIBRARY-8 |
| FR-LIBRARY-4 | Maintain a Journals & Periodicals register with per-copy accession number, national/international type, and subscription status. | Must | — |
| FR-LIBRARY-5 | Maintain a 3-level physical location hierarchy — Library → Rack → Shelf — with CRUD on Rack and Shelf masters, scoped uniqueness (per-library for racks, per-rack for shelves), and async name/code uniqueness checks. | Must | FR-LIBRARY-1 |
| FR-LIBRARY-6 | Transfer one book (or many, in bulk) to a different library/rack/shelf, with a full audit trail (`library_book_shelf_transfers`); block transfer of a currently-issued book; bulk transfer reports partial success. | Must | FR-LIBRARY-5 |
| FR-LIBRARY-7 | Filter the Book Catalogue and the student/faculty-facing Search Catalogue by Rack → Shelf (cascading). | Should | FR-LIBRARY-5 |
| FR-LIBRARY-8 | Issue a book or periodical to a student or faculty member, subject to: item must be `AVAILABLE`; borrower must not already hold the configured max concurrent items (default 2 students / 3 faculty); due date = issue date + configured loan days (default 14 students / 30 faculty). | Must | FR-LIBRARY-1, FR-LIBRARY-4, LibrarySetting config |
| FR-LIBRARY-9 | Return an issued item, flipping it back to `AVAILABLE`; auto-create a `PENDING` fine (`overdue_days × fine_per_day`) if returned after the due date. | Must | FR-LIBRARY-8 |
| FR-LIBRARY-10 | Renew an issued item, extending the due date by a full loan period from today and incrementing a renewal counter, blocked once the configured max-renewals count (default 2) is reached or the item is already returned; a successful renewal resets `OVERDUE` back to `ISSUED`. | Must | FR-LIBRARY-8 |
| FR-LIBRARY-11 | Nightly scheduled job (1 AM) auto-flips any `ISSUED` item whose due date has passed to `OVERDUE`. | Must | FR-LIBRARY-8 |
| FR-LIBRARY-12 | Manage fines: list/filter, waive (with a "waived by" record), or collect (with a collected timestamp). No automatic linkage to fee/cashier records. | Must | FR-LIBRARY-9 |
| FR-LIBRARY-13 | Front-desk lookup of a book/periodical by accession number or barcode (Issue screen), and scan-to-return lookup resolving a scanned code to its active issue. | Must | FR-LIBRARY-1, FR-LIBRARY-4 |
| FR-LIBRARY-14 | "My Library" self-service portal: a logged-in student/faculty member sees their own active issues, borrow history, fine status, and can search the catalogue — scoped server-side to the caller's own records. | Must | FR-LIBRARY-8 |
| FR-LIBRARY-15 | Generate a Code128 barcode per book/periodical, editable/independent of the accession number; print a single label (preview) or a batch of selected items (label sheet), via a librarian-configurable transport: browser print dialog (default), direct network socket to a thermal printer (ZPL), or a local USB-printer agent (ZPL). | Should | FR-LIBRARY-1, FR-LIBRARY-4 |
| FR-LIBRARY-16 | Bulk-import books and, separately, journals from Excel via a validate → preview → execute flow, with a downloadable template. | Should | FR-LIBRARY-1, FR-LIBRARY-4 |
| FR-LIBRARY-17 | Configure library-wide settings (loan days, max concurrent items, fine rate, max renewals, barcode label size, printer transport) via a typed key-value settings screen. | Must | — |
| FR-LIBRARY-18 | Produce operational reports: paginated overdue-items report (with export), plus catalogue/issue/fine data reachable via each screen's own export. | Should | FR-LIBRARY-1, FR-LIBRARY-8, FR-LIBRARY-9 |
| FR-LIBRARY-19 | View the full acquisition + shelf-transfer + circulation history timeline for a single book or periodical ("View History" action). | Could | FR-LIBRARY-1, FR-LIBRARY-4, FR-LIBRARY-6, FR-LIBRARY-8 |
| FR-LIBRARY-20 | Export each of the four Library list screens (Book Catalogue, Issue Register, Fines, Journals & Periodicals) independently, each gated by its own dedicated export permission. | Should | FR-LIBRARY-1, FR-LIBRARY-4, FR-LIBRARY-8, FR-LIBRARY-9 |

---

## 4. External Interface Requirements

### 4.1 Screens (frontend feature folders under `frontend/src/app/features/library/`)
Book Catalogue list/form, Issue Desk list/form, Journals/Periodicals list/form, Fine Management, My Library (student/faculty portal), Library Reports, Book/Journal Import (validate → preview → execute), Library Racks list/form, Library Shelves list/form, Book Transfer dialog, Barcode Preview dialog, Item History dialog, Library Settings. All routes are permission-guarded (`canActivate: withPermission(...)`) in `frontend/src/app/app.routes.ts`.

### 4.2 API Endpoints (high level — see FRD.md for the full table)
Base paths: `/libraries`, `/library/books` (+ `/import`), `/library/periodicals` (+ `/import`), `/library/issues`, `/library/fines`, `/library/reports`, `/library/settings`, `/library/racks`, `/library/shelves`. Standard CRUD + page/export/exists-check pattern per screen, plus circulation-specific endpoints (`issue`, `return`, `renew`, `lookup`, `lookup-active`) and barcode endpoints (`barcode.png`, `barcode.zpl`, `barcode-labels`, `barcode-print`).

### 4.3 Key DB Entities
`libraries`, `library_racks`, `library_shelves`, `library_books`, `library_periodicals`, `library_issues`, `library_fines`, `library_settings`, `library_book_shelf_transfers` — full column detail in FRD.md §6.

---

## 5. Non-Functional Requirements

### 5.1 Performance
- Book Catalogue and Search Catalogue use server-side pagination (`GET .../page`) rather than loading the full table client-side — the Search Catalogue tab was specifically migrated off an unpaginated full-fetch-and-filter pattern (BR-35) once it stopped scaling.
- `library_books.title` has a GIN full-text index (`to_tsvector('english', title)`); status, call number, subject category, and shelf are all individually indexed.
- Circulation active-issue lookups are covered by dedicated indexes on `student_id`, `faculty_id`, `status`, `due_date`, `book_id`, `periodical_id`.
- The nightly overdue-marking job runs once (1 AM cron), not on every report load, keeping report reads cheap.

### 5.2 Security / RBAC
- Every endpoint is gated by `@PreAuthorize("@perm.has(...)")` / `hasAny(...)` against one or more of 25+ dedicated `LIBRARY_*`/`MY_LIBRARY_VIEW` permission codes (see FRD.md §2) — no endpoint is unauthenticated or role-name-gated directly.
- Follows the project's "operation-wise permission mapping" rule: View, Manage, Export, Import, Transfer, Print Barcode, and View History are each their own dedicated permission per screen, never conflated.
- A prior over-grant (STUDENT receiving catalogue/issue export rights via a blanket V242 backfill) was identified and revoked in V258 once the export endpoints actually went live in V257 — documented as a live access-control fix, not a hypothetical.
- Barcode `NETWORK`-mode printer IP is validated server-side as a private/RFC1918 or loopback address, both at settings-save time and again immediately before every socket send.
- "Own records only" for student/faculty circulation history is enforced in the service layer (keyed off the caller's Keycloak username), not by a separate permission — a deliberate design choice documented in BR-38.

### 5.3 Auditability
- Every book shelf/library transfer is logged in `library_book_shelf_transfers` (old/new library, rack, shelf, who, when, notes).
- `LibraryIssue` records `issued_by` / `returned_to` (librarian username) for every circulation event.
- `LibraryFine` records `waived_by` and `collected_at`.
- All core Library entities carry `created_at`/`updated_at` via `AuditingEntityListener`.

---

## 6. Known Gaps / Not Yet Implemented

### R2-3.2 — Digital Library APIs (Deferred)
Per `docs/RELEASE_2_MILESTONES.md` line ~118, R2-3.2 ("Backend: Create digital library APIs") is checked as **deferred — out of scope for nursing college**, and remains an unchecked (`- [ ]`) item in both the milestone list and the progress tracker (line 424: *"R2-3.1 ✅ R2-3.2 deferred R2-3.3 ✅ R2-3.4 ✅ R2-3.5 ✅"*). Its planned scope, per the milestone doc, was:
- Digital resource management (e-books, journals, papers)
- Access control to those resources by student/faculty role

**Verified not implemented:** no `DigitalResource`/e-book/digital-library entity, controller, service, repository, or migration exists anywhere in the codebase (only physical book/periodical circulation is implemented). No frontend screen references digital resources. This is a genuine scope deferral, not an oversight — the milestone doc's own annotation states the reason (out of scope for a nursing college's current needs).

### Other confirmed gaps (from BR-38's own "Explicitly Out of Scope")
- **No automatic fine-to-cashier integration.** Waiving/collecting a fine is a manual status change on `library_fines`, with no linkage into `PaymentReceipt` or the fee-collection register. V196's own migration comment flagged this as "Phase 2" at initial build time; it is still the case.
- **No Library master CRUD screen.** Only one `Library` row exists (seeded); `GET /libraries` exists solely to populate dropdowns. The schema is multi-library-ready but a management UI was not built since it would have no purpose with one row.
