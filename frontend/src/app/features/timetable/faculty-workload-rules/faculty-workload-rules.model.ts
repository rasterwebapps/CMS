/** Null means no institution-wide cap/floor configured for that tier. Sessions (a real Period-row
 *  count), not hours -- 2026-09-24 design decision: a Theory row is 1 period, a Clinical block can
 *  be 4, so there's no honest hours-equivalent for a cap that hasn't picked a concrete session
 *  shape yet. Real worked hours (for INC/university reporting) are shown elsewhere, computed from
 *  each actually-scheduled session's own real Period duration. */
export interface FacultyWorkloadRules {
  maxDailySessions: number | null;
  maxWeeklySessions: number | null;
  maxContinuousSessions: number | null;
  /** Advisory floor, never a hard block -- a faculty member below this many sessions/week shows as
   *  under-loaded in Assign Faculty and the Faculty Workload report. */
  minWeeklySessions: number | null;
}
