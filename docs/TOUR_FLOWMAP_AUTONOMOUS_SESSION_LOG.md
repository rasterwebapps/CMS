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
- [ ] Conflict Inspector — `/timetable/conflict-inspector` — Tour:N FM:N
- [ ] Timetable Draft Review — `/timetable/draft-review` — Tour:N FM:N
- [ ] Timetable — `/timetable` — Tour:N FM:N
- [ ] Resource Timetable — `/timetable/resource-grid` — Tour:N FM:N
- [ ] Faculty Absence — `/faculty-absence` — Tour:N FM:N
- [ ] Staff Session Swap — `/timetable/staff-swap` — Tour:N FM:N
- [ ] My Special Classes — `/timetable/special-classes/my-requests` — Tour:N FM:N
- [ ] Special Class Approvals — `/timetable/special-classes/approval-queue` — Tour:N FM:N
- [ ] Attendance — `/attendance` — Tour:N FM:N
- [ ] Progress Report — `/progress-report` — Tour:N FM:N
- [ ] Manage Exams — `/examinations` — Tour:N FM:N
- [ ] Exam Results — `/exam-results` — Tour:N FM:N
- [ ] Student Promotion — `/student-promotions` — Tour:N FM:N

### Library
- [ ] Issue Books — `/library/issues/new` — Tour:N FM:N
- [ ] Issue Explorer — `/library/issues` — Tour:N FM:N
- [ ] Overdue Books — `/library/reports` — Tour:N FM:N
- [x] Book Explorer — `/library/books` — Tour:Y FM:Y
- [ ] Journal Explorer — `/library/periodicals` — Tour:N FM:N
- [ ] My Library — `/library/my-issues` — Tour:N FM:N
- [ ] Fines — `/library/fines` — Tour:N FM:N
- [ ] Racks & Shelves — `/library/racks` — Tour:N FM:N
- [ ] Import — `/library/import` — Tour:N FM:N
- [x] Library Settings — `/library/settings` — Tour:Y FM:Y

### Core Infrastructure
- [ ] Campus Infrastructure — `/campus-infrastructure` — Tour:N FM:N
- [ ] Room Purpose Categories — `/room-purpose-categories` — Tour:N FM:N
- [ ] Room Sub-Types — `/room-sub-types` — Tour:N FM:N
- [ ] Branch Diagrams — `/branch-diagrams` — Tour:N FM:N
- [ ] Floor Plans — `/floor-plans` — Tour:N FM:N
- [ ] Zone Diagrams — `/zone-diagrams` — Tour:N FM:N
- [ ] Room Diagrams — `/room-diagrams` — Tour:N FM:N

### Inventory Management
- [ ] Inventory — `/inventory` — Tour:N FM:N
- [ ] Maintenance — `/maintenance` — Tour:N FM:N

### Hostel Management
- [ ] Hostel Room Types — `/hostel-room-types` — Tour:N FM:N
- [ ] Room Preferences — `/room-preferences` — Tour:N FM:N
- [ ] Room Allocation — `/room-allocations` — Tour:N FM:N

### Reports & Analytics
- [ ] General Reports — `/reports` — Tour:N FM:N
- [ ] Fee Reports — `/fee-reports` — Tour:N FM:N

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
