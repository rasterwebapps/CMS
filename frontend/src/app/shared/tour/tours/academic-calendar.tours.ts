import { TourDefinition } from '../tour.service';

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
          'At a glance: total weeks, number of terms, teaching days, holidays, and exam days in the selected academic year.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-cal-terms',
      popover: {
        title: 'Term Timeline',
        description:
          'Each term is shown with its start and end dates, status (Upcoming / Ongoing / Completed), and a visual progress bar. Admins can add term instances via the <strong>Add Term</strong> button.',
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
          'Each event is colour-coded by type — <em>holiday</em>, <em>exam</em>, <em>institute event</em>, <em>term start/end</em>. Admins can edit or delete events using the action icons.',
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

