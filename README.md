# 🏛️ CMS

A comprehensive, enterprise-grade **College Management System** designed to manage and automate the day-to-day administrative and academic operations of a college or educational institution — with **Lab Management deeply integrated** across all modules.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Module Architecture](#-module-architecture)
- [All Modules](#-all-modules)
  - [1. Administration & Core](#-module-1-administration--core)
  - [2. Student Management](#-module-2-student-management)
  - [3. Faculty & Staff Management](#-module-3-faculty--staff-management)
  - [4. Academic & Curriculum](#-module-4-academic--curriculum)
  - [5. Attendance Management](#-module-5-attendance-management)
  - [6. Examination & Assessment](#-module-6-examination--assessment)
  - [7. Lab Management](#-module-7-lab-management)
  - [8. Finance & Fee Management](#-module-8-finance--fee-management)
  - [9. Library Management](#-module-9-library-management)
  - [10. Hostel Management](#-module-10-hostel-management)
  - [11. Transport Management](#-module-11-transport-management)
  - [12. Communication & Collaboration](#-module-12-communication--collaboration)
  - [13. Reports & Analytics](#-module-13-reports--analytics)
  - [14. Research & Publication](#-module-14-research--publication)
  - [15. Placement & Career](#-module-15-placement--career)
  - [16. Infrastructure & Inventory](#-module-16-infrastructure--inventory)
  - [17. Event & Activity Management](#-module-17-event--activity-management)
  - [18. Security & Compliance](#-module-18-security--compliance)
  - [19. Online Learning / LMS](#-module-19-online-learning--lms)
  - [20. Mobile & Integration](#-module-20-mobile--integration)
  - [21. Feedback & Survey](#-module-21-feedback--survey)
- [Lab Management Integration Map](#-lab-management-integration-map)
- [Why Lab Management Is Deeply Integrated](#-why-lab-management-is-deeply-integrated)
- [Implementation Roadmap](#-implementation-roadmap)
  - [Phase 1: Foundation & Identity](#-phase-1-foundation--identity-the-greenfield-setup)
  - [Phase 2: Core Academic & Lab Mapping](#-phase-2-core-academic--lab-mapping)
  - [Phase 3: Operational Logistics](#-phase-3-operational-logistics)
  - [Phase 4: Finance & Asset Management](#-phase-4-finance--asset-management)
  - [Phase 5: Assessment & Reporting](#-phase-5-assessment--reporting)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Overview

The **College Management System (CMS)** streamlines and centralizes all institutional processes — from student enrollment to faculty management, course scheduling, fee collection, lab operations, and more — into a single integrated platform.

### What Makes This CMS Unique?

- **21 Core Modules** covering every aspect of institutional management
- **~212 Features/Sub-Modules** for complete operational coverage
- **🧪 Lab Management deeply integrated** across 80%+ of all modules with ~121 integration points
- Designed for **accreditation compliance** (NBA, NAAC, ABET, UGC)
- Built for **scalability** — from small colleges to multi-campus universities

---

## 🚀 Key Features

| Feature | Description |
|---------|-------------|
| 🎓 **Student Lifecycle** | Admission → Enrollment → Attendance → Exams → Placement |
| 👨‍🏫 **Faculty Management** | Profiles, workload, appraisal, lab assignments |
| 🧪 **Integrated Lab Management** | 73 features across 15 sub-modules, connected to every module |
| 📝 **Examination System** | Theory + Practical exams, GPA/CGPA, transcripts |
| 💰 **Finance & Fees** | Fee collection, scholarships, budgeting, lab fee tracking |
| 📊 **Analytics & Reports** | Real-time dashboards, KPIs, accreditation reports |
| 🔒 **Safety & Compliance** | Lab safety, incident tracking, audit logs |
| 📱 **Mobile & Integrations** | Android/iOS app, biometric, RFID, IoT, virtual labs |
| 🌐 **Online Learning (LMS)** | Virtual classrooms, assignments, virtual lab integration |
| 💬 **Communication** | Portals for students, faculty, parents with notifications |

---

## 🏗️ Module Architecture

```
┌─────────────────────────────────────────────────────────┐
│                 COLLEGE MANAGEMENT SYSTEM                │
│                                                         │
│   ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐       │
│   │Student│ │Faculty│ │Finance│ │ Exam │ │  LMS │ ...  │
│   └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘       │
│      │        │        │        │        │             │
│      └────────┴────────┴───┬────┴────────┘             │
│                            │                            │
│                    ┌───────▼───────┐                    │
│                    │ 🧪 LAB MODULE │                    │
│                    │  (Integrated)  │                    │
│                    └───────────────┘                    │
│                                                         │
│   Shared Database │ Shared APIs │ Shared Auth │ Shared UI│
└─────────────────────────────────────────────────────────┘
```

---

## 📦 All Modules

### 📌 Master Module Index

| # | Category | Modules | Lab Integration |
|---|----------|---------|-----------------|
| 1 | Administration & Core | 7 | ✅ 7 points |
| 2 | Student Management | 8 | ✅ 7 points |
| 3 | Faculty & Staff Management | 8 | ✅ 8 points |
| 4 | Academic & Curriculum | 8 | ✅ 8 points |
| 5 | Attendance Management | 6 | ✅ 6 points |
| 6 | Examination & Assessment | 10 | ✅ 10 points |
| 7 | **🧪 Lab Management** | **73 features (15 sub-modules)** | **Core Module** |
| 8 | Finance & Fee Management | 11 | ✅ 10 points |
| 9 | Library Management | 6 | ✅ 4 points |
| 10 | Hostel Management | 6 | — |
| 11 | Transport Management | 6 | ✅ 1 point |
| 12 | Communication & Collaboration | 8 | ✅ 8 points |
| 13 | Reports & Analytics | 9 | ✅ 9 points |
| 14 | Research & Publication | 5 | ✅ 5 points |
| 15 | Placement & Career | 5 | ✅ 3 points |
| 16 | Infrastructure & Inventory | 6 | ✅ 6 points |
| 17 | Event & Activity Management | 5 | ✅ 4 points |
| 18 | Security & Compliance | 7 | ✅ 7 points |
| 19 | Online Learning / LMS | 7 | ✅ 7 points |
| 20 | Mobile & Integration | 7 | ✅ 7 points |
| 21 | Feedback & Survey | 4 | ✅ 4 points |
| | **Grand Total** | **~212 Modules/Features** | **~121 Integration Points** |

---

### 🏛️ Module 1: Administration & Core

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 1 | **Dashboard** | Overview of key metrics, notifications, quick links | ✅ Lab utilization widget, equipment alerts |
| 2 | **User Management** | Role-based access (Admin, Faculty, Student, Lab In-Charge, Technician, Parent) | ✅ Lab-specific roles |
| 3 | **Institution Profile** | College details, branding, campus management | ✅ Lab count, lab types per campus |
| 4 | **Academic Year Management** | Define academic years, semesters, terms | ✅ Lab sessions mapped per semester |
| 5 | **Department Management** | Create and manage departments | ✅ Department-wise lab assignment |
| 6 | **Program/Course Management** | Degree programs, streams, specializations | ✅ Lab components per program/course |
| 7 | **Notification & Alerts** | System-wide alerts, SMS, email, push | ✅ Lab schedule changes, equipment breakdown alerts |

---

### 🎓 Module 2: Student Management

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 8 | **Admission Management** | Online applications, merit lists, seat allocation | ✅ Lab fee included in admission fee structure |
| 9 | **Student Registration/Enrollment** | Student profiles, ID generation, document upload | ✅ Lab batch/group assignment during enrollment |
| 10 | **Student Promotion/Migration** | Year/semester promotion, transfers | ✅ Lab batch re-assignment on promotion |
| 11 | **Student Directory** | Searchable student database | ✅ Filter by lab batch, lab attendance |
| 12 | **Alumni Management** | Alumni profiles, networking | — |
| 13 | **Student ID Card Generation** | Automated ID cards | ✅ Lab access permissions encoded on ID |
| 14 | **Student Disciplinary Management** | Misconduct, warnings, suspensions | ✅ Lab misconduct incidents, safety violations |
| 15 | **Student Health Records** | Medical history, emergency contacts | ✅ Lab accident/injury records linked |

---

### 👨‍🏫 Module 3: Faculty & Staff Management

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 16 | **Faculty Management** | Faculty profiles, qualifications | ✅ Lab teaching assignments, lab expertise |
| 17 | **Staff/Employee Management** | Non-teaching staff records | ✅ Lab technicians, lab assistants as staff |
| 18 | **Recruitment Management** | Job postings, applications | ✅ Lab technician/assistant recruitment |
| 19 | **Payroll Management** | Salary processing, deductions | ✅ Lab staff payroll processing |
| 20 | **Leave Management** | Leave applications, approvals | ✅ Lab in-charge leave impacts scheduling |
| 21 | **Staff Attendance** | Biometric/manual attendance | ✅ Lab staff attendance tracking |
| 22 | **Performance Appraisal** | Faculty/staff evaluations | ✅ Lab teaching effectiveness, student feedback |
| 23 | **Faculty Workload Management** | Teaching hours, load balancing | ✅ Lab hours counted in total workload |

---

### 📚 Module 4: Academic & Curriculum

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 24 | **Curriculum/Syllabus Management** | Define syllabus, learning outcomes, credits | ✅ Lab syllabus, practical credit hours |
| 25 | **Subject/Course Allocation** | Assign subjects to faculty | ✅ Lab subject allocation to faculty |
| 26 | **Timetable/Schedule Management** | Auto/manual timetable generation | ✅ Lab timetable integrated, conflict-free scheduling |
| 27 | **Classroom/Room Management** | Room allocation, capacity | ✅ Lab rooms included in room inventory |
| 28 | **Lesson Planning** | Daily/weekly lesson plans | ✅ Lab experiment planning per session |
| 29 | **Academic Calendar** | Institution-wide calendar | ✅ Lab practical exam dates, lab events on calendar |
| 30 | **Elective Management** | Elective course selection | ✅ Lab electives with seat limits per lab capacity |
| 31 | **Lab-Curriculum Mapping** | Map experiments to course outcomes | ✅ Experiment-to-CO/PO mapping for accreditation |

---

### ✅ Module 5: Attendance Management

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 32 | **Student Attendance (Theory)** | Daily/period-wise attendance | — |
| 33 | **Student Attendance (Lab/Practical)** | Lab session attendance | ✅ Dedicated lab attendance — batch-wise, experiment-wise |
| 34 | **Attendance Reports** | Subject-wise, month-wise reports | ✅ Lab attendance reports — separate and combined |
| 35 | **Attendance Alerts** | Auto-alerts for low attendance | ✅ Lab attendance shortage alerts to students/parents |
| 36 | **Attendance Exemption** | Medical/event-based exemptions | ✅ Lab attendance exemptions |
| 37 | **Biometric/RFID Attendance** | Automated attendance capture | ✅ Lab-specific biometric/RFID entry/exit logging |

---

### 📝 Module 6: Examination & Assessment

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 38 | **Examination Management** | Exam scheduling, seating, hall tickets | ✅ Lab practical exam scheduling |
| 39 | **Online Examination/Quiz** | MCQ, descriptive, timed online tests | ✅ Online lab viva, pre-lab quizzes |
| 40 | **Grading & Marks Management** | Grade entry, GPA/CGPA calculation | ✅ Lab marks in GPA calculation |
| 41 | **Result Processing & Publishing** | Result generation, publishing | ✅ Lab practical results combined with theory |
| 42 | **Report Card/Transcript** | Automated report cards, marksheets | ✅ Lab marks displayed separately on transcripts |
| 43 | **Internal Assessment** | Assignments, projects, presentations | ✅ Lab internal assessment — record + viva + performance |
| 44 | **Question Bank Management** | Store, categorize exam questions | ✅ Lab viva question bank, practical question bank |
| 45 | **Moderation & Re-evaluation** | Re-checking, moderation | ✅ Lab practical re-evaluation requests |
| 46 | **Lab Practical Exam Management** | End-semester practical exams | ✅ External examiner assignment, practical exam grading |
| 47 | **Lab Continuous Evaluation** | Week-by-week lab performance | ✅ Experiment-wise marks, cumulative lab score |

---

### 🧪 Module 7: Lab Management

> **The core Lab Management Module with 73 features across 15 sub-modules.**

#### 7.1 Lab Setup & Configuration

| # | Feature | Description |
|---|---------|-------------|
| 48 | **Lab Registration** | Register labs — name, type, department, location, capacity |
| 49 | **Lab Types Management** | Computer, Physics, Chemistry, Electronics, Biology, Language, etc. |
| 50 | **Lab Capacity & Layout** | Seating/workstation capacity, floor plan upload |
| 51 | **Lab In-Charge Assignment** | Assign in-charge, technicians, assistants per lab |
| 52 | **Multi-Campus Lab Management** | Manage labs across multiple campuses/buildings |

#### 7.2 Lab Scheduling & Timetable

| # | Feature | Description |
|---|---------|-------------|
| 53 | **Lab Timetable Generation** | Auto/manual scheduling per batch/section |
| 54 | **Batch/Group Assignment** | Divide students into lab batches |
| 55 | **Conflict Detection** | Prevent double-booking of labs |
| 56 | **Lab Slot Management** | Define time slots, recurring sessions |
| 57 | **Ad-hoc Lab Booking** | Faculty/students request extra lab sessions |
| 58 | **Lab Calendar View** | Visual calendar showing lab occupancy |

#### 7.3 Equipment & Apparatus Management

| # | Feature | Description |
|---|---------|-------------|
| 59 | **Equipment Registration** | Name, model, serial number, specs, purchase date |
| 60 | **Equipment Categorization & Tagging** | Barcode/QR/RFID tagging, category management |
| 61 | **Equipment Status Tracking** | Available, In Use, Under Repair, Damaged, Disposed |
| 62 | **Equipment Location Tracking** | Track which lab/room each equipment is in |
| 63 | **Equipment Lifecycle History** | Purchase → Usage → Repair → Disposal |
| 64 | **Equipment Specifications & Manuals** | Upload datasheets, user manuals, photos |

#### 7.4 Lab Inventory & Stock Management

| # | Feature | Description |
|---|---------|-------------|
| 65 | **Consumable Inventory** | Chemicals, reagents, wires, paper, ink, etc. |
| 66 | **Non-Consumable Inventory** | Microscopes, oscilloscopes, computers, etc. |
| 67 | **Stock In/Out Tracking** | Record incoming and outgoing stock |
| 68 | **Minimum Stock Alerts** | Auto-alerts when stock falls below threshold |
| 69 | **Vendor/Supplier Management** | Manage suppliers, purchase orders |
| 70 | **Purchase Request & Approval Workflow** | Request → HOD Approval → Admin Procurement |
| 71 | **Expiry Date & Hazardous Material Tracking** | Chemical expiry, MSDS, safe storage |
| 72 | **Wastage/Spillage & Disposal Tracking** | Record waste, safe disposal of hazardous materials |
| 73 | **Inventory Audit & Reconciliation** | Periodic stock verification |

#### 7.5 Experiment & Practical Management

| # | Feature | Description |
|---|---------|-------------|
| 74 | **Experiment Catalog** | Master list of experiments per subject/course |
| 75 | **Experiment Schedule Mapping** | Map experiments to lab sessions across semester |
| 76 | **Experiment Instructions & Resources** | Upload procedures, videos, diagrams, pre-lab materials |
| 77 | **Pre-Lab Requirements** | Required apparatus, chemicals, software per experiment |
| 78 | **Experiment Learning Outcomes** | CO/PO mapping for accreditation |
| 79 | **Experiment Completion Tracking** | Track which experiments each student completed |
| 80 | **Virtual Lab / Simulation Integration** | Remote experiments via virtual lab platforms |

#### 7.6 Student Lab Activities

| # | Feature | Description |
|---|---------|-------------|
| 81 | **Student Lab Attendance** | Session-wise, experiment-wise attendance |
| 82 | **Lab Workstation Assignment** | Assign specific benches/workstations to students |
| 83 | **Lab Journal/Record Submission** | Digital upload of lab records/journals |
| 84 | **Lab Viva / Oral Assessment** | Record viva marks per experiment |
| 85 | **Lab Internal Marks Calculation** | Attendance + Record + Viva + Performance = Total |
| 86 | **Student Equipment Issue/Return** | Track equipment checked out by students |
| 87 | **Student Lab Project Tracking** | Lab-based mini-projects, final year projects |

#### 7.7 Maintenance & Repair

| # | Feature | Description |
|---|---------|-------------|
| 88 | **Preventive Maintenance Scheduling** | Scheduled maintenance for lab equipment |
| 89 | **Breakdown Reporting** | Report equipment failures |
| 90 | **Repair Request & Tracking** | Raise requests, assign technicians, track status |
| 91 | **AMC (Annual Maintenance Contract)** | AMC details, renewal dates, vendor contacts |
| 92 | **Calibration Management** | Schedule calibration of precision instruments |
| 93 | **Repair Cost Tracking** | Record repair costs and spare parts |
| 94 | **Equipment Disposal/Write-Off** | Manage obsolete/damaged equipment disposal |

#### 7.8 Lab Safety & Compliance

| # | Feature | Description |
|---|---------|-------------|
| 95 | **Safety Guidelines Management** | Upload and display safety protocols per lab |
| 96 | **Safety Equipment Tracking** | Fire extinguishers, first aid, eye wash, fume hoods |
| 97 | **Safety Training Records** | Track safety training completion |
| 98 | **Incident/Accident Reporting** | Document lab accidents, injuries, near-misses |
| 99 | **Hazardous Material (MSDS) Management** | Chemical storage rules, safety data sheets |
| 100 | **Lab Access Control** | Biometric/RFID/card-based lab entry |
| 101 | **PPE Tracking** | Goggles, gloves, lab coats issuance and returns |
| 102 | **Emergency Procedures** | Evacuation plans, emergency contacts display |
| 103 | **Safety Audit & Compliance** | Periodic safety inspections, audit reports |

#### 7.9 Computer Lab Specific

| # | Feature | Description |
|---|---------|-------------|
| 104 | **Software Inventory & Licensing** | Track installed software, licenses, versions, renewals |
| 105 | **Hardware Inventory** | CPU, RAM, storage, monitor specs per workstation |
| 106 | **Network & IP Management** | IP addresses, network config, internet access |
| 107 | **User Account Management** | Student login accounts, access permissions |
| 108 | **Printing & Usage Tracking** | Print jobs, internet usage, screen time |
| 109 | **Remote Desktop Access** | Remote access to lab machines |
| 110 | **System Image & Cloning** | OS image management, bulk deployment |
| 111 | **Antivirus & Security Status** | Track security software per machine |

#### 7.10 Lab Reports & Analytics

| # | Feature | Description |
|---|---------|-------------|
| 112 | **Lab Utilization Report** | Usage %, peak hours, idle time per lab |
| 113 | **Equipment Utilization Report** | Frequency of use per equipment |
| 114 | **Lab Inventory Report** | Current stock, consumed items, pending orders |
| 115 | **Maintenance Report** | Pending repairs, completed maintenance, costs |
| 116 | **Student Lab Performance Report** | Student-wise attendance, marks, completion |
| 117 | **Lab Expense Report** | Consumables, repairs, purchases per lab |
| 118 | **Safety Incident Report** | Incident summary, actions taken |
| 119 | **Equipment Lifecycle Report** | Age, depreciation, remaining useful life |
| 120 | **Lab Accreditation Report** | CO/PO attainment, experiment completion rates for NBA/NAAC |

---

### 💰 Module 8: Finance & Fee Management

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 121 | **Fee Structure Management** | Define fee categories, amounts, due dates | ✅ Lab fee as separate fee component |
| 122 | **Fee Collection** | Online/offline payment, receipts | ✅ Lab fee collection tracking |
| 123 | **Scholarship & Financial Aid** | Scholarship applications, disbursement | ✅ Lab fee waiver/scholarship |
| 124 | **Fine & Penalty Management** | Late fees, library fines | ✅ Lab equipment damage fines, breakage charges |
| 125 | **Expense Management** | Track institutional expenses | ✅ Lab expenses — consumables, equipment, maintenance |
| 126 | **Income & Revenue Reports** | Revenue tracking | ✅ Lab fee revenue tracking |
| 127 | **Payment Gateway Integration** | Razorpay, Stripe, PayPal | ✅ Lab fee online payment |
| 128 | **Invoice & Receipt Management** | Auto-generated invoices | ✅ Lab fee receipts |
| 129 | **Budget Planning** | Departmental budgets | ✅ Lab budget allocation per department |
| 130 | **Purchase Order Management** | PO creation, tracking | ✅ Lab equipment/consumable POs |
| 131 | **Asset Depreciation** | Track asset value over time | ✅ Lab equipment depreciation |

---

### 📖 Module 9: Library Management

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 132 | **Book Cataloging** | Add, categorize, barcode/ISBN management | ✅ Lab manuals, practical handbooks |
| 133 | **Book Issue & Return** | Check-out, check-in, renewals | ✅ Lab reference books issuance |
| 134 | **Digital Library / E-Resources** | E-books, journals, research papers | ✅ Lab experiment guides, virtual lab resources |
| 135 | **Library Fine Management** | Overdue fines | — |
| 136 | **Library Membership** | Student/staff membership | — |
| 137 | **Library Reports** | Most borrowed, overdue, inventory | ✅ Lab manual borrowing statistics |

---

### 🏠 Module 10: Hostel Management

| # | Module | Description |
|---|--------|-------------|-----------------|
| 138 | **Hostel Room Allocation** | Room/bed assignment, capacity management |
| 139 | **Hostel Fee Management** | Hostel fee collection and tracking |
| 140 | **Mess/Food Management** | Meal plans, menu scheduling, mess fees |
| 141 | **Hostel Attendance** | In/out tracking, night attendance |
| 142 | **Hostel Complaint/Maintenance** | Maintenance requests, complaint tracking |
| 143 | **Visitor Management** | Visitor logs for hostel residents |

---

### 🚌 Module 11: Transport Management

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 144 | **Route Management** | Bus routes, stops, timings | — |
| 145 | **Vehicle Management** | Vehicle records, insurance | ✅ Lab equipment transport/delivery tracking |
| 146 | **Transport Fee Management** | Route-wise fee collection | — |
| 147 | **Driver & Staff Management** | Driver details, licenses | — |
| 148 | **GPS Tracking** | Real-time bus tracking | — |
| 149 | **Transport Reports** | Utilization, revenue reports | — |

---

### 💬 Module 12: Communication & Collaboration

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 150 | **Notice Board / Announcements** | Digital notice board | ✅ Lab schedule changes, lab notices |
| 151 | **Messaging System** | Internal messaging | ✅ Lab in-charge ↔ Faculty ↔ Student messaging |
| 152 | **Email Integration** | Bulk and individual emails | ✅ Lab-related email notifications |
| 153 | **SMS Integration** | SMS alerts | ✅ Lab attendance shortage SMS alerts |
| 154 | **Discussion Forum** | Topic-based discussions | ✅ Lab experiment discussion forums |
| 155 | **Parent Portal** | Parent access to child's progress | ✅ Lab attendance & marks visible to parents |
| 156 | **Student Portal** | Self-service student portal | ✅ Lab schedule, marks, record submission, equipment requests |
| 157 | **Faculty Portal** | Faculty dashboard and tools | ✅ Lab experiment management, grading, attendance |

---

### 📊 Module 13: Reports & Analytics

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 158 | **Student Performance Analytics** | Trend analysis, pass/fail rates | ✅ Lab vs. Theory performance comparison |
| 159 | **Attendance Analytics** | Department/class-wise trends | ✅ Lab attendance analytics |
| 160 | **Financial Reports** | Income, expense summaries | ✅ Lab expense & revenue reports |
| 161 | **Faculty Performance Reports** | Teaching effectiveness | ✅ Lab teaching effectiveness scores |
| 162 | **Custom Report Builder** | Custom reports with filters | ✅ Custom lab reports |
| 163 | **Dashboard & KPIs** | Real-time KPI dashboards | ✅ Lab KPI dashboard — utilization, safety, inventory |
| 164 | **Data Export** | Export to PDF, Excel, CSV | ✅ Lab data export |
| 165 | **Comparative Analytics** | Year-over-year comparison | ✅ Lab performance YoY comparison |
| 166 | **Accreditation Reports** | NBA/NAAC compliance reports | ✅ Lab CO/PO attainment, experiment completion rates |

---

### 🧪 Module 14: Research & Publication

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 167 | **Research Project Management** | Track research projects | ✅ Lab-based research projects — equipment, resources |
| 168 | **Publication Tracker** | Papers published, journals | ✅ Lab research publications |
| 169 | **Patent Management** | Patent applications and grants | ✅ Lab innovation/patent tracking |
| 170 | **Research Grant Management** | Funding, proposals | ✅ Lab equipment funded through grants |
| 171 | **Research Lab Management** | Dedicated research lab tracking | ✅ Separate management for research labs vs. teaching labs |

---

### 🏢 Module 15: Placement & Career

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 172 | **Placement Management** | Company drives, scheduling | ✅ Lab skills highlighted in student profiles |
| 173 | **Job/Internship Portal** | Job postings, applications | ✅ Lab-based internship opportunities |
| 174 | **Resume Builder** | Student CV creation | ✅ Lab projects, skills auto-populated in resume |
| 175 | **Placement Reports** | Placement statistics | — |
| 176 | **Career Counseling** | Counseling sessions | — |

---

### 🏗️ Module 16: Infrastructure & Inventory

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 177 | **Asset Management** | Track institutional assets | ✅ Lab equipment as assets — full lifecycle |
| 178 | **Inventory Management** | Stock tracking, procurement | ✅ Lab inventory synced with central inventory |
| 179 | **Lab Management (Cross-Link)** | Dedicated lab infrastructure | ✅ Direct link to Lab Module |
| 180 | **Maintenance & Helpdesk** | Facility maintenance | ✅ Lab maintenance requests routed through helpdesk |
| 181 | **Venue/Hall Booking** | Auditorium, conference room booking | ✅ Lab booking for special events, workshops |
| 182 | **Procurement Management** | Central procurement workflow | ✅ Lab procurement integrated with central purchasing |

---

### 🎉 Module 17: Event & Activity Management

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 183 | **Event Management** | Create, manage institutional events | ✅ Lab open days, lab exhibitions |
| 184 | **Club & Society Management** | Student clubs, activities | ✅ Science/tech club lab access |
| 185 | **Sports Management** | Sports events, achievements | — |
| 186 | **Cultural Activities** | Fests, competitions | ✅ Science fairs, lab competitions |
| 187 | **Certificate Generation** | Auto-generate certificates | ✅ Lab workshop/competition certificates |

---

### 🔒 Module 18: Security & Compliance

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 188 | **Gate Pass / Visitor Management** | Visitor entry logs | ✅ Lab visitor logs — external examiners, auditors |
| 189 | **CCTV Integration** | Security camera feeds | ✅ Lab CCTV monitoring |
| 190 | **Audit Logs** | Track all system activities | ✅ Lab module audit trails |
| 191 | **GDPR/Data Privacy Compliance** | Data protection | ✅ Lab student data privacy |
| 192 | **Document Management System** | Centralized document storage | ✅ Lab documents — MSDS, manuals, safety protocols |
| 193 | **Backup & Disaster Recovery** | Automated backups | ✅ Lab data included in backups |
| 194 | **Lab Safety Compliance** | Lab-specific safety standards | ✅ OSHA, fire safety, chemical safety compliance |

---

### 🌐 Module 19: Online Learning / LMS

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 195 | **Learning Management System** | Course content delivery | ✅ Lab course content, pre-lab materials |
| 196 | **Assignment Submission** | Online upload, grading | ✅ Lab record/journal submission |
| 197 | **Virtual Classroom** | Live classes, Zoom/Meet | ✅ Virtual lab demonstrations |
| 198 | **Study Material Sharing** | Notes, PDFs, videos | ✅ Lab experiment videos, procedure documents |
| 199 | **Plagiarism Checker** | Check submissions | ✅ Lab report plagiarism check |
| 200 | **Virtual Lab Integration** | Remote experiments | ✅ Integration with NPTEL/Virtual Lab platforms |
| 201 | **SCORM/xAPI Support** | E-learning standards | ✅ Virtual lab content compatibility |

---

### 📱 Module 20: Mobile & Integration

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 202 | **Mobile App (Android/iOS)** | On-the-go access | ✅ Lab schedule, attendance, marks on mobile |
| 203 | **Biometric Integration** | Fingerprint/face recognition | ✅ Lab biometric attendance |
| 204 | **RFID/Smart Card Integration** | Library, attendance, access | ✅ Lab RFID access control |
| 205 | **ERP Integration** | Existing ERP systems | ✅ Lab data synced with ERP |
| 206 | **Government Portal Integration** | AISHE, UGC, AICTE | ✅ Lab infrastructure data for regulatory submissions |
| 207 | **Third-party API Integration** | Google, Microsoft, etc. | ✅ Virtual lab APIs, IoT sensor data |
| 208 | **IoT Integration** | Smart sensors, automation | ✅ Lab IoT sensors (temperature, humidity, equipment monitoring) |

---

### 🗳️ Module 21: Feedback & Survey

| # | Module | Description | Lab Integration |
|---|--------|-------------|-----------------|
| 209 | **Student Feedback System** | Course and faculty feedback | ✅ Lab-specific feedback — facilities, equipment, teaching |
| 210 | **360° Feedback** | Multi-stakeholder feedback | ✅ Lab in-charge, technician feedback |
| 211 | **Survey/Poll Management** | Custom surveys | ✅ Lab satisfaction surveys |
| 212 | **Grievance Redressal** | Complaint submission, tracking | ✅ Lab grievances — equipment issues, safety concerns |

---

## 🔗 Lab Management Integration Map

```
                            ┌─────────────────────┐
                            │   🧪 LAB MANAGEMENT  │
                            │      (Core Hub)      │
                            └──────────┬──────────┘
                                       │
        ┌──────────────────────────────┼──────────────────────────────┐
        │                              │                              │
   ┌────▼─────┐                 ┌──────▼──────┐               ┌──────▼──────┐
   │ ACADEMIC  │                │  STUDENT    │               │  FACULTY    │
   │• Curriculum│               │• Lab Batch  │               │• Lab Assign │
   │• Timetable │               │• Lab Attend │               │• Lab Workld │
   │• Syllabus  │               │• Lab Marks  │               │• Lab Eval   │
   └────┬──────┘                └──────┬──────┘               └──────┬──────┘
        │                              │                              │
   ┌────▼──────┐                ┌──────▼──────┐               ┌──────▼──────┐
   │ EXAM      │                │  FINANCE    │               │ INFRA &     │
   │• Practical │               │• Lab Fees   │               │• Equipment  │
   │• Lab Viva  │               │• Lab Budget │               │• Maintenance│
   │• Lab Marks │               │• Damage Fine│               │• Helpdesk   │
   └────┬──────┘                └──────┬──────┘               └──────┬──────┘
        │                              │                              │
   ┌────▼──────┐                ┌──────▼──────┐               ┌──────▼──────┐
   │ ATTENDANCE│                │  LMS /      │               │ REPORTS &   │
   │• Lab Attend│               │  E-LEARNING │               │ ANALYTICS   │
   │• Biometric │               │• Virtual Lab│               │• Lab Reports│
   │• RFID      │               │• Pre-Lab    │               │• KPI Dashbd │
   └────┬──────┘                └──────┬──────┘               └──────┬──────┘
        │                              │                              │
   ┌────▼──────┐                ┌──────▼──────┐               ┌──────▼──────┐
   │ SAFETY &  │                │ COMMUNICA-  │               │ RESEARCH    │
   │ COMPLIANCE│                │ TION        │               │ MODULE      │
   │• Incidents │               │• Lab Alerts │               │• Research   │
   │• MSDS      │               │• Lab Notices│               │  Labs       │
   │• PPE       │               │• Parent Ntfy│               │• Lab Grants │
   └───────────┘                └─────────────┘               └─────────────┘
```

---

## 🤔 Why Lab Management Is Deeply Integrated

Lab Management **cannot function in isolation**. It touches students, faculty, finance, academics, safety, inventory, examinations, and compliance — making it one of the most cross-functional modules.

### Top 10 Reasons:

| # | Reason | Impact |
|---|--------|--------|
| 1 | **Student-Centric** | Complete student profile requires lab data |
| 2 | **Academic Completeness** | Curriculum, timetable, assessment depend on lab |
| 3 | **Financial Accuracy** | Lab income, expenses, budget need unified tracking |
| 4 | **Attendance Compliance** | Combined theory + lab attendance for exam eligibility |
| 5 | **Asset Management** | Lab equipment = most expensive institutional assets |
| 6 | **Safety & Legal** | Incident tracking, compliance, legal protection |
| 7 | **Accreditation** | NBA/NAAC/ABET require integrated lab data |
| 8 | **Real-Time Decisions** | Leadership needs complete dashboard visibility |
| 9 | **User Experience** | Students, parents, faculty need one unified portal |
| 10 | **Efficiency** | Eliminates 500-1000 hours/year of manual work |

### What Happens Without Integration:

- ❌ Duplicate data entry across systems
- ❌ Attendance mismatches affecting exam eligibility
- ❌ Lab marks manually transferred to result processing
- ❌ Equipment not tracked in institutional asset register
- ❌ Budget blind spots — lab expenses invisible in financial reports
- ❌ Weeks of manual work for accreditation data compilation

---

## 🗓️ Implementation Roadmap

### 🛠️ Phase 1: Foundation & Identity (The "Greenfield" Setup)

> **Focus:** Security, Infrastructure, and Core Data.

| # | Area | Tasks | Key Deliverables |
|---|------|-------|-----------------|
| 1.1 | **Identity Infrastructure** | Deploy Keycloak via Docker; Configure LDAP/AD federation; Define roles (ROLE_ADMIN, ROLE_FACULTY, ROLE_STUDENT, ROLE_LAB_INCHARGE, ROLE_TECHNICIAN, ROLE_PARENT) | Working SSO login for all user types |
| 1.2 | **Backend Core Setup** | Initialize Spring Boot 3.x (Java 21) with Virtual Threads; Implement Spring Security + OAuth2 Resource Server for JWT validation from Keycloak; Set up Global Exception Handler & Jakarta Bean Validation | Secured API skeleton with health endpoints |
| 1.3 | **Frontend Shell** | Initialize Angular 21 (Standalone Components, Signals); Implement Keycloak Auth Guard; Default landing page — Master Dashboard (Material 3) with "Lab Utilization" widget | Authenticated SPA shell with role-based navigation |

---

### 🧪 Phase 2: Core Academic & Lab Mapping

> **Focus:** Defining the "Who, Where, and What."

| # | Module | Tasks | Key Deliverables |
|---|--------|-------|-----------------|
| 2.1 | **Administration (Module 1)** | Department management; Program/Course management; Academic Year & Calendar setup | Institutional structure defined |
| 2.2 | **Lab Setup (Module 7.1)** | Register physical labs & lab types (Computer, Physics, etc.); Assign Lab In-Charges; Lab capacity & equipment baseline | Lab inventory & ownership established |
| 2.3 | **Academic & Curriculum (Module 4)** | Curriculum/Syllabus management; **Lab-Curriculum Mapping** — map experiments to Course Outcomes (CO/PO for accreditation) | Experiment-to-outcome traceability |
| 2.4 | **Faculty (Module 3)** | Faculty profile management; "Lab Expertise" tracking; Lab teaching assignments | Faculty–Lab readiness |

---

### 📅 Phase 3: Operational Logistics

> **Focus:** Scheduling and Attendance.

| # | Module | Tasks | Key Deliverables |
|---|--------|-------|-----------------|
| 3.1 | **Lab Scheduling (Module 7.2)** | Automated/manual lab timetable generator; Conflict detection with room & faculty availability | Conflict-free lab timetables |
| 3.2 | **Student Management (Module 2)** | Admission management; Student enrollment with Lab Batch/Group assignment | Students mapped to lab sessions |
| 3.3 | **Attendance (Module 5)** | Dedicated Lab/Practical attendance (batch-wise, experiment-wise); RFID/Biometric integration hooks; Attendance alerts & exemptions | Real-time lab attendance tracking |

---

### 💰 Phase 4: Finance & Asset Management

> **Focus:** The "High-Density" Accounting UI.

| # | Module | Tasks | Key Deliverables |
|---|--------|-------|-----------------|
| 4.1 | **Finance (Module 8)** | Fee Structure Management (BigDecimal for all calculations); Lab fee tracking; Scholarship & payment processing | End-to-end fee lifecycle |
| 4.2 | **Lab Inventory (Module 7.3 & 7.4)** | Equipment registration; Consumable stock tracking; Maintenance scheduling | Equipment & consumable management |
| 4.3 | **Infrastructure (Module 16)** | Cross-link lab equipment as institutional assets; Depreciation tracking; Room & resource management | Unified asset register with depreciation |

---

### 📝 Phase 5: Assessment & Reporting

> **Focus:** Exams and Accreditation.

| # | Module | Tasks | Key Deliverables |
|---|--------|-------|-----------------|
| 5.1 | **Examination (Module 6)** | Practical exam scheduling; Continuous Evaluation (experiment-wise marks); GPA/CGPA calculation with lab components | Complete assessment pipeline |
| 5.2 | **Analytics & Reports (Module 13)** | NBA/NAAC accreditation report generation; Lab Utilization KPIs & dashboards; Department-wise & program-wise analytics | Accreditation-ready reports & insights |

---

## 🛠️ Tech Stack

> For full technical standards, coding conventions, and architecture guidelines see **[docs/TECHNICAL_STANDARDS.md](docs/TECHNICAL_STANDARDS.md)**.

| Layer | Technology |
|-------|-----------|
| **Frontend** | Angular 21 (Standalone Components, Signals) |
| **UI Design System** | Angular Material 21 / Material 3 (M3) |
| **State Management** | Angular Signals + Signals (for async streams) |
| **Theming** | Tailwind CSS Variables via `mat.theme()` Sass mixins (Light & Dark mode) |
| **Backend** | Java 21 + Spring Boot 3.x (Virtual Threads / Project Loom) |
| **Identity & Auth** | Keycloak (OAuth 2.0 / OpenID Connect), Stateless JWT with Spring Security |
| **Database** | PostgreSQL (DECIMAL/NUMERIC for monetary values) |
| **Validation** | Jakarta Bean Validation |
| **Error Handling** | Global `@ControllerAdvice` exception handler |
| **Data Integrity** | BigDecimal for all currency calculations |
| **CI/CD** | GitHub Actions |
| **Containerization** | Docker, Kubernetes |

---

## 🏁 Getting Started

### Prerequisites

- Git
- Java 21 (JDK)
- Node.js 20+ & Angular CLI
- PostgreSQL
- Docker & Docker Compose (for Keycloak and containerized deployment)

### Installation

```bash
# Clone the repository
git clone https://github.com/rasterwebapps/CollegeManagementSystem.git

# Navigate to the project directory
cd CollegeManagementSystem

# Start Keycloak (Identity Provider)
docker compose up -d keycloak

# Backend — build & run
cd backend
./gradlew bootRun

# Frontend — install dependencies & run
cd ../frontend
npm install
ng serve
```

---

## 🤝 Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines, coding standards, and the contribution workflow.

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

## 📞 Contact

- **Repository:** [rasterwebapps/CollegeManagementSystem](https://github.com/rasterwebapps/CollegeManagementSystem)
- **Issues:** [GitHub Issues](https://github.com/rasterwebapps/CollegeManagementSystem/issues)

---

> **Built with ❤️ for Educational Institutions**
