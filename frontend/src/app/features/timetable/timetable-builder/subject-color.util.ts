import { TimetableSessionType } from './timetable-builder.model';

/** Four tints of the tenant's own themed primary color (`--cms-primary-rgb`, set per school by
 *  ThemeService — never a hardcoded brand color), one per real curriculum category, so the grid
 *  reads as one cohesive palette instead of an arbitrary per-subject rainbow (2026-09-21: the
 *  previous hash-per-courseOfferingId palette gave every individual subject its own color, which
 *  read as noisy — especially in dark mode — and gave Co-curricular content no visual identity of
 *  its own at all). Alpha, not a mix toward white/black, so each tint composites correctly against
 *  any card background in either theme rather than assuming a light backdrop. Mandatory Theory
 *  gets the strongest (most prominent) tint; advisory Co-curricular the faintest, matching its
 *  lower scheduling priority (see TimetableGlobalAutoScheduleService#isAdvisoryRow). */
const THEORY_MANDATORY_COLOR = 'rgba(var(--cms-primary-rgb), 1)';
const LAB_COLOR = 'rgba(var(--cms-primary-rgb), 0.75)';
const CLINICAL_COLOR = 'rgba(var(--cms-primary-rgb), 0.55)';
const CO_CURRICULAR_COLOR = 'rgba(var(--cms-primary-rgb), 0.3)';

/** LIBRARY cells have no CourseOffering at all (see TimetableGlobalAutoScheduleService
 *  #fillLibraryGaps) — a fixed slate outside the four curriculum tints, so Library always reads
 *  as its own consistent category. */
export const LIBRARY_CELL_COLOR = '#64748b';

/** SPORTS cells have no CourseOffering either (see TimetableGlobalAutoScheduleService
 *  #fillSportsGaps) — a fixed field-green outside the four curriculum tints, for the same reason. */
export const SPORTS_CELL_COLOR = '#65a30d';

/** THEORY/LAB/CLINICAL cell accent — Co-curricular (advisory) always wins regardless of session
 *  type, since it's a curriculum classification orthogonal to session type; the three mandatory
 *  session types are otherwise told apart by {@code sessionType} alone. */
export function colorForCell(sessionType: TimetableSessionType, coCurricular: boolean): string {
  if (coCurricular) return CO_CURRICULAR_COLOR;
  switch (sessionType) {
    case 'LAB': return LAB_COLOR;
    case 'CLINICAL': return CLINICAL_COLOR;
    default: return THEORY_MANDATORY_COLOR;
  }
}
