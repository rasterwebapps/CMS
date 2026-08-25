import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Assign Faculty (Academics nav)
// ─────────────────────────────────────────────────────────────────────────────
export const ASSIGN_FACULTY_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🧑‍🏫 Assign Faculty',
        description:
          'Every course offering for the selected term, and who — if anyone — is currently assigned to teach it, per cohort and section.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-af-toolbar',
      popover: {
        title: 'Term, Search & Filters',
        description: 'Pick a term, then search or filter by semester/cohort to find the offering you need.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-af-table',
      popover: {
        title: 'Assign or Manage Batches',
        description:
          'Click Assign Faculty on a row to pick who teaches it — per section if the cohort has more than one. If it has Lab/Clinical batches, Manage Batches assigns faculty per batch instead.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Then build the timetable',
        description:
          'Once faculty are assigned, Skeleton Builder and Staffing can place and staff their sessions.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const ASSIGN_FACULTY_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Assign Faculty', description: 'Assign who teaches each course offering, per cohort and section.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Pick Term & Filter', icon: 'search', detail: 'Choose the term, then search or filter by semester/cohort.' },
    { label: 'Review Offerings', icon: 'checklist', detail: 'See each offering\'s current faculty assignment status at a glance.' },
    { label: 'Assign', icon: 'open', detail: 'Assign faculty per section, or per Lab/Clinical batch via Manage Batches.' },
    { label: 'Ready for Timetabling', icon: 'send', detail: 'Once assigned, Skeleton Builder and Staffing can build against it.' },
  ],
};
