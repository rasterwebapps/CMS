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
          'Place every Theory, Lab, and Clinical session for a cohort\'s whole term into the weekly grid at once — faculty and rooms are assigned afterward in Staffing.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-skel-toolbar',
      popover: {
        title: 'Pick a Term and Cohort',
        description: 'Select the academic year, term, and cohort whose skeleton you want to build.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-skel-subjects',
      popover: {
        title: 'Subject Rail',
        description:
          'Click a subject to make it active, then place its sessions in the grid. Each subject shows its weekly-session budget and how many are already placed, per session type/batch/section.',
        side: 'right',
        align: 'start',
      },
    },
    {
      element: '#tour-skel-grid',
      popover: {
        title: 'The Weekly Grid',
        description:
          'Click + on a day/period cell to place the active subject\'s next session there, or drag an existing session to move it. Cells color-code by subject and show Theory/Lab/Clinical and staffed status.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Auto-place or fine-tune',
        description:
          'Use Auto-place Remaining for a quick first pass, Set up Rotation for week-parity batches, or Place Elective Block to bulk-place a term\'s elective group in one go.',
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
    { label: 'Skeleton Builder', description: 'Place Theory/Lab/Clinical sessions into periods for each subject across a cohort\'s term.' },
    { label: 'Staffing', description: 'Assign faculty to each placed session and pick theory rooms.' },
    { label: 'Capacity Planner', description: 'Work out how many classrooms and lab/clinical batches a cohort needs, and commit the physical rooms.' },
    { label: 'Conflict Inspector', description: 'Scan the whole term for room, faculty, and workload conflicts before publishing.' },
    { label: 'Timetable Draft Review', description: 'Review the generated draft timetable before it goes live.' },
    { label: 'Timetable', description: 'The published, live timetable for the term.' },
  ],
  currentIndex: 2,
  steps: [
    { label: 'Pick a Term & Cohort', icon: 'search', detail: 'Select the academic year, term, and cohort whose skeleton you\'re building.' },
    { label: 'Select a Subject', icon: 'checklist', detail: 'Click a subject in the rail to make it active — its weekly-session budget shows what\'s still needed.' },
    { label: 'Place Sessions', icon: 'open', detail: 'Click + on a grid cell to place a session, or drag existing sessions to move them.' },
    { label: 'Auto-Place or Fine-Tune', icon: 'send', detail: 'Use Auto-place Remaining, Set up Rotation, or Place Elective Block for bulk placement.' },
  ],
};
