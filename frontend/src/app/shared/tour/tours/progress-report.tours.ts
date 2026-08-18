import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Subject Progress Report
// ─────────────────────────────────────────────────────────────────────────────
export const PROGRESS_REPORT_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📈 Subject Progress Report',
        description: 'Unit-wise syllabus coverage across every subject in a term — how much has actually been taught against the plan.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-pr-toolbar',
      popover: {
        title: 'Academic Year & Term',
        description: 'Pick the term whose progress you want to review.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-pr-list',
      popover: {
        title: 'Subject Progress',
        description: 'Each subject shows units covered out of total units. Click a subject to expand its unit-by-unit breakdown, with hours logged and dates covered.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Planned vs Projected',
        description: 'Admin/HOD can generate a portion-completion blueprint per subject — it freezes planned completion dates from the current timetable, then shows each unit as on-track, ahead, or behind against that plan.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const PROGRESS_REPORT_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Progress Report', description: 'Unit-wise syllabus coverage across every subject in a term.' }],
  currentIndex: 0,
  steps: [
    { label: 'Pick a Term', icon: 'search', detail: 'Choose the academic year and term to review.' },
    { label: 'Overall & Per-Subject Progress', icon: 'checklist', detail: 'See the term\'s overall coverage, then each subject\'s units-covered count.' },
    { label: 'Expand a Subject', icon: 'open', detail: 'View unit-by-unit status, hours logged, and dates covered.' },
    { label: 'Blueprint Variance', icon: 'send', detail: 'Admin/HOD can generate a planned-completion blueprint and see on-track/ahead/behind per unit.' },
  ],
};
