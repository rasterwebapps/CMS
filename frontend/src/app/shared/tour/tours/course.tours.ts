import { TourDefinition } from '../tour.service';

export const COURSE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to Courses',
        description:
          'This screen lists every course offered across all programmes and specializations. Let\'s do a quick walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-course-header',
      popover: {
        title: 'Page Summary',
        description:
          'Total number of courses and how many unique specializations exist across them.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-course-add-btn',
      popover: {
        title: 'Add a Course',
        description:
          'Click here to register a new course — set the name, code, optional specialization, and the parent programme.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-course-filters',
      popover: {
        title: 'Filter by Program',
        description:
          'Narrow the list to courses belonging to a specific degree programme.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-course-search',
      popover: {
        title: 'Search Courses',
        description:
          'Quickly filter by course name, code, or specialization.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-course-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between <strong>Card</strong> and <strong>Table</strong> view. Your preference is remembered across sessions.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-course-content',
      popover: {
        title: 'Course Cards',
        description:
          'Each card shows the code, name, specialization tag, and parent programme. Hover to <strong>Edit</strong> or <strong>Delete</strong>.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ You\'re all set!',
        description:
          'You now know how to navigate the Courses screen. Replay this tour any time via <em>Take a Tour</em>.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const COURSE_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📋 Course Form',
        description:
          'This form lets you create or edit a course. We\'ll walk through each field together.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#course-name',
      popover: {
        title: 'Course Name',
        description:
          'Enter the full official course name — e.g., <em>Data Structures</em>. This field is required.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#course-code',
      popover: {
        title: 'Course Code',
        description:
          'A short identifier for the course — e.g., <strong>CS101</strong>.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#course-specialization',
      popover: {
        title: 'Specialization',
        description:
          'Optional specialization within the course, e.g., <em>Obs Gyn</em>. Leave blank if not applicable.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#course-program',
      popover: {
        title: 'Parent Program',
        description:
          'Required — pick the degree programme this course belongs to.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-course-submit',
      popover: {
        title: 'Save the Course',
        description:
          'When all required fields are filled, click here to create (or update) the course.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description:
          'You know everything about the course form. Hit <strong>Create Course</strong> to add it.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

