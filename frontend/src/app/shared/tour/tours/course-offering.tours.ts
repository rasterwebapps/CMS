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
          'Course offerings are the per-term, per-subject teaching assignments generated from the active curriculum — faculty, section, and status, ready for timetabling.',
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
          'Each row is one subject offered that term — its faculty, secondary faculty, section, and active status. Manage Batches splits a section into teaching batches for labs/clinicals.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Generate or manage',
        description:
          'Use Generate Offerings to create them from the curriculum, or Assign Electives to place students into elective offerings for the term.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const COURSE_OFFERING_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Course Offerings', description: 'Per-term, per-subject teaching assignments generated from the active curriculum.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Pick a Term', icon: 'search', detail: 'Select an academic year and term to load its offerings, then search within them.' },
    { label: 'Generate Offerings', icon: 'open', detail: 'Generate offerings from the active curriculum for the selected term.' },
    { label: 'Offering Records', icon: 'checklist', detail: 'Each row shows the subject, faculty, section, and active status for that term.' },
    { label: 'Manage Batches / Electives', icon: 'send', detail: 'Split a section into lab/clinical batches, or assign electives before the timetable is built.' },
  ],
};
