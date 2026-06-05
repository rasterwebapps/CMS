/**
 * Canonical list of payment modes supported by the system.
 * NET_BANKING has been consolidated into BANK_TRANSFER (same concept).
 */
export const PAYMENT_MODES = [
  'CASH',
  'UPI',
  'BANK_TRANSFER',
  'CARD',
  'CHEQUE',
  'DEMAND_DRAFT',
  'SCHOLARSHIP',
] as const;

export type PaymentModeValue = (typeof PAYMENT_MODES)[number];

/** Human-readable labels for each payment mode (full form — used in print receipts). */
export const PAYMENT_MODE_LABELS: Record<string, string> = {
  CASH: 'Cash',
  UPI: 'UPI (GPay / PhonePe / Paytm)',
  BANK_TRANSFER: 'Bank Transfer (NEFT / RTGS / IMPS)',
  NET_BANKING: 'Bank Transfer (NEFT / RTGS / IMPS)', // legacy — display same as BANK_TRANSFER
  CARD: 'Card (Debit / Credit)',
  CHEQUE: 'Cheque',
  DEMAND_DRAFT: 'Demand Draft (DD)',
  SCHOLARSHIP: 'Scholarship / Fee Waiver',
};

/** Shortened labels for table/badge display. */
export const PAYMENT_MODE_SHORT_LABELS: Record<string, string> = {
  CASH: 'Cash',
  UPI: 'UPI',
  BANK_TRANSFER: 'Bank Transfer',
  NET_BANKING: 'Bank Transfer',
  CARD: 'Card',
  CHEQUE: 'Cheque',
  DEMAND_DRAFT: 'Demand Draft',
  SCHOLARSHIP: 'Scholarship',
};

/**
 * Returns the human-readable label for a payment mode enum value.
 * Falls back gracefully for unknown values.
 */
export function getPaymentModeLabel(mode: string | null | undefined): string {
  if (!mode) return '—';
  return PAYMENT_MODE_LABELS[mode] ?? mode;
}

/** Short form — use in table badges and filter dropdowns. */
export function getPaymentModeShortLabel(mode: string | null | undefined): string {
  if (!mode) return '—';
  return PAYMENT_MODE_SHORT_LABELS[mode] ?? mode;
}

