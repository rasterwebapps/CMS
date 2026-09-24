/** The one color system every screen that renders a THEORY/LAB/CLINICAL/LIBRARY/SPORTS session
 *  must use — originally Timetable Builder-only (see git history), promoted here 2026-09-23 so
 *  Week Grid and Day Agenda (both shared components consumed outside Timetable Builder too — My
 *  Timetable, Timetable View) render the exact same accent instead of each inventing its own
 *  unrelated hardcoded palette. Lives in shared/util rather than a feature folder because shared
 *  components never import from a feature folder (see week-grid.model.ts's own doc on why
 *  WeekGridSessionType is a local duplicate of TimetableSessionType, not an import).
 *
 *  Four tints of the tenant's own themed primary color (`--cms-primary-rgb`, set per school by
 *  ThemeService — never a hardcoded brand color), one per real curriculum category, so every
 *  timetable-shaped screen reads as one cohesive palette instead of an arbitrary per-screen
 *  rainbow. Alpha, not a mix toward white/black, so each tint composites correctly against any
 *  card background in either theme rather than assuming a light backdrop. Clinical gets the
 *  strongest (most prominent) tint, then Lab, then Theory; advisory Co-curricular stays the
 *  faintest of all four regardless of session type (2026-09-23: explicit user-directed ordering,
 *  overriding this file's original Theory-strongest priority).
 *
 *  <p>Every consumer's SCSS composites this value again at a low percentage (`color-mix(in srgb,
 *  var(--subject-color) 12%, transparent)`, see e.g. week-grid.component.scss) to get an actual
 *  cell fill — that second dilution multiplies whatever gap exists between two stops here, so the
 *  original 0.55/0.75/1 spacing (deltas of 0.2) survived as an almost-imperceptible ~2-3% opacity
 *  difference between Theory and Lab cells, especially in dark mode where the card background sits
 *  close in lightness to the tinted result to begin with. Spaced further apart (0.35/0.7/1, Lab
 *  exactly 2x Theory) so that same downstream dilution still reads as two visibly different
 *  tints (2026-09-23, reported: "in dark mode the primary color variants of theory and lab don't
 *  look different"). */
const THEORY_MANDATORY_COLOR = 'rgba(var(--cms-primary-rgb), 0.35)';
const LAB_COLOR = 'rgba(var(--cms-primary-rgb), 0.7)';
const CLINICAL_COLOR = 'rgba(var(--cms-primary-rgb), 1)';
const CO_CURRICULAR_COLOR = 'rgba(var(--cms-primary-rgb), 0.15)';

/** LIBRARY sessions have no CourseOffering at all (see TimetableGlobalAutoScheduleService
 *  #fillLibraryGaps) — a fixed slate outside the four curriculum tints, so Library always reads
 *  as its own consistent category. */
export const LIBRARY_CELL_COLOR = '#64748b';

/** SPORTS sessions have no CourseOffering either (see TimetableGlobalAutoScheduleService
 *  #fillSportsGaps) — a fixed field-green outside the four curriculum tints, for the same reason. */
export const SPORTS_CELL_COLOR = '#65a30d';

/** Deliberately the plain string union, not a shared type import — every consumer (Timetable
 *  Builder's TimetableSessionType, Week Grid's WeekGridSessionType, Day Agenda's session type)
 *  already duplicates this exact same union locally rather than sharing one type across a
 *  feature/shared boundary, so this accepts the shape structurally instead of forcing another
 *  cross-boundary type import. */
export type SessionTypeForColor = 'THEORY' | 'LAB' | 'CLINICAL' | 'LIBRARY' | 'SPORTS';

/** One accent color for any session, any screen. {@code coCurricular} is a curriculum
 *  classification orthogonal to session type (Theory/Lab/Clinical) and always wins when true —
 *  omit it (defaults false) for a caller whose data shape doesn't carry that distinction (Week
 *  Grid, Day Agenda today), which simply falls back to session-type-only coloring. */
export function colorForSessionType(sessionType: SessionTypeForColor, coCurricular = false): string {
  if (sessionType === 'LIBRARY') return LIBRARY_CELL_COLOR;
  if (sessionType === 'SPORTS') return SPORTS_CELL_COLOR;
  if (coCurricular) return CO_CURRICULAR_COLOR;
  switch (sessionType) {
    case 'LAB': return LAB_COLOR;
    case 'CLINICAL': return CLINICAL_COLOR;
    default: return THEORY_MANDATORY_COLOR;
  }
}
