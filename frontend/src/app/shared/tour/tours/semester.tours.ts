import { TourDefinition } from '../tour.service';

export const SEMESTER_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Semesters',
        description:
          'This screen lets you manage semester periods within academic years. Quick walkthrough of the controls.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-sem-header',
      popover: {
        title: 'Page Summary',
        description:
          'Total number of semesters across all academic years.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-sem-add-btn',
      popover: {
        title: 'Add a Semester',
        description:
          'Click here to register a new semester — pick the academic year, name, semester number, and date range.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-sem-filters',
      popover: {
        title: 'Filter by Academic Year',
        description:
          'Narrow the list to semesters belonging to a specific academic year.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-sem-search',
      popover: {
        title: 'Search Semesters',
        description:
          'Quickly filter by semester name, number, or parent year.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-sem-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between <strong>Card</strong> and <strong>Table</strong> view. Your preference is remembered.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-sem-content',
      popover: {
        title: 'Semester Cards',
        description:
          'Each card shows the semester name, number badge, parent year, and date range. Hover for edit/delete actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ You\'re all set!',
        description:
          'You now know how to navigate Semesters. Replay this tour any time via <em>Take a Tour</em>.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const SEMESTER_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Semester Form',
        description:
          'This form lets you create or edit a semester. We\'ll walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#sem-ay',
      popover: {
        title: 'Academic Year',
        description:
          'Pick the academic year this semester belongs to. Required.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#sem-name',
      popover: {
        title: 'Semester Name',
        description:
          'A descriptive label, e.g., <em>Fall 2024</em>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#sem-number',
      popover: {
        title: 'Semester Number',
        description:
          'The sequence number within the academic year (e.g., <strong>1</strong>, <strong>2</strong>).',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#sem-start',
      popover: {
        title: 'Start Date',
        description:
          'The first day of this semester.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#sem-end',
      popover: {
        title: 'End Date',
        description:
          'The last day of this semester. Must be after the start date.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-sem-submit',
      popover: {
        title: 'Save the Semester',
        description:
          'Click here to create (or update) the semester.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'Hit <strong>Create Semester</strong> to add it.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

