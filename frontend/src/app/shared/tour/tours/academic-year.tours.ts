import { TourDefinition } from '../tour.service';

export const ACADEMIC_YEAR_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Academic Years',
        description:
          'This screen lets you define and manage academic year sessions. Quick walkthrough of the controls.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-ay-header',
      popover: {
        title: 'Page Summary',
        description:
          'See total academic years and how many are marked as the <strong>Current</strong> session at a glance.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-ay-add-btn',
      popover: {
        title: 'Add an Academic Year',
        description:
          'Click here to register a new academic year — set the name, start date, end date, and mark as current if applicable.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-ay-search',
      popover: {
        title: 'Search Years',
        description:
          'Quickly filter academic years by name, e.g., <em>2024-2025</em>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-ay-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between <strong>Card</strong> and <strong>Table</strong> view. Your preference is remembered.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-ay-content',
      popover: {
        title: 'Year Cards',
        description:
          'Each card shows the year name, date range, and a <em>Current</em>/<em>Past</em> badge. Hover for view, edit, and delete actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ You\'re all set!',
        description:
          'You now know how to navigate Academic Years. Replay this tour any time via <em>Take a Tour</em>.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const ACADEMIC_YEAR_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Academic Year Form',
        description:
          'This form lets you create or edit an academic year session. We\'ll walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#ay-name',
      popover: {
        title: 'Name',
        description:
          'Enter the session name in the format <em>YYYY-YYYY</em>, e.g., <strong>2024-2025</strong>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#ay-start',
      popover: {
        title: 'Start Date',
        description:
          'The first day of this academic session.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#ay-end',
      popover: {
        title: 'End Date',
        description:
          'The last day of this academic session. Must be after the start date.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-ay-current-toggle',
      popover: {
        title: 'Current Session',
        description:
          'Mark this year as the active session for new admissions. Only <strong>one</strong> year can be current at a time.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-ay-submit',
      popover: {
        title: 'Save the Academic Year',
        description:
          'Click here to create (or update) the year. The button is disabled while saving.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'Hit <strong>Create Academic Year</strong> to add it.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

