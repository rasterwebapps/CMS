import { TourDefinition, TourFlowMap } from '../tour.service';

export const FACULTY_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Faculty',
        description:
          'This screen lets you manage faculty members, designations, and speciality assignments. Quick walkthrough of the controls.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-fac-header',
      popover: {
        title: 'Page Summary',
        description:
          'Total faculty count and how many are currently <strong>Active</strong>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-fac-add-btn',
      popover: {
        title: 'Add a Faculty Member',
        description:
          'Click here to register a new faculty member with employee code, contact info, speciality, designation, and joining date.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-fac-filters',
      popover: {
        title: 'Filter by Speciality & Status',
        description:
          'Narrow the list by <strong>Speciality</strong> or <strong>Status</strong> (Active, On Leave, etc.).',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-fac-search',
      popover: {
        title: 'Search Faculty',
        description:
          'Quickly filter by name, employee code, email, or speciality.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-fac-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between <strong>Card</strong> and <strong>Table</strong> view. Your preference is remembered.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-fac-content',
      popover: {
        title: 'Faculty Cards',
        description:
          'Each card shows the employee code, full name, designation, speciality badge, and current status. Click a card to view full details.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ You\'re all set!',
        description:
          'You now know how to navigate Faculty. Replay this tour any time via <em>Take a Tour</em>.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const FACULTY_LIST_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Faculty', description: 'Master list of faculty members — speciality, designation, and contact details used across timetable and academics.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Filter & Search', icon: 'search', detail: 'Filter by speciality and status, or search faculty by name.' },
    { label: 'Faculty Cards', icon: 'checklist', detail: 'Each card shows employee code, speciality, designation, and contact details.' },
    { label: 'Add a Faculty Member', icon: 'open', detail: 'Register a new faculty member with employee code, speciality, designation, and specialization/lab expertise.' },
    { label: 'Save', icon: 'send', detail: 'Save with a real-time uniqueness check on the employee code and email.' },
  ],
};

export const FACULTY_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Faculty Form',
        description:
          'This form lets you create or edit a faculty member. We\'ll walk through the key fields.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#fac-code',
      popover: {
        title: 'Employee Code',
        description:
          'Unique employee identifier — e.g., <em>EMP001</em>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#fac-email',
      popover: {
        title: 'Email',
        description:
          'Official email address. Required for login and notifications.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#fac-first',
      popover: {
        title: 'First Name',
        description:
          'Faculty member\'s first name.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#fac-last',
      popover: {
        title: 'Last Name',
        description:
          'Faculty member\'s last (family) name.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#fac-phone',
      popover: {
        title: 'Phone',
        description:
          'Optional contact number with country code, e.g., <em>+91 9876543210</em>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#fac-dept',
      popover: {
        title: 'Speciality',
        description:
          'Assign the faculty member to a speciality. Required.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#fac-designation',
      popover: {
        title: 'Designation',
        description:
          'Pick the role — Professor, Associate Professor, Assistant Professor, etc.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#fac-joining',
      popover: {
        title: 'Joining Date',
        description:
          'Date the faculty member joined the college.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#fac-specialization',
      popover: {
        title: 'Specialization',
        description:
          'Subject expertise areas, e.g., <em>Artificial Intelligence, Data Science</em>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#fac-expertise',
      popover: {
        title: 'Lab Expertise',
        description:
          'Describe lab equipment knowledge and technical skills — used for lab assignments and incharge selection.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-fac-submit',
      popover: {
        title: 'Save the Faculty Member',
        description:
          'Click here to create (or update) the faculty member.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'Hit <strong>Create Faculty</strong> to add the member.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const FACULTY_FORM_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Faculty', description: 'Master list of faculty members — speciality, designation, and contact details used across timetable and academics.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Identity & Contact', icon: 'checklist', detail: 'Employee code, email, name, and phone.' },
    { label: 'Speciality & Designation', icon: 'open', detail: 'Assign a speciality and designation, plus joining date.' },
    { label: 'Specialization & Expertise', icon: 'checklist', detail: 'Subject expertise and lab equipment knowledge — used for lab assignments and incharge selection.' },
    { label: 'Save', icon: 'send', detail: 'Save with a real-time uniqueness check on the employee code and email.' },
  ],
};

