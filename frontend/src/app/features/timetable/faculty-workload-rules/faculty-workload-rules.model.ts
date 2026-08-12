/** Null means no institution-wide cap configured for that tier. */
export interface FacultyWorkloadRules {
  maxDailyHours: number | null;
  maxWeeklyHours: number | null;
  maxContinuousHours: number | null;
}
