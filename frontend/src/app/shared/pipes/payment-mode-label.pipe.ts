import { Pipe, PipeTransform } from '@angular/core';
import { getPaymentModeLabel } from '../utils/payment-mode.utils';

/**
 * Transforms a raw PaymentMode enum value into a human-readable label.
 * Usage: {{ payment.paymentMode | paymentModeLabel }}
 */
@Pipe({
  name: 'paymentModeLabel',
  standalone: true,
})
export class PaymentModeLabelPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return getPaymentModeLabel(value);
  }
}

