import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Custom Angular Validators for Date Fields
 * Used in reactive forms throughout the application
 */

/**
 * Validator: Ensures date is in the past (today or earlier)
 * Usage: control.addValidators(pastDateOnlyValidator())
 * @returns ValidationErrors if date is in the future
 */
export function pastDateOnlyValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null; // Allow null/empty (use @required if mandatory)
    }

    const selectedDate = new Date(control.value);
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Reset time to start of day for fair comparison

    if (selectedDate > today) {
      return {
        pastDateOnly: {
          value: control.value,
          message: 'Date must be in the past (today or earlier)'
        }
      };
    }

    return null;
  };
}

/**
 * Validator: Ensures date is in the future (today or later)
 * Usage: control.addValidators(futureDateOnlyValidator())
 * @returns ValidationErrors if date is in the past
 */
export function futureDateOnlyValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null; // Allow null/empty (use @required if mandatory)
    }

    const selectedDate = new Date(control.value);
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Reset time to start of day for fair comparison

    if (selectedDate < today) {
      return {
        futureDateOnly: {
          value: control.value,
          message: 'Date must be today or in the future'
        }
      };
    }

    return null;
  };
}

/**
 * Validator: No restrictions (any date allowed)
 * Returns null always (pass-through)
 */
export function anyDateValidator(): ValidatorFn {
  return (): ValidationErrors | null => null;
}

/**
 * Get appropriate validator based on validation type
 * @param type 'A' = past dates only, 'B' = future dates only, 'C' = any date
 * @returns ValidatorFn for the specified type
 */
export function getDateValidator(type: 'A' | 'B' | 'C'): ValidatorFn {
  switch (type) {
    case 'A':
      return pastDateOnlyValidator();
    case 'B':
      return futureDateOnlyValidator();
    case 'C':
    default:
      return anyDateValidator();
  }
}

