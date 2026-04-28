import { TourDefinition } from '../tour.service';

export const AGENT_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Referral Agents',
        description:
          'This screen lets you manage all referral agents who bring in admissions. Let\'s walk through the key controls — it only takes a minute.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-agent-header',
      popover: {
        title: 'Page Summary',
        description:
          'At a glance: total referral agents and how many are currently <strong>active</strong>. These stats refresh as you add or update agents.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-agent-add-btn',
      popover: {
        title: 'Add an Agent',
        description:
          'Click here to register a new referral agent — capture their contact details, area, allotted seats, and commission rate.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-agent-search',
      popover: {
        title: 'Search Agents',
        description:
          'Quickly filter by name, phone, email, or area. The search works across both card and table views.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-agent-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between a visual <strong>Card view</strong> and a compact <strong>Table view</strong>. Your preference is remembered.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-agent-content',
      popover: {
        title: 'Agent Cards',
        description:
          'Each card shows the agent\'s name, contact details, area, allotted seats, and active status. Hover for <strong>Edit</strong> and <strong>Delete</strong> actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ You\'re all set!',
        description:
          'You now know how to navigate the Agents screen. Replay this tour any time via <em>Take a Tour</em>.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const AGENT_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Agent Form',
        description:
          'This form lets you create or edit a referral agent. Let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#agent-name',
      popover: {
        title: 'Agent Name',
        description: 'Full name of the referral agent — required.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#agent-phone',
      popover: {
        title: 'Phone Number',
        description: 'Primary contact number for the agent — used for follow-ups.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#agent-email',
      popover: {
        title: 'Email Address',
        description: 'Optional email for digital correspondence and commission statements.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#agent-area',
      popover: {
        title: 'Area',
        description: 'Broad geographical area the agent operates in — e.g., <em>North Bangalore</em>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#agent-locality',
      popover: {
        title: 'Locality',
        description: 'More specific neighbourhood or locality within the area.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#agent-seats',
      popover: {
        title: 'Allotted Seats',
        description: 'Number of admission seats assigned to this agent for the academic year.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#agent-commission',
      popover: {
        title: 'Commission Override',
        description:
          'Override the default commission for this agent. Leave blank to use the <strong>Referral Type</strong> default.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-agent-active-toggle',
      popover: {
        title: 'Active Status',
        description: 'Enable to allow new admissions to be referred via this agent.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-agent-submit',
      popover: {
        title: 'Save the Agent',
        description: 'Click here to save. The button is disabled while saving is in progress.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'You now know everything about the agent form. Hit <strong>Save</strong> to add the agent.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

