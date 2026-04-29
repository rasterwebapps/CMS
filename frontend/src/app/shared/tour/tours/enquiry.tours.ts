import { TourDefinition } from '../tour.service';

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
// Document Submission List
// ─────────────────────────────────────────────────────────────────────────────
export const DOCUMENT_SUBMISSION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📂 Document Submission',
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
          'Once all mandatory documents are verified, the candidate appears in <em>Admission Completion</em>, where you can finalise their record.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Admission Completion List
// ─────────────────────────────────────────────────────────────────────────────
export const ADMISSION_COMPLETION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🎓 Admission Completion',
        description:
          'The final step in the admission workflow. Candidates here have paid and submitted documents — they\'re ready to be promoted to active students.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-adcomp-stats',
      popover: {
        title: 'Ready-to-Admit Counters',
        description:
          'See at a glance how many candidates are ready for admission completion across each programme.',
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
          'Fill in the academic year, semester, batch, and any other admission-specific fields needed to enrol the student.',
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

