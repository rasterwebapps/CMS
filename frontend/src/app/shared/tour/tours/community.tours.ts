import { TourDefinition, TourFlowMap } from '../tour.service';

export const COMMUNITY_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Community Master',
        description:
          'This screen lets you manage community categories used in student admission records — e.g., General, OBC, SC, ST. Let\'s take a quick walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-community-header',
      popover: {
        title: 'Page Header',
        description:
          'The header shows the title and description. Admins will also see the <strong>Add Community</strong> button to create a new category.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-community-search',
      popover: {
        title: 'Search Communities',
        description:
          'Type a community name or code to instantly filter the list. Works across both card and table views.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-community-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between a visual <strong>Card view</strong> and a compact <strong>Table view</strong>. Your preference is saved.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-community-content',
      popover: {
        title: 'Community Cards / Rows',
        description:
          'Each entry shows the community name, code, and active status. Admins can hover to reveal <strong>Edit</strong> and <strong>Delete</strong> actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description:
          'You now know how to navigate the Community Master screen. Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const COMMUNITY_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Communities', description: 'Master list of community/category values used on student admission and statutory records.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'Search communities by name or code.' },
    { label: 'Browse Records', icon: 'checklist', detail: 'Cards/rows show each community\'s name, code, description, and active status.' },
    { label: 'Add', icon: 'open', detail: 'Create a new community with a name, code, and description.' },
    { label: 'Save', icon: 'send', detail: 'Save with a real-time uniqueness check on the name and code.' },
  ],
};

export const COMMUNITY_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Community Form',
        description:
          'This form lets you create or edit a community category for student demographics. Let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#community-name',
      popover: {
        title: 'Community Name',
        description:
          'Full name of the community category — e.g., <em>Backward Class</em>, <em>Scheduled Tribe</em>. This is required and must be unique.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#community-code',
      popover: {
        title: 'Community Code',
        description:
          'Short uppercase code used in dropdowns and reports — e.g., <strong>BC</strong>, <strong>MBC</strong>, <strong>OC</strong>. Must be unique.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-community-description',
      popover: {
        title: 'Description',
        description:
          'Optional. Add a brief note about this community category or its applicability.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-community-active',
      popover: {
        title: 'Active Status',
        description:
          'Enable to make this community available for selection in admission forms. Inactive communities are hidden from student-facing dropdowns.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-community-submit',
      popover: {
        title: 'Save the Community',
        description:
          'Click <strong>Save</strong> to create (or update) the community. The button is disabled while saving.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'You know everything about the community form. Fill in the details and hit <strong>Save</strong> to get started.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

