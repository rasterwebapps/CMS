import { TourDefinition, TourFlowMap } from '../tour.service';

export const INSTITUTION_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Institution Master',
        description:
          'This screen lets you manage the known sister-concern institutions. Staff Referrers can only be linked to one of these institutions. Let\'s take a quick walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-inst-header',
      popover: {
        title: 'Page Header',
        description:
          'The header shows the title and description. Admins will also see the <strong>Add Institution</strong> button to register a new sister concern.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-inst-search',
      popover: {
        title: 'Search Institutions',
        description:
          'Type an institution name or code to instantly filter the list. Works across both card and table views.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-inst-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between a visual <strong>Card view</strong> and a compact <strong>Table view</strong>. Your preference is saved.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-inst-content',
      popover: {
        title: 'Institution Cards / Rows',
        description:
          'Each entry shows the institution name, code, and active status. Admins can hover to reveal <strong>Edit</strong> and status-toggle actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description:
          'You now know how to navigate the Institution Master screen. Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const INSTITUTION_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Institutions', description: 'Master list of institutions under the organization, used to scope programs and infrastructure.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'Search institutions by name or code.' },
    { label: 'Browse Records', icon: 'checklist', detail: 'Cards/rows show each institution\'s name, code, and active status.' },
    { label: 'Add', icon: 'open', detail: 'Create a new institution with a name and code.' },
    { label: 'Save', icon: 'send', detail: 'Save with a real-time uniqueness check on the name and code.' },
  ],
};

export const INSTITUTION_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Institution Form',
        description:
          'This form lets you create or edit a sister-concern institution of SKSCON. Let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#inst-name',
      popover: {
        title: 'Institution Name',
        description: 'Full name of the sister-concern institution. Required and must be unique.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#inst-code',
      popover: {
        title: 'Institution Code',
        description: 'Short code used internally for this institution. Must be unique.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-inst-active',
      popover: {
        title: 'Active Status',
        description:
          'Enable to make this institution available for selection on the Staff Referrer form. Inactive institutions are hidden from new referrers but remain visible on existing ones.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-inst-submit',
      popover: {
        title: 'Save the Institution',
        description:
          'Click <strong>Save</strong> to create (or update) the institution. The button is disabled while saving.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'You know everything about the institution form. Fill in the details and hit <strong>Save</strong> to register it.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};
