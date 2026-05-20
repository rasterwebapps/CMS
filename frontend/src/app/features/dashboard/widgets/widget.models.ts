import { ActivityItem } from '../dashboard.models';

export interface ConnectionItem {
  id: number;
  name: string;
  initials: string;
  role: string;
  online: boolean;
}

export interface AdminQuickLink {
  icon: string;
  label: string;
  route: string;
  desc: string;
  cc: string;
}

/**
 * Static placeholder activity feed shown until `GET /api/dashboard/activity`
 * is implemented backend-side. Same payload shape as {@link ActivityItem}
 * so swapping in the real endpoint is a one-line change.
 */
export const SAMPLE_ACTIVITY: Pick<ActivityItem, 'id' | 'action' | 'actor' | 'timestamp'>[] = [
  { id: 1, action: 'Appointment Letter verified', actor: 'System', timestamp: new Date(Date.now() - 2 * 86400000).toISOString() },
  { id: 2, action: 'PG Degree document uploaded',  actor: 'You',    timestamp: new Date(Date.now() - 5 * 86400000).toISOString() },
  { id: 3, action: 'Aadhaar Card verified',         actor: 'System', timestamp: new Date(Date.now() - 7 * 86400000).toISOString() },
  { id: 4, action: 'Transfer Cert. rejected',       actor: 'Admin',  timestamp: new Date(Date.now() - 14 * 86400000).toISOString() },
  { id: 5, action: 'UG Degree uploaded',            actor: 'You',    timestamp: new Date(Date.now() - 21 * 86400000).toISOString() },
];

export const SAMPLE_CONNECTIONS: ConnectionItem[] = [
  { id: 1, name: 'Dr. Ananya Kumar',  initials: 'AK', role: 'Professor',           online: true },
  { id: 2, name: 'Prof. Meena Devi',  initials: 'MD', role: 'Associate Professor', online: true },
  { id: 3, name: 'Dr. Ramesh Iyer',   initials: 'RI', role: 'Head of Department',  online: false },
  { id: 4, name: 'Ms. Preethi S.',    initials: 'PS', role: 'Lecturer',            online: true },
];

export const ADMIN_QUICK_LINKS: AdminQuickLink[] = [
  { icon: 'school',                 label: 'Programs',    route: '/programs',                      desc: 'Academic programs',     cc: 'indigo'  },
  { icon: 'business',               label: 'Departments', route: '/departments',                   desc: 'Manage departments',    cc: 'violet'  },
  { icon: 'groups',                 label: 'Faculty',     route: '/faculty',                       desc: 'Faculty & docs',        cc: 'blue'    },
  { icon: 'person',                 label: 'Student Explorer',    route: '/students',              desc: 'Student records',       cc: 'sky'     },
  { icon: 'contact_mail',           label: 'Enquiries',   route: '/enquiries',                     desc: 'Admissions funnel',     cc: 'teal'    },
  { icon: 'assignment_ind',         label: 'Admission Explorer',  route: '/admissions',            desc: 'Student admissions',    cc: 'emerald' },
  { icon: 'upload_file',            label: 'Documents',   route: '/enquiries/document-submission', desc: 'Submit & verify',       cc: 'cyan'    },
  { icon: 'account_balance_wallet', label: 'Fees',        route: '/student-fees',                  desc: 'Fee management',        cc: 'rose'    },
  { icon: 'manage_accounts',        label: 'Users',       route: '/user-management',               desc: 'Roles & access',        cc: 'pink'    },
  { icon: 'assessment',             label: 'Reports',     route: '/reports',                       desc: 'Academic & financial',  cc: 'amber'   },
];

