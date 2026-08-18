import { TourDefinition, TourFlowMap } from '../tour.service';

// Room Purpose Categories → Room Sub-Types, a small local funnel (BR-54's 2-tier master:
// Category is the broad purpose, Sub-Type is the specific function within it).
export const ROOM_PURPOSE_FUNNEL = [
  { label: 'Room Purpose Categories', description: 'The broad purpose categories rooms can be classified under (Academic, Residential, etc.).' },
  { label: 'Room Sub-Types', description: 'The specific room function within each Purpose Category (Classroom, Student Bedroom, Staff Room, etc.).' },
];

// ─────────────────────────────────────────────────────────────────────────────
// Room Purpose Categories
// ─────────────────────────────────────────────────────────────────────────────
export const ROOM_PURPOSE_CATEGORY_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🏷️ Room Purpose Categories',
        description: 'The top tier of room classification — broad categories like Academic or Residential. Every room is eventually tagged with one via a Sub-Type.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-rpc-toolbar',
      popover: {
        title: 'Search & View',
        description: 'Search by name or code, and switch between card and table view.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Residential Gate',
        description: 'A category marked Residential is what makes a room eligible for hostel-room assignment — the isResidential flag gates that entire flow.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const ROOM_PURPOSE_CATEGORY_LIST_FLOW_MAP: TourFlowMap = {
  funnel: ROOM_PURPOSE_FUNNEL,
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'By name or code, in card or table view.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Name, code, description, and the Residential flag.' },
    { label: 'Residential Flag', icon: 'checklist', detail: 'Gates whether rooms under this category can be assigned as hostel rooms.' },
    { label: 'Activate / Deactivate', icon: 'send', detail: 'Toggle a category\'s status from its row/card actions.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Room Sub-Types
// ─────────────────────────────────────────────────────────────────────────────
export const ROOM_SUB_TYPE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🚪 Room Sub-Types',
        description: 'The specific room function within a Purpose Category — Classroom, Student Bedroom, Staff Room, and so on. Every physical room gets tagged with one.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-rst-toolbar',
      popover: {
        title: 'Search, Filter & View',
        description: 'Search by name or code, filter by Purpose Category, and switch between card and table view.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Drives Room Classification',
        description: 'This is the actual value assigned to a Room in Campus Setup — its parent Purpose Category is what determines whether it can be a hostel room.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const ROOM_SUB_TYPE_LIST_FLOW_MAP: TourFlowMap = {
  funnel: ROOM_PURPOSE_FUNNEL,
  currentIndex: 1,
  steps: [
    { label: 'Search & Filter', icon: 'search', detail: 'By name, code, or parent Purpose Category, in card or table view.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Name, code, and which Purpose Category it belongs to.' },
    { label: 'Assign to Rooms', icon: 'checklist', detail: 'Used when classifying an actual Room in Campus Setup.' },
    { label: 'Activate / Deactivate', icon: 'send', detail: 'Toggle a sub-type\'s status from its row/card actions.' },
  ],
};
