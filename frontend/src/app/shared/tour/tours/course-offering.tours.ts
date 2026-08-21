import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Course Offering List
// ─────────────────────────────────────────────────────────────────────────────
export const COURSE_OFFERING_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📅 Course Offerings',
        description:
          'Course offerings are the per-term, per-subject records generated from the active curriculum. Deciding who teaches each one happens later, on the separate Assign Faculty screen — this screen is just structure and status.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-cofr-toolbar',
      popover: {
        title: 'Pick a Term',
        description:
          'Select an academic year and term to load its offerings, then search or generate new offerings for that term.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cofr-table',
      popover: {
        title: 'Offering Records',
        description:
          'Each row is one subject offered that term, its cohort(s), and active status.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Generate, then staff later',
        description:
          'Use Generate Offerings to create them from the curriculum, and Assign Electives to place students into elective offerings — then head to Assign Faculty once you know which subjects and elective options actually need a teacher (that\'s also where Manage Batches lives now).',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const COURSE_OFFERING_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Course Offerings', description: 'Per-term, per-subject records generated from the active curriculum.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Pick a Term', icon: 'search', detail: 'Select an academic year and term to load its offerings, then search within them.' },
    { label: 'Generate Offerings', icon: 'open', detail: 'Generate offerings from the active curriculum for the selected term.' },
    { label: 'Offering Records', icon: 'checklist', detail: 'Each row shows the subject, its cohort(s), and active status.' },
    { label: 'Assign Electives', icon: 'send', detail: 'Place students into elective offerings — then staff everything, including batches, on Assign Faculty.' },
  ],
};
