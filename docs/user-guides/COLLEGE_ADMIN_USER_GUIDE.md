# College Management System - College Admin User Guide

## Version 1.0 | April 27, 2026

---

## Table of Contents

1. [Introduction](#introduction)
2. [Role Overview](#role-overview)
3. [Getting Started](#getting-started)
4. [Main Dashboard](#main-dashboard)
5. [Preferences & Settings](#preferences--settings)
6. [Admission Management](#admission-management)
7. [Finance Management](#finance-management)
8. [Reports](#reports)
9. [Common Workflows](#common-workflows)
10. [Troubleshooting](#troubleshooting)
11. [Keyboard Shortcuts](#keyboard-shortcuts)

---

## Introduction

Welcome to the College Management System! This guide is designed specifically for **College Administrators**. As a College Admin, you have comprehensive access to manage college operations, from student admissions to financial tracking and reporting.

### About This Document

This handbook will walk you through all features available to your role, providing step-by-step instructions for common tasks and best practices for efficient workflow management.

---

## Role Overview

### What is a College Admin?

A College Admin is responsible for:
- ✅ Managing the college's academic structure (departments, programs, courses)
- ✅ Setting up academic calendars and semesters
- ✅ Managing fee structures and financial parameters
- ✅ Overseeing the admission process
- ✅ Processing fee finalization and payments
- ✅ Managing faculty and administrative staff
- ✅ Generating reports and analytics
- ✅ Configuring system settings

### Permissions Summary

| Area | Permission | Access |
|------|-----------|--------|
| Dashboard | View | ✅ Full |
| Preferences | Edit | ✅ Full (Departments, Programs, Courses, Academic Calendar, etc.) |
| Admission Management | Full | ✅ Enquiries, Document Submission, Admissions, Students |
| Finance | Full | ✅ Fee Structures, Student Fees, Fee Payments, Fee Finalization |
| Curriculum | View Only | ❌ Limited (Admin only) |
| Reports | View | ✅ Full |
| Settings | No Access | ❌ Admin only |

---

## Getting Started

### Login Process

1. **Navigate to the application**: Open your browser and go to the CMS application URL
2. **Keycloak Login Screen**: You will be redirected to the Keycloak identity server
3. **Enter Credentials**:
   - Username: Your college admin email
   - Password: Your Keycloak password
4. **Dashboard**: You'll be directed to the admin dashboard after successful authentication

### First-Time Setup

**Step 1**: Verify your profile
- Click your **avatar/initials** in the top-right corner
- Verify your name, email, and role displays as "College Admin"

**Step 2**: Familiarize yourself with the navigation menu
- Left sidebar contains all major sections
- Expandable groups show related features
- Shortcuts to frequently used screens

**Step 3**: Customize your dashboard
- Dashboard provides quick statistics
- Pin favorite reports for quick access

---

## Main Dashboard

### Dashboard Overview

The Dashboard provides at-a-glance metrics about your institution's operations.

### Dashboard Widgets

#### Admissions Widget
- **Total Enquiries**: Number of active enquiries
- **Documents Submitted**: Ready for admission creation
- **Admissions Pending**: Awaiting completion

**Action**: Click on any metric to jump to that section

#### Finance Widget
- **Outstanding Fees**: Total pending collections
- **Monthly Collections**: This month's fee payments
- **Fee Finalization Status**: Pending fee finalizations

**Action**: Click metrics to view detailed lists

#### Faculty Widget
- **Total Faculty**: Count of active faculty members
- **By Department**: Breakdown by academic department

#### System Status
- **Active Academic Year**: Current operational year
- **Current Semester**: Ongoing semester details
- **Online Users**: Real-time count

### Dashboard Actions

**Quick Navigation Buttons**:
- "New Enquiry" - Create a new student enquiry
- "Record Payment" - Quick access to payment collection
- "Finalize Fees" - Start fee finalization process
- "View Reports" - Access reporting dashboard

---

## Preferences & Settings

### Overview

The Preferences section is where you configure your college's core academic and administrative structure.

### 1. Departments Management

**Access**: Sidenav → Preferences → Departments

#### Creating a Department

1. Click **"+ New Department"** button (top-right)
2. Fill in the form:
   - **Department Name**: e.g., "Computer Science"
   - **Department Code**: e.g., "CS" (unique identifier)
   - **Description**: Brief description of the department
3. Click **"Save Department"**
4. Verify the new department appears in the list

#### Editing a Department

1. Find the department in the list
2. Click the **pencil icon** (edit button)
3. Update the required fields
4. Click **"Save"**

#### Viewing Department Details

1. Click on the department row (or the eye icon)
2. View associated programs and courses
3. See faculty assigned to the department
4. Click **"Back"** to return to the list

**Best Practice**: Use meaningful codes (e.g., "CS" for Computer Science) for quick identification.

---

### 2. Programs Management

**Access**: Sidenav → Preferences → Programs

#### Creating a New Program

1. Click **"+ New Program"** button
2. Form fields:
   - **Program Name**: e.g., "Bachelor of Technology in CSE"
   - **Program Code**: e.g., "B.TECH.CSE"
   - **Department**: Select from dropdown
   - **Duration (Years)**: e.g., 4
   - **Intake Capacity**: Number of seats available
   - **Description**: Program overview
3. Click **"Save Program"**

#### Important**: Each program must be associated with a department. Select the correct department before saving.

---

### 3. Courses Management

**Access**: Sidenav → Preferences → Courses

**Courses are the individual subjects/subjects included in a program each semester**

#### Creating a Course

1. Click **"+ New Course"** button
2. Fill in details:
   - **Course Name**: e.g., "Data Structures"
   - **Course Code**: e.g., "CS201"
   - **Program**: Select the program this course belongs to
   - **Semester**: Which semester does this course run?
   - **Credits**: Academic credits (e.g., 4)
3. Click **"Save Course"**

---

### 4. Academic Years

**Access**: Sidenav → Preferences → Academic Years

Academic years define study periods for your institution.

#### Creating an Academic Year

1. Click **"+ New Academic Year"** button
2. Enter:
   - **Start Year**: e.g., 2026
   - **End Year**: e.g., 2027
   - **Label**: Auto-filled as "2026-27" (can edit)
   - **Status**: Set to "Active" if this is the current year
3. Click **"Save"**

**Note**: You can have one active academic year at a time.

#### Changing Active Academic Year

1. Find the desired academic year in the list
2. Click the **status dropdown** for that year
3. Select **"Active"**
4. Confirm the change

---

### 5. Semesters

**Access**: Sidenav → Preferences → Semesters

Semesters divide the academic year into instructional periods.

#### Creating a Semester

1. Click **"+ New Semester"** button
2. Fill in:
   - **Name**: e.g., "Fall 2026"
   - **Semester Number**: e.g., 1 (first semester)
   - **Academic Year**: Select from dropdown
   - **Start Date**: When instruction begins
   - **End Date**: When instruction ends
   - **Exam Start Date**: When exams begin
   - **Exam End Date**: When exams conclude
3. Click **"Save"**

---

### 6. Fee Structures

**Access**: Sidenav → Preferences → Fee Structures

Fee structures define how much students pay and what components comprise fees.

#### Creating a Fee Structure

1. Click **"+ New Fee Structure"** button
2. Basic Information:
   - **Program**: Which program is this for?
   - **Academic Year**: Applicable year
   - **Course**: Select specific course (if course-specific fees)
   - **Fee Type**: e.g., "Tuition", "Lab Fee", "Hostel"
3. Fee Components (click "Add Fee Component"):
   - **Component Name**: e.g., "Tuition Fee"
   - **Year (1-4)**: Which year's students pay this
   - **Amount**: Fee amount in currency
4. Click **"Save Fee Structure"**

#### Example Fee Structure
```
Program: B.Tech CSE
Academic Year: 2026-27
Components:
  Year 1: ₹100,000
  Year 2: ₹95,000
  Year 3: ₹90,000
  Year 4: ₹85,000
```

#### Bulk Update Fee Structures

For multiple programs/years:
1. Click **"Bulk Upload"** button
2. Prepare CSV file with columns: Program, AcademicYear, FeeType, Year, Amount
3. Upload the file
4. Verify preview
5. Click **"Confirm Upload"**

---

### 7. Faculty Management

**Access**: Sidenav → Preferences → Faculty

#### Adding a Faculty Member

1. Click **"+ New Faculty"** button
2. Personal Information:
   - **First Name**, **Last Name**
   - **Email**: Official email address
   - **Phone**: Contact number
   - **Employee ID**: Unique identifier
3. Assignment:
   - **Department**: Faculty department
   - **Designation**: e.g., "Assistant Professor"
   - **Specialization**: Subject expertise
4. Click **"Save Faculty"**

#### Assigning Courses to Faculty

1. Find faculty in list, click to open
2. Click **"Assign Courses"** tab
3. Select courses to assign from dropdown
4. Click **"Add Course"**
5. Review assignments
6. Click **"Save"**

---

### 8. Agents & Referral Types

**Access**: Sidenav → Preferences → Agents and Referral Types

#### Managing Agents (Admission Representatives)

Agents help recruit students to your institution.

1. Click **"+ New Agent"** button
2. Enter:
   - **Agent Name**: Representative's name
   - **Agency Name**: If representing an organization
   - **Commission Type**: Percentage commission or flat fee
   - **Commission Rate**: e.g., 5% or ₹500
3. Save agent

#### Managing Referral Types

Referral types categorize how enquiries came to you.

1. Click **"+ New Referral Type"** button
2. Enter:
   - **Type Name**: e.g., "Website", "Direct Walk-in", "Alumni"
   - **Description**: Optional details
3. Save

---

## Admission Management

### Overview

The Admission Management section handles the complete student admission workflow from initial enquiry to student enrollment.

### 1. Enquiries Management

**Access**: Sidenav → Admission Management → Enquiries

Enquiry is the first step when someone shows interest in your programs.

#### Creating a New Enquiry

1. Click **"+ New Enquiry"** button (top-right)
2. Fill in Student Information:
   - **Name**: Prospective student's name
   - **Email**: Contact email
   - **Phone**: Contact number
   - **Program**: Interested program
   - **Course**: (If applicable)
   - **Student Type**: e.g., "First Year", "Lateral Entry"
3. Add Optional Information:
   - **Agent**: If referred by an agent
   - **Referral Type**: Source of enquiry
   - **Remarks**: Any notes about the student
4. Click **"Create Enquiry"**
5. Enquiry status automatically set to "ENQUIRED"

#### Enquiry Status Workflow

An enquiry progresses through these statuses:

```
ENQUIRED → INTERESTED → FEES_FINALIZED → FEES_PAID → DOCUMENTS_SUBMITTED → ADMITTED
```

#### Managing Enquiries - List View

1. Navigate to Enquiries list
2. **Search**: Find by student name, phone, or email
3. **Filter by Status**: Use dropdown to filter
4. **Date Range**: Filter by enquiry date range
5. **Sort**: Click column headers to sort

#### Key Actions in List View

**Status Update** (when applicable):
- Click the **status badge** on an enquiry
- Select new status from dropdown
- Confirm transition

**Finalize Fee** (from shortcut button):
1. Find enquiry with status "INTERESTED"
2. Click **"Finalize Fee"** button (green button, right side)
3. Redirects to Fee Finalization screen (see Finance section)

**Collect Payment** (from shortcut button):
1. Find enquiry with status "FEES_FINALIZED" or "PARTIALLY_PAID"
2. Click **"Collect Payment"** button (success/teal button)
3. Redirects to payment collection screen

**Submit Documents** (from shortcut button):
1. Find enquiry with status "FEES_PAID" or "PARTIALLY_PAID"
2. Click **"Submit Documents"** button
3. Opens document submission workflow

**View Details**:
- Click on enquiry name or row
- Opens detailed enquiry view with tabs:
  - Overview (personal details)
  - Documents (uploaded files)
  - Payments (transaction history)
  - Status History (workflow timeline)

#### Enquiry Detail View

When you click on an enquiry:

**Overview Tab**:
- Student details
- Program information
- Agent and referral details
- Net fee amount

**Documents Tab**:
- Uploaded documents
- Verification status (PENDING, VERIFIED, REJECTED)
- File download/view options

**Payments Tab**:
- Payment history
- Receipt numbers
- Amount paid
- Collection method (CASH, ONLINE, CHEQUE)

**Status History Tab**:
- Timeline of status changes
- Who made each change
- When each change occurred

#### Quick Actions on Enquiry Detail

**Submit Documents Button** (becomes active when status = FEES_PAID):
1. Verify all mandatory documents are marked UPLOADED or VERIFIED
2. Click **"Submit Documents"**
3. Status changes to DOCUMENTS_SUBMITTED
4. Documents cannot be reverse-transitioned

**Create Admission Button** (becomes active when status = DOCUMENTS_SUBMITTED):
1. Click **"Create Admission"**
2. Redirects to admission creation form
3. Enquiry details pre-filled
4. Create new student with additional data
5. Completes admission workflow

---

### 2. Document Submission

**Access**: Sidenav → Admission Management → Document Submission

Documents are proof of student eligibility and qualifications.

#### What are Mandatory Documents?

Your institution requires these 5 documents from every student:
1. Tenth Grade Marksheet
2. Twelfth Grade Marksheet
3. Transfer Certificate
4. Aadhar Card (or Government ID)
5. Passport Photo

#### Uploading Documents

1. Click **"Document Submission"** from menu
2. Find enquiry with status "FEES_PAID" or "PARTIALLY_PAID"
3. Click to open document collection screen
4. For each document:
   - Select document type from dropdown
   - Click **"Upload File"**
   - Select image/PDF from your computer
   - Click **"Verify"** if document is authentic
5. Add remarks if needed
6. Repeat for all 5 mandatory documents

#### Document Status Indicators

- **⏳ PENDING**: Uploaded but not yet reviewed
- **✅ VERIFIED**: Document reviewed and accepted
- **❌ REJECTED**: Document not acceptable, needs resubmission

#### Completing Document Submission

When all 5 mandatory documents are marked UPLOADED or VERIFIED:
1. Click **"Submit Documents"** button at bottom
2. System validates all mandatory docs are submitted
3. Enquiry status changes to DOCUMENTS_SUBMITTED
4. Student becomes eligible for admission creation

**Important**: All mandatory documents must be submitted. You cannot proceed without them.

---

### 3. Admission Completion

**Access**: Sidenav → Admission Management → Admission Completion

This screen shows enquiries ready for admission creation.

#### Creating an Admission

1. Navigate to "Admission Completion"
2. List shows only enquiries with status = DOCUMENTS_SUBMITTED
3. Click on an enquiry to open
4. Click **"Create Admission"** button
5. Form opens pre-filled with enquiry data:
   - **Student Name**: Auto-filled
   - **Email**: Auto-filled
   - **Semester**: Select entrance semester
   - **Admission Date**: Date of joining
   - **Academic Year**: From and To years
   - **Application Date**: When application was submitted
   - **Additional Fields**: Personal details, family info, etc.
6. Complete all required fields (marked with *)
7. Click **"Create Admission"**
8. System creates:
   - New **Admission** record
   - New **Student** record
   - Transitions enquiry to **ADMITTED** status
9. Success message appears with new Student ID

---

### 4. Admissions List

**Access**: Sidenav → Admission Management → Admissions

View all admissions created in the system.

#### Features

**Search and Filter**:
- Search by student name or admission ID
- Sort by admission date
- Filter by status

**Status Types**:
- PENDING - Awaiting completion
- APPROVED - Approved admission
- ENROLLED - Student actively studying
- GRADUATED - Completed program
- WITHDRAWN - Student left

#### Actions

**View Admission Details**:
- Click on admission to view:
  - Student information
  - Documents uploaded
  - Associated enquiry
  - Fees and payments

**Update Admission Status**:
- Click status dropdown
- Select new status
- Confirm update

#### Academic Qualifications

Each admission can have multiple prior academic qualifications:
1. Open an admission
2. Click **"Academic Qualifications"** tab
3. Click **"+ Add Qualification"**
4. Fill in:
   - **Examination**: e.g., "10th Grade"
   - **Board**: e.g., "CBSE"
   - **Marks**: Score obtained
   - **Percentage**: Calculate percentage
5. Save qualification

---

### 5. Students List

**Access**: Sidenav → Admission Management → Students

View all enrolled and active students.

#### Student Information

Each student record contains:
- Personal details
- Academic enrollment information
- Associated admission
- Active courses and progress

#### Search and Filter

- Search by name, student ID, email
- Filter by program
- Filter by enrollment status

#### Student Details View

Click on a student to see:
- **Profile**: Personal information
- **Enrollment**: Courses and semester info
- **Fees**: Fee allocation and payment status
- **Documents**: Submitted documents
- **Attendance**: (if enabled) Attendance records

---

## Finance Management

### Overview

The Finance section handles all monetary transactions including fee collection, finalization, and payment tracking.

**Access**: Sidenav → Finance

---

### 1. Student Fees

**Access**: Sidenav → Finance → Student Fees

Student Fees is where you allocate fees to students based on program and semester.

#### Fee Finalization

Fee Finalization is the process of assigning fee amounts to students for a specific academic year.

#### How to Finalize Student Fees

1. Navigate to **Finance → Student Fees → Finalize**
2. **Search for Student/Enquiry**:
   - Enter student name or enquiry ID
   - Select from dropdown
3. **Fee Breakdown** appears showing:
   - Year 1 fee: ₹X
   - Year 2 fee: ₹X
   - Year 3 fee: ₹X
   - Year 4 fee: ₹X
   - **Original Total**: Sum of all years
4. **Apply Discount** (Optional):
   - Enter global discount amount: ₹Y
   - Or enter discount percentage: Z%
   - System recalculates final amount
   - **Final Total** = Original - Discount
5. **Add Discount Reason** (if discount applied):
   - e.g., "Merit scholarship", "Financial hardship"
6. Click **"Finalize Fee"**
7. System creates fee allocation
8. Enquiry status changes from INTERESTED to FEES_FINALIZED
9. Confirmation shows:
   - Final fee amount
   - Number of years included
   - Applied discount

#### Bulk Fee Finalization

For multiple students:
1. Click **"Bulk Finalize"** button (if available)
2. Upload CSV file with:
   - Student ID / Enquiry ID
   - Discount amount (optional)
   - Discount reason (optional)
3. Review preview
4. Confirm bulk operation
5. System processes all records
6. Download results summary

---

### 2. Fee Payments

**Access**: Sidenav → Finance → Fee Payments

Record all payments received from students.

#### Recording a Payment

**Method 1: From Fee Payments List**
1. Click **"+ Record Payment"** button
2. **Payment Details**:
   - **Student**: Select from dropdown
   - **Fee Structure**: Choose which fee is being paid
   - **Amount Paid**: Entry amount
   - **Payment Date**: Date money received
   - **Payment Method**: CASH / CARD / ONLINE / CHEQUE
   - **Transaction ID**: (For online/card payments)
   - **Status**: Usually COMPLETED
3. Click **"Save Payment"**
4. Receipt generated automatically
5. Payment appears in student's fee history

**Method 2: From Enquiry List (Shortcut)**
1. Navigate to Enquiries
2. Find enquiry with status "FEES_FINALIZED" or "PARTIALLY_PAID"
3. Click **"Collect Payment"** button
4. Page redirects to payment form pre-filled with enquiry details
5. Enter payment amount and method
6. Process payment

#### Payment Methods

- **CASH**: Student pays in person
- **CHEQUE**: Student provides cheque (include cheque number)
- **ONLINE**: Bank transfer (include transaction reference)
- **CARD**: Credit/Debit card payment (include transaction ID)
- **DD**: Demand Draft (include DD number)

#### Viewing Payment History

1. Navigate to **Fee Payments** list
2. View all payments recorded
3. Search by student name or receipt number
4. Filter by payment method or date range
5. Click on payment to view receipt

#### Receipt Details

After recording payment, receipt shows:
- **Receipt Number**: Unique identifier
- **Student Name**: Who paid
- **Amount**: Payment amount
- **Date**: Payment date
- **Method**: How paid
- **Remaining Balance**: Still outstanding

---

### 3. Fee Finalization

**Access**: Sidenav → Finance → Fee Finalization

This is the primary area for fee finalization (already covered above, repeated for convenience).

**Important Actions**:
- Allocate fees to students
- Apply scholarships or discounts
- Set payment terms
- Track payments against allocation

**Note**: Only College Admin and Admin can finalize fees. Front Office and Cashier cannot.

---

## Reports

### Overview

Reports provide business intelligence on your institution's operations.

**Access**: Sidenav → Reports

### Available Reports

#### 1. Admissions Report
- Total admissions by period
- Program-wise admissions
- Status breakdown
- Admission source analysis

**How to Generate**:
1. Click **"Admissions Report"**
2. Select date range
3. Optionally filter by program
4. Click **"Generate"**
5. View in browser or export to Excel

#### 2. Fee Collections Report
- Total fees collected by period
- Collection trend analysis
- Outstanding fees
- Collection method breakdown

**How to Generate**:
1. Click **"Fee Collections Report"**
2. Select date range
3. Choose academic year
4. Click **"Generate"**
5. Displays pie chart and detailed table

#### 3. Student Progress Report
- Semester-wise progress
- Course completion rates
- Attendance summary
- Grades and performance

#### 4. Faculty Workload Report
- Courses assigned per faculty
- Class sizes
- Lab/practical sessions

#### 5. Financial Summary Report
- Total fees budgeted vs. collected
- Outstanding list
- Department-wise fees
- Year-wise trends

**How to Generate**:
1. Click report type
2. Configure filters and date range
3. Click **"Run Report"**
4. View or export results

#### Export Options

Most reports allow:
- **Export to PDF**: Download as formatted PDF
- **Export to Excel**: Download spreadsheet for further analysis
- **Print**: Direct print to configured printer
- **Email**: Schedule report to email recipients

---

## Common Workflows

### Workflow 1: Complete Admission from Enquiry to Student

**Goal**: Convert an enquiry into an active student

**Steps**:

1. **Create Enquiry** (Day 1)
   - New → Enquiry
   - Enter student details
   - Student now in "ENQUIRED" status

2. **Mark as Interested** (When student expresses interest)
   - Open enquiry
   - Click status dropdown
   - Change to "INTERESTED"
   - Enquiry now open for fee finalization

3. **Finalize Fees** (College Admin action)
   - Finance → Student Fees → Finalize
   - Search the enquiry
   - Review fee breakdown
   - Apply discount (if applicable)
   - Click "Finalize Fee"
   - Status changes to "FEES_FINALIZED"

4. **Collect Payment** (Cashier or College Admin action)
   - Finance → Fee Payments
   - OR from Enquiry list: Click "Collect Payment" button
   - Enter payment amount and method
   - Process payment
   - Status changes to "FEES_PAID" (full) or "PARTIALLY_PAID"

5. **Submit Documents** (College Admin action)
   - Admission Management → Document Submission
   - Find the enquiry
   - Upload all 5 mandatory documents
   - Mark documents as VERIFIED
   - Click "Submit Documents"
   - Status changes to "DOCUMENTS_SUBMITTED"

6. **Create Admission** (College Admin action)
   - Admission Management → Admission Completion
   - Find the enquiry (status = DOCUMENTS_SUBMITTED)
   - Click "Create Admission"
   - Fill in additional student details (semester, admission date, etc.)
   - Click "Create Admission"
   - New Student created
   - Enquiry status changes to "ADMITTED"

**Timeline**: Usually 1-7 days depending on document submission speed

---

### Workflow 2: Apply Bulk Fee Finalization

**Goal**: Finalize fees for multiple students at once

**Steps**:

1. **Prepare Data**:
   - Create Excel/CSV with columns:
     - Student ID or Enquiry ID
     - Discount Amount (optional)
     - Discount Reason (optional)

2. **Upload**:
   - Finance → Student Fees → Finalize
   - Click "Bulk Upload" (if available)
   - Select CSV file
   - Upload

3. **Preview**:
   - System shows preview of all records
   - Verify data accuracy
   - Check for any errors

4. **Confirm**:
   - Click "Confirm Upload"
   - System processes all records
   - Shows results summary
   - Download results CSV (optional)

**Result**: All students in the file have fees finalized

---

### Workflow 3: Generate Monthly Collections Report

**Goal**: Understand how much fee revenue your college collected this month

**Steps**:

1. **Navigate to Reports**:
   - Sidenav → Reports

2. **Select Report Type**:
   - Click "Fee Collections Report"

3. **Configure**:
   - Start Date: 1st of month
   - End Date: Today's date
   - Academic Year: Current year (if applicable)

4. **Generate**:
   - Click "Generate Report"

5. **Review**:
   - Shows total collected
   - Payment method breakdown
   - Program-wise collections
   - Trends and comparisons

6. **Export**:
   - Click "Export to Excel" or "Export to PDF"
   - File downloads to your computer
   - Use for financial reporting

---

## Troubleshooting

### Issue: I cannot see "Finalize Fee" button for an enquiry

**Solution**:
- The enquiry must be in "INTERESTED" status
- If enquiry is in "ENQUIRED" status, change it first:
  - Open enquiry
  - Click status dropdown
  - Select "INTERESTED"
  - Finalize button will now appear

---

### Issue: Student documents rejected, cannot submit

**Solution**:
1. Open the enquiry with rejected documents
2. Go to Document Submission tab
3. For each rejected document:
   - Re-upload the file
   - Ensure it's clear and legible
   - Click "Verify" to mark as accepted
4. Once all 5 are submitted/verified:
   - Click "Submit Documents" button
   - Status changes to DOCUMENTS_SUBMITTED

---

### Issue: Fee finalization shows wrong amount

**Solution**:
1. Check the fee structure for the student's program:
   - Sidenav → Preferences → Fee Structures
   - Search for the program
   - Verify amount for the student's year

2. Check for active discounts:
   - Some scholarships automatically apply
   - Verify discount rules in financial settings

3. If still incorrect:
   - Contact system administrator
   - Provide student name and program

---

### Issue: Payment entered but not appearing in collection report

**Solution**:
1. Verify payment was saved:
   - Finance → Fee Payments
   - Search for the payment
   - If not found, re-enter payment

2. Check the date:
   - Report date range must include payment date
   - If payment is dated next month, won't appear in this month's report

3. Verify academic year:
   - Payment and report must use same academic year

---

### Issue: Cannot create admission for a student

**Solution**:
- Student enquiry must be in "DOCUMENTS_SUBMITTED" status
- Check status progression:
  1. ENQUIRED → Update to INTERESTED
  2. INTERESTED → Finalize fees
  3. FEES_FINALIZED → Collect payment
  4. FEES_PAID → Submit documents
  5. DOCUMENTS_SUBMITTED → Now can create admission

- If stuck at any stage, work through each step before attempting admission creation

---

### Issue: Keycloak login not working

**Solution**:
1. Verify username and password are correct
2. Check Caps Lock is OFF
3. Verify you have "ROLE_COLLEGE_ADMIN" role assigned in Keycloak
4. Clear browser cache and cookies
   - Press Ctrl+Shift+Delete (or Cmd+Shift+Delete on Mac)
   - Select "Cookies and cached images"
   - Click "Clear"
5. Try again in a private/incognito browser window

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl + K` | Open global search bar |
| `Ctrl + /` | Show keyboard shortcuts help |
| `Escape` | Close current dialog/modal |
| `Enter` | Submit form / Confirm action |
| `Alt + D` | Go to Dashboard |
| `Alt + A` | Go to Admission Management |
| `Alt + F` | Go to Finance |
| `Alt + R` | Go to Reports |

---

## Quick Reference Card

### Most Common Actions

| Task | Path | Time |
|------|------|------|
| Create new enquiry | Admission → Enquiries → +New | 2 min |
| Finalize fees | Finance → Student Fees → Finalize | 5 min |
| Record payment | Finance → Fee Payments → Record | 3 min |
| Upload documents | Admission → Document Submission | 10 min |
| Create admission | Admission → Admission Completion | 5 min |
| Generate report | Reports → [Select Report] | 2 min |

### Key Statuses Remember

```
ENQUIRED → INTERESTED → FEES_FINALIZED → FEES_PAID → DOCUMENTS_SUBMITTED → ADMITTED
```

When student moves through each status, additional actions become available.

---

## Support & Help

### Need Help?
- Check the **Troubleshooting** section above
- Contact your system administrator
- Email: [support email from your institution]
- Phone: [support phone from your institution]

### System Status
- Check system status page for maintenance notices
- Follow your institution's IT department for announcements

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Apr 27, 2026 | Initial release for College Admin role |

---

**Last Updated**: April 27, 2026  
**Next Review**: July 27, 2026

