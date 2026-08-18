import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Faculty Availability
// ─────────────────────────────────────────────────────────────────────────────
export const FACULTY_AVAILABILITY_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🚫 Faculty Availability',
        description:
          'Mark when a faculty member is unavailable — recurring or one-off — so the timetable engine steers around it when generating, regenerating, or swapping sessions.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-facavail-toolbar',
      popover: {
        title: 'Pick a Faculty Member',
        description: 'Select the faculty member whose availability you want to view or edit.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-facavail-grid',
      popover: {
        title: 'Weekly Grid',
        description:
          'Click any day/period cell to toggle it between Free and Blocked. Blocked cells are excluded when the timetable engine places or moves sessions for this faculty member.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Keep it current',
        description:
          'Update this whenever a faculty member\'s regular availability changes — Skeleton Builder, Staffing, and Swap all respect these blocks.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const FACULTY_AVAILABILITY_FLOW_MAP: TourFlowMap = {
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
  currentIndex: 0,
  steps: [
    { label: 'Pick a Faculty Member', icon: 'search', detail: 'Select the faculty member whose availability you want to view or edit.' },
    { label: 'Toggle Cells', icon: 'checklist', detail: 'Click any day/period cell to mark it Free or Blocked.' },
    { label: 'Auto-Respected', icon: 'send', detail: 'Blocked cells are excluded automatically by Skeleton Builder, Staffing, and Swap.' },
  ],
};
