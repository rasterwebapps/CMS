import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Faculty View (Faculty List's detail screen — 5 tabs built across several
// tickets: Profile / Professional / Courses / Lab Schedules / Documents)
// ─────────────────────────────────────────────────────────────────────────────
export const FACULTY_DETAIL_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🧑‍🏫 Faculty Profile',
        description:
          'A complete record for one faculty member — identity, professional details, term workload, lab schedule, and documents.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-fac-detail-hero',
      popover: {
        title: 'Identity & Status',
        description:
          'Name, employee code, designation, speciality, and current status badge — confirms you\'re viewing the right faculty member. Hours assigned this term shows here too, if you have workload access.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-fac-detail-tabs',
      popover: {
        title: 'Profile, Professional, Courses, Lab Schedules, Documents',
        description:
          'Profile and Professional hold static details. Courses shows this term\'s real teaching workload (hours assigned vs. capacity, with a Raise Cap option). Lab Schedules shows their actual placed timetable sessions. Documents manages uploaded files. Courses and Lab Schedules only show if you have workload/lab-schedule view access.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Profile loaded',
        description:
          'Edit from here if you have manage access, or return to Faculty to browse the full list.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const FACULTY_DETAIL_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Faculty', description: 'Browse and manage every faculty member\'s profile.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Identity & Status', icon: 'checklist', detail: 'Name, employee code, designation, speciality, and current status badge.' },
    { label: 'Profile & Professional', icon: 'open', detail: 'Contact details, qualifications, specialization, and lab expertise.' },
    { label: 'Courses & Lab Schedules', icon: 'search', detail: 'Real term workload (hours vs. capacity) and actual placed timetable sessions, where permitted.' },
    { label: 'Documents', icon: 'receipt', detail: 'Uploaded documents for this faculty member.' },
  ],
};
