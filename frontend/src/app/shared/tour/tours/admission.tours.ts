import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Admission List
// ─────────────────────────────────────────────────────────────────────────────
export const ADMISSION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🎓 Welcome to Admissions',
        description:
          'This screen lists every admission record. Admissions are created automatically when an enquiry is completed from the Complete Admission screen.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-adm-toolbar',
      popover: {
        title: 'Search & Columns',
        description:
          'Search for admissions by student name and choose which columns are visible to focus on what matters today.',
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
          'You\'re ready. Open an existing admission to drill down into its details, fees, and documents.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Admission List — Flow Map
// Admission Explorer isn't a pipeline stage of its own (it's the browse/search
// screen over records the pipeline already created), so it gets a single-entry
// funnel per the README's guidance for screens with no natural multi-screen journey.
// ─────────────────────────────────────────────────────────────────────────────
export const ADMISSION_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Admission Explorer', description: 'Browse and drill into every admission record created from completed enquiries.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search & Columns', icon: 'search', detail: 'Search for admissions by student name and choose which columns are visible.' },
    { label: 'Browse Records', icon: 'open', detail: 'Click any row to open the full admission profile — fees, documents, and academic placement.' },
    { label: 'Review Tabs', icon: 'checklist', detail: 'Switch between Application, Fees, and Documents tabs for a focused view of each aspect.' },
    { label: 'Edit', icon: 'send', detail: 'Use Edit to update the academic year, programme, or other administrative fields.' },
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
          'All admissions flow through the enquiry process. Select a pending enquiry to auto-fill candidate details, complete the form, and create the student record.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-adm-form-body',
      popover: {
        title: 'Admission Details',
        description:
          'Enter the academic year, programme, year of study, batch, and any other admission-specific information needed to enrol the student.',
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
// Admission Form — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const ADMISSION_FORM_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Admission Explorer', description: 'Browse and drill into every admission record created from completed enquiries.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Select Enquiry', icon: 'search', detail: 'Select a pending enquiry to auto-fill the candidate\'s details.' },
    { label: 'Admission Details', icon: 'checklist', detail: 'Enter the academic year, programme, year of study, batch, and other admission-specific information.' },
    { label: 'Save', icon: 'send', detail: 'On save, the admission is created and the candidate becomes a student — roll number can be assigned later.' },
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

// ─────────────────────────────────────────────────────────────────────────────
// Admission Detail — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const ADMISSION_DETAIL_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Admission Explorer', description: 'Browse and drill into every admission record created from completed enquiries.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Identity & Actions', icon: 'checklist', detail: 'Student name plus the Edit button to update the academic year, programme, or other administrative fields.' },
    { label: 'Sectioned Tabs', icon: 'open', detail: 'Switch between Application, Fees, and Documents for a focused view of each aspect.' },
    { label: 'Edit', icon: 'send', detail: 'Use Edit to update the admission record as circumstances change.' },
  ],
};

