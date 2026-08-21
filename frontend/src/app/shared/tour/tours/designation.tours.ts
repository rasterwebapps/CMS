import { TourDefinition, TourFlowMap } from '../tour.service';

export const DESIGNATION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Designations',
        description:
          'This screen lets you manage all faculty designation titles used across the college — e.g., Professor, Associate Professor, Lecturer. Let\'s take a quick walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-dsg-header',
      popover: {
        title: 'Page Header',
        description:
          'The header shows the screen title and description. The <strong>Add Designation</strong> button opens the creation form.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-dsg-search',
      popover: {
        title: 'Search Designations',
        description:
          'Type a designation name or code to instantly filter the list. Works across both card and table views.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-dsg-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between a visual <strong>Card view</strong> and a compact <strong>Table view</strong>. Your preference is remembered across sessions.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-dsg-content',
      popover: {
        title: 'Designation Cards / Rows',
        description:
          'Each entry shows the designation code, name, and description. Hover over a card to reveal <strong>Edit</strong> and <strong>Delete</strong> actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description:
          'You now know how to navigate the Designations screen. Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const DESIGNATION_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Designations', description: 'Master list of faculty/staff job designations used across HR and faculty records.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'Search designations by name or code.' },
    { label: 'Browse Records', icon: 'checklist', detail: 'Cards/rows show each designation\'s name, code, and description.' },
    { label: 'Add', icon: 'open', detail: 'Create a new designation with a name, code, and description.' },
    { label: 'Save', icon: 'send', detail: 'Save with a real-time uniqueness check on the name and code.' },
  ],
};

export const DESIGNATION_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Designation Form',
        description:
          'This form lets you create or edit a faculty designation title. Let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#dsg-name',
      popover: {
        title: 'Designation Name',
        description:
          'Full title of the designation — e.g., <em>Professor</em>, <em>Associate Professor</em>, <em>Lecturer</em>. This is required.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#dsg-code',
      popover: {
        title: 'Designation Code',
        description:
          'A short uppercase identifier used in reports and dropdowns — e.g., <strong>PROF</strong>, <strong>ASSOC_PROF</strong>. Must be unique.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#dsg-desc',
      popover: {
        title: 'Description',
        description:
          'Optional. Briefly describe the role, responsibilities, or seniority level associated with this designation.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-dsg-submit',
      popover: {
        title: 'Save the Designation',
        description:
          'Click <strong>Save</strong> to create (or update) the designation. The button is disabled while saving is in progress.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'You know everything about the designation form. Fill in the details and hit <strong>Save</strong> to get started.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const DESIGNATION_FORM_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Designations', description: 'Master list of faculty/staff job designations used across HR and faculty records.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Name & Code', icon: 'checklist', detail: 'Full designation title and a short uppercase code — both required and unique.' },
    { label: 'Description', icon: 'open', detail: 'Optional description of the role, responsibilities, or seniority level.' },
    { label: 'Save', icon: 'send', detail: 'Save with a real-time uniqueness check on the name and code.' },
  ],
};

