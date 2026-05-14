export interface WidgetDef {
  key: string;
  label: string;
  description: string;
  icon: string;
  category: 'layout' | 'stats' | 'charts' | 'lists';
}

export const WIDGET_REGISTRY: WidgetDef[] = [
  // ── Layout ───────────────────────────────────────────────────────────
  { key: 'hero',               label: 'Welcome Hero',       description: 'Personalized welcome banner with academic year',  icon: 'waving_hand',      category: 'layout' },
  { key: 'quick-actions',      label: 'Quick Actions',      description: 'Shortcut buttons to common tasks',                icon: 'flash_on',         category: 'layout' },
  // ── Stat cards ───────────────────────────────────────────────────────
  { key: 'stat-students',      label: 'Total Students',     description: 'Enrolled student count',                          icon: 'person',           category: 'stats' },
  { key: 'stat-faculty',       label: 'Active Faculty',     description: 'Active staff members',                            icon: 'groups',           category: 'stats' },
  { key: 'stat-labs',          label: 'Total Labs',         description: 'Operational labs',                                icon: 'science',          category: 'stats' },
  { key: 'stat-fee-collected', label: 'Fee Collected',      description: 'Fee collected this month',                        icon: 'payments',         category: 'stats' },
  { key: 'stat-outstanding',   label: 'Outstanding Fees',   description: 'Fees pending collection',                         icon: 'warning',          category: 'stats' },
  { key: 'stat-enquiries',     label: 'Enquiries',          description: 'New enquiries this month',                        icon: 'contact_mail',     category: 'stats' },
  { key: 'stat-admissions',    label: 'Admissions',         description: 'Admissions completed this month',                 icon: 'how_to_reg',       category: 'stats' },
  { key: 'stat-departments',   label: 'Departments',        description: 'Active departments',                              icon: 'business',         category: 'stats' },
  { key: 'stat-programs',      label: 'Programs',           description: 'Active programs',                                 icon: 'school',           category: 'stats' },
  { key: 'stat-equipment',     label: 'Equipment',          description: 'Total equipment items',                           icon: 'devices',          category: 'stats' },
  // ── Charts ───────────────────────────────────────────────────────────
  { key: 'chart-trend',        label: 'Admission Trend',    description: '6-month enrolment bar chart',                     icon: 'trending_up',      category: 'charts' },
  // ── Lists / detail cards ─────────────────────────────────────────────
  { key: 'pending-approvals',  label: 'Pending Approvals',  description: 'Outstanding items requiring attention',            icon: 'pending_actions',  category: 'lists' },
  { key: 'equipment-status',   label: 'Equipment Status',   description: 'Equipment status breakdown with progress bars',   icon: 'inventory_2',      category: 'lists' },
  { key: 'fee-overview',       label: 'Fee Overview',       description: 'Fee collection and outstanding summary',          icon: 'account_balance',  category: 'lists' },
  // ── Faculty & Student shared widgets ─────────────────────────────────
  { key: 'doc-stats',          label: 'Document Stats',     description: 'Document completion statistics row',              icon: 'folder_open',      category: 'stats' },
  { key: 'completion-ring',    label: 'Completion Ring',    description: 'Visual progress ring for document completion',    icon: 'donut_large',      category: 'stats' },
  { key: 'recent-activity',    label: 'Recent Activity',    description: 'Recent system activity feed',                     icon: 'history',          category: 'lists' },
  { key: 'colleagues',         label: 'Colleagues',         description: 'Connected faculty members or peers',              icon: 'group',            category: 'lists' },
  // ── Faculty-specific ─────────────────────────────────────────────────
  { key: 'classes-today',      label: 'Classes & Schedule', description: 'Today\'s classes, pending attendance, lab slots', icon: 'calendar_today',   category: 'lists' },
  // ── Student-specific ─────────────────────────────────────────────────
  { key: 'student-quicklinks', label: 'Quick Links',        description: 'Student quick-navigation shortcuts',              icon: 'flash_on',         category: 'layout' },
];

/** Widget keys shown by default when a role has no saved configuration. */
export const DEFAULT_WIDGET_KEYS: string[] = [
  'hero',
  'stat-students',
  'stat-faculty',
  'stat-labs',
  'stat-fee-collected',
  'stat-outstanding',
  'quick-actions',
  'chart-trend',
  'pending-approvals',
  'equipment-status',
  'fee-overview',
];

export function widgetByKey(key: string): WidgetDef | undefined {
  return WIDGET_REGISTRY.find((w) => w.key === key);
}
