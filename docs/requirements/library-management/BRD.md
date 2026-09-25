# Business Requirements Document — Library Management

**Product:** OneCMS / College Management System
**Company:** Raster / Raster Images Pvt. Ltd.
**Client:** SKSCON / SKS College Of Nursing
**Module:** Library Management (Release 2, Milestone 3 — R2-M3)
**Document type:** Reverse-engineered from shipped code, migrations, and `docs/BUSINESS_REQUIREMENTS.md` (BR-35, BR-37, BR-38, BR-39)

---

## 1. Executive Summary / Business Objective

SKSCON needed to replace manual/paper-based library record-keeping (accession registers, issue registers, fine ledgers) with a digital system integrated into OneCMS. The Library Management module digitizes the full physical-library workflow: cataloguing books and periodicals, tracking their physical shelf location, circulating them to students and faculty, computing overdue fines, and giving library staff (a new `LIBRARIAN` role) and self-service portal access to students/faculty. It was built as Module 9 of Release 2 and is the first Release 2 module marked fully complete.

## 2. Stakeholders

| Stakeholder | Interest |
|---|---|
| Librarian / Library staff (`LIBRARIAN` role) | Day-to-day catalogue, circulation, fines, periodicals, settings, reports |
| College Admin / Admin (`COLLEGE_ADMIN`, `ADMIN`) | Oversight access to all library functions |
| Faculty | Borrow books/periodicals; view own issue history |
| Students | Borrow books/periodicals; view own issue history; search catalogue |
| Raster (vendor DEV_ADMIN/SUPPORT_ADMIN) | Full access via mandatory catch-all permission sync on every migration |

## 3. Business Rules

Business rules found in `docs/BUSINESS_REQUIREMENTS.md`, renumbered here as BR-LIBRARY-N and mapped to their source BR:

| ID | Source BR | Rule |
|---|---|---|
| BR-LIBRARY-1 | BR-38 | Issuing a book/periodical requires `AVAILABLE` status and enforces two librarian-configurable limits, tracked separately for students vs. faculty: loan period (default 14 days students / 30 days faculty) and max concurrent items held (default 2 students / 3 faculty). Due date = issue date + loan days. |
| BR-LIBRARY-2 | BR-38 | A physical copy can be actively issued to only one person at a time — enforced by a database partial unique index on `book_id`/`periodical_id` where status is `ISSUED`/`OVERDUE`, not application logic alone. |
| BR-LIBRARY-3 | BR-38 | Returning an item after its due date auto-creates a `PENDING` fine = `overdue_days × fine_per_day` (default ₹1/day). The fine is computed once, at return time — not accrued day-by-day while the item remains overdue. |
| BR-LIBRARY-4 | BR-38 | Fines are manually resolved to `WAIVED` or `COLLECTED` by staff. There is no automatic tie-in to the Finance/cashier module (explicitly deferred as "Phase 2" since the module's initial build). |
| BR-LIBRARY-5 | BR-38 | Renewal is blocked once `renewal_count` reaches the configured max (default 2) or the item was already returned. A successful renewal extends the due date by a full new loan period from today and resets an `OVERDUE` issue back to `ISSUED`. |
| BR-LIBRARY-6 | BR-38 | A daily 1 AM scheduled job flips any `ISSUED` item whose due date has passed to `OVERDUE`. This only changes status/visibility — it does not itself create or adjust a fine. |
| BR-LIBRARY-7 | BR-38 | Students and Faculty may view only their own issue/borrow history ("own only"), enforced server-side by the caller's identity, not by a separate permission. |
| BR-LIBRARY-8 | BR-35 | A book's physical location is a real 3-level hierarchy (Library → Rack → Shelf), not free text. A book currently `ISSUED` cannot be transferred to a different shelf/rack/library — it must be returned first. |
| BR-LIBRARY-9 | BR-35 | Only one physical Library exists today, but the schema is deliberately multi-library-ready; no Library CRUD screen exists because a single row has no CRUD need. |
| BR-LIBRARY-10 | BR-37 | Every book/periodical has its own `barcode` value, independent of (but defaulting to) its accession number, printable via a librarian-configurable transport (browser dialog / networked thermal printer / local USB agent) — the same "Print" action regardless of transport. |
| BR-LIBRARY-11 | BR-39 (applied) | Every distinct operation on a Library screen (View, Manage, Export, Import, Transfer, Print Barcode, View History) has its own dedicated permission code — never shared across operations, even on the same screen. |
| BR-LIBRARY-12 | Derived (V196 CHECK constraints) | Catalogue book/periodical status is one of `AVAILABLE`, `ISSUED`, `LOST`, `DAMAGED`, `WITHDRAWN`; an issue's status is one of `ISSUED`, `RETURNED`, `OVERDUE`, `LOST`; a fine's status is one of `PENDING`, `WAIVED`, `COLLECTED`. No other codes are valid at the database layer, independent of frontend validation. |
| BR-LIBRARY-13 | Derived (`LibraryBookService.delete`) | A book cannot be deleted while `ISSUED`, nor if it has any issue history at all (not just active issues) — preserving circulation audit trail integrity. |

## 4. Business Process / Workflow Narrative

**Cataloguing:** A librarian adds a book (accession number, title/authors/publisher/ISBN/etc., call number, source of supply, price) or a journal/periodical (journal name, national/international type, volume/issue, subscription status) to the register, optionally assigning it to a Library → Rack → Shelf location. Accession number and barcode uniqueness are validated live as the librarian types. Bulk cataloguing is also possible via Excel import (validate → preview → execute) for both books and journals independently.

**Shelving & Transfer:** Once catalogued, a book can be moved between shelves/racks/libraries at any time it is not currently issued. Each move is logged with who moved it, when, and why (notes), viewable per-book. Bulk transfer of multiple selected books is supported, with per-book partial success (issued books are skipped, not blocking the rest).

**Issue (Circulation Out):** At the Issue Desk (or the quick "Issue Book" entry point), staff look up an item by scanning/typing its accession number or barcode, select the borrower (student or faculty), and issue it. The system rejects the issue if the item isn't `AVAILABLE` or the borrower is already at their concurrent-item limit. On success, the item flips to `ISSUED` and a due date is set from the configured loan period.

**Return (Circulation In):** Staff scan/type the item's code to resolve its active issue (scan-to-return), then confirm the return. The item flips back to `AVAILABLE`. If the return date is past the due date, a fine is automatically generated in `PENDING` status.

**Renewal:** A borrower (via staff) can extend an active issue's due date, up to the configured renewal limit. Renewing an `OVERDUE` item resets it to `ISSUED`.

**Fine Lifecycle:** `PENDING` (auto-created on late return) → staff either `WAIVE` (recorded with who waived it) or `COLLECT` (recorded with a collection timestamp). No further state exists; there is no partial-payment or dispute state.

**Overdue Detection:** Independent of the return/fine flow, a nightly job re-labels any `ISSUED` item past its due date as `OVERDUE`, keeping list screens and the Overdue Report accurate without per-request computation.

**Self-Service:** Students and faculty use "My Library" to see their own active issues, borrow history, fine status, and to search the catalogue (with the same Rack/Shelf filter staff use) without needing catalogue-manage rights.

## 5. Success Criteria

Not formally defined in project documentation — no KPI, adoption target, or measurable success metric for this module was found in `BUSINESS_REQUIREMENTS.md` or the milestone tracker. Success is inferred from feature completeness against the milestone's stated scope: R2-3.1 (backend), R2-3.3 (frontend), R2-3.4 (unit tests), and R2-3.5 (manual test cases) are all marked complete, and the module carries a 982-line manual test suite (`docs/manual-test-cases/library-management.md`) plus dedicated service-layer unit tests (`LibraryBookServiceTest`, `LibraryFineServiceTest`, `LibraryIssueServiceTest`).

## 6. Assumptions & Constraints

- Assumes Student and Faculty records already exist in the system (created elsewhere); this module only references them for circulation, never creates members.
- Assumes a single physical library for SKSCON at present; multi-library operation is schema-ready but unbuilt at the UI layer.
- Fine amounts are tracked in the library domain only — collecting a fine does not create any Finance-module receipt or ledger entry.
- Barcode `NETWORK` print mode assumes a private/RFC1918 or loopback network address for the label printer (validated server-side); `LOCAL_AGENT` mode is documented as Windows/Mac-only (no Linux build of the vendor's local agent), so a Linux circulation desk with a USB printer must use `BROWSER` mode via CUPS instead.
- Milestone tracker records "40 pre-existing test compilation failures in unrelated modules" blocked full test-suite execution at the time R2-3.4 was completed — noted as a separate cleanup item, not a defect in the Library module's own tests.

## 7. Known Gaps / Deferred

**R2-3.2 — Digital Library APIs: deferred, out of scope for nursing college.** Planned scope was digital resource management (e-books, journals, papers) and role-based access control to those digital resources — neither was built. Confirmed by codebase search: no digital-resource entity, controller, service, or migration exists. This was a scoping decision recorded directly in the milestone doc, not a silently dropped requirement.

Also deferred (from BR-38's own "Explicitly Out of Scope," still true as of this writing): automatic fine-to-cashier/Finance integration — fines are tracked and resolved entirely within the Library module.
