import { TourDefinition } from '../tour.service';

export const REFERRAL_TYPE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Referral Types',
        description:
          'This screen lets you configure all referral sources used during admissions — walk-in, agent, online, and more. Let\'s tour the key controls.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-rt-header',
      popover: {
        title: 'Page Summary',
        description:
          'At a glance: total referral types and how many are currently active. System-defined types are protected from deletion.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-rt-add-btn',
      popover: {
        title: 'Add a Referral Type',
        description:
          'Click here to define a new referral source — set the name, code, commission rules, and description.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-rt-search',
      popover: {
        title: 'Search Referral Types',
        description: 'Quickly filter by name or code across both card and table views.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-rt-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between <strong>Card view</strong> and <strong>Table view</strong>. Your preference is remembered.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-rt-content',
      popover: {
        title: 'Referral Type Cards',
        description:
          'Each card shows the code, name, commission amount, and active state. <em>System-defined types</em> cannot be deleted.',
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

export const REFERRAL_TYPE_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Referral Type Form',
        description:
          'This form lets you create or edit a referral type — let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#rt-name',
      popover: {
        title: 'Name',
        description: 'Display name for the referral source — e.g., <em>Walk-in</em> or <em>Agent</em>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#rt-code',
      popover: {
        title: 'Code',
        description: 'Unique uppercase identifier — e.g., <strong>WALK_IN</strong>, <strong>AGENT</strong>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-rt-commission-toggle',
      popover: {
        title: 'Has Commission',
        description: 'Enable to charge a commission per referral via this source.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#rt-commission',
      popover: {
        title: 'Commission Amount',
        description:
          'Default commission amount in <strong>₹</strong> — used unless overridden per agent.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#rt-description',
      popover: {
        title: 'Description',
        description: 'Optional description to clarify when and how to use this referral type.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-rt-active-toggle',
      popover: {
        title: 'Active Status',
        description: 'Enable to make this referral type available during admission entry.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-rt-submit',
      popover: {
        title: 'Save the Referral Type',
        description: 'Click here to save. The button is disabled while saving.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description: 'You know everything about the form — hit <strong>Save</strong> to create the referral type.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

