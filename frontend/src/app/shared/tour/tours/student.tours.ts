import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Student List
// ─────────────────────────────────────────────────────────────────────────────
export const STUDENT_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👥 Students',
        description:
          'This screen lists all enrolled students across programmes. Use it to search records, open profiles, edit details, or remove incorrect entries.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-stu-add-btn',
      popover: {
        title: 'Add Student',
        description:
          'Create a new student record manually. Most admissions should flow from enquiry conversion, but this option is useful for corrections or legacy entries.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-stu-toolbar',
      popover: {
        title: 'Search & Columns',
        description:
          'Search by name, roll number, programme, or other visible values. Use Columns to show or hide fields for your current task.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-stu-table',
      popover: {
        title: 'Student Records',
        description:
          'Each row shows key academic placement details. Use the row action buttons to view, edit, or delete a student record.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Ready',
        description:
          'You can now find students quickly and open their full profiles whenever you need admission, fee, or academic details.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Student List — Flow Map
// Standalone browse/search screen, not a pipeline stage — single-entry funnel
// per the README's guidance (no rail, Flow Map only).
// ─────────────────────────────────────────────────────────────────────────────
export const STUDENT_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Student Explorer', description: 'Search, view, and manage every enrolled student record.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Add Student', icon: 'open', detail: 'Create a new student record manually — most admissions flow from enquiry conversion, but this covers corrections or legacy entries.' },
    { label: 'Search & Columns', icon: 'search', detail: 'Search by name, roll number, or programme; show or hide columns for your current task.' },
    { label: 'Browse Records', icon: 'checklist', detail: 'Each row shows key academic placement details, with actions to view, edit, or delete.' },
    { label: 'Open Profile', icon: 'send', detail: 'Open a student\'s full profile for admission, fee, or academic details.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Student Form
// ─────────────────────────────────────────────────────────────────────────────
export const STUDENT_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🧾 Student Form',
        description:
          'Create or update a student record. Capture identity, programme placement, family details, and address information in one form.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-stu-basic',
      popover: {
        title: 'Basic Information',
        description:
          'Roll number, admission date, name, email, programme, and year of study are the core fields used across fees, attendance, and exams.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-stu-personal',
      popover: {
        title: 'Personal Details',
        description:
          'Date of birth, gender, community category, caste, and blood group support statutory reports and student services.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-stu-family',
      popover: {
        title: 'Family Details',
        description:
          'Parent and guardian details are important for communications, attendance alerts, and fee follow-ups.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-stu-submit',
      popover: {
        title: 'Save Student',
        description:
          'Review required fields, then save. The button is disabled while the record is being submitted.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Student ready',
        description:
          'Once saved, this student appears in the Students list and can be used by fee, attendance, and exam modules.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Student Form — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const STUDENT_FORM_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Student Explorer', description: 'Search, view, and manage every enrolled student record.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Basic Information', icon: 'checklist', detail: 'Roll number, admission date, name, email, programme, and year of study — used across fees, attendance, and exams.' },
    { label: 'Personal Details', icon: 'open', detail: 'Date of birth, gender, community category, caste, and blood group — support statutory reports and student services.' },
    { label: 'Family Details', icon: 'checklist', detail: 'Parent and guardian details for communications, attendance alerts, and fee follow-ups.' },
    { label: 'Save', icon: 'send', detail: 'Save the record — once saved, the student appears in fee, attendance, and exam modules.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Student Detail
// ─────────────────────────────────────────────────────────────────────────────
export const STUDENT_DETAIL_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👤 Student Profile',
        description:
          'A complete academic and personal record for one student — identity, family, contact, fees, attendance, and exam history.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-stu-detail-hero',
      popover: {
        title: 'Identity & Status',
        description:
          'Name, programme, year of study, and current status badge — confirms you\'re viewing the right student.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-stu-detail-content',
      popover: {
        title: 'Detailed Sections',
        description:
          'Browse personal info, family details, address, fee history, attendance, and exam records — everything about this student in one place.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Profile loaded',
        description:
          'You now have the full picture. Edit the record from the list screen, or jump to fees / attendance using the side navigation.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Student Detail — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const STUDENT_DETAIL_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Student Explorer', description: 'Search, view, and manage every enrolled student record.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Identity & Status', icon: 'checklist', detail: 'Name, programme, year of study, and current status badge confirm you\'re viewing the right student.' },
    { label: 'Detailed Sections', icon: 'open', detail: 'Personal info, family details, address, fee history, attendance, and exam records — all in one place.' },
    { label: 'Take Action', icon: 'send', detail: 'Edit from the list screen, or jump to fees / attendance using the side navigation.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Roll Number Assignment
// ─────────────────────────────────────────────────────────────────────────────
export const ROLL_NUMBER_ASSIGNMENT_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🔢 Roll Number Assignment',
        description:
          'Bulk-assign roll numbers to students after admission. Filter by programme and course, then enter or auto-generate roll numbers in one go.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-rollno-filters',
      popover: {
        title: 'Filter Students',
        description:
          'Pick a programme and course to load only the relevant students — keeps the table focused and prevents accidental edits across batches.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-rollno-table',
      popover: {
        title: 'Edit Roll Numbers',
        description:
          'Each row shows a student. Type the new roll number directly in the cell — changes are highlighted until you save.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-rollno-save',
      popover: {
        title: 'Save All Changes',
        description:
          'When you\'re done editing, click <strong>Save All</strong> to persist every roll number in one batch. Nothing is saved until you click this.',
        side: 'left',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Done',
        description:
          'Roll numbers are now permanent on the student record and will appear on hall tickets, mark sheets, and ID cards.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Roll Number Assignment — Flow Map
// Standalone utility screen, not a pipeline stage — single-entry funnel per
// the README's guidance (no rail, Flow Map only).
// ─────────────────────────────────────────────────────────────────────────────
export const ROLL_NUMBER_ASSIGNMENT_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Assign Roll Numbers', description: 'Bulk-assign roll numbers to newly admitted students by programme and course.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Filter Students', icon: 'search', detail: 'Pick a programme and course to load only the relevant students.' },
    { label: 'Edit Roll Numbers', icon: 'checklist', detail: 'Type the new roll number directly in each cell — changes are highlighted until saved.' },
    { label: 'Save All', icon: 'send', detail: 'Click Save All to persist every roll number in one batch — nothing is saved until then.' },
    { label: 'Ready for Use', icon: 'receipt', detail: 'Roll numbers become permanent and appear on hall tickets, mark sheets, and ID cards.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Retro Admit
// ─────────────────────────────────────────────────────────────────────────────
export const RETRO_ADMIT_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🔄 Retroactive Admission',
        description:
          'Add students who were admitted in previous years but lack admission records in the system. This includes capturing historical payments and fees.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-retro-stepper',
      popover: {
        title: 'Seven-Step Workflow',
        description:
          'Follow the flow from admission context through family details, fees, and payment history. Use the steps to navigate and track progress.',
        side: 'right',
        align: 'start',
      },
    },
    {
      element: '#retro-section-0',
      popover: {
        title: '📋 Step 1: Admission Context',
        description:
          'Select the program, academic year, and quota. Set the student\'s year of study and fee state.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#retro-section-1',
      popover: {
        title: '👤 Step 2: Student Details',
        description:
          'Enter the student\'s identity — name, email, phone, roll number, and academic registration numbers (University Registration Number, UMIS).',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#retro-section-5',
      popover: {
        title: '💵 Step 6: Fee Structure',
        description:
          'Define year-wise fees. The system fetches guideline fees and lets you enter the actual fees charged to this student for each year.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#retro-section-6',
      popover: {
        title: '💰 Step 7: Payment History',
        description:
          'Add all historical payments with dates, amounts, modes (cash/cheque/UPI/etc.), and optional receipt/transaction references.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Save the admission',
        description:
          'At the end, click <strong>Save</strong> to create the student record with all historical fees and payments captured.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Retro Admit — Flow Map
// A separate, exceptional entry path (not part of the main pipeline funnel),
// so it gets a single-entry funnel per the README's guidance.
// ─────────────────────────────────────────────────────────────────────────────
export const RETRO_ADMIT_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Retro Admit', description: 'Add students admitted in previous years who lack admission records in the system, including historical fees and payments.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Admission Context', icon: 'search', detail: 'Select the program, academic year, quota, year of study, and fee state.' },
    { label: 'Student & Family Details', icon: 'checklist', detail: 'Enter identity, registration numbers, and family details across the seven-step workflow.' },
    { label: 'Fee Structure', icon: 'open', detail: 'Define year-wise fees, starting from system-fetched guideline fees.' },
    { label: 'Payment History', icon: 'payment', detail: 'Add all historical payments with dates, amounts, modes, and receipt references.' },
    { label: 'Save', icon: 'send', detail: 'Save to create the student record with all historical fees and payments captured.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Scholarship Applications
// ─────────────────────────────────────────────────────────────────────────────
export const SCHOLARSHIP_APPLICATIONS_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🎓 Scholarship Applications',
        description:
          'Review student scholarship requests waiting for approval. This screen helps you approve, reject, and monitor the final approved amount.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-schapp-header',
      popover: {
        title: 'Applications Queue',
        description:
          'The header confirms you are in the scholarship approval workspace used by authorised staff to process pending student requests.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-schapp-content',
      popover: {
        title: 'Application Review Workspace',
        description:
          'This area shows the current scholarship approval queue. Depending on data, you will see the review table, a loading state, or an empty state message for pending requests.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Ready to review',
        description:
          'Open pending applications from the table and complete the approval workflow with the correct sanctioned amount or rejection reason.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Scholarship Applications — Flow Map
// Standalone review screen, not a pipeline stage — single-entry funnel per
// the README's guidance (no rail, Flow Map only).
// ─────────────────────────────────────────────────────────────────────────────
export const SCHOLARSHIP_APPLICATIONS_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Scholarship Applications', description: 'Review and approve student scholarship requests.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Applications Queue', icon: 'checklist', detail: 'The pending scholarship approval queue for authorised staff to process.' },
    { label: 'Review Workspace', icon: 'open', detail: 'Review each request\'s details, table, or empty-state depending on current data.' },
    { label: 'Approve or Reject', icon: 'send', detail: 'Complete the approval workflow with the correct sanctioned amount or a rejection reason.' },
    { label: 'Sanctioned Amount', icon: 'receipt', detail: 'The final approved amount is recorded against the student\'s fee once sanctioned.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Student Data Import
// ─────────────────────────────────────────────────────────────────────────────
export const STUDENT_DATA_IMPORT_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📥 Student Data Import',
        description:
          'Use this workflow to migrate legacy student records, qualifications, and fee history from the Excel template into the system.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-import-header',
      popover: {
        title: 'Import Overview',
        description:
          'This header introduces the migration workspace and provides the Take a Tour action whenever users need guidance again.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-import-nav',
      popover: {
        title: 'Four-Step Workflow',
        description:
          'Move through the process in order: download the template, set default values, upload and validate the file, then review the results before import.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-import-step-template-btn',
      popover: {
        title: 'Template Download',
        description:
          'Start at the Template step to download the live workbook with required sheets, reference values, and spreadsheet validation support.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-import-step-defaults-btn',
      popover: {
        title: 'Set Defaults',
        description:
          'Use the Defaults step to prefill joining year, student type, admission category, and other fallback values for blank spreadsheet cells.',
        side: 'bottom',
        align: 'center',
      },
    },
    {
      element: '#tour-import-step-upload-btn',
      popover: {
        title: 'Upload, Validate, and Import',
        description:
          'After filling the template, move to Upload & Validate. Validate first to review issues, then run the import once the file is ready.',
        side: 'bottom',
        align: 'center',
      },
    },
    {
      element: '#tour-import-step-result-btn',
      popover: {
        title: 'Review Results',
        description:
          'The Results step summarizes imported records, skipped rows, warnings, and errors so you can confirm the migration outcome clearly.',
        side: 'bottom',
        align: 'center',
      },
    },
    {
      popover: {
        title: '✅ Import with confidence',
        description:
          'Follow the steps carefully and always validate before importing to catch spreadsheet issues early and keep student data clean.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Student Data Import — Flow Map
// Standalone utility screen, not a pipeline stage — single-entry funnel per
// the README's guidance (no rail, Flow Map only).
// ─────────────────────────────────────────────────────────────────────────────
export const STUDENT_DATA_IMPORT_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Data Import', description: 'Migrate legacy student records, qualifications, and fee history from Excel.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Download Template', icon: 'open', detail: 'Download the live workbook with required sheets, reference values, and validation support.' },
    { label: 'Set Defaults', icon: 'checklist', detail: 'Prefill joining year, student type, admission category, and other fallback values.' },
    { label: 'Upload & Validate', icon: 'search', detail: 'Upload the filled template and validate it first to review issues before importing.' },
    { label: 'Review Results', icon: 'receipt', detail: 'The Results step summarizes imported records, skipped rows, warnings, and errors.' },
  ],
};

