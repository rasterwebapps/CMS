import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

// ─────────────────────────────────────────────────────────────────────────────
// CMS Shared Form Validators  (BR-29)
//
// Apply these consistently across every master-data and entity form.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Rejects values that contain two or more consecutive whitespace characters.
 * Applies to every name / text field (BR-29 EMPTY SPACE rule).
 */
export function noConsecutiveSpaces(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: string = control.value ?? '';
    return /  /.test(value) ? { noConsecutiveSpaces: true } : null;
  };
}

/**
 * Rejects any whitespace inside a value (including copy-paste with spaces).
 * Applies to every CODE field (BR-29 CODE rule).
 */
export function noInternalSpaces(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: string = control.value ?? '';
    return /\s/.test(value) ? { noInternalSpaces: true } : null;
  };
}

/**
 * Enforces a minimum character length on the TRIMMED value.
 * Boundary: empty → skip (use Validators.required separately).
 * BR-29 BOUNDARY VALUE rule — trim before checking min.
 */
export function trimmedMinLength(min: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const trimmed = (control.value ?? '').trim();
    if (trimmed.length === 0) return null; // let required handle empty
    return trimmed.length < min
      ? { trimmedMinLength: { requiredLength: min, actualLength: trimmed.length } }
      : null;
  };
}

/**
 * Returns a human-readable error string for a single form control.
 * Use in component templates or getErrorMessage() helpers.
 *
 * BR-29 standard error messages — consistent across all screens.
 */
export function cmsFieldError(
  control: AbstractControl | null,
  label: string,
): string {
  if (!control || !control.errors || !control.touched) return '';

  const e = control.errors;

  if (e['required'])             return `${label} is required`;
  if (e['duplicate'])            return `${label} already exists`;
  if (e['trimmedMinLength'])     return `${label} must be at least ${e['trimmedMinLength'].requiredLength} characters`;
  if (e['maxlength'])            return `${label} must be at most ${e['maxlength'].requiredLength} characters`;
  if (e['noConsecutiveSpaces']) return `${label} must not contain consecutive spaces`;
  if (e['noInternalSpaces'])    return `${label} must not contain spaces`;
  if (e['pattern'])             return `${label} contains invalid characters`;
  if (e['min'])                 return `${label} must be at least ${e['min'].min}`;
  if (e['max'])                 return `${label} must be at most ${e['max'].max}`;
  if (e['email'])               return `Enter a valid email address`;
  if (e['transactionReferenceRequired']) return 'Transaction reference is required for this payment mode';

  return 'Invalid value';
}

/**
 * Utility: strip internal spaces from a raw input string.
 * Use in (input) event handlers on CODE fields so paste-with-spaces is cleaned
 * immediately, before the user tries to submit (BR-29 CODE rule).
 */
export function stripSpaces(value: string): string {
  return value.replace(/\s/g, '');
}

/**
 * Utility: collapse consecutive spaces into a single space (but do NOT trim).
 * Use in (blur) event handlers on NAME fields so the value is normalised
 * when the user leaves the field (BR-29 EMPTY SPACE rule).
 */
export function collapseSpaces(value: string): string {
  return value.replace(/  +/g, ' ');
}
