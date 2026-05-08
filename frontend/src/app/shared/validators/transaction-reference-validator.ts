import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Custom validator that makes transactionReference required when payment mode
 * is UPI, BANK_TRANSFER, CHEQUE, or DEMAND_DRAFT.
 *
 * @param paymentModeControlName - The name of the payment mode form control
 * @returns ValidatorFn for the transactionReference control
 */
export function transactionReferenceRequiredValidator(
  paymentModeControlName: string
): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.parent) {
      return null;
    }

    const paymentModeControl = control.parent.get(paymentModeControlName);
    if (!paymentModeControl) {
      return null;
    }

    const paymentMode = paymentModeControl.value;
    const requiresTransactionRef = ['UPI', 'BANK_TRANSFER', 'CHEQUE', 'DEMAND_DRAFT'].includes(paymentMode);

    if (requiresTransactionRef) {
      const value = control.value;
      if (!value || (typeof value === 'string' && value.trim().length === 0)) {
        return { transactionReferenceRequired: true };
      }
    }

    return null;
  };
}

