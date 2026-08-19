import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Faculty Absence
// ─────────────────────────────────────────────────────────────────────────────
export const FACULTY_ABSENCE_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🚫 Faculty Absence',
        description: 'Mark a faculty member absent for a date, see every published session it affects, and line up a substitute.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-fa-form',
      popover: {
        title: 'Mark Absent',
        description: 'Pick the faculty member and the date they\'ll be absent, add an optional reason, then Mark Absent.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-fa-sessions',
      popover: {
        title: 'Affected Sessions',
        description: 'Every published session that faculty was teaching that day. Sessions already covered show who\'s substituting.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Find a Substitute',
        description: 'For each uncovered session, Find Substitute lists only faculty who are actually free and eligible for that exact slot — pick one to apply it immediately.',
        side: 'over',
        align: 'center',
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
