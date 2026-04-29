import { TourDefinition } from '../tour.service';

export const PROGRAM_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Programs',
        description:
          'This screen lists every academic degree programme offered by the college. Let\'s do a quick walkthrough of the controls.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-program-header',
      popover: {
        title: 'Page Summary',
        description:
          'See the total number of programmes at a glance. The count updates as you add or remove programmes.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-program-add-btn',
      popover: {
        title: 'Add a Program',
        description:
          'Click here to register a new degree programme — set the name, code, duration in years, and status.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-program-search',
      popover: {
        title: 'Search Programs',
        description:
          'Quickly filter the list by programme name or code. Search works across both card and table views.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-program-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between a visual <strong>Card</strong> view and a compact <strong>Table</strong> view. Your preference is remembered across sessions.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-program-content',
      popover: {
        title: 'Program Cards',
        description:
          'Each card shows the code, name, duration, semester count, and status. Hover a card to reveal <strong>Edit</strong> and <strong>Delete</strong> actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ You\'re all set!',
        description:
          'You now know how to navigate the Programs screen. You can replay this tour any time via <em>Take a Tour</em>.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const PROGRAM_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Program Form',
        description:
          'This form lets you create or edit an academic programme. We\'ll walk through each field together.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#program-name',
      popover: {
        title: 'Program Name',
        description:
          'Enter the full official name of the programme — e.g., <em>Bachelor</em>. This field is required.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#program-code',
      popover: {
        title: 'Program Code',
        description:
          'A short uppercase identifier (max 20 characters) used across the system — e.g., <strong>BACH</strong>, <strong>MAST</strong>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#program-duration',
      popover: {
        title: 'Duration',
        description:
          'Length of the programme in years. Enter a value between <strong>1</strong> and <strong>10</strong>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#program-status',
      popover: {
        title: 'Status',
        description:
          'Set <strong>Active</strong> to make this programme available for new admissions, or <strong>Inactive</strong> to retire it.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-program-submit',
      popover: {
        title: 'Save the Program',
        description:
          'When all required fields are filled, click here to create (or update) the programme. The button is disabled while saving.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'You know everything about the programme form. Fill in the details and hit <strong>Create Program</strong> to add it.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

