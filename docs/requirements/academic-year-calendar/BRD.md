# Business Requirements Document — Academic Year & Calendar

**Client:** SKSCON / SKS College Of Nursing | **App:** OneCMS / College Management System | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective
The college's academic operations (fee billing, term-wise attendance, exams, timetables) all run on an Academic Year / Term cycle. This module gives the institution a single source of truth for academic years and their two terms (Odd/Even), plus a shared calendar of holidays/exams/events that every other module (Timetable, Fee, Attendance) can reference by date range.

## 2. Stakeholders
- **College Admin / Admin** — creates Academic Years, advances term lifecycle, maintains calendar events.
- **Accounting/Fee module** — bills students per term within an academic year.
- **Timetable/Attendance modules** — consume Calendar Events (holidays) and Term Instance date ranges for scheduling and working-day calculation.
- **Faculty/Student/Parent** — read-only calendar consumers.

## 3. Business Rules
Directly documented in BR-59 (Academic Calendar base module); the following synthesizes it plus code-derived rules for Academic Year/Term:

- **BR-ACADCAL-1 (from BR-59):** A `CalendarEvent` is a titled date range of one of six types, mandatorily scoped to one `AcademicYear`; `endDate` must not be before `startDate` (the only validated date rule).
- **BR-ACADCAL-2 (from BR-59):** A `CalendarEvent` carries no term-level FK — term relevance is resolved by client-side date-range overlap against the academic year's `TermInstance`s, not a database relationship.
- **BR-ACADCAL-3 (from BR-59):** A `HOLIDAY`-type event may additionally carry a `holidayCategory` (`GOVERNMENT`/`LOCAL`/`INSTITUTIONAL`), cleared server-side for any other event type.
- **BR-ACADCAL-4 (derived):** Creating an `AcademicYear` always provisions exactly two `TermInstance`s (`ODD`, `EVEN`) — there is no manual "add a term" action; term count/naming is not admin-configurable.
- **BR-ACADCAL-5 (derived):** A `TermInstance`'s status only ever advances forward (`PLANNED → OPEN → LOCKED`) — it cannot be reverted through the normal update path.
- **BR-ACADCAL-6 (derived):** Updating an Academic Year's date range is blocked if the new bounds would no longer contain its existing Term Instances' date ranges.
- **BR-ACADCAL-7 (from BR-59):** Print uses the browser's native print mechanism (hidden iframe + `window.print()`), not a generated PDF; Export is a hand-rolled CSV builder, not a spreadsheet library — both are read-only conveniences available to every role.

## 4. Business Process / Workflow
1. Admin creates a new Academic Year (e.g. "2027-2028") with start/end dates; system auto-creates its ODD and EVEN Term Instances in `PLANNED` status.
2. Admin populates the Academic Calendar with holidays, exam windows, and events for the year.
3. As the year progresses, Admin advances each Term Instance's status: `PLANNED → OPEN` (admits students into that term / starts course offerings), then `OPEN → LOCKED` (closes the term to further changes) — each transition requires confirming a checklist (BR-53, not detailed here).
4. Faculty/Student/Parent view the calendar (Timeline or Month-Grid) throughout the year; Print/Export are available to everyone.
5. Downstream modules (Fee billing, Attendance, Timetable) read the Academic Year's current Term Instance and the Calendar Events' date ranges to drive their own scheduling/billing logic.

## 5. Success Criteria
Not formally defined — inferred from feature completeness and BR-59's confirmation that the base module has been in production since 2026-04-27, with the timetable engine (a separate, later initiative) subsequently building on top of it.

## 6. Assumptions & Constraints
- The system assumes a strict two-term (Odd/Even) academic model; a program using a different term cadence (e.g. trimesters) is not representable without further engineering.
- Only one Academic Year is assumed "current" at a time; multi-year overlapping enrollment is not modeled at this layer.

## 7. Known Gaps / Deferred
- The original milestone's "Semester entity" (freely named, admin-created) was superseded by the fixed 2-terms-per-year `TermInstance` model — a scope/terminology drift from the milestone tracker, not a missing feature.
- No formal, dedicated BRD previously existed for Academic Year itself (only for the Calendar sub-feature via BR-59); this document backfills that gap.
