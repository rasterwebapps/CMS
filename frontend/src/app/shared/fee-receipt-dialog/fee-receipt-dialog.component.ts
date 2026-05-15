import { Component, computed, input, output } from '@angular/core';
import { InrPipe } from '../pipes/inr.pipe';
import { AppDatePipe } from '../pipes/app-date.pipe';
import { PaymentModeLabelPipe } from '../pipes/payment-mode-label.pipe';
import { ReceiptDisplayData } from '../../features/finance/finance.model';

const ONES: string[] = [
  '', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine',
  'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen',
  'Seventeen', 'Eighteen', 'Nineteen',
];
const TENS: string[] = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];

/**
 * Slide-in receipt panel shown after a successful fee collection.
 *
 * Usage (parent owns the signal; passes value via normal @Input binding):
 *   <cms-fee-receipt-dialog
 *     [receipt]="receipt()"
 *     (closed)="doneWithReceipt()"
 *     (printed)="doneWithReceipt()"
 *   />
 */
@Component({
  selector: 'cms-fee-receipt-dialog',
  standalone: true,
  imports: [InrPipe, AppDatePipe, PaymentModeLabelPipe],
  templateUrl: './fee-receipt-dialog.component.html',
  styleUrl: './fee-receipt-dialog.component.scss',
})
export class FeeReceiptDialogComponent {
  /** The receipt data to display. Treated as truthy gate — dialog only renders when non-null. */
  readonly receipt = input<ReceiptDisplayData | null>(null);

  /** Emitted when the user explicitly closes the dialog (Close button or backdrop click). */
  readonly closed = output<void>();

  /** Emitted after print() is called so the parent can clean up. */
  readonly printed = output<void>();

  /** True when the payer is a student (shows roll number row). */
  protected readonly isStudent = computed(() => this.receipt()?.payerType === 'STUDENT');

  /** True when the Web Share API is available (modern mobile browsers). */
  protected get hasShare(): boolean {
    return typeof navigator !== 'undefined' && !!navigator.share;
  }

  protected close(): void {
    this.closed.emit();
  }

  protected print(): void {
    window.print();
    this.printed.emit();
  }

  protected download(): void {
    const r = this.receipt();
    if (!r) return;
    const content = this.buildReceiptText(r);
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `Receipt-${r.receiptNumber}.txt`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  protected async share(): Promise<void> {
    const r = this.receipt();
    if (!r || !this.hasShare) return;
    try {
      await navigator.share({
        title: `Fee Receipt – ${r.receiptNumber}`,
        text: [
          `SKS College Of Nursing`,
          `Receipt No: ${r.receiptNumber}`,
          `Name: ${r.payerName}`,
          `Amount: ₹${r.amountPaid.toLocaleString('en-IN')}`,
          `Mode: ${r.paymentMode}`,
          r.installmentsCovered ? `Towards: ${r.installmentsCovered}` : '',
        ]
          .filter(Boolean)
          .join('\n'),
      });
    } catch {
      /* User cancelled or share API error — silently ignore */
    }
  }

  /**
   * Converts a rupee amount (whole number) to Indian-style amount words.
   * e.g. 75000 → "Seventy-Five Thousand Rupees Only"
   */
  protected buildAmountWords(amount: number): string {
    const whole = Math.round(Math.abs(amount));
    if (whole === 0) return 'Zero Rupees Only';

    const crore    = Math.floor(whole / 10_000_000);
    const lakh     = Math.floor((whole % 10_000_000) / 100_000);
    const thousand = Math.floor((whole % 100_000)    / 1_000);
    const remainder = whole % 1_000;

    const parts: string[] = [];
    if (crore)    parts.push(this.threeDigits(crore)   + ' Crore');
    if (lakh)     parts.push(this.twoDigits(lakh)      + ' Lakh');
    if (thousand) parts.push(this.twoDigits(thousand)  + ' Thousand');
    if (remainder) parts.push(this.threeDigits(remainder));

    return parts.join(' ') + ' Rupees Only';
  }

  // ─── Internal helpers ────────────────────────────────────────────────────

  private twoDigits(n: number): string {
    if (n < 20) return ONES[n];
    const ten = TENS[Math.floor(n / 10)];
    const one = n % 10 ? '-' + ONES[n % 10] : '';
    return ten + one;
  }

  private threeDigits(n: number): string {
    if (n === 0) return '';
    if (n < 100) return this.twoDigits(n);
    const hundred = ONES[Math.floor(n / 100)] + ' Hundred';
    const rest    = n % 100 ? ' ' + this.twoDigits(n % 100) : '';
    return hundred + rest;
  }

  private buildReceiptText(r: ReceiptDisplayData): string {
    const sep = '─'.repeat(44);
    const lines = [
      'SKS College Of Nursing',
      'No.31, Neikkarapatti, Salem – 636 010',
      sep,
      `Receipt No : ${r.receiptNumber}`,
      `Date       : ${r.paymentDate}`,
      sep,
      `Name       : ${r.payerName}`,
      r.payerIdentifier ? `Roll No    : ${r.payerIdentifier}` : '',
      r.programName     ? `Program    : ${r.programName}`     : '',
      `Amount     : ₹${r.amountPaid.toLocaleString('en-IN')}`,
      `Mode       : ${r.paymentMode}`,
      r.transactionReference ? `Txn Ref    : ${r.transactionReference}` : '',
      r.installmentsCovered  ? `Towards    : ${r.installmentsCovered}`  : '',
      r.remarks              ? `Remarks    : ${r.remarks}`              : '',
      sep,
      this.buildAmountWords(r.amountPaid),
      sep,
      'This is a computer-generated receipt.',
    ].filter(Boolean);

    return lines.join('\n');
  }
}

