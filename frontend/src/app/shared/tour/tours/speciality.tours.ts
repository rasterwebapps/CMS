import { TourDefinition } from '../tour.service';

export const DEPT_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Specialities',
        description:
          'This screen lets you view and manage all academic specialities in the college. Let\'s walk through the key areas — it only takes a minute.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-dept-header',
      popover: {
        title: 'Page Summary',
        description:
          'At a glance: total specialities and how many have a Head of Speciality assigned. These stats update as you add or edit specialities.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-dept-add-btn',
      popover: {
        title: 'Add a Speciality',
        description:
          'Click here to open the speciality creation form. You\'ll be able to set the name, a short code, assign a Head, and add a description.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-dept-search',
      popover: {
        title: 'Search & Filter',
        description:
          'Type a speciality name, code, or HOD name here to instantly filter the list. The search works across both card and table views.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-dept-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between a visual <strong>Card view</strong> and a compact <strong>Table view</strong>. Your preference is remembered across sessions.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-dept-content',
      popover: {
        title: 'Speciality Cards',
        description:
          'Each card shows the speciality code, name, and assigned HOD. Hover over a card to reveal <strong>Edit</strong> and <strong>Delete</strong> actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ You\'re all set!',
        description:
          'You now know how to navigate the Specialities screen. You can start this tour again any time by clicking <em>Take a Tour</em>.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const DEPT_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Speciality Form',
        description:
          'This form lets you create or edit an academic speciality. We\'ll walk through each field and the live preview on the right.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#dept-name',
      popover: {
        title: 'Speciality Name',
        description:
          'Enter the full official name of the speciality — e.g., <em>General Nursing</em> or <em>Medical Surgical Nursing</em>. This is required.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-dept-code-group',
      popover: {
        title: 'Speciality Code',
        description:
          'A short uppercase identifier (max 20 characters) used across the system — e.g., <strong>GN</strong>, <strong>MSN</strong>. The preview badge on the right updates as you type.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#dept-description',
      popover: {
        title: 'Description',
        description:
          'Optional but helpful. Briefly describe the speciality\'s focus area and the programmes it offers.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#dept-hod',
      popover: {
        title: 'Head of Speciality',
        description:
          'Enter the HOD\'s full name including title — e.g., <em>Dr. Priya Sharma</em>. Their initials will appear as an avatar throughout the app.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-dept-preview',
      popover: {
        title: 'Live Preview',
        description:
          'This preview card updates in real time as you fill in the form — exactly how the speciality will look on the list screen.',
        side: 'left',
        align: 'start',
      },
    },
    {
      element: '#tour-dept-tips',
      popover: {
        title: 'Guidance Tips',
        description:
          'These tips explain best practices for each field — keep them in mind when setting up a speciality.',
        side: 'left',
        align: 'start',
      },
    },
    {
      element: '#tour-dept-submit',
      popover: {
        title: 'Save the Speciality',
        description:
          'When all required fields are filled, click here to create (or update) the speciality. The button is disabled while saving.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'You know everything about the speciality form. Fill in the details and hit <strong>Create Speciality</strong> to get started.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};
