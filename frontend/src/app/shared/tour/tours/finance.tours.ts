import { TourDefinition } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Fee Collection
// ─────────────────────────────────────────────────────────────────────────────
export const FEE_COLLECTION_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '💰 Collect Payments',
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
