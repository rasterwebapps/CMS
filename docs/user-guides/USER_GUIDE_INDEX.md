# College Management System - User Guide Index

## Version 1.0 | April 27, 2026

---

## Quick Navigation

### Find Your Role-Specific Guide

**Are you a...?**

- [College Admin](#college-admin-guide) - Manage academic structure, fees, admissions, reports
- [Front Office Staff](#front-office-guide) - Manage student inquiries and admission workflow
- [Cashier/Finance](#cashier-guide) - Collect payments and maintain payment records
- [Administrator](#administrator-notes) - System configuration and role management

---

## Available User Guides

### College Admin Guide

**File**: [`COLLEGE_ADMIN_USER_GUIDE.md`](./COLLEGE_ADMIN_USER_GUIDE.md)

**Who should read this**: College Administrators, Academic Coordinators

**Key Topics**:
- Setting up academic calendar, programs, courses
- Managing departments, faculty, agents
- Fee structure configuration
- Complete admission workflow management
- Finance oversight (fee finalization, payment tracking)
- Administrative reporting

**Time to Review**: 1-2 hours

**Primary Responsibilities**:
- Configure academic structure (departments, programs, courses, semesters)
- Define fee structures for programs
- Finalize student fees
- Oversee complete admission process
- Manage faculty and support staff
- Generate strategic reports

**Key Menu Items**:
```
Preferences
  ├─ Departments
  ├─ Programs
  ├─ Courses
  ├─ Academic Years
  ├─ Semesters
  ├─ Fee Structures
  ├─ Faculty
  ├─ Agents
  └─ Referral Types

Admission Management
  ├─ Enquiries
  ├─ Document Submission
  ├─ Admission Completion
  ├─ Admissions
  └─ Students

Finance
  ├─ Student Fees (Finalize)
  └─ Fee Payments (Collect)

Reports
```

**Most Common Tasks**:
1. Finalize fees for students
2. Collect payments (via shortcuts)
3. Create admissions
4. Generate reports

---

### Front Office Guide

**File**: [`FRONT_OFFICE_USER_GUIDE.md`](./FRONT_OFFICE_USER_GUIDE.md)

**Who should read this**: Front Office Staff, Admissions Coordinators, Student Liaison Officers

**Key Topics**:
- Creating and managing student inquiries
- Collecting student payments
- Document collection and verification
- Admission creation
- Student communication
- Report generation

**Time to Review**: 1 hour

**Primary Responsibilities**:
- First point of contact for prospective students
- Record student inquiries
- Guide students through admission workflow
- Collect payments
- Collect and verify documents
- Create admissions for eligible students
- Generate admission reports

**Key Menu Items**:
```
Admission Management
  ├─ Enquiries
  ├─ Document Submission
  ├─ Admission Completion
  └─ Admissions (View)

Reports
  ├─ Admissions Report
  └─ Collection Report
```

**Most Common Tasks**:
1. Create new enquiries for prospective students
2. Collect payments from students
3. Upload and verify documents
4. Create admissions from completed enquiries
5. Track student progress
6. Generate admission reports

**Not Allowed**:
- Cannot configure fees (College Admin only)
- Cannot finalize fees (College Admin only)
- Cannot access Preferences section

---

### Cashier Guide

**File**: [`CASHIER_USER_GUIDE.md`](./CASHIER_USER_GUIDE.md)

**Who should read this**: Cashiers, Finance Staff, Accountants

**Key Topics**:
- Payment collection from students
- Multiple payment methods (cash, check, online, card)
- Receipt generation and tracking
- Daily reconciliation
- Payment reporting
- Banking and cash handling

**Time to Review**: 45 minutes

**Primary Responsibilities**:
- Collect all student payments
- Process multiple payment methods
- Generate payment receipts
- Maintain payment records
- Reconcile daily collections
- Prepare bank deposits
- Track outstanding balances

**Key Menu Items**:
```
Admission Management
  ├─ Enquiries (View payments and shortcuts)
  └─ Payment Recording

Reports
  ├─ Daily Collection Report
  ├─ Weekly Collection Report
  ├─ Monthly Collection Report
  ├─ Receipt Report
  └─ Outstanding Balance Report
```

**Most Common Tasks**:
1. Record student payments (multiple methods)
2. Generate and issue payment receipts
3. Daily cash reconciliation
4. Tracking outstanding balances
5. Weekly bank deposits
6. Collection reporting

**Not Allowed**:
- Cannot configure fees
- Cannot finalize fees
- Cannot create admissions
- Cannot submit documents
- Cannot access Preferences or Finance configuration

---

## General Features Available to All Users

### Authentication & Login
- Login with Keycloak credentials
- Support for single sign-on
- Role-based access control
- Automatic token refresh

### Dashboard & Navigation
- Personalized dashboard per role
- Responsive sidebar navigation
- Search functionality
- Keyboard shortcuts (Ctrl+K to search)

### Common Actions
- Create new records (as per role)
- Search and filter lists
- Export to Excel/PDF
- Generate reports
- Print documents/receipts

### Reporting
- Standard reports available to all roles
- Filter by date range, program, student
- Export and email functionality
- Print-ready formatting

---

## Workflow Summary: Complete Admission Process

The following shows how different roles interact:

```
STEP 1: ENQUIRY CREATION [Front Office]
├─ Create new enquiry
├─ Record student details
└─ Status: ENQUIRED

STEP 2: INTEREST CONFIRMATION [Front Office]
├─ Student confirms interest
├─ Update status to INTERESTED
└─ Notify College Admin for fee finalization

STEP 3: FEE FINALIZATION [College Admin]
├─ Determine fee amount for student
├─ Apply scholarships/discounts if applicable
├─ Finalize fees
└─ Status: FEES_FINALIZED

STEP 4: PAYMENT COLLECTION [Cashier/Front Office/College Admin]
├─ Student pays (full or partial)
├─ Record payment in system
├─ Issue receipt
└─ Status: FEES_PAID (or PARTIALLY_PAID)

STEP 5: DOCUMENT COLLECTION [Front Office/College Admin]
├─ Collect 5 mandatory documents
├─ Verify documents
├─ Upload verified documents
└─ Status: DOCUMENTS_SUBMITTED

STEP 6: ADMISSION CREATION [Front Office/College Admin]
├─ Create admission from enquiry
├─ Generate new Student ID
└─ Status: ADMITTED

STEP 7: STUDENT ENROLLMENT [College Admin]
└─ Student can now enroll in courses
```

---

## Access Control Matrix

| Feature | College Admin | Front Office | Cashier |
|---------|:---:|:---:|:---:|
| **Create Enquiry** | ✅ | ✅ | ❌ |
| **View Enquiries** | ✅ | ✅ | ✅ (read-only) |
| **Update Enquiry Status** | ✅ | ✅ | ❌ |
| **Finalize Fees** | ✅ | ❌ | ❌ |
| **Collect Payments** | ✅ | ✅ | ✅ |
| **Submit Documents** | ✅ | ✅ | ❌ |
| **Create Admissions** | ✅ | ✅ | ❌ |
| **Configure Programs** | ✅ | ❌ | ❌ |
| **Configure Fee Structures** | ✅ | ❌ | ❌ |
| **View Reports** | ✅ | ✅ | ✅ |
| **Access Preferences** | ✅ | ❌ | ❌ |
| **Access Finance Config** | ✅ | ❌ | ❌ |

---

## Quick Reference by Task

### Task: Create New Student Enquiry
- **Best access**: Front Office
- **Also can do**: College Admin
- **Guide section**: Front Office Guide → Enquiry Management

### Task: Record Student Payment
- **Best access**: Cashier, Front Office, or College Admin
- **Guide section**: Cashier Guide → Payment Collection, or Front Office Guide → Payment Collection

### Task: Set Up New Program
- **Best access**: College Admin
- **Guide section**: College Admin Guide → Preferences & Settings → Programs

### Task: Create Admission
- **Best access**: Front Office or College Admin
- **Guide section**: Front Office Guide → Admission Creation, or College Admin Guide → Admission Management

### Task: Track Payment History
- **Best access**: Cashier or College Admin
- **Guide section**: Cashier Guide → Payment Records

### Task: Generate Reports
- **Best access**: Any role (filtered per role)
- **Guide section**: Respective role guide → Reports section

---

## Getting Help

### Before Contacting Support

1. **Check the appropriate user guide** for your role
2. **Review the Troubleshooting section** in your guide
3. **Search using Ctrl+K** in the application
4. **Contact your department supervisor**

### Support Contacts

| Issue | Contact | Info |
|-------|---------|------|
| Application Access | System Admin | [contact info] |
| Payment Processing | Finance Manager | [contact info] |
| Admission Questions | Admissions Coordinator | [contact info] |
| Account Issues | IT Support | [contact info] |
| General Questions | Your Supervisor | [contact info] |

---

## Version History & Updates

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Apr 27, 2026 | Initial release: College Admin, Front Office, Cashier guides |

### Planned Updates
- Scheduled for July 27, 2026
- Additional workflows
- Advanced reporting features
- Integration with new modules

---

## Frequently Asked Questions

### General Questions

**Q: How do I reset my password?**
A: Use the "Forgot Password" link on the Keycloak login screen. Follow email instructions to reset.

**Q: Can I access the system from my phone?**
A: Yes, the system is responsive. Open the URL in your phone browser. Login with your credentials.

**Q: What browser should I use?**
A: Chrome, Firefox, Safari, or Edge. Recommended: Chrome (latest version).

### Role-Specific Questions

**Q (Front Office): Can I finalize fees for a student?**
A: No, only College Admin can finalize fees. You can view finalized amounts and record collections.

**Q (Cashier): Can I create admissions?**
A: No, only College Admin or Front Office can create admissions. You can record payments and track payment status.

**Q (College Admin): Can I access Preferences if I have ROLE_FRONT_OFFICE in addition to ROLE_COLLEGE_ADMIN?**
A: Yes, you can access Preferences because you have ROLE_COLLEGE_ADMIN. Role permissions are additive.

### Workflow Questions

**Q: What if a student wants to pay in installments?**
A: Record each payment separately. System automatically marks as PARTIALLY_PAID. Student can make additional payments when ready.

**Q: Can admissions be created before all documents are submitted?**
A: No, enquiry must be in DOCUMENTS_SUBMITTED status. All 5 documents must be verified first.

**Q: What if we need to change finalized fees?**
A: Contact College Admin. They can adjust fee structure. Changes affect future students or exceptions can be noted.

---

## Key Concepts Glossary

| Term | Definition |
|------|------------|
| **Enquiry** | Initial registration of interest from prospective student |
| **Status** | Current position in the admission workflow |
| **Finalized Fee** | Amount determined by College Admin that student must pay |
| **Collection** | Payment received from student |
| **Document** | One of 5 mandatory files (marksheets, ID, photo) |
| **Receipt** | Proof of payment issued to student |
| **Admission** | Final acceptance of student into college |
| **Student ID** | Unique identifier for admitted student |
| **Partially Paid** | Student has paid some but not full amount |
| **Outstanding Balance** | Amount still owed by student |
| **RBAC** | Role-Based Access Control - system restricts features by user role |

---

## Tips for Success

### For College Admin
- ✅ Regularly review dashboard for pending actions
- ✅ Finalize fees promptly after student confirms interest
- ✅ Monitor collection progress
- ✅ Generate reports for strategic planning
- ⚠️ Never change fee structure mid-year without careful review

### For Front Office
- ✅ Follow up with students on payment collection
- ✅ Verify all documents before submission
- ✅ Keep accurate records of all communications
- ✅ Maintain professional courtesy with students
- ⚠️ Don't create duplicate enquiries for same student

### For Cashier
- ✅ Reconcile cash daily without fail
- ✅ Process payments accurately
- ✅ Keep secure record of checks
- ✅ Prepare timely bank deposits
- ✅ Generate daily reports for Finance Manager
- ⚠️ Never leave cash unattended
- ⚠️ Double-check payment amounts before recording

---

## Recent RBAC Enhancements (April 2026)

Recent updates have improved role-based access control:

**College Admin Now Can**:
- Collect payments directly from enquiry shortcuts
- Finalize fees from enquiry shortcuts
- Submit documents from enquiry shortcuts
- Create admissions from enquiry shortcuts

**Cashier Now Can**:
- Collect payments from enquiry shortcuts
- View enquiry payment history

**Front Office Enhancements**:
- Properly restricted from fee finalization (College Admin only)
- Full access to payment collection and document submission

For details, see: [`RBAC_FIXES_SUMMARY.md`](../RBAC_FIXES_SUMMARY.md)

---

## Feedback & Suggestions

Have feedback on these guides? Please share:
- Email: [feedback email]
- Form: [feedback form link]
- Your Name: [Optional]
- Suggestion: [Your suggestion]

We continually improve our guides based on user feedback!

---

## Appendix: Menu Navigation Quick Reference

### College Admin Full Menu
```
Dashboard
Preferences
  ├─ Departments
  ├─ Programs
  ├─ Courses
  ├─ Academic Years
  ├─ Semesters
  ├─ Fee Structures
  ├─ Faculty
  ├─ Agents
  └─ Referral Types
Admission Management
  ├─ Enquiries
  ├─ Document Submission
  ├─ Admission Completion
  ├─ Admissions
  └─ Students
Finance
  ├─ Student Fees
  └─ Fee Payments
Reports
```

### Front Office Menu
```
Dashboard
Admission Management
  ├─ Enquiries
  ├─ Document Submission
  ├─ Admission Completion
  ├─ Admissions
  └─ Students
Reports
```

### Cashier Menu
```
Dashboard
Admission Management
  └─ Enquiries (for payment shortcuts)
Reports
```

---

**Last Updated**: April 27, 2026  
**Document Version**: 1.0  
**Next Review Date**: July 27, 2026

---

## Document Information

**Purpose**: Central hub for all user guides and documentation

**Target Audience**: All roles (College Admin, Front Office, Cashier)

**Related Documents**:
- College Admin User Guide (`COLLEGE_ADMIN_USER_GUIDE.md`)
- Front Office User Guide (`FRONT_OFFICE_USER_GUIDE.md`)
- Cashier User Guide (`CASHIER_USER_GUIDE.md`)
- RBAC Implementation Summary (`../RBAC_FIXES_SUMMARY.md`)
- Manual Test Cases (`../manual-test-cases/rbac-fixes-college-admin-cashier.md`)

**Questions?** Contact your system administrator or department supervisor.

---

**Welcome to the College Management System! 🎓**

