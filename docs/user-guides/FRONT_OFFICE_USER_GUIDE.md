# SKS College of Nursing
# Front Office Staff — User Guide

**Version 2.0 | April 27, 2026**

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Your Role at SKS College of Nursing](#2-your-role-at-sks-college-of-nursing)
3. [Getting Started](#3-getting-started)
4. [Front Office Dashboard](#4-front-office-dashboard)
5. [Enquiry Management](#5-enquiry-management)
   - [5.1 Creating an Enquiry](#51-creating-an-enquiry)
   - [5.2 Managing the Enquiry List](#52-managing-the-enquiry-list)
   - [5.3 Enquiry Detail View](#53-enquiry-detail-view)
   - [5.4 Status Workflow](#54-status-workflow)
6. [Payment Collection](#6-payment-collection)
7. [Document Management](#7-document-management)
8. [Admission Creation](#8-admission-creation)
9. [Reports](#9-reports)
10. [Daily Workflows](#10-daily-workflows)
11. [Troubleshooting](#11-troubleshooting)
12. [Quick Reference](#12-quick-reference)

---

## 1. Introduction

Welcome to the **SKS College of Nursing** Management System — Front Office Edition.

As a **Front Office representative**, you are the first face students and parents see when they contact SKS. You create enquiry records, guide students through the admission process, collect payments, and upload documents. Your role spans from first contact all the way to creating the student's admission record.

---

## 2. Your Role at SKS College of Nursing

### What You Can Access

| Section | Access |
|---------|--------|
| Dashboard | ✅ Front Office Dashboard |
| Admission Management — Enquiries | ✅ Full (Create, Edit, Update Status) |
| Admission Management — Document Submission | ✅ Full |
| Admission Management — Admission Completion | ✅ Full |
| Admission Management — Admissions & Students | ✅ Read |
| Finance — Collect Payment | ✅ Full |
| Reports | ✅ Admission & Collection reports |
| Preferences (Academic Setup) | ❌ College Admin only |
| Finance — Fee Finalization | ❌ College Admin only |

### What You Cannot Do

- ❌ Configure programs, courses, fees, faculty
- ❌ Finalize student fees (only College Admin can do this)
- ❌ Access system settings

### Actions Available to You from Enquiry Shortcuts

From the **Enquiries list row**, you will see these shortcut buttons:

| Button | Required Status | What It Does |
|--------|----------------|-------------|
| 💳 Collect Payment | `FEES_FINALIZED` or `PARTIALLY_PAID` | Jump to payment form pre-filled with this enquiry |
| 📁 Submit Documents | `FEES_PAID` or `PARTIALLY_PAID` | Jump to document upload screen |
| 🎓 Create Admission | `DOCUMENTS_SUBMITTED` (from detail view) | Open admission creation form |

---

## 3. Getting Started

### Logging In

1. Open your browser → CMS Application URL
2. Redirected to **Keycloak login screen**
3. Enter your **college email and password**
4. Front Office Dashboard loads

### Checklist Before You Start

- [ ] Confirm role shows "Front Office" in your profile (avatar → top-right)
- [ ] Familiarize yourself with the left sidebar navigation
- [ ] Note: You can see Admission Management and Reports only
- [ ] Ensure programme brochures and fee information are available to share with students

---

## 4. Front Office Dashboard

Your dashboard shows metrics relevant to your daily responsibilities.

### Dashboard Widgets

| Widget | What It Shows |
|--------|--------------|
| Today's New Enquiries | Enquiries created today |
| Awaiting Documents | Students in `FEES_PAID` status (need documents collected) |
| Awaiting Payments | Students in `FEES_FINALIZED` (need to pay) |
| Ready for Admission | Students in `DOCUMENTS_SUBMITTED` |
| Monthly Conversions | Enquiries converted to admissions this month |
| Collections Today | Payments recorded today by you |

### Quick Actions

| Button | Takes You To |
|--------|-------------|
| + New Enquiry | Create new enquiry form |
| Collect Payment | Payment recording shortcut |
| View All Enquiries | Full enquiry list |

---

## 5. Enquiry Management

An **enquiry** is created the moment a student or parent expresses interest in joining SKS College of Nursing.

### 5.1 Creating an Enquiry

**Navigate**: Sidenav → Admission Management → Enquiries → "**+ New Enquiry**"

#### Enquiry Creation Form — Nursing-Specific

Fill in the following fields:

**Student Basic Information**

| Field | Example | Notes |
|-------|---------|-------|
| Full Name | `Divya Krishnan` | As per school certificates |
| Email | `divya.k@gmail.com` | Active email for letters/receipts |
| Phone | `+91-7XXXXXXXXX` | WhatsApp-enabled preferred |

**Program Preferences**

| Field | Example | Options |
|-------|---------|---------|
| Program | `B.Sc Nursing` | BSC-NUR / MSC-NUR / GNM / PBB-NUR / DOTT / DMLT |
| Student Type | `General` | General / SC/ST / OBC / NRI / Lateral Entry |

**Referral Information** *(Optional but important)*

| Field | Example | Why It Matters |
|-------|---------|----------------|
| Agent | `Mrs. Lakshmi` | For commission tracking |
| Referral Type | `Hospital Referral` | Marketing analytics |
| Remarks | `Completing 12th in April 2026, wants B.Sc Nursing` | Any useful context |

#### Step-by-Step

1. Click **"+ New Enquiry"** button
2. Enter student name, email, phone
3. Select program (e.g., `Bachelor of Science in Nursing`)
4. Select student type
5. Select referral type (how did they find us?)
6. Add remarks if needed
7. Click **"Create Enquiry"**
8. Status automatically set to **ENQUIRED**

#### Common Programs at SKS — Quick Description for Students

| Program | Duration | Who Should Apply |
|---------|----------|-----------------|
| B.Sc Nursing | 4 years | Class 12, Science (Bio/Chem/Phy), min 45% |
| M.Sc Nursing | 2 years | B.Sc Nursing graduates, 1 yr experience |
| GNM | 3½ years | Class 12 pass (any stream), min 40% |
| Post Basic B.Sc | 2 years | Registered GNM nurses with 1 yr experience |
| DOTT / DMLT | 2 years | Class 12 pass, Science preferred |

---

### 5.2 Managing the Enquiry List

**Navigate**: Sidenav → Admission Management → Enquiries

#### Search and Filter

| Tool | How to Use | Purpose |
|------|-----------|---------|
| Search box | Type name, phone, or email | Find a specific student |
| Status filter | Select status from dropdown | See students at a specific stage |
| Date range | Set from–to dates | Enquiries in a date range |
| Program filter | (if available) | See by program |

#### List Columns Explained

| Column | Meaning |
|--------|---------|
| Name | Prospective student name |
| Phone | Contact number |
| Program | Interested program |
| Type | General / SC/ST / OBC / etc. |
| Enquiry Date | When they enquired |
| Referral | Source of enquiry |
| Status | Current position in workflow |
| Actions | Shortcut buttons (Collect Payment, Submit Docs) |

#### Status Action Buttons

| Button | When It Appears | What to Do |
|--------|----------------|-----------|
| 💳 Collect Payment | When status = `FEES_FINALIZED` or `PARTIALLY_PAID` | Click to collect fee |
| 📁 Submit Documents | When status = `FEES_PAID` or `PARTIALLY_PAID` | Click to upload docs |

> **Note**: You do not see "Finalize Fee" — that is done by the College Admin.

---

### 5.3 Enquiry Detail View

Click on any enquiry name to open the full **Enquiry Detail** screen.

#### Overview Tab

Shows complete enquiry information:
- Student name, email, phone
- Program and student type
- Referral type and agent
- Finalized fee amount (if fee is already set by College Admin)
- Current status badge

#### Documents Tab

Shows document upload status:
- Document type name
- Status: PENDING / UPLOADED / VERIFIED / REJECTED
- File name and size
- View and download buttons (if file uploaded)

#### Payments Tab

Shows full payment history for this student:
- Receipt number
- Amount paid
- Payment date
- Payment method (CASH / NEFT / UPI / CARD)
- Collected by
- Balance remaining

#### Status History Tab

Timeline of all changes:
- Each status transition shown chronologically
- Who changed it and when
- Remarks added with each change

#### Action Buttons on Detail Screen

| Button | When Visible | Action |
|--------|-------------|--------|
| Submit Documents | Status = `FEES_PAID` or `PARTIALLY_PAID` | Submits all docs and changes status to `DOCUMENTS_SUBMITTED` |
| Create Admission | Status = `DOCUMENTS_SUBMITTED` | Opens admission creation form |
| Edit | Always | Edit enquiry details |
| Back | Always | Return to list |

---

### 5.4 Status Workflow

The enquiry status tells you exactly **what step the student is at** and **what you need to do next**.

```
╔══════════════════════════════════════════════════════════════╗
║   FRONT OFFICE VIEW — ENQUIRY STATUS & YOUR ACTIONS          ║
╚══════════════════════════════════════════════════════════════╝

ENQUIRED  ───────────────────────────────────────────────────►
                Your Action: Contact student, discuss programs.
                             Change status to INTERESTED
                             when student confirms.

INTERESTED ──────────────────────────────────────────────────►
                Wait: College Admin sets the fee.
                Your Action: Let student know fee is
                             being calculated and to
                             come back in 1-2 days.

FEES_FINALIZED ──────────────────────────────────────────────►
                Your Action: Call student to collect payment.
                💳 "Collect Payment" button is now visible.

FEES_PAID ───────────────────────────────────────────────────►
  PARTIALLY_PAID                                               │
                Your Action: Collect 7 mandatory documents.    │
                📁 "Submit Documents" button is now visible.   │

DOCUMENTS_SUBMITTED ─────────────────────────────────────────►
                Enquiry detail shows "Create Admission".
                🎓 Click to complete admission.

ADMITTED ✅──────────────────────────────────────────────────►
                Done! Student ID issued.
                Welcome the student to SKS!
```

#### How to Change Status

1. Open the enquiry
2. Click the **status badge** next to the student's name
3. Choose the new status from the dropdown
4. Confirm

**Manual Transitions You Can Make**:
- `ENQUIRED` → `INTERESTED` (when student confirms)
- `ENQUIRED` → `NOT_INTERESTED` (if student declines)
- `NOT_INTERESTED` → `INTERESTED` (if student changes mind)

---

## 6. Payment Collection

As Front Office, you can collect payments from students who have had their fees finalized.

### How to Collect a Payment

**Method A (Recommended): From Enquiry List Shortcut**

1. Navigate to **Enquiries list**
2. Find student with status `FEES_FINALIZED` or `PARTIALLY_PAID`
3. Click **💳 "Collect Payment"** button (teal/green button on the right)
4. Payment collection screen opens with enquiry auto-filled
5. Enter payment details:

| Field | Example |
|-------|---------|
| Amount Paid | `50000` |
| Payment Date | `27-04-2026` |
| Payment Method | `CASH` / `CHEQUE` / `NEFT` / `UPI` / `CARD` |
| Transaction Reference | Cheque No / UTR / UPI Ref ID (if applicable) |
| Remarks | `Year 1 first instalment` |

6. Click **"Collect Payment"**
7. Receipt is displayed — print or email to student

**Method B: From Enquiry Detail View**

1. Open enquiry → click Payments tab
2. Then record payment from there

---

### Payment Methods Accepted at SKS

| Method | When | Reference |
|--------|------|-----------|
| CASH | At counter | No reference needed |
| CHEQUE | At counter | Record cheque number (e.g., `CK112345`) |
| NEFT / RTGS | Bank transfer | UTR number (e.g., `UTR20262704XXXXX`) |
| UPI | Phone transfer | UPI Transaction ID |
| Card | At campus terminal | Authorization code |
| DD | At counter | DD number and bank |

### Understanding the Receipt

After payment is recorded, the system shows a receipt like:

```
┌─────────────────────────────────────────┐
│     SKS COLLEGE OF NURSING              │
│         PAYMENT RECEIPT                 │
├─────────────────────────────────────────┤
│ Receipt No:    RCP-SKS-20260427-001     │
│ Date:          27-Apr-2026              │
├─────────────────────────────────────────┤
│ Student:       Divya Krishnan           │
│ Program:       B.Sc Nursing             │
│ Enquiry ID:    ENQ-2026-042             │
├─────────────────────────────────────────┤
│ Amount Paid:   ₹50,000                 │
│ Method:        NEFT                     │
│ Ref:           UTR20262704XXXXX        │
│ Collected by:  Ms. Radhika (Front Off)  │
├─────────────────────────────────────────┤
│ Balance Due:   ₹53,000                 │
└─────────────────────────────────────────┘
 Print | Email | Close
```

### Partial Payments

If a student cannot pay the full amount:
- Record the amount they are paying (e.g., ₹50,000 of ₹1,03,000)
- Status changes to `PARTIALLY_PAID`
- Student can still proceed to document submission
- System tracks balance automatically
- Remind student of remaining balance

---

## 7. Document Management

Collecting and verifying documents is one of your most important responsibilities. **All 7 documents must be uploaded** before a student can be admitted.

### Mandatory Documents — INC Requirements

SKS College of Nursing requires these **7 documents** for every admission:

| # | Document | Requirement |
|---|----------|-------------|
| 1 | **10th Grade Marksheet** | Minimum 45% aggregate |
| 2 | **12th Grade Marksheet** | Science stream — Biology + Chemistry + Physics compulsory |
| 3 | **Transfer Certificate** | From last school/college with "conduct: good" |
| 4 | **Aadhar Card** | Valid government-issued ID |
| 5 | **Passport Photo** | White background, recent, 4×6 cm |
| 6 | **Medical Fitness Certificate** | From registered medical practitioner, dated within 6 months |
| 7 | **Blood Group Report** | From accredited hospital or diagnostic lab |

### Document Collection Workflow

```
╔══════════════════════════════════════════════════════════════════╗
║      DOCUMENT COLLECTION WORKFLOW — FRONT OFFICE                ║
╚══════════════════════════════════════════════════════════════════╝

Student arrives with documents
          │
          ▼
Navigate to:
Admission Management → Document Submission
          │
          ▼
Find student's enquiry
  (Status must be FEES_PAID or PARTIALLY_PAID)
          │
          ▼
Open document submission screen
          │
          ▼
FOR EACH of the 7 documents:
    ├─ Select document type from dropdown
    ├─ Click "Upload File" → select from computer
    ├─ Review uploaded preview
    ├─ Is it valid?
    │   ├─ YES → Click "Verify" ✅
    │   └─ NO  → Click "Reject" ❌
    │              Add reason (e.g., "Photo has coloured background")
    │              Return to student for resubmission
    └─ Proceed to next document
          │
          ▼
All 7 documents uploaded and verified?
          │
    ┌─────┴─────┐
   YES         NO
    │           └─ Request missing/rejected docs from student
    ▼               and revisit when corrected
Click "Submit Documents"
          │
          ▼
✅ Status changes to DOCUMENTS_SUBMITTED
   Student is now ready for admission!
```

### Verifying Each Document

#### 10th Marksheet
- All subjects and grades visible
- School name, year, and board clearly stated
- Not a photocopy of a photocopy

#### 12th Marksheet
- Must show **Biology**, **Chemistry**, and **Physics** as subjects
- Mark above minimum (45% general / 40% SC-ST)
- Science stream confirmed

#### Transfer Certificate
- On institution letterhead
- Mentions "conduct: satisfactory" or "good"
- Signed and stamped

#### Aadhar Card
- Both sides scanned
- Card is valid and not expired
- If no Aadhar, accept Voter ID or Passport with note

#### Passport Photo
- White/light background
- Recent (within 6 months)
- Clear face visible, no glasses or headwear

#### Medical Fitness Certificate
- On doctor's official letterhead
- Doctor must be registered (IMA or State Medical Council)
- Must state student is "fit for nursing education"
- **Dated within 6 months** — reject older certificates

#### Blood Group Report
- From accredited lab or hospital
- Shows blood group clearly (e.g., B+, O-, A+)
- Name matches student's name on other documents

### Submitting Documents

Once all 7 are uploaded and verified:

1. Scroll to bottom of document submission screen
2. Click **"Submit Documents"** button
3. System checks:
   - All 7 mandatory documents present?
   - At least UPLOADED (not necessarily VERIFIED)?
4. If all present → Status changes to `DOCUMENTS_SUBMITTED`
5. Success message confirms submission

---

## 8. Admission Creation

When a student's enquiry is in `DOCUMENTS_SUBMITTED` status, you can create their official admission record.

### Create Admission from Admission Completion Screen

**Navigate**: Sidenav → Admission Management → Admission Completion

1. Screen shows all enquiries with `DOCUMENTS_SUBMITTED` status
2. Search for your student
3. Click on the enquiry
4. Click **"Create Admission"** button

### Admission Form — Nursing-Specific Fields

Fill in all required fields (marked with *):

**Auto-filled from Enquiry**:
- Student Name, Email, Phone, Program

**You Need to Enter**:

| Section | Field | Example |
|---------|-------|---------|
| **Admission Details** | Semester | `Semester 1` |
| | Admission Date | `01-08-2026` |
| | Academic Year From | `2026` |
| | Academic Year To | `2027` |
| | Application Date | `15-07-2026` |
| **Personal Info** | Date of Birth | `15-03-2007` |
| | Gender | `Female` (most nursing students) |
| | Aadhar Number | From Aadhar card |
| | Blood Group | `B+` (from blood group report) |
| | Community Category | `OC` / `BC` / `SC` / `ST` / `MBC` / `DNC` |
| | Nationality | `Indian` |
| **Family Info** | Father's Name | `Mr. K Krishnan` |
| | Mother's Name | `Mrs. S Krishnan` |
| | Parent Mobile | `+91-98XXXXXXXX` |

### After Submitting

System shows:

```
✅ ADMISSION CREATED SUCCESSFULLY

Admission ID:  ADM-2026-042
Student ID:    SKS-NUR-2026-042
Student Name:  Divya Krishnan
Program:       Bachelor of Science in Nursing
Semester:      1 (August 2026 batch)

[ Print Admission Confirmation ] [ Email to Student ] [ Continue ]
```

---

## 9. Reports

**Navigate**: Sidenav → Reports

### Reports Available to Front Office

| Report | What It Shows | Useful For |
|--------|--------------|-----------|
| Admissions Report | Total admissions by period | Weekly/monthly review |
| Enquiry Status Report | Where enquiries are in pipeline | Daily follow-up planning |
| Fee Collections Report | Collections recorded by you | Your day's performance |

### How to Generate a Report

1. Navigate to **Reports**
2. Click the report name
3. Set date range (e.g., April 1 – April 27, 2026)
4. Click **"Generate"**
5. View or export:
   - **Excel**: For spreadsheet analysis
   - **PDF**: For printing and sharing
   - **Email**: Send report by email

---

## 10. Daily Workflows

### Workflow A: New Student Walk-In

```
Student arrives at front office
         │
         ▼
Greet student professionally:
"Welcome to SKS College of Nursing!"
         │
         ▼
Collect basic information:
  • Name, phone, email
  • Which program they're interested in
  • Class 12 stream (must be Science for B.Sc Nursing)
         │
         ▼
Create enquiry in system
  Status: ENQUIRED
         │
         ▼
Explain programs, fees (approximate),
duration, career scope
         │
         ▼
Student confirms interest?
  ├── YES → Update status to INTERESTED
  │          Inform College Admin to finalize fees
  │
  └── NO  → Leave as ENQUIRED
             Schedule follow-up call in 2 days
```

---

### Workflow B: Collecting Documents from Student

```
Student arrives with documents
         │
         ▼
Find enquiry (must be FEES_PAID or PARTIALLY_PAID)
         │
         ▼
Click "Submit Documents" shortcut or
navigate to Admission Management → Document Submission
         │
         ▼
Go through 7 documents one by one:
  1. 10th Marksheet        ── Upload → Verify ✅
  2. 12th Marksheet        ── Upload → Verify ✅
  3. Transfer Certificate  ── Upload → Verify ✅
  4. Aadhar Card           ── Upload → Verify ✅
  5. Passport Photo        ── Upload → Verify ✅
  6. Medical Fitness Cert  ── Upload → Verify ✅ (Check date!)
  7. Blood Group Report    ── Upload → Verify ✅
         │
         ▼
All 7 verified? → Click "Submit Documents"
Status: DOCUMENTS_SUBMITTED
         │
         ▼
Inform College Admin / schedule admission
creation appointment
```

---

### Workflow C: Full Day — Front Office Routine

```
9:00 AM — Morning Shift Start
  ├─ Check dashboard: any students awaiting payment or docs?
  ├─ Call students with status FEES_FINALIZED (collect payment)
  └─ Arrange appointment book for walk-in admissions

10:00 AM – 1:00 PM — Receive Students
  ├─ Process new enquiries (walk-ins, referrals)
  ├─ Collect documents from students
  └─ Process payments

1:00 PM – 2:00 PM — Lunch Break

2:00 PM – 5:00 PM — Process & Follow Up
  ├─ Create admissions for ready students
  ├─ Follow up by phone on pending docs
  ├─ Update statuses based on interactions
  └─ Generate daily collection report

5:00 PM — End of Day
  ├─ Review pending actions on dashboard
  ├─ Note follow-up tasks for tomorrow
  └─ Log out of system
```

---

## 11. Troubleshooting

### Cannot see 💳 Collect Payment button

**Cause**: Student's fee has not been finalized yet by College Admin.

**Solution**:
- Check enquiry status — must be `FEES_FINALIZED` or `PARTIALLY_PAID`
- If status is `INTERESTED`, the College Admin has not yet finalized the fee
- Contact College Admin to finalize fee first
- Once finalized, button will appear automatically

---

### Cannot upload a document

**Cause**: File format or size issue.

**Solution**:
- Accepted formats: **PDF, JPG, PNG** only
- Max file size: **5 MB** per document
- If file is larger: compress the image (use Paint, mobile photo settings, or online compressor)
- Try a different browser (Chrome recommended)

---

### 12th Marksheet rejected — wrong stream

**Situation**: Student's 12th marksheet doesn't have Biology.

**Solution**:
- Explain INC requirement: Biology is compulsory for B.Sc Nursing
- GNM accepts students without Biology — suggest this program
- If they want B.Sc Nursing, they may need to re-appear for Biology exam

---

### Medical Fitness Certificate is outdated

**Situation**: Certificate is more than 6 months old.

**Solution**:
- Reject the document with note: "Certificate dated more than 6 months ago. Please resubmit."
- Provide student with list of nearby hospitals/clinics for quick check
- Remind them the certificate must explicitly say "fit for nursing study"

---

### Cannot find student in enquiry list

**Possible Causes**:
1. Different phone number used when enquiry was created
2. Different name spelling
3. Date filter is excluding their enquiry date

**Solution**:
- Try searching by phone number
- Clear all filters and search again
- Check if enquiry is under a different name (family name used first)

---

### Student says they paid but system shows balance outstanding

**Solution**:
- Ask for payment receipt number
- Search by receipt number in the enquiry's Payments tab
- If receipt not found, check if payment was recorded under a different enquiry
- If not found anywhere, the payment may not have been entered
- Ask for bank SMS or UPI notification as proof
- Record payment with transaction reference from their proof

---

## 12. Quick Reference

### Nursing Program Eligibility — Quick Check

| Program | Minimum Qualification | Min % | Stream Required |
|---------|----------------------|-------|----------------|
| B.Sc Nursing | 12th Pass | 45% (Gen) / 40% (SC/ST) | Science (Bio+Chem+Physics) |
| GNM | 12th Pass | 40% | Any stream |
| M.Sc Nursing | B.Sc Nursing + 1 yr experience | 55% in B.Sc | N/A |
| Post Basic B.Sc | GNM + 1 yr experience | — | N/A |
| DOTT / DMLT | 12th Pass | 40% | Science preferred |

### Status Cheat Sheet for Front Office

| Status | What You Need to Do |
|--------|-------------------|
| ENQUIRED | Contact student, explain program, update to INTERESTED |
| INTERESTED | Wait for College Admin to finalize fee |
| FEES_FINALIZED | Call student to collect payment ← **💳 Action** |
| FEES_PAID | Collect 7 documents ← **📁 Action** |
| PARTIALLY_PAID | Collect documents AND chase remaining payment |
| DOCUMENTS_SUBMITTED | Create admission ← **🎓 Action** |
| ADMITTED | ✅ Done! Welcome new student to SKS! |

### 7 Mandatory Documents Checklist

```
[ ] 1. 10th Marksheet
[ ] 2. 12th Marksheet (Science with Biology)
[ ] 3. Transfer Certificate
[ ] 4. Aadhar Card
[ ] 5. Passport Photo (white background, recent)
[ ] 6. Medical Fitness Certificate (within 6 months)
[ ] 7. Blood Group Report (from accredited lab)
```

### How to Greet Different Student Types

| Situation | Approach |
|-----------|---------|
| First-year B.Sc Nursing | Discuss career in nursing, duration, clinical training hospitals |
| GNM Applicant | Simpler career path, explain bridge to B.Sc later |
| Post Basic applicant | Talk about degree upgrade for career advancement |
| Parent accompanying student | Address parent's concerns about career, safety, hostel |
| NRI / International student | Enquire about documentation requirements for NRI quota |

---

**SKS College of Nursing | Front Office User Guide**
**Version 2.0 | April 27, 2026 | Next Review: July 2026**

