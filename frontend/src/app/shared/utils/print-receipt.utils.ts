import { numberToWords } from './number-to-words.utils';

export interface ReceiptPrintData {
  receiptNumber: string;
  /** Primary payer name (student or enquiry person). */
  payerName: string;
  /** Identifier shown below name: roll number for students, empty/null for enquiries. */
  payerIdentifier?: string | null;
  /** Optional program name. */
  programName?: string | null;
  amountPaid: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference?: string | null;
  installmentBreakdown: Array<{ installmentLabel: string; amountApplied: number }>;
}

export function printFeeReceipt(data: ReceiptPrintData): void {
  const amountWords = numberToWords(data.amountPaid);
  // Append 'T00:00:00' to avoid UTC-to-local-timezone shift on ISO date strings
  const formattedDate = new Date(data.paymentDate + 'T00:00:00').toLocaleDateString('en-IN', {
    day: '2-digit', month: 'long', year: 'numeric',
  });
  const formattedAmount = data.amountPaid.toLocaleString('en-IN');
  const towards = data.installmentBreakdown
    .map(s => `${s.installmentLabel} (₹${s.amountApplied.toLocaleString('en-IN')})`)
    .join(', ');
  const payerLine = data.payerIdentifier
    ? `${data.payerName} (${data.payerIdentifier})`
    : data.payerName;

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<title>Receipt - ${data.receiptNumber}</title>
<style>
  @page { size: A5 landscape; margin: 10mm; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: 'Times New Roman', Times, serif; background: #fff; color: #000; }
  .receipt {
    width: 100%; border: 3px double #1a237e;
    padding: 14px 18px 18px; min-height: 140mm;
    display: flex; flex-direction: column;
  }
  .header { display: flex; align-items: flex-start; gap: 16px; padding-bottom: 10px; border-bottom: 1px solid #1a237e; }
  .logo { width: 64px; height: 64px; flex-shrink: 0; }
  .header-text { flex: 1; }
  .college-name { font-size: 24px; font-weight: 900; color: #1a237e; letter-spacing: 0.5px; line-height: 1.1; text-transform: uppercase; }
  .college-sub { font-size: 10px; color: #333; margin-top: 3px; line-height: 1.5; }
  .receipt-badge { display: inline-block; border: 2px solid #1a237e; padding: 2px 14px; font-size: 13px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; color: #1a237e; margin-top: 8px; }
  .meta-row { display: flex; justify-content: space-between; align-items: flex-start; margin-top: 12px; }
  .receipt-no { font-size: 12px; }
  .receipt-no strong { font-size: 15px; }
  .amount-box { border: 2px solid #1a237e; padding: 6px 14px; text-align: center; min-width: 110px; }
  .amount-box .rs-label { font-size: 11px; font-weight: 700; letter-spacing: 1px; }
  .amount-box .rs-value { font-size: 17px; font-weight: 900; margin-top: 2px; letter-spacing: 0.5px; }
  .body { flex: 1; margin-top: 14px; }
  .line { display: flex; align-items: baseline; gap: 4px; margin-bottom: 12px; font-size: 13px; line-height: 1.6; }
  .line-label { white-space: nowrap; }
  .line-dots { flex: 1; border-bottom: 1px dotted #555; margin: 0 6px 3px; min-width: 60px; }
  .line-value { font-weight: 600; }
  .date-row { display: flex; justify-content: flex-end; margin-top: 6px; font-size: 12px; }
  .footer { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 20px; padding-top: 10px; border-top: 1px solid #1a237e; font-size: 12px; }
  .footer-sig { text-align: center; }
  .footer-sig .for-label { font-size: 10px; margin-bottom: 2px; }
  .footer-sig .college-for { font-weight: 700; letter-spacing: 0.3px; }
  .sig-line { border-top: 1px solid #000; margin-top: 28px; padding-top: 3px; font-size: 10px; text-align: center; min-width: 120px; }
</style>
</head>
<body>
<div class="receipt">
  <div class="header">
    <img class="logo" src="/assets/images/sks-logo-icon.svg" alt="SKS Logo" />
    <div class="header-text">
      <div class="college-name">SKS College of Nursing</div>
      <div class="college-sub">
        Run By VS Educational Trust (Regn. No. 579 / 1997)<br/>
        No.31, Neikkarapatti, Salem &ndash; 636 010.
      </div>
    </div>
    <div class="receipt-badge">Receipt</div>
  </div>

  <div class="meta-row">
    <div class="receipt-no">No. <strong>${data.receiptNumber}</strong></div>
    <div class="amount-box">
      <div class="rs-label">Rs.</div>
      <div class="rs-value">${formattedAmount} /-</div>
    </div>
  </div>

  <div class="body">
    <div class="line">
      <span class="line-label">Received with thanks from</span>
      <span class="line-dots"></span>
      <span class="line-value">${payerLine}</span>
    </div>
    <div class="line">
      <span class="line-label">the sum of Rupees</span>
      <span class="line-dots"></span>
      <span class="line-value">${amountWords}</span>
    </div>
    <div class="line">
      <span class="line-label">towards</span>
      <span class="line-dots"></span>
      <span class="line-value">${towards}</span>
    </div>
    ${data.transactionReference ? `<div class="line"><span class="line-label">Ref. No.</span><span class="line-dots"></span><span class="line-value">${data.transactionReference}</span></div>` : ''}
  </div>

  <div class="date-row">Date : ${formattedDate}</div>

  <div class="footer">
    <div></div>
    <div class="footer-sig">
      <div class="for-label">For</div>
      <div class="college-for">SKS COLLEGE OF NURSING</div>
      <div class="sig-line">Authorised Signature</div>
    </div>
  </div>
</div>
<script>window.onload = function() { window.print(); };</script>
</body>
</html>`;

  const win = window.open('', '_blank', 'width=900,height=650');
  if (win) {
    win.document.write(html);
    win.document.close();
  }
}
