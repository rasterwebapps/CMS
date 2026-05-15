import { Type } from '@angular/core';

import { WidgetPlaceholderComponent }      from './widgets/widget-placeholder/widget-placeholder.component';
import { HeroWidgetComponent }             from './widgets/hero/hero-widget.component';
import { StatCardWidgetComponent }         from './widgets/stat-card/stat-card-widget.component';
import { TrendChartWidgetComponent }       from './widgets/trend-chart/trend-chart-widget.component';
import { PendingApprovalsWidgetComponent } from './widgets/pending-approvals/pending-approvals-widget.component';
import { EquipmentStatusWidgetComponent }  from './widgets/equipment-status/equipment-status-widget.component';
import { FeeOverviewWidgetComponent }      from './widgets/fee-overview/fee-overview-widget.component';
import { QuickActionsWidgetComponent }     from './widgets/quick-actions/quick-actions-widget.component';
import { SystemHealthWidgetComponent }     from './widgets/system-health/system-health-widget.component';
import { DocStatsWidgetComponent }         from './widgets/doc-stats-widget/doc-stats-widget.component';
import { CompletionRingWidgetComponent }   from './widgets/completion-ring-widget/completion-ring-widget.component';
import { RecentActivityComponent }         from './widgets/recent-activity/recent-activity.component';
import { ConnectionsCardComponent }        from './widgets/connections-card/connections-card.component';
import { StudentQuickLinksWidgetComponent} from './widgets/student-quicklinks/student-quicklinks-widget.component';

const PH = WidgetPlaceholderComponent;

export interface WidgetDef {
  key:           string;
  label:         string;
  description:   string;
  icon:          string;
  category:      'layout' | 'stats' | 'charts' | 'lists' | 'operational';
  component:     Type<unknown>;
  defaultColSpan: 1 | 2 | 3 | 4;
  defaultRowSpan: 1 | 2;
}

export const WIDGET_REGISTRY: WidgetDef[] = [
  // ── Layout ────────────────────────────────────────────────────────────────
  { key: 'hero',               label: 'Welcome Banner',     description: 'Personalized greeting with role-specific quick stats', icon: 'waving_hand',      category: 'layout',      component: HeroWidgetComponent,             defaultColSpan: 4, defaultRowSpan: 1 },
  { key: 'quick-actions',      label: 'Quick Actions',      description: 'Role-aware shortcut tiles',                            icon: 'flash_on',         category: 'layout',      component: QuickActionsWidgetComponent,     defaultColSpan: 4, defaultRowSpan: 1 },
  // ── Stat cards ────────────────────────────────────────────────────────────
  { key: 'stat-students',      label: 'Total Students',     description: 'Enrolled student count',                               icon: 'person',           category: 'stats',       component: StatCardWidgetComponent,         defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-faculty',       label: 'Active Faculty',     description: 'Active staff members',                                 icon: 'groups',           category: 'stats',       component: StatCardWidgetComponent,         defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-labs',          label: 'Total Labs',         description: 'Operational labs',                                     icon: 'science',          category: 'stats',       component: StatCardWidgetComponent,         defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-fee-collected', label: 'Fee Collected',      description: 'Fee collected this month',                             icon: 'payments',         category: 'stats',       component: StatCardWidgetComponent,         defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-outstanding',   label: 'Outstanding Fees',   description: 'Fees pending collection',                              icon: 'warning',          category: 'stats',       component: StatCardWidgetComponent,         defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-enquiries',     label: 'Enquiries',          description: 'New enquiries this month',                             icon: 'contact_mail',     category: 'stats',       component: StatCardWidgetComponent,         defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-admissions',    label: 'Admissions',         description: 'Admissions completed this month',                      icon: 'how_to_reg',       category: 'stats',       component: StatCardWidgetComponent,         defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-departments',   label: 'Departments',        description: 'Active departments',                                   icon: 'business',         category: 'stats',       component: StatCardWidgetComponent,         defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-programs',      label: 'Programs',           description: 'Active programs',                                      icon: 'school',           category: 'stats',       component: StatCardWidgetComponent,         defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-equipment',     label: 'Equipment',          description: 'Total equipment items',                                icon: 'devices',          category: 'stats',       component: StatCardWidgetComponent,         defaultColSpan: 1, defaultRowSpan: 1 },
  // ── Charts ────────────────────────────────────────────────────────────────
  { key: 'chart-trend',        label: 'Admission Trend',    description: '6-month enrolment bar chart',                          icon: 'trending_up',      category: 'charts',      component: TrendChartWidgetComponent,       defaultColSpan: 2, defaultRowSpan: 1 },
  // ── Lists ─────────────────────────────────────────────────────────────────
  { key: 'pending-approvals',  label: 'Pending Approvals',  description: 'Outstanding items requiring attention',                 icon: 'pending_actions',  category: 'lists',       component: PendingApprovalsWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'equipment-status',   label: 'Equipment Status',   description: 'Equipment status breakdown with progress bars',        icon: 'inventory_2',      category: 'lists',       component: EquipmentStatusWidgetComponent,  defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'fee-overview',       label: 'Fee Overview',       description: 'Fee collection and outstanding summary',               icon: 'account_balance',  category: 'lists',       component: FeeOverviewWidgetComponent,      defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'system-health',      label: 'System Health',      description: 'Database, API and auth status indicators',             icon: 'monitoring',       category: 'operational', component: SystemHealthWidgetComponent,     defaultColSpan: 2, defaultRowSpan: 1 },
  // ── Shared (faculty + student) ────────────────────────────────────────────
  { key: 'doc-stats',          label: 'Document Stats',     description: 'Document completion statistics row',                   icon: 'folder_open',      category: 'stats',       component: DocStatsWidgetComponent,         defaultColSpan: 4, defaultRowSpan: 1 },
  { key: 'completion-ring',    label: 'Completion Ring',    description: 'Visual progress ring for document completion',         icon: 'donut_large',      category: 'stats',       component: CompletionRingWidgetComponent,   defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'recent-activity',    label: 'Recent Activity',    description: 'Recent system activity feed',                          icon: 'history',          category: 'lists',       component: RecentActivityComponent,         defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'colleagues',         label: 'Colleagues',         description: 'Connected faculty members or peers',                   icon: 'group',            category: 'lists',       component: ConnectionsCardComponent,        defaultColSpan: 2, defaultRowSpan: 1 },
  // ── Faculty-specific ──────────────────────────────────────────────────────
  { key: 'classes-today',      label: 'Classes & Schedule', description: "Today's classes, pending attendance, lab slots",      icon: 'calendar_today',   category: 'operational', component: PH,                              defaultColSpan: 4, defaultRowSpan: 2 },
  // ── Student-specific ──────────────────────────────────────────────────────
  { key: 'student-quicklinks', label: 'Quick Links',        description: 'Student quick-navigation shortcuts',                   icon: 'flash_on',         category: 'layout',      component: StudentQuickLinksWidgetComponent, defaultColSpan: 4, defaultRowSpan: 1 },
];

export const DEFAULT_WIDGET_KEYS: string[] = [
  'hero',
  'stat-students', 'stat-faculty', 'stat-labs',
  'stat-fee-collected', 'stat-outstanding',
  'quick-actions',
  'chart-trend', 'pending-approvals',
  'equipment-status', 'fee-overview',
];

export function widgetByKey(key: string): WidgetDef | undefined {
  return WIDGET_REGISTRY.find(w => w.key === key);
}
