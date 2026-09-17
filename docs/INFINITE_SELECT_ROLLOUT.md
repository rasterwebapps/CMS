# Infinite-Select Rollout Tracker

Tracks migrating every `getAll()` / capped `getPage({size: N})` dropdown
(filter bars and form-field pickers) to `CmsInfiniteSelectComponent`
(`frontend/src/app/shared/infinite-select/`), per the standing rule that
every DB-backed dropdown must search + paginate against the backend rather
than eagerly loading or capping the full option list.

Started from Enquiry List (`enquiry-list.component.ts`, commit `f96b4456`),
which is the reference implementation — Program, Course (dependent on
Program), Academic Year (multi-select), Referral Type, Agent.

## How to use this file

- One module = one commit. Check items off as they land; leave a note with
  the commit hash.
- "Backend gap" items need a paginated `getPage(search, page, size)`
  controller+service endpoint added first — bigger than a frontend swap.
  Confirmed by grepping the entity's `*.service.ts` for `getPage`.
- Genuinely small, fixed-cardinality lists (days of week, exam periods in a
  day) get infinite-scroll's search/paginate UI even though they'll never
  truly paginate, per the "convert everything for consistency" scope the
  user chose — but flag in review if forcing scroll-pagination UI onto a
  ~7-row list looks actively worse than a plain select, rather than silently
  no-op'ing the decision.

## Status legend
`[ ]` not started · `[~]` in progress · `[x]` done · `[!]` backend gap, blocked

---

## Tier 1 — Enquiry-adjacent (Program/Course filters, same shape as Enquiry List)

- [x] `enquiry-list` — Program, Course, Academic Year, Referral Type, Agent (commit `f96b4456`)
- [x] `admission-completion-list` — Program, Course (active-only; see backend `activeOnly` note below)
- [x] `document-verification-list` — Program, Course (active-only)
- [x] `document-submission-list` — Program, Course (active-only)

All three restricted Program to `status === 'ACTIVE'` client-side. Rather than drop that behavior,
added a proper `activeOnly` query param to `GET /programs/page` (`ProgramController`/`ProgramService`,
with unit tests) and `ProgramService.getPage({ activeOnly })` on the frontend, so `activeOnly: true`
in `programFetchPage` now does this server-side wherever it's needed.

## Tier 2 — Admission module

- [x] `admission-list` — Program, Course, Academic Year (Academic Year keeps a small `getAllAcademicYears()` list
      client-side for `resolveLabel`, since `AcademicYearService` has no `getById`; same bounded-list exception as Enquiry List)
- [x] `admission-detail` — `settingsService.getAll()` confirmed **out of scope**: it reads branding config key/value
      pairs (college name/address/phone) for a print header, not a dropdown
- [!] `admission-form` — `studentService.getAll()` — Student has no `getPage` yet

## Tier 3 — Student module

- [x] `student-list` — Program, Course, Academic Year (same pattern as admission-list)
- [~] `student-detail` — Program feeds `ProgramTransferDialogComponent`'s **`mat-select`** (excludes the
      current program), not a plain `<select>` filter — converting it means either (a) filtering results
      inside the `fetchPage` closure and swapping to `cms-infinite-select` (breaks the dialog's Material
      form-field visual consistency) or (b) leaving it. Flagged for a design call rather than forced through.
- [x] `roll-number-assignment` — Program, Course (ngModel-driven, not URL filters; adapted the same way)
- [~] `retro-admit` — Program is one field inside a single `forkJoin` initializing a large multi-step wizard
      (with Communities/BloodGroups/ReferralTypes/Agents/FeeStates, all genuinely small/enum-like) — pulling
      Program out into a separate fetch-based picker is a real refactor of that init flow, not a drop-in swap.
      Student x2 + Faculty in the same file are backend-gap anyway (`studentSvc.getAll()`, Student has no
      `getPage`). Flagged for a design call rather than forced through.

## Tier 4 — Course / Subject / Faculty

- [x] `course-list` — Program
- [x] `course-form` — Program. First Reactive-Forms (`formControlName`) conversion — added
      `ControlValueAccessor` to `CmsInfiniteSelectComponent` (writeValue/registerOnChange/
      registerOnTouched/setDisabledState, `NG_VALUE_ACCESSOR` provider), purely additive to the
      existing `[selectedValue]`/`(selectedValueChange)` API. Multi-select stays outside CVA (a
      `Set` isn't a typical control value). 3 new unit tests on the shared component.
- [x] `subject-list` — Course
- [x] `subject-form` — Speciality only (Reactive Forms, `formControlName`). "Eligible Labs/Clinical
      Venues/Faculty" are deliberately **left as their existing always-visible checklists**, not
      converted to `cms-infinite-select[multiple]` — they show capacity/speciality inline for
      browsing a small, bounded list at a glance; collapsing that into a searchable dropdown would
      be a real UX regression, not a fix for a truncation risk. Flag, don't force.
- [ ] `faculty-list` — Speciality
- [ ] `faculty-form` — Speciality, Designation
- [ ] `faculty-doc-config` — Speciality, Designation
- [ ] `faculty-availability` — Faculty (getAll, has getPage OK); Period (getAll(true) — small fixed list, low priority)
- [ ] `speciality-form` — Faculty

## Tier 5 — Academic Year / Examination / Timetable

- [ ] `academic-year-form` — Course x2
- [!] `academic-calendar` — Period (has getPage); Blocked Period, Day Mapping — backend gap, and Day Mapping is a fixed 7-row list (flag per note above)
- [!] `examination-list` / `exam-result-list` — Examination — backend gap
- [ ] `faculty-absence` — Faculty
- [ ] `skeleton-builder` — Period
- [ ] `special-class-request-flyout` — Period, Classroom, Lab, Clinical Venue, Faculty
- [!] `lab-schedule-list` — backend gap (LabSchedule has no getPage — but this is the screen's own list data, not a filter dropdown; likely out of scope)
- [ ] `lab-schedule-form` — Classroom, Period, Clinical Venue
- [ ] `lab-list` — Speciality
- [ ] `lab-form` — Room Purpose Category, Speciality
- [ ] `classroom-form` — Room Purpose Category
- [ ] `clinical-venue-form` — Room Purpose Category

## Tier 6 — Hostel / Spatial

- [ ] `room-sub-type-list` / `room-sub-type-form` — Category (hostel room-purpose-category)
- [ ] `room-purpose-category-form` — Category
- [ ] `room-allocation-dashboard` / `room-preference-picker` — Hostel Room Type
- [ ] `campus-side-panel` — Room Type, Category, Faculty, SubType
- [ ] `detected-shapes-review-flyout` — Category, SubType

## Tier 7 — Misc

- [!] `maintenance-list` — backend gap (Maintenance has no getPage — likely the screen's own list, not a filter; confirm)
- [ ] `number-sequences-list` — uses `NumberSeriesDefinitionService.getAll()` for its own list rendering, not a filter dropdown — **likely out of scope**, confirm before touching
- [ ] `branding` — `settingsService.getAll()` — confirm usage shape before converting
- [ ] `staff-referrer-form` — Institution

## Tier 8 — Inventory module (large submodule, its own pass)

Location, Supplier, Category, Product, UOM, Brand, Tax Rule — all confirmed
`getPage`-capable. Affected screens (filter bars + form pickers), from the
sweep grep:

cycle-count-detail, cycle-count-list, cycle-count-new, gate-pass-new,
gate-pass-list, agreement(-list/-form), service-contract-form,
stock-movement-form, stock-balance-list, maintenance-schedule-form,
vendor-product-mapping(-form/-list), quotation-request(-new/-detail/-list),
wanted-list-list, purchase-order(-list/-new/-detail), budget(-form/-list),
approval-workflow-form, category(-form/-list), uom-conversion-template-form,
rack(-form/-list), ticket(-list/-new), rate-contract(-form/-list),
purchase-requisition(-list/-new), stock-transfer(-list/-new), asset(-list/-form),
stock-valuation, product-form, issue/loanable-item-issue(-list/-new),
issue/stock-issue-request(-list/-new)

- [ ] Product filters/pickers
- [ ] Location filters/pickers
- [ ] Supplier filters/pickers
- [ ] Category filters/pickers
- [ ] UOM / Brand / Tax Rule pickers (product-form only)

---

## Backend-gap items (need `getPage(search, page, size)` added first)

Confirmed missing on: `Student`, `Examination`, `LabSchedule`, `DayMapping`,
`BlockedPeriod`, `Maintenance` (services only expose `getAll()`). These are
their own mini-projects (controller + service + repository query) — treat
as a separate specialist-reviewed slice, not bundled silently into a
frontend-only tier.
