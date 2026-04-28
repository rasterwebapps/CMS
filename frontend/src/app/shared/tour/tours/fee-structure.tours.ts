import { TourDefinition } from '../tour.service';

export const FEE_STRUCTURE_LIST_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '💰 Fee Structures',
        description:
          'This screen lets you manage fee schedules for academic years, programs, and courses. Let\'s walk through the key controls.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-fs-header',
      popover: {
        title: 'Page Summary',
        description:
          'At a glance: total fee structures and the combined fee amount configured across them.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-fs-add-btn',
      popover: {
        title: 'Add Fee Structure',
        description:
          'Click here to create a new fee schedule for an academic year, program, and optional course.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-fs-filters',
      popover: {
        title: 'Filter Fee Structures',
        description:
          'Narrow the list by academic year, program, and course to quickly find the schedule you need.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-fs-search',
      popover: {
        title: 'Search Fee Structures',
        description: 'Search by program, course, or academic year across both card and table views.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-fs-view-toggle',
      popover: {
        title: 'Switch Views',
        description:
          'Toggle between <strong>Card view</strong> and <strong>Table view</strong>. Your preference is remembered.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-fs-content',
      popover: {
        title: 'Fee Structure Cards',
        description:
          'Each card shows program, course, academic year badge, fee type count, and grand total. Hover to edit or delete.',
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

export const FEE_STRUCTURE_FORM_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '📊 Fee Structure Form',
        description:
          'Configure year-wise fee schedules for a program and course. Let\'s walk through the important sections.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-fs-criteria',
      popover: {
        title: 'Selection Criteria',
        description:
          'Pick the academic year, program, and optional course this fee schedule applies to.',
        side: 'bottom',
        align: 'start',
      },
    },
    { element: '#fs-ay', popover: { title: 'Academic Year', description: 'Select the academic year for this fee structure.', side: 'bottom', align: 'start' } },
    { element: '#fs-program', popover: { title: 'Program', description: 'Select the program. The grid adapts to the program duration.', side: 'bottom', align: 'start' } },
    { element: '#fs-course', popover: { title: 'Course', description: 'Optionally bind the schedule to a specific course, or leave it as program-level.', side: 'bottom', align: 'start' } },
    {
      element: '#tour-fs-course-fees',
      popover: {
        title: 'Course Fees',
        description:
          'Enter year-wise amounts for tuition and core fee types. Row totals and grand totals update live.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-fs-additional-fees',
      popover: {
        title: 'Additional Fees',
        description:
          'Optional charges like hostel and transport support per-year amounts and notes without changing core course totals.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-fs-submit',
      popover: {
        title: 'Save Fee Structure',
        description: 'Click here to save. The button is disabled while saving is in progress.',
        side: 'top',
        align: 'end',
      },
    },
    {
      popover: {
        title: '✅ Ready to go!',
        description: 'Review the totals and hit <strong>Save Fee Structure</strong> to finish.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

