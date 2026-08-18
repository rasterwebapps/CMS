import { TourDefinition, TourFlowMap } from '../tour.service';

// Timetable-build group of the Academics nav (nav-config.ts, in nav order).
// Reused across every timetable-planning screen's Flow Map as they get built
// in Phase 2 — only Capacity Planner uses it so far (its own Tour already
// existed pre-rollout).
export const TIMETABLE_BUILD_FUNNEL = [
  { label: 'Faculty Availability', description: 'Record which faculty are unavailable for which periods before building the timetable.' },
  { label: 'Faculty Workload Rules', description: 'Set weekly/daily/continuous teaching-load caps used to validate staffing.' },
  { label: 'Skeleton Builder', description: 'Place Theory/Lab/Clinical sessions into periods for each subject across a cohort\'s term.' },
  { label: 'Staffing', description: 'Assign faculty to each placed session and pick theory rooms.' },
  { label: 'Capacity Planner', description: 'Work out how many classrooms and lab/clinical batches a cohort needs, and commit the physical rooms.' },
  { label: 'Conflict Inspector', description: 'Scan the whole term for room, faculty, and workload conflicts before publishing.' },
  { label: 'Timetable Draft Review', description: 'Review the generated draft timetable before it goes live.' },
  { label: 'Timetable', description: 'The published, live timetable for the term.' },
];

// ─────────────────────────────────────────────────────────────────────────────
// Capacity Planner
// ─────────────────────────────────────────────────────────────────────────────
export const CAPACITY_PLANNER_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📐 Plan a Term\'s Timetable Capacity',
        description:
          'Work out how many classrooms and lab/clinical batches a cohort actually needs — before building its timetable — and commit the physical rooms Skeleton Builder and Staffing will build against.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-cap-toolbar',
      popover: {
        title: 'Pick a Term and Cohort',
        description:
          'Choose the term and cohort, and whether to plan against enrolled headcount or sanctioned intake (useful in a first term where enrollment is still rolling in), then Calculate.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cap-allocation',
      popover: {
        title: 'Theory Sections',
        description:
          'If no single classroom fits the whole cohort, you\'ll be walked through splitting it into sections, each with its own classroom.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-cap-allocation',
      popover: {
        title: 'Lab & Clinical Batches',
        description:
          'Pick a subject with Lab/Clinical hours and a default venue — one row auto-generates per section; split a row if a venue can\'t seat the whole section — then Commit.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Next: build the timetable',
        description:
          'Open Skeleton Builder to place Theory/Lab/Clinical sessions into periods for each subject, then Staffing to assign faculty — Theory rooms are picked there, but Lab/Clinical rooms are already fixed from here and can\'t be changed in Staffing.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Capacity Planner — Flow Map
// ─────────────────────────────────────────────────────────────────────────────
export const CAPACITY_PLANNER_FLOW_MAP: TourFlowMap = {
  funnel: TIMETABLE_BUILD_FUNNEL,
  currentIndex: 4,
  steps: [
    { label: 'Pick Term & Cohort', icon: 'search', detail: 'Choose the term and cohort, and whether to plan against enrolled headcount or sanctioned intake, then Calculate.' },
    { label: 'Theory Sections', icon: 'checklist', detail: 'If no single classroom fits the whole cohort, split it into sections, each with its own classroom.' },
    { label: 'Lab & Clinical Batches', icon: 'open', detail: 'Pick a subject with Lab/Clinical hours and a default venue — one row auto-generates per section; split a row if needed.' },
    { label: 'Commit', icon: 'send', detail: 'Commit the physical rooms — Skeleton Builder and Staffing build against them next.' },
  ],
};
