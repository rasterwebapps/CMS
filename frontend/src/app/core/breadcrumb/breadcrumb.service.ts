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
  departments: 'Departments',
  programs: 'Programs',
  courses: 'Courses',
  'academic-years': 'Academic Years',
  semesters: 'Semesters',
  'academic-calendar': 'Academic Calendar',
  labs: 'Labs',
  'fee-structures': 'Fee Structures',
  equipment: 'Equipment',
  settings: 'Settings',
  enquiries: 'Enquiries',
  admissions: 'Admissions',
  agents: 'Agents',
  'referral-types': 'Referral Types',
  faculty: 'Faculty',
  students: 'Students',
  attendance: 'Attendance',
  examinations: 'Examinations',
  'exam-results': 'Exam Results',
  syllabi: 'Syllabi',
  experiments: 'Experiments',
  'curriculum-mappings': 'CO/PO Mapping',
  'lab-schedules': 'Lab Schedules',
  reports: 'Reports',
  'student-fees': 'Student Fees',
  'fee-payments': 'Fee Payments',
  inventory: 'Inventory',
  maintenance: 'Maintenance',
  new: 'New',
  edit: 'Edit',
  'roll-numbers': 'Roll Number Assignment',
  finalize: 'Fee Finalization',
  'collect-payment': 'Collect Payment',
  convert: 'Convert to Student',
  mark: 'Mark Attendance',
  search: 'Search',
  'document-submission': 'Document Submission',
  'year-wise-fee-status': 'Year-wise Fee Status',
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
      if (isId(segment)) continue;
      accumulated += '/' + segment;
      const label = SEGMENT_LABELS[segment] ?? segment;
      const nextIsId = i + 1 < segments.length && isId(segments[i + 1]);
      const isLast = i === segments.length - 1 || (i === segments.length - 2 && nextIsId);
      crumbs.push({ label, route: isLast ? undefined : accumulated });
    }
    return crumbs;
  }
}
