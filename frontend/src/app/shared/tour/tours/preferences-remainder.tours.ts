import { TourDefinition, TourFlowMap } from '../tour.service';

// Small, single-entry-funnel masters — the same 3-step shape (search/filter+view toggle → add/edit →
// used elsewhere) reused per screen, since each is a straightforward card/table master list.

// ─────────────────────────────────────────────────────────────────────────────
// Classrooms
// ─────────────────────────────────────────────────────────────────────────────
export const CLASSROOM_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🏫 Classrooms',
        description: 'Physical rooms used for theory session scheduling — referenced when staffing a timetable session.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-cls-toolbar',
      popover: {
        title: 'Search & View',
        description: 'Search by name or code, and switch between card and table view.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Used in Staffing',
        description: 'A classroom is picked as a session\'s room when staffing the timetable in Skeleton Builder / Staffing.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const CLASSROOM_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Classrooms', description: 'Physical rooms used for theory session scheduling.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search & View', icon: 'search', detail: 'By name or code, in card or table view.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Register a new classroom or update an existing one.' },
    { label: 'Used in Staffing', icon: 'checklist', detail: 'Picked as a session\'s room when staffing the timetable.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Clinical Venues
// ─────────────────────────────────────────────────────────────────────────────
export const CLINICAL_VENUE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🏥 Clinical Venues',
        description: 'Hospital wards/departments used for clinical posting scheduling.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-cv-toolbar',
      popover: {
        title: 'Search & View',
        description: 'Search by name or code, and switch between card and table view.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Used for Clinical Sessions',
        description: 'A clinical venue is picked as the location for CLINICAL-type sessions when staffing the timetable.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const CLINICAL_VENUE_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Clinical Venues', description: 'Hospital wards/departments used for clinical posting scheduling.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search & View', icon: 'search', detail: 'By name or code, in card or table view.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Register a new clinical venue or update an existing one.' },
    { label: 'Used for Clinical Sessions', icon: 'checklist', detail: 'Picked as the location for CLINICAL-type timetable sessions.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Periods
// ─────────────────────────────────────────────────────────────────────────────
export const PERIOD_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '⏰ Periods',
        description: 'The lecture time slots used for theory session scheduling — the fixed grid every timetable session is placed into.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-per-toolbar',
      popover: {
        title: 'Search & View',
        description: 'Search by name, and switch between card and table view.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ The Timetable Grid',
        description: 'Every period defined here becomes a column in Skeleton Builder\'s weekly grid.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const PERIOD_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Periods', description: 'Lecture time slots used for theory session scheduling.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search & View', icon: 'search', detail: 'By name, in card or table view.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Start time, end time, and name for a period.' },
    { label: 'Grid Columns', icon: 'checklist', detail: 'Each period becomes a column in the Skeleton Builder weekly grid.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Subjects
// ─────────────────────────────────────────────────────────────────────────────
export const SUBJECT_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📚 Academic Subjects',
        description: 'The subject master — every subject taught, mapped into curricula via Curriculum Map.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-sub-toolbar',
      popover: {
        title: 'Search, Filter & View',
        description: 'Search by name or code, filter by course, and switch between card and table view.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Curriculum Map',
        description: 'A subject on its own doesn\'t belong to any curriculum — that link is made in Curriculum Map, where subjects get placed into a curriculum version\'s terms.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const SUBJECT_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Subjects', description: 'The subject master — mapped into curricula via Curriculum Map.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search & Filter', icon: 'search', detail: 'By name, code, or course, in card or table view.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Register a new subject or update an existing one.' },
    { label: 'Map to Curriculum', icon: 'checklist', detail: 'Placed into a curriculum version\'s terms via Curriculum Map — not done here.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Staff Referrers
// ─────────────────────────────────────────────────────────────────────────────
export const STAFF_REFERRER_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🤝 Staff Referrers',
        description: 'Sister-concern staff who refer students to the college — tracked so referral commissions can be attributed correctly.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-sr-toolbar',
      popover: {
        title: 'Search & View',
        description: 'Search by name, and switch between card and table view.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Feeds Commission Explorer',
        description: 'A staff referrer\'s name shows up in Enquiries and, downstream, in Commission Explorer for any commission tied to their referral.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const STAFF_REFERRER_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Staff Referrers', description: 'Sister-concern staff who refer students to the college.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search & View', icon: 'search', detail: 'By name, in card or table view.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Register a new staff referrer or update an existing one.' },
    { label: 'Used in Enquiries', icon: 'checklist', detail: 'Selected as the referrer on an Enquiry, feeding Commission Explorer.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Holiday Templates
// ─────────────────────────────────────────────────────────────────────────────
export const HOLIDAY_TEMPLATE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📅 Holiday Templates',
        description: 'Recurring holidays — yearly (a fixed date) or monthly (e.g. every 2nd Saturday) — that auto-seed into every new academic year\'s calendar.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-ht-toolbar',
      popover: {
        title: 'Search & View',
        description: 'Search by name, and switch between card and table view.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Auto-Seeds New Years',
        description: 'When a new academic year is created, every active template here is used to generate its holiday entries automatically — no need to re-enter recurring holidays each year.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const HOLIDAY_TEMPLATE_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Holiday Templates', description: 'Recurring holidays that auto-seed into every new academic year\'s calendar.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search & View', icon: 'search', detail: 'By name, in card or table view.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Yearly (fixed date) or monthly (e.g. every 2nd Saturday) recurrence.' },
    { label: 'Auto-Seed', icon: 'checklist', detail: 'Every active template generates that holiday automatically in each new academic year.' },
  ],
};
