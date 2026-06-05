import { Pipe, PipeTransform } from '@angular/core';
import { getPaymentModeShortLabel } from '../utils/payment-mode.utils';

/**
 * Transforms a raw PaymentMode enum value into a short human-readable label
 * suitable for table badges and filter dropdowns.
 * Usage: {{ payment.paymentMode | paymentModeLabel }}
 */
@Pipe({
  name: 'paymentModeLabel',
  standalone: true,
})
export class PaymentModeLabelPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return getPaymentModeShortLabel(value);
  }
}

