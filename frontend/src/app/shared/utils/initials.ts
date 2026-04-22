/**
 * Returns the first + last initial of a full name (uppercase).
 * Used by profile-hero avatars across detail screens.
 *
 *   computeInitials('Jane Mary Doe') === 'JD'
 *   computeInitials('Cher')          === 'C'
 *   computeInitials('')              === ''
 */
export function computeInitials(fullName: string | null | undefined): string {
  const name = fullName?.trim();
  if (!name) return '';
  const words = name.split(/\s+/).filter(Boolean);
  if (words.length === 0) return '';
  if (words.length === 1) return (words[0][0] || '').toUpperCase();
  const first = words[0][0] || '';
  const last = words[words.length - 1][0] || '';
  return (first + last).toUpperCase();
}
