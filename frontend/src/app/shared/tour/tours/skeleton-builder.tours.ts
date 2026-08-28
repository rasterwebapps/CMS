import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Skeleton Builder
// ─────────────────────────────────────────────────────────────────────────────
export const SKELETON_BUILDER_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🗓️ Skeleton Builder',
        description:
          'Run Automation places every Theory, Lab, and Clinical session for a cohort\'s whole term into the weekly grid at once — faculty and rooms are assigned afterward in Staffing. You edit the result, not build it cell by cell.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-skel-toolbar',
      popover: {
        title: 'Pick a Term and Cohort',
        description: 'Select the academic year, term, and cohort whose skeleton you want to build — or "All cohorts…" to run automation for the whole term at once.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-skel-grid',
      popover: {
        title: 'The Weekly Grid',
        description:
          'Drag a placed session to move it, or click it to remove it — automation fills the shortfall on its next run, never touching what you\'ve already placed. Cells color-code by subject and show Theory/Lab/Clinical and staffed status.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Automate, then fine-tune',
        description:
          'Use Run Automation to place and staff a cohort\'s (or every cohort\'s) shortfall in one shot, Set up Rotation for week-parity batches, or Place Elective Block to bulk-place a term\'s elective group.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const SKELETON_BUILDER_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Faculty Availability', description: 'Record which faculty are unavailable for which periods before building the timetable.' },
    { label: 'Faculty Workload Rules', description: 'Set weekly/daily/continuous teaching-load caps used to validate staffing.' },
    { label: 'Skeleton Builder', description: 'Automate Theory/Lab/Clinical session placement into periods across a cohort\'s term.' },
    { label: 'Staffing', description: 'Assign faculty to each placed session and pick theory rooms.' },
    { label: 'Capacity Planner', description: 'Work out how many classrooms and lab/clinical batches a cohort needs, and commit the physical rooms.' },
    { label: 'Conflict Inspector', description: 'Scan the whole term for room, faculty, and workload conflicts before publishing.' },
    { label: 'Timetable Draft Review', description: 'Review the generated draft timetable before it goes live.' },
    { label: 'Timetable', description: 'The published, live timetable for the term.' },
  ],
  currentIndex: 2,
  steps: [
    { label: 'Pick a Term & Cohort', icon: 'search', detail: 'Select the academic year, term, and cohort whose skeleton you\'re building — or all cohorts at once.' },
    { label: 'Run Automation', icon: 'send', detail: 'Places and staffs the shortfall in one shot, checking prerequisites and faculty capacity first.' },
    { label: 'Edit the Result', icon: 'open', detail: 'Drag a session to move it, or click it to remove it — rerun automation to refill any shortfall.' },
    { label: 'Fine-Tune', icon: 'checklist', detail: 'Use Set up Rotation for week-parity batches, or Place Elective Block for a term\'s elective group.' },
  ],
};
