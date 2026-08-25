import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// My Timetable (Overview nav group — personal read-only schedule)
// ─────────────────────────────────────────────────────────────────────────────
export const MY_TIMETABLE_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📆 My Timetable',
        description: 'Your own published theory and lab schedule for the selected term — read-only, always current.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-mytt-toolbar',
      popover: {
        title: 'Term & View',
        description: 'Pick a term, then switch between Week / Month / Day views to see your schedule at different levels of detail.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-mytt-view',
      popover: {
        title: 'Your Sessions',
        description: 'Click any session to log progress against it, if you have that permission — otherwise this is a pure read-only view.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const MY_TIMETABLE_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'My Timetable', description: 'Your own published theory and lab schedule for the selected term.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Pick Term & View', icon: 'search', detail: 'Choose the term, then Week / Month / Day view.' },
    { label: 'Browse Sessions', icon: 'checklist', detail: 'See your own theory and lab sessions for the period.' },
    { label: 'Log Progress', icon: 'open', detail: 'Click a session to log progress against it, where permitted.' },
  ],
};
