import { numberToWords } from './number-to-words.utils';
import { formatCurrency } from '@angular/common';

export interface ReceiptPrintData {
  receiptNumber: string;
  /** Primary payer name (student or enquiry person). */
  payerName: string;
  /**
   * Roll number — shown only after roll number generation.
   * When present, takes priority over admissionNumber for display.
   */
  payerIdentifier?: string | null;
  /** Admission number — shown after admission and before roll number generation. */
  admissionNumber?: string | null;
  /** Course/program name shown as "Course:" row. */
  programName?: string | null;
  amountPaid: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference?: string | null;
  installmentBreakdown: Array<{ installmentLabel: string; amountApplied: number }>;
  /** Determines "towards" label: TUITION_AND_HOSTEL → "Tuition Fees And Hostel Fees", TUITION_ONLY → "Tuition Fees" */
  feeCategory?: 'TUITION_ONLY' | 'TUITION_AND_HOSTEL' | null;
}

function buildReceiptHtml(data: ReceiptPrintData, autoPrint: boolean): string {
  const amountWords = numberToWords(data.amountPaid);
  // Append 'T00:00:00' to avoid UTC-to-local-timezone shift on ISO date strings
  const formattedDate = new Date(data.paymentDate + 'T00:00:00').toLocaleDateString('en-IN', {
    day: '2-digit', month: 'long', year: 'numeric',
  });
  const formattedAmount = formatCurrency(data.amountPaid, 'en-IN', '₹', 'INR', '1.0-0');
  const towards = data.feeCategory === 'TUITION_AND_HOSTEL'
    ? 'Tuition Fees And Hostel Fees'
    : data.feeCategory === 'TUITION_ONLY'
      ? 'Tuition Fees'
      : data.installmentBreakdown.length
        ? data.installmentBreakdown
            .map(s => `${s.installmentLabel} (${formatCurrency(s.amountApplied, 'en-IN', '₹', 'INR', '1.0-0')})`)
            .join(', ')
        : 'Fee Payment';

  // ID row: roll number takes priority; admission number shown only when no roll number yet
  const idRow = data.payerIdentifier
    ? `<div class="fill-row">
      <span class="fill-label">Roll No.</span>
      <span class="fill-blank"><span class="fill-value">${data.payerIdentifier}</span></span>
    </div>`
    : data.admissionNumber
      ? `<div class="fill-row">
      <span class="fill-label">Admission No.</span>
      <span class="fill-blank"><span class="fill-value">${data.admissionNumber}</span></span>
    </div>`
      : '';

  const courseRow = data.programName
    ? `<div class="fill-row">
      <span class="fill-label">Course</span>
      <span class="fill-blank"><span class="fill-value">${data.programName}</span></span>
    </div>`
    : '';

  const refRow = data.transactionReference
    ? `<div class="fill-row">
      <span class="fill-label">Ref. No.</span>
      <span class="fill-blank"><span class="fill-value">${data.transactionReference}</span></span>
    </div>`
    : '';

  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<title>Receipt - ${data.receiptNumber}</title>
<style>
  @page { size: A5 landscape; margin: 7mm 10mm; }
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: 'Times New Roman', Times, serif;
    font-size: 12.5px;
    background: #fff;
    color: #000;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  /* ── Outer card ── */
  .receipt {
    width: 100%;
    min-height: 126mm;
    border: 3px double #1a237e;
    padding: 10px 16px 12px;
    display: flex;
    flex-direction: column;
  }

  /* ── Header ── */
  .header {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding-bottom: 8px;
    border-bottom: 1.5px solid #1a237e;
  }
  .logo { width: 56px; height: 56px; flex-shrink: 0; object-fit: contain; }
  .header-center { flex: 1; }
  .college-name {
    font-size: 20px; font-weight: 900; color: #1a237e;
    letter-spacing: 0.4px; text-transform: uppercase; line-height: 1.15;
  }
  .college-sub { font-size: 10px; color: #333; margin-top: 3px; line-height: 1.5; }
  .receipt-badge {
    border: 2px solid #1a237e; padding: 4px 12px;
    font-size: 12px; font-weight: 800; letter-spacing: 3px;
    text-transform: uppercase; color: #1a237e;
    align-self: center; white-space: nowrap;
  }

  /* ── Meta row: Receipt No (left) + Date (right) ── */
  .meta-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin: 8px 0 2px;
  }
  .receipt-no { font-size: 12.5px; }
  .receipt-no strong { font-size: 15px; }
  .meta-date { font-size: 12px; font-weight: 600; }

  /* ── Fill-in rows ── */
  .body { margin-top: 10px; }
  .fill-row {
    display: flex;
    align-items: flex-end;
    margin-bottom: 15px;
    font-size: 12.5px;
    line-height: 1;
  }
  .fill-label {
    white-space: nowrap;
    flex-shrink: 0;
    padding-right: 6px;
    padding-bottom: 3px;
  }
  .fill-blank {
    flex: 1;
    border-bottom: 1.5px dotted #444;
    display: flex;
    align-items: flex-end;
    justify-content: flex-start;
    min-width: 60px;
    padding-bottom: 3px;
  }
  .fill-value {
    font-weight: 700;
    white-space: nowrap;
    padding-left: 0.45in;
  }

  /* ── Amount box — below fill rows, right-aligned ── */
  .amount-row {
    display: flex;
    justify-content: flex-end;
    margin-top: 2px;
    margin-bottom: 4px;
  }
  .amount-box {
    border: 2px solid #1a237e;
    padding: 4px 12px;
    text-align: center;
    min-width: 110px;
  }
  .amount-label { font-size: 10px; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; }
  .amount-value { font-size: 17px; font-weight: 900; margin-top: 2px; }

  /* ── Footer ── */
  .spacer { flex: 1; }
  .footer-divider { border-top: 1.5px solid #1a237e; margin-bottom: 7px; }
  .footer {
    display: flex; justify-content: flex-end; align-items: flex-end;
    gap: 36px;
  }
  .footer-for { text-align: right; }
  .footer-for .for-word { font-size: 10px; margin-bottom: 1px; }
  .footer-for .for-college { font-size: 11px; font-weight: 800; letter-spacing: 0.3px; }
  .sig-area { text-align: center; }
  .sig-space { height: 26px; }
  .sig-line-rule { border-top: 1px solid #000; }
  .sig-text { font-size: 10px; padding-top: 3px; }
</style>
</head>
<body>
<div class="receipt">

  <!-- Header -->
  <div class="header">
    <img class="logo" src="/assets/images/sks-logo-icon.png" alt="SKS Logo" />
    <div class="header-center">
      <div class="college-name">SKS College Of Nursing</div>
      <div class="college-sub">
        Run By VS Educational Trust (Regn. No. 579 / 1997)<br/>
        No.31, Neikkarapatti, Salem &ndash; 636 010.
      </div>
    </div>
    <div class="receipt-badge">R E C E I P T</div>
  </div>

  <!-- Receipt No (left) + Date (right, moved from bottom) -->
  <div class="meta-row">
    <div class="receipt-no">No. <strong>${data.receiptNumber}</strong></div>
    <div class="meta-date">Date : ${formattedDate}</div>
  </div>

  <!-- Fill-in rows -->
  <div class="body">
    <div class="fill-row">
      <span class="fill-label">Received with thanks from</span>
      <span class="fill-blank"><span class="fill-value">${data.payerName}</span></span>
    </div>
    ${idRow}
    ${courseRow}
    <div class="fill-row">
      <span class="fill-label">the sum of Rupees</span>
      <span class="fill-blank"><span class="fill-value">${amountWords}</span></span>
    </div>
    <div class="fill-row">
      <span class="fill-label">towards</span>
      <span class="fill-blank"><span class="fill-value">${towards}</span></span>
    </div>
    ${refRow}
  </div>

  <!-- Amount box (moved below fill rows) -->
  <div class="amount-row">
    <div class="amount-box">
      <div class="amount-label">Amount</div>
      <div class="amount-value">${formattedAmount} /-</div>
    </div>
  </div>

  <div class="spacer"></div>

  <!-- Footer -->
  <div class="footer-divider"></div>
  <div class="footer">
    <div class="footer-for">
      <div class="for-word">For</div>
      <div class="for-college">SKS COLLEGE OF NURSING</div>
    </div>
    <div class="sig-area">
      <div class="sig-space"></div>
      <div class="sig-line-rule"></div>
      <div class="sig-text">Authorised Signature</div>
    </div>
  </div>

</div>
${autoPrint ? '<script>window.onload = function() { window.print(); };<\/script>' : ''}
</body>
</html>`;
}

export function printFeeReceipt(data: ReceiptPrintData): void {
  const html = buildReceiptHtml(data, true);
  const win = window.open('', '_blank', 'width=900,height=650');
  if (win) {
    win.document.write(html);
    win.document.close();
  }
}

export function downloadFeeReceipt(data: ReceiptPrintData): void {
  const html = buildReceiptHtml(data, false);
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `Receipt-${data.receiptNumber}.html`;
  anchor.click();
  URL.revokeObjectURL(url);
}
