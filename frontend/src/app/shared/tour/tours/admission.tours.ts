import { TourDefinition } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Admission List
// ─────────────────────────────────────────────────────────────────────────────
export const ADMISSION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🎓 Welcome to Admissions',
        description:
          'This screen lists every admission record — both confirmed and in-progress applications. Use it to track the academic onboarding pipeline.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-adm-add-btn',
      popover: {
        title: 'New Admission',
        description:
          'Create an admission directly or convert an existing enquiry. Click here to start a new admission record from scratch or pick from the enquiry pool.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-adm-toolbar',
      popover: {
        title: 'Filters & Columns',
        description:
          'Filter admissions by status, choose which columns are visible, and refine the view to focus on what matters today.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-adm-content',
      popover: {
        title: 'Admission Records',
        description:
          'Click any row to open the full admission profile — review fees, documents, and academic placement, or move it through the workflow.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All set',
        description:
          'You\'re ready. Open an existing admission to drill down or create a new one to enrol a candidate.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Admission Form
// ─────────────────────────────────────────────────────────────────────────────
export const ADMISSION_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📝 New / Edit Admission',
        description:
          'Create an admission either from an existing enquiry (recommended) or by entering the details from scratch.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-adm-form-mode',
      popover: {
        title: 'Choose a Mode',
        description:
          '<strong>From Enquiry</strong> pulls candidate details from a registered enquiry — fastest and most accurate. <strong>Manual</strong> lets you key in everything from scratch.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-adm-form-body',
      popover: {
        title: 'Admission Details',
        description:
          'Enter the academic year, programme, semester, batch, and any other admission-specific information needed to enrol the student.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Save the record',
        description:
          'On save, the admission is created and the candidate becomes a student. Roll number can be assigned later from the Roll Number Assignment screen.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Admission Detail
// ─────────────────────────────────────────────────────────────────────────────
export const ADMISSION_DETAIL_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📑 Admission Profile',
        description:
          'A complete view of one admission — application info, fees, documents, and academic placement, all on one page.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-adm-detail-header',
      popover: {
        title: 'Identity & Actions',
        description:
          'Student name and the Edit button. Use Edit to update the academic year, programme, or other administrative fields.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-adm-detail-tabs',
      popover: {
        title: 'Sectioned Tabs',
        description:
          'Switch between Application, Fees, and Documents. Each tab gives you a focused view of that aspect of the admission.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All set',
        description:
          'You can now navigate every aspect of an admission record. Use the tabs to drill into fees or documents as needed.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

