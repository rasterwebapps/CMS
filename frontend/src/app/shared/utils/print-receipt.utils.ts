import { numberToWords } from './number-to-words.utils';
import { getPaymentModeLabel } from './payment-mode.utils';
import { formatCurrency } from '@angular/common';

const SKS_LOGO_DATA_URL = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMDAgMjAwIiB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCI+CiAgPCEtLSBPdXRlciBjaXJjbGUgYmFja2dyb3VuZCAtLT4KICA8Y2lyY2xlIGN4PSIxMDAiIGN5PSIxMDAiIHI9Ijk2IiBmaWxsPSIjMWEyYTVlIiBzdHJva2U9IiM0YTZmYTUiIHN0cm9rZS13aWR0aD0iMiIvPgoKICA8IS0tIElubmVyIGRpdmlkaW5nIGNyb3NzIGxpbmVzIC0tPgogIDxsaW5lIHgxPSIxMDAiIHkxPSIzMCIgeDI9IjEwMCIgeTI9IjE3MCIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIxLjUiIG9wYWNpdHk9IjAuNyIvPgogIDxsaW5lIHgxPSIzMCIgeTE9IjEwMCIgeDI9IjE3MCIgeTI9IjEwMCIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIxLjUiIG9wYWNpdHk9IjAuNyIvPgoKICA8IS0tIENlbnRlciBzbWFsbCBjaXJjbGUgd2l0aCBFU1RELiAxOTkzIC0tPgogIDxjaXJjbGUgY3g9IjEwMCIgY3k9IjEwMCIgcj0iMjIiIGZpbGw9IiMxYTJhNWUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS13aWR0aD0iMS41Ii8+CiAgPHRleHQgeD0iMTAwIiB5PSI5NiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjciIGZvbnQtd2VpZ2h0PSJib2xkIiBmaWxsPSJ3aGl0ZSI+RVNURC48L3RleHQ+CiAgPHRleHQgeD0iMTAwIiB5PSIxMDciIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZvbnQtZmFtaWx5PSJBcmlhbCwgc2Fucy1zZXJpZiIgZm9udC1zaXplPSI3IiBmb250LXdlaWdodD0iYm9sZCIgZmlsbD0id2hpdGUiPjE5OTM8L3RleHQ+CgogIDwhLS0gVG9wLWxlZnQgcXVhZHJhbnQ6IEhlYXJ0IC0tPgogIDxwYXRoIGQ9Ik0gNzIgNzIgQyA2NiA2NCA1NSA2NCA1NSA3NCBDIDU1IDgwIDYyIDg3IDcyIDk0IEMgODIgODcgODkgODAgODkgNzQgQyA4OSA2NCA3OCA2NCA3MiA3MiBaIiBmaWxsPSJ3aGl0ZSIgb3BhY2l0eT0iMC45Ii8+CgogIDwhLS0gVG9wLXJpZ2h0IHF1YWRyYW50OiBDYWR1Y2V1cy9tZWRpY2FsIHN0YWZmIChzaW1wbGlmaWVkKSAtLT4KICA8bGluZSB4MT0iMTI4IiB5MT0iNTUiIHgyPSIxMjgiIHkyPSI5MiIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIyLjUiLz4KICA8cGF0aCBkPSJNIDExOCA2NSBDIDExOCA1OCAxMzggNTggMTM4IDY1IEMgMTM4IDcyIDExOCA3MiAxMTggNzkgQyAxMTggODYgMTM4IDg2IDEzOCA3OSIgZmlsbD0ibm9uZSIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIxLjgiLz4KICA8Y2lyY2xlIGN4PSIxMjgiIGN5PSI1MyIgcj0iNCIgZmlsbD0id2hpdGUiIG9wYWNpdHk9IjAuOSIvPgoKICA8IS0tIEJvdHRvbS1sZWZ0IHF1YWRyYW50OiBGbGFtZSAtLT4KICA8cGF0aCBkPSJNIDcyIDE0OCBDIDYwIDEzOCA1OCAxMjUgNjUgMTE4IEMgNjMgMTI4IDcwIDEzMiA3MiAxMjUgQyA3NCAxMzIgODEgMTI4IDc5IDExOCBDIDg2IDEyNSA4NCAxMzggNzIgMTQ4IFoiIGZpbGw9IndoaXRlIiBvcGFjaXR5PSIwLjkiLz4KCiAgPCEtLSBCb3R0b20tcmlnaHQgcXVhZHJhbnQ6IEdyYWR1YXRpb24gY2FwIC0tPgogIDxwb2x5Z29uIHBvaW50cz0iMTI4LDExOCAxMDgsMTI4IDEyOCwxMzggMTQ4LDEyOCIgZmlsbD0id2hpdGUiIG9wYWNpdHk9IjAuOSIvPgogIDxyZWN0IHg9IjEyMCIgeT0iMTI4IiB3aWR0aD0iMTYiIGhlaWdodD0iMTAiIHJ4PSIyIiBmaWxsPSJ3aGl0ZSIgb3BhY2l0eT0iMC45Ii8+CiAgPGxpbmUgeDE9IjE0OCIgeTE9IjEyOCIgeDI9IjE0OCIgeTI9IjEzOCIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIyIi8+CiAgPGNpcmNsZSBjeD0iMTQ4IiBjeT0iMTQwIiByPSIzIiBmaWxsPSJ3aGl0ZSIgb3BhY2l0eT0iMC45Ii8+CgogIDwhLS0gQ2lyY3VsYXIgdGV4dDogU0tTIENPTExFR0UgT0YgTlVSU0lORyAodG9wIGFyYykgLS0+CiAgPHBhdGggaWQ9InRvcEFyYyIgZD0iTSAxOCwxMDAgQSA4Miw4MiAwIDAsMSAxODIsMTAwIiBmaWxsPSJub25lIi8+CiAgPHRleHQgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjEwIiBmb250LXdlaWdodD0iYm9sZCIgZmlsbD0id2hpdGUiIGxldHRlci1zcGFjaW5nPSIyIj4KICAgIDx0ZXh0UGF0aCBocmVmPSIjdG9wQXJjIiBzdGFydE9mZnNldD0iNSUiPlNLUyBDT0xMRUdFIE9GIE5VUlNJTkc8L3RleHRQYXRoPgogIDwvdGV4dD4KCiAgPCEtLSBDaXJjdWxhciB0ZXh0OiBWUyBFRFVDQVRJT05BTCBUUlVTVCwgU0FMRU0gKGJvdHRvbSBhcmMpIC0tPgogIDxwYXRoIGlkPSJib3R0b21BcmMiIGQ9Ik0gMTgsMTAwIEEgODIsODIgMCAwLDAgMTgyLDEwMCIgZmlsbD0ibm9uZSIvPgogIDx0ZXh0IGZvbnQtZmFtaWx5PSJBcmlhbCwgc2Fucy1zZXJpZiIgZm9udC1zaXplPSI4LjUiIGZvbnQtd2VpZ2h0PSJib2xkIiBmaWxsPSJ3aGl0ZSIgbGV0dGVyLXNwYWNpbmc9IjEiPgogICAgPHRleHRQYXRoIGhyZWY9IiNib3R0b21BcmMiIHN0YXJ0T2Zmc2V0PSI1JSI+VlMgRURVQ0FUSU9OQUwgVFJVU1QsIFNBTEVNPC90ZXh0UGF0aD4KICA8L3RleHQ+CgogIDwhLS0gWWVsbG93IHN0YXJzIC0tPgogIDxwb2x5Z29uIHBvaW50cz0iMjIsMTAwIDI1LDkyIDI4LDEwMCAyMCw5NSAzMCw5NSIgZmlsbD0iI0ZGRDcwMCIvPgogIDxwb2x5Z29uIHBvaW50cz0iMTc4LDEwMCAxODEsOTIgMTg0LDEwMCAxNzYsOTUgMTg2LDk1IiBmaWxsPSIjRkZENzAwIi8+Cjwvc3ZnPgo=';

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
  const paymentModeLabel = getPaymentModeLabel(data.paymentMode);
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
  html, body {
    width: 210mm;
    height: 148mm;
  }
  body {
    font-family: 'Times New Roman', Times, serif;
    font-size: 12.5px;
    background: #fff;
    color: #000;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  @media screen {
    html { background: #888; display: flex; justify-content: center; align-items: flex-start; padding: 16px; }
    body { box-shadow: 0 2px 12px rgba(0,0,0,0.4); }
  }

  /* ── Outer card — fills the full A5 landscape page ── */
  .receipt {
    width: 100%;
    height: 134mm;
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
    border: 2px solid #1a237e; padding: 5px 14px;
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
    padding-bottom: 4px;
    border-bottom: 1px solid #c5cae9;
  }
  .receipt-no { font-size: 12.5px; }
  .receipt-no strong { font-size: 15px; }
  .meta-date { font-size: 12px; font-weight: 600; }

  /* ── Fill-in rows ── */
  .body { margin-top: 10px; }
  .fill-row {
    display: flex;
    align-items: flex-end;
    margin-bottom: 12px;
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
    border-bottom: 1.5px dotted #555;
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

  /* ── Amount + footer anchored to bottom via margin-top: auto ── */
  .amount-row {
    display: flex;
    justify-content: flex-end;
    align-items: flex-end;
    margin-top: auto;
    margin-bottom: 6px;
    gap: 0;
  }
  .amount-box {
    border: 2px solid #1a237e;
    padding: 6px 16px;
    text-align: center;
    min-width: 130px;
  }
  .amount-label { font-size: 10px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; color: #1a237e; }
  .amount-value { font-size: 20px; font-weight: 900; margin-top: 3px; color: #1a237e; }

  /* ── Footer ── */
  .footer-divider { border-top: 1.5px solid #1a237e; margin-bottom: 8px; }
  .footer { display: flex; justify-content: space-between; align-items: flex-end; }
  .footer-left { font-size: 10px; color: #555; line-height: 1.6; padding-bottom: 3px; }
  .footer-left strong { color: #000; font-size: 10.5px; }
  .sig-block { text-align: center; min-width: 190px; }
  .for-word { font-size: 10px; margin-bottom: 1px; }
  .for-college { font-size: 11.5px; font-weight: 800; letter-spacing: 0.4px; }
  .sig-space { height: 34px; }
  .sig-line-rule { border-top: 1px solid #000; }
  .sig-text { font-size: 10px; padding-top: 3px; }
</style>
</head>
<body>
<div class="receipt">

  <!-- Header -->
  <div class="header">
    <img class="logo" src="${SKS_LOGO_DATA_URL}" alt="SKS Logo" />
    <div class="header-center">
      <div class="college-name">SKS College Of Nursing</div>
      <div class="college-sub">
        Run By VS Educational Trust (Regn. No. 579 / 1997)<br/>
        No.31, Neikkarapatti, Salem &ndash; 636 010.
      </div>
    </div>
    <div class="receipt-badge">R E C E I P T</div>
  </div>

  <!-- Receipt No (left) + Date (right) -->
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

  <!-- Amount box — margin-top: auto pulls it flush to the footer block -->
  <div class="amount-row">
    <div class="amount-box">
      <div class="amount-label">Amount</div>
      <div class="amount-value">${formattedAmount} /-</div>
    </div>
  </div>

  <!-- Footer -->
  <div class="footer-divider"></div>
  <div class="footer">
    <div class="footer-left">
      Mode of Payment: <strong>${paymentModeLabel}</strong>
    </div>
    <div class="sig-block">
      <div class="for-word">For</div>
      <div class="for-college">SKS COLLEGE OF NURSING</div>
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
  const win = window.open('', '_blank', 'width=830,height=620');
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
