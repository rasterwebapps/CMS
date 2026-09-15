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
