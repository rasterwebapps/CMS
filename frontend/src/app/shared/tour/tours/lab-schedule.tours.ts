import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Class Schedule List — date-wise occurrence browser
// ─────────────────────────────────────────────────────────────────────────────
export const LAB_SCHEDULE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🧪 Class Schedules',
        description:
          'See every real session that actually happens on one calendar date — across every room, faculty, and cohort — not just the weekly recurring pattern.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-labsch-toolbar',
      popover: {
        title: 'Date & Filters',
        description: 'Pick a date to browse, then narrow by session type, room, faculty, status, or search.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-labsch-table',
      popover: {
        title: 'Session Records',
        description:
          'Each row is one real session on the picked date — time, type, room, subject, faculty, batch, and status (Held, Substituted, Rescheduled, or Cancelled). Swap or Reschedule one session here without touching its recurring weekly template.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const LAB_SCHEDULE_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Class Schedules', description: 'Every real session on one date, across every room and faculty.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Date & Filters', icon: 'search', detail: 'Pick a date, then narrow by type, room, faculty, status, or search.' },
    { label: 'Session Records', icon: 'checklist', detail: 'Each row is one real session with its time, room, subject, faculty, batch, and status.' },
  ],
};
