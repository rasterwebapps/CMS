import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Racks & Shelves
// ─────────────────────────────────────────────────────────────────────────────
export const LIBRARY_RACK_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🗄️ Racks & Shelves',
        description: 'The physical storage hierarchy — racks hold shelves, and every book/journal is placed on one, following the C{n}-R{n} shelf/rack code convention.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-librack-add',
      popover: {
        title: 'Add a Rack',
        description: 'Register a new physical rack — name, code, and description.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-librack-table',
      popover: {
        title: 'Racks',
        description: 'View Shelves drills into that rack\'s shelves. Activate/Deactivate and Edit are also available per row.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const LIBRARY_RACK_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Racks & Shelves', description: 'Manage the physical storage hierarchy that every book/journal is shelved under.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'Find a rack by name or code.' },
    { label: 'Add a Rack', icon: 'open', detail: 'Register a new physical rack.' },
    { label: 'View Shelves', icon: 'checklist', detail: 'Drill into a rack to manage its individual shelves.' },
    { label: 'Toggle / Edit', icon: 'send', detail: 'Activate, deactivate, or edit a rack from its row actions.' },
  ],
};
