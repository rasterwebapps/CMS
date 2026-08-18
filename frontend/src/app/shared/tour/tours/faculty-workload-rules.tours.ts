import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Faculty Workload Rules
// ─────────────────────────────────────────────────────────────────────────────
export const FACULTY_WORKLOAD_RULES_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '⚖️ Faculty Workload Rules',
        description:
          'Set the institution-wide teaching-hour caps used as the last fallback when validating staffing — daily, weekly, and continuous hours.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-fwr-form',
      popover: {
        title: 'Three Caps, Three Tiers',
        description:
          'A faculty member\'s own override (on the Faculty screen) wins first, then their designation\'s default, then these institution-wide values — the last, catch-all fallback tier.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Save the rules',
        description: 'Leave a field blank for no fallback cap on that dimension. Save applies the new caps to future staffing validation immediately.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const FACULTY_WORKLOAD_RULES_FLOW_MAP: TourFlowMap = {
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
  currentIndex: 1,
  steps: [
    { label: 'Three Caps', icon: 'checklist', detail: 'Max daily, weekly, and continuous teaching hours — each an institution-wide fallback.' },
    { label: 'Override Chain', icon: 'search', detail: 'Per-faculty override wins first, then per-designation default, then these institution-wide values.' },
    { label: 'Save', icon: 'send', detail: 'Save to apply the new caps to future staffing validation immediately.' },
  ],
};
