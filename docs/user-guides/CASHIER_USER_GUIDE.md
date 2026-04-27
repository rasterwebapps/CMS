# College Management System - Cashier User Guide

## Version 1.0 | April 27, 2026

---

## Table of Contents

1. [Introduction](#introduction)
2. [Role Overview](#role-overview)
3. [Getting Started](#getting-started)
4. [Cashier Dashboard](#cashier-dashboard)
5. [Payment Collection](#payment-collection)
6. [Payment Records](#payment-records)
7. [Reports](#reports)
8. [Common Workflows](#common-workflows)
9. [Troubleshooting](#troubleshooting)
10. [Banking & Cash Handling](#banking--cash-handling)
11. [Keyboard Shortcuts](#keyboard-shortcuts)

---

## Introduction

Welcome to the College Management System! This guide is specifically designed for **Cashiers** and **Finance Staff**.

As a Cashier, your primary responsibility is collecting fees from students, maintaining accurate payment records, and ensuring proper financial documentation. You are a key member of the revenue collection team.

### About This Document

This handbook walks you through all features available to your role, with focus on efficient payment collection and accurate record-keeping.

---

## Role Overview

### What is a Cashier's Responsibility?

A Cashier is responsible for:
- ✅ Collecting student fee payments (cash, check, online, card)
- ✅ Recording payments in the system
- ✅ Issuing receipts and proof of payment
- ✅ Maintaining payment records
- ✅ Reconciling daily collections
- ✅ Reporting revenue collected
- ✅ Handling inquiries about payment status
- ✅ Supporting payment exceptions

### Permissions Summary

| Area | Permission | Access |
|------|-----------|--------|
| Payment Collection | Full | ✅ Record all payment types |
| Payment History | View | ✅ View student payment records |
| Reports | View | ✅ Collection and receipt reports |
| Enquiries | Read-only | ✅ View enquiry status (for context) |
| Fee Configuration | No Access | ❌ (College Admin only) |
| Admission Management | Limited | ⚠️ View only (for payment context) |

### Key Restrictions

- ✅ YOU CAN: Collect payments
- ✅ YOU CAN: View payment history
- ❌ YOU CANNOT: Finalize fees (College Admin only)
- ❌ YOU CANNOT: Create admissions
- ❌ YOU CANNOT: Submit documents
- ❌ YOU CANNOT: Access preferences or settings

---

## Getting Started

### Login Process

1. **Open Application**: Navigate to CMS application URL
2. **Keycloak Login**: Enter your credentials
   - Username: Your college email
   - Password: Your Keycloak password
3. **Cashier Dashboard**: Access your payment-focused dashboard

### First-Time Setup

**Verify Your Profile**:
- Click avatar (top-right)
- Confirm role shows "Cashier"
- Verify your name and contact info

**Familiarize Yourself**:
- Dashboard shows payment metrics
- Limited menu (only payment-related options)
- Cannot access system configuration

---

## Cashier Dashboard

### Dashboard Overview

Your dashboard displays real-time payment and collection metrics.

### Key Widgets

#### Today's Collections
- **Amount Collected Today**: Total revenue received so far
- **Payments Recorded**: Number of payments processed
- **Outstanding Balance**: Total still owed by all students

**Action**: Click metrics to see detailed lists

#### Payment Methods Today
- **Cash**: Amount collected via cash
- **Online**: Amount received via bank transfer
- **Check**: Checks received (value, not cleared)
- **Card**: Credit/debit card payments

#### Collection Targets
- **Target for Month**: Goal set by finance team
- **Collected So Far**: Year-to-date total
- **Status**: On track or behind

**Blue bar** shows progress toward target

#### Recent Payments
- **Latest 5 payments** processed
- **Student name**, **amount**, **method**
- Click to see full receipt

---

## Payment Collection

### Overview

Payment collection is your core function. The system supports multiple payment methods.

**Access**: Sidenav → Admission Management → Enquiries (or direct shortcuts)

### Payment Collection Methods

#### Method 1: From Enquiry List (Recommended)

1. **Navigate to Enquiries**:
   - Sidenav → Admission Management → Enquiries

2. **Find Student**:
   - Search by name, phone, or email
   - Filter by status: "FEES_FINALIZED" or "PARTIALLY_PAID"
   - These students are ready to pay

3. **Look for "Collect Payment" button**:
   - Appears only for eligible enquiries (right side)
   - Teal/success colored button
   - Hover to see tooltip

4. **Click "Collect Payment"**:
   - Opens payment collection form
   - Enquiry details pre-filled:
     - Student name
     - Program
     - Finalized fee amount
     - Any previous payments

5. **Enter Payment Details**:
   - **Amount Paid**: How much student is paying
   - **Payment Date**: Date of payment (defaults to today)
   - **Payment Method**: Select from options
   - **Transaction Reference**: Required for online/card

6. **Click "Collect Payment"**:
   - Payment recorded
   - Receipt generated
   - Confirmation displayed

#### Method 2: Direct Payment Entry

1. **Navigate to Enquiries**
2. **Find the student**
3. **Click on student name** to open detail view
4. **Click "Payments" tab**
5. **Click "Record Payment" button**
6. Follow same process as Method 1

### Payment Methods & Procedures

#### 1. CASH Payment

**When to use**: Student pays in person at college

**Process**:
1. Student brings cash to your counter
2. You count and verify amount
3. Record in Payment Collection form:
   - **Amount**: Actual cash received
   - **Payment Method**: Select "CASH"
   - **No transaction reference needed**
4. Click "Collect Payment"
5. System generates receipt with receipt number
6. Provide receipt to student

**Receipt Handling**:
- Keep one copy for records
- Give student a copy
- Note receipt number with cash deposit

**Important**: Reconcile cash at end of day

---

#### 2. ONLINE Payment (Bank Transfer)

**When to use**: Student transfers money via net banking or UPI

**Process**:
1. Student makes bank transfer (provide college account details)
2. Student brings proof (SMS, bank screenshot, app notification)
3. You verify transfer amount on bank account (if you have access)
4. Record in Payment Collection form:
   - **Amount**: Transferred amount
   - **Payment Method**: Select "ONLINE"
   - **Transaction Reference**: Bank reference/confirmation number (ask student)
   - **Remarks**: Optional - "UTR: [number]" or "Reference ID: [number]"
5. Click "Collect Payment"
6. Receipt generated
7. Provide receipt to student
8. Keep proof document (SMS screenshot, bank printout)

**Bank Reconciliation**:
- Verify transfer in your bank account (daily)
- Match amount and reference number
- Flag any discrepancies

---

#### 3. CHECK/CHEQUE Payment

**When to use**: Student provides check as payment

**Process**:
1. Student provides physical check
2. Verify:
   - Check is signed
   - Check is dated (not post-dated unless special)
   - Payee name is college name
   - Amount is legible and correct
3. Record in Payment Collection form:
   - **Amount**: Check amount
   - **Payment Method**: Select "CHEQUE"
   - **Transaction Reference**: Cheque number from the check
   - **Remarks**: "Check #12345 from [Bank Name]"
4. Click "Collect Payment"
5. Receipt generated (notes check pending clearance)
6. Provide receipt to student
7. **Store physical check** in safe deposit/lock box

**Check Clearing Process**:
1. Deposit checks to bank periodically (daily/weekly)
2. Bank clears check (2-3 business days usually)
3. Amount reflects in college bank account
4. Follow up on bounced checks immediately

---

#### 4. CARD Payment (Credit/Debit)

**When to use**: Student pays via card at your counter

**Process**:
1. Student brings credit/debit card
2. Use card payment terminal (if available at your college):
   - Swipe or insert card
   - Enter amount
   - Terminal generates transaction receipt/authorization code
3. Get authorization and note transaction ID
4. Record in Payment Collection form:
   - **Amount**: Authorized amount
   - **Payment Method**: Select "CARD"
   - **Transaction Reference**: Terminal transaction ID / Authorization code
   - **Remarks**: "Card Last 4: 5678 - VISA" or similar
5. Click "Collect Payment"
6. Receipt generated
7. Provide terminal receipt + system receipt to student
8. Keep transaction record

**If No Terminal Available**:
- Collect card details manually (write down last 4 digits, expiry)
- But this is NOT RECOMMENDED for security reasons
- Prefer: Online payment by student

---

### Handling Partial Payments

If student pays less than full amount:

1. Record actual amount received (e.g., ₹30,000 toward ₹100,000 fee)
2. System automatically calculates remaining balance
3. Status changes to "PARTIALLY_PAID"
4. Receipt shows:
   - Amount paid: ₹30,000
   - Remaining: ₹70,000
   - "Please complete payment by [date]"
5. Student can make additional payments later
6. Each payment generates separate receipt number

**Example**:
```
Finalized Fee: ₹100,000
First Payment: ₹50,000 (Receipt: RCP001)
Status: PARTIALLY_PAID
Remaining: ₹50,000

Later:
Second Payment: ₹50,000 (Receipt: RCP002)
Status: FEES_PAID (complete)
```

### Payment Receipts

#### Receipt Content

System-generated receipt includes:
- **Receipt Number**: Unique identifier (e.g., RCP20260427001)
- **Student Name**: Who paid
- **Student ID/Enquiry ID**: For reference
- **Program**: Their program
- **Amount Paid**: Rupee amount with decimal
- **Date**: Payment date
- **Method**: CASH / ONLINE / CHEQUE / CARD
- **Balance Remaining**: If partial payment
- **Collected By**: Your name
- **College Stamp/Seal**: For authenticity

#### Receipt Actions

After payment is recorded:

**Print**:
- Click "Print" to print receipt
- Provide printed copy to student
- Keep one for your records

**Email**:
- Click "Email Receipt"
- System sends receipt to student's email
- Student has digital proof

**Save/Archive**:
- Receipt automatically saved in system
- Accessible later via payment history
- Cannot be edited or deleted (audit trail)

---

## Payment Records

### Accessing Payment History

**Method 1: From Student Enquiry**
1. Navigate to Enquiries
2. Find student
3. Click to open student detail
4. Click "Payments" tab
5. View all payments for that student

**Method 2: Payment Records**
1. Navigate to Payment Records (if separate menu option)
2. View all payments across all students
3. Search by receipt number, student name, date range

### Understanding Payment Status

| Status | Meaning |
|--------|---------|
| PENDING | Payment recorded but not yet verified |
| COMPLETED | Payment received and verified |
| FAILED | Payment attempt failed (e.g., check bounced) |
| REFUNDED | Payment returned to student |

### Daily Payment Reconciliation

**End of Day Routine**:

1. **Total Cash Collected**:
   - Sum all CASH payments entered today
   - Count physical cash in drawer
   - Verify amounts match
   - If discrepancy, note and investigate

2. **Online Transfers**:
   - Check bank account balance
   - Verify all transfers posted
   - Match reference numbers to system records

3. **Checks Received**:
   - Count checks
   - Verify amounts entered match physical checks
   - Prepare for bank deposit

4. **Card Transactions**:
   - Review terminal receipts
   - Match to system records
   - Verify authorization codes

5. **Generate Daily Report**:
   - Navigate to Reports
   - Generate "Daily Collection Report"
   - Email to Finance Manager
   - Shows total collected by method

### Payment Exceptions

#### Bounced Check
1. Bank notifies you check bounced
2. Update payment status to "FAILED"
3. Contact student immediately
4. Request resubmission or alternate payment method
5. Note in student record the bounce date
6. Follow college policy on fees for bounce

#### Failed Online Transfer
1. Student complains payment didn't go through
2. Check college bank account
3. Verify transfer didn't post
4. Ask student to provide failed transaction proof
5. Request student retry transfer
6. Don't record payment until confirmed

#### Incorrect Amount
1. Student paid ₹5,000 instead of ₹5,500
2. Record actual amount paid (₹5,000)
3. Status becomes "PARTIALLY_PAID"
4. Address remaining balance with student
5. Ask when they can pay remainder

---

## Reports

### Overview

Reports help you track collections, analyze payment patterns, and meet financial reporting requirements.

**Access**: Sidenav → Reports

### Available Reports for Cashier

#### 1. Daily Collection Report

**Shows**: Payment collected today

**How to Generate**:
1. Click "Daily Collection Report"
2. Date auto-fills with today
3. Click "Generate"
4. Report shows:
   - Total collected today
   - Payment method breakdown
   - Student-wise collections
   - Outstanding balance for each student

**Use**: End-of-day reconciliation, giving to Finance Manager

**Export**: Click "Export to Excel" or "Email"

---

#### 2. Weekly Collection Report

**Shows**: Payments for selected week

**How to Generate**:
1. Click "Collection Report" (or "Weekly Collection")
2. Select week:
   - Start Date: Monday of week
   - End Date: Friday of week (or Sunday if including weekend)
3. Click "Generate"
4. Report shows:
   - Total for week: ₹X
   - Daily breakdown (Mon, Tue, Wed, etc.)
   - Payment methods used
   - Top collecting days

**Use**: Track week-over-week performance, Plan cash deposits

---

#### 3. Monthly Collection Report

**Shows**: All payments for the month

**How to Generate**:
1. Click "Collection Report"
2. Select month:
   - Start Date: 1st of month
   - End Date: Last day of month
3. Click "Generate"
4. Report displays:
   - Total collected this month
   - Comparison to previous month
   - Trend analysis (increasing/decreasing)
   - Program-wise collections
   - Collection method analysis

**Use**: Monthly financial statements, Management review

---

#### 4. Receipt Report

**Shows**: Detailed receipt information

**How to Generate**:
1. Click "Receipt Report"
2. Optional filters:
   - Receipt Number Range
   - Student Name
   - Date Range
   - Payment Method
3. Click "Generate"
4. Lists all receipts matching criteria
5. Shows receipt number, amount, date, method

**Use**: Audit trail, Verification of receipts issued

---

#### 5. Outstanding Balance Report

**Shows**: Students who haven't paid full fees

**How to Generate**:
1. Click "Outstanding Balance Report"
2. Optional filter: Program
3. Click "Generate"
4. Shows:
   - Student name
   - Finalized fee amount
   - Amount paid so far
   - **Remaining balance** (highlighted)
   - Last payment date
   - Days since last payment

**Use**: Follow-up calls to students, Cash flow planning

---

### Exporting and Sharing Reports

**Export Options**:

**Excel**:
- Click "Export to Excel"
- File downloads (.xlsx format)
- Can further analyze in Excel

**PDF**:
- Click "Export to PDF"
- Formatted report
- Good for printing/archiving

**Email**:
- Click "Email Report"
- Enter recipient email
- Report sends as attachment

**Print**:
- Click "Print"
- Sends to configured printer
- Keep hard copy for files

---

## Common Workflows

### Workflow 1: Morning Shift Start

**6:00 AM - Preparation**
1. Log into system
2. Check dashboard for any alerts
3. Verify computer, printer, card terminal working
4. Test receipt printer
5. Count starting cash drawer
6. Record opening balance

**8:30 AM - Start of Business**
1. Navigate to Enquiries
2. Filter by "FEES_FINALIZED" or "PARTIALLY_PAID" status
3. This shows students ready to pay
4. Prepare desk for payments
5. Wait for students to arrive

---

### Workflow 2: Processing a Student Payment

**When student arrives to pay**:

1. **Greet student politely**
   - "Welcome to our college!"
   - "How can I help you?"

2. **Identify student**:
   - Ask name
   - Verify enrollment/enquiry ID (if available)
   - Pull up record

3. **Explain amount due**:
   - "Your finalized fee is ₹100,000"
   - If partial: "You've paid ₹30,000 so far, remaining is ₹70,000"
   - Allow flexibility if financial hardship

4. **Accept payment**:
   - Ask preferred method: cash, check, or online?
   - For online: provide account details, explain UPI/transfer
   - For cash/check: process immediately
   - For card: run through terminal

5. **Record in system**:
   - Navigate to Enquiries
   - Click "Collect Payment" or Payment Record
   - Enter payment details
   - Save

6. **Generate receipt**:
   - Print receipt
   - Provide to student
   - Keep copy for records

7. **Thank student**:
   - "Thank you for your payment!"
   - "Your receipt number is RCP20260427001"
   - "If you need anything else, please ask"

---

### Workflow 3: End of Day Cash Reconciliation

**5:00 PM - End of Day**

1. **Stop taking new payments** (or close your counter)

2. **Cash Reconciliation**:
   - Open cash register/drawer
   - Count all cash received today
   - Verify against:
     - System total of CASH payments
     - Receipts issued for cash
   - Any discrepancy? Investigate immediately

3. **Check Reconciliation**:
   - Count all checks received
   - Verify amount on each
   - Verify payee is college
   - Compare to system records
   - Lock checks in safe deposit

4. **Card Reconciliation**:
   - Review card terminal receipts
   - Compare to system records
   - Match transaction IDs

5. **Online Transfer Reconciliation**:
   - Log into college bank account (if authorized)
   - Check if new transfers posted
   - Verify amounts match system

6. **Generate Daily Report**:
   - Navigate to Reports
   - Generate "Daily Collection Report"
   - Shows total by method
   - Email to Finance Manager

7. **Prepare Deposit** (if needed):
   - Bag cash
   - Bundle checks with deposit form
   - Label with date and amount
   - Give to authorized person for bank deposit

8. **Close Out**:
   - Log out of system
   - Lock cash drawer
   - Turn off terminal
   - Leave desk clean

---

### Workflow 4: Weekly Bank Deposit

**Once per week (usually Friday or Monday)**:

1. **Gather all deposits**:
   - Cheques collected during week
   - Cash (if not bagged daily)
   - Deposit forms with bank account details

2. **Prepare deposit**:
   - Count total amount
   - List each cheque:
     - From: Student name
     - Cheque #
     - Amount
   - Count total checks
   - Count total cash

3. **Go to bank**:
   - Take deposit package
   - Provide filled deposit form
   - Bank counts and verifies
   - Bank provides deposit receipt

4. **Keep receipt**:
   - File receipt in college records
   - Reference number for records
   - Bank confirmation

5. **Record deposit**:
   - Back at office
   - Note deposit date and amount in records
   - Can reference for audits

---

## Troubleshooting

### Issue: Student says they already paid but system shows no payment

**Causes & Solution**:

1. **Payment not yet recorded**:
   - Ask when they paid
   - When payment method (cash/online/check)
   - Ask for receipt or proof
   - If they have receipt number, search system by receipt #
   - If no receipt, ask for proof (SMS for online, cheque images, etc.)
   - Manually record payment if proof provided

2. **Payment in different academic year**:
   - Payment might be for previous year
   - Check filters and date range
   - Expand search to include prior years

3. **System not refreshed**:
   - Refresh page (F5)
   - Log out and log back in
   - Clear browsercache

---

### Issue: Card payment terminal not working

**Troubleshooting**:

1. **Check physical connections**:
   - Is terminal plugged in?
   - Is it turned on?
   - Any error messages on display?

2. **Restart terminal**:
   - Turn off
   - Wait 10 seconds
   - Turn back on
   - Try payment again

3. **Alternative payment**:
   - If still not working, offer alternatives:
     - Cash payment
     - Check payment
     - Online transfer
     - Come back later
   - Don't hold up student

4. **Contact support**:
   - Call payment terminal provider
   - Report issue
   - Get estimated repair time

---

### Issue: Receipt printer not printing

**Solutions**:

1. **Check printer**:
   - Is printer plugged in and on?
   - Is paper loaded?
   - Any error lights on?

2. **Test print**:
   - Try printing a test receipt from system
   - Or try printing from another application
   - Determines if issue is system or printer

3. **Restart printer**:
   - Turn off printer
   - Wait 5 seconds
   - Turn back on
   - Try printing again

4. **Generate alternate receipt**:
   - If still no printing
   - Email receipt to student (if they provide email)
   - Print from email later
   - Or issue paper receipt acknowledgment with receipt #

---

### Issue: Payment amount doesn't match what student says they paid

**Resolution**:

1. **Clarify with student**:
   - "I have ₹5,000 recorded. Is that correct?"
   - Sometimes students remember paying different amount

2. **If student paid more**:
   - "Actually I paid ₹5,500"
   - Record the correct amount student provided
   - If paid with cash, recount
   - If online transfer, check bank account
   - Update amount to correct figure
   - Generate adjusted receipt

3. **If student paid less**:
   - "Actually I only gave ₹4,500"
   - Count cash in drawer again
   - Match to system
   - Record correct amount

4. **If large discrepancy**:
   - Inform supervisor immediately
   - Don't process into reconciliation until resolved
   - Investigate thoroughly

---

### Issue: Student wants refund

**Process**:

1. **Verify reason** for refund:
   - Student fee waived by college admin
   - Student withdrawing from program
   - Overpayment/duplicate payment
   - Other reason

2. **Get authorization**:
   - Cannot process refunds independently
   - Must get approval from:
     - Finance Manager
     - Or College Admin
   - Keep written authorization

3. **Process refund**:
   - Contact Finance Manager for procedure
   - Usually involves:
     - Returning cash if possible
     - Or refund check from college account
   - Document refund reason
   - Mark payment as refunded in system

4. **Get student signature**:
   - On refund form/receipt
   - Acknowledge they received money back

---

## Banking & Cash Handling

### Best Practices for Cash

**Security**:
- Never leave cash unattended
- Keep cash register locked when not in use
- Don't disclose drawer total to students
- Use safe deposit box for large amounts
- Don't carry large cash amounts alone

**Accuracy**:
- Count cash carefully before handing to student
- Use cash counting machine if available
- Verify exchanged amount with student
- Get signature for large amounts

**Records**:
- Keep receipt stubs showing amounts
- File receipts daily
- Don't lose receipts - audit trail
- Reconcile daily without fail

### Bank Account Details

**College Bank Account**:
- Account Number: [Get from your Finance Manager]
- Bank Name: [Bank details]
- SWIFT Code: [if international]
- IFSC Code: [for Indian transfers]
- Student Transfer Instructions: Provide to students who want to pay online

### Deposits to Bank

**Frequency**: [College decision]
- Daily
- Weekly
- Twice weekly

**Process**:
1. Prepare deposit envelope/bag
2. List contents (checks, cash, any notes)
3. Seal envelope
4. Get two signatures
5. Take to bank during business hours
6. Provide deposit receipt to Finance Manager

### Handling Checks

**Receiving**:
- Verify payee is college name
- Verify check is signed
- Confirm amount is legible
- Check not post-dated (unless approved)
- Get student contact info

**Processing**:
- Store in secure location
- Don't deposit immediately (let student confirm funds available)
- Create backup list of checks
- Reconcile with bank deposits

**Bounced Checks**:
- Bank notifies if check doesn't clear
- Contact student immediately
- Request alternative payment
- Document the bounce
- Charge bounce fee (if college policy allows)

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl + K` | Open search |
| `Escape` | Close dialog |
| `Enter` | Submit form |
| `Ctrl + P` | Print receipt |
| `Alt + A` | Go to Admission Management |
| `Alt + R` | Go to Reports |

---

## Daily Checklist

- [ ] Count starting cash drawer
- [ ] Test receipt printer
- [ ] Test card terminal (if available)
- [ ] Check system for alerts
- [ ] Log payment transactions throughout day
- [ ] Process all payments clearly
- [ ] Generate receipts for all payments
- [ ] Reconcile cash at end of day
- [ ] Generate daily reconciliation report
- [ ] Count ending cash drawer
- [ ] Lock cash drawer
- [ ] Prepare bank deposit if needed
- [ ] Log out and secure workstation

---

## Performance Metrics

Track these metrics:
- **Total collected per day**
- **Number of payments processed**
- **Average payment amount**
- **Payment methods breakdown** (% cash vs online vs check vs card)
- **Customer satisfaction** (based on feedback)
- **Accuracy** (cash reconciliation - should be 100%)
- **Outstanding balance trends** (is it increasing or decreasing)

---

## Quick Reference - Common Scenarios

| Scenario | Action |
|----------|--------|
| Student wants to pay ₹50K now, ₹50K later | Record full amount in system as 2 separate payments |
| Student overpaid by ₹500 | Record full amount, note overpayment in remarks, follow up with supervisor |
| Check bounced after being deposited | Contact student, request retry, mark payment as failed |
| Student wants receipt for tax purposes | Print from system, ensure receipt shows all details |
| Can't find student in system | Ask for enquiry ID or call/email to get enrollment info |

---

## Support & Help

### Need Assistance?
- Check Troubleshooting section above
- Ask your Finance Manager
- Contact system administrator

### Contact Information
- Finance Manager: [Name/Email/Phone]
- Support Email: [support@college.edu]
- Support Phone: [phone number]

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Apr 27, 2026 | Initial cashier guide |

---

**Last Updated**: April 27, 2026  
**Next Review**: July 27, 2026

**Thank you for accurate and efficient financial operations!**

