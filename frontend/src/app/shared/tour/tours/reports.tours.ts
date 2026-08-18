import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// General Reports
// ─────────────────────────────────────────────────────────────────────────────
export const REPORTS_DASHBOARD_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📊 Reports & Analytics',
        description: 'Lab utilization, equipment status, and attendance insights, all in one read-only dashboard.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-rpt-lab',
      popover: {
        title: 'Lab Utilization',
        description: 'Total labs, schedules, and equipment, with breakdowns of equipment and lab status.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-rpt-attendance',
      popover: {
        title: 'Attendance Analytics',
        description: 'Total students and attendance records, broken down by status and type.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const REPORTS_DASHBOARD_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'General Reports', description: 'Lab utilization, equipment status, and attendance insights.' }],
  currentIndex: 0,
  steps: [
    { label: 'Lab Utilization', icon: 'checklist', detail: 'Total labs, schedules, and equipment by status.' },
    { label: 'Attendance Analytics', icon: 'checklist', detail: 'Total students and records, by status and type.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Fee Reports
// ─────────────────────────────────────────────────────────────────────────────
export const FEE_REPORTS_DASHBOARD_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '💵 Fee Reports',
        description: 'Outstanding fees, collection summaries, and per-student ledgers — three tabs, each scoped to a term (or student).',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-fr-tabs',
      popover: {
        title: 'Three Tabs',
        description: 'Outstanding Fees lists every unpaid/partial demand for a term. Collection Summary rolls that up by program. Student Ledger shows one student\'s full fee/payment history.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Export & Print',
        description: 'Once a report is loaded, CSV export and Print are available for that tab\'s current view.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const FEE_REPORTS_DASHBOARD_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Fee Reports', description: 'Outstanding fees, collection summaries, and student ledgers.' }],
  currentIndex: 0,
  steps: [
    { label: 'Pick a Tab & Term', icon: 'search', detail: 'Outstanding Fees, Collection Summary (both term-scoped), or Student Ledger (by student ID).' },
    { label: 'Load Report', icon: 'checklist', detail: 'Fetches the data for the selected term or student.' },
    { label: 'Review', icon: 'open', detail: 'Table of demands, summary by program, or a full per-term ledger with payments.' },
    { label: 'Export / Print', icon: 'send', detail: 'CSV export and Print are available once a report is loaded.' },
  ],
};
