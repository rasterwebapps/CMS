import { Injectable } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { getDateValidationType, DateValidationType } from '../validators/date-validation.config';
import { getDateValidator } from '../validators/date.validators';

/**
 * Service to manage date field validations across the application
 * Provides methods to apply appropriate validators to form controls
 */
@Injectable({
  providedIn: 'root'
})
export class DateValidationService {
  /**
   * Apply date validation to a form control based on entity and field name
   * @param control FormControl to apply validation to
   * @param entity Entity name (e.g., 'Student', 'Enquiry')
   * @param field Field name (e.g., 'dateOfBirth', 'admissionDate')
   * @example
   * const control = fb.control('');
   * this.dateValidationService.applyDateValidation(control, 'Student', 'dateOfBirth');
   */
  applyDateValidation(control: FormControl, entity: string, field: string): void {
    const validationType = getDateValidationType(entity, field);
    const validator = getDateValidator(validationType);
    control.addValidators(validator);
    control.updateValueAndValidity();
  }

  /**
   * Apply date validation to a form group for multiple date fields
   * @param formGroup FormGroup to apply validation to
   * @param dateFields Array of {entity, field} objects
   * @example
   * this.dateValidationService.applyFormDateValidations(this.form, [
   *   { entity: 'Student', field: 'dateOfBirth' },
   *   { entity: 'Student', field: 'admissionDate' }
   * ]);
   */
  applyFormDateValidations(
    formGroup: FormGroup,
    dateFields: Array<{ entity: string; field: string }>
  ): void {
    dateFields.forEach(({ entity, field }) => {
      const control = formGroup.get(field) as FormControl;
      if (control) {
        this.applyDateValidation(control, entity, field);
      }
    });
  }

  /**
   * Get the validation type for a specific date field
   * @param entity Entity name
   * @param field Field name
   * @returns Validation type: 'A' (past), 'B' (future), 'C' (any)
   */
  getValidationType(entity: string, field: string): DateValidationType {
    return getDateValidationType(entity, field);
  }

  /**
   * Check if a date value is valid based on validation type
   * @param date Date to validate
   * @param validationType Validation type: 'A' (past), 'B' (future), 'C' (any)
   * @returns true if valid, false if invalid
   */
  isDateValid(date: Date | string | null, validationType: DateValidationType): boolean {
    if (!date) {
      return true; // Allow null/empty
    }

    const selectedDate = new Date(date);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    switch (validationType) {
      case 'A': // Past dates only
        return selectedDate <= today;
      case 'B': // Future dates only
        return selectedDate >= today;
      case 'C': // Any date
      default:
        return true;
    }
  }

  /**
   * Get error message for a date validation error
   * @param validationType Validation type
   * @returns User-friendly error message
   */
  getErrorMessage(validationType: DateValidationType): string {
    switch (validationType) {
      case 'A':
        return 'Date must be in the past (today or earlier)';
      case 'B':
        return 'Date must be today or in the future';
      case 'C':
      default:
        return '';
    }
  }

  /**
   * Get disabled date filter for MatDatepicker based on validation type
   * Returns a function that can be used with [matDatepickerFilter]
   * @param validationType Validation type
   * @returns Function that returns true if date should be disabled
   * @example
   * <mat-form-field>
   *   <input matInput [matDatepicker]="picker"
   *     [matDatepickerFilter]="dateValidationService.getDatepickerFilter('A')">
   *   <mat-datepicker #picker></mat-datepicker>
   * </mat-form-field>
   */
  getDatepickerFilter(validationType: DateValidationType): (date: Date | null) => boolean {
    return (date: Date | null): boolean => {
      if (!date) return false;

      const today = new Date();
      today.setHours(0, 0, 0, 0);

      switch (validationType) {
        case 'A': // Disable future dates
          return date > today;
        case 'B': // Disable past dates
          return date < today;
        case 'C': // Allow all dates
        default:
          return false; // false = date is enabled
      }
    };
  }
}

