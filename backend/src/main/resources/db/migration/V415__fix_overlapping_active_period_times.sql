-- OC-186: repair active periods that overlap each other in clock time.
--
-- Two ACTIVE periods are the timetable grid's real columns and must never share clock time.
-- Period 1 was widened from 50 to 60 minutes (09:00-09:50 -> 09:00-10:00) while Period 2 kept
-- starting at 09:50, leaving a 10-minute overlap. That is not cosmetic: every room/faculty
-- conflict check compares real time ranges rather than period ids, so a session in Period 1 and
-- another in Period 2 read as a genuine clash, and the auto-scheduler could never place a block
-- spanning the two (PeriodGapPolicy sees a negative gap where it expects back-to-back times).
--
-- Narrow and idempotent by design: it repairs only the one shape that actually occurs here -- an
-- ACTIVE period running past the start of a LATER active period -- and repairs it only by pulling
-- its end time back to that next start, so re-running is a no-op and an environment that never had
-- the overlap is unaffected. A period wholly containing another (same or earlier start) is
-- deliberately left alone rather than guessed at; the save-time gate below stops any new one.
-- Inactive rows are excluded on both sides -- the retired standalone Lab Slot master rows
-- (inactive since V331 merged them into Period) legitimately span the live periods.
--
-- Going forward PeriodService.requireNoActiveOverlap rejects this at save time, so this is a
-- one-time forward repair of data that predates that gate, not a recurring cleanup.

UPDATE periods p
SET end_time = sub.next_start,
    duration_minutes = ROUND(EXTRACT(EPOCH FROM (sub.next_start - p.start_time)) / 60)::int,
    updated_at = now()
FROM (
    SELECT a.id,
           MIN(b.start_time) AS next_start
    FROM periods a
    JOIN periods b
      ON b.id <> a.id
     AND b.is_active = true
     AND b.start_time > a.start_time
     AND b.start_time < a.end_time
    WHERE a.is_active = true
    GROUP BY a.id
) AS sub
WHERE p.id = sub.id
  AND sub.next_start > p.start_time;
