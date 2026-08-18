import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Timetable (published, browse view)
// ─────────────────────────────────────────────────────────────────────────────
export const TIMETABLE_VIEW_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📆 Timetable',
        description: 'Browse the published theory and lab timetable for a term — the live, approved schedule.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-tt-toolbar',
      popover: {
        title: 'Term, View, and Filters',
        description:
          'Pick a term, switch between Week / Month / Day views, and filter by faculty, room, or batch to focus on what matters.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-tt-view',
      popover: {
        title: 'The Timetable',
        description:
          'Week shows a real weekly grid, Month shows a calendar of session-days, and Day shows a full agenda for one date — with room relocation available where permitted.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Read-only, always current',
        description: 'This reflects whatever was last approved in Timetable Draft Review — changes there appear here automatically.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const TIMETABLE_VIEW_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Faculty Availability', description: 'Record which faculty are unavailable for which periods before building the timetable.' },
    { label: 'Faculty Workload Rules', description: 'Set weekly/daily/continuous teaching-load caps used to validate staffing.' },
    { label: 'Skeleton Builder', description: 'Place Theory/Lab/Clinical sessions into periods for each subject across a cohort\'s term.' },
    { label: 'Staffing', description: 'Assign faculty to each placed session and pick theory rooms.' },
    { label: 'Capacity Planner', description: 'Work out how many classrooms and lab/clinical batches a cohort needs, and commit the physical rooms.' },
    { label: 'Conflict Inspector', description: 'Scan the whole term for room, faculty, and workload conflicts before publishing.' },
    { label: 'Timetable Draft Review', description: 'Review the generated draft timetable before it goes live.' },
    { label: 'Timetable', description: 'The published, live timetable for the term.' },
  ],
  currentIndex: 7,
  steps: [
    { label: 'Term, View & Filters', icon: 'search', detail: 'Pick a term, switch Week/Month/Day views, and filter by faculty, room, or batch.' },
    { label: 'Browse the Grid', icon: 'checklist', detail: 'Week/Month/Day views of the published, approved schedule.' },
    { label: 'Relocate a Room', icon: 'open', detail: 'Where permitted, relocate a session\'s room directly from the Day view.' },
    { label: 'Always Current', icon: 'send', detail: 'Reflects whatever was last approved in Timetable Draft Review, automatically.' },
  ],
};
