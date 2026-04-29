import { TourDefinition } from '../tour.service';

export const DEPT_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Departments',
        description:
          'This screen lets you view and manage all academic departments in the college. Let\'s walk through the key areas — it only takes a minute.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-dept-header',
      popover: {
        title: 'Page Summary',
        description:
          'At a glance: total departments and how many have a Head of Department assigned. These stats update as you add or edit departments.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-dept-add-btn',
      popover: {
        title: 'Add a Department',
        description:
          'Click here to open the department creation form. You\'ll be able to set the name, a short code, assign a Head, and add a description.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-dept-search',
      popover: {
        title: 'Search & Filter',
        description:
          'Type a department name, code, or HOD name here to instantly filter the list. The search works across both card and table views.',
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
        title: 'Department Cards',
        description:
          'Each card shows the department code, name, and assigned HOD. Hover over a card to reveal <strong>Edit</strong> and <strong>Delete</strong> actions.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ You\'re all set!',
        description:
          'You now know how to navigate the Departments screen. You can start this tour again any time by clicking <em>Take a Tour</em>.',
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
        title: '📋 Department Form',
        description:
          'This form lets you create or edit an academic department. We\'ll walk through each field and the live preview on the right.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#dept-name',
      popover: {
        title: 'Department Name',
        description:
          'Enter the full official name of the department — e.g., <em>General Nursing</em> or <em>Medical Surgical Nursing</em>. This is required.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-dept-code-group',
      popover: {
        title: 'Department Code',
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
          'Optional but helpful. Briefly describe the department\'s focus area and the programmes it offers.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#dept-hod',
      popover: {
        title: 'Head of Department',
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
          'This preview card updates in real time as you fill in the form — exactly how the department will look on the list screen.',
        side: 'left',
        align: 'start',
      },
    },
    {
      element: '#tour-dept-tips',
      popover: {
        title: 'Guidance Tips',
        description:
          'These tips explain best practices for each field — keep them in mind when setting up a department.',
        side: 'left',
        align: 'start',
      },
    },
    {
      element: '#tour-dept-submit',
      popover: {
        title: 'Save the Department',
        description:
          'When all required fields are filled, click here to create (or update) the department. The button is disabled while saving.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'You know everything about the department form. Fill in the details and hit <strong>Create Department</strong> to get started.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};
