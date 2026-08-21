import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Elective Assignment
// ─────────────────────────────────────────────────────────────────────────────
export const ELECTIVE_ASSIGNMENT_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '☑️ Elective Assignment',
        description:
          'Assign each enrolled student\'s choice-based elective for the term — either students pick for themselves, or the institution decides for everyone.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-elec-choose',
      popover: {
        title: 'Choose a Group',
        description:
          'Pick an academic year and term, then choose from every elective group open that term — each shows an assigned/eligible count so you can see progress at a glance. Click one to work on it.',
        side: 'right',
        align: 'start',
      },
    },
    {
      element: '#tour-elec-table',
      popover: {
        title: 'Assign',
        description:
          'Each row is a student in the group, showing their current choice. A group opens read-only once everyone\'s assigned — click Edit to make changes. In Student Choice mode, assign or change each row individually; in Institution Decided mode, use Apply to All. Switch the Selection Mode from here too.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Assign before scheduling',
        description:
          'Elective assignment must be resolved before the group is placed into the timetable in Skeleton Builder.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const ELECTIVE_ASSIGNMENT_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Elective Assignment', description: 'Assign each enrolled student\'s choice-based elective for the term.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Pick a Group', icon: 'search', detail: 'Select an academic year and term, then choose an elective group card on the left.' },
    { label: 'Review Choices', icon: 'checklist', detail: 'Each row shows a student\'s current elective choice, or "Not assigned" if still pending.' },
    { label: 'Assign', icon: 'open', detail: 'Assign each student individually, or use Apply to All in Institution Decided mode.' },
    { label: 'Ready for Timetabling', icon: 'send', detail: 'Once resolved, the elective group can be placed into the timetable in Skeleton Builder.' },
  ],
};
