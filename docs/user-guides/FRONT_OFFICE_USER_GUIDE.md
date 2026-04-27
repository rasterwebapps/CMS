# College Management System - Front Office User Guide

## Version 1.0 | April 27, 2026

---

## Table of Contents

1. [Introduction](#introduction)
2. [Role Overview](#role-overview)
3. [Getting Started](#getting-started)
4. [Front Office Dashboard](#front-office-dashboard)
5. [Enquiry Management](#enquiry-management)
6. [Payment Collection](#payment-collection)
7. [Document Management](#document-management)
8. [Admission Creation](#admission-creation)
9. [Reports](#reports)
10. [Common Workflows](#common-workflows)
11. [Troubleshooting](#troubleshooting)
12. [Keyboard Shortcuts](#keyboard-shortcuts)

---

## Introduction

Welcome to the College Management System! This guide is designed specifically for **Front Office Staff**.

As a Front Office representative, you are the first point of contact for prospective students. You manage their inquiries, track their progress through the admission process, collect initial information, and guide them through document submission and admission completion.

### About This Document

This handbook walks you through all features available to your role, focusing on student-facing interactions and admission process management.

---

## Role Overview

### What is a Front Office Representative?

A Front Office representative is responsible for:
- ✅ Managing student enquiries and interactions
- ✅ Recording initial student information
- ✅ Collecting payments from students
- ✅ Managing student document submissions
- ✅ Coordinating with students for admission completion
- ✅ Generating reports on admissions progress
- ✅ Supporting the admission workflow

### Permissions Summary

| Area | Permission | Access |
|------|-----------|--------|
| Dashboard | View | ✅ Front Office Dashboard |
| Enquiry Management | Full | ✅ Create, update, search enquiries |
| Payment Collection | Full | ✅ Record and track payments |
| Document Submission | Full | ✅ Collect and verify documents |
| Admission Creation | Full | ✅ Create admissions from enquiries |
| Reports | View | ✅ Admission and collection reports |
| Preferences | No Access | ❌ (College Admin only) |
| Finance Configuration | No Access | ❌ (College Admin only) |

---

## Getting Started

### Login Process

1. **Navigate to Application**: Open your browser and go to the CMS application URL
2. **Keycloak Login**: You'll see the Keycloak login screen
3. **Enter Credentials**:
   - Username: Your college email
   - Password: Your Keycloak password
4. **Front Office Dashboard**: You'll see your role-specific dashboard

### First-Time Setup

**Verify Your Profile**:
- Click your avatar in the top-right corner
- Confirm role shows as "Front Office"
- Verify contact information is correct

**Explore Your Access**:
- You have access to: Admission Management and Reports
- You cannot access: Preferences and Finance Configuration
- These are controlled by College Admin

---

## Front Office Dashboard

### Dashboard Overview

Your personalized dashboard shows metrics specific to the admission process you manage.

### Dashboard Widgets

#### Today's Statistics
- **New Enquiries**: How many new students inquired today
- **Outstanding Collections**: Payments due from students
- **Document Submissions**: Documents received today

**Click any metric** to jump to that section for details

#### Admission Progress
- **In Progress**: Enquiries moving through the workflow
- **Ready for Admission**: Students with complete documents
- **Admissions Created**: Students admitted this month

#### Weekly Performance
- **Conversion Rate**: Enquiry to admission conversion
- **Collection Amount**: Fees collected this week
- **Document Completion**: Percentage of complete applications

### Quick Actions

**Action Buttons** on the dashboard:
- **"New Enquiry"** - Start a new student registration
- **"Collect Payment"** - Quick access to payment entry
- **"View All Enquiries"** - Jump to full enquiry list

---

## Enquiry Management

### Overview

An enquiry is created when a prospective student shows interest in your institution. Managing enquiries is your primary responsibility.

**Access**: Sidenav → Admission Management → Enquiries

### Creating a New Enquiry

#### Step-by-Step

1. **Click "New Enquiry"** button (top-right)

2. **Student Information Section**:
   - **Name**: Student's full name
   - **Email**: Contact email address
   - **Phone**: Mobile number for contact
   - These are required fields (marked with *)

3. **Program Interest Section**:
   - **Program**: Select from dropdown (e.g., "B.Tech CSE")
   - **Course**: Optional - specific course (if applicable)
   - **Student Type**: How are they entering?
     - First Year (brand new, just finished 12th grade)
     - Lateral Entry (transferring from another program)

4. **Reference Information** (Optional):
   - **Agent**: If referred by recruitment agent
   - **Referral Type**: How did they learn about you?
     - "Website", "Direct Walk-in", "Alumni Referral", etc.
   - **Remarks**: Any notes about the student

5. **Click "Create Enquiry"**

6. **Confirmation**:
   - New enquiry created
   - Automatically assigned to "ENQUIRED" status
   - Unique enquiry ID generated
   - Enquiry now appears in your list

#### Example Enquiry Creation

```
Name: Rahul Kumar
Email: rahul@email.com
Phone: +91-9876543210
Program: B.Tech Computer Science
Student Type: First Year
Referral Type: Website
Remarks: Good performance in 12th, interested in scholarship
```

---

### Managing Enquiries - List View

#### Accessing the Enquiry List

1. **Sidenav** → **Admission Management** → **Enquiries**
2. Shows all enquiries you have access to
3. Default sorted by latest enquiry date first

#### Search and Filter

**Search by Name/Phone/Email**:
- Type in search box top-left
- System searches across name, email, and phone
- Real-time filtering as you type

**Filter by Status**:
- Dropdown at top: "All Statuses" or select specific status
- Status options:
  - ENQUIRED - Just started
  - INTERESTED - Student confirmed interest
  - FEES_FINALIZED - Fees set by college admin
  - FEES_PAID - Payment received (full or partial)
  - DOCUMENTS_SUBMITTED - All documents uploaded
  - ADMITTED - Admission created, accepted

**Filter by Date Range**:
- "From Date" and "To Date" fields
- Click dates to select
- Filters enquiries created in that range

**Sort**:
- Click column headers to sort
- Click name, date, program, etc.
- Arrow indicates sort direction (↑ ascending, ↓ descending)

#### Enquiry List Columns

| Column | Meaning |
|--------|---------|
| **Name** | Student name |
| **Phone** | Contact number |
| **Program** | Interested program |
| **Type** | Student type (First Year, Lateral Entry) |
| **Enquiry Date** | When enquiry was created |
| **Referral** | How they heard about you |
| **Status** | Current status in workflow |
| **Agent** | Assigned agent (if any) |
| **Actions** | Quick action buttons |

#### Quick Action Buttons

**For each enquiry in the list**:

1. **View Details** (Eye icon):
   - Opens full enquiry record
   - Shows all information and tabs

2. **Edit** (Pencil icon):
   - Modify enquiry details
   - Update phone, email, program preference

3. **Status Actions**:
   - **Update Status** (Status badge):
     - Click the status badge/label
     - Select new status from dropdown
     - Confirm change
   - **Finalize Fee** (Green button) - Only if status = INTERESTED:
     - College Admin sets the fees
     - You will see this button after admin finalizes
   - **Collect Payment** (Teal button) - Only if status = FEES_FINALIZED or PARTIALLY_PAID:
     - Click to record payments
   - **Submit Documents** (Blue button) - Only if status = FEES_PAID or PARTIALLY_PAID:
     - Collect uploaded documents

---

### Enquiry Detail View

When you click on an enquiry or open via edit icon:

#### Overview Tab

Shows complete student information:
- **Student Details**: Name, email, phone
- **Program Info**: Program, course, student type
- **Referral Info**: Agent, referral type, external remarks
- **Financial**: Net finalized fee (if already finalized by college admin)
- **Status Indicators**: Current status badge with visual color coding

#### Documents Tab

Shows all uploaded documents for the student:

**Document List shows**:
- **Type**: TENTH_MARKSHEET, TWELFTH_MARKSHEET, TRANSFER_CERTIFICATE, AADHAR_CARD, PASSPORT_PHOTO
- **Status**: PENDING, VERIFIED, REJECTED
- **File Name**: Original filename uploaded
- **Verification Info**: Who verified and when

**File Actions**:
- **View** (Eye icon): Opens document (preview mode)
- **Download** (Download icon): Save to your computer

#### Payments Tab

Shows all payments recorded against this enquiry:

**For each payment**:
- **Receipt Number**: Unique identifier for this payment
- **Amount Paid**: How much was received
- **Date**: When payment was made
- **Method**: CASH, ONLINE, CHEQUE, CARD
- **Collected By**: Who recorded the payment
- **Status**: Usually COMPLETED

#### Status History Tab

Timeline of all status changes:

**Shows for each change**:
- **Changed At**: Date and time
- **From Status**: Previous status
- **To Status**: New status
- **Changed By**: Who made the change
- **Remarks**: Any note about the change

---

### Status Workflow

An enquiry progresses through statuses. Understand each:

```
ENQUIRED
    ↓ [Student confirms interest]
INTERESTED
    ↓ [College Admin finalizes fees]
FEES_FINALIZED
    ↓ [Student pays fees]
FEES_PAID (or PARTIALLY_PAID)
    ↓ [All documents submitted]
DOCUMENTS_SUBMITTED
    ↓ [Admission is created]
ADMITTED
```

### Your Role in Each Status

| Status | Your Actions |
|--------|--------------|
| ENQUIRED | Contact student, answer questions, help select program |
| INTERESTED | Coordinate with college to set fees |
| FEES_FINALIZED | Collect payment, send receipt |
| FEES_PAID | Collect documents from student |
| DOCUMENTS_SUBMITTED | Prepare for admission creation |
| ADMITTED | Onboard new student |

---

## Payment Collection

### Overview

As Front Office, you collect initial payments from students. This is a crucial function for revenue collection.

**Access**: Sidenav → Admission Management → Enquiries (or shortcuts from enquiry list)

### Recording a Payment - Method 1: From Enquiry List

1. **Find the enquiry** with status "FEES_FINALIZED" or "PARTIALLY_PAID"
2. **Click "Collect Payment"** button (teal button on the right)
3. **You're taken to Payment Collection screen** with enquiry pre-filled
4. **Enter Payment Details**:
   - **Amount Paid**: How much student is paying
   - **Payment Date**: Date money received
   - **Payment Method**: 
     - CASH: Student pays in person
     - ONLINE: Bank transfer
     - CHEQUE: Student gives cheque
     - CARD: Credit/debit card payment
   - **Transaction Reference**: (For online/card, include confirmation number)
   - **Remarks**: Any notes about payment

5. **Click "Collect Payment"**

6. **Receipt Generated**:
   - Receipt number assigned
   - Shows all payment details
   - Display on screen for student to keep
   - Or print if in-person

### Recording a Payment - Method 2: Direct Entry

If you're not starting from enquiry list:

1. Navigate to Admission Management
2. Click on Enquiries > Find the enquiry
3. Open enquiry detail
4. Click "Payments" tab
5. Click "Record Payment" button
6. follows same process as above

### Payment Methods Explained

#### CASH Payment
- Student pays in person
- You count money
- Verify amount matches
- Record date received
- Student gets receipt

#### ONLINE Payment
- Student makes bank transfer
- You receive a confirmation
- Enter transaction reference number
- Online confirmation email forwarded to college
- Record in system

#### CHEQUE Payment
- Student gives physical cheque
- Write down cheque number
- Include cheque number in transaction reference
- Record date cheque received
- Cheque goes to finance for clearing

#### CARD Payment
- Student pays via card (in-office terminal)
- Card reader confirms transaction
- Transaction ID generated
- Record transaction ID in system
- Keep receipt for records

### Record Partial Payments

If student pays less than full amount:
- Enter the amount actually received
- Status changes to "PARTIALLY_PAID"
- Student still has remaining balance
- Remaining amount shows in enquiry

**Example**:
- Finalized fee: ₹100,000
- Student pays: ₹50,000
- Remaining: ₹50,000
- Status: PARTIALLY_PAID

### Payment Receipt

After successful payment entry, you see:

**Receipt Display shows**:
- **Receipt Number**: e.g., "RCP20260427001"
- **Student Name**: Who paid
- **Program**: Their program
- **Amount Paid**: Rupee amount
- **Balance Remaining**: If partial payment
- **Payment Method**: How paid
- **Date**: Payment date
- **Collected By**: Your name

**Actions**:
- **Print**: Print for student records
- **Email**: Send receipt to student's email
- **Save/Close**: Dismiss and continue

---

## Document Management

### Overview

Documents are proof of student eligibility. Managing submissions is a key front office duty.

**Access**: Sidenav → Admission Management → Document Submission

### What are Mandatory Documents?

All students must submit these 5 documents:

1. **Tenth Grade Marksheet**
   - Marksheet from 10th standard examination
   - Shows grades/scores

2. **Twelfth Grade Marksheet**
   - Marksheet from 12th standard examination
   - Most recent academic record

3. **Transfer Certificate**
   - Document from previous school
   - Shows good standing and character

4. **Aadhar Card**
   - Government-issued ID proof
   - Or alternative national ID

5. **Passport Photo**
   - 4x6 cm photo
   - Recent passport-size photo

### Collecting Documents Process

1. **Navigate to Document Submission**
   - Sidenav → Admission Management → Document Submission

2. **Find Eligible Enquiry**
   - Status must be "FEES_PAID" or "PARTIALLY_PAID"
   - List shows only eligible enquiries
   - Search for student by name

3. **Click on Enquiry**
   - Opens document collection screen
   - Student details shown at top

4. **Upload First Document**:
   - Click dropdown for document type
   - Select first document type
   - Click "Upload File" button
   - Browse and select file from computer
   - File uploads to system

5. **Verify Document**:
   - Review uploaded document
   - Ensure it's clear and readable
   - If acceptable, click "Verify"
   - Status changes to VERIFIED
   - If not acceptable, click "Reject"
   - Add remark explaining why

6. **Repeat for All 5 Documents**:
   - Upload each of 5 mandatory documents
   - Verify each one

7. **Complete Submission**:
   - Once all 5 are VERIFIED or UPLOADED
   - Click "Submit Documents" button
   - System validates completeness
   - Enquiry status changes to DOCUMENTS_SUBMITTED
   - Student now ready for admission creation

### Document Status Meanings

| Status | Meaning | Action |
|--------|---------|--------|
| PENDING | Uploaded but not reviewed | Review and verify |
| UPLOADED | Document received but not checked | Verify when ready |
| VERIFIED | Document reviewed and accepted | None needed |
| REJECTED | Document not acceptable | Ask student to resubmit |

### Document Verification Rules

**Clear and Legible**:
- Document must be readable
- No excessive blur or damage
- All text visible

**Not Altered**:
- No signs of modification
- Original document preferred
- All stamps/signatures intact

**Valid Timeframe**:
- Document must be current
- No expired ID proofs
- Recent marksheets acceptable

### Rejecting and Requesting Resubmission

If document is unacceptable:

1. Click "Reject" button for that document
2. Add remark explaining issue:
   - "Photo too blurry, please resubmit"
   - "Aadhar card expired, need new copy"
   - "Marksheet incomplete, needs all pages"
3. Click "Save"
4. Contact student to resubmit corrected document
5. Student uploads new version
6. You verify again

---

## Admission Creation

### Overview

Once documents are submitted, you guide the student through admission creation.

**Access**: Sidenav → Admission Management → Admission Completion (or from enquiry detail)

### Prerequisites for Admission Creation

Student enquiry must have:
- ✅ Status = "DOCUMENTS_SUBMITTED"
- ✅ All 5 documents uploaded and verified
- ✅ Fees finalized
- ✅ Payment received (at least partial)

### Creating an Admission

#### Step 1: Find the Enquiry

1. Navigate to **Admission Completion**
2. List shows only enquiries ready for admission
3. Search for student by name
4. Click on the enquiry

#### Step 2: Fill Admission Form

**Basic Information** (Pre-filled):
- **Student Name**: Auto-filled from enquiry
- **Email**: Auto-filled from enquiry
- **Phone**: Auto-filled from enquiry

**Admission Details** (Required):
- **Semester**: Which semester starting (e.g., "Fall 2026, Semester 1")
- **Admission Date**: Date of joining
- **Academic Year From**: Starting year
- **Academic Year To**: Ending year
- **Application Date**: When application was submitted

**Student Personal Information**:
- **Date of Birth**: Student's birthdate
- **Gender**: Male, Female, Other
- **Aadhar Number**: Government ID number
- **Nationality**: Country of citizenship
- **Religion**: Optional
- **Community Category**: E.g., SC, ST, OC, MBC, DNC
- **Blood Group**: For records

**Family Information**:
- **Father's Name**: Father/guardian name
- **Mother's Name**: Mother name
- **Parent Mobile**: Parent contact number

#### Step 3: Verify All Information

Before submitting:
1. Review all filled information
2. Verify dates are correct
3. Check spelling of names
4. Confirm semester and academic year

#### Step 4: Create Admission

1. Click **"Create Admission"** button
2. System processes the form
3. Success message appears
4. New **Admission ID** generated
5. New **Student ID** created
6. Enquiry status changes to **"ADMITTED"**

#### Confirmation Screen

Shows:
- **Admission ID**: e.g., "ADM20260427001"
- **Student ID**: e.g., "STU20260001"
- **Student Name**
- **Program**
- **Enrollment details**

**Actions Available**:
- **Print Admission Form**: For student records
- **Email Confirmation**: Send to student
- **Continue**: Go back to enquiries

---

## Reports

### Overview

Reports help track admission progress and performance metrics.

**Access**: Sidenav → Reports

### Available Reports for Front Office

#### 1. Admissions Report
**Shows**: Total admissions by time period

**How to Generate**:
1. Click "Admissions Report"
2. Select "Date Range":
   - From Date
   - To Date
3. Click "Generate"
4. Report shows:
   - Total admissions in period
   - Trend over time
   - Program-wise breakdown
   - Source/referral analysis

**Use Case**: Manager asks "How many students did we admit this month?"

#### 2. Collection Report
**Shows**: Payment collections by period

**How to Generate**:
1. Click "Fee Collections Report"
2. Select Period (e.g., this month)
3. Click "Generate"
4. Report shows:
   - Total collected
   - Collection method breakdown (cash vs. online)
   - Student-wise collections
   - Outstanding balances

**Use Case**: Finance asks "What was our collection this week?"

#### 3. Enquiry Status Report
**Shows**: Where enquiries are in the workflow

**How to Generate**:
1. Click "Enquiry Status Report" (if available)
2. View breakdown:
   - How many in ENQUIRED
   - How many in INTERESTED
   - How many waiting on documents
   - etc.

**Use Case**: "Where are we in our admission pipeline?"

### Exporting Reports

**Export Options**:
- **Excel**: Download spreadsheet for analysis
- **PDF**: Download formatted report
- **Print**: Send to configured printer
- **Email**: Schedule report to recipients

**How to Export**:
1. After generating report
2. Click export format button
3. File downloads to your computer
4. Optional: Save or open file

---

## Common Workflows

### Workflow 1: First Contact to Admission (Complete Journey)

**Day 1 - Initial Inquiry**
1. Student walks in or calls
2. You create new enquiry
3. Enter student details and program interest
4. Status: ENQUIRED

**Days 2-5 - Interest Confirmation**
1. Student confirms interest
2. Change status to INTERESTED
3. College Admin is notified

**Day 5-7 - Fee Finalization** (College Admin action)
1. College Admin finalizes fees
2. Student informed of amount due

**Day 7-14 - Payment Collection** (Your action)
1. Student comes to pay or transfers online
2. You record payment
3. Generate and provide receipt
4. Status: FEES_PAID or PARTIALLY_PAID

**Day 14-21 - Document Collection** (Your action)
1. Student provides documents
2. You upload each document
3. Verify authenticity
4. Once all 5 verified, mark DOCUMENTS_SUBMITTED
5. Status: DOCUMENTS_SUBMITTED

**Day 21-30 - Admission Creation** (Your action)
1. Create admission from enquiry
2. Fill in additional details
3. Generate Student ID
4. Provide admission confirmation
5. Status: ADMITTED

**Timeline**: 1-4 weeks from inquiry to admitted student

---

### Workflow 2: Handling Multiple Students (Daily Routine)

**Morning** (9:00 AM - 12:00 PM):
1. Check dashboard for action items
2. Review enquiries waiting for payments
3. Send payment reminders to students
4. Meet students bringing documents
5. Upload and verify documents submitted

**Afternoon** (1:00 PM - 5:00 PM):
1. Collect payments from students
2. Process document submissions
3. Create admissions for ready students
4. Answer student questions
5. Update enquiry statuses

---

### Workflow 3: Generating Weekly Reports

**Every Friday**:
1. Navigate to Reports
2. Generate "Fee Collections Report"
   - Date Range: This week
3. Export to Excel
4. Send to Finance department
5. Generate "Admissions Report"
   - Date Range: This week
6. Share with management

---

## Troubleshooting

### Issue: Cannot see "Collect Payment" button

**Causes & Solutions**:
1. **Enquiry not in correct status**:
   - Status must be FEES_FINALIZED or PARTIALLY_PAID
   - If status is ENQUIRED or INTERESTED:
     - College Admin hasn't finalized fees yet
     - Wait for admin to finalize, button will appear

2. **Fees not finalized**:
   - Contact College Admin
   - Ask them to finalize fees for this student
   - Once finalized, button appears

---

### Issue: Student says they already paid but I don't see the payment

**Solutions**:
1. **Check if payment is recorded**:
   - Open enquiry detail
   - Go to Payments tab
   - Search for their receipt

2. **If not found**:
   - Ask student for receipt number
   - Search by receipt number
   - If not in system, needs to be recorded:
     - Open enquiry
     - Click "Collect Payment"
     - Enter amount and date
     - Save

3. **If dated months ago**:
   - Payment may be in old academic year
   - Check date range in filter

---

### Issue: Document upload fails

**Solutions**:
1. **File too large**:
   - Compress image first
   - Supported formats: JPG, PNG, PDF
   - Max size: Usually 10 MB

2. **Wrong file format**:
   - Ensure file is PDF or image
   - Not Word, Excel, or other formats
   - If document is a scan, save as JPG or PDF

3. **Browser issue**:
   - Try different browser
   - Clear cache: Ctrl+Shift+Del
   - Try uploading again

---

### Issue: Cannot find student in list

**Solutions**:
1. **Search by different field**:
   - Try searching by phone instead of name
   - Try searching by email
   - Name spelling may be different

2. **Check status filter**:
   - If looking for "FEES_PAID" students
   - But student in "INTERESTED" status
   - Won't appear in filtered view
   - Change filter to "All Statuses"

3. **Check date range**:
   - If enquiry created long ago
   - May not appear in current date range filter
   - Expand date range

---

### Issue: Cannot create admission for a student

**Solutions**:
1. **Check prerequisites**:
   ```
   ✓ Status = DOCUMENTS_SUBMITTED
   ✓ All 5 documents verified
   ✓ Fees finalized
   ✓ Payment received
   ```

2. **If status not DOCUMENTS_SUBMITTED**:
   - Complete document submission first
   - Click "Submit Documents" button
   - Wait for status to change

3. **If documents not all submitted**:
   - Upload missing documents
   - Verify each one
   - Then submit documents

---

### Issue: Report not showing expected data

**Solutions**:
1. **Check date range**:
   - Ensure date range includes the data
   - If looking for January data, start date should be Jan 1

2. **Check filters**:
   - If report has program filter
   - Ensure correct program selected
   - Or select "All Programs"

3. **Data timing**:
   - Data may not update immediately
   - Try refreshing page: F5
   - Wait a few moments and try again

---

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `Ctrl + K` | Open search bar |
| `Ctrl + /` | Show all shortcuts |
| `Escape` | Close dialog/modal |
| `Enter` | Submit form |
| `Alt + A` | Go to Admission Management |
| `Alt + R` | Go to Reports |
| `Ctrl + Shift + L` | Focus on search/list |

---

## Quick Reference Card

### Daily Tasks Checklist

- [ ] Review new enquiries from morning walk-ins
- [ ] Follow up on students with outstanding payments
- [ ] Collect and upload documents from students
- [ ] Create admissions for ready students
- [ ] Generate daily collection report
- [ ] Update enquiry statuses
- [ ] Send receipts to students

### Status Decision Tree

```
Is student ENQUIRED?
  → Call student, confirm interest
  → If confirmed, change to INTERESTED

Is student INTERESTED?
  → Wait for College Admin to finalize fees
  → Once finalized, change to FEES_FINALIZED

Is student FEES_FINALIZED?
  → Collect payment from student
  → Record in system
  → Status changes automatically

Is student FEES_PAID/PARTIALLY_PAID?
  → Collect documents from student
  └→ 5 mandatory documents needed
  → Upload and verify
  → Submit documents
  → Status changes to DOCUMENTS_SUBMITTED

Is student DOCUMENTS_SUBMITTED?
  → Create admission with complete information
  → Generate Student ID
  → Status changes to ADMITTED
```

### Most Used Paths

| Task | Path | Time |
|------|------|------|
| New enquiry | Admission → Enquiries → +New | 3 min |
| Find student | Admission → Enquiries → Search | 1 min |
| Record payment | Click "Collect Payment" on enquiry | 5 min |
| Upload documents | Document Submission → Find student | 10 min |
| Create admission | Admission Completion → Select student | 5 min |
| Check reports | Reports → Select report type | 2 min |

---

## Support & Help

### Questions?
- Refer to relevant section above
- Ask your Front Office supervisor
- Contact system administrator
- Email: [support email]
- Phone: [support number]

### Common Terms Glossary

| Term | Meaning |
|------|---------|
| Enquiry | Initial student registration of interest |
| Status | Current position in admission workflow |
| Finalized Fee | Amount student must pay (set by college admin) |
| Collection | Payment received from student |
| Documents | 5 mandatory files (marksheets, ID, photo) |
| Admission | Final acceptance and enrollment |
| Student ID | Unique identifier for admitted student |
| Receipt | Proof of payment for student |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Apr 27, 2026 | Initial release |

---

**Last Updated**: April 27, 2026  
**Next Review**: July 27, 2026

**Thank you for your dedicated service to our students!**

