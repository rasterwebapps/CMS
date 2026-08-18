import { TourDefinition, TourFlowMap } from '../tour.service';
import { TIMETABLE_OPERATIONS_FUNNEL } from './resource-timetable-grid.tours';

// ─────────────────────────────────────────────────────────────────────────────
// Staff Session Swap
// ─────────────────────────────────────────────────────────────────────────────
export const STAFF_SESSION_SWAP_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🔁 Staff Session Swap',
        description: 'Trade which staff member teaches a session, for one date only — both sides must be mutually available.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-sss-toolbar',
      popover: {
        title: 'Term and Date',
        description: 'Pick a term and a date to see every published session scheduled that day.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-sss-list',
      popover: {
        title: 'Sessions',
        description: 'Each row is one session. Click Swap on the session you want to hand off to see who\'s mutually available to take it.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ One-date swap',
        description: 'Applying a swap only changes faculty for that single date — it doesn\'t touch the underlying timetable skeleton or staffing.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const STAFF_SESSION_SWAP_FLOW_MAP: TourFlowMap = {
  funnel: TIMETABLE_OPERATIONS_FUNNEL,
  currentIndex: 2,
  steps: [
    { label: 'Term & Date', icon: 'search', detail: 'Pick a term and date to see every published session scheduled that day.' },
    { label: 'Pick a Session to Swap', icon: 'checklist', detail: 'Click Swap on the session you want to hand off.' },
    { label: 'Choose a Partner', icon: 'open', detail: 'Only staff who are mutually available for that exact slot on that date are listed.' },
    { label: 'Apply', icon: 'send', detail: 'Confirm a candidate — the swap applies for that single date only.' },
  ],
};
