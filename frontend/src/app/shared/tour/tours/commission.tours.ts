import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Commission Explorer
// ─────────────────────────────────────────────────────────────────────────────
export const COMMISSION_EXPLORER_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🤝 Commission Explorer',
        description:
          'Track referral commissions owed to agents and staff referrers — from pending, through approval, to payout.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-comm-stats',
      popover: {
        title: 'Summary Stats',
        description:
          'Total commission due, how much has been paid out, what\'s still outstanding, and how many are awaiting approval.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-comm-toolbar',
      popover: {
        title: 'Search & Filter',
        description:
          'Search by student, referrer, or admission number. Filter by date range, status, or referral source.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-comm-table',
      popover: {
        title: 'Commission Records',
        description:
          'Each row shows the total, paid, and outstanding commission amount, with its current status. Expand a row to see enquiry details and full payout history.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Approve, reject, or settle',
        description:
          'Pending commissions can be approved or rejected. Once approved (and outside OneBook auto-transmission), record a payout to settle it in cash or another mode.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// Standalone audit/reporting tool, not a pipeline stage — single-entry funnel
// per the README's guidance (no rail, Flow Map only).
export const COMMISSION_EXPLORER_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Commissions', description: 'Agent commission tracking on converted admissions.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search & Filter', icon: 'search', detail: 'Search by student, referrer, or admission number; filter by date range, status, or referral source.' },
    { label: 'Review Record', icon: 'open', detail: 'Expand a row to see enquiry details, OneBook integration status, and the full payout history.' },
    { label: 'Approve or Reject', icon: 'checklist', detail: 'Pending commissions can be approved (sent to OneBook, if enabled) or rejected with a reason.' },
    { label: 'Record Payout', icon: 'payment', detail: 'Once approved and outside OneBook auto-transmission, record a cash/other-mode payout to settle it.' },
  ],
};
