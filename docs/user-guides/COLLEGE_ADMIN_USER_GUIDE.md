# SKS College of Nursing
# College Administrator — User Guide

**Version 2.0 | April 27, 2026**

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Your Role at SKS College of Nursing](#2-your-role-at-sks-college-of-nursing)
3. [Getting Started & Login](#3-getting-started--login)
4. [Admin Dashboard](#4-admin-dashboard)
5. [Preferences — Academic Setup](#5-preferences--academic-setup)
   - [5.1 Departments](#51-departments)
   - [5.2 Programs](#52-programs)
   - [5.3 Courses (Subjects)](#53-courses-subjects)
   - [5.4 Academic Years](#54-academic-years)
   - [5.5 Semesters](#55-semesters)
   - [5.6 Fee Structures](#56-fee-structures)
   - [5.7 Faculty](#57-faculty)
   - [5.8 Agents & Referral Types](#58-agents--referral-types)
6. [Admission Management](#6-admission-management)
   - [6.1 Status Lifecycle](#61-admission-status-lifecycle)
   - [6.2 Enquiries](#62-enquiries)
   - [6.3 Document Submission](#63-document-submission)
   - [6.4 Admission Completion](#64-admission-completion)
   - [6.5 Admissions List](#65-admissions-list)
   - [6.6 Students List](#66-students-list)
7. [Finance Management](#7-finance-management)
   - [7.1 Fee Finalization](#71-fee-finalization)
   - [7.2 Payment Collection](#72-payment-collection)
8. [Reports](#8-reports)
9. [Complete Admission Workflow](#9-complete-admission-workflow)
10. [Troubleshooting](#10-troubleshooting)
11. [Quick Reference](#11-quick-reference)

---

## 1. Introduction

Welcome to the **SKS College of Nursing Management System**. This guide is written for **College Administrators** who are responsible for configuring academic operations, managing admissions from enquiry to enrollment, and overseeing financial collections.

SKS College of Nursing offers INC-approved programs in nursing and allied health sciences. The system is designed to help you manage the entire student journey from the first phone call to graduation.

---

## 2. Your Role at SKS College of Nursing

### Permissions at a Glance

| Menu Section | Access |
|------|--------|
| Dashboard | ✅ Full access |
| Preferences — Academic Setup | ✅ Full access |
| Admission Management | ✅ Full access |
| Finance — Fee Finalization | ✅ College Admin only |
| Finance — Fee Payments | ✅ Full access |
| Reports | ✅ Full access |
| System Settings | ❌ System Admin only |

### Enquiry Shortcut Actions Available to You

From the **Enquiries list**, you can trigger key workflow steps with a single button:

| Button | Enquiry Status Required | Action |
|--------|------------------------|--------|
| 💰 Finalize Fee | `INTERESTED` | Set student's fee amount, apply scholarships |
| 💳 Collect Payment | `FEES_FINALIZED` or `PARTIALLY_PAID` | Record a cash/online/cheque payment |
| 📁 Submit Documents | `FEES_PAID` or `PARTIALLY_PAID` | Open document upload workflow |
| 🎓 Create Admission | `DOCUMENTS_SUBMITTED` (via enquiry detail) | Create admission and student record |

---

## 3. Getting Started & Login

### How to Log In

1. Open your browser and navigate to the CMS application URL
2. You are automatically redirected to the **Keycloak login screen**
3. Enter:
   - **Username**: Your college email (e.g., `admin@sksnursing.edu.in`)
   - **Password**: Your Keycloak password
4. Click **"Log In"**
5. The **College Admin Dashboard** loads

### First-Time Checklist

- [ ] Verify your name and role display correctly (avatar → top-right)
- [ ] Check that "College Admin" appears in your profile
- [ ] Explore the left navigation sidebar
- [ ] Complete Step 5 (Preferences) before processing first admission

---

## 4. Admin Dashboard

### Dashboard Widgets

| Widget | What It Shows |
|--------|--------------|
| New Enquiries This Month | Count of prospective students who enquired |
| Pending Fee Finalizations | Enquiries in `INTERESTED` status awaiting fee setup |
| Ready for Admission | Enquiries in `DOCUMENTS_SUBMITTED` status |
| Monthly Collections | Total fees received this calendar month |
| Outstanding Balance | Total fees yet to be collected across all students |
| Active Academic Year | Currently configured academic year (e.g., 2026–27) |

### Quick Action Shortcuts

| Button | Takes You To |
|--------|-------------|
| + New Enquiry | Create enquiry screen |
| Finalize Fee | Student fees finalization |
| View Reports | Reports dashboard |
| Record Payment | Fee payment recording |

---

## 5. Preferences — Academic Setup

This section configures the academic and operational backbone of SKS College of Nursing. Set up Preferences **before** processing any admissions.

---

### 5.1 Departments

**Navigate**: Sidenav → Preferences → Departments

SKS College of Nursing organizes its faculty and curriculum into the following departments:

| Department Name | Code | Description |
|----------------|------|-------------|
| Medical-Surgical Nursing | MSN | Adult inpatient and surgical care nursing |
| Community Health Nursing | CHN | Public health, epidemiology, and rural health nursing |
| Child Health Nursing | CHD | Paediatric and neonatal nursing |
| Psychiatric & Mental Health Nursing | PMH | Psychiatry care and mental wellness nursing |
| OBG Nursing | OBG | Obstetrics, Gynaecology, and Midwifery |
| Nursing Foundations | NF | Fundamentals of nursing and clinical skills |
| Basic Sciences | BSC | Anatomy, Physiology, Biochemistry, Microbiology |

#### How to Create a Department

1. Click **"+ New Department"** button (top-right of the screen)
2. Fill in:
   - **Department Name**: e.g., `Medical-Surgical Nursing`
   - **Department Code**: e.g., `MSN`
   - **Description**: Brief description of scope
3. Click **"Save Department"**
4. New department appears in the list

---

### 5.2 Programs

**Navigate**: Sidenav → Preferences → Programs

SKS College of Nursing offers the following INC-approved programs:

| Program Name | Code | Duration | Intake Seats | Department |
|-------------|------|----------|--------------|-----------|
| Bachelor of Science in Nursing | BSC-NUR | 4 Years | 60 | Nursing Foundations |
| Master of Science in Nursing | MSC-NUR | 2 Years | 30 | Medical-Surgical Nursing |
| General Nursing & Midwifery | GNM | 3½ Years | 60 | Nursing Foundations |
| Post Basic B.Sc Nursing | PBB-NUR | 2 Years | 30 | Nursing Foundations |
| Diploma in OT Technician | DOTT | 2 Years | 30 | Basic Sciences |
| Diploma in Medical Lab Technology | DMLT | 2 Years | 30 | Basic Sciences |

#### How to Create a Program

1. Click **"+ New Program"**
2. Fill in:
   - **Program Name**: `Bachelor of Science in Nursing`
   - **Program Code**: `BSC-NUR`
   - **Department**: `Nursing Foundations`
   - **Duration (Years)**: `4`
   - **Intake Capacity**: `60`
   - **Description**: `INC-approved 4-year nursing degree program`
3. Click **"Save Program"**

---

### 5.3 Courses (Subjects)

**Navigate**: Sidenav → Preferences → Courses

Courses are individual subjects taught within programs. Below is the complete curriculum for **B.Sc Nursing (BSC-NUR)**:

#### Semester-wise Course List — B.Sc Nursing

| Semester | Subject | Code | Credits |
|----------|---------|------|---------|
| Sem 1 | Anatomy and Physiology | BSC-NUR-101 | 4 |
| Sem 1 | Biochemistry | BSC-NUR-102 | 3 |
| Sem 1 | Nursing Foundations I | BSC-NUR-103 | 6 |
| Sem 1 | Nutrition and Dietetics | BSC-NUR-104 | 3 |
| Sem 2 | Psychology | BSC-NUR-105 | 3 |
| Sem 2 | Microbiology | BSC-NUR-106 | 4 |
| Sem 2 | Nursing Foundations II | BSC-NUR-107 | 6 |
| Sem 2 | Sociology | BSC-NUR-108 | 2 |
| Sem 3 | Medical-Surgical Nursing I | BSC-NUR-201 | 8 |
| Sem 3 | Pharmacology | BSC-NUR-202 | 4 |
| Sem 3 | Pathology and Genetics | BSC-NUR-203 | 3 |
| Sem 4 | Medical-Surgical Nursing II | BSC-NUR-204 | 8 |
| Sem 4 | Community Health Nursing I | BSC-NUR-205 | 6 |
| Sem 4 | Mental Health Nursing | BSC-NUR-206 | 5 |
| Sem 5 | Child Health Nursing | BSC-NUR-301 | 6 |
| Sem 5 | Midwifery & OBG Nursing I | BSC-NUR-302 | 8 |
| Sem 6 | Midwifery & OBG Nursing II | BSC-NUR-303 | 8 |
| Sem 6 | Community Health Nursing II | BSC-NUR-304 | 5 |
| Sem 7 | Nursing Research & Statistics | BSC-NUR-401 | 4 |
| Sem 7 | Management of Nursing Services | BSC-NUR-402 | 5 |
| Sem 8 | Nursing Education | BSC-NUR-403 | 4 |
| Sem 8 | Clinical Internship | BSC-NUR-404 | 20 |

#### How to Create a Course

1. Click **"+ New Course"**
2. Fill in:
   - **Course Name**: `Anatomy and Physiology`
   - **Course Code**: `BSC-NUR-101`
   - **Program**: `Bachelor of Science in Nursing`
   - **Semester**: `1`
   - **Credits**: `4`
3. Click **"Save Course"**

---

### 5.4 Academic Years

**Navigate**: Sidenav → Preferences → Academic Years

SKS College follows the **INC academic calendar** (August to July).

| Academic Year | Status |
|---------------|--------|
| 2023–24 | Archived |
| 2024–25 | Archived |
| 2025–26 | Archived |
| 2026–27 | ✅ **Active** |
| 2027–28 | Future |

#### How to Create an Academic Year

1. Click **"+ New Academic Year"**
2. Enter:
   - **Start Year**: `2026`
   - **End Year**: `2027`
   - **Status**: `Active`
3. Click **"Save"**

> **Note**: Only one academic year should be set to `Active` at a time.

---

### 5.5 Semesters

**Navigate**: Sidenav → Preferences → Semesters

Each academic year has two semesters for degree programs and three blocks for GNM.

| Semester Name | Number | Period | Exam Dates |
|---------------|--------|--------|------------|
| Sem 1 — Foundation (Odd) 2026 | 1 | 01 Aug – 12 Dec 2026 | 15 Dec – 28 Dec 2026 |
| Sem 2 — Foundation (Even) 2027 | 2 | 03 Jan – 10 May 2027 | 12 May – 25 May 2027 |
| Sem 3 — Clinical I (Odd) 2027 | 3 | 01 Aug – 12 Dec 2027 | 15 Dec – 28 Dec 2027 |
| Sem 4 — Clinical I (Even) 2028 | 4 | 03 Jan – 10 May 2028 | 12 May – 25 May 2028 |

#### How to Create a Semester

1. Click **"+ New Semester"**
2. Enter:
   - **Name**: `Sem 1 — Foundation (Odd) 2026`
   - **Semester Number**: `1`
   - **Academic Year**: `2026–27`
   - **Start Date**: `01-08-2026`
   - **End Date**: `12-12-2026`
   - **Exam Start Date**: `15-12-2026`
   - **Exam End Date**: `28-12-2026`
3. Click **"Save"**

---

### 5.6 Fee Structures

**Navigate**: Sidenav → Preferences → Fee Structures

SKS College of Nursing fee structures comply with INC regulations and state government guidelines.

#### B.Sc Nursing Fee Structure — 2026–27

| Fee Component | Year 1 | Year 2 | Year 3 | Year 4 | Total |
|--------------|--------|--------|--------|--------|-------|
| Tuition Fee | ₹60,000 | ₹60,000 | ₹60,000 | ₹60,000 | ₹2,40,000 |
| Clinical Training Fee | ₹20,000 | ₹25,000 | ₹30,000 | ₹25,000 | ₹1,00,000 |
| Lab & Practical Fee | ₹10,000 | ₹10,000 | ₹10,000 | ₹10,000 | ₹40,000 |
| Exam Fee | ₹5,000 | ₹5,000 | ₹5,000 | ₹5,000 | ₹20,000 |
| Uniform & Kit (Year 1) | ₹8,000 | — | — | — | ₹8,000 |
| **Annual Total** | **₹1,03,000** | **₹1,00,000** | **₹1,05,000** | **₹1,00,000** | **₹4,08,000** |

> Hostel fee (₹36,000/year) is additional and optional.

#### M.Sc Nursing Fee Structure — 2026–27

| Fee Component | Year 1 | Year 2 |
|--------------|--------|--------|
| Tuition Fee | ₹80,000 | ₹80,000 |
| Research Lab Fee | ₹15,000 | ₹20,000 |
| Clinical Placement Fee | ₹25,000 | ₹30,000 |
| Exam Fee | ₹5,000 | ₹5,000 |
| **Annual Total** | **₹1,25,000** | **₹1,35,000** |

#### GNM Fee Structure — 2026–27

| Fee Component | Year 1 | Year 2 | Year 3 |
|--------------|--------|--------|--------|
| Tuition Fee | ₹45,000 | ₹45,000 | ₹45,000 |
| Clinical Training | ₹15,000 | ₹18,000 | ₹20,000 |
| Uniform & Kit | ₹6,000 | — | — |
| Exam Fee | ₹4,000 | ₹4,000 | ₹4,000 |
| **Annual Total** | **₹70,000** | **₹67,000** | **₹69,000** |

#### Scholarship / Concession Categories

| Scholarship | Eligibility | Discount |
|-------------|-------------|---------|
| Merit Scholarship | 95%+ in 12th Grade | ₹30,000/year |
| SC/ST Government Concession | Caste certificate | As per Govt norms |
| Staff Ward Concession | Child of SKS staff | 25% tuition fee |
| Hospital Referral Concession | Referred by affiliated hospital | 15% tuition fee |
| Financial Hardship Concession | Income certificate < ₹3 lakh | Case by case |

#### How to Create a Fee Structure

1. Click **"+ New Fee Structure"**
2. Select:
   - **Program**: `Bachelor of Science in Nursing`
   - **Academic Year**: `2026–27`
   - **Fee Type**: `Tuition Fee`
3. Add Year Amounts:
   - Year 1: `60000`, Year 2: `60000`, Year 3: `60000`, Year 4: `60000`
4. Click **"Save Fee Structure"**

> **Tip**: Create separate fee structure entries for each component (Tuition, Clinical, Lab, Exam) and the system aggregates them.

---

### 5.7 Faculty

**Navigate**: Sidenav → Preferences → Faculty

#### SKS College of Nursing — Faculty Register (Sample)

| Name | Designation | Department | Qualification |
|------|-------------|-----------|---------------|
| Ms. Anita Rao | Principal & Professor | Nursing Foundations | M.Sc Nursing, Ph.D |
| Dr. Kavitha M | Reader | Medical-Surgical Nursing | M.Sc Nursing (Critical Care) |
| Ms. Priya Sundaram | Lecturer | Community Health Nursing | M.Sc Nursing |
| Ms. Deepa Nair | Lecturer | OBG Nursing | M.Sc Nursing (Midwifery) |
| Ms. Lakshmi K | Tutor | Child Health Nursing | B.Sc Nursing |
| Mr. Arun Kumar | Tutor | Basic Sciences | M.Sc Anatomy |
| Ms. Sindhu G | Clinical Instructor | Medical-Surgical Nursing | B.Sc Nursing |

#### How to Add Faculty

1. Click **"+ New Faculty"**
2. Enter:
   - **First Name / Last Name**: e.g., `Kavitha M`
   - **Email**: `kavitha.m@sksnursing.edu.in`
   - **Phone**: `+91-98XXXXXXXX`
   - **Employee ID**: `FAC-001`
   - **Department**: `Medical-Surgical Nursing`
   - **Designation**: `Reader`
   - **Specialization**: `Critical Care Nursing`
3. Click **"Save Faculty"**

---

### 5.8 Agents & Referral Types

**Navigate**: Sidenav → Preferences → Agents and Referral Types

#### SKS College — Referral Type Setup

| Referral Type | Description |
|---------------|-------------|
| Direct Walk-In | Walk-in student — no referral |
| Hospital Referral | Staff from affiliated hospital recommends |
| Alumni Network | SKS alumni refers prospective student |
| Advertisement | Newspaper, TV, or Radio ad |
| Social Media | Facebook, Instagram, WhatsApp group |
| INC / State Nursing Board | Via INC or TNMSC notice board |
| School / PU College | Referred by a 12th-grade institution |
| Education Agent | Commission-based recruitment agent |

---

## 6. Admission Management

### 6.1 Admission Status Lifecycle

Every prospective student passes through the following statuses, each unlocking new actions:

```
╔══════════════════════════════════════════════════════════════════════╗
║         ENQUIRY STATUS LIFECYCLE — SKS COLLEGE OF NURSING           ║
╚══════════════════════════════════════════════════════════════════════╝

  [ENQUIRED]
    │  Student expressed interest (walk-in, call, social media)
    │  Action: Front Office records enquiry details
    │
    ▼
  [INTERESTED]
    │  Student confirmed they want to join
    │  Action: Notify College Admin to finalize fees
    │
    ▼
  [FEES_FINALIZED]
    │  College Admin has set the fee amount, applied scholarships
    │  Action: Cashier / Front Office / Admin collects payment
    │
    ▼
  [FEES_PAID] ──── or ──── [PARTIALLY_PAID]
    │                              │
    │  Full payment received       │  First instalment received
    │                              │  (Remaining > 0)
    └──────────┬───────────────────┘
               │
               │  Action: Collect 7 mandatory documents
               ▼
  [DOCUMENTS_SUBMITTED]
    │  All 7 documents uploaded and verified
    │  Action: College Admin / Front Office creates admission
    │
    ▼
  [ADMITTED] ✅
       Student ID generated — Admission is complete!
```

**What happens if a student changes their mind?**
- From `ENQUIRED` or `INTERESTED` → Change status to `NOT_INTERESTED`
- From `NOT_INTERESTED` → Can return to `INTERESTED` later
- From `FEES_FINALIZED` onwards → Status change requires College Admin action

---

### 6.2 Enquiries

**Navigate**: Sidenav → Admission Management → Enquiries

#### Creating a New Enquiry

1. Click **"+ New Enquiry"** (top-right)
2. Fill in the form:

   **Student Information**:
   | Field | Example |
   |-------|---------|
   | Name | `Meena Rajendran` |
   | Email | `meena@gmail.com` |
   | Phone | `+91-99XXXXXXXX` |
   | Program | `Bachelor of Science in Nursing` |
   | Student Type | `General` / `SC/ST` / `OBC` / `NRI` / `Lateral Entry` |
   | Referral Type | `Hospital Referral` |
   | Agent | (if referred through an agent) |
   | Remarks | `Interested in critical care specialization` |

3. Click **"Create Enquiry"**
4. Status automatically set to `ENQUIRED`

#### Searching and Filtering Enquiries

| Filter | Options | Purpose |
|--------|---------|---------|
| Search Box | Name / Phone / Email | Find specific student |
| Status Filter | All statuses or specific | See students at a stage |
| Program Filter | BSC-NUR / GNM / etc. | See by program |
| Date Range | From – To | Enquiries in a period |

#### Quick Action Buttons in List View

From the enquiry list, these buttons appear automatically when the enquiry is in the correct status:

- **💰 Finalize Fee** → Click to set student's fee (status: `INTERESTED`)
- **💳 Collect Payment** → Click to record a payment (status: `FEES_FINALIZED` / `PARTIALLY_PAID`)
- **📁 Submit Documents** → Click to collect docs (status: `FEES_PAID` / `PARTIALLY_PAID`)

---

### 6.3 Document Submission

**Navigate**: Sidenav → Admission Management → Document Submission

SKS College of Nursing is affiliated with the **Indian Nursing Council (INC)** and **State Nurses Registration Council**, which mandates specific documents for admission:

#### Mandatory Document Checklist (7 Documents)

| # | Document Type | Minimum Requirement | Notes |
|---|---------------|---------------------|-------|
| 1 | 10th Grade Marksheet | Min 45% aggregate | SSC / CBSE / State Board |
| 2 | 12th Grade Marksheet | Science stream — Biology compulsory | Min 45% for General, 40% SC/ST |
| 3 | Transfer Certificate | Original from last institution | Clear conduct stated |
| 4 | Aadhar Card | Valid government ID | Or Voter ID / Passport |
| 5 | Passport Photo | White background, recent | 4×6 cm, 2 copies scanned |
| 6 | Medical Fitness Certificate | From registered medical practitioner | Dated within 6 months |
| 7 | Blood Group Report | From accredited lab/hospital | Required for clinical posting |

> **Optional**: NEET scorecard (required if state mandates it), Caste certificate (for reservations), Income certificate (for concessions).

#### Document Collection Steps

1. Navigate to **Admission Management → Document Submission**
2. Find the eligible enquiry (`FEES_PAID` or `PARTIALLY_PAID` status)
3. Click on the enquiry
4. For each of the 7 documents:
   - Select document type from dropdown
   - Click **"Upload File"** → Browse and select file (PDF or JPG, max 5 MB)
   - Click **"Verify"** if document meets requirements
   - Click **"Reject"** if document is unacceptable → Add rejection reason
5. When all 7 documents are `UPLOADED` or `VERIFIED`:
   - Click **"Submit Documents"** button
   - System validates completeness
   - Status changes to `DOCUMENTS_SUBMITTED`

#### Document Verification Standards

| Status | When to Use |
|--------|------------|
| VERIFIED ✅ | Document is clear, original-looking, and meets requirements |
| PENDING ⏳ | Uploaded but not yet reviewed |
| REJECTED ❌ | Document is blurry, expired, incorrect, or tampered |

**Common Rejection Reasons for Nursing**:
- Medical fitness certificate older than 6 months
- 12th marksheet without Biology/Chemistry/Physics
- Passport photo with coloured background
- Blood group certificate not from accredited lab
- Transfer certificate with "conduct: poor" remark

---

### 6.4 Admission Completion

**Navigate**: Sidenav → Admission Management → Admission Completion

This screen shows **only** enquiries in `DOCUMENTS_SUBMITTED` status — i.e., students ready for admission creation.

#### How to Create an Admission

1. Find the student's enquiry in the list
2. Click to open enquiry detail
3. Click **"Create Admission"** button
4. Fill in the **Admission Form**:

   **Auto-filled from enquiry**:
   - Student Name, Email, Phone, Program

   **Required Fields**:
   | Field | Example |
   |-------|---------|
   | Semester | `Semester 1` |
   | Admission Date | `01-08-2026` |
   | Academic Year From | `2026` |
   | Academic Year To | `2027` |
   | Application Date | `05-07-2026` |

   **Student Personal Details**:
   | Field | Example |
   |-------|---------|
   | Date of Birth | `15-03-2007` |
   | Gender | `Female` |
   | Aadhar Number | `XXXX XXXX XXXX` |
   | Blood Group | `B+` |
   | Community Category | `OC` / `BC` / `SC` / `ST` / `MBC` / `DNC` / `EWS` |
   | Nationality | `Indian` |
   | Religion | `Hindu` / `Christian` / `Muslim` / etc. |

   **Family Information**:
   | Field | Example |
   |-------|---------|
   | Father's Name | `Mr. K Rajendran` |
   | Mother's Name | `Mrs. S Rajendran` |
   | Parent Mobile | `+91-98XXXXXXXX` |

5. Click **"Create Admission"**

#### What Happens After Admission Creation

```
System creates:
  ✅ Admission Record   → Admission ID: ADM-2026-XXX
  ✅ Student Record     → Student ID:   SKS-NUR-2026-XXX
  ✅ Enquiry            → Status transitions to: ADMITTED
  ✅ Fee Allocation     → Linked to student for payment tracking
```

---

### 6.5 Admissions List

**Navigate**: Sidenav → Admission Management → Admissions

View and manage all created admissions:

| Column | Description |
|--------|-------------|
| Admission ID | e.g., `ADM-2026-001` |
| Student Name | Full name |
| Program | B.Sc Nursing, GNM, etc. |
| Admission Date | Date of joining |
| Status | PENDING / APPROVED / ENROLLED / GRADUATED |

**Actions**:
- View full admission details including qualifications
- Update admission status
- View linked documents and fee records

---

### 6.6 Students List

**Navigate**: Sidenav → Admission Management → Students

All admitted and enrolled students appear here with their Student IDs (e.g., `SKS-NUR-2026-001`).

---

## 7. Finance Management

### 7.1 Fee Finalization

**Navigate**: Finance → Student Fees → Finalize  
*Or click 💰 Finalize Fee button from enquiry list*

#### Fee Finalization Flow

```
╔═══════════════════════════════════════════════════════╗
║            FEE FINALIZATION WORKFLOW                  ║
╚═══════════════════════════════════════════════════════╝

  College Admin clicks "Finalize Fee"
  for enquiry: Meena Rajendran (BSC-NUR)
         │
         ▼
  System loads INC fee structure:
  ┌──────────────────────────────────────────────┐
  │  Program: B.Sc Nursing (BSC-NUR)             │
  │  Academic Year: 2026–27                      │
  │                                              │
  │  Year 1:  ₹1,03,000  (Tuition+Clinical+Lab) │
  │  Year 2:  ₹1,00,000                         │
  │  Year 3:  ₹1,05,000                         │
  │  Year 4:  ₹1,00,000                         │
  │  ─────────────────────────────────────────── │
  │  Original Total:  ₹4,08,000                  │
  └──────────────────────────────────────────────┘
         │
         ▼
  Apply Scholarship?
         │
  ┌─────┴─────┐
  │YES        │NO
  ▼           ▼
  Enter:      Skip
  ₹30,000 discount
  Reason: "Merit — 96% in 12th"
         │
         ▼
  ┌──────────────────────────────────────────────┐
  │  Final Fee:   ₹4,08,000 − ₹30,000           │
  │             = ₹3,78,000  ✅                  │
  └──────────────────────────────────────────────┘
         │
         ▼
  Click "Finalize Fee"
         │
         ▼
  ✅ Status → FEES_FINALIZED
  Student is notified of amount due
```

#### Step-by-Step: Finalize a Fee

1. Navigate to **Finance → Student Fees** or click 💰 button in enquiry list
2. Find the enquiry by student name
3. Review the year-wise fee breakdown
4. To apply a discount:
   - Enter amount in "Global Discount" field (e.g., `30000`)
   - Enter reason: `Merit Scholarship — 96% in 12th Grade`
5. Verify the **Final Total** shown
6. Click **"Finalize Fee"**
7. Enquiry status changes to `FEES_FINALIZED`

---

### 7.2 Payment Collection

**Navigate**: Finance → Fee Payments  
*Or click 💳 Collect Payment from enquiry list*

#### Accepted Payment Modes at SKS College of Nursing

| Mode | Reference Required | Notes |
|------|--------------------|-------|
| CASH | No | Receipt issued immediately |
| CHEQUE | Cheque number | Payable to "SKS College of Nursing" |
| NEFT / RTGS | UTR number | Bank transfer reference |
| UPI | Transaction ID | Google Pay, PhonePe, BHIM UPI |
| CARD | Terminal Auth code | Card swipe at campus terminal |
| Demand Draft (DD) | DD number | Payable at local bank |

#### How to Record a Payment

1. Navigate to Fee Payments or click Collect Payment from enquiry
2. Fill in:
   - **Student / Enquiry**: Auto-filled if from shortcut
   - **Amount Paid**: e.g., `50000`
   - **Payment Date**: `27-04-2026`
   - **Payment Method**: `NEFT`
   - **Transaction Reference**: e.g., `UTR123456789`
   - **Remarks**: e.g., `First instalment — Year 1 fee`
3. Click **"Collect Payment"**
4. Receipt is generated:
   ```
   Receipt No: RCP-SKS-20260427-001
   Student:    Meena Rajendran
   Program:    B.Sc Nursing
   Amount:     ₹50,000
   Method:     NEFT — UTR123456789
   Date:       27-Apr-2026
   Collected by: Ms. N Kumar (Admin)
   Balance:    ₹53,000 remaining
   ```

---

## 8. Reports

**Navigate**: Sidenav → Reports

### Reports Available to College Admin

| Report Name | Description | Useful For |
|-------------|-------------|-----------|
| Admissions Report | Total admissions by date range, by program | INC annual compliance |
| Fee Collections Report | All payments by period and method | Finance monthly review |
| Program-wise Enrollment | Current students per program | Capacity planning |
| Outstanding Fees Report | Students with pending balances | Collections drive |
| Enquiry Conversion Report | Enquiry to admission ratio | Marketing analysis |
| Document Verification Report | Docs verified vs. pending | Pre-registration audit |
| Faculty Workload Report | Courses per faculty | Semester planning |
| Department Report | Student count per department | Administrative review |

### How to Generate a Report

1. Click the desired report name
2. Set filters:
   - **Date Range**: Start and end date
   - **Program**: All or specific (e.g., `Bachelor of Science in Nursing`)
   - **Academic Year**: `2026–27`
3. Click **"Generate Report"**
4. View on screen or export:
   - **Export to PDF** → Download formatted file
   - **Export to Excel** → Download for further analysis
   - **Print** → Send to printer

---

## 9. Complete Admission Workflow

```
╔══════════════════════════════════════════════════════════════════════════╗
║     SKS COLLEGE OF NURSING — FULL ADMISSION WORKFLOW (ALL ROLES)        ║
╚══════════════════════════════════════════════════════════════════════════╝

   PROSPECTIVE STUDENT
          │ Enquires by walk-in / phone / social media
          ▼
  ┌────────────────────────────────────────────────────────────────────┐
  │  STEP 1 — ENQUIRY CREATION                    [Front Office]       │
  │  • Student name, phone, email collected                           │
  │  • Program selected: B.Sc Nursing / M.Sc / GNM / PBB / DOTT / DMLT │
  │  • Referral type noted                                            │
  │  • Status = ENQUIRED                                              │
  └──────────────────────────────────┬─────────────────────────────────┘
                                     │ Student confirms interest
                                     ▼
  ┌────────────────────────────────────────────────────────────────────┐
  │  STEP 2 — INTEREST CONFIRMATION               [Front Office]       │
  │  • Discuss program details, duration, career prospects            │
  │  • Change status to INTERESTED                                    │
  │  • Notify College Admin to set fees                               │
  │  • Status = INTERESTED                                            │
  └──────────────────────────────────┬─────────────────────────────────┘
                                     │ (College Admin action)
                                     ▼
  ┌────────────────────────────────────────────────────────────────────┐
  │  STEP 3 — FEE FINALIZATION                    [College Admin ONLY] │
  │  • Load fee structure for program + academic year                 │
  │  • Apply scholarship / concession if applicable                   │
  │  • Confirm final fee amount                                       │
  │  • Status = FEES_FINALIZED                                        │
  └──────────────────────────────────┬─────────────────────────────────┘
                                     │ Student pays
                                     ▼
  ┌────────────────────────────────────────────────────────────────────┐
  │  STEP 4 — PAYMENT COLLECTION    [Cashier / Front Office / Admin]  │
  │  • Accept CASH / CHEQUE / NEFT / UPI / CARD / DD                 │
  │  • Record in system with transaction reference                    │
  │  • Issue printed receipt                                          │
  │  • Full pay → FEES_PAID                                           │
  │  • Partial pay → PARTIALLY_PAID (can still proceed to docs)        │
  └──────────────────────────────────┬─────────────────────────────────┘
                                     │ Student brings documents
                                     ▼
  ┌────────────────────────────────────────────────────────────────────┐
  │  STEP 5 — DOCUMENT COLLECTION   [Front Office / College Admin]    │
  │  Upload & verify 7 mandatory documents:                           │
  │    ✓ 10th Marksheet                  ✓ Medical Fitness Certificate │
  │    ✓ 12th Marksheet (Science stream)  ✓ Blood Group Report         │
  │    ✓ Transfer Certificate             ✓ Aadhar Card                │
  │    ✓ Passport Photo                                               │
  │  Click "Submit Documents" once all 7 are verified                 │
  │  Status = DOCUMENTS_SUBMITTED                                     │
  └──────────────────────────────────┬─────────────────────────────────┘
                                     │
                                     ▼
  ┌────────────────────────────────────────────────────────────────────┐
  │  STEP 6 — ADMISSION CREATION    [College Admin / Front Office]    │
  │  • Open Admission Completion screen                               │
  │  • Auto-fill from enquiry: Name, Program, Phone                   │
  │  • Enter: Semester, Admission date, DOB, Blood Group, Community   │
  │  • Click "Create Admission"                                       │
  │  • Student ID: SKS-NUR-2026-XXX                                   │
  │  • Status = ADMITTED ✅                                            │
  └────────────────────────────────────────────────────────────────────┘

  Typical Timeline: 1–3 weeks from enquiry to admission
```

---

## 10. Troubleshooting

### Cannot see 💰 Finalize Fee button

- Enquiry must be in `INTERESTED` status
- If status is `ENQUIRED` → Update status to `INTERESTED` first from the status dropdown
- If status is already `FEES_FINALIZED` → Fee is already done, skip to payment

### Admission Creation is disabled / greyed out

- Check that **all 7 mandatory documents** are uploaded (UPLOADED or VERIFIED status)
- Verify enquiry status is `DOCUMENTS_SUBMITTED`
- If stuck at `FEES_PAID`, go to Document Submission, upload docs, then submit

### Fee amount shows as ₹0 or incorrect amount

- Check fee structure exists for the student's program and academic year
- Navigate to Preferences → Fee Structures → Search program
- If missing, create fee structure with correct year amounts
- Then re-attempt fee finalization

### Document rejected — Medical Fitness Certificate

- Must be from State Medical Council registered doctor
- Certificate must be dated within last 6 months
- Must mention "fit for nursing study" explicitly
- Must have doctor's stamp and signature
- Ask student to get a fresh certificate

### Student has duplicate enquiries

- Search student's phone number in enquiry list
- Identify the correct (most recent) enquiry
- On the duplicate, change status to `CLOSED`
- Continue processing the correct enquiry

### Keycloak session expired

- You see "Session expired / unauthorized" error
- Refresh the browser page (F5)
- You should be automatically redirected to Keycloak login
- Log in again with your credentials
- Session typically lasts 8 hours

---

## 11. Quick Reference

### SKS Programs at a Glance

| Code | Program | Duration | Seats | Key Subjects |
|------|---------|----------|-------|-------------|
| BSC-NUR | B.Sc Nursing | 4 years | 60 | Anatomy, MSN, Community, OBG, Paediatrics |
| MSC-NUR | M.Sc Nursing | 2 years | 30 | Advanced nursing, Research, Leadership |
| GNM | Gen. Nursing & Midwifery | 3.5 years | 60 | Nursing basics, Midwifery, Community |
| PBB-NUR | Post Basic B.Sc Nursing | 2 years | 30 | Bridge to degree for GNM nurses |
| DOTT | Diploma OT Technician | 2 years | 30 | OT procedures, instruments, sterile technique |
| DMLT | Diploma Med. Lab Technology | 2 years | 30 | Haematology, Biochemistry, Microbiology |

### Status Cheat Sheet

| Status | What it Means | Your Action |
|--------|--------------|------------|
| ENQUIRED | Just enquired | Assess interest, guide |
| INTERESTED | Confirmed interest | 💰 Finalize fees |
| FEES_FINALIZED | Fees set | Wait for payment |
| FEES_PAID | Fully paid | 📁 Collect documents |
| PARTIALLY_PAID | Partially paid | Collect documents, collect remaining fee |
| DOCUMENTS_SUBMITTED | All docs uploaded | 🎓 Create admission |
| ADMITTED | ✅ Done! | Enroll in semester |

### Admin Daily Checklist

```
MORNING:
  [ ] Review dashboard — any new enquiries?
  [ ] Check enquiries awaiting fee finalization (status: INTERESTED)
  [ ] Check document submissions awaiting verification

AFTERNOON:
  [ ] Process fee finalizations for interested students
  [ ] Review and follow up on outstanding payments
  [ ] Handle admissions for DOCUMENTS_SUBMITTED enquiries

END OF DAY:
  [ ] Generate daily collections summary
  [ ] Check pending document verifications
  [ ] Log out securely
```

---

**SKS College of Nursing | College Administrator User Guide**
**Version 2.0 | April 27, 2026 | Next Review: July 2026**

