import { TourDefinition, TourFlowMap } from '../tour.service';

// Room Preferences → Room Allocation is a real pipeline (a non-binding request gets fulfilled by an
// actual allocation); Hostel Room Types is the master both depend on, included for context.
export const HOSTEL_MANAGEMENT_FUNNEL = [
  { label: 'Hostel Room Types', description: 'Room sharing options and yearly hostel fees by room type.' },
  { label: 'Room Preferences', description: 'Non-binding room requests captured at enquiry and admission, waiting to be allocated.' },
  { label: 'Room Allocation', description: 'The occupancy map — allocate a student to an actual bed, or free one up.' },
];

// ─────────────────────────────────────────────────────────────────────────────
// Hostel Room Types
// ─────────────────────────────────────────────────────────────────────────────
export const HOSTEL_ROOM_TYPE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🛏️ Hostel Room Types',
        description: 'The room sharing options offered — single, double, triple-sharing, etc. — each with its own yearly hostel fee.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-hrt-toolbar',
      popover: {
        title: 'Search & View',
        description: 'Search by name or code, and switch between card and table view.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Feeds Room Preferences & Allocation',
        description: 'Students pick a preferred room type in Room Preferences, and an actual hostel room\'s type shows here on the Room Allocation board.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const HOSTEL_ROOM_TYPE_LIST_FLOW_MAP: TourFlowMap = {
  funnel: HOSTEL_MANAGEMENT_FUNNEL,
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'By name or code, in card or table view.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Sharing capacity and the yearly hostel fee for this type.' },
    { label: 'Used Downstream', icon: 'checklist', detail: 'Referenced by Room Preferences and by every actual hostel room in Room Allocation.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Room Preferences
// ─────────────────────────────────────────────────────────────────────────────
export const ROOM_PREFERENCE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Room Preferences',
        description: 'Non-binding room requests captured at enquiry and admission — a student\'s preferred room type and zone, waiting to be turned into an actual allocation.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-rpl-toolbar',
      popover: {
        title: 'Search & Filter',
        description: 'Search by requester name, or filter by Pending / Fulfilled / Cancelled status.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-rpl-table',
      popover: {
        title: 'Preferences',
        description: 'A Pending row can jump straight to Room Allocation, be marked Fulfilled once housed, or be Cancelled if no longer needed.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const ROOM_PREFERENCE_LIST_FLOW_MAP: TourFlowMap = {
  funnel: HOSTEL_MANAGEMENT_FUNNEL,
  currentIndex: 1,
  steps: [
    { label: 'Search & Filter', icon: 'search', detail: 'By requester name, or by Pending/Fulfilled/Cancelled status.' },
    { label: 'Review', icon: 'checklist', detail: 'Preferred room type and zone per pending request.' },
    { label: 'Go to Allocation', icon: 'open', detail: 'Jump straight to Room Allocation to act on a pending preference.' },
    { label: 'Resolve', icon: 'send', detail: 'Mark Fulfilled once housed, or Cancel if no longer needed.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Room Allocation
// ─────────────────────────────────────────────────────────────────────────────
export const ROOM_ALLOCATION_DASHBOARD_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🏠 Room Allocation',
        description: 'The occupancy map across every active hostel room — see who\'s where, allocate a student to a bed, or free one up.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-rad-toolbar',
      popover: {
        title: 'Search & Filters',
        description: 'Search by room number or student, and filter by room type, zone, or gender eligibility.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-rad-grid',
      popover: {
        title: 'Room Cards',
        description: 'Each card is one hostel room, with a capacity meter and its current occupants. Allocate opens the form for an empty bed; the ✕ next to an occupant cancels their allocation.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Allocate a Student',
        description: 'Search the hosteler by name or admission number, set a start date (and optional end date), then confirm — this is the step that actually assigns a bed.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const ROOM_ALLOCATION_DASHBOARD_FLOW_MAP: TourFlowMap = {
  funnel: HOSTEL_MANAGEMENT_FUNNEL,
  currentIndex: 2,
  steps: [
    { label: 'Search & Filter', icon: 'search', detail: 'By room number/student, room type, zone, or gender eligibility.' },
    { label: 'Review Occupancy', icon: 'checklist', detail: 'Each room card shows its capacity meter and current occupants.' },
    { label: 'Allocate', icon: 'open', detail: 'Search a hosteler, set a start (and optional end) date, and confirm.' },
    { label: 'Free a Bed', icon: 'send', detail: 'Cancel an occupant\'s allocation directly from their room card.' },
  ],
};
