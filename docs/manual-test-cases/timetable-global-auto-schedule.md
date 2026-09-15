# Manual Test Cases — Global Auto-Schedule: Monday–Friday First (OC-227)

## Prerequisites

- Frontend running (`ng serve`) at `https://localhost:4200`, backend running, logged in as an admin who holds `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE` and `TIMETABLE_SKELETON_CLINICAL_DUTY_FIT`.
- A term with committed Cohort Room Allocations for its cohorts, faculty assigned, and Clinical Shift groups (e.g. 2026-2027 ODD, BSc Nursing 2023-2027 / 2024-2028 / 2025-2029 / 2026-2030).
- Working Saturdays configured as a partial pattern (e.g. 1st Saturday only).
- Every elective group set to **Institution decided** on Elective Assignment, with the chosen option bulk-assigned to its cohort's students.
- Open **Timetable → Skeleton Builder**, pick the term, click **Run Automation** and let it finish.

---

### TC-GAS-001: Report lists only what the finished grid still owes

| Field       | Value |
|-------------|-------|
| **Action**  | After the run, compare each cohort's orange lines in the report with the cohort's grid and hours cards |
| **Expected**| No line says a subject was "displaced during a backtrack… could not be placed" when that subject's placed sessions already cover its curriculum hours across the term. The remaining lines match real gaps. |

---

### TC-GAS-002: A chosen working Saturday is used like a regular day

| Field       | Value |
|-------------|-------|
| **Action**  | With a working-Saturday pattern chosen (e.g. 1st Saturday only), run automation and look at each cohort's Saturday column |
| **Expected**| Saturday carries sessions like any weekday — Theory, and Lab/Clinical/Library where the cohort has them — and each cohort's week is spread evenly across Monday–Saturday rather than Saturday being left empty. A subject with a session on a partial-pattern Saturday also has enough weekday sessions to cover its hours (see TC-GAS-004). |

---

### TC-GAS-003: No Saturday chosen means Monday–Friday only

| Field       | Value |
|-------------|-------|
| **Action**  | Clear every pattern on Configure Working Saturdays, run automation, then find a subject needing more weekly sessions than the cohort has usable days (e.g. 5 a week in a cohort with Mon/Wed/Fri duty) |
| **Expected**| Nothing is placed on Saturday. The extra session goes on a weekday the subject already uses — two periods of the same subject on one day, back-to-back allowed. |

---

### TC-GAS-004: A partial-pattern Saturday counts for its real hours, and nothing is left short

| Field       | Value |
|-------------|-------|
| **Action**  | With 1st Saturday only chosen (6 working Saturdays in a 26-week term), run automation, open the run report, then check the Theory card and a subject that has a Saturday session |
| **Expected**| The report has **no** orange "one weekly session is on Saturday… runs 6 times instead of 26" lines. A subject with a Saturday session also has enough weekday sessions to cover its hours (the Saturday session adds 5 h, not 21.7 h). The Theory card shows no unassigned hours when the term's periods can hold every subject's hours. Dragging one more session of a subject that is already at its hours is refused with "budget is already met (… h placed across the term)". |

---

### TC-GAS-005: Library shrinks to a grey note

| Field       | Value |
|-------------|-------|
| **Action**  | Check a cohort whose week left no room for a 2-period Library block after curriculum |
| **Expected**| A grey line reads "Library reduced to 0 of 1 weekly session(s) — …". Library is **not** listed among the orange unplaced lines. |

---

### TC-GAS-006: A management-selected elective is a common cohort subject

| Field       | Value |
|-------------|-------|
| **Action**  | Open the grid for a cohort whose elective group is Institution decided (e.g. 2024-2028, Elective II → Addiction Psychiatry) |
| **Expected**| Only the chosen option appears, in the section's row, with a small "ELECTIVE" tag. It's staffed in the section's own classroom. No other option of that group is placed. The report has no "Elective groups not placed" line for it. |

---

### TC-GAS-007: A management-selected group with no option assigned

| Field       | Value |
|-------------|-------|
| **Action**  | On Elective Assignment, clear the bulk assignment for one Institution-decided group, then run automation |
| **Expected**| That cohort's report shows one line naming the group: "…management-selected but no option has been assigned to the cohort yet…". None of its options are placed. |

---

### TC-GAS-008: Clinical duty-length suggestion

| Field       | Value |
|-------------|-------|
| **Action**  | Open "Clinical hours still owed" in the report for a subject with a 6h shift over a 26-week term |
| **Expected**| Next to the extra-duty-days remedy: "Or lengthen each duty from 360 to 370 minutes — students are still back before the next free period, so it costs no timetable periods." |

---

### TC-GAS-009: Apply duty length

| Field       | Value |
|-------------|-------|
| **Action**  | Click **Apply duty length** on that subject, then run automation again |
| **Expected**| A success toast appears and the row shows "✓ Duty set to 370 minutes — run automation again…". The Course Offering's clinical shift duration is 370. After the re-run, the subject no longer appears under "Clinical hours still owed" and its Clinical card shows no unassigned hours. |

---

### TC-GAS-010: Apply button needs its own permission

| Field       | Value |
|-------------|-------|
| **Action**  | Log in as a role with `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE` but without `TIMETABLE_SKELETON_CLINICAL_DUTY_FIT`, then run automation |
| **Expected**| The duty-length suggestion text is shown, but there is no **Apply duty length** button. A direct `POST /timetables/skeleton/clinical-duty-fit/{id}` returns 403. |

---

### TC-GAS-011: Elective hours count once on the hours cards

| Field       | Value |
|-------------|-------|
| **Action**  | With a management-selected elective placed, read the Theory Assigned/Total card |
| **Expected**| The elective adds its full weekly hours once, not divided by the number of options in the group. |

---

### TC-GAS-012: Light and dark mode

| Field       | Value |
|-------------|-------|
| **Action**  | Repeat TC-GAS-004 to TC-GAS-009 in light and dark mode |
| **Expected**| The grey info lines, orange notices, Apply button and the ELECTIVE tag are all readable, including on dark-filled Lab/Clinical cells. |

---

### TC-GAS-013: Every Saturday chosen — Saturday counts as a full week

| Field       | Value |
|-------------|-------|
| **Action**  | Choose all five patterns (1st, 2nd, 3rd, 4th, Last) on Configure Working Saturdays and run automation |
| **Expected**| Sessions are spread evenly across Monday–Saturday. A subject's Saturday session counts as a full weekly session, since it now runs every week, so subjects need no extra weekday session to cover it. |

---

### TC-GAS-014: Hours cards measure like with like

| Field       | Value |
|-------------|-------|
| **Action**  | Open a cohort with Clinical duty that starts before Period 1 (e.g. BSc Nursing 2025-2029, 07:00–13:10) and extra-hours filler, after a run |
| **Expected**| *Total Hours Available* is larger than *Total Assigned* — it now includes the duty time before Period 1 (for 3 duty days a week: 906.7 h + about 221 h). *Total Assigned* is at most *Total Hours Required*; hours beyond curriculum show as "+N h extra" under Total Assigned and under each Theory/Lab/Clinical card. A subject that is short still shows its gap as unassigned even when another subject has extra hours. |

---

### TC-GAS-015: The Saturday alert appears only when a subject is short of hours

| Field       | Value |
|-------------|-------|
| **Action**  | With 1st Saturday only chosen, run automation for All Cohorts on a term whose periods can hold every subject's hours, where some cohorts still get orange "Idle batch fallback (LIBRARY)" or Library lines |
| **Expected**| The per-cohort list still shows the Library/idle-batch lines in orange, but the yellow "Only some Saturdays are working days" box (and its **Configure Working Saturdays** button) does **not** appear, and every cohort's Theory card shows no unassigned hours. Repeat on a term too small for its curriculum: a subject line saying "…h still unplaced" does bring the box back, with **Schedule Special Class** when it is the only short subject. |

---

### TC-GAS-016: Sports is placed at the end of the day with a PE faculty

| Field       | Value |
|-------------|-------|
| **Action**  | Tag a room (e.g. the ground or indoor court) with the **Sports & Recreation** purpose category and give it a Classroom; on the Subject master, tick the PE faculty on the **Sports** subject's eligible-faculty list; run automation for All Cohorts |
| **Expected**| Each cohort section gets 1 Sports session of 2 periods a week (settings `timetable.sports_sessions_per_week` / `timetable.sports_block_size_periods`), in the day's last two periods where free, in the Sports room, staffed by one of the ticked PE faculty. It shows in green with a "SPORTS" tag. Its PE faculty count it toward their workload caps. |

---

### TC-GAS-017: Sports that can't be placed is a grey note

| Field       | Value |
|-------------|-------|
| **Action**  | (a) Remove the Sports & Recreation tag from every room and run automation; (b) restore it but untick every PE faculty on the Sports subject and run again |
| **Expected**| No Sports sessions are placed. Each cohort shows a grey note — (a) "Sports not placed — no room is tagged Sports & Recreation…", (b) "…no active PE faculty with spare capacity is on the Sports subject's eligible-faculty list". No orange line, no Saturday alert, and Draft Review approval is not blocked. |

---

### TC-GAS-018: Leftover periods go equally to curriculum subjects, never Self-Study

| Field       | Value |
|-------------|-------|
| **Action**  | Run automation on a term with spare periods (e.g. 2026-2027 ODD), then compare each Theory subject's "+N h extra" on the hours cards and the Self-Study subject's hours |
| **Expected**| Self-Study/Co-curricular has exactly its curriculum hours (e.g. 80 h), no extra. Every other non-elective Theory subject's extra hours are within one session (≈ 0.8 h) of each other. Library (1 × 2 periods, plus a bonus second session where the week has room — TC-GAS-024) and Sports (1 × 2 periods) are placed before any extra hours. |

---

### TC-GAS-019: Set up Rotation and Place Elective Block are gone; institution-decided electives move

| Field       | Value |
|-------------|-------|
| **Action**  | Open Skeleton Builder for a cohort with an institution-decided elective, check the header buttons, then open that elective session's menu and drag it to another free slot |
| **Expected**| The header shows only **Run Automation** and **Configure Working Saturdays**. The elective session offers Swap, Replace and Reassign Faculty like any subject, and dragging it to a free slot succeeds. A student-choice elective session still says "Student-choice electives share one slot — Run Automation places the whole group." |

---

### TC-GAS-020: Drag a 2-period Lab onto empty periods

| Field       | Value |
|-------------|-------|
| **Action**  | In a draft term, drag one row of a 2-period Lab (running as Batch A and Batch B in parallel) and hover over the grid, then drop it on a day with two empty back-to-back periods |
| **Expected**| While dragging, legal windows turn green (move) or blue (swap) and refused ones stay grey, with the reason on hover. The drop opens a preview listing the Lab with both batches, e.g. "Mon Period 3–Period 4 → Wed Period 1–Period 2". **Move** places both batches' blocks there, pinned. |

---

### TC-GAS-021: Swap a 2-period Lab with two Theory sessions

| Field       | Value |
|-------------|-------|
| **Action**  | Drag the Lab block onto a window holding two single-period Theory sessions (or open the Lab's menu → **Move or swap…** and pick that window) |
| **Expected**| The window shows blue. The preview lists the Lab moving into the window and the two Theory sessions moving into the Lab's old periods in the same order. **Swap** applies all of it at once; every moved session is pinned. |

---

### TC-GAS-022: Refused windows explain why

| Field       | Value |
|-------------|-------|
| **Action**  | Drag a 2-period block and hover grey slots: the day's last period, a window crossing lunch, a window where only half of another 2-period block sits, a window where the Lab's faculty already teaches |
| **Expected**| Each shows a reason — "would run past the end of the day", "can't run across the break between … and …", "… runs past this window — choose a window that covers the whole session", or the faculty clash. Dropping there does nothing but show that reason. A Clinical block (where one exists) may cross a short recess but never lunch. |

---

### TC-GAS-023: Move a Clinical duty to another day

| Field       | Value |
|-------------|-------|
| **Action**  | As a user holding `TIMETABLE_SKELETON_DUTY_DAY_MOVE`, drag a cohort's Friday duty banner onto Monday; repeat as a user without it |
| **Expected**| While dragging, days the duty can move to turn green, others grey with the reason ("already away on … on Monday", a clinical venue over capacity, a blocked day, a coordinator busy). The preview lists Monday's sessions inside the duty window moving to the same periods on Friday. **Move duty** moves the banner to Monday, renames a "(Friday)" label to "(Monday)", and pins the swapped sessions. Without the permission the banner can't be dragged. |

---

### TC-GAS-024: Library is one session a week; a second only when the week has room

| Field       | Value |
|-------------|-------|
| **Action**  | Run automation on a term where one cohort's week is tight (e.g. Sem 3/5/7 with hospital duty three days a week) and another has plenty of spare periods (e.g. BSc Nursing 2025-2029). Then set `timetable.library_extra_session_min_free_periods` to 99 on System Configuration and run again |
| **Expected**| Every section has one 2-period Library session. A section whose week still had 8 or more free periods after curriculum, Library and Sports also has a second Library session on another day; the tight cohort has only one, with no note or warning about it. With the setting at 99, no section gets a second session and the report is otherwise unchanged. |

---

### TC-GAS-025: No lab double-booking, and the Conflict Inspector ignores earlier runs

| Field       | Value |
|-------------|-------|
| **Action**  | In a draft term where two subjects each split a section into Batch 1 and Batch 2 in the same lab (e.g. NRST405 and EDUC315 in the Computer lab), run automation twice in a row. Open Conflict Inspector for the term, then Timetable Draft Review and click **Approve** |
| **Expected**| No lab holds two batches at the same day and period. Conflict Inspector's summary card shows its three counts, only this run's sessions are counted in "Sessions scanned", and the term reads "Clean — ready to publish" (no "already scheduled for another session" rows left over from the first run). Approve publishes only this run's sessions; the live Timetable shows each session once. |

---

### TC-GAS-026: All cohorts by default; one Run button on the right; confirm before overwriting a draft

| Field       | Value |
|-------------|-------|
| **Action**  | Open Skeleton Builder as an admin who holds `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE`. Note the cohort dropdown and the toolbar's right-hand button. Switch to a single cohort that already has draft sessions placed (pinned or not) and click the button; Cancel, then click again and confirm. Switch back to **All cohorts…** on a term where at least one cohort already has draft content and click the button; Cancel, then confirm. Finally pick a cohort/term combination with an empty grid and click the button. Repeat the initial page load as an admin who holds `TIMETABLE_WORKING_SATURDAYS_MANAGE` but not `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE` |
| **Expected**| On load, the cohort dropdown defaults to **All cohorts…** and the toolbar's right-hand button reads **Run Global Auto-Schedule**, in the same slot next to the card border as **Configure Working Saturdays**. Selecting a single cohort relabels that same button **Run Automation**, still in the same slot. Clicking it against a cohort with existing draft content shows an "Overwrite Existing Draft?" confirmation naming that cohort; Cancel leaves the grid untouched and opens nothing; confirming opens the usual prerequisite checklist. The same confirmation, worded for the whole term, appears for an **All cohorts** run when any cohort in the term already has draft content. Against an empty grid, the checklist opens directly with no confirmation. The centered "Global Auto-Schedule" card in All-cohorts view no longer has its own action button — only the toolbar one. For the admin without the Global Auto-Place permission, the dropdown has no "All cohorts…" option and defaults to the first individual cohort, matching pre-existing behavior. |
