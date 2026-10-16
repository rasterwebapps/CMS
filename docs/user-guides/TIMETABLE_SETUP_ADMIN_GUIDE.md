# Timetable Setup — Admin Guide (OneCMS)

**Audience:** College admin setting up a term's timetable for the first time (or a new term).
**Rule of thumb:** work top to bottom. Each stage feeds the next; skipping ahead is what produces
"unassigned", "no committed room" and "over capacity" blockers later.

Menu paths are written as **Group › Menu item**. A menu item only appears if your role holds its
permission and the module is enabled (Role Management is DB-driven — ask the role owner, not a developer).

---

## Stage 0 — Foundations (one-time, rarely changed)

| # | Menu › Item | What to configure | Why the timetable needs it |
|---|---|---|---|
| 0.1 | **Preferences › Programs** | Programs / courses (e.g. BSc Nursing) | Cohorts and curriculum hang off a program |
| 0.2 | **Preferences › Specialities** | Departments / specialities | Drives which faculty are eligible for a subject |
| 0.3 | **Preferences › Subjects** | Every subject; set **Eligible Labs**, **Eligible Clinical Venues**, **Eligible Faculty** | Staffing only considers eligible faculty/venues. The seeded `SYSTEM-SPORTS` subject needs your PE faculty ticked under Eligible Faculty |
| 0.4 | **Preferences › Periods** | The daily period grid (start/end of each period, lunch/recess) | Every placement is on this grid. **Do not edit Period durations once a term is live** (corrupts history) |
| 0.5 | **Core Infrastructure › Campus Infrastructure** (+ **Room Purpose Categories**, **Room Sub-Types**) | Branch → Block → Floor → Zone → Room hierarchy and room purpose | Classrooms, labs, clinical venues sit on this |
| 0.6 | **Preferences › Classrooms** | Every teaching room with its **seating capacity**. Tag at least one room **Sports & Recreation** | Theory is staffed into classrooms by capacity; Sports sessions need a Sports & Recreation room |
| 0.7 | **Preferences › Labs** | Labs with capacity | Lab batches are fitted to lab capacity |
| 0.8 | **Preferences › Clinical Venues** | Hospitals / postings with capacity | Clinical batches and shift groups are fitted to these |
| 0.9 | **Preferences › Designations** and **Preferences › Faculty** | Faculty records with designation and speciality | Staffing candidates; designation drives default weekly workload cap |
| 0.10 | **Preferences › Faculty Workload Rules** | Daily / weekly / continuous-run teaching caps (institution-wide; weekly can be overridden per designation or per faculty) | Hard limits during staffing. Blank = no cap |

## Stage 1 — Academic calendar for the term

| # | Menu › Item | What to configure | Notes |
|---|---|---|---|
| 1.1 | **Preferences › Academic Years** | Create the academic year and its **terms** (e.g. 2026-2027 ODD, start/end dates). Seat allocation per cohort lives here | Term dates must not overlap institution-wide |
| 1.2 | **Preferences › Holiday Templates** | Reusable holiday sets | Holidays are treated as non-teachable days |
| 1.3 | **Preferences › Academic Calendar** | Add events/holidays for the year. Use the **Blocked Periods** view for recurring/one-off locks (e.g. every Wednesday afternoon, exam week) and **Day Mappings** for "this Saturday runs Monday's schedule" | Blocked periods are never scheduled |

## Stage 2 — Curriculum (what must be taught)

Order matters: each item is a child of the one above it.

| # | Menu › Item | What to configure |
|---|---|---|
| 2.1 | **Academics › Curriculum Versions** | One version per program/cohort-batch; map subjects to each term with hours (Theory / Lab / Clinical), define **Elective Groups**, attendance thresholds. Every active cohort needs a version, or the term cannot generate offerings |
| 2.2 | **Academics › Syllabus** | Syllabus rows per subject |
| 2.3 | **Academics › Experiments** | Lab experiments per subject |
| 2.4 | **Academics › CO/PO Mapping** | Outcome mapping (hangs off Experiments) |

## Stage 3 — Open the term and generate offerings

| # | Where | Action |
|---|---|---|
| 3.1 | **Academic Years** (term row) | Advance the term **PLANNED → OPEN** and tick the term-advance checklist. This **auto-generates Course Offerings** from the curriculum. Do this only when stage 2 is complete — you cannot "undo" an open term |
| 3.2 | **Academics › Student Promotion** | Promote/enrol existing students into the new term (sole path for next-term enrolment). New admissions come through **Admission Management** |
| 3.3 | **Academics › Course Offerings** | Verify one offering per subject per cohort. Per offering you can set: **Clinical Shift groups** (duty days/length, for Clinical subjects) and **Escort Rotation** (transport/escort duty) |
| 3.4 | **Academics › Elective Assignment** | For each elective group choose **Institution decided** (bulk-assign the option to the cohort) or student choice. Do this *before* faculty and capacity — only options with real students need a teacher |

## Stage 4 — Capacity, rooms and batches

| # | Menu › Item | What to do |
|---|---|---|
| 4.1 | **Academics › Capacity Auto-Plan** | Pick year → term → cohort. Review the proposed **Theory sections** (home classroom each) and **Lab / Clinical batches** (venue each), adjust if needed, then **Confirm & Commit** per cohort. This is the **only** place batches are created. Commit is an exclusive term-wide claim on each room |
| 4.2 | (linked from 4.1) **Adjust manually** → Capacity Planner | Use for real restructuring; also shows the Faculty Workload advisory tab |

> Do this **before** Assign Faculty: it decides whether Theory splits into multiple sections, and the
> per-section faculty picker only exists once sections exist.

## Stage 5 — Faculty

| # | Menu › Item | What to do |
|---|---|---|
| 5.1 | **Academics › Assign Faculty** | Assign Theory faculty (per section where sections exist) and the **coordinator** for each Lab/Clinical batch, for every offering |
| 5.2 | **Academics › Recurring Unavailability** | Standing weekly blocks (e.g. external duty every Tuesday). Hard constraint in staffing. *(One-off leave is **Faculty Absence**, used later.)* |

## Stage 6 — Build the timetable

| # | Menu › Item | What to do |
|---|---|---|
| 6.1 | **Academics › Timetable Builder** | Select year + term. Every cohort starts **Pending**. |
| 6.2 | Toolbar › **Configure Working Saturdays** | Choose none, or any pattern (1st, 2nd, nth, all). Chosen Saturdays count as regular days; none = Monday–Friday |
| 6.3 | Toolbar › **Run Global Auto-Schedule** (All cohorts) or **Run** on one Pending row | Confirm the overwrite prompt if a draft exists, then work the **prerequisite checklist** (below). Tick every item + acknowledgment, then Run. The run places *and* staffs all cohorts |
| 6.4 | Same screen | Read the run report. Fix gaps by dragging sessions (move/swap whole blocks), clicking to remove, or **Reassign Faculty**. Pin sessions you want to keep |
| 6.5 | Row action **Check & Resolve Conflicts** | Must come back clean → status **Conflicts Resolved** |
| 6.6 | Row action **Publish** | Status **Published**. *Revert to Draft* / *Discard* are available only until attendance is recorded |

Status flow per cohort: **Pending → Drafted → Conflicts Resolved → Published**.

### The run's prerequisite checklist (hard blocks must be cleared first)

| Check | Hard block? | Fix it in |
|---|---|---|
| Every offering has faculty | Yes | Assign Faculty |
| No faculty over term capacity | Yes | Assign Faculty / Faculty Workload Rules |
| Faculty at ~100% capacity | Warning | Spread load or add faculty |
| Lab/Clinical venue over weekly capacity | Yes | Clinical Venues / Labs / Capacity Auto-Plan |
| Venue at ~100% | Warning | Same |
| Cohort/day with zero periods left after Clinical Shift | Yes | Clinical Shift group (Course Offerings) |
| Cohort/day with one period left | Warning | Same |
| Cohort has committed room allocation | Yes | Capacity Auto-Plan (stage 4) |
| Total faculty capacity covers term demand | Warning | More faculty, hours or Saturdays |

## Stage 7 — After publish (on-demand)

| Menu › Item | Use |
|---|---|
| **Overview › My Timetable (Student / Staff)** | What students and faculty see |
| **Academics › Timetable** | Browse the published timetable |
| **Academics › Resource Timetable** | Faculty-wise and classroom-wise grids |
| **Academics › Class Schedules** | Date-wise occurrences; **Swap** or **Reschedule** a single session (no create/delete) |
| **Academics › Faculty Absence** | Mark one-off leave and apply a substitute (fully manual) |
| **Academics › My Special Classes** / **Special Class Approvals** | Faculty request extra/remedial classes (theory, lab or clinical); admin approves |
| **Academics › My Escort Duties** | Escort rotation duties |
| **Academics › Attendance**, **Progress Report** | Attendance marking and portion tracking against the timetable |

## Quick order-of-operations (print this)

1. Periods, Classrooms (+Sports room), Labs, Clinical Venues, Subjects, Faculty, Workload Rules
2. Academic Year + terms → Holidays → Academic Calendar (Blocked Periods / Day Mappings)
3. Curriculum Versions (+ elective groups) → Syllabus → Experiments → CO/PO
4. Open the term (offerings auto-generate) → Student Promotion / admissions
5. Course Offerings (Clinical Shift, Escort) → Elective Assignment
6. **Capacity Auto-Plan** (commit rooms and batches)
7. **Assign Faculty** → Recurring Unavailability
8. **Timetable Builder**: Working Saturdays → Run → fix → Check Conflicts → Publish

## Common blockers

| Message | Meaning | Go to |
|---|---|---|
| "N offerings have no faculty assigned" | Stage 5 incomplete | Assign Faculty |
| "N cohorts have no committed room allocation" | Stage 4 incomplete | Capacity Auto-Plan |
| "Faculty over term capacity" | Too much load on one person | Assign Faculty / Workload Rules |
| "Zero periods left after Clinical Shift" | Duty window covers the whole day | Clinical Shift group |
| Course Offerings list empty | Term not OPEN, or cohort missing a curriculum version | Academic Years / Curriculum Versions |
| Elective group "no option assigned" | Institution-decided group has no bulk assignment | Elective Assignment |
| Sports / Library shown as grey notes | No Sports room or PE faculty, or week too full | Classrooms tag / Subject eligible faculty (informational only) |
