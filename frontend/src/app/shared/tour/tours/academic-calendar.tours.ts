import { TourDefinition, TourFlowMap } from '../tour.service';

export const ACADEMIC_CALENDAR_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👋 Welcome to the Academic Calendar',
        description:
          'This screen gives a visual overview of the academic year — terms, holidays, exams, and key events — so faculty and admin can plan their semester at a glance.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-cal-header',
      popover: {
        title: 'Page Controls',
        description:
          'Use the <strong>Year Selector</strong> dropdown to switch between academic years. Admins can also <strong>Add Events</strong>, export to CSV, print the calendar, or navigate to the Academic Years master.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cal-stats',
      popover: {
        title: 'Stats Strip',
        description:
          'At a glance: total weeks, number of terms, days left in the current term, and total calendar events for the selected academic year.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cal-terms',
      popover: {
        title: 'Term Timeline',
        description:
          'Each term is shown with start and end dates, status (Upcoming / Ongoing / Completed), and progress. Related events appear under each term in timeline mode.',
        side: 'top',
        align: 'start',
      },
    },
    {
      element: '#tour-cal-view-toggle',
      popover: {
        title: 'Timeline vs Grid View',
        description:
          'Switch between a vertical <strong>Timeline</strong> view of events and a month-by-month <strong>Grid</strong> view. Both views show the same events — choose whichever helps you plan best.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-cal-events',
      popover: {
        title: 'Events & Holidays',
        description:
          'Upcoming events are highlighted and all events are colour-coded by type (holiday, exam, cultural, sports, workshop, other). Admins can edit or delete events using the action icons.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ All done!',
        description:
          'You now know how to use the Academic Calendar. Click <em>Take a Tour</em> any time to replay this walkthrough.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const ACADEMIC_CALENDAR_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Academic Calendar', description: 'Visual overview of an academic year\'s terms, holidays, exams, and key events.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Pick a Year', icon: 'search', detail: 'Switch academic years with the Year Selector, or export/print the calendar.' },
    { label: 'Term Timeline', icon: 'checklist', detail: 'Each term shows start/end dates, status, and progress, with related events underneath.' },
    { label: 'Timeline or Grid', icon: 'open', detail: 'Switch between a vertical Timeline view and a month-by-month Grid view of the same events.' },
    { label: 'Manage Events', icon: 'send', detail: 'Add, edit, or delete events — colour-coded by type (holiday, exam, cultural, sports, workshop, other).' },
  ],
};

