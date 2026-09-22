# Timetable — Decision Log

This file is the append-only chronological record of scope/behaviour decisions behind the timetable engine (Skeleton Builder, Global Auto-Schedule, Staffing), so the reasoning survives independently of whichever code or doc is being edited. The requirements themselves live in `docs/BUSINESS_REQUIREMENTS.md` (BR-55 to BR-58).

## Rules for this file

1. Append only — never edit or delete a past entry. If a decision is later reversed, add a new entry that says so and references the old one.
2. Every entry needs a date, what prompted it, the decision(s) made, the evidence behind it, and the impact (what doc/code changed).
3. Cross-update BR-56 in `BUSINESS_REQUIREMENTS.md` in the same change whenever a decision here changes placement behaviour.

---

## 2026-09-11 — Monday–Friday is the destination (OC-227)

**Prompted by:** the 2026-2027 ODD Global Auto-Schedule run for BSc Nursing. The user saw 906.7 h "available", many "unassigned" sessions and orange warnings even after opting into "6 Saturdays", and set the goal: *"We must find an idea to bring in all the hours into the Mon–Fri slots itself — that will be the final destination."*

**Evidence (local DB, the exact 17:48 IST run):**
- **906.7 h was correct arithmetic.** 8 × 50-minute periods = 6.67 h a day. The term runs 2026-10-01 to 2027-03-31, which is 26 weeks. "6 Saturdays" was the *1st Saturday only* pattern, which gives just 6 working days. So 6.67 h × (26 × 5 + 6) days = 906.7 h, and those Saturdays added only 40 h.
- **The orange list was mostly false.** It was an append-only log of mid-run failures. Every subject listed as "displaced… could not be placed" was at or above its weekly quota in the finished grid. The genuine gaps were:
  - one Community Health Nursing I session a week (2024-2028);
  - 4–12 h of Clinical in 6 of 7 clinical subjects;
  - Elective II not placed;
  - one Library session for 2025-2029.
- **Saturday had become the preferred day.** An uncommitted change ranked an empty Saturday by day-load, so Library landed there for three cohorts, and so did a Community Health Nursing I session. Under a 1st-Saturday pattern each such session runs 6 times instead of 26.
- **Elective II had 9 options and 60 students, all in one option.** It was already *Institution decided*, but automation ignored that. It demanded 9 free 60-seat rooms at one period, and the college owns 5 classrooms (Rooms 101–104 and Library Hall).
- **Clinical fell short by a few hours in every subject.** A 6 h duty × 26 weeks = 156 h against 160 h units. A 6 h 10 m duty closes it, and the bus is back at 14:10, exactly when Period 6 starts. `ClinicalShiftWindow.overlaps` is half-open, so this costs zero periods.
- **Capacity by cohort.** Sem 3, 5 and 7 all have hospital duty Mon/Wed/Fri, which leaves 25 usable periods a week; Sem 1 has 35. Sem 5 needed 27, over by 2: rounding each subject up to whole weekly sessions adds 2 periods, and Library takes 4.

**Decisions**, by specialist round:
- **Product Owner:**
  - All elective groups are management-selected, and the chosen option becomes common to the whole cohort. Group 11 is switched in the UI.
  - Plan Monday–Friday by default. Add Saturdays as regular days only if Monday–Friday is never possible. Special Classes cover short-notice catch-up.
  - Library shrinks to fit.
  - Clinical: both remedies are acceptable — duty-length fit and extra duty days.
- **Backend Architect:**
  - The elective selection is the bulk assignment (existing registrations); no new field.
  - Allow a back-to-back double on a used weekday before Saturday.
  - Filler may fill a Saturday only once curriculum has opened it.
  - Duty fit is a suggestion plus an Apply button behind its own permission.
- **Frontend Architect:**
  - The Apply button lives in the run report.
  - A management-selected elective renders as a regular cell with an "Elective" tag.
  - The Library shrink is a grey info line.
  - The Saturday notice shows the hours impact and the action to take.
- **DBA:**
  - Group 11 is flipped by an admin in the UI on every environment; no data migration.
  - No schema change beyond one permission row (V503).
- **QA:**
  - Unit tests under the JaCoCo 90% gate.
  - A local Global Auto-Schedule re-run for verification.
  - "Done" means everything fits Monday–Friday, or the report names the exact blocker.
- **Documentation:**
  - Update BR-56, write this log, and add manual test cases (`docs/manual-test-cases/timetable-global-auto-schedule.md`).
  - Tracked as OC-227.

**Impact:**
- **`TimetableGlobalAutoScheduleService`:**
  - Placement order: Saturday goes last in `tryPlaceAndStaff`, and a same-day double comes before Saturday in `placeShortfallRow` and the backtrack/restore paths.
  - Filler Saturday gate (`curriculumOpenedSaturday`).
  - Library notes, via `libraryInfoNotes`.
  - Common-elective handling in Phase 0; Phase 3 skips options with 0 students.
  - `computeDutyFit`/`applyClinicalDutyFit`.
  - `saturdaySessionNotices`.
  - `reconcileUnplacedAgainstFinalPlacements`.
  - Phase 5 mending now only swaps with a row that can afford the move.
- **`TimetableSkeletonService.isCommonCohortElective`**, used by the placement/move checks and by `TimetableStaffingService`, so the section's own room is used.
- **New endpoint** `POST /timetables/skeleton/clinical-duty-fit/{courseOfferingId}`.
- **DTOs:** new fields on `ClinicalResidualItem`/`CohortPlacementSummary`, plus a new `SaturdaySessionNotice`.
- **Frontend:** the run report, the grid tag, and elective hours counting on the hours card.
- **Removed:** `WorkingSaturdayCalculator.isEverySaturdayWorking`/`totalSaturdayCount`, added by the reverted Saturday-ranking change.

---

## 2026-09-15 — Chosen working Saturdays are regular days (OC-227 follow-up)

**Prompted by:** after the 2026-09-11 change, the 2026-2027 ODD term had the *1st Saturday only* pattern chosen, but automation never put anything on it, because everything already fitted Monday–Friday. The user: *"If the user deliberately wants to choose all Saturdays, it must also be treated as a regular working day and the offerings are split evenly to occupy those periods also"*, then clarified *"I mean all first or second or any saturdays"* and *"the user has provision to choose none or any saturdays"*.

**Reverses:** the 2026-09-11 decisions "Add Saturdays as regular days only if Monday–Friday is never possible", "Allow a back-to-back double on a used weekday before Saturday" and "Filler may fill a Saturday only once curriculum has opened it", and the Phase 5 Saturday-upgrade mending pass.

**Decisions** (Product Owner round):
- **Hours:** spread evenly Monday–Saturday. A session on a partial-pattern Saturday delivers fewer hours, and the run report shows the gap (Saturday hours notice). Chosen over "Monday–Friday full, Saturday extra" and "count real Saturday dates".
- **Session types:** Theory, Lab, Clinical and Library all use Saturday.
- **Periods:** a working Saturday runs the same periods as a weekday (full day).
- **No Saturday chosen:** Monday–Friday only, unchanged.
- Other specialists: no schema change, no new permission, no new endpoint.

**Impact:**
- **`TimetableGlobalAutoScheduleService`:**
  - New `saturdayIsWorkingDay`/`workingDays`. Every day list uses them: `tryPlaceAndStaff`, `tryPairOfferings`, `fillLibraryGaps`, `fillSelfStudyGaps` and the Library relocation.
  - `placeShortfallRow`, `attemptBacktrack` and the bumped-row re-place: the Saturday-after-double fallback is gone, and a double may go on Saturday like any day.
  - Removed `curriculumOpenedSaturday`, `excludingSaturday` and the Phase 5 mending pass (`attemptSaturdayUpgradeMending`, `upgradeOneSaturdayPlacement`, `tryUpgradeSwap`).
  - `saturdaySessionNotices` returns nothing when every Saturday is working.
- **Frontend:** the report no longer says "some placed on Saturday — Mon–Fri didn't have room"; the Saturday notice says the session "is on Saturday".
- **Docs:** BR-56 Monday–Friday rules 1, 2 and 6; manual test cases TC-GAS-002 to TC-GAS-004 and TC-GAS-013.

---

## 2026-09-15 — Hours cards: Available is every working day's clock time (OC-227 follow-up)

**Prompted by:** BSc Nursing (2025-2029), 2026-2027 ODD showed *Total Assigned 979.3 h* against *Total Hours Available 906.7 h*. The user: *"Available becomes the total working days hours, assigned will always be lesser than available, right?"*

**Evidence:** Available counted only the 8 periods (09:00–16:55) × (5 × 26 + 6) days. Clinical counted its full duty length (07:00–13:10, 481 h), about 2 h of it before Period 1. Theory showed 411.7 h against 260 h because the extra-hours filler was counted as assigned. What the periods actually held (Theory + Lab) was 498.4 h.

**Decisions:**
- *Available* = every working day's clock time: all periods plus Clinical duty time outside them, overlaps once, bus buffer excluded, Saturday only on working Saturdays.
- *Assigned* = curriculum hours capped per subject; hours beyond curriculum show as "+N h extra"; *Unassigned* is summed per subject.

**Impact:** `SkeletonBuilderComponent.hoursSummary` and its template only; no backend or placement change. Manual test case TC-GAS-014.

---

## 2026-09-15 — Plan against the term's total hours (OC-227 follow-up)

**Prompted by:** the next 2026-2027 ODD All-Cohorts run with *1st Saturday only* chosen. Every cohort's report listed orange lines like *"one weekly session is on Saturday, and only 6 of this term's Saturdays are working days, so it runs 6 times instead of 26: 5h of 21.7h"*. BSc Nursing (2025-2029) showed Theory 245 h / 260 h, 15 h unassigned, while +196.7 h of extra-hours filler sat on Monday–Friday. Asked how partial Saturdays should be used, the user answered: *"You must plan based on the total hours available, if 26 weeks with mon-fri 26\*5\*8=1040 periods, if first saturday is chosen, 26\*5\*8 + 6\*8 becomes the total available hours! there is no need to see if it mon-fri or saturday, Or (place theory on Mon-Fri and place Library on Saturday) nothing like this"*.

**Cause:**
- Every budget was counted in weekly sessions. A session on Saturday counted as one full weekly session.
- An empty Saturday is always the least-loaded day, so one weekly session per subject went there. The subject then read as met while running on 6 of 26 weeks.
- The Saturday hours notice from the entry above reported that gap, but nothing ever closed it. The weekday room that could have closed it went to filler.

**Reverses:** the hours part of the first 2026-09-15 entry ("spread evenly Monday–Saturday; a session on a partial-pattern Saturday delivers fewer hours, and the run report shows the gap"). Saturday stays a regular working day. Also rejected, and never to be re-proposed: "curriculum on Monday–Friday, Library/extras on Saturday".

**Decisions:**
- A weekly slot is worth the number of times it really runs in the term: the term's weeks for Monday–Friday, and the chosen working Saturdays for Saturday.
- A subject is placed once its sessions' runs cover its curriculum hours, whatever days they sit on. A subject with a first-Saturday session simply gets placed on another day as well.
- No Saturday hours notice. Weekday and Saturday are the same kind of capacity, so "is on Saturday" is never a warning. Only a genuine shortfall (the term's total periods can't hold the hours) is reported.

**Impact:**
- **`SkeletonSubjectBudget`:** new `requiredTermRuns`, `deliveredTermRuns` and `deliveredHours`, plus `remainingTermRuns()`/`isMet()`. The weekly figures stay for display.
- **`TimetableSkeletonService`:**
  - Budgets and the `checkBudgetNotExceeded` placement cap count real runs (`deliveredRuns`). A placement is refused only once the hours are met, so it overshoots by at most one session.
  - The replace-dialog shortfall (`DisplacedSubjectShortfall`) is now in hours.
- **`TimetableGlobalAutoScheduleService`:**
  - `placeShortfallRow` subtracts each placement's real runs (`runsFor`). `attemptBacktrack` returns the day it used.
  - A bumped session is never restored onto a day that runs less often than its original one.
  - Lab pairing on a Saturday re-queues the runs still owed.
  - `reconcileUnplacedAgainstFinalPlacements` counts in runs.
  - `saturdayIsWorkingDay` also requires the pattern to match at least one Saturday in the term.
- **Removed:** `saturdaySessionNotices`, `SaturdaySessionNotice` and `CohortPlacementSummary.saturdaySessions`.
- **New helpers:** `WorkingSaturdayCalculator.runsInTerm` and `CurriculumHoursCalculator.sessionsOverTerm`.
- **Frontend:** the Saturday notice block is removed from the run report. The displaced-subject strip and the replace dialog show hours. The elective flyout's "still owed" check uses runs.
- **Docs:** BR-56 rule 6; manual test cases TC-GAS-002, TC-GAS-004 and TC-GAS-013.

---

## 2026-09-15 — Sports, required filler first, and no manual Rotation/Elective Block (OC-227 follow-up)

**Prompted by:** after the hours fix, BSc Nursing (2025-2029) showed Theory 260 h / 260 h with "+191.7 h extra": the leftover-period filler had given Pathology I 65 h against 20 h and Pharmacology I 53 h against 20 h. The user: *"spare time for self study and library and sports time"*, and asked whether **Set up Rotation** and **Place Elective Block** are still needed. Specialist review (Product, Backend, DBA, QA, Frontend; Security and Docs had no open questions) settled the rules below. The same day's run report also raised the Saturday alert when only Library/idle-batch lines were orange — fixed separately by `AutoPlaceUnplacedItem.slotShortfall` (TC-GAS-015).

**Decisions:**
- **Order:** curriculum hours first (regulator-mandated). Then required quotas: Library (2 × 2 periods, unchanged), **Sports (new, 1 × 2 periods)**, and Self-Study gets exactly its curriculum hours. Whatever periods are left go to the other non-elective curriculum Theory subjects as extra revision, **equally** — also for a cohort with no Self-Study subject. Self-Study takes no extra.
- **Sports:** its own session type `SPORTS`. Taught by a PE faculty — exactly the faculty ticked on the seeded `SYSTEM-SPORTS` subject's eligible-faculty list (not "anyone", which is what subject eligibility means for a subject with no speciality). Held in a classroom whose Room is tagged **Sports & Recreation**. Placed in the day's last periods where free, otherwise any free block. Counts toward the PE faculty's workload caps.
- **Tight weeks / setup gaps:** Library and Sports shrink to fit, and a missing Sports room or PE faculty is reported as a grey note — never an orange line, never the Saturday alert, never an approval blocker.
- **Buttons:** **Set up Rotation** and **Place Elective Block** are removed with their flyouts. Automation already builds Lab rotations every run (and a rebuild cleared manual ones on unpinned cells), and places elective groups itself. Their backend APIs and permissions (`TIMETABLE_ROTATION_MANAGE`, `TIMETABLE_SKELETON_ELECTIVE_PLACE`) stay.
- **Electives on the grid:** an institution-decided elective runs only its chosen option, so it moves, swaps, is replaced and has its faculty reassigned like any subject. A student-choice group still shares one slot that only Run Automation moves.

**Impact:**
- **Schema (V505):** seeds the `SYSTEM-SPORTS` subject and `timetable.sports_sessions_per_week` = 1 / `timetable.sports_block_size_periods` = 2; widens `chk_class_schedule_session_shape` so a published SPORTS row needs a faculty and a classroom. Non-destructive — every existing row still satisfies it.
- **`ClassSessionType.SPORTS`**, handled like a classroom session in every venue/room/conflict switch; refused by the manual Class Schedule and Special Class paths, like Library.
- **`TimetableGlobalAutoScheduleService`:** new `fillSportsGaps` pass right after Library, in the same most-constrained-cohort-first order; `sportsInfoNotes`; `resolveExtraHoursFillerRows` excludes Self-Study; `fillSelfStudyGaps` gives each free period to the subject with the fewest extra sessions so far.
- **`TimetableSkeletonService`:** `saveSportsBlockCells`; Sports cells count in the cohort skeleton and block the audience like Library; `replaceCellSubject` only refuses student-choice electives. **`TimetableStaffingService`:** stages a SPORTS cell's PE faculty with its room already booked.
- **`SkeletonCellResponse.commonElective`** drives the frontend gates.
- **Frontend:** `SPORTS` type, green cell (`SPORTS_CELL_COLOR`), labels; the two buttons, `RotationSetupFlyoutComponent`, `ElectiveSlotBlockFlyoutComponent` and the unused client methods removed; dialog and tour text updated.
- **Docs:** BR-56 change log; manual test cases TC-GAS-016 to TC-GAS-019.

---

## 2026-09-15 — Move and swap whole blocks, and move a Clinical duty day (OC-227 follow-up)

**Prompted by:** the user asked whether a 2-period Lab can be swapped with two Theory sessions, and how a Clinical session could move from Friday to Monday when Monday already has Theory and Lab — then: *"the swap should be allowed if equal number of periods are available … the system must alert/warn the user to choose the slot and give suggestions"*. Move and swap refused any multi-period session outright, and changing a Clinical Shift group's day never checked the sessions already on the new day. Local data: 2026-2027 ODD has no Clinical grid cells at all — every Clinical hour is a Clinical Shift duty banner — so "moving Clinical" means moving a duty day.

**Decisions (specialist review):**
- Dropping onto empty periods **moves**; onto occupied periods **swaps**, using a same-length window: everything in the window takes the block's old periods in the same order. A session hanging over the window's edge makes that window unavailable.
- Same rules as automation: consecutive periods, never across lunch (a Clinical block may cross a short recess), never off the end of the day, blocked periods, Clinical duty windows, cohort exclusivity, and faculty/room clashes.
- A window with any illegal part is refused with the reason and other windows are suggested — nothing is half-swapped. All-or-nothing, every moved session pinned, draft timetables only.
- Parallel Lab batches, a rotation partner and an idle batch's fallback move together with their block; Library and Sports blocks move like any block; student-choice electives still move only with automation.
- **Duty day:** drag the duty banner to another day. The new day's sessions inside the duty window swap into the same periods of the day the duty leaves. The new day must pass every automation check: not blocked, the cohort not already away on another duty then, the clinical venue's capacity that day, each coordinator available and free. Own permission `TIMETABLE_SKELETON_DUTY_DAY_MOVE` (V506), defaulting to holders of `TIMETABLE_SKELETON_MOVE`.
- UX: legal windows light up while dragging (green move, blue swap, grey + reason); the session menu's **Move or swap…** lists them; a before → after preview is confirmed before anything applies.

**Impact:**
- **`TimetableSkeletonService`:** `previewRelocation`/`relocate` and `previewDutyDayMove`/`moveDutyDay`. A move unit is resolved from the session group, parallel venue batches and rotation slots; a plan is judged with every moving row set aside, then applied, flushed and re-judged in place (so faculty daily caps see the real arrangement) — any violation rolls it back. The duty preview genuinely tries each day inside a transaction, puts everything back and marks it rollback-only. The exclusion-taking checks gained set-valued `…Excluding` variants.
- **`TimetableStaffingService.validateAssignmentExcluding`** (set of excluded rows); `validateAssignment` delegates to it.
- **Endpoints:** `GET/PUT /timetables/skeleton/cells/{id}/relocate(-preview)` (`TIMETABLE_SKELETON_MOVE`), `GET/PUT /timetables/skeleton/clinical-shift-groups/{id}/day(-preview)` (`TIMETABLE_SKELETON_DUTY_DAY_MOVE`).
- **Frontend:** blocks are draggable; drag highlights come from the relocation preview; the swap dialog became the move-or-swap list and the preview; duty banners are draggable.
- **Docs:** BR-56 change log; manual test cases TC-GAS-020 to TC-GAS-023.

---

## 2026-09-15 — Library is one session a week; a second only when the week has room (OC-227 follow-up)

**Prompted by:** reviewing the Sports entry above, the user corrected its Library quota: *"Libraries are 2 periods per session per week, not two sessions, if there are a lot of free periods, we can give another library session"*.

**Reverses:** the "Library (2 × 2 periods, unchanged)" part of the Sports entry above. Everything else in that entry stands.

**Decisions:**
- **Required quota:** one 2-period Library session per section a week. Shrinking it to fit a full week is still a grey note ("Library reduced to 0 of 1 weekly session(s)").
- **Bonus session:** one more Library session, on another day, only when the section's cohort still has **8 or more free periods a week** after curriculum, the first Library session and Sports (user's choice among 6, 8, "whenever it fits" and "only if every subject still gets one extra"). The 8 is an admin setting. It runs before the extra-hours filler. A bonus that isn't placed is never reported.
- **Counting free periods:** blocked periods and clinical duty don't count; a free Saturday period counts for the share of weeks that Saturday really runs (about 6 of 26 on a 1st-Saturday-only term), per the term-total hours rule.

**Evidence:** the 2026-09-15 run showed BSc Nursing (2025-2029) with about 7–8 spare periods a week after curriculum (+191.7 h extra over 26 weeks), while Sem 3/5/7 have about 25 usable periods a week and no room to spare. Two fixed Library sessions took 4 periods from every one of them.

**Impact:**
- **Schema (V512):** `timetable.library_sessions_per_week` 2 → 1 (guarded on the V412 seed value), its description updated, and a new `timetable.library_extra_session_min_free_periods` = 8. Non-destructive.
- **`TimetableGlobalAutoScheduleService`:** default Library quota 1; new `fillExtraLibrarySession` pass after Sports, in the same most-constrained-cohort-first order; `fillLibraryGaps`' per-audience search moved into `placeLibraryBlocks`, shared by both passes; `weeklyFreePeriods` measures the week.
- **Docs:** BR-56 change log and filler rules; manual test cases TC-GAS-005, TC-GAS-018 and new TC-GAS-024.

---

## 2026-09-15 — Switched-off sessions aren't part of the timetable; a run can't double-book a lab (bug fix)

**Prompted by:** right after an automation run the Conflict Inspector listed dozens of clashes and said "Blocks publish". The user asked: *"Are these real issues? We ran auto schedule finally with all bugs fixed! Why this screen showing so many conflicts"*.

**Evidence (local, 2026-2027 ODD):**
- 2,713 placed rows, only 130 active. The other 2,583 were earlier runs' drafts, which every rebuild switches off instead of deleting (since OC-173). Each leftover sits in the same slot as its replacement, and the Inspector scanned them all, so every rebuilt session was reported as clashing with itself. Among the active rows there were no faculty double-bookings.
- 32 real clash pairs across five labs, all 30-seat labs holding 30-seat batches: the Computer lab and the Nursing Foundation Lab each held four batches at Monday Periods 7–8; the Child Health and Medical Surgical Labs two each at Tuesday Periods 1–2; the OBG Lab two at Wednesday Periods 7–8.
- Cause of the lab clashes: staffing is when a placed LAB row gets its lab, but the run's in-memory copy of the week (`AutoScheduleRunCache`) only took the new faculty across. For the rest of the run the lab looked empty, so the room check let the next batch into it. The Inspector reads the database, which is why only it saw them.

**Decisions:**
- A switched-off session is not part of a term's timetable anywhere: the Inspector, Approve and revert-to-draft, the unstaffed-sessions list, the draft and published views, the live calendar, faculty workload hours and the term checklist's draft count all ignore it. Discard still deletes, and counts, every row.
- The run cache records the room committed at staffing (classroom, lab or clinical venue) together with the faculty.
- Leftover rows stay in the database; nothing is deleted.

**Impact:**
- **`ClassScheduleRepository`:** `findByTermInstanceIdAndIsActiveTrue`, `findByTermInstanceIdAndStatusAndIsActiveTrue`.
- **Switched to them:** `TimetableConflictInspectorService`, `TimetableGenerationService` (approve, revertToDraft), `TimetableStaffingService#getUnstaffedCells`, `ClassScheduleService#findByTermInstanceIdAndStatus` (`/timetables/draft` and the published list), `TimetableOccurrenceService`, `FacultyWorkloadCapacityService`, `TermInstanceService`.
- **`AutoScheduleRunCache.recordStaffing(ClassSchedule)`** copies faculty, classroom, lab and clinical venue.
- **Frontend:** the Conflict Inspector's summary card no longer collapses to one line and hides its counts.
- **No schema change.**
- **Tests:** new `AutoScheduleRunCacheTest`; regression tests in `TimetableConflictInspectorServiceTest` and `TimetableGenerationServiceTest`.
- **Docs:** BR-56 change log; manual test case TC-GAS-025.

---

## 2026-09-15 — All cohorts by default; one Run button; confirm before overwriting a draft (OC-227 follow-up)

**Prompted by:** the user asked for the Run Automation / Run Global Auto-Schedule button to always sit in the same spot on the right of the toolbar (relabeling by scope instead of two separately-placed buttons), for **All cohorts…** to be the cohort dropdown's default, and — after a specialist round on what "already generated content" should mean — for a confirmation before a run overwrites an already-placed draft.

**Decisions (specialist review):**
- **One button, one slot.** The toolbar's right side (next to Configure Working Saturdays) always shows a single button: **Run Global Auto-Schedule** while "All cohorts…" is selected, **Run Automation** for a single cohort — same click handler and slot either way. The All-cohorts empty-state card keeps its explanatory text but drops its own duplicate action button, since the toolbar one now covers it.
- **Default selection.** The cohort dropdown defaults to **All cohorts…** on load for anyone holding `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE` — placing every cohort's whole term shortfall at once is this screen's main job. Anyone without that permission (who never sees the "All cohorts…" option at all) still defaults to the first individual cohort, unchanged.
- **What counts as "already generated content."** Per the user: any draft session already placed for the target scope, pinned or unpinned — not narrowed to only the non-pinned rows a rebuild would actually clear. A single cohort's own grid already answers this instantly (`hasNoCells`); an "All cohorts" run has no grid loaded, so a new read-only check answers the term-wide equivalent (any cohort with an active DRAFT row).
- **Where the confirmation sits.** A plain confirm dialog fires first, before the existing prerequisite checklist flyout even opens — not folded in as one more checklist tick — so overwriting existing work is a decision made up front, not discovered partway through the routine capacity/faculty checklist. Cancel opens nothing; confirming opens the checklist exactly as before. Reused by the Working Saturdays flyout's "Run Automation now" follow-up too, since that path can just as easily be running back over existing content.

**Impact:**
- **`ClassScheduleRepository`:** `existsByTermInstanceIdAndStatusAndIsActiveTrue`.
- **`TimetableGlobalAutoScheduleService.hasExistingDraftContent(termInstanceId)`** — read-only, checks for any active DRAFT row anywhere in the term.
- **Endpoint:** `GET /timetables/skeleton/global-auto-place/has-existing-draft` (`TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE`).
- **Frontend:** `SkeletonBuilderComponent.confirmAndOpenGlobalAutoSchedule()` is the one entry point for the toolbar button, both empty-state action buttons, and the Working Saturdays "Run Automation now" follow-up; `ConfirmDialogComponent` reused for the overwrite prompt. `loadCohorts()` defaults `cohortSelection`/`allCohortsSelected` to ALL when permitted.
- **No schema change.**
- **Tests:** `hasExistingDraftContent_true_whenAnActiveDraftRowExistsInTheTerm` / `_false_...` in `TimetableGlobalAutoScheduleServiceTest`.
- **Docs:** BR-56 change log; manual test case TC-GAS-026.

---

## 2026-09-18 — Lab Schedules screen loses its manual "Add" path; edit/delete only

**Prompted by:** a walkthrough of what the `/lab-schedules` (`ClassScheduleController`/`ClassSchedule`) screen is for. It's a manual CRUD editor over the same `class_schedules` table Skeleton Builder/Global Auto-Schedule write to, but its create form skipped the `check-conflicts` call the backend already exposes — a manually-added row could silently clash with an existing Lab/Faculty/Batch assignment in a way the algorithm itself never would. The user decided: *"Remove the add button, we can work out on the special classes (if it needs a lab or clinical session) write down a todo on it!"*

**Investigation before writing the TODO:** BR-55's Special/Remedial Class Scheduler already handles this. `SpecialClassRequestService.resolveVenue`/`applyVenue` switch on `THEORY`/`LAB`/`CLINICAL` (classroom/lab/clinical_venue columns all exist on `session_occurrences` since V374), and the request flyout (`special-class-request-flyout.component.ts/.html`) already has a session-type selector that swaps the venue dropdown between classrooms/labs/clinical venues. Day-repeat mode copies whichever `sessionType` the source day's row actually is, LAB/CLINICAL included. So there is **no open gap** for one-off Lab/Clinical sessions — BR-55 already covers them end-to-end (faculty-request → admin-approval), just as a `SessionOccurrence` (one-off), not a recurring `ClassSchedule` row.

**Decision:** Removed the create path entirely (button, `/lab-schedules/new` route, the form's create branch, `LabScheduleService.create()`) rather than just hiding the button — matches the existing "Batch creation = Capacity Auto-Plan only" hard-gate precedent of not leaving a create path reachable by direct URL once nothing legitimate calls it. Confirmed via grep that nothing else called `ClassScheduleService.create`/`POST /lab-schedules` (Skeleton Builder and Staffing write via repository, not this endpoint; Faculty Detail's Lab Schedules tab only reads). The backend `POST /lab-schedules` endpoint and `ClassScheduleService.create()` were left in place (frontend-only removal) since they share `applyRequest`/`enforceNoConflicts` with `update()` and have existing test coverage in `ClassScheduleControllerTest`/`ClassScheduleServiceTest` — untangling that wasn't asked for.

**TODO:** None needed for Special Class Lab/Clinical support (already built, see above). Open item instead: if a future need arises for a genuinely **recurring** (not one-off) Lab/Clinical `ClassSchedule` row outside of Skeleton Builder/Global Auto-Schedule, that is a deviation from this decision and should get a full specialist round before re-adding any manual create path, per the same rule already applied to `Batch` creation.

**Impact:**
- **Frontend:** `lab-schedule-list.component.html` (Add Schedule button removed), `app.routes.ts` (`lab-schedules/new` route removed), `lab-schedule-form.component.ts`/`.html` (edit-only now — `isEditMode`/`pageTitle` signals and the create branch in `onSubmit` removed), `lab-schedule.service.ts` (`create()` removed), `lab-schedule.tours.ts` (dropped the "Add a schedule" tour step/flow-map entries).
- **Backend:** unchanged — `POST /lab-schedules` and `ClassScheduleService.create()` remain, now unreachable from the UI.
- **No schema change.**

---

## 2026-09-22 — Skeleton Builder renamed to Timetable Builder; a real Pending status; attendance-gated Discard/Revert

**Prompted by:** the user asked for three things together: (1) rename the screen from Skeleton Builder to Timetable Builder; (2) opening the screen with an academic year/term selected should show every cohort for that term with status Pending by default, each getting its own grid once Run Global Auto-Schedule completes, with per-row View/Re-run/Discard/Check-Conflicts actions; (3) Discard returns a cohort to Pending, a re-run stays in Draft, a clean conflict check flips it to Conflicts Resolved with a Publish button, and a Published-and-never-used cohort keeps a Revert-to-Draft option.

**Investigation before the specialist round:** OC-260 (2026-09-21, "fold Timetable Draft Review into Skeleton Builder's cohort list") had already built most of (2) and (3) — the table already defaulted to every enrolled cohort for the term (not just ones with existing drafts), "Check & Resolve Conflicts" already flipped a row to a Publish button, and Discard/Revert-to-Draft already hard-refused server-side once attendance had been recorded against a cohort's sessions (`LifecycleConflictException` in `TimetableGenerationService.clear`/`revertToDraft`). The actual gaps were: a cohort with zero sessions rendered as `DRAFT`/readiness `DRAFT_GENERATED` (same bucket as one mid-progress, no distinct Pending state existed), and the Revert-to-Draft/Discard buttons showed unconditionally on a Published/Partially-Published row even when the attendance guard would refuse the click.

**Decisions (specialist review, `AskUserQuestion` one round per specialist):**
- **Rename scope — Product Owner:** full rename of the route, component/service/model/dialog files and class names, nav label, tour, and every user-facing string, but **`TIMETABLE_SKELETON_*` permission keys stay as-is** (Role Management is DB-only and a key rename would need a data migration and a re-verification of every role's assignment for zero user-visible benefit). Backend Java class/DTO names (`TimetableSkeletonService`, `SkeletonBuilderResponse.java`, etc.) were likewise left untouched — the approved option only covered "route path and component/file names," a frontend concern.
- **Status model — Frontend Architect:** unify the table's two previously-separate signals (a coarse `status` badge and a hidden `readinessStatus` driving only the action button) into **one lifecycle field** — Pending → Drafted → Conflicts Resolved → Published/Partially Published — so the visible badge itself changes at each step, not just the button underneath it. (`DRAFT_GENERATED` was renamed to `DRAFTED` mid-implementation per the user's own correction, to read better as a badge label.)
- **Used-gate UX — QA Lead:** rather than keep "show the button, fail on click" (Discard's existing pattern), a new `CohortTermStatusSummary.attendanceRecorded` flag is computed server-side (same attendance check `clear`/`revertToDraft` already run) so the table can hide Discard/Revert-to-Draft upfront on a row once attendance exists, instead of offering an action that can only ever fail.
- **Pending row action — QA Lead:** a Pending row gets its own inline **Run** button (not purely informational) that opens the existing Global Auto-Schedule flyout scoped to just that one cohort, without switching into its grid — a new entry point alongside "run from an open grid" and "All cohorts…".

**Impact:**
- **Backend:** `CohortTermStatusSummary` collapses to one `status` field (`PENDING`/`DRAFTED`/`CONFLICTS_RESOLVED`/`PUBLISHED`/`PARTIALLY_PUBLISHED`) plus new `attendanceRecorded`; `TimetableGenerationService.computeReadinessStatus` returns `PENDING` when a cohort has zero draft/published rows, ahead of the existing gate checks; new `isAttendanceRecorded()` reuses `resolveCohortScheduleIds` + `labAttendanceRepository.existsByLabScheduleIdIn`. No schema change, no new permissions, no new endpoint.
- **Frontend:** whole feature folder moved `skeleton-builder/` → `timetable-builder/` (component/service/model/dialog files and classes renamed to match; `SkeletonBuilderComponent` → `TimetableBuilderComponent`, `SkeletonCell`/`SkeletonBuilderResponse`/etc. → `TimetableCell`/`TimetableBuilderResponse`/etc.), route `/timetable/skeleton-builder` → `/timetable/timetable-builder`, tour file/consts renamed, nav/breadcrumb labels updated. `CmsStatusBadgeComponent.resolveClass()` extended for `PENDING` (existing neutral bucket), `DRAFTED` (existing warning bucket), `CONFLICTS_RESOLVED` (new — mapped to the existing "current" blue bucket, distinct from Published's green) per the shared-badge consistency gate — deliberately did **not** reuse the bare `DRAFT` case already in that switch, since that string is shared with unrelated screens' own generic draft status and would have silently forced their color onto this one. New `TimetableBuilderComponent.runRowAutomation()`/`rowRunCohortName` signal for the Pending row's Run button.
- **Docs:** BR-56 change log; this entry.
- **Verified:** `npx tsc -p tsconfig.app.json --noEmit` and `ng build --configuration=development` both clean; `./gradlew compileJava` clean. No automated test run in this pass (not requested; flag if regression coverage for the new Pending/attendanceRecorded branches is wanted as a follow-up).

---

## 2026-09-22 — Conflict Inspector screen removed (fully superseded by Timetable Builder)

**Prompted by:** the user asked, now that Timetable Builder bundles auto-scheduling, conflict checking, and publishing in one screen, whether a separate Conflict Inspector screen is still needed. Initial pushback was that permission granularity alone doesn't force separation — the real question is whether Conflict Inspector's term-wide diagnostic view and its own "Proceed to Review" gate still do anything Timetable Builder doesn't.

**Investigation before the specialist round:** the same OC-260 pass logged above had already retired the standalone Draft Review screen into Timetable Builder and given it its own per-cohort gate — `checkAndResolveConflicts()` → `TimetableService.acknowledgeCohortConflicts()` → `CohortConflictAcknowledgment`, independent of Conflict Inspector. Conflict Inspector's own code comment already said its term-wide "Proceed to Review" ack was "kept only as a term-wide diagnostic." Confirmed nothing else depended on it: `GET /timetables/conflict-inspector/acknowledgment-status` had zero frontend callers left, and `TermInstanceService`'s BR-53 term-advance checklist never read `conflict_acknowledged_at`. The underlying scan (`TimetableConflictInspectorService.scanTerm`) stays — it's genuinely reused by `TimetableGlobalAutoScheduleService`'s post-run report and `TimetableGenerationService`'s cohort-readiness summary.

**Decision:** removed the screen entirely rather than folding its term-wide table into Timetable Builder — no PO-level need surfaced for a whole-term violation table now that the actual gate is per-cohort. User confirmed "go with existing flow": full deletion of the screen/route/nav/permissions, DB columns left in place unused.

**Impact:**
- **Frontend:** deleted `features/timetable/conflict-inspector/` (component/service/model/spec) and `shared/tour/tours/conflict-inspector.tours.ts`; removed the route from `app.routes.ts`, the nav entry from `nav-config.ts`, and the breadcrumb label from `breadcrumb.service.ts`. `TimetableConflictRow`/`TimetableConstraintViolation` (still needed by Timetable Builder's post-run report) and `ConflictAcknowledgmentStatus` (still needed by the per-cohort `TimetableService` calls) were relocated into the shared `timetable.model.ts` before the folder was deleted; `ConflictScanResponse` had no surviving frontend consumer and was dropped.
- **Backend:** deleted `TimetableConflictInspectorController` (all three endpoints — `scan`, `acknowledgment-status`, `acknowledge` — had no consumer besides the deleted screen). Removed the now-dead term-wide `acknowledge()`/`getAcknowledgmentStatus()`/`isAcknowledgmentValid()` methods from `TimetableConflictInspectorService`; the cohort-scoped siblings (`acknowledgeCohort`/`getCohortAcknowledgmentStatus`/`isCohortAcknowledgmentValid`) and `scanTerm`/`scanCohorts`/`filterScanForCohorts` are untouched and still back `TimetableController`'s per-cohort endpoints plus the two other services above. Removed the six now-dead unit tests (and their now-unused imports) from `TimetableConflictInspectorServiceTest`; the remaining scan-behavior tests are untouched.
- **DB:** new migration `V550__remove_conflict_inspector_permissions.sql` deletes the `TIMETABLE_CONFLICT_INSPECTOR_VIEW`/`_ACKNOWLEDGE` permission rows (`role_permissions` cascades, per the V312 precedent). `term_instances.conflict_acknowledged_at`/`conflict_acknowledged_cell_count` (V528) are deliberately left in place, unused — dropping columns was out of scope.
- **Verified:** `npx tsc -p tsconfig.app.json --noEmit`, `ng build --configuration production`, `./gradlew compileJava`/`compileTestJava`, and the trimmed `TimetableConflictInspectorServiceTest` all clean.
