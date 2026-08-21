import { TourDefinition, TourFlowMap } from '../tour.service';

// The Admission Management pipeline (nav-config.ts "Admission Management" group,
// first 6 items) — same funnel used by fee-collection's FEE_COLLECTION_FLOW_MAP.
const ADMISSION_PIPELINE_FUNNEL = [
  { label: 'Enquiries', description: 'Track interest, follow up, and convert promising enquiries into admissions.' },
  { label: 'Finalize Fee', description: 'Set the final fee amount for each enquiry before payment can begin.' },
  { label: 'Collect Payment', description: 'Record payments from enquiries and students, installment by installment.' },
  { label: 'Submit Documents', description: 'Collect proof of identity, transcripts, and certificates once a candidate has paid.' },
  { label: 'Verify Documents', description: 'Review and approve submitted documents before admission can be completed.' },
  { label: 'Complete Admission', description: 'Finalize paid, verified candidates into enrolled students with a roll number.' },
];

// ─────────────────────────────────────────────────────────────────────────────
// Enquiry List
// ─────────────────────────────────────────────────────────────────────────────
export const ENQUIRY_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Student Enquiries',
        description:
          'This is the entry point for every prospective student. Track interest, follow up, and convert promising enquiries into admissions.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-enq-header',
      popover: {
        title: 'Pipeline at a Glance',
        description:
          'See the total number of enquiries plus how many are in the pipeline, marked interested, or already admitted. These counters update live.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-enq-add-btn',
      popover: {
        title: 'Add a New Enquiry',
        description:
          'Click here to register a walk-in or telephone enquiry. You\'ll capture the candidate\'s contact, programme interest, and referral source.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-enq-toolbar',
      popover: {
        title: 'Search & Filter',
        description:
          'Filter by status, search by name/phone/email, or switch between card and table views. Your view choice is remembered for next time.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-enq-content',
      popover: {
        title: 'Enquiry Records',
        description:
          'Click any row or card to open the full enquiry — view history, capture follow-up notes, collect payment, or convert to an admission.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ You\'re ready!',
        description:
          'Start by adding a new enquiry or opening an existing one. Re-launch this tour any time from the info icon next to the title.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Enquiry List — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const ENQUIRY_LIST_FLOW_MAP: TourFlowMap = {
  funnel: ADMISSION_PIPELINE_FUNNEL,
  currentIndex: 0,
  steps: [
    { label: 'Pipeline Counters', icon: 'checklist', detail: 'See total enquiries plus how many are in the pipeline, marked interested, or already admitted — counters update live.' },
    { label: 'Add Enquiry', icon: 'open', detail: 'Register a walk-in or telephone enquiry, capturing contact info, programme interest, and referral source.' },
    { label: 'Search & Filter', icon: 'search', detail: 'Filter by status, search by name/phone/email, or switch between card and table views.' },
    { label: 'Open & Convert', icon: 'send', detail: 'Click a row to view history, capture follow-up notes, collect payment, or convert the enquiry into an admission.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Enquiry Form
// ─────────────────────────────────────────────────────────────────────────────
export const ENQUIRY_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 New / Edit Enquiry',
        description:
          'Capture every detail about a prospective student — personal info, programme interest, referral source, and fee adjustments — in one place.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-enq-form-basic',
      popover: {
        title: 'Candidate Basics',
        description:
          'Name, phone, and email are the bare minimum to follow up. Phone is most important — most candidates respond faster via call than email.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-enq-form-programme',
      popover: {
        title: 'Programme & Course',
        description:
          'Select the programme the candidate is interested in. The course list and base fee will load automatically based on this choice.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-enq-form-referral',
      popover: {
        title: 'Referral & Agent',
        description:
          'Track where the enquiry came from. If an agent referred the candidate, select them here so commissions can be calculated automatically.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Save & continue',
        description:
          'Click <strong>Save</strong> in the top-right to register this enquiry. You can always return later to update notes, take payments, or convert it.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Enquiry Form — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const ENQUIRY_FORM_FLOW_MAP: TourFlowMap = {
  funnel: ADMISSION_PIPELINE_FUNNEL,
  currentIndex: 0,
  steps: [
    { label: 'Candidate Basics', icon: 'checklist', detail: 'Name, phone, and email — phone matters most since most candidates respond faster via call than email.' },
    { label: 'Programme & Course', icon: 'open', detail: 'Select the programme of interest — the course list and base fee load automatically from this choice.' },
    { label: 'Referral & Agent', icon: 'checklist', detail: 'Track where the enquiry came from; select an agent if referred so commissions calculate automatically.' },
    { label: 'Save', icon: 'send', detail: 'Save to register the enquiry — return anytime to update notes, take payments, or convert it.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Enquiry Detail
// ─────────────────────────────────────────────────────────────────────────────
export const ENQUIRY_DETAIL_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👤 Enquiry Profile',
        description:
          'A complete view of one prospective student — personal info, status history, payments, documents, and conversion options.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-enq-detail-hero',
      popover: {
        title: 'Candidate Identity',
        description:
          'Name, programme, course, status badge, and referral source — everything you need to recognise the candidate at a glance.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-enq-detail-actions',
      popover: {
        title: 'Quick Actions',
        description:
          'Update status, capture follow-up notes, collect a fee payment, or convert this enquiry into a confirmed admission — all from here.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-enq-detail-tabs',
      popover: {
        title: 'Tabbed Sections',
        description:
          'Switch between profile details, status history, payment records, and uploaded documents. Each tab loads its data on demand.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All set',
        description:
          'You now know how to navigate an enquiry profile. Use the action buttons to move the candidate through the admission workflow.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Enquiry Detail — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const ENQUIRY_DETAIL_FLOW_MAP: TourFlowMap = {
  funnel: ADMISSION_PIPELINE_FUNNEL,
  currentIndex: 0,
  steps: [
    { label: 'Candidate Identity', icon: 'checklist', detail: 'Name, programme, course, status badge, and referral source, at a glance.' },
    { label: 'Quick Actions', icon: 'open', detail: 'Update status, capture follow-up notes, collect a fee payment, or convert to an admission.' },
    { label: 'Tabbed Sections', icon: 'checklist', detail: 'Switch between profile details, status history, payment records, and uploaded documents.' },
    { label: 'Convert', icon: 'send', detail: 'Move the candidate forward through the admission workflow using the action buttons.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Submit Documents List
// ─────────────────────────────────────────────────────────────────────────────
export const DOCUMENT_SUBMISSION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📂 Submit Documents',
        description:
          'After candidates pay (fully or partially), the next step is collecting their documents. This screen lists everyone awaiting verification.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-docsub-header',
      popover: {
        title: 'Collection Queue',
        description:
          'Counters show how many candidates are pending, fully paid, and partially paid. Use this to prioritise document collection.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-docsub-toolbar',
      popover: {
        title: 'Filter & Search',
        description:
          'Narrow down by payment status or search by candidate name to quickly find the file you need.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-docsub-content',
      popover: {
        title: 'Open a Candidate File',
        description:
          'Click any row to open the document collection screen for that candidate — you can then upload, verify, and approve each document.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Ready to verify',
        description:
          'Pick a candidate from the list and start collecting their proof of identity, transcripts, and certificates.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Submit Documents List — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const DOCUMENT_SUBMISSION_LIST_FLOW_MAP: TourFlowMap = {
  funnel: ADMISSION_PIPELINE_FUNNEL,
  currentIndex: 3,
  steps: [
    { label: 'Collection Queue', icon: 'checklist', detail: 'Counters show how many candidates are pending, fully paid, and partially paid — use this to prioritise document collection.' },
    { label: 'Filter & Search', icon: 'search', detail: 'Narrow down by payment status or search by candidate name to find the right file quickly.' },
    { label: 'Open Candidate File', icon: 'open', detail: 'Click a row to open the document collection screen for that candidate.' },
    { label: 'Upload & Verify', icon: 'send', detail: 'Upload, verify, and approve each required document — proof of identity, transcripts, and certificates.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Document Collection (per-candidate)
// ─────────────────────────────────────────────────────────────────────────────
export const DOCUMENT_COLLECTION_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📎 Collect Documents',
        description:
          'Upload, preview, and verify each required document for this candidate. Once all mandatory documents are approved, the admission can proceed.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-doccol-banner',
      popover: {
        title: 'Candidate Banner',
        description:
          'Confirms you\'re working on the right person — name, programme, course, and overall payment status are all visible up top.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-doccol-list',
      popover: {
        title: 'Document Checklist',
        description:
          'Each item shows the required document, its current status (Pending / Submitted / Verified), and an upload button. Mandatory items are marked.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Move to admission',
        description:
          'Once all mandatory documents are verified, the candidate appears in <em>Complete Admission</em>, where you can finalise their record.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Document Collection (per-candidate) — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const DOCUMENT_COLLECTION_FLOW_MAP: TourFlowMap = {
  funnel: ADMISSION_PIPELINE_FUNNEL,
  currentIndex: 3,
  steps: [
    { label: 'Candidate Banner', icon: 'checklist', detail: 'Confirms you\'re working on the right person — name, programme, course, and overall payment status.' },
    { label: 'Document Checklist', icon: 'open', detail: 'Each item shows the required document, its status (Pending / Submitted / Verified), and an upload button. Mandatory items are marked.' },
    { label: 'Upload & Verify', icon: 'send', detail: 'Upload each document — once all mandatory documents are verified, the candidate moves toward Complete Admission.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Complete Admission List
// ─────────────────────────────────────────────────────────────────────────────
export const ADMISSION_COMPLETION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🎓 Complete Admission',
        description:
          'The final step in the admission workflow. Candidates here have paid and completed document verification — they\'re ready to be admitted as active students.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-adcomp-stats',
      popover: {
        title: 'Ready-to-Admit Counters',
        description:
          'See at a glance how many candidates are ready for complete admission across each programme.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-adcomp-table',
      popover: {
        title: 'Candidate Queue',
        description:
          'Click any row to open the candidate, review their documents and payment, then complete the admission to generate their student record.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Final step',
        description:
          'Pick a candidate from the queue and complete their admission. They\'ll appear in the Students screen with a roll number ready to be assigned.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Complete Admission List — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const ADMISSION_COMPLETION_LIST_FLOW_MAP: TourFlowMap = {
  funnel: ADMISSION_PIPELINE_FUNNEL,
  currentIndex: 5,
  steps: [
    { label: 'Ready-to-Admit Counters', icon: 'checklist', detail: 'See at a glance how many candidates are ready for complete admission across each programme.' },
    { label: 'Candidate Queue', icon: 'open', detail: 'Click any row to open the candidate and review their documents and payment status.' },
    { label: 'Complete Admission', icon: 'send', detail: 'Finalize the admission to generate the student record.' },
    { label: 'Assign Roll Number', icon: 'receipt', detail: 'The new student appears in Students, ready to have a roll number assigned.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Enquiry → Admission Conversion
// ─────────────────────────────────────────────────────────────────────────────
export const ENQUIRY_CONVERT_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🔄 Convert Enquiry → Admission',
        description:
          'This screen turns a confirmed enquiry into an active admission record. Review the summary and confirm the details before submitting.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-conv-summary',
      popover: {
        title: 'Enquiry Summary',
        description:
          'Verify the candidate, programme, course, and final fee one last time. Anything wrong here? Cancel and edit the enquiry first.',
        side: 'right',
        align: 'start',
      },
    },
    {
      element: '#tour-conv-form',
      popover: {
        title: 'Admission Details',
        description:
          'Fill in the academic year, year of study, batch, and any other admission-specific fields needed to enrol the student.',
        side: 'left',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Confirm conversion',
        description:
          'On submit, a new admission record is created and the enquiry status moves to <strong>Admitted</strong>. The candidate then appears in Students.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Enquiry → Admission Conversion — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const ENQUIRY_CONVERT_FLOW_MAP: TourFlowMap = {
  funnel: ADMISSION_PIPELINE_FUNNEL,
  currentIndex: 0,
  steps: [
    { label: 'Enquiry Summary', icon: 'checklist', detail: 'Verify the candidate, programme, course, and final fee one last time before converting.' },
    { label: 'Admission Details', icon: 'open', detail: 'Fill in the academic year, year of study, batch, and other admission-specific fields.' },
    { label: 'Confirm Conversion', icon: 'send', detail: 'On submit, a new admission record is created and the enquiry moves to Admitted.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Document Verification List — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const DOCUMENT_VERIFICATION_LIST_FLOW_MAP: TourFlowMap = {
  funnel: ADMISSION_PIPELINE_FUNNEL,
  currentIndex: 4,
  steps: [
    { label: 'Filter & Search', icon: 'search', detail: 'Filter by program or course, or search by candidate name to locate documents awaiting verification.' },
    { label: 'Verification Queue', icon: 'checklist', detail: 'Each row is a candidate pending verification — click to open their profile.' },
    { label: 'Review & Approve', icon: 'open', detail: 'Review each submitted document and approve or reject it.' },
    { label: 'Auto-Advance', icon: 'send', detail: 'Once all of a candidate\'s documents are verified, they automatically appear in Complete Admission.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Document Verification List
// ─────────────────────────────────────────────────────────────────────────────
export const DOCUMENT_VERIFICATION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '✅ Verify Documents',
        description:
          'Review and approve all submitted documents from candidates. Only candidates with fully verified documentation can complete their admission.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-docverif-toolbar',
      popover: {
        title: 'Filter & Search',
        description:
          'Filter by program or course, search by candidate name to quickly locate documents awaiting verification.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-docverif-content',
      popover: {
        title: 'Verification Queue',
        description:
          'Each row shows a candidate pending verification. Click to open their profile and review/approve each document they submitted.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All set',
        description:
          'Once all candidate documents are verified, they\'ll automatically appear in <em>Complete Admission</em> to finalize enrollment.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};
