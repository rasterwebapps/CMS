import { TourDefinition } from '../tour.service';

export const BLOOD_GROUP_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Blood Group Master',
        description:
          'This screen lets you manage blood group types used in student health records — e.g., A+, O−, AB+. Let\'s take a quick walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-bg-header',
      popover: {
        title: 'Page Header',
        description:
          'The header shows the title and description. Admins will also see the <strong>Add Blood Group</strong> button to register a new type.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-bg-search',
      popover: {
        title: 'Search Blood Groups',
        description:
          'Type a blood group name or code to instantly filter the list. Works across both card and table views.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-bg-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between a visual <strong>Card view</strong> and a compact <strong>Table view</strong>. Your preference is saved.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-bg-content',
      popover: {
        title: 'Blood Group Cards / Rows',
        description:
          'Each entry shows the blood group name, code, and active status. Admins can hover to reveal <strong>Edit</strong> and <strong>Delete</strong> actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description:
          'You now know how to navigate the Blood Group Master screen. Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const BLOOD_GROUP_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Blood Group Form',
        description:
          'This form lets you create or edit a blood group type for student records. Let\'s walk through each field.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#bg-name',
      popover: {
        title: 'Blood Group Name',
        description:
          'Full descriptive name of the blood group — e.g., <em>A Positive</em>, <em>O Negative</em>. Required and must be unique.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#bg-code',
      popover: {
        title: 'Blood Group Code',
        description:
          'Short clinical code shown in dropdowns — e.g., <strong>A+</strong>, <strong>O−</strong>, <strong>AB+</strong>. Must be unique.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-bg-active',
      popover: {
        title: 'Active Status',
        description:
          'Enable to make this blood group available for selection in student admission forms. Inactive groups are hidden from dropdowns.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-bg-submit',
      popover: {
        title: 'Save the Blood Group',
        description:
          'Click <strong>Save</strong> to create (or update) the blood group. The button is disabled while saving.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'You know everything about the blood group form. Fill in the details and hit <strong>Save</strong> to register it.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

