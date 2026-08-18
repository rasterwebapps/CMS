import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Conflict Inspector
// ─────────────────────────────────────────────────────────────────────────────
export const CONFLICT_INSPECTOR_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🩺 Conflict Inspector',
        description:
          'A whole-term scan for every structural violation — room, faculty, and workload conflicts — using the same checks that must all be clean before a term can be published.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-ci-toolbar',
      popover: {
        title: 'Pick a Term',
        description: 'Select the academic year and term to scan, then Rescan to re-run the checks after making changes elsewhere.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-ci-summary',
      popover: {
        title: 'Scan Summary',
        description: 'Total sessions scanned, how many have violations, and the overall violation count — clean means ready to publish.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-ci-table',
      popover: {
        title: 'Violations Table',
        description:
          'Each row is a session with at least one problem — day/period, subject, faculty, venue, and the specific violation messages.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Fix and rescan',
        description:
          'Fix flagged sessions in Skeleton Builder or Staffing, then come back and Rescan — the term can\'t be published until this list is empty.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const CONFLICT_INSPECTOR_FLOW_MAP: TourFlowMap = {
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
  currentIndex: 5,
  steps: [
    { label: 'Pick a Term & Rescan', icon: 'search', detail: 'Select the academic year and term to scan, then Rescan after making changes.' },
    { label: 'Scan Summary', icon: 'checklist', detail: 'Total sessions scanned, sessions with violations, and total violation count.' },
    { label: 'Review Violations', icon: 'open', detail: 'Each row shows a session\'s day/period, subject, faculty, venue, and specific violations.' },
    { label: 'Fix and Rescan', icon: 'send', detail: 'Fix flagged sessions in Skeleton Builder or Staffing, then rescan until the list is clean.' },
  ],
};
