import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// Dashboard (role-based widget grid, Overview nav group)
// ─────────────────────────────────────────────────────────────────────────────
export const DASHBOARD_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🏠 Your Dashboard',
        description:
          'This is your home screen — the widgets shown depend on your role (admin, faculty, front office, or student), so what you see here may differ from a colleague\'s dashboard.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-dash-grid',
      popover: {
        title: 'Widgets',
        description:
          'Each tile is a live widget — quick counts, shortcuts, or a summary pulled straight from the module it represents. Click through a widget to open its full screen.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Make it yours',
        description:
          'If you have customize access, look for the Customize button to rearrange, resize, or swap out widgets for the ones you use most.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const DASHBOARD_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'Dashboard', description: 'Your home screen — a role-based widget grid summarizing what matters most to you.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'Role-Based Widgets', icon: 'search', detail: 'The widgets shown depend on your role — admin, faculty, front office, or student.' },
    { label: 'Open a Widget', icon: 'open', detail: 'Click through any tile to jump straight to its full screen.' },
    { label: 'Customize', icon: 'checklist', detail: 'If permitted, rearrange, resize, or swap widgets to fit how you actually work.' },
  ],
};
