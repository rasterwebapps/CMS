import { TourDefinition } from '../tour.service';

export const EQUIPMENT_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Lab Equipment',
        description:
          'This screen lets you track every piece of lab equipment along with its category, lab, and maintenance status. Let\'s walk through the key controls.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-eq-header',
      popover: {
        title: 'Page Summary',
        description: 'At a glance: total equipment registered across all labs.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-eq-add-btn',
      popover: {
        title: 'Add Equipment',
        description:
          'Click here to register a new piece of equipment — name, model, serial, lab, category, and warranty info.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-eq-search',
      popover: {
        title: 'Search Equipment',
        description: 'Quickly filter by name, model, serial number, or category.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-eq-view-toggle',
      popover: {
        title: 'Switch Views',
        description: 'Toggle between <strong>Card view</strong> and <strong>Table view</strong>.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-eq-content',
      popover: {
        title: 'Equipment Cards',
        description:
          'Each card shows the equipment name, model, category badge, lab, status, and purchase date. Hover for actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ You\'re all set!',
        description: 'Replay this tour any time via <em>Take a Tour</em>.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const EQUIPMENT_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Equipment Form',
        description: 'This form lets you create or edit a piece of lab equipment — let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    { element: '#eq-name', popover: { title: 'Equipment Name', description: 'Descriptive name of the equipment — e.g., <em>Microscope X-200</em>.', side: 'bottom', align: 'start' } },
    { element: '#eq-model', popover: { title: 'Model', description: 'Manufacturer model identifier.', side: 'bottom', align: 'start' } },
    { element: '#eq-serial', popover: { title: 'Serial Number', description: 'Unique serial number from the manufacturer for traceability.', side: 'bottom', align: 'start' } },
    { element: '#eq-lab', popover: { title: 'Lab', description: 'Which lab the equipment lives in.', side: 'bottom', align: 'start' } },
    { element: '#eq-category', popover: { title: 'Category', description: 'Equipment category — e.g., Chemistry, Computer, Electronics.', side: 'bottom', align: 'start' } },
    { element: '#eq-status', popover: { title: 'Status', description: 'Operational status — Available, Under Maintenance, Retired, etc.', side: 'bottom', align: 'start' } },
    { element: '#eq-purchase-date', popover: { title: 'Purchase Date', description: 'Date the equipment was acquired.', side: 'bottom', align: 'start' } },
    { element: '#eq-warranty', popover: { title: 'Warranty Until', description: 'Warranty expiry date — used for maintenance scheduling.', side: 'bottom', align: 'start' } },
    { element: '#eq-cost', popover: { title: 'Cost (₹)', description: 'Purchase cost of the equipment in <strong>₹</strong>.', side: 'bottom', align: 'start' } },
    { element: '#tour-eq-submit', popover: { title: 'Save the Equipment', description: 'Click here to save. The button is disabled while saving.', side: 'top', align: 'end' } },
    {
      popover: {
        title: '✅ Ready to go!',
        description: 'Hit <strong>Save</strong> to register the equipment.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

