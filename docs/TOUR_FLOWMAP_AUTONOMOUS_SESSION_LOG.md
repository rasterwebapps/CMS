# Take a Tour + Flow Map Rollout — Autonomous Session Log

Parent ticket: **OC-139**. Pattern reference: `frontend/src/app/shared/tour/README.md`.
Built for the unattended cron sessions (9 PM 2026-08-18 / 3 AM 2026-08-19) that
roll out Guided Tour + Flow Map to every screen in the app, matching what
OC-136/137/138 shipped for Collect Payment.

Each autonomous run appends timestamped progress lines to the **Progress Log**
section at the bottom (real `date` output, not guessed timestamps) and ticks
boxes in the **Screen Checklist** below as it completes them. This file is the
human-readable status; raw transcripts are in `~/.tour-autonomous-logs/`
(outside the repo).

## Screen Checklist

"Tour" = has an existing Guided Tour (`tourService.register(...)`) as of the
2026-08-18 pre-survey — **heuristic, based on folder-name matching, not
verified per-screen.** Always re-check the actual target component with
`grep tourService.register` before assuming a screen's state; if this table
disagrees with reality, trust the code and fix the table.

"FM" = has a Flow Map (`registerFlowMap(...)`). Only Collect Payment does, as
of 2026-08-18.

Work **Phase 1** (add Flow Map only, to every row already marked Tour=Y)
across all modules first — it's the fast win (~20 lines/screen, no anchors
needed). Then **Phase 2** (build Guided Tour + Flow Map from scratch) for
every row marked Tour=N, module by module in the order below. Within each
phase, follow module order top to bottom as listed (mirrors nav-config.ts /
CLAUDE.md module priority).

### Overview
- [x] Dashboard — `/dashboard` — Tour:N FM:N **SKIPPED** — widget-driven, per-role customizable layout (see dashboard-configure.component.ts); no fixed structure to anchor a generic tour to, and content varies too much by role to write one honest walkthrough.
- [x] My Profile — `/profile` — Tour:N FM:N **SKIPPED** — personal self-service screen (view/edit own data); self-explanatory, low incremental value.
- [x] My Timetable — `/my-timetable` — Tour:N FM:N **SKIPPED** — personal read-only schedule view; single-purpose and self-explanatory. Effort better spent on the 21 Tour:N screens in Academics.

### Admission Management (funnel: Enquiries → Finalize Fee → Collect Payment → Submit Documents → Verify Documents → Complete Admission)
- [x] Enquiries — `/enquiries` — Tour:Y FM:Y
- [x] Finalize Fee — `/student-fees/finalize` — Tour:Y FM:Y
- [x] Collect Payment — `/fee-collection` — Tour:Y FM:Y **(done — OC-136/137/138, reference implementation)**
- [x] Submit Documents — `/enquiries/document-submission` — Tour:Y FM:Y
- [x] Verify Documents — `/enquiries/document-verification` — Tour:Y FM:Y
- [x] Complete Admission — `/enquiries/admission-completion` — Tour:Y FM:Y
- [x] Admission Explorer — `/admissions` — Tour:Y FM:Y
- [x] Retro Admit — `/students/retro-admit` — Tour:Y FM:Y

### Student Management
- [x] Student Explorer — `/students` — Tour:Y FM:Y
- [x] Assign Roll Numbers — `/students/roll-numbers` — Tour:Y FM:Y
- [x] Scholarship Applications — `/scholarship-applications` — Tour:Y FM:Y
- [x] Data Import — `/import` — Tour:Y FM:Y

### Finance
- [x] Fee Explorer — `/student-fees` — Tour:Y FM:Y
- [x] Receipts — `/receipts` — Tour:Y FM:Y
- [x] Refunds — `/refund-approvals` — Tour:Y FM:Y
- [x] Commissions — `/commission-explorer` — Tour:Y FM:Y

### Academics
- [x] Curriculum Versions — `/curriculum-versions` — Tour:Y FM:Y
- [x] Syllabus — `/syllabi` — Tour:Y FM:Y
- [x] Experiments — `/experiments` — Tour:Y FM:Y
- [x] CO/PO Mapping — `/curriculum-mappings` — Tour:Y FM:Y
- [x] Course Offerings — `/course-offerings` — Tour:Y FM:Y
- [x] Elective Assignment — `/elective-assignment` — Tour:Y FM:Y
- [x] Lab Schedules — `/lab-schedules` — Tour:Y FM:Y
- [x] Faculty Availability — `/faculty-availability` — Tour:Y FM:Y
- [x] Faculty Workload Rules — `/timetable/workload-rules` — Tour:Y FM:Y
- [x] Skeleton Builder — `/timetable/skeleton-builder` — Tour:Y FM:Y
- [x] Staffing — `/timetable/staffing` — Tour:Y FM:Y
- [x] Capacity Planner — `/timetable/capacity-planner` — Tour:Y FM:Y
- [x] Conflict Inspector — `/timetable/conflict-inspector` — Tour:Y FM:Y
- [x] Timetable Draft Review — `/timetable/draft-review` — Tour:Y FM:Y
- [x] Timetable — `/timetable` — Tour:Y FM:Y
- [x] Resource Timetable — `/timetable/resource-grid` — Tour:Y FM:Y
- [x] Faculty Absence — `/faculty-absence` — Tour:Y FM:Y
- [x] Staff Session Swap — `/timetable/staff-swap` — Tour:Y FM:Y
- [x] My Special Classes — `/timetable/special-classes/my-requests` — Tour:Y FM:Y
- [x] Special Class Approvals — `/timetable/special-classes/approval-queue` — Tour:Y FM:Y
- [x] Attendance — `/attendance` — Tour:Y FM:Y
- [x] Progress Report — `/progress-report` — Tour:Y FM:Y
- [x] Manage Exams — `/examinations` — Tour:Y FM:Y
- [x] Exam Results — `/exam-results` — Tour:Y FM:Y **(pre-existing bug found, unrelated to tour work — see Progress Log)**
- [x] Student Promotion — `/student-promotions` — Tour:Y FM:Y

### Library
- [x] Issue Books — `/library/issues/new` — Tour:Y FM:Y
- [x] Issue Explorer — `/library/issues` — Tour:Y FM:Y
- [x] Overdue Books — `/library/reports` — Tour:Y FM:Y
- [x] Book Explorer — `/library/books` — Tour:Y FM:Y
- [x] Journal Explorer — `/library/periodicals` — Tour:Y FM:Y
- [x] My Library — `/library/my-issues` — Tour:Y FM:Y
- [x] Fines — `/library/fines` — Tour:Y FM:Y
- [x] Racks & Shelves — `/library/racks` — Tour:Y FM:Y
- [x] Import — `/library/import` — Tour:Y FM:Y
- [x] Library Settings — `/library/settings` — Tour:Y FM:Y **(Library module complete)**

### Core Infrastructure
- [ ] Campus Infrastructure — `/campus-infrastructure` — Tour:Y(uncommitted) FM:Y(uncommitted) **BLOCKED on commit — see Progress Log**
- [x] Room Purpose Categories — `/room-purpose-categories` — Tour:Y FM:Y
- [x] Room Sub-Types — `/room-sub-types` — Tour:Y FM:Y
- [x] Branch Diagrams — `/branch-diagrams` — Tour:Y FM:Y
- [x] Floor Plans — `/floor-plans` — Tour:Y FM:Y
- [x] Zone Diagrams — `/zone-diagrams` — Tour:Y FM:Y
- [x] Room Diagrams — `/room-diagrams` — Tour:Y FM:Y

### Inventory Management
- [x] Inventory — `/inventory` — Tour:Y FM:Y
- [x] Maintenance — `/maintenance` — Tour:Y FM:Y

### Hostel Management
- [x] Hostel Room Types — `/hostel-room-types` — Tour:Y FM:Y
- [x] Room Preferences — `/room-preferences` — Tour:Y FM:Y
- [x] Room Allocation — `/room-allocations` — Tour:Y FM:Y

### Reports & Analytics
- [x] General Reports — `/reports` — Tour:Y FM:Y
- [x] Fee Reports — `/fee-reports` — Tour:Y FM:Y

### Preferences
- [x] Academic Calendar — `/academic-calendar` — Tour:Y FM:Y
- [x] Academic Years — `/academic-years` — Tour:Y FM:Y
- [x] Agents — `/agents` — Tour:Y FM:Y
- [x] Blood Groups — `/blood-groups` — Tour:Y FM:Y
- [ ] Classrooms — `/classrooms` — Tour:N FM:N
- [ ] Clinical Venues — `/clinical-venues` — Tour:N FM:N
- [x] Communities — `/communities` — Tour:Y FM:Y
- [x] Courses — `/courses` — Tour:Y FM:Y
- [x] Designations — `/designations` — Tour:Y FM:Y
- [x] Equipment — `/equipment` — Tour:Y FM:Y
- [x] Faculty — `/faculty` — Tour:Y FM:Y
- [x] Faculty Doc Config — `/faculty/document-config` — Tour:Y FM:Y
- [x] Fee Structures — `/fee-structures` — Tour:Y FM:Y
- [ ] Holiday Templates — `/holiday-templates` — Tour:N FM:N
- [x] Institutions — `/institutions` — Tour:Y FM:Y
- [ ] Labs — `/labs` — Tour:N FM:N
- [x] Location Master — `/india-locations` — Tour:Y FM:Y
- [x] Number Sequences — `/number-sequences` — Tour:Y FM:Y
- [ ] Periods — `/periods` — Tour:N FM:N
- [x] Programs — `/programs` — Tour:Y FM:Y
- [x] Referral Types — `/referral-types` — Tour:Y FM:Y
- [x] Scholarship Types — `/scholarships` — Tour:Y FM:Y
- [ ] Settings — `/settings` — Tour:N FM:N
- [x] Specialities — `/specialities` — Tour:Y FM:Y
- [ ] Staff Referrers — `/staff-referrers` — Tour:N FM:N
- [ ] Subjects — `/subjects` — Tour:N FM:N

### User Management
- [ ] Users — `/user-management` — Tour:N FM:N
- [ ] Roles & Permissions — `/role-management` — Tour:N FM:N
- [ ] Permission Tiers — `/permission-tiers` — Tour:N FM:N

## Progress Log

Format: `- YYYY-MM-DD HH:MM | <screen> | DONE|PARTIAL|BLOCKED|SKIPPED | <ticket> | <note>`

- 2026-08-18 18:XX | setup | DONE | OC-139 | Parent ticket created, README written, checklist scaffolded, cron sessions scheduled for 21:00 today and 03:00 tomorrow. No screens started yet — this is the pre-work session.
- 2026-08-18 21:05 | Enquiries, Submit Documents, Verify Documents, Complete Admission, Admission Explorer, Retro Admit | DONE | OC-140 | Phase 1 Flow Maps added to all remaining Admission Management Tour:Y screens. Pipeline screens share one ADMISSION_PIPELINE_FUNNEL (same 6 stages as fee-collection's); Admission Explorer and Retro Admit use single-entry funnels (not pipeline stages). tsc clean. Needs manual light/dark + role check.
- 2026-08-18 21:05 | Student Explorer, Assign Roll Numbers, Scholarship Applications, Data Import | DONE | OC-141 | Phase 1 Flow Maps added to all Student Management Tour:Y screens, sharing one STUDENT_MANAGEMENT_FUNNEL (4 nav-group stages). tsc clean. Needs manual light/dark + role check.
- 2026-08-18 21:06 | Finalize Fee, Fee Explorer, Receipts, Refunds | DONE | OC-142 | Phase 1 Flow Maps added to all Finance Tour:Y screens. Finalize Fee reuses the Admission pipeline funnel (duplicated locally in finance.tours.ts); Fee Explorer/Receipts/Refunds share a new FINANCE_FUNNEL (nav-group order, Commissions included as 4th stage for context even though Commissions itself is Tour:N/Phase 2). tsc clean. Needs manual light/dark + role check.
- 2026-08-18 21:08 | Curriculum Versions, Syllabus, Experiments, Capacity Planner | DONE | OC-143 | Phase 1 Flow Maps added to Academics' 4 Tour:Y screens. Curriculum Versions/Syllabus/Experiments share a new CURRICULUM_AUTHORING_FUNNEL (3-stage). Capacity Planner gets a new TIMETABLE_BUILD_FUNNEL (8-stage, nav-group order) exported from timetable.tours.ts for reuse by the other timetable screens in Phase 2. tsc clean. Needs manual light/dark + role check.
- 2026-08-18 21:10 | Book Explorer, Library Settings | DONE | OC-144 | Phase 1 Flow Maps added to Library's 2 Tour:Y screens, both single-entry funnels (no natural multi-screen journey between them). tsc clean. Needs manual light/dark + role check.
- 2026-08-18 21:14 | Academic Calendar, Academic Years, Agents, Blood Groups, Communities, Courses, Designations, Equipment, Faculty, Faculty Doc Config, Fee Structures, Institutions, Location Master, Number Sequences, Programs, Referral Types, Scholarship Types, Specialities | DONE | OC-145 | Phase 1 Flow Maps added to all 18 Preferences Tour:Y screens — every one is a single-entry funnel per the README's guidance for master/config screens with no natural multi-screen journey. tsc clean across all 18. This closes out Phase 1 entirely — every Tour:Y screen in the checklist now has a Flow Map. Needs manual light/dark + role check.
- 2026-08-18 21:15 | Dashboard, My Profile, My Timetable | SKIPPED | OC-139 | Phase 2 begins. All 3 Overview screens skipped per the judgment-call guidance — see per-screen reasoning in the checklist above. Moving to Phase 2 proper starting with Finance (Commissions) then Academics (21 Tour:N screens, largest remaining chunk).
- 2026-08-18 21:16 | Commissions | DONE | OC-146 | Phase 2: full Guided Tour + Flow Map built from scratch (new commission.tours.ts, 4 tour-anchor ids added to commission-explorer-list.component.html, cms-tour-button wired). Reuses the Finance funnel as its 4th stage. tsc clean.
- 2026-08-18 21:19 | CO/PO Mapping, Course Offerings | DONE | OC-147 | Phase 2 Academics begins. Full Guided Tour + Flow Map built from scratch for both (new co-po-mapping.tours.ts, course-offering.tours.ts), each single-entry funnel. tsc clean.
- 2026-08-18 21:20 | Elective Assignment, Lab Schedules | DONE | OC-147 | Full Guided Tour + Flow Map built from scratch for both. Note: lab-schedule-list.component.html already had a `<cms-tour-button>` placeholder pre-scaffolded with no register() call — checklist's Tour:N was still correct (no tour actually registered) but worth flagging that some templates have dead tour buttons already in place; check for this before assuming a screen needs the button added. tsc clean.
- 2026-08-18 21:22 | Faculty Availability, Faculty Workload Rules | DONE | OC-147 | Full Guided Tour + Flow Map built from scratch for both, both reusing the 8-stage TIMETABLE_BUILD_FUNNEL (currentIndex 0 and 1) so the rail is consistent with Capacity Planner. tsc clean.
- 2026-08-18 21:24 | Skeleton Builder, Staffing | DONE | OC-147 | Full Guided Tour + Flow Map built from scratch for both — the two largest/most complex Academics screens so far (drag-drop grid, subject rail, per-row staffing list). Both use the shared TIMETABLE_BUILD_FUNNEL at currentIndex 2 and 3. tsc clean.
- 2026-08-18 21:26 | Conflict Inspector, Timetable Draft Review | DONE | OC-147 | Full Guided Tour + Flow Map built from scratch for both, sharing the timetable-build funnel at currentIndex 5 and 6. tsc clean. Timetable-build funnel screens remaining: Timetable, Resource Timetable, Faculty Absence, Staff Session Swap, My Special Classes, Special Class Approvals.
- 2026-08-19 03:01 | resume | DONE | OC-139 | Morning continuation session started. Found Timetable + Resource Timetable Grid already implemented in the working tree from the tail end of the night session but never committed (uncommitted .ts/.html changes + 2 new untracked tours files). Verified against tsc (clean) and against the established pattern (matches Conflict Inspector's header/anchor structure) — committed as-is under OC-147. Also found unrelated pre-existing uncommitted WIP (Campus Infrastructure org-editing + uniqueness-validation backend/frontend changes) in the working tree — NOT tour-related, left untouched/unstaged, not part of this task. Timetable uses the 8-stage TIMETABLE_BUILD_FUNNEL at currentIndex 7 (last stage). Resource Timetable introduces a new 5-stage TIMETABLE_OPERATIONS_FUNNEL (day-to-day ops group) at currentIndex 0, to be shared by Faculty Absence, Staff Session Swap, My Special Classes, Special Class Approvals next. Continuing Academics Phase 2 with those 4 screens, then Attendance/Progress Report/Manage Exams/Exam Results/Student Promotion.
- 2026-08-19 03:04 | Faculty Absence, Staff Session Swap, My Special Classes, Special Class Approvals | DONE | OC-147 | Full Guided Tour + Flow Map built from scratch for all 4, closing out the 5-stage TIMETABLE_OPERATIONS_FUNNEL entirely (Resource Timetable was stage 0 from the earlier commit). New tours files: faculty-absence.tours.ts, staff-session-swap.tours.ts, special-class.tours.ts (holds both My Special Classes and Special Class Approvals — admin/faculty pair for the same feature, same as other paired screens in this rollout). tsc clean after each commit. Remaining in Academics: Attendance, Progress Report, Manage Exams, Exam Results, Student Promotion — none of these are part of the timetable-build/operations funnels, so each will get its own single-entry (or small local) funnel.
- 2026-08-19 03:08 | Attendance, Progress Report, Manage Exams, Exam Results, Student Promotion | DONE | OC-147 | Full Guided Tour + Flow Map built from scratch for all 5, closing out **all of Academics Phase 2** — every screen in the module checklist is now Tour:Y FM:Y. New tours files: attendance.tours.ts, progress-report.tours.ts, examination.tours.ts (holds both Manage Exams and Exam Results, sharing a small local EXAMINATION_FUNNEL), student-promotion.tours.ts (3-step wizard: select cohort/terms → review eligibility → execute → result, anchors on all 3 conditional step containers plus the execute bar). tsc clean after each commit. **Flagging a pre-existing bug found while touching Exam Results (`exam-result-list.component.ts`), unrelated to tour work and NOT fixed in this session**: `selectedExamId` (which gates the whole results table behind `@if`) is only ever set by `onExaminationChange()`, but that method is never called from the template — there is no exam-picker UI anywhere in the component, and nothing elsewhere in the app links to `/exam-results` with a way to select an exam. The `examinations` signal loads data that's never rendered. Net effect: the Exam Results screen currently shows nothing and cannot be made to show anything via the UI as it exists. This is a functional/product-design gap (what should the picker look like — dropdown in toolbar? row action from Manage Exams?) requiring a design decision, not a same-day bug fix, so left as-is with the tour built honest to current (broken) state. Ticket OC-147 (Academics Phase 2) now moved to review. Next: Library module (Phase 2) — checklist shows Issue Books, Issue Explorer, Overdue Books, Journal Explorer, My Library, Fines, Racks & Shelves, Import all Tour:N (Book Explorer and Library Settings already done in Phase 1).
- 2026-08-19 03:15 | Issue Books, Issue Explorer, Overdue Books, Fines, Journal Explorer, My Library, Racks & Shelves, Import | DONE | OC-148 | Full Guided Tour + Flow Map built from scratch for all 8, closing out **all of Library Phase 2** — every screen in the module is now Tour:Y FM:Y. New tours files: library-circulation.tours.ts (Issue Books/Issue Explorer/Overdue Books/Fines sharing a new LIBRARY_CIRCULATION_FUNNEL, mirroring Admission Management's real-pipeline idiom), library-periodical.tours.ts, library-my-issues.tours.ts, library-rack.tours.ts, library-import.tours.ts (each single-entry, matching the Book Explorer/Settings precedent for non-pipeline screens). tsc clean after each commit. Library module fully closed. Ticket OC-148 moved to review. Next: Core Infrastructure module (Phase 2) — Campus Infrastructure, Room Purpose Categories, Room Sub-Types, Branch Diagrams, Floor Plans, Zone Diagrams, Room Diagrams, all Tour:N.
- 2026-08-19 03:20 | Branch Diagrams, Floor Plans, Zone Diagrams, Room Diagrams, Room Purpose Categories, Room Sub-Types | DONE | OC-149 | Full Guided Tour + Flow Map built and committed for 6 of 7 Core Infrastructure screens. Branch/Floor/Zone/Room Diagrams share one physical component (`FloorPlanListComponent`, route `data.level` = BRANCH/FLOOR/ZONE/ROOM) — built `floor-plan-list.tours.ts` with per-level builder functions (`buildFloorPlanListTour(level)`/`buildFloorPlanListFlowMap(level)`) instead of 4 static exports, each level still registers under its own distinct tourKey (`branch-diagrams`/`floor-plans`/`zone-diagrams`/`room-diagrams`) so all 4 routes work independently. Room Purpose Categories + Room Sub-Types share a small local ROOM_PURPOSE_FUNNEL (2-tier BR-54 master). tsc clean, both committed. **Campus Infrastructure (7th screen) is BLOCKED, not committed**: `campus-setup.component.html`/`.ts` already had substantial *pre-existing, unrelated* uncommitted changes in the working tree before this session started (an organization-CRUD feature — add/edit Organization, plus matching backend `CampusInfrastructureController`/`Service` uniqueness-check endpoints — none of it mine, not tour-related, not verified/tested by me). My tour anchors/registration are now layered into the same two files on top of that WIP, so `git add <file>` would stage and commit the unrelated feature together with the tour work, violating scope. Left both files **uncommitted** (tour code is written and tsc-clean, just sitting in the working tree) along with a new `campus-setup.tours.ts` (also uncommitted, so all the campus-setup tour pieces stay together for whoever picks this up). Did not touch/attempt to complete or revert the pre-existing org-CRUD WIP — that decision belongs to a human. Ticket OC-149 not yet moved to review since one screen is outstanding. Next: Inventory Management (Inventory, Maintenance) then Hostel Management (Hostel Room Types, Room Preferences, Room Allocation), then Reports & Analytics, then remaining Preferences (Classrooms, Clinical Venues, Holiday Templates, Labs, Periods, Settings, Staff Referrers, Subjects), then User Management (Users, Roles & Permissions, Permission Tiers).
- 2026-08-19 03:22 | Inventory, Maintenance | DONE | OC-150 | Full Guided Tour + Flow Map built from scratch for both, closing **Inventory Management module**. Both are simple single-entry-funnel list screens (new inventory.tours.ts holds both). tsc clean, committed. Next: Hostel Management (Hostel Room Types, Room Preferences, Room Allocation).
- 2026-08-19 03:25 | Hostel Room Types, Room Preferences, Room Allocation | DONE | OC-151 | Full Guided Tour + Flow Map built from scratch for all 3, closing **Hostel Management module**. New hostel-management.tours.ts, all 3 sharing a real HOSTEL_MANAGEMENT_FUNNEL (Room Types master → Preferences request → Allocation fulfillment). tsc clean, committed. Next: Reports & Analytics (General Reports, Fee Reports), then remaining Preferences masters, then User Management.
- 2026-08-19 03:27 | General Reports, Fee Reports | DONE | OC-152 | Full Guided Tour + Flow Map built from scratch for both, closing **Reports & Analytics module**. New reports.tours.ts. Along the way, found and fixed a badge-audit-gate violation in Fee Reports (`fee-reports-dashboard.component.html`): 3 status badges used `cms-badge--soft-warning`/`soft-error`/`soft-success`/`soft-default`, none of which are defined anywhere in styles.scss (exact repeat of the documented soft-* bug pattern, just not previously caught on this screen) — remapped to the real defined classes (`soft-amber`/`soft-red`/`soft-green`/`soft-gray`) across all 3 occurrences (Outstanding Fees status pill, Student Ledger ID chip, Ledger entry status pill). tsc clean, committed. Next: remaining Preferences masters (Classrooms, Clinical Venues, Holiday Templates, Labs, Periods, Settings, Staff Referrers, Subjects), then User Management (Users, Roles & Permissions, Permission Tiers) — the last module in the checklist.
