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
import { AdmissionFunnelWidgetComponent }      from './widgets/admission-funnel/admission-funnel-widget.component';
import { FeeCollectionTargetWidgetComponent }  from './widgets/fee-collection-target/fee-collection-target-widget.component';
import { DuesAgingWidgetComponent }            from './widgets/dues-aging/dues-aging-widget.component';
import { ProgramAdmissionsWidgetComponent }    from './widgets/program-admissions/program-admissions-widget.component';
import { AgentPerformanceWidgetComponent }     from './widgets/agent-performance/agent-performance-widget.component';
import { ProgramRevenueMixWidgetComponent }    from './widgets/program-revenue-mix/program-revenue-mix-widget.component';
import { ScholarshipBurnWidgetComponent }      from './widgets/scholarship-burn/scholarship-burn-widget.component';
import { DocVerificationBacklogWidgetComponent } from './widgets/doc-verification-backlog/doc-verification-backlog-widget.component';
import { Tier3StrategicWidgetComponent }       from './widgets/tier3-strategic/tier3-strategic-widget.component';
import { Tier4AlertWidgetComponent }           from './widgets/tier4-alert/tier4-alert-widget.component';
import { GovtLapsedSeatsWidgetComponent }      from './widgets/govt-lapsed-seats/govt-lapsed-seats-widget.component';

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
  { key: 'stat-equipment',          label: 'Equipment',           description: 'Total equipment items',                                   icon: 'devices',          category: 'stats', component: StatCardWidgetComponent, defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-male-students',      label: 'Male Students',       description: 'Total male enrolled student count',                       icon: 'person',           category: 'stats', component: StatCardWidgetComponent, defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-female-students',    label: 'Female Students',     description: 'Total female enrolled student count',                     icon: 'person_outline',   category: 'stats', component: StatCardWidgetComponent, defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-management-quota',   label: 'Management Quota',    description: 'Students admitted via management quota',                  icon: 'business_center',  category: 'stats', component: StatCardWidgetComponent, defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-counselling-quota',  label: 'Counselling Quota',   description: 'Students admitted via government counselling',            icon: 'groups',           category: 'stats', component: StatCardWidgetComponent, defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-govt-lapsed-seats',      label: 'Govt. Lapsed Seats',      description: 'Unfilled counselling seats per cohort with grand total (current year)', icon: 'event_seat', category: 'stats', component: GovtLapsedSeatsWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'stat-counselling-seats-fill', label: 'Counselling Seats Filled', description: 'Counselling seats filled vs total (current academic year)',        icon: 'how_to_reg',      category: 'stats', component: StatCardWidgetComponent, defaultColSpan: 1, defaultRowSpan: 1 },
  { key: 'stat-management-seats-fill',  label: 'Management Seats Filled',  description: 'Management seats filled vs total (current academic year)',         icon: 'business_center', category: 'stats', component: StatCardWidgetComponent, defaultColSpan: 1, defaultRowSpan: 1 },
  // ── Charts ────────────────────────────────────────────────────────────────
  { key: 'chart-trend',        label: 'Admission Trend',    description: '6-month enrolment bar chart',                          icon: 'trending_up',      category: 'charts',      component: TrendChartWidgetComponent,       defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'admission-funnel',   label: 'Admission Funnel',   description: 'Enquiry → admission pipeline with stage conversion %',  icon: 'filter_alt',       category: 'charts',      component: AdmissionFunnelWidgetComponent,     defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'fee-collection-target', label: 'Fee Collection vs Target', description: 'Current-month collection gauge with month-over-month delta', icon: 'monitoring',  category: 'charts',      component: FeeCollectionTargetWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'dues-aging',         label: 'Outstanding Dues Aging', description: 'Overdue fees grouped by days outstanding (0–30, 31–60, 61–90, 90+)', icon: 'schedule',  category: 'charts',      component: DuesAgingWidgetComponent,           defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'program-admissions', label: 'Admissions by Program', description: 'Enrolled student count per active program',         icon: 'school',           category: 'charts',      component: ProgramAdmissionsWidgetComponent,   defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'program-revenue-mix', label: 'Program Revenue Mix', description: 'Donut chart of net fee revenue by program',           icon: 'pie_chart',        category: 'charts',      component: ProgramRevenueMixWidgetComponent,    defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'scholarship-burn',   label: 'Scholarship & Concession Burn', description: 'Gross fee → discount + scholarship → net collectable', icon: 'volunteer_activism', category: 'charts', component: ScholarshipBurnWidgetComponent,      defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'geographic-admissions', label: 'Geographic Admissions Heatmap', description: 'District/state-wise admissions density for recruitment planning', icon: 'map', category: 'charts', component: Tier3StrategicWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'yoy-admissions',     label: 'YoY Admission Comparison', description: 'Current year vs previous two years by admission month', icon: 'bar_chart', category: 'charts', component: Tier3StrategicWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'refund-cancellation-rate', label: 'Refund & Cancellation Rate', description: 'Refund and withdrawal KPI with 12-month trend', icon: 'sync_problem', category: 'charts', component: Tier3StrategicWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'payment-mode-breakdown', label: 'Fee Payment Mode Breakdown', description: 'UPI, cash, cheque, card and bank-transfer collection mix', icon: 'donut_large', category: 'charts', component: Tier3StrategicWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'student-faculty-ratio', label: 'Student : Faculty Ratio', description: 'Department-wise compliance ratio with threshold marker', icon: 'groups_2', category: 'charts', component: Tier3StrategicWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'lab-utilization-heatmap', label: 'Lab Utilization Heatmap', description: 'Day × slot lab-schedule density heatmap', icon: 'science', category: 'charts', component: Tier3StrategicWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'cohort-retention',   label: 'Cohort Retention', description: 'Semester-on-semester retained/enrolled students by cohort', icon: 'timeline', category: 'charts', component: Tier3StrategicWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'top-line-kpis',      label: 'Top-line KPI Strip', description: 'Compact daily admin pulse: collection, admissions, verifications, fees and conversion', icon: 'dashboard', category: 'stats', component: Tier3StrategicWidgetComponent, defaultColSpan: 4, defaultRowSpan: 1 },
  // ── Lists ─────────────────────────────────────────────────────────────────
  { key: 'agent-performance',  label: 'Agent Performance',  description: 'Top referral agents ranked by conversions',             icon: 'emoji_events',     category: 'lists',       component: AgentPerformanceWidgetComponent,    defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'doc-verification-backlog', label: 'Document Verification Backlog', description: 'Pending document verifications with oldest age + 24h throughput', icon: 'verified', category: 'operational', component: DocVerificationBacklogWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'anomaly-banner',    label: 'Collection Anomaly Banner', description: 'Passive alert when collections change sharply vs same day last week', icon: 'crisis_alert', category: 'operational', component: Tier4AlertWidgetComponent, defaultColSpan: 4, defaultRowSpan: 1 },
  { key: 'capacity-alert',    label: 'Capacity Alert', description: 'Program seat-capacity pressure alerts for high-filled programs', icon: 'event_seat', category: 'operational', component: Tier4AlertWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'compliance-alerts', label: 'Compliance Alerts', description: 'UGC/NAAC/AICTE documents expiring within 90 days', icon: 'gpp_maybe', category: 'operational', component: Tier4AlertWidgetComponent, defaultColSpan: 2, defaultRowSpan: 1 },
  { key: 'audit-mini-feed',   label: 'Audit Log Mini-feed', description: 'Last five high-privilege role, user, permission and fee changes', icon: 'manage_history', category: 'operational', component: Tier4AlertWidgetComponent, defaultColSpan: 4, defaultRowSpan: 1 },
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
