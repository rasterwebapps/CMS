# SKS College of Nursing
# Cashier — User Guide

**Version 2.0 | April 27, 2026**

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Your Role at SKS College of Nursing](#2-your-role)
3. [Getting Started](#3-getting-started)
4. [Cashier Dashboard](#4-cashier-dashboard)
5. [Payment Collection](#5-payment-collection)
   - [5.1 From Enquiry Shortcuts](#51-from-enquiry-shortcuts)
   - [5.2 Cash Payments](#52-cash-payments)
   - [5.3 Cheque Payments](#53-cheque-payments)
   - [5.4 NEFT / RTGS / UPI Payments](#54-neft--rtgs--upi-payments)
   - [5.5 Card Payments](#55-card-payments)
   - [5.6 Partial Payments](#56-partial-payments)
6. [Payment Receipts](#6-payment-receipts)
7. [Payment Records](#7-payment-records)
8. [Daily Reconciliation](#8-daily-reconciliation)
9. [Reports](#9-reports)
10. [Daily Workflows](#10-daily-workflows)
11. [Troubleshooting](#11-troubleshooting)
12. [Quick Reference](#12-quick-reference)

---

## 1. Introduction

Welcome to the **SKS College of Nursing** Management System — Cashier Edition.

As a **Cashier**, you are responsible for collecting fees from students and maintaining accurate financial records. Every rupee that comes into SKS College flows through your counter. Accurate recording, proper receipts, and daily reconciliation keep the college's finances clean and audit-ready.

This guide walks you through everything you need to do your job well.

---

## 2. Your Role

### What You Can Access

| Section | Access |
|---------|--------|
| Dashboard | ✅ Cashier Dashboard |
| Enquiries | ✅ View only (for context when collecting) |
| Fee Payments — Collect | ✅ Full |
| Payment History | ✅ Full |
| Reports | ✅ Collection and receipt reports |
| Fee Finalization | ❌ College Admin only |
| Admission Creation | ❌ Front Office / College Admin only |
| Document Submission | ❌ Front Office / College Admin only |
| Preferences / Settings | ❌ Not accessible |

### What You Can and Cannot Do

| Task | Cashier | Reason |
|------|---------|--------|
| Collect student payment | ✅ YES | Primary responsibility |
| View enquiry payment history | ✅ YES | To verify payments |
| Finalize student fees | ❌ NO | Pricing authority — College Admin only |
| Submit student documents | ❌ NO | Admission process — Front Office only |
| Create student admission | ❌ NO | Enrollment authority — Front Office/Admin |
| Generate collection reports | ✅ YES | Financial accountability |

### Payment Shortcut from Enquiry List

You see the enquiry list with one key shortcut button:

| Button | Required Status | What It Does |
|--------|----------------|-------------|
| 💳 Collect Payment | `FEES_FINALIZED` or `PARTIALLY_PAID` | Opens payment form pre-filled with student data |

---

## 3. Getting Started

### Logging In

1. Open browser → navigate to CMS application
2. Redirected to **Keycloak login**
3. Enter your **college email and password**
4. Cashier dashboard loads

### Counter Setup Checklist (Start of Day)

```
BEFORE OPENING COUNTER:
  [ ] Log into CMS system
  [ ] Test receipt printer (print a test receipt)
  [ ] Count starting cash in drawer — note opening balance
  [ ] Verify card terminal is working (if available)
  [ ] Check system dashboard for any alerts
  [ ] Note any large cheques or transfers expected today
  [ ] Confirm college bank account details visible for NEFT guidance
```

---

## 4. Cashier Dashboard

### Dashboard Widgets

| Widget | What It Shows |
|--------|--------------|
| Collected Today | Total fees received today |
| Payments Processed Today | Number of transactions |
| FEES_FINALIZED Enquiries | Students ready to pay |
| Outstanding Balance (total) | All pending fee amounts |
| Collections This Month | Month-to-date total |
| Payment Methods Today | Breakdown: Cash / NEFT / UPI / Cheque / Card |

### Monthly Target Widget

A progress bar shows collection vs. month's target:
```
April 2026 Collection:
████████████░░░░ ₹4,20,000 / ₹6,00,000  (70% of target)
```

---

## 5. Payment Collection

### Payment Flow

```
╔══════════════════════════════════════════════════════════════════╗
║             CASHIER PAYMENT COLLECTION WORKFLOW                  ║
╚══════════════════════════════════════════════════════════════════╝

Student approaches counter
        │
        ▼
Cashier identifies student in system:
  • Search in Enquiries by name or phone
  • Status must be FEES_FINALIZED or PARTIALLY_PAID
        │
        ▼
Cashier verifies amount due:
  • Total finalized: ₹1,03,000
  • Previously paid: ₹0 (or previous amount)
  • Balance today:   ₹1,03,000 (or remaining)
        │
        ▼
Student pays today:
  • Cash / Cheque / NEFT / UPI / Card / DD
        │
        ▼
Cashier enters payment in system:
  Amount paid: ₹___
  Method:      ________________
  Reference:   ________________ (if applicable)
        │
        ▼
Click "Collect Payment"
        │
        ▼
System generates receipt:
  Receipt No: RCP-SKS-20260427-XXX
        │
        ▼
Print receipt → Give to student
Keep copy for daily reconciliation
        │
        ▼
Update student:
  "Payment recorded. Balance = ₹X"
  "Please collect documents when ready next"
```

---

### 5.1 From Enquiry Shortcuts

The fastest way to collect a payment is from the **Enquiry List**:

1. Navigate to **Sidenav → Admission Management → Enquiries**
2. Search for student by name or phone number
3. Look for **💳 "Collect Payment"** button (teal/green, right column)
   - This appears only if status = `FEES_FINALIZED` or `PARTIALLY_PAID`
4. Click the button
5. Payment form opens with student details pre-filled
6. Enter payment details and save

---

### 5.2 Cash Payments

**When**: Student pays in person with physical cash.

#### Procedure

1. **Count the cash** before touching the register:
   - Count once in front of student
   - Count again for confirmation
   - Announce amount: "I have received ₹50,000"

2. **Open Enquiry → Click Collect Payment**:
   - **Amount Paid**: `50000`
   - **Payment Date**: Today's date
   - **Payment Method**: `CASH`
   - **Transaction Reference**: Leave blank (not required for cash)
   - **Remarks**: e.g., `Cash received — Year 1 first instalment`

3. **Save and print receipt**
4. **Hand receipt to student**

#### Safety Tips for Cash

- Never leave cash on the counter unattended
- Use cash box / drawer with lock
- Do not mix personal cash with college cash
- Reconcile cash drawer at end of every session

---

### 5.3 Cheque Payments

**When**: Student provides a bearer/crossed cheque.

#### Procedure

1. **Verify the cheque**:
   - Payee name: **"SKS College of Nursing"** (exactly)
   - Amount in words matches amount in numbers
   - Date is today or recent (not post-dated unless authorized)
   - Signed by account holder
   - Not cancelled or overwritten

2. **Enter in system**:
   - **Amount Paid**: Amount written on cheque
   - **Payment Method**: `CHEQUE`
   - **Transaction Reference**: Cheque number (printed at bottom of cheque, e.g., `112345`)
   - **Remarks**: `Cheque #112345 — Indian Bank, Coimbatore`

3. **Give receipt to student** (with note "subject to cheque clearance")

4. **Store cheque in secure folder**:
   - Label with: Student name, enquiry ID, amount, cheque #

5. **Prepare for bank deposit** (weekly)

#### Cheque Verification Checklist

```
[ ] Payee is "SKS College of Nursing"
[ ] Amount in words = amount in numbers
[ ] Cheque is signed
[ ] Not post-dated beyond current date (unless authorized)
[ ] No overwriting or corrections
[ ] Bank name and account holder name visible
```

---

### 5.4 NEFT / RTGS / UPI Payments

**When**: Student transfers money from their bank or UPI app.

#### Types

| Type | Amount Range | Notes |
|------|-------------|-------|
| UPI | < ₹2 lakh / transaction | Instant, confirm SMS |
| NEFT | Any amount | 2-hour cycles, confirm UTR |
| RTGS | ≥ ₹2 lakh | Same-day, confirm UTR |

#### Important: College Bank Account Details

Provide students with official transfer details:
```
Beneficiary Name:  SKS College of Nursing
Account Number:    [Obtain from Finance Manager]
IFSC Code:         [Obtain from Finance Manager]
Bank & Branch:     [Obtain from Finance Manager]
UPI ID:            [If available]
```

> ⚠️ Always get these from your Finance Manager. Never share unofficial account numbers.

#### Procedure

1. **Ask student for proof of transfer**:
   - Bank SMS: "NEFT transfer ₹50,000 to SKS College of Nursing, UTR: xxxxxxxx"
   - Or UPI app screenshot with transaction ID
   - Or bank transaction reference sheet

2. **Verify transfer received** (if you have bank portal access):
   - Check that amount matches
   - Verify student name or reference in narration

3. **Enter in system**:
   - **Amount Paid**: `50000`
   - **Payment Method**: `NEFT` / `UPI`
   - **Transaction Reference**: `UTR20262704XXXXXX` or `UPI Ref: 12345ABCDE`
   - **Remarks**: `NEFT from [Bank Name]: UTR confirmed`

4. **Print receipt and give to student**

---

### 5.5 Card Payments

**When**: Student pays using credit or debit card at campus POS terminal.

#### Procedure

1. **Swipe/Tap the card** on campus POS terminal
2. Enter amount
3. Student approves on terminal
4. Terminal prints **authorization receipt**
5. Note the **Authorization Code** from terminal receipt

6. **Enter in system**:
   - **Amount Paid**: Amount from terminal
   - **Payment Method**: `CARD`
   - **Transaction Reference**: Authorization code from terminal
   - **Remarks**: `Card payment — Last 4 digits: XXXX`

7. **Give student**: both terminal slip and system receipt

#### If Terminal Fails

- Restart terminal (power off → wait 10 sec → power on)
- If still not working, offer student another method: NEFT / Cash
- Contact POS support: [Number from Finance Manager]
- Never attempt manual card entry

---

### 5.6 Partial Payments

Students may pay in **instalments**. Record each payment separately.

#### Example — Student Meena Rajendran, B.Sc Nursing (Fee: ₹1,03,000)

| Date | Payment | Method | Balance |
|------|---------|--------|---------|
| 27-Apr-2026 | ₹50,000 | NEFT | ₹53,000 |
| 15-May-2026 | ₹30,000 | CASH | ₹23,000 |
| 01-Jun-2026 | ₹23,000 | UPI | ₹0 |

- After 1st payment → Status: `PARTIALLY_PAID`
- After 2nd payment → Still: `PARTIALLY_PAID`
- After 3rd payment → Status changes to: `FEES_PAID`

> **Note**: Students can proceed to document submission even with partial payment. However, full payment must be cleared before admission.

---

## 6. Payment Receipts

Every payment generates a **system receipt** with:

```
┌──────────────────────────────────────────────────────┐
│              SKS COLLEGE OF NURSING                  │
│      [College Logo]  Coimbatore — 641 XXX            │
│   Phone: +91-422-XXXXXXX   Email: info@sks.edu.in   │
╞══════════════════════════════════════════════════════╡
│                  PAYMENT RECEIPT                      │
│                                                      │
│  Receipt No:   RCP-SKS-20260427-001                  │
│  Date:         27-Apr-2026                           │
╞══════════════════════════════════════════════════════╡
│  Student Name: Meena Rajendran                       │
│  Program:      Bachelor of Science in Nursing        │
│  Enquiry ID:   ENQ-2026-042                          │
╞══════════════════════════════════════════════════════╡
│  Amount Paid:  ₹50,000.00                            │
│  Payment Mode: NEFT                                  │
│  Reference:    UTR202604XXXXXXXX                     │
│  Collected by: Mr. Vijay Kumar (Cashier)             │
╞══════════════════════════════════════════════════════╡
│  Balance Due:  ₹53,000.00                            │
│                                                      │
│  "All fees subject to INC regulations"               │
│  This is a computer-generated receipt.               │
└──────────────────────────────────────────────────────┘
```

### Receipt Actions

| Button | What It Does |
|--------|------------|
| Print | Sends to connected printer |
| Email | Sends receipt PDF to student's registered email |
| Download | Downloads receipt as PDF to your computer |
| Close | Dismisses receipt screen |

---

## 7. Payment Records

### Accessing All Payments

All recorded payments are visible in the enquiry list.

**Navigate**: Admission Management → Enquiries → Click on student → Payments tab

The **Payments tab** shows:
- Each receipt with receipt number
- Amount, method, date
- Collected by (who recorded it)
- Balance after each payment

### Payment Statuses

| Status | Meaning |
|--------|---------|
| COMPLETED | Payment received and confirmed |
| PENDING | Recorded but awaiting bank confirmation |
| FAILED | Cheque bounced / transfer failed |
| REFUNDED | Payment returned to student |

---

## 8. Daily Reconciliation

**End-of-day reconciliation** ensures that what is in the system matches what is physically in your hands.

### Reconciliation Workflow

```
╔══════════════════════════════════════════════════════════════════╗
║            END OF DAY RECONCILIATION PROCEDURE                   ║
╚══════════════════════════════════════════════════════════════════╝

STEP 1: Generate Daily Collection Report
  • Navigate to Reports → Daily Collection Report
  • Select today's date
  • Generate report
  • Print or note totals by method:
      Cash:   ₹_______
      NEFT:   ₹_______
      UPI:    ₹_______
      Cheque: ₹_______
      Card:   ₹_______
      TOTAL:  ₹_______

STEP 2: Cash Reconciliation
  • Count all cash in drawer
  • Subtract opening balance
  • Amount = Cash received today
  • Must match system CASH total
  • If mismatch → investigate before leaving

STEP 3: Cheque Reconciliation
  • Count physical cheques received today
  • Verify each cheque amount against system
  • Prepare cheque list for weekly bank deposit:
      ┌─────┬──────────────┬────────┬───────────────┐
      │ No. │ Student      │ Amount │ Cheque #      │
      ├─────┼──────────────┼────────┼───────────────┤
      │  1  │ Meena R      │ 50,000 │ 112345        │
      │  2  │ Divya K      │ 80,000 │ 667788        │
      └─────┴──────────────┴────────┴───────────────┘

STEP 4: NEFT / UPI Verification
  • Check college bank portal (if access granted)
  • Confirm today's transfers posted
  • Match UTR/UPI reference numbers in system

STEP 5: Email Report to Finance Manager
  • Attach Daily Collection Report (PDF)
  • Note any discrepancies
  • Mention any pending cheques
  • Subject: "SKS Cashier Report — [Date]"

STEP 6: Secure and Close
  • Lock cash drawer
  • Place cheques in secure folder/safe
  • Log out of system
  • Turn off terminal
```

---

## 9. Reports

**Navigate**: Sidenav → Reports

### Reports Available to Cashier

| Report | Description | Frequency |
|--------|-------------|-----------|
| Daily Collection Report | Today's payments by method | Every day |
| Weekly Collection Summary | 7-day breakdown | Every Friday |
| Monthly Collection Report | Month's total with trends | Monthly |
| Receipt Report | Detailed list of all receipts issued | As needed |
| Outstanding Balance Report | Students with pending payments | As needed |

### How to Generate the Daily Collection Report

1. Navigate to **Reports → Daily Collection Report**
2. Date auto-fills with today
3. Click **"Generate"**
4. Review:
   - Total collected
   - Cash subtotal
   - NEFT/UPI subtotal
   - Cheque subtotal
   - Card subtotal
5. Click **"Export to PDF"** → Download
6. Email to Finance Manager

### Outstanding Balance Report

Shows students who haven't paid full fees:

1. Reports → Outstanding Balance Report
2. Optional: filter by Program (e.g., `B.Sc Nursing`)
3. Generate
4. Columns:
   - Student Name
   - Program
   - Finalized Fee
   - Paid So Far
   - **Balance Due** (highlighted)
   - Last Payment Date

**Use this for**: Follow-up call list, budget planning, fee collections drive.

---

## 10. Daily Workflows

### Workflow: Start of Business

```
8:30 AM
  ├─ Log into CMS
  ├─ Check dashboard for FEES_FINALIZED enquiries
  │      These students are ready to pay — call/message them
  ├─ Count opening cash balance
  ├─ Verify printer and terminal working
  └─ Note any cheques pending bank deposit
```

---

### Workflow: Processing a Student Payment (Step-by-Step)

```
Student arrives at cashier counter:
"I want to pay my B.Sc Nursing fees"
         │
         ▼
Cashier: "Please provide your name / phone number"
         │
         ▼
Search in Enquiries: Find "Meena Rajendran"
  Status: FEES_FINALIZED
  Fee finalized: ₹1,03,000
  Paid so far:   ₹0
  Balance:       ₹1,03,000
         │
         ▼
Cashier: "Your fee is ₹1,03,000. How much would you like to pay today?"
         │
         ▼
Student: "I'll pay ₹50,000 by NEFT. Here is the bank transfer reference."
         │
         ▼
1. Click "Collect Payment" button for this enquiry
2. Amount: 50000
3. Method: NEFT
4. Reference: UTR20262704XXXXXX
5. Date: 27-04-2026
6. Remarks: First instalment
7. Click "Collect Payment"
         │
         ▼
Receipt generated:
  RCP-SKS-20260427-001
  Amount: ₹50,000
  Balance: ₹53,000
         │
         ▼
Print receipt → Give to student

"Your payment of ₹50,000 has been recorded.
 Balance remaining: ₹53,000.
 Please clear it before starting document submission."
```

---

### Workflow: End of Day (Closing Procedure)

```
5:00 PM
  ├─ Stop accepting new payments (or as per policy)
  ├─ Generate Daily Collection Report
  │     Print or note: Cash, NEFT, UPI, Cheque, Card totals
  │
  ├─ Cash Reconciliation:
  │     Count cash → Must match system CASH total
  │     Discrepancy? → Investigate, document
  │
  ├─ Cheque Management:
  │     Count cheques → Match with system
  │     Place in secure folder labeled with date
  │
  ├─ Online Transfers:
  │     Check bank portal → Confirm transfers posted
  │
  ├─ Prepare Summary:
  │     Email to Finance Manager:
  │       • Total collected today: ₹___
  │       • Cash: ₹___, NEFT: ₹___, UPI: ₹___, Cheque: ₹___
  │       • Pending collections: [list]
  │
  └─ Secure and Log Out
```

---

## 11. Troubleshooting

### Student says they paid via UPI but system shows no payment

**Solution**:
1. Ask student to show UPI app → Transaction history
2. Note the **Transaction ID** and **Amount** and **Date**
3. Check if it matches college bank UPI ID
4. If yes → Record payment with that transaction ID
5. If no → Student may have paid wrong UPI ID
   - Contact Finance Manager for resolution

---

### Cheque bounced after deposit

**Solution**:
1. Bank notifies you (usually within 2-3 business days)
2. Update payment status to `FAILED` in CMS (contact Admin if you cannot update)
3. Call student immediately:
   - "Your cheque #112345 for ₹50,000 was returned by the bank"
   - "Please bring cash or NEFT payment within 2 days"
4. Per college policy, a cheque bounce fee may apply
5. Do not update enquiry status until cleared
6. Document the bounce with bank memo as evidence

---

### Cash amount doesn't match at end of day

**Solution**:
1. Do NOT skip this step
2. Recount cash carefully (in a quiet space)
3. Review all cash transactions entered today:
   - Navigate to Reports → Daily Collection → Cash only
4. Compare each entry vs. your paper receipt stubs
5. Discrepancy found?
   - Short (less cash than expected) → May have given wrong change; check all transactions
   - Over (more cash than expected) → May have over-collected; contact student
6. Document finding regardless
7. Report to Finance Manager before leaving

---

### Card terminal not working

**Solution**:
1. Restart terminal: power off → wait 10 seconds → power on
2. Re-attempt the transaction
3. If still failing:
   - Offer student alternative: UPI / Cash / NEFT
   - Note terminal failure on your daily report
   - Contact IT or terminal provider for repair

---

### Printed receipt is blank

**Solution**:
1. Check if paper roll is loaded (thermal paper, correct side up)
2. Try printing a test receipt from printer test button
3. If test receipt also blank → Paper loaded wrong way; flip it
4. If paper is good but still blank → Call IT support
5. In the meantime: show receipt on screen to student and email the PDF

---

## 12. Quick Reference

### Payment Method Quick Summary

| Method | Reference Needed | Notes |
|--------|-----------------|-------|
| CASH | None | Count twice before recording |
| CHEQUE | Cheque number | Verify payee, amount, signature |
| NEFT | UTR number | Confirm with bank portal |
| UPI | Transaction ID | Get screenshot from student |
| CARD | Auth code | From POS terminal receipt |
| DD | DD number | Similar to cheque |

### SKS Bank Details for NEFT Guidance to Students

```
Beneficiary:   SKS College of Nursing
Account No:    [From Finance Manager]
IFSC Code:     [From Finance Manager]
Bank & Branch: [From Finance Manager]
UPI ID:        [If enabled by college]
```
*(Fill in above from your Finance Manager)*

### Reporting Deadlines

| Report | Submit To | Deadline |
|--------|-----------|---------|
| Daily Collection Report | Finance Manager | Before 5:30 PM |
| Weekly Collection Summary | Finance Manager | Every Friday 5:00 PM |
| Monthly Report | Principal + Finance Manager | 1st of next month |
| Cheque Deposit List | Finance Manager | Before bank deposit |

### Cashier End-of-Day Checklist

```
[ ] Generate and print Daily Collection Report
[ ] Count cash — matches system?
[ ] Count cheques — matches system?
[ ] Verify NEFT/UPI from bank portal
[ ] Email report to Finance Manager
[ ] Lock cash drawer
[ ] Store cheques in secure folder
[ ] Log out of CMS
[ ] Switch off terminal and printer
[ ] Record closing cash balance
[ ] Note any discrepancies in logbook
```

---

**SKS College of Nursing | Cashier User Guide**
**Version 2.0 | April 27, 2026 | Next Review: July 2026**

