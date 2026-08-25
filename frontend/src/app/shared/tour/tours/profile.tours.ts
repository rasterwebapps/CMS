import { TourDefinition, TourFlowMap } from '../tour.service';

// ─────────────────────────────────────────────────────────────────────────────
// My Profile (Overview nav group — self-service identity screen for all roles)
// ─────────────────────────────────────────────────────────────────────────────
export const PROFILE_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🪪 My Profile',
        description:
          'Your own identity, contact details, and preferences — what shows here adapts to whether you\'re an admin, faculty member, or student.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-profile-actions',
      popover: {
        title: 'Edit Your Profile',
        description:
          'Update your bio, contact details, and photo here. Some fields may only be editable by an administrator — an "Admin Edit" shortcut appears if you have that access.',
        side: 'bottom',
        align: 'end',
      },
    },
    {
      element: '#tour-profile-grid',
      popover: {
        title: 'About, Security & More',
        description:
          'Below the header: your bio, account security (password/session), notification preferences, and — for faculty/students — role-specific details like qualifications or program info.',
        side: 'top',
        align: 'start',
      },
    },
  ],
};

export const PROFILE_FLOW_MAP: TourFlowMap = {
  funnel: [
    { label: 'My Profile', description: 'Your own identity, contact details, and preferences.' },
  ],
  currentIndex: 0,
  steps: [
    { label: 'View Identity', icon: 'search', detail: 'Name, role, and role-specific summary shown at the top.' },
    { label: 'Edit Profile', icon: 'open', detail: 'Update your bio, contact details, and photo.' },
    { label: 'About & Security', icon: 'checklist', detail: 'Bio, account security, and notification preferences below the header.' },
    { label: 'Save', icon: 'send', detail: 'Changes are saved directly from the edit form.' },
  ],
};
