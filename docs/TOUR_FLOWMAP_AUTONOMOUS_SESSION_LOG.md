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
- [ ] Dashboard — `/dashboard` — Tour:N FM:N (low priority — highly role-variable content, judgment call whether a generic tour adds value; use best judgment, log the decision either way)
- [ ] My Profile — `/profile` — Tour:N FM:N (low priority, same reasoning)
- [ ] My Timetable — `/my-timetable` — Tour:N FM:N

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
- [ ] Commissions — `/commission-explorer` — Tour:N FM:N

### Academics
- [ ] Curriculum Versions — `/curriculum-versions` — Tour:Y FM:N
- [ ] Syllabus — `/syllabi` — Tour:Y FM:N
- [ ] Experiments — `/experiments` — Tour:Y FM:N
- [ ] CO/PO Mapping — `/curriculum-mappings` — Tour:N FM:N
- [ ] Course Offerings — `/course-offerings` — Tour:N FM:N
- [ ] Elective Assignment — `/elective-assignment` — Tour:N FM:N
- [ ] Lab Schedules — `/lab-schedules` — Tour:N FM:N
- [ ] Faculty Availability — `/faculty-availability` — Tour:N FM:N
- [ ] Faculty Workload Rules — `/timetable/workload-rules` — Tour:N FM:N
- [ ] Skeleton Builder — `/timetable/skeleton-builder` — Tour:N FM:N
- [ ] Staffing — `/timetable/staffing` — Tour:N FM:N
- [ ] Capacity Planner — `/timetable/capacity-planner` — Tour:Y FM:N
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
- [ ] Book Explorer — `/library/books` — Tour:Y FM:N
- [ ] Journal Explorer — `/library/periodicals` — Tour:N FM:N
- [ ] My Library — `/library/my-issues` — Tour:N FM:N
- [ ] Fines — `/library/fines` — Tour:N FM:N
- [ ] Racks & Shelves — `/library/racks` — Tour:N FM:N
- [ ] Import — `/library/import` — Tour:N FM:N
- [ ] Library Settings — `/library/settings` — Tour:Y FM:N

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
- [ ] Academic Calendar — `/academic-calendar` — Tour:Y FM:N
- [ ] Academic Years — `/academic-years` — Tour:Y FM:N
- [ ] Agents — `/agents` — Tour:Y FM:N
- [ ] Blood Groups — `/blood-groups` — Tour:Y FM:N
- [ ] Classrooms — `/classrooms` — Tour:N FM:N
- [ ] Clinical Venues — `/clinical-venues` — Tour:N FM:N
- [ ] Communities — `/communities` — Tour:Y FM:N
- [ ] Courses — `/courses` — Tour:Y FM:N
- [ ] Designations — `/designations` — Tour:Y FM:N
- [ ] Equipment — `/equipment` — Tour:Y FM:N
- [ ] Faculty — `/faculty` — Tour:Y FM:N
- [ ] Faculty Doc Config — `/faculty/document-config` — Tour:Y FM:N
- [ ] Fee Structures — `/fee-structures` — Tour:Y FM:N
- [ ] Holiday Templates — `/holiday-templates` — Tour:N FM:N
- [ ] Institutions — `/institutions` — Tour:Y FM:N
- [ ] Labs — `/labs` — Tour:N FM:N
- [ ] Location Master — `/india-locations` — Tour:Y FM:N
- [ ] Number Sequences — `/number-sequences` — Tour:Y FM:N
- [ ] Periods — `/periods` — Tour:N FM:N
- [ ] Programs — `/programs` — Tour:Y FM:N
- [ ] Referral Types — `/referral-types` — Tour:Y FM:N
- [ ] Scholarship Types — `/scholarships` — Tour:Y FM:N
- [ ] Settings — `/settings` — Tour:N FM:N
- [ ] Specialities — `/specialities` — Tour:Y FM:N
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
