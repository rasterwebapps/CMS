import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Attendance
// ─────────────────────────────────────────────────────────────────────────────
export const ATTENDANCE_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🗓️ Attendance',
        description: 'Every attendance record marked so far — filter, search, and drill into a student or subject\'s history.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-att-mark',
      popover: {
        title: 'Mark Attendance',
        description: 'Take attendance for a class session — marks every enrolled student Present, Absent, Late, or Excused in one pass.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-att-toolbar',
      popover: {
        title: 'Search',
        description: 'Search across student, subject, and date to find a specific record quickly.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-att-table',
      popover: {
        title: 'Records',
        description: 'Each row is one student\'s attendance for one session, with a Present / Absent / Late status badge. Records can be deleted if marked in error.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const ATTENDANCE_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Attendance', description: 'Track and review student attendance records by course and date.' }],
  currentIndex: 0,
  steps: [
    { label: 'Mark Attendance', icon: 'checklist', detail: 'Take attendance for a session — Present/Absent/Late/Excused for every enrolled student at once.' },
    { label: 'Search & Filter', icon: 'search', detail: 'Find a specific student, subject, or date across all recorded attendance.' },
    { label: 'Review Records', icon: 'open', detail: 'Each row shows one student\'s status for one session.' },
    { label: 'Correct Mistakes', icon: 'send', detail: 'Delete a record if it was marked in error.' },
  ],
};
