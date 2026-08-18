import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Inventory
// ─────────────────────────────────────────────────────────────────────────────
export const INVENTORY_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📦 Lab Inventory',
        description: 'Track lab consumables — quantities, units, and minimum stock levels — per lab.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-inv-toolbar',
      popover: {
        title: 'Search',
        description: 'Search by item name, lab, or category.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-inv-table',
      popover: {
        title: 'Items',
        description: 'Each row is one consumable, with its current quantity against its Min Stock threshold. Edit or delete from the row actions, or Add Item for something new.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const INVENTORY_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Inventory', description: 'Track lab consumables — quantities, units, and minimum stock levels.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'By item name, lab, or category.' },
    { label: 'Review Stock', icon: 'checklist', detail: 'Current quantity against Min Stock per item.' },
    { label: 'Add / Edit', icon: 'open', detail: 'Register a new consumable or update an existing one.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Maintenance
// ─────────────────────────────────────────────────────────────────────────────
export const MAINTENANCE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🔧 Maintenance Requests',
        description: 'Track equipment maintenance requests — priority, status, and who\'s assigned to fix it.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-maint-toolbar',
      popover: {
        title: 'Search',
        description: 'Search by equipment or who requested it.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-maint-table',
      popover: {
        title: 'Requests',
        description: 'Each row shows priority, status, and assigned technician. Edit or delete from the row actions, or New Request to report a new issue.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const MAINTENANCE_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Maintenance', description: 'Track equipment maintenance requests, priorities, and assignments.' }],
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'By equipment or who requested it.' },
    { label: 'Review Requests', icon: 'checklist', detail: 'Priority, status, and assigned technician per request.' },
    { label: 'Report / Edit', icon: 'open', detail: 'File a new maintenance request or update an existing one.' },
  ],
};
