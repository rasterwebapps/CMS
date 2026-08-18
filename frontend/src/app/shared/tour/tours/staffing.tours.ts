import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Staffing
// ─────────────────────────────────────────────────────────────────────────────
export const STAFFING_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👩‍🏫 Timetable Staffing',
        description:
          'Assign faculty and a room to every session placed in the Skeleton Builder — this term\'s draft can\'t be approved until every session is staffed.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-staff-toolbar',
      popover: {
        title: 'Pick a Term',
        description: 'Select the academic year and term whose unstaffed sessions you want to work through.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-staff-list',
      popover: {
        title: 'Unstaffed Sessions',
        description:
          'Each row is one placed session still missing a faculty (and, for Theory, a room). Lab/Clinical rooms are already fixed from Capacity Planner and shown read-only here.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Assign or auto-staff',
        description:
          'Pick eligible faculty and (for Theory) a room, then Assign. Use Auto-staff Remaining for a quick first pass across the whole term.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const STAFFING_FLOW_MAP: TourFlowMap = {
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
  currentIndex: 3,
  steps: [
    { label: 'Pick a Term', icon: 'search', detail: 'Select the academic year and term whose unstaffed sessions you want to work through.' },
    { label: 'Review Unstaffed Sessions', icon: 'checklist', detail: 'Each row is a placed session still missing a faculty, and for Theory, a room.' },
    { label: 'Pick Faculty & Room', icon: 'open', detail: 'Choose eligible faculty; Theory rooms are picked here, Lab/Clinical rooms are already fixed from Capacity Planner.' },
    { label: 'Assign or Auto-Staff', icon: 'send', detail: 'Assign row by row, or use Auto-staff Remaining for a quick first pass across the term.' },
  ],
};
