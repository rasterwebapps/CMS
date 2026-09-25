# Functional Requirements Document — Academic Year & Calendar

## 1. Overview
Provides the Academic Year master, its auto-generated Term Instances, and the base Academic Calendar (events) screen with Timeline/Month-Grid views, Print, and CSV Export.

## 2. Actors & Permissions
| Permission | Grants |
|---|---|
| `ACADEMIC_YEAR_MANAGE` | Create/update/delete Academic Year; `/name-exists` |
| `ACADEMIC_YEAR_VIEW`, `_CREATE`, `_EDIT`, `_DELETE`, `_EXPORT`, `_COURSE` | Granular codes seeded (Permission Model V2) — controller currently gates writes only on `ACADEMIC_YEAR_MANAGE`. |
| `SEMESTER_MANAGE` | Update Term Instance (status/dates), view/update its advance-checklist — legacy permission name retained from pre-rename "Semester" entity |
| `TIMETABLE_WORKING_SATURDAYS_MANAGE` | View/update a Term Instance's configured working-Saturday weeks |
| `ACADEMIC_CALENDAR_MANAGE` | Create/update/delete Calendar Events, delete an event series; shows Years/Semesters/Add Event controls on the calendar screen |
| `ACADEMIC_CALENDAR_VIEW` | Read access to the Academic Calendar screen |
| *(none)* | List/get Academic Years, list/get Term Instances, list/get Calendar Events — authentication only |

## 3. Screens & UI Behavior

### 3.1 Academic Year List / Form
- List: Name, Start Date, End Date, Current-year badge, Actions.
- Form: Name, Start Date, End Date, Is Current checkbox; real-time name uniqueness validation.
- Also embeds Term Instance status display/advance actions per academic year, opening the Term Advance Checklist Dialog before a `PLANNED→OPEN` or `OPEN→LOCKED` transition (full checklist behavior under BR-53).

### 3.2 Academic Calendar
- **Timeline view:** term progress strip with marker dots per event.
- **Month-Grid view:** one calendar-month grid per month the selected academic year spans (Sunday-aligned), days colored by term status, dotted per event type.
- **Add/Edit Event** dialog: Title, Description, Start Date, End Date, Event Type (Holiday/Exam/Cultural/Sports/Workshop/Other), Holiday Category (Holiday type only).
- **Overlap check:** before save, calls `GET /calendar-events/academic-year/{id}/overlapping` to warn about date-range conflicts with any existing event (not just other Holidays).
- **Print:** clones page content into a hidden iframe mirroring host stylesheets, triggers `window.print()`.
- **Export:** client-built UTF-8 BOM-prefixed CSV (Title/Type/Start Date/End Date/Description/Academic Year), filename `academic-calendar-{academicYearName}.csv`; disabled when there are no events.
- Role gating: Admin/College Admin see Years/Semesters/Add Event controls and can edit; Faculty/Student/Parent see the full read-only timeline/grid/events list with controls hidden entirely (not merely disabled). Print/Export are visible to every role.

## 4. Functional Workflows

### 4.1 Create Academic Year
1. Admin submits Academic Year Form → `POST /academic-years`.
2. Service creates the `AcademicYear` row, then calls `TermInstanceService.createTermInstancesForAcademicYear` to insert its ODD and EVEN `TermInstance` rows in `PLANNED` status.

### 4.2 Advance a Term
1. Admin opens the Term Advance Checklist Dialog for a `PLANNED` or `OPEN` term.
2. Checklist items are individually ticked plus a final "I acknowledge this is permanent" checkbox (BR-53).
3. `PUT /term-instances/{id}` transitions status forward one step.

### 4.3 Add a Calendar Event
1. Admin fills the Add Event dialog; overlap check runs against the selected date range.
2. `POST /calendar-events` with `academicYearId`, `eventType`, dates, description, optional `holidayCategory` (Holiday only).
3. Event appears immediately on both Timeline and Month-Grid views.

## 5. API Endpoints

### Academic Year (`/academic-years`)
| Method | Path | Permission |
|---|---|---|
| POST | `/academic-years` | `ACADEMIC_YEAR_MANAGE` |
| GET | `/academic-years` | authenticated |
| GET | `/academic-years/page?search=&isCurrent=` | authenticated |
| GET | `/academic-years/{id}` | authenticated |
| GET | `/academic-years/current` | authenticated |
| PUT | `/academic-years/{id}` | `ACADEMIC_YEAR_MANAGE` |
| PUT | `/academic-years/{id}/full` | `ACADEMIC_YEAR_MANAGE` |
| DELETE | `/academic-years/{id}` | `ACADEMIC_YEAR_MANAGE` |
| GET | `/academic-years/name-exists` | `ACADEMIC_YEAR_MANAGE` |

### Term Instance (`/term-instances`)
| Method | Path | Permission |
|---|---|---|
| GET | `/term-instances` | authenticated |
| GET | `/term-instances/{id}` | authenticated |
| PUT | `/term-instances/{id}` | `SEMESTER_MANAGE` |
| GET | `/term-instances/{id}/advance-checklist` | `SEMESTER_MANAGE` |
| GET | `/term-instances/{id}/working-saturdays` | `TIMETABLE_WORKING_SATURDAYS_MANAGE` |
| PUT | `/term-instances/{id}/working-saturdays` | `TIMETABLE_WORKING_SATURDAYS_MANAGE` |

### Calendar Event (`/calendar-events`)
| Method | Path | Permission |
|---|---|---|
| POST | `/calendar-events` | `ACADEMIC_CALENDAR_MANAGE` |
| GET | `/calendar-events` | authenticated |
| GET | `/calendar-events/{id}` | authenticated |
| GET | `/calendar-events/academic-year/{id}?eventType=` | authenticated |
| GET | `/calendar-events/academic-year/{id}/overlapping?start=&end=&excludeId=` | authenticated |
| PUT | `/calendar-events/{id}` | `ACADEMIC_CALENDAR_MANAGE` |
| DELETE | `/calendar-events/{id}` | `ACADEMIC_CALENDAR_MANAGE` |
| DELETE | `/calendar-events/{id}/series` | `ACADEMIC_CALENDAR_MANAGE` |

## 6. Data Model

**`academic_years`**: id, name (UNIQUE), start_date, end_date, is_current, created_at, updated_at.

**`term_instances`**: id, academic_year_id (FK), term_type (`ODD`/`EVEN`, unique with academic_year_id), start_date, end_date, status (`PLANNED`/`OPEN`/`LOCKED`), conflict_acknowledged_at, conflict_acknowledged_cell_count, created_at, updated_at.

**`term_working_saturdays`** (element collection on `term_instances`): term_instance_id, week_of_month.

**`calendar_events`**: id, title, description, start_date, end_date, event_type (`HOLIDAY`/`EXAM`/`CULTURAL`/`SPORTS`/`WORKSHOP`/`OTHER`), academic_year_id (FK NOT NULL), holiday_category (`GOVERNMENT`/`LOCAL`/`INSTITUTIONAL`, nullable), created_at, updated_at. Indexed on `(academic_year_id, event_type, start_date, end_date)`.

## 7. Edge Cases & Validation Rules
- `CalendarEvent.endDate` must not be before `startDate` — the only validated date rule (BR-59).
- Updating an `AcademicYear`'s date bounds is rejected if any existing `TermInstance` would fall outside the new range (`validateTermInstancesFitWithinBounds`).
- `TermInstance` status transitions are one-directional; attempting to move a `LOCKED` term backward is rejected (`requireNotLocked` guard used elsewhere in `AcademicYearService`).
- A `holidayCategory` value set on a non-`HOLIDAY` event type is cleared server-side rather than rejected.
- Deleting a single Calendar Event that was generated from a recurring Holiday Template series only removes that one occurrence; `DELETE /{id}/series` removes it and all future occurrences, and 400s if the event was never template-linked.

## 8. Known Gaps / Deferred
- No backend "calendar stats" summary endpoint — all figures (weeks, term count, days remaining, progress percentages) are computed client-side from the same two GET calls already needed to render the page (BR-59).
- `TermInstanceController` uses the legacy `SEMESTER_MANAGE` permission rather than a `TERM_*`-named code.
- Full Term Lifecycle confirmation-checklist content and overdue-term alerting are documented under BR-53, not repeated here.
- Holiday auto-block, Holiday Templates, and the inline Repeats picker are documented under BR-58, not repeated here.
