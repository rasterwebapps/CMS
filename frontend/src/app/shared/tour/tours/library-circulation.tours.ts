import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Issue Books
// ─────────────────────────────────────────────────────────────────────────────
export const LIBRARY_ISSUE_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📖 Issue Book / Journal',
        description: 'Look up an available book or journal by accession number or barcode, and issue it to a student or faculty member.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-libissue-item',
      popover: {
        title: 'Item Lookup',
        description: 'Scan or type an accession number/barcode and press Enter (or Look Up). The item must be Available — its title and shelf location confirm you have the right one.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-libissue-borrower',
      popover: {
        title: 'Borrower',
        description: 'Pick Student or Faculty, then select who\'s borrowing it and the issue date.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Issue',
        description: 'Issuing computes the due date automatically from Library Settings\' loan period for that borrower type.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// Standalone transaction screen, not a pipeline stage — single-entry funnel
// per the README's guidance (no rail, Flow Map only).
export const LIBRARY_ISSUE_FORM_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Issue Books', description: 'Look up an available book or journal and issue it to a student or faculty member.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Look Up Item', icon: 'search', detail: 'Scan or type an accession number/barcode; the item must be Available.' },
    { label: 'Pick Borrower', icon: 'checklist', detail: 'Student or Faculty, then who, and the issue date.' },
    { label: 'Issue', icon: 'send', detail: 'Due date is computed automatically from Library Settings\' loan period.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Issue Explorer
// ─────────────────────────────────────────────────────────────────────────────
export const LIBRARY_ISSUE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📚 Issue Register',
        description: 'Every book/journal issue, return, and renewal — search, filter, scan-to-return, or issue a new item.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-libiss-scan',
      popover: {
        title: 'Scan to Return',
        description: 'Scan or type an accession number and it finds and returns that active issue in one step — no need to search the table first.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-libiss-toolbar',
      popover: {
        title: 'Search & Filters',
        description: 'Search by title, accession number, or member; filter by item type, status, or member type.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-libiss-table',
      popover: {
        title: 'Issues',
        description: 'Overdue rows are highlighted. Renew or Return from the row actions — renewal is capped at 2 times per issue.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

// Standalone tracking screen, not a pipeline stage — single-entry funnel per
// the README's guidance (no rail, Flow Map only).
export const LIBRARY_ISSUE_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Issue Explorer', description: 'Track every issue, return, and renewal — scan-to-return in one motion.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Scan to Return', icon: 'search', detail: 'Scan an accession number to find and return that issue directly.' },
    { label: 'Search & Filter', icon: 'checklist', detail: 'By title, accession number, member, item type, status, or member type.' },
    { label: 'Renew or Return', icon: 'open', detail: 'From each row — renewal capped at 2 times per issue.' },
    { label: 'Issue New', icon: 'send', detail: 'Issue Book / Journal to start a new circulation.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Overdue Books
// ─────────────────────────────────────────────────────────────────────────────
export const LIBRARY_REPORTS_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '⏰ Overdue Books',
        description: 'Every issue past its due date and not yet returned, with the estimated fine.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-libover-toolbar',
      popover: {
        title: 'Search & Filter',
        description: 'Search by title, accession number, or borrower; filter by student or faculty.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-libover-table',
      popover: {
        title: 'Overdue Issues',
        description: 'Days overdue and estimated fine per issue — return or collect the fine from Issue Explorer or Fines.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

// Standalone report screen, not a pipeline stage — single-entry funnel per
// the README's guidance (no rail, Flow Map only).
export const LIBRARY_REPORTS_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Overdue Books', description: 'Every issue past its due date and not yet returned.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search & Filter', icon: 'search', detail: 'By title, accession number, borrower, or member type.' },
    { label: 'Review', icon: 'checklist', detail: 'Days overdue and estimated fine per issue.' },
    { label: 'Act', icon: 'send', detail: 'Return the item from Issue Explorer, or collect/waive its fine from Fines.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Fines
// ─────────────────────────────────────────────────────────────────────────────
export const LIBRARY_FINES_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '💰 Fine Management',
        description: 'View, waive, and collect overdue fines generated from late returns.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-libfine-toolbar',
      popover: {
        title: 'Search & Filter',
        description: 'Search by title, accession number, or member; filter by fine status or member type.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-libfine-table',
      popover: {
        title: 'Fines',
        description: 'Each row shows overdue days, the per-day rate, and total. Pending fines can be marked Collected or Waived from the row actions.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

// Standalone management screen, not a pipeline stage — single-entry funnel
// per the README's guidance (no rail, Flow Map only).
export const LIBRARY_FINES_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Fines', description: 'View, waive, and collect overdue fines.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search & Filter', icon: 'search', detail: 'By title, accession number, member, fine status, or member type.' },
    { label: 'Review Fines', icon: 'checklist', detail: 'Overdue days, per-day rate, and total per fine.' },
    { label: 'Collect or Waive', icon: 'payment', detail: 'Resolve a pending fine from its row actions.' },
  ],
};
