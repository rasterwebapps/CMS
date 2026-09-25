# Functional Requirements Document — Library Management

**Product:** OneCMS / College Management System
**Company:** Raster / Raster Images Pvt. Ltd.
**Client:** SKSCON / SKS College Of Nursing
**Module:** Library Management (Release 2, Milestone 3 — R2-M3)
**Document type:** Reverse-engineered from shipped code (backend controllers/services/entities, frontend routes, Flyway migrations)

---

## 1. Overview

Library Management provides: a Book Accession Register, a Journals & Periodicals Register, circulation (issue/return/renew) for students and faculty, overdue fine tracking, a Library → Rack → Shelf physical-location hierarchy with transfer audit trail, barcode generation and multi-transport label printing, Excel bulk import, configurable settings, operational reports, and student/faculty self-service ("My Library"). Backend package: `com.cms.{model,controller,service,repository,dto}` (prefix `Library*`). Frontend: `frontend/src/app/features/library/*`, routed under `/library/*` in `frontend/src/app/app.routes.ts`.

---

## 2. Actors & Permissions

### 2.1 Role
`LIBRARIAN` (hierarchy level 5, `is_system_role = false`) — introduced in V197, holds every permission in the `LIBRARY` category.

### 2.2 Permission Codes (category `LIBRARY`, plus `MY_LIBRARY_VIEW`)

| Code | Purpose | Introduced |
|---|---|---|
| `LIBRARY_CATALOGUE_VIEW` | View Book Catalogue | V197 |
| `LIBRARY_CATALOGUE_MANAGE` | Create/edit/delete books | V197 |
| `LIBRARY_CATALOGUE_EXPORT` | Export Book Catalogue | V257 |
| `LIBRARY_CATALOGUE_PRINT_BARCODE` | Print book barcode labels | V260 |
| `LIBRARY_CATALOGUE_VIEW_HISTORY` | View a book's full acquisition/transfer/circulation timeline | V261 |
| `LIBRARY_ISSUE_VIEW` | View Library Issues (own, for Faculty/Student) | V197 |
| `LIBRARY_ISSUE_MANAGE` | Full issue/return/renew management | V197 |
| `LIBRARY_ISSUE_EXPORT` | Export Issue Register | V257 |
| `LIBRARY_QUICK_ISSUE` | Access the quick "Issue Book" entry point | V247 |
| `LIBRARY_FINE_VIEW` | View fines | V197 |
| `LIBRARY_FINE_MANAGE` | Waive/collect fines | V197 |
| `LIBRARY_FINE_EXPORT` | Export Fine Register | V257 |
| `LIBRARY_PERIODICAL_VIEW` | View Journals & Periodicals | V197 |
| `LIBRARY_PERIODICAL_MANAGE` | Create/edit/delete periodicals | V197 |
| `LIBRARY_PERIODICAL_EXPORT` | Export Journals & Periodicals | V257 |
| `LIBRARY_PERIODICAL_PRINT_BARCODE` | Print journal barcode labels | V260 |
| `LIBRARY_PERIODICAL_VIEW_HISTORY` | View a journal's full history timeline | V261 |
| `LIBRARY_PERIODICAL_IMPORT` | Bulk-import journals | V279 |
| `LIBRARY_SETTINGS_MANAGE` | Configure library settings | V197 |
| `LIBRARY_REPORT_VIEW` | View library reports | V197 |
| `LIBRARY_REPORT_EXPORT` | Export library reports | V281 |
| `LIBRARY_IMPORT` | Bulk-import books | V197 |
| `LIBRARY_SHELF_VIEW` | View Rack/Shelf masters | V256 |
| `LIBRARY_SHELF_MANAGE` | Manage Rack/Shelf masters | V256 |
| `LIBRARY_TRANSFER` | Transfer book(s) to a different shelf/rack/library | V256 |
| `MY_LIBRARY_VIEW` | Access "My Library" self-service portal | V247 |

### 2.3 Role Grants (as seeded; actual live assignment is DB-managed via Role Management)
- **LIBRARIAN, DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN:** every `LIBRARY_*` permission (V197 + mandatory catch-all sync on every subsequent library-permission migration).
- **FACULTY, STUDENT:** `LIBRARY_CATALOGUE_VIEW`, `LIBRARY_ISSUE_VIEW` (V197). `LIBRARY_CATALOGUE_EXPORT`/`LIBRARY_ISSUE_EXPORT` were briefly over-granted to STUDENT via a blanket V242 backfill and were revoked for STUDENT specifically in V258 once V257 made the export endpoints live (FACULTY was unaffected by the revoke).
- Rack/Shelf/Transfer permissions are staff-only (auto-assigned only to roles already holding `LIBRARY_CATALOGUE_MANAGE`) — never granted to FACULTY/STUDENT.

---

## 3. Screens & UI Behavior

| Screen (component) | Route | Key fields / behavior |
|---|---|---|
| Book Catalogue list | `/library/books` | Search (title/authors/accession/publisher/call no.), status filter, Rack→Shelf cascading filter, summary cards (Total/Available/Issued), bulk select + "Transfer Selected", per-row Transfer (disabled while `ISSUED`), Export, Print Barcode. |
| Book Catalogue form | `/library/books/new`, `/library/books/:id/edit` | Accession number + barcode fields with **real-time async uniqueness validation** (`uniqueFieldValidator` against `/library/books/accession-number-exists` and `/barcode-exists`); title/authors required; source of supply (Purchase/Donation/Exchange); Library→Rack→Shelf cascading picker. |
| Issue Desk list | `/library/issues` | Filter by member type, status, item type; paginated; Export. |
| Issue form | `/library/issues/new` | Accession-number/barcode lookup, member type toggle (Student/Faculty), borrower picker, computed due date preview. |
| Journals & Periodicals list | `/library/periodicals` | Same pattern as Book Catalogue (search, filters, export, barcode print). |
| Periodical form | `/library/periodicals/new`, `:id/edit` | Accession number + barcode uniqueness validators; journal type (National/International); volume/issue/month range/year; subscription status. |
| Fine Management | `/library/fines` | Filterable list, summary cards, Waive / Collect actions each with a confirm modal, Export. |
| My Library | `/library/my-issues` | Student/faculty self-service: active issues, borrow history, fine status, Search Catalogue tab (server-side paginated, Rack→Shelf filter). |
| Library Reports | `/library/reports` | Overdue report (paginated, server-side, export) — this replaced earlier client-side-only Fine Summary / Issue History / Accession Register tabs, which duplicated the Fines/Issue/Book screens (V281). |
| Book/Journal Import | `/library/import` | Validate → Preview → Execute flow; downloadable Excel template; shared for books (`LIBRARY_IMPORT`) and journals (`LIBRARY_PERIODICAL_IMPORT`). |
| Library Racks list/form | `/library/racks`, `/library/racks/new`, `/library/racks/:id/edit` | Async name/code uniqueness scoped per-library; active/inactive status toggle. |
| Library Shelves list/form | `/library/racks/:rackId/shelves`, `.../new`, `.../:id/edit` | Nested under its Rack; async name/code uniqueness scoped per-rack. |
| Book Transfer dialog | shared component | Library → Rack → Shelf cascading pick + notes; used from both single-row and bulk actions. |
| Barcode Preview dialog | shared component | Renders Code128 preview; "Print" branches on the configured transport (Browser/Network/Local Agent) via `LibraryPrintTransportService`. |
| Item History dialog | shared component | Full acquisition + shelf-transfer + circulation timeline for one book or periodical. |
| Library Settings | `/library/settings` | Typed key-value editor for loan days, max items, fine rate, max renewals, barcode label size, printer transport/IP/port/labels-per-row. |

**Badges/status rendering:** `BookStatus` (`AVAILABLE`/`ISSUED`/`LOST`/`DAMAGED`/`WITHDRAWN`) and `IssueStatus`/`FineStatus` are rendered as status chips on their respective list screens — verify against the project's badge-consistency standard (`cms-status-badge` / `styles.scss` modifier classes) if this module's screens are touched again.

---

## 4. Functional Workflows

**Issue:**
1. Staff opens Issue Desk → New Issue (or the quick "Issue Book" entry, `LIBRARY_QUICK_ISSUE`).
2. Scans/types accession number or barcode → `GET /library/issues/lookup` resolves it to a book or periodical.
3. Selects member type (Student/Faculty) and the specific borrower.
4. Backend (`LibraryIssueService.issue`) validates: item status is `AVAILABLE`; borrower's active-item count `<` the configured max (`student_max_books`=2 / `faculty_max_books`=3 by default); computes `dueDate = issuedDate + loanDays` (14 students / 30 faculty by default).
5. On success: item flips to `ISSUED`; `LibraryIssue` row created with status `ISSUED`, `issuedBy` = acting user.

**Return:**
1. Staff uses scan-to-return (`GET /library/issues/lookup-active`) or opens the issue directly.
2. `POST /library/issues/{id}/return` — rejects if already `RETURNED` or `LOST`.
3. Item flips to `AVAILABLE`; if `today > dueDate`, a `LibraryFine` is created (`overdueDays = today - dueDate`, `totalFine = overdueDays × finePerDay`, status `PENDING`).

**Renew:**
1. `POST /library/issues/{id}/renew` — rejects if already `RETURNED`, or if `renewalCount >= maxRenewals` (default 2).
2. Due date reset to `today + loanDays`; `renewalCount` incremented; `lastRenewedDate` set; status forced back to `ISSUED` (clears `OVERDUE`).

**Overdue marking (automated):** `@Scheduled(cron = "0 0 1 * * *")` — daily at 1 AM, `markOverdueIssues()` bulk-flips every `ISSUED` row with `dueDate < today` to `OVERDUE`.

**Fine resolution:** `POST /library/fines/{id}/waive` or `/collect` — both reject a fine not currently `PENDING`.

**Book transfer:** `POST /library/books/{id}/transfer` (single) or `/library/books/transfer/bulk` — rejects a currently-`ISSUED` book; bulk transfer returns a per-book result list (success/skip reason) rather than an all-or-nothing failure.

**Bulk import (books or journals):** upload → `POST .../import/validate` (dry-run, returns row-level errors) → review → `POST .../import/execute` (commits valid rows). Template via `GET .../import/template`.

---

## 5. API Endpoints

All endpoints are Spring `@PreAuthorize`-gated; permission column shows the exact expression.

### `/libraries`
| Method | Path | Permission |
|---|---|---|
| GET | `/` | `LIBRARY_CATALOGUE_VIEW` \| `_MANAGE` \| `LIBRARY_ISSUE_VIEW` \| `_MANAGE` \| `LIBRARY_SHELF_VIEW` \| `_MANAGE` (any) |

### `/library/books`
| Method | Path | Permission |
|---|---|---|
| POST | `/` | `LIBRARY_CATALOGUE_MANAGE` |
| GET | `/` | `LIBRARY_CATALOGUE_VIEW` \| `_MANAGE` |
| GET | `/{id}` | `LIBRARY_CATALOGUE_VIEW` \| `_MANAGE` |
| GET | `/accession-number-exists` | `LIBRARY_CATALOGUE_VIEW` \| `_MANAGE` |
| GET | `/barcode-exists` | `LIBRARY_CATALOGUE_VIEW` \| `_MANAGE` |
| PUT | `/{id}` | `LIBRARY_CATALOGUE_MANAGE` |
| DELETE | `/{id}` | `LIBRARY_CATALOGUE_MANAGE` |
| GET | `/page` | `LIBRARY_CATALOGUE_VIEW` \| `_MANAGE` |
| GET | `/export` | `LIBRARY_CATALOGUE_EXPORT` |
| GET | `/{id}/barcode.png` | `LIBRARY_CATALOGUE_PRINT_BARCODE` |
| POST | `/barcode-labels` | `LIBRARY_CATALOGUE_PRINT_BARCODE` |
| GET | `/{id}/barcode.zpl` | `LIBRARY_CATALOGUE_PRINT_BARCODE` |
| POST | `/{id}/barcode-print` | `LIBRARY_CATALOGUE_PRINT_BARCODE` |
| POST | `/barcode-labels.zpl` | `LIBRARY_CATALOGUE_PRINT_BARCODE` |
| POST | `/barcode-labels-print` | `LIBRARY_CATALOGUE_PRINT_BARCODE` |
| POST | `/{id}/transfer` | `LIBRARY_TRANSFER` |
| POST | `/transfer/bulk` | `LIBRARY_TRANSFER` |
| GET | `/{id}/transfers` | `LIBRARY_CATALOGUE_VIEW` \| `_MANAGE` |

`/library/books/import`: `GET /template`, `POST /validate`, `POST /execute` — all `LIBRARY_IMPORT`.

### `/library/periodicals`
Mirrors `/library/books` (create/list/get/exists-checks/update/delete/page/export/barcode.*) under `LIBRARY_PERIODICAL_VIEW`/`_MANAGE`/`_EXPORT`/`_PRINT_BARCODE` respectively (no transfer/transfers endpoints — the Rack/Shelf hierarchy was never extended to periodicals).

`/library/periodicals/import`: `GET /template`, `POST /validate`, `POST /execute` — all `LIBRARY_PERIODICAL_IMPORT`.

### `/library/issues`
| Method | Path | Permission |
|---|---|---|
| POST | `/` | `LIBRARY_ISSUE_MANAGE` \| `LIBRARY_QUICK_ISSUE` |
| GET | `/lookup` | `LIBRARY_ISSUE_MANAGE` \| `LIBRARY_QUICK_ISSUE` |
| GET | `/lookup-active` | `LIBRARY_ISSUE_MANAGE` |
| GET | `/` | `LIBRARY_ISSUE_MANAGE` |
| GET | `/my` | `LIBRARY_ISSUE_VIEW` \| `_MANAGE` \| `MY_LIBRARY_VIEW` |
| GET | `/{id}` | `LIBRARY_ISSUE_VIEW` \| `_MANAGE` \| `MY_LIBRARY_VIEW` |
| GET | `/student/{studentId}` | `LIBRARY_ISSUE_MANAGE` |
| GET | `/faculty/{facultyId}` | `LIBRARY_ISSUE_MANAGE` |
| GET | `/book/{bookId}` | `LIBRARY_CATALOGUE_VIEW_HISTORY` |
| GET | `/periodical/{periodicalId}` | `LIBRARY_PERIODICAL_VIEW_HISTORY` |
| POST | `/{id}/return` | `LIBRARY_ISSUE_MANAGE` |
| POST | `/{id}/renew` | `LIBRARY_ISSUE_MANAGE` |
| GET | `/page` | `LIBRARY_ISSUE_MANAGE` |
| GET | `/export` | `LIBRARY_ISSUE_EXPORT` |

### `/library/fines`
| Method | Path | Permission |
|---|---|---|
| GET | `/` | `LIBRARY_FINE_VIEW` \| `_MANAGE` |
| POST | `/{id}/waive` | `LIBRARY_FINE_MANAGE` |
| POST | `/{id}/collect` | `LIBRARY_FINE_MANAGE` |
| GET | `/page` | `LIBRARY_FINE_VIEW` \| `_MANAGE` |
| GET | `/export` | `LIBRARY_FINE_EXPORT` |

### `/library/reports`
| Method | Path | Permission |
|---|---|---|
| GET | `/overdue/page` | `LIBRARY_REPORT_VIEW` |
| GET | `/overdue/export` | `LIBRARY_REPORT_EXPORT` |

### `/library/settings`
| Method | Path | Permission |
|---|---|---|
| GET | `/` | `LIBRARY_SETTINGS_MANAGE` \| `LIBRARY_CATALOGUE_VIEW` \| `LIBRARY_ISSUE_MANAGE` |
| PUT | `/{key}` | `LIBRARY_SETTINGS_MANAGE` |

### `/library/racks` and `/library/shelves` (identical shape)
| Method | Path | Permission |
|---|---|---|
| POST | `/` | `LIBRARY_SHELF_MANAGE` |
| GET | `/` | broad view (catalogue/issue/shelf view or manage, any) |
| GET | `/{id}` | `LIBRARY_SHELF_VIEW` \| `_MANAGE` |
| PUT | `/{id}` | `LIBRARY_SHELF_MANAGE` |
| DELETE | `/{id}` | `LIBRARY_SHELF_MANAGE` |
| PATCH | `/{id}/status` | `LIBRARY_SHELF_MANAGE` |
| GET | `/page` | `LIBRARY_SHELF_VIEW` \| `_MANAGE` |
| GET | `/name-exists` | `LIBRARY_SHELF_MANAGE` |
| GET | `/code-exists` | `LIBRARY_SHELF_MANAGE` |

(`/library/shelves` additionally scopes by `rack_id` per the frontend's nested route.)

---

## 6. Data Model

| Table | Key columns | Relationships |
|---|---|---|
| `libraries` | `name` (unique), `code` (unique), `address`, `is_active` | Referenced by `library_racks`, `library_books` |
| `library_racks` | `library_id` FK, `name`+`code` (unique per library), `description`, `is_active` | Belongs to `libraries`; parent of `library_shelves` |
| `library_shelves` | `rack_id` FK, `name`+`code` (unique per rack), `description`, `is_active` | Belongs to `library_racks`; referenced by `library_books.shelf_id` |
| `library_books` | `accession_number` (unique), `barcode` (unique), `title`, `authors`, `publisher`, `isbn`, `call_number`, `library_id` FK (not null), `shelf_id` FK (nullable), `subject_category`, `source_of_supply` (Purchase/Donation/Exchange), `price_rs`, `status` | FK to `libraries`, `library_shelves`; referenced by `library_issues.book_id`, `library_book_shelf_transfers.book_id` |
| `library_periodicals` | `accession_number` (unique, not-null since V260), `barcode` (unique), `journal_name`, `journal_type` (National/International), `volume_number`, `issue_number`, `month_range`, `year`, `subscription_status` (Active/Expired), `status` (per-copy, since V250) | Referenced by `library_issues.periodical_id` |
| `library_issues` | `book_id` XOR `periodical_id` (CHECK), `member_type` (Student/Faculty), `student_id` XOR `faculty_id` (CHECK), `issued_date`, `due_date`, `returned_date`, `renewal_count`, `status` (Issued/Returned/Overdue/Lost), `issued_by`, `returned_to` | FK to `library_books`/`library_periodicals`/`students`/`faculty`; referenced 1:1 by `library_fines.issue_id`. Partial unique index enforces one active issue per physical copy. |
| `library_fines` | `issue_id` (unique FK, 1:1), `overdue_days`, `fine_per_day`, `total_fine`, `status` (Pending/Waived/Collected), `waived_by`, `collected_at` | Belongs to `library_issues` |
| `library_settings` | `setting_key` (unique), `setting_value`, `display_name`, `data_type` (Integer/Decimal/String) | Standalone typed config store |
| `library_book_shelf_transfers` | `book_id` FK, `old_library_id`/`old_rack_id`/`old_shelf_id`, `new_library_id`(not null)/`new_rack_id`/`new_shelf_id`, `transferred_at`, `transferred_by`, `notes` | Belongs to `library_books`; references `libraries`/`library_racks`/`library_shelves` twice each (old/new) |

**Seeded `library_settings` defaults (V196):** `student_loan_days=14`, `faculty_loan_days=30`, `student_max_books=2`, `faculty_max_books=3`, `fine_per_day=1.00`, `max_renewals=2`. **Barcode-related (V260/V263):** `barcode_label_width_mm=50`, `barcode_label_height_mm=25`, `barcode_printer_mode=BROWSER`, `barcode_printer_ip=''`, `barcode_printer_port=9100`, `barcode_labels_per_row=1`.

**Enums:** `BookStatus{AVAILABLE,ISSUED,LOST,DAMAGED,WITHDRAWN}`, `IssueStatus{ISSUED,RETURNED,OVERDUE,LOST}`, `FineStatus{PENDING,WAIVED,COLLECTED}`, `JournalType{NATIONAL,INTERNATIONAL}`, `SubscriptionStatus{ACTIVE,EXPIRED}`, `BookSourceOfSupply{PURCHASE,DONATION,EXCHANGE}`, `LibraryMemberType{STUDENT,FACULTY}`, `LibraryItemType{BOOK,JOURNAL}`, `SettingDataType{INTEGER,DECIMAL,STRING}`.

---

## 7. Edge Cases & Validation Rules

- **Delete a book:** blocked (`IllegalStateException`) if `status == ISSUED` or if any issue-history row references it at all (even fully returned) — preserves audit trail.
- **Issue:** blocked if item not `AVAILABLE`; blocked if borrower already at/over the configured max concurrent items (message includes borrower name, current count, and max).
- **Return:** blocked if already `RETURNED`; blocked (distinct message) if status is `LOST`.
- **Renew:** blocked if already `RETURNED`; blocked once `renewalCount >= maxRenewals`.
- **Concurrency-safety:** one active issue per physical copy is enforced by a Postgres partial unique index (`WHERE status IN ('ISSUED','OVERDUE')`), not just a pre-check in code — protects against race conditions two application-level checks alone would not.
- **Fine waive/collect:** blocked unless current status is `PENDING`.
- **Book/periodical uniqueness:** accession number and barcode are both globally unique per table, validated live via dedicated `-exists` endpoints (uniqueFieldValidator pattern) and enforced again by DB `UNIQUE` constraints server-side.
- **Rack/Shelf uniqueness:** name and code are unique per-parent (per-library for racks, per-rack for shelves) — not globally unique, since two libraries/racks may legitimately reuse a label like "Rack A".
- **Book transfer:** blocked while `status == ISSUED`; bulk transfer is partial-success (each book's outcome reported independently, not all-or-nothing).
- **Barcode printer IP:** validated as a private/RFC1918 or loopback address both when saved in Settings and again immediately before each `NETWORK`-mode socket send (defense in depth).
- **Periodical `copies_count`:** legacy aggregate-copies field was dropped entirely in V280 once every row had a real per-copy `accession_number` — periodicals are now issued/returned per physical copy exactly like books, not as an aggregate pool.

---

## 8. Known Gaps / Deferred

**R2-3.2 — Digital Library APIs.** Planned but deferred (marked "out of scope for nursing college" in the milestone doc): digital resource management (e-books, journals, papers) and role-based access control to those resources. No entity, controller, service, repository, or migration for this exists in the codebase — confirmed by full-repo search. Everything implemented is physical-item circulation only.

**No fine ↔ Finance/cashier integration.** Fine collection is a manual status flip on `library_fines`; it creates no `PaymentReceipt` or ledger entry in the Finance module (flagged as "Phase 2" since the original V196 migration, still unimplemented).

**No Library (top-level) CRUD screen.** Only `GET /libraries` exists (for dropdown population); with exactly one seeded row, a management screen was judged to have no current purpose, though the schema supports adding one without a further structural migration.
