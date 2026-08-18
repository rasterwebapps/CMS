import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// System Settings
// ─────────────────────────────────────────────────────────────────────────────
export const SETTINGS_SHELL_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '⚙️ System Settings',
        description: 'Application configuration, branding, and external integrations, in one place.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-set-tabs',
      popover: {
        title: 'Three Tabs',
        description: 'Configuration holds arbitrary key/value app settings. Branding controls the app\'s name, logo, and colors. Integrations manages connections to external services.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Save Changes',
        description: 'Configuration entries are added/edited individually. Branding and Integrations are each saved as a whole via the Save Changes button in the header.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const SETTINGS_SHELL_FLOW_MAP: TourFlowMap = {
  funnel: [{ label: 'Settings', description: 'Application configuration, branding, and external integrations.' }],
  currentIndex: 0,
  steps: [
    { label: 'Configuration', icon: 'checklist', detail: 'Arbitrary key/value app settings, added and edited individually.' },
    { label: 'Branding', icon: 'open', detail: 'App name, logo, and colors — saved as a whole.' },
    { label: 'Integrations', icon: 'open', detail: 'Connections to external services — saved as a whole.' },
    { label: 'Save', icon: 'send', detail: 'Save Changes commits the active tab\'s edits.' },
  ],
};
