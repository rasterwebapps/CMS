import { TourDefinition } from '../tour.service';

export const CURRICULUM_VERSION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📚 Welcome to Curriculum Versions',
        description:
          'This screen lets you manage curriculum versions per academic program — clone, revise, and activate versions over time. Let\'s walk through it.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-cv-header',
      popover: {
        title: 'Curriculum Versions',
        description: 'Manage curriculum versions for the program selected below.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cv-program-select',
      popover: {
        title: 'Pick a Program',
        description:
          'Choose a program here to view its curriculum versions. The <strong>New Version</strong> button enables once a program is selected.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cv-add-btn',
      popover: {
        title: 'New Version',
        description: 'Create a brand-new curriculum version for the selected program.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-cv-content',
      popover: {
        title: 'Version Cards',
        description:
          'Each card shows the version name, effective academic year, and status. Use the <strong>clone</strong> icon to fork a new version, or click <em>View Curriculum Map</em> to see the full curriculum.',
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

