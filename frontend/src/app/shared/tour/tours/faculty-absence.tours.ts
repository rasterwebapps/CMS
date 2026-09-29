import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Faculty Absence
// ─────────────────────────────────────────────────────────────────────────────
export const FACULTY_ABSENCE_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🚫 Faculty Absence',
        description: 'See every faculty absence on record, filter by date/faculty/substitute status, and mark a new one.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-fa-add-btn',
      popover: {
        title: 'Mark Absent',
        description: 'Pick the faculty member and the date they\'ll be absent, add an optional reason, then Mark Absent — opens in a side panel over this list.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-fa-list-table',
      popover: {
        title: 'Affected Sessions & Substitutes',
        description: 'Each row shows how many of that faculty\'s published sessions are covered. Open a row\'s substitute status to see every affected session — sessions already covered show who\'s substituting, and uncovered ones let you find an eligible substitute.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

// Standalone day-to-day action, not a pipeline stage — single-entry funnel
// per the README's guidance (no rail, Flow Map only).
export const FACULTY_ABSENCE_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Faculty Absence', description: 'Mark a faculty member absent and arrange a substitute for their affected sessions.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Mark Absent', icon: 'search', detail: 'Pick the faculty member and date, add an optional reason.' },
    { label: 'Review Affected Sessions', icon: 'checklist', detail: 'See every published session that faculty was teaching that day.' },
    { label: 'Find Substitute', icon: 'open', detail: 'For an uncovered session, list only faculty who are actually free and eligible for that exact slot.' },
    { label: 'Apply', icon: 'send', detail: 'Pick a candidate to cover the session — applied immediately for that date only.' },
  ],
};
