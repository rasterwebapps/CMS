import { TourDefinition, TourFlowMap } from '../tour.service';

// Day-to-day timetable operations group (nav-config.ts, after the build pipeline) —
// shared by Resource Timetable, Faculty Absence, Staff Session Swap, My Special
// Classes, and Special Class Approvals.
export const TIMETABLE_OPERATIONS_FUNNEL = [
  { label: 'Resource Timetable', description: 'See every faculty or room\'s schedule for one day at a glance.' },
  { label: 'Faculty Absence', description: 'Mark a faculty member absent and arrange a substitute for their affected sessions.' },
  { label: 'Staff Session Swap', description: 'Swap a session\'s assigned faculty with another eligible faculty member.' },
  { label: 'My Special Classes', description: 'Request or track extra classes outside the regular timetable.' },
  { label: 'Special Class Approvals', description: 'Approve or reject faculty requests for special classes.' },
];

// ─────────────────────────────────────────────────────────────────────────────
// Resource Timetable
// ─────────────────────────────────────────────────────────────────────────────
export const RESOURCE_TIMETABLE_GRID_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🗂️ Resource Timetable',
        description:
          'See every faculty member\'s schedule, or every classroom/lab/clinical venue\'s schedule, for one day at once — a resource-first view instead of a cohort-first one.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-resgrid-toolbar',
      popover: {
        title: 'Term, Resource Type, and Day',
        description:
          'Pick a term, switch between Faculty and Classroom/Lab/Clinical, and choose Date (a specific day) or Weekday (planning, before dates are fixed).',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-resgrid-grid',
      popover: {
        title: 'Resource Grid',
        description:
          'Each row is one faculty member or room; each column is a time slot. A chip shows the subject and (depending on view) the room or faculty teaching it.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Spot gaps and clashes at a glance',
        description: 'Use this to check a room or faculty member\'s full day before manually scheduling something around them.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const RESOURCE_TIMETABLE_GRID_FLOW_MAP: TourFlowMap = {
  funnel: TIMETABLE_OPERATIONS_FUNNEL,
  currentIndex: 0,
  steps: [
    { label: 'Term, Resource & Day', icon: 'search', detail: 'Pick a term, switch Faculty/Classroom view, and choose a specific date or planning weekday.' },
    { label: 'Resource Grid', icon: 'checklist', detail: 'Rows are faculty or rooms, columns are time slots, chips show what\'s scheduled.' },
    { label: 'Spot Gaps', icon: 'open', detail: 'Check a resource\'s full day before scheduling something else around it.' },
  ],
};
