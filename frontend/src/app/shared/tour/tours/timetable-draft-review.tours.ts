import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Timetable Draft Review
// ─────────────────────────────────────────────────────────────────────────────
export const TIMETABLE_DRAFT_REVIEW_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📝 Timetable Draft Review',
        description:
          'Review the draft built via Skeleton Builder and Staffing as a real week grid, swap sessions if something\'s wrong, and approve it once it looks right.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-tdr-toolbar',
      popover: {
        title: 'Pick a Term',
        description: 'Select the academic year and term whose draft you want to review.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-tdr-grid',
      popover: {
        title: 'Week Grid',
        description:
          'The full draft as a real weekly timetable. Click a session to start a swap — highlighted cells show where it could move — or use Approve/Discard/Revert for the whole draft.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Approve when clean',
        description:
          'Run the Conflict Inspector first if you\'re unsure — Draft Review lets you swap and fix issues, then Approve makes the timetable live.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const TIMETABLE_DRAFT_REVIEW_FLOW_MAP: TourFlowMap = {
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
  currentIndex: 6,
  steps: [
    { label: 'Pick a Term', icon: 'search', detail: 'Select the academic year and term whose draft you want to review.' },
    { label: 'Review the Grid', icon: 'checklist', detail: 'The full draft laid out as a real weekly timetable.' },
    { label: 'Swap if Needed', icon: 'open', detail: 'Click a session to start a swap — highlighted cells show valid destinations.' },
    { label: 'Approve', icon: 'send', detail: 'Approve to publish the draft as the live timetable, or Discard/Revert to undo it.' },
  ],
};
