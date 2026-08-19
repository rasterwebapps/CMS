import { TourDefinition, TourFlowMap } from '../tour.service';

// Same 6-stage Admission Management pipeline as FEE_COLLECTION_FLOW_MAP below
// (duplicated rather than imported cross-file, matching how each tours.ts file
// stays self-contained).
const ADMISSION_PIPELINE_FUNNEL = [
  { label: 'Enquiries', description: 'Track interest, follow up, and convert promising enquiries into admissions.' },
  { label: 'Finalize Fee', description: 'Set the final fee amount for each enquiry before payment can begin.' },
  { label: 'Collect Payment', description: 'Record payments from enquiries and students, installment by installment.' },
  { label: 'Submit Documents', description: 'Collect proof of identity, transcripts, and certificates once a candidate has paid.' },
  { label: 'Verify Documents', description: 'Review and approve submitted documents before admission can be completed.' },
  { label: 'Complete Admission', description: 'Finalize paid, verified candidates into enrolled students with a roll number.' },
];


// ─────────────────────────────────────────────────────────────────────────────
// Fee Collection
// ─────────────────────────────────────────────────────────────────────────────
export const FEE_COLLECTION_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🧭 Take A Tour',
        description:
          'Record payments from enquiries and students. Track installments, due dates, and payment modes (cash, cheque, UPI, bank transfer, etc.) all in one place.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-feecol-toolbar',
      popover: {
        title: 'Filter by Status & Type',
        description:
          'Filter by payment status (All / Overdue / Outstanding) or by person type (Enquiries / Students) to focus on urgent collections.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-feecol-search',
      popover: {
        title: 'Quick Search',
        description:
          'Search for a student or enquiry by name, roll number, or program. Results update instantly as you type.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-feecol-table',
      popover: {
        title: 'Select a Record',
        description:
          'Click any row to open a detailed payment form for that person. You\'ll see their fee breakdown by installment and all previous payments.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Ready to collect',
        description:
          'Pick a person from the list to start recording their payment. The payment form shows their remaining balance and due dates.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Fee Finalization
// ─────────────────────────────────────────────────────────────────────────────
export const FEE_FINALIZATION_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🔒 Finalize Fees',
        description:
          'Set the final fee amount for each enquiry before admission completion. You can apply discounts and confirm the amount each candidate will pay.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-feefinal-header',
      popover: {
        title: 'Global Summary',
        description:
          'See the total number of pending enquiries ready for fee finalization at a glance.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-feefinal-filters',
      popover: {
        title: 'Filters & Search',
        description:
          'Filter by program, quota (Management/Counselling), or academic year. Search by candidate name or program to quickly find who needs fee finalization.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-feefinal-table',
      popover: {
        title: 'Enquiry List',
        description:
          'Each row shows a candidate ready for fee finalization. Click to open their profile where you can adjust fees and apply discounts.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Confirm finalization',
        description:
          'After setting the final fee, candidates move to Complete Admission where they can become enrolled students.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Fee Finalization — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const FEE_FINALIZATION_FLOW_MAP: TourFlowMap = {
  funnel: ADMISSION_PIPELINE_FUNNEL,
  currentIndex: 1,
  steps: [
    { label: 'Global Summary', icon: 'checklist', detail: 'See the total number of pending enquiries ready for fee finalization at a glance.' },
    { label: 'Filters & Search', icon: 'search', detail: 'Filter by program, quota (Management/Counselling), or academic year; search by candidate name or program.' },
    { label: 'Enquiry List', icon: 'open', detail: 'Click a candidate to open their profile where you can adjust fees and apply discounts.' },
    { label: 'Confirm Finalization', icon: 'send', detail: 'After setting the final fee, the candidate moves to Complete Admission.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Collect Balance (Payment Detail)
// ─────────────────────────────────────────────────────────────────────────────
export const COLLECT_BALANCE_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '💳 Collect Balance',
        description:
          'This form shows the fee breakdown for the selected person. Review their installments and record the payment details.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-collect-summary',
      popover: {
        title: 'Installment Table',
        description:
          'View each installment with its due date, fee amount, paid amount, and outstanding balance. The status badge shows whether each installment is paid, partial, overdue, or pending.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-collect-form',
      popover: {
        title: 'Payment Form',
        description:
          'Enter the collection amount, payment date, payment mode (cash, cheque, UPI, etc.), and any additional remarks. Cash payments can be broken down by denomination.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Record payment',
        description:
          'Once submitted, the payment is recorded and a receipt can be printed or downloaded. The outstanding balance will update automatically.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Collect Payment — Flow Map (Take a Tour, second view)
// Funnel labels/order and the "current" position mirror the Admission Management
// nav group (AD) in nav-config.ts; the six steps are the real Fee Collection /
// Collect Balance workflow already described in FEE_COLLECTION_TOUR and
// COLLECT_BALANCE_TOUR above, restated as flowchart nodes instead of prose.
// ─────────────────────────────────────────────────────────────────────────────
export const FEE_COLLECTION_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Enquiries', description: 'Track interest, follow up, and convert promising enquiries into admissions.' },
    { label: 'Finalize Fee', description: 'Set the final fee amount for each enquiry before payment can begin.' },
    { label: 'Collect Payment', description: 'Record payments from enquiries and students, installment by installment.' },
    { label: 'Submit Documents', description: 'Collect proof of identity, transcripts, and certificates once a candidate has paid.' },
    { label: 'Verify Documents', description: 'Review and approve submitted documents before admission can be completed.' },
    { label: 'Complete Admission', description: 'Finalize paid, verified candidates into enrolled students with a roll number.' },
  ],
  currentIndex: 2,
  steps: [
    { label: 'Filter / Search', icon: 'search', detail: 'Filter by status (All / Overdue / Outstanding) or type (Enquiries / Students), or use Quick Search to find the person paying.' },
    { label: 'Open Record', icon: 'open', detail: 'Click their row to open the payment form for that person.' },
    { label: 'Review Installments', icon: 'checklist', detail: 'Check due date, fee amount, already paid, and outstanding balance — each installment shows a paid / partial / overdue / pending badge.' },
    { label: 'Enter Payment', icon: 'payment', detail: 'Enter the collection amount, payment date, and payment mode (cash, cheque, UPI, bank transfer). Cash can be broken down by denomination.' },
    { label: 'Submit', icon: 'send', detail: 'Add remarks if needed, then submit. The outstanding balance updates automatically.' },
    { label: 'Receipt', icon: 'receipt', detail: 'The payment is recorded and a receipt is ready to print or download immediately.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Receipts List
// ─────────────────────────────────────────────────────────────────────────────
export const RECEIPTS_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Payment Receipts',
        description:
          'Access all issued payment receipts in one place. Search, filter, and reprint receipts for payments collected from enquiries and students.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-receipts-toolbar',
      popover: {
        title: 'Search & Filter',
        description:
          'Search by receipt number, payer name, or payment date. Use the filters to focus on specific payment modes or receipt types.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-receipts-table',
      popover: {
        title: 'Receipt Records',
        description:
          'Each row shows a payment receipt with payer name, amount, payment mode, and date. Click the print or download icon to generate a hardcopy or PDF.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Manage receipts',
        description:
          'Use this screen to audit all payments made and generate receipt documentation for records, refunds, or customer inquiries.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Receipts List — Flow Map
// Standalone audit/reporting tool, not a pipeline stage — the Finance nav
// group's screens are independent of each other, so this gets a single-entry
// funnel per the README's guidance (no rail, Flow Map only).
// ─────────────────────────────────────────────────────────────────────────────
export const RECEIPTS_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Receipts', description: 'All issued payment receipts, searchable and reprintable.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search & Filter', icon: 'search', detail: 'Search by receipt number, payer name, or payment date; filter by payment mode or receipt type.' },
    { label: 'Receipt Records', icon: 'checklist', detail: 'Each row shows a payment receipt with payer name, amount, mode, and date.' },
    { label: 'Print / Download', icon: 'receipt', detail: 'Click the print or download icon to generate a hardcopy or PDF.' },
    { label: 'Audit Payments', icon: 'send', detail: 'Use this screen to audit all payments made and support refund or enquiry documentation.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Fee Refund List
// ─────────────────────────────────────────────────────────────────────────────
export const FEE_REFUND_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '↩️ Fee Refunds',
        description:
          'Manage refund requests from students and enquiries. Review pending requests, approve or reject them, and restore outstanding balances.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-refund-toolbar',
      popover: {
        title: 'Search & Status Filter',
        description:
          'Search for refund requests by student name or receipt number. Filter by status (Pending / Approved / Rejected) to prioritize your workflow.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-refund-table',
      popover: {
        title: 'Refund Requests',
        description:
          'Each row is a refund request showing the original receipt, amount, requestor, and current status. Click any row to view the full details and take action.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Approve or Reject',
        description:
          'Open a request and choose to approve (restoring the outstanding balance) or reject (keeping the payment recorded). Approved refunds generate a refund voucher.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Fee Refund List — Flow Map
// Standalone audit/reporting tool, not a pipeline stage — single-entry funnel
// per the README's guidance (no rail, Flow Map only).
// ─────────────────────────────────────────────────────────────────────────────
export const FEE_REFUND_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Refunds', description: 'Refund requests from students and enquiries, pending review and approval.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search & Status Filter', icon: 'search', detail: 'Search by student name or receipt number; filter by status (Pending / Approved / Rejected).' },
    { label: 'Refund Requests', icon: 'checklist', detail: 'Each row is a refund request showing the original receipt, amount, requestor, and status.' },
    { label: 'Review Details', icon: 'open', detail: 'Open a request to view the full details before deciding.' },
    { label: 'Approve or Reject', icon: 'send', detail: 'Approve to restore the outstanding balance, or reject to keep the payment recorded. Approved refunds generate a voucher.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Fee Explorer (Student Fees)
// ─────────────────────────────────────────────────────────────────────────────
export const FEE_EXPLORER_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🔍 Student Fees Explorer',
        description:
          'Get a high-level view of all student fee allocations. See payment status, outstanding balances, and penalties at a glance.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-explorer-toolbar',
      popover: {
        title: 'Search Students',
        description:
          'Search by student name or roll number to quickly find a student. The results update instantly as you type.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-explorer-table',
      popover: {
        title: 'Fee Summary Table',
        description:
          'Each row shows a student with their total fee, amount paid, outstanding balance, and any penalties. Click a row to open detailed fee history and payment options.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Drill into details',
        description:
          'Click any student to see their complete installment breakdown, payment history, and available actions (collect balance, process refund, etc.).',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Fee Explorer — Flow Map
// Standalone audit/reporting tool, not a pipeline stage — single-entry funnel
// per the README's guidance (no rail, Flow Map only).
// ─────────────────────────────────────────────────────────────────────────────
export const FEE_EXPLORER_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Fee Explorer', description: 'High-level view of every student fee allocation, payment status, and outstanding balance.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search Students', icon: 'search', detail: 'Search by student name or roll number — results update instantly.' },
    { label: 'Fee Summary Table', icon: 'checklist', detail: 'Each row shows total fee, amount paid, outstanding balance, and any penalties.' },
    { label: 'Drill Into Details', icon: 'open', detail: 'Click a student to see their complete installment breakdown and payment history.' },
    { label: 'Take Action', icon: 'send', detail: 'Collect balance or process a refund directly from the detail view.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Student Fee Detail
// ─────────────────────────────────────────────────────────────────────────────
export const STUDENT_FEE_DETAIL_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '💰 Student Fee Collection',
        description:
          'Complete view of a student\'s fee allocation, installment schedule, payment history, and all available collection actions.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-student-summary',
      popover: {
        title: 'Student & Summary Stats',
        description:
          'View the student name, roll number, program, and quick stats: total fee, amount paid, outstanding balance, and any penalties incurred.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-student-installments',
      popover: {
        title: 'Installment Schedule',
        description:
          'See all installments with their due dates, allocated amounts, paid amounts, and status (Paid / Partial / Overdue / Pending). Click a row to drill deeper.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-student-history',
      popover: {
        title: 'Payment History & Receipts',
        description:
          'Review all past payments grouped by receipt, showing date, mode, reference, and amount. Use the print/download icons to retrieve receipt documents.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Take action',
        description:
          'Use the "Collect Balance" button to record new payments or the "Request Refund" option to begin a refund workflow.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};
