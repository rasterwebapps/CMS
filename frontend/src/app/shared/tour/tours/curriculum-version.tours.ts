import { TourDefinition } from '../tour.service';

export const CURRICULUM_VERSION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📚 Welcome to Curriculum Versions',
        description:
          'This screen lists every curriculum version across all programs — clone, revise, and activate versions over time. Let\'s walk through it.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-cv-header',
      popover: {
        title: 'Curriculum Versions',
        description: 'All curriculum versions are listed here by default — filter down to one program below if needed.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cv-program-filter',
      popover: {
        title: 'Filter by Program',
        description: 'Narrow the list down to one program, or leave it on <em>All Programs</em>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cv-status-filter',
      popover: {
        title: 'Filter by Status',
        description: 'Show only Active or only Inactive versions.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cv-view-toggle',
      popover: {
        title: 'Card or Table View',
        description: 'Switch between a card layout and a sortable table — your choice is remembered.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-cv-add-btn',
      popover: {
        title: 'New Version',
        description: 'Create a brand-new curriculum version — you\'ll pick the program inside the form.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-cv-content',
      popover: {
        title: 'Version Cards',
        description:
          'Each card shows the version name, program, effective academic year, content summary, and status. Use <strong>Clone</strong> to duplicate a version with its full term/subject mapping, or click <em>View Curriculum Map</em> to see the full curriculum.',
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

export const CURRICULUM_VERSION_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Curriculum Version Form',
        description: 'Create or edit a curriculum version — let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    { element: '#cv-program', popover: { title: 'Program', description: 'The academic program this curriculum version belongs to.', side: 'bottom', align: 'start' } },
    { element: '#cv-version-name', popover: { title: 'Version Name', description: 'Use a versioning convention like <em>2024-25 v1</em>.', side: 'bottom', align: 'start' } },
    { element: '#cv-ay', popover: { title: 'Effective Academic Year', description: 'The academic year from which this version takes effect.', side: 'bottom', align: 'start' } },
    {
      element: '#tour-cv-active-toggle',
      popover: {
        title: 'Active Status',
        description:
          'When active, this version is used for new enrollments — <strong>only one active version per program</strong>.',
        side: 'bottom',
        align: 'start',
      },
    },
    { element: '#tour-cv-submit', popover: { title: 'Save the Version', description: 'Click here to save. The button is disabled while saving.', side: 'top', align: 'end' } },
    {
      popover: {
        title: '✅ Ready to go!',
        description: 'Hit <strong>Save</strong> to create the curriculum version.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

