import { TourDefinition, TourFlowMap } from '../tour.service';

// Users → Roles & Permissions → Permission Tiers, a real hierarchy (a user holds a role, a role
// holds permissions, and Permission Tiers governs which roles a permission can even be assigned to).
export const USER_MANAGEMENT_FUNNEL = [
  { label: 'Users', description: 'System user accounts — create accounts and assign each a role.' },
  { label: 'Roles & Permissions', description: 'Configure roles and the granular permissions each holds.' },
  { label: 'Permission Tiers', description: 'Control which roles are even allowed to hold and delegate each permission.' },
];

// ─────────────────────────────────────────────────────────────────────────────
// Users
// ─────────────────────────────────────────────────────────────────────────────
export const USER_MANAGEMENT_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '👤 User Management',
        description: 'Create and manage system user accounts, and assign each one a role.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-um-toolbar',
      popover: {
        title: 'Search',
        description: 'Search by name, email, or role.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-um-list',
      popover: {
        title: 'Users',
        description: 'Each card shows the user\'s role and active status. Deactivate suspends login without deleting the account; Edit updates their name, email, or role.',
        side: 'right',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Roles Come From Role Management',
        description: 'The role dropdown here only offers roles already defined in Roles & Permissions — a role\'s actual permissions are configured there, not here.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const USER_MANAGEMENT_FLOW_MAP: TourFlowMap = {
  funnel: USER_MANAGEMENT_FUNNEL,
  currentIndex: 0,
  steps: [
    { label: 'Search', icon: 'search', detail: 'By name, email, or role.' },
    { label: 'Add User', icon: 'open', detail: 'Full name, email, username, temporary password, and role.' },
    { label: 'Edit / Deactivate', icon: 'checklist', detail: 'Update details from the row actions, or deactivate to suspend login.' },
    { label: 'Role Comes from Role Management', icon: 'send', detail: 'Pick from roles already defined — permissions are configured separately.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Roles & Permissions
// ─────────────────────────────────────────────────────────────────────────────
export const ROLE_MANAGEMENT_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🛡️ Role Management',
        description: 'Configure roles and assign granular, per-module permissions to each one.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-rm-roles',
      popover: {
        title: 'Roles',
        description: 'Click any role to open its permission editor. System roles are marked and can\'t be deleted.',
        side: 'right',
        align: 'start',
      },
    },
    {
      element: '#tour-rm-editor',
      popover: {
        title: 'Permission Matrix',
        description: 'Permissions are grouped by module, then by screen. Toggle a whole group at once with its master checkbox, or check individual permissions — the progress bar shows how many are selected per group.',
        side: 'left',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Dashboard Widgets Too',
        description: 'The Dashboard button in the permission editor configures which dashboard widgets this role\'s users see — a separate concern from permissions, but part of the same role.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const ROLE_MANAGEMENT_FLOW_MAP: TourFlowMap = {
  funnel: USER_MANAGEMENT_FUNNEL,
  currentIndex: 1,
  steps: [
    { label: 'Pick a Role', icon: 'search', detail: 'Click any role in the left column, or Add Role to create a new one.' },
    { label: 'Toggle Permissions', icon: 'checklist', detail: 'Grouped by module and screen; toggle a whole group or individual permissions.' },
    { label: 'Save', icon: 'send', detail: 'Save Permissions commits the change immediately.' },
    { label: 'Dashboard Widgets', icon: 'open', detail: 'Configure which widgets this role\'s users see on their dashboard.' },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// Permission Tiers
// ─────────────────────────────────────────────────────────────────────────────
export const PERMISSION_TIER_TOUR: TourDefinition = {
  steps: [
    {
      popover: {
        title: '🎚️ Permission Tiers',
        description: 'Control which roles are even allowed to hold and delegate each permission — a gate above Role Management\'s per-role checkboxes. Every change here is audit-logged.',
        side: 'over',
        align: 'center',
      },
    },
    {
      element: '#tour-pt-legend',
      popover: {
        title: 'Tier Legend',
        description: 'Each tier (T1, T2, …) represents a level of trust — what it means is described here.',
        side: 'bottom',
        align: 'start',
      },
    },
    {
      element: '#tour-pt-matrix',
      popover: {
        title: 'Permission Matrix',
        description: 'Grouped by module, then screen. Pick a tier for each permission — this sets the ceiling for which roles may ever hold it, independent of what\'s toggled in Role Management.',
        side: 'top',
        align: 'start',
      },
    },
    {
      popover: {
        title: '✅ Impact Preview Before Saving',
        description: 'Saving shows a confirmation with exactly which roles (and how many users) will lose a permission immediately if their role no longer qualifies for its new tier.',
        side: 'over',
        align: 'center',
      },
    },
  ],
};

export const PERMISSION_TIER_FLOW_MAP: TourFlowMap = {
  funnel: USER_MANAGEMENT_FUNNEL,
  currentIndex: 2,
  steps: [
    { label: 'Review Tiers', icon: 'checklist', detail: 'The tier legend explains what each level of trust means.' },
    { label: 'Set a Tier', icon: 'open', detail: 'Pick a tier per permission — the ceiling for which roles may ever hold it.' },
    { label: 'Preview Impact', icon: 'search', detail: 'See exactly which roles/users lose access before committing.' },
    { label: 'Save', icon: 'send', detail: 'Changes apply immediately and are audit-logged.' },
  ],
};
