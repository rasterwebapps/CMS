import { TourDefinition } from '../tour.service';

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
          'Roll number, admission date, name, email, programme, and semester are the core fields used across fees, attendance, and exams.',
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
          'Name, programme, semester, and current status badge — confirms you\'re viewing the right student.',
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

