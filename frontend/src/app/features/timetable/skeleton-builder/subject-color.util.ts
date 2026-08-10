/** Cycled by courseOfferingId so each subject in the cohort-wide grid gets a stable, distinct
 *  accent color — same hash-to-fixed-palette approach as colorForNavGroup in
 *  core/permissions/menu-order.util.ts, copied rather than imported/shared since that file is
 *  nav-specific and this is an unrelated feature. */
const SUBJECT_COLOR_PALETTE = [
  '#6366f1', '#3b82f6', '#0ea5e9', '#14b8a6', '#10b981',
  '#f59e0b', '#f97316', '#ec4899', '#8b5cf6', '#a855f7', '#ef4444',
];

export function colorForSubject(courseOfferingId: number): string {
  const key = String(courseOfferingId);
  let hash = 0;
  for (let i = 0; i < key.length; i++) hash = (hash * 31 + key.charCodeAt(i)) | 0;
  return SUBJECT_COLOR_PALETTE[Math.abs(hash) % SUBJECT_COLOR_PALETTE.length];
}
