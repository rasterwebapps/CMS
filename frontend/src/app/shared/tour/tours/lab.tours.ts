import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Labs
// ─────────────────────────────────────────────────────────────────────────────
export const LAB_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🔬 Lab Facilities',
        description: 'Laboratory facilities — type, speciality assignment, and status — used for lab session scheduling.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-lab-toolbar',
      popover: {
        title: 'Search, Filter & View',
        description: 'Search by name, filter by speciality/type/status, and switch between card and table view.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Used for Lab Sessions',
        description: 'A lab is picked as the venue for Lab-type sessions when staffing the timetable — its speciality assignment narrows which subjects it\'s eligible for.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const LAB_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Labs', description: 'Laboratory facilities used for lab session scheduling.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search & Filter', icon: 'search', detail: 'By name, speciality, type, or status, in card or table view.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Register a new lab or update an existing one.' },
    { label: 'Used in Staffing', icon: 'checklist', detail: 'Picked as the venue for Lab-type sessions, narrowed by its speciality assignment.' },
  ],
};
