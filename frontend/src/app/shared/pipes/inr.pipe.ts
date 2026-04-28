import { Pipe, PipeTransform } from '@angular/core';
import { formatCurrency } from '@angular/common';

/**
 * Formats a numeric value as Indian Rupees (INR) using the `en-IN` locale.
 *
 * Indian number system uses 2-2-3 grouping (e.g. ₹12,34,567).
 *
 * Usage:
 *   {{ amount | inr }}              → ₹1,23,456  (no paise, with symbol — cards/dialogs/summaries)
 *   {{ amount | inr:true }}         → ₹1,23,456.00  (with paise, with symbol — receipts/ledgers)
 *   {{ amount | inr:false:false }}  → 1,23,456  (no paise, no symbol — table cells)
 *   {{ amount | inr:true:false }}   → 1,23,456.00  (with paise, no symbol — rare)
 *   {{ null | inr }}                → —
 *
 * **2026 UX Pattern**: Don't repeat the symbol in table cells. Show "(₹)" in column headers instead.
 *
 * Always import `InrPipe` in the component's `imports[]` array.
 * Never use `CurrencyPipe`, `| currency:'INR'`, or raw `₹{{ val | number }}` in templates.
 */
@Pipe({
  name: 'inr',
  standalone: true,
  pure: true,
})
export class InrPipe implements PipeTransform {
  transform(
    value: number | string | null | undefined,
    showPaise: boolean = false,
    showSymbol: boolean = true
  ): string {
    if (value == null || value === '') return '—';
    const num = typeof value === 'string' ? parseFloat(value) : value;
    if (isNaN(num)) return '—';
    const symbol = showSymbol ? '₹' : '';
    return formatCurrency(num, 'en-IN', symbol, 'INR', showPaise ? '1.2-2' : '1.0-0');
  }
}

