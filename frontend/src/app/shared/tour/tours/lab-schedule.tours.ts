import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Lab Schedule List
// ─────────────────────────────────────────────────────────────────────────────
export const LAB_SCHEDULE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🧪 Lab Schedules',
        description:
          'Manage lab session timetables — day, time, room, subject, faculty, and batch assignments for every lab session.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-labsch-toolbar',
      popover: {
        title: 'Search',
        description: 'Search schedules by subject, room, or faculty name.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-labsch-table',
      popover: {
        title: 'Schedule Records',
        description:
          'Each row is one lab session — day, session type, room, subject, faculty, batch, and start/end time. New sessions come from Timetable Builder or the Special Class Scheduler; edit an existing row here to correct it.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const LAB_SCHEDULE_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Lab Schedules', description: 'Day, time, room, subject, faculty, and batch assignments for every lab session.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'Search schedules by subject, room, or faculty name.' },
    { label: 'Schedule Records', icon: 'checklist', detail: 'Each row is one lab session with its day, time, room, subject, faculty, and batch.' },
  ],
};
