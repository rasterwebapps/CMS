/** A placement/staffing/special-class attempt can fail several independent backend checks at
 *  once (already placed, blocked period, faculty conflict, ...) — the backend reports every one
 *  of them together (`err.error.violations`) instead of just the first, so this joins them into
 *  one multi-line message instead of making the user fix-and-resubmit repeatedly to discover each
 *  problem in turn. Falls back to the plain `err.error.message` for any other error shape. */
export function violationText(err: any): string | undefined {
  const violations = err?.error?.violations as { message: string }[] | undefined;
  return violations?.length ? violations.map((v) => v.message).join('\n') : err?.error?.message;
}
