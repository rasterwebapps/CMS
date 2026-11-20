export type CounsellingStateContext = 'enquiry' | 'convert' | 'student';

/**
 * Returns true when the given state string matches Tamil Nadu (case- and whitespace-insensitive).
 */
export function isTamilNaduState(state: string | null | undefined): boolean {
  if (!state) return false;
  const normalized = state.trim().replace(/\s+/g, ' ').toLowerCase();
  return normalized === 'tamil nadu' || normalized === 'tamilnadu';
}

/**
 * Returns true when the admission quota or final admission category involves Government Counselling.
 */
export function isCounsellingCategory(categoryOrQuota: string | null | undefined): boolean {
  return categoryOrQuota === 'COUNSELLING' || categoryOrQuota === 'MANAGEMENT_TO_COUNSELLING';
}

/**
 * Computes a non-blocking advisory warning message when a candidate/student has a counselling
 * quota/category while their residential state is not Tamil Nadu (or is blank).
 * Returns null when the state is Tamil Nadu.
 */
export function getCounsellingStateWarning(
  categoryLabel: string,
  state: string | null | undefined,
  context: CounsellingStateContext,
): string | null {
  if (isTamilNaduState(state)) {
    return null;
  }

  const trimmedState = state?.trim() ?? '';

  if (context === 'enquiry') {
    if (trimmedState) {
      return (
        `Selected state is "${trimmedState}" (outside Tamil Nadu). ${categoryLabel} is typically ` +
        `applicable only to Tamil Nadu state candidates, and the Tamil Nadu counselling fee slab ` +
        `will still be applied. Please verify nativity / counselling eligibility before saving.`
      );
    }
    return (
      `Candidate residential state is not selected. ${categoryLabel} is typically applicable only ` +
      `to Tamil Nadu state candidates — please select and verify the candidate's state.`
    );
  }

  if (context === 'convert') {
    if (trimmedState) {
      return (
        `Residential state is "${trimmedState}" (outside Tamil Nadu) while this enquiry is under ` +
        `${categoryLabel}. Government Counselling is typically applicable only to Tamil Nadu state ` +
        `candidates. Please verify nativity / allotment eligibility before creating this admission.`
      );
    }
    return (
      `Residential state is not selected while this enquiry is under ${categoryLabel}. Government ` +
      `Counselling is typically applicable only to Tamil Nadu state candidates — please select and ` +
      `verify the student's state.`
    );
  }

  if (trimmedState) {
    return (
      `Student's residential state is "${trimmedState}" (outside Tamil Nadu). "${categoryLabel}" ` +
      `is typically applicable only to Tamil Nadu state students. Please verify counselling ` +
      `allotment / nativity eligibility before saving.`
    );
  }

  return (
    `Student's residential state is not specified. "${categoryLabel}" is typically applicable ` +
    `only to Tamil Nadu state students — please verify counselling allotment / nativity ` +
    `eligibility before saving.`
  );
}
