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
      element: '#tour-elec-toolbar',
      popover: {
        title: 'Pick a Group',
        description:
          'Select an academic year, term, and elective group. Switch the Selection Mode between Student Choice and Institution Decided.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-elec-table',
      popover: {
        title: 'Assignment Records',
        description:
          'Each row is a student in the elective group, showing their current choice. In Student Choice mode, assign each row individually; in Institution Decided mode, use Apply to All.',
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
    { label: 'Pick a Group', icon: 'search', detail: 'Select an academic year, term, and elective group, then choose the selection mode.' },
    { label: 'Review Choices', icon: 'checklist', detail: 'Each row shows a student\'s current elective choice, or "Not assigned" if still pending.' },
    { label: 'Assign', icon: 'open', detail: 'Assign each student individually, or use Apply to All in Institution Decided mode.' },
    { label: 'Ready for Timetabling', icon: 'send', detail: 'Once resolved, the elective group can be placed into the timetable in Skeleton Builder.' },
  ],
};
