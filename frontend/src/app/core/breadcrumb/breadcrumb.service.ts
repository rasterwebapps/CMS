import { Injectable, inject } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';

export interface Breadcrumb {
  label: string;
  route?: string;
}

/** Human-readable labels for URL path segments used in breadcrumbs */
const SEGMENT_LABELS: Record<string, string> = {
  dashboard: 'Dashboard',
  configure: 'Configure',
  specialities: 'Specialities',
  programs: 'Programs',
  courses: 'Courses',
  'academic-years': 'Academic Years',
  'academic-calendar': 'Academic Calendar',
  detail: 'Detail',
  labs: 'Labs',
  'fee-structures': 'Fee Structures',
  'fee-collection': 'Fee Collection',
  'fee-reports': 'Fee Reports',
  equipment: 'Equipment',
  settings: 'Settings',
  branding: 'Branding',
  integrations: 'Integrations',
  enquiries: 'Enquiries',
  admissions: 'Admission Explorer',
  agents: 'Agents',
  'referral-types': 'Referral Types',
  'staff-referrers': 'Staff Referrers',
  faculty: 'Faculty',
  students: 'Student Explorer',
  'retro-admit': 'Retro Admit',
  attendance: 'Attendance',
  examinations: 'Examinations',
  'exam-results': 'Exam Results',
  syllabi: 'Syllabi',
  experiments: 'Experiments',
  'curriculum-mappings': 'CO/PO Mapping',
  'curriculum-versions': 'Curriculum Versions',
  'curriculum-map': 'Curriculum Map',
  'lab-schedules': 'Lab Schedules',
  reports: 'Reports',
  'student-fees': 'Student Fees',
  inventory: 'Inventory',
  maintenance: 'Maintenance',
  'number-sequences': 'Number Sequences',
  receipts: 'Receipts',
  'role-management': 'Role Management',
  'user-management': 'User Management',
  'commission-explorer': 'Commission Explorer',
  communities: 'Communities',
  designations: 'Designations',
  scholarships: 'Scholarships',
  'scholarship-applications': 'Scholarship Applications',
  'blood-groups': 'Blood Groups',
  'india-locations': 'India Locations',
  countries: 'Countries',
  states: 'States',
  districts: 'Districts',
  library: 'Library',
  books: 'Books',
  periodicals: 'Periodicals',
  issues: 'Issues',
  'my-issues': 'My Issued Books',
  fines: 'Library Fines',
  import: 'Import',
  new: 'New',
  edit: 'Edit',
  'roll-numbers': 'Roll Number Assignment',
  finalize: 'Fee Finalization',
  'collect-payment': 'Collect Payment',
  convert: 'Create Admission',
  mark: 'Mark Attendance',
  search: 'Search',
  profile: 'My Profile',
  'document-submission': 'Submit Documents',
  'document-verification': 'Document Verification',
  'document-config': 'Document Config',
  'admission-completion': 'Complete Admission',
  'year-wise-fee-status': 'Year-wise Fee Status',
  'refund-approvals': 'Refund Approvals',
  timetable: 'Timetable',
  'capacity-planner': 'Capacity Planner',
  'capacity-auto-plan': 'Capacity Auto-Plan',
  'skeleton-builder': 'Skeleton Builder',
  staffing: 'Staffing',
  'conflict-inspector': 'Conflict Inspector',
  'draft-review': 'Timetable Draft Review',
  'resource-grid': 'Resource Timetable',
  'workload-rules': 'Faculty Workload Rules',
  'staff-swap': 'Staff Session Swap',
  'special-classes': 'Special Classes',
  'approval-queue': 'Special Class Approvals',
  'my-requests': 'My Special Classes',
  'assign-faculty': 'Assign Faculty',
  'faculty-absence': 'Faculty Absence',
  'faculty-availability': 'Faculty Availability',
  'elective-assignment': 'Elective Assignment',
  'my-timetable': 'My Timetable',
  'progress-report': 'Progress Report',
  'student-promotions': 'Student Promotion',
  'course-offerings': 'Course Offerings',
  'campus-infrastructure': 'Campus Infrastructure',
  classrooms: 'Classrooms',
  'clinical-venues': 'Clinical Venues',
  'holiday-templates': 'Holiday Templates',
  'hostel-room-types': 'Hostel Room Types',
  institutions: 'Institutions',
  periods: 'Periods',
  'permission-tiers': 'Permission Tiers',
  racks: 'Racks & Shelves',
  'room-allocations': 'Room Allocation',
  'room-preferences': 'Room Preferences',
  'room-purpose-categories': 'Room Purpose Categories',
  'room-sub-types': 'Room Sub-Types',
  subjects: 'Subjects',
  'floor-plans': 'Floor Plans',
  'branch-diagrams': 'Branch Diagrams',
  'zone-diagrams': 'Zone Diagrams',
  'room-diagrams': 'Room Diagrams',
  locations: 'Locations',
  shelves: 'Shelves',
};

@Injectable({ providedIn: 'root' })
export class BreadcrumbService {
  private readonly router = inject(Router);

  readonly breadcrumbs = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => this.buildCrumbs(e.urlAfterRedirects)),
    ),
    { initialValue: this.buildCrumbs(this.router.url) },
  );

  private buildCrumbs(url: string): Breadcrumb[] {
    const path = url.split('?')[0];
    const segments = path.split('/').filter((s) => s);
    if (segments.length === 0) return [];

    const crumbs: Breadcrumb[] = [{ label: 'Home', route: '/dashboard' }];
    let accumulated = '';
    const isId = (s: string) => /^(\d+|[0-9a-f-]{36})$/i.test(s);

    for (let i = 0; i < segments.length; i++) {
      const segment = segments[i];
      if (isId(segment)) {
        // Terminal ID segment → add a non-clickable "View" crumb for the current detail page
        if (i === segments.length - 1) {
          crumbs.push({ label: 'View' });
        }
        continue;
      }
      accumulated += '/' + segment;
      const label = SEGMENT_LABELS[segment] ?? segment;
      const isLast = i === segments.length - 1;
      crumbs.push({ label, route: isLast ? undefined : accumulated });
    }
    return crumbs;
  }
}
