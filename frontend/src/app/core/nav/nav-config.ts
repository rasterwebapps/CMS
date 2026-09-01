export interface NavItem {
  label: string;
  icon: string;
  route: string;
  /** DB permission codes — show only if user holds at least one. Empty/absent = visible to all. */
  permissions?: string[];
}

export interface NavGroup {
  label: string;
  icon: string;
  items: NavItem[];
  /** DB permission codes — show only if user holds at least one. Empty/absent = visible to all. */
  permissions?: string[];
}

export type NavEntry = NavItem | NavGroup;

export function isNavGroup(entry: NavEntry): entry is NavGroup {
  return 'items' in entry;
}

/**
 * Canonical module/menu-item structure and order for the app's sidenav.
 * Also the single source of truth for "menu order" used to order permission
 * groupings on the Permission Tiers and Roles & Permissions screens —
 * see core/permissions/menu-order.util.ts.
 */
export const NAV_ENTRIES: NavEntry[] = [
  // 1. Overview — primary landmarks
  {
    label: 'Overview',
    icon: 'home',
    items: [
      { label: 'Dashboard',  icon: 'dashboard', route: '/dashboard' },
      { label: 'My Profile', icon: 'id_card',   route: '/profile' },
      { label: 'My Timetable', icon: 'event_note', route: '/my-timetable', permissions: ['TIMETABLE_VIEW'] },
    ],
  },
  // 2. Admission Management
  {
    label: 'Admission Management',
    icon: 'how_to_reg',
    items: [
      { label: 'Enquiries',          icon: 'contact_mail',         route: '/enquiries',                      permissions: ['ENQUIRY_VIEW', 'ENQUIRY_CREATE', 'ENQUIRY_EDIT', 'ENQUIRY_DELETE', 'ENQUIRY_EXPORT'] },
      { label: 'Finalize Fee',       icon: 'lock',                 route: '/student-fees/finalize',          permissions: ['FEE_FINALIZE'] },
      { label: 'Collect Payment',    icon: 'payments',             route: '/fee-collection',                 permissions: ['FEE_COLLECT'] },
      { label: 'Submit Documents',   icon: 'upload_file',          route: '/enquiries/document-submission',  permissions: ['DOCUMENT_SUBMISSION_VIEW', 'DOCUMENT_SUBMISSION_CREATE', 'DOCUMENT_SUBMISSION_EDIT', 'DOCUMENT_SUBMISSION_DELETE', 'DOCUMENT_SUBMISSION_MANAGE'] },
      { label: 'Verify Documents',   icon: 'verified',             route: '/enquiries/document-verification',permissions: ['DOCUMENT_VERIFICATION_MANAGE'] },
      { label: 'Complete Admission', icon: 'assignment_turned_in', route: '/enquiries/admission-completion', permissions: ['ADMISSION_COMPLETE'] },
      { label: 'Admission Explorer', icon: 'assignment_ind',       route: '/admissions',                     permissions: ['ADMISSION_VIEW', 'ADMISSION_CREATE', 'ADMISSION_EDIT', 'ADMISSION_DELETE', 'ADMISSION_EXPORT'] },
      { label: 'Retro Admit',        icon: 'history_edu',          route: '/students/retro-admit',           permissions: ['RETRO_ADMIT'] },
    ],
  },
  // 3. Student Management
  {
    label: 'Student Management',
    icon: 'person',
    items: [
      { label: 'Student Explorer',         icon: 'person_search', route: '/students',                 permissions: ['STUDENT_VIEW', 'STUDENT_CREATE', 'STUDENT_EDIT', 'STUDENT_DELETE', 'STUDENT_EXPORT'] },
      { label: 'Assign Roll Numbers',      icon: 'tag',           route: '/students/roll-numbers',    permissions: ['ROLL_NUMBER_ASSIGN'] },
      { label: 'Scholarship Applications', icon: 'military_tech', route: '/scholarship-applications', permissions: ['SCHOLARSHIP_APPROVE'] },
      { label: 'Data Import',              icon: 'upload',        route: '/import',                   permissions: ['IMPORT_DATA'] },
    ],
  },
  // 4. Finance
  {
    label: 'Finance',
    icon: 'wallet',
    items: [
      { label: 'Fee Explorer', icon: 'account_balance_wallet', route: '/student-fees',        permissions: ['STUDENT_FEE_VIEW', 'STUDENT_FEE_CREATE', 'STUDENT_FEE_EDIT', 'STUDENT_FEE_DELETE', 'STUDENT_FEE_EXPORT', 'STUDENT_FEE_MANAGE'] },
      { label: 'Receipts',     icon: 'receipt_long',           route: '/receipts',            permissions: ['RECEIPT_VIEW', 'RECEIPT_EXPORT'] },
      { label: 'Refunds',      icon: 'assignment_return',      route: '/refund-approvals',    permissions: ['FEE_REFUND_APPROVE', 'FEE_REFUND_EXPORT'] },
      { label: 'Commissions',  icon: 'handshake',              route: '/commission-explorer', permissions: ['COMMISSION_VIEW', 'COMMISSION_CREATE', 'COMMISSION_EDIT', 'COMMISSION_DELETE', 'COMMISSION_EXPORT', 'COMMISSION_MANAGE'] },
    ],
  },
  // 5. Academics
  {
    label: 'Academics',
    icon: 'auto_stories',
    items: [
      // -- Curriculum setup -- (Curriculum Versions first: Syllabus/Attendance-Threshold rows and
      // Elective Groups are children of a curriculum version; Experiments hang off Subject and CO/PO
      // Mapping hangs off Experiments, so both must follow it too)
      { label: 'Curriculum Versions', icon: 'layers',             route: '/curriculum-versions', permissions: ['CURRICULUM_VIEW', 'CURRICULUM_CREATE', 'CURRICULUM_EDIT', 'CURRICULUM_DELETE', 'CURRICULUM_MANAGE', 'ATTENDANCE_THRESHOLD_VIEW', 'ATTENDANCE_THRESHOLD_MANAGE', 'CURRICULUM_ELECTIVE_GROUP_VIEW', 'CURRICULUM_ELECTIVE_GROUP_MANAGE'] },
      { label: 'Syllabus',            icon: 'library_books',      route: '/syllabi',             permissions: ['SYLLABUS_VIEW', 'SYLLABUS_CREATE', 'SYLLABUS_EDIT', 'SYLLABUS_DELETE', 'SYLLABUS_EXPORT', 'SYLLABUS_MANAGE'] },
      { label: 'Experiments',         icon: 'biotech',            route: '/experiments',         permissions: ['EXPERIMENT_VIEW', 'EXPERIMENT_CREATE', 'EXPERIMENT_EDIT', 'EXPERIMENT_DELETE', 'EXPERIMENT_EXPORT', 'EXPERIMENT_MANAGE'] },
      { label: 'CO/PO Mapping',       icon: 'account_tree',       route: '/curriculum-mappings', permissions: ['COPO_VIEW', 'COPO_CREATE', 'COPO_EDIT', 'COPO_DELETE', 'COPO_MANAGE'] },
      // -- Term offering --
      { label: 'Course Offerings',    icon: 'event_available',    route: '/course-offerings',    permissions: ['COURSE_VIEW', 'COURSE_MANAGE'] },
      { label: 'Elective Assignment', icon: 'how_to_reg',         route: '/elective-assignment', permissions: ['COURSE_REGISTRATION_ELECTIVE_ASSIGN'] },
      // Deliberately after Elective Assignment: only once electives are assigned do you know which
      // options actually have real enrolled students and need a faculty at all. Capacity planning
      // must run before Assign Faculty, not after -- it's what decides whether a cohort's Theory
      // splits into 2+ CohortSection rows, and Assign Faculty's per-section "Section Faculty"
      // picker (CourseOfferingSectionFacultyService.getForOffering) only appears once those
      // sections already exist. No separate "Capacity Planner" menu item -- it's still a real,
      // fully standalone screen (own year/term/cohort pickers), just reached contextually from
      // here, from Skeleton Builder, and from Staffing ("Adjust manually" / "size rooms first"
      // links) rather than adding a 2nd top-level entry for what's largely the same job.
      { label: 'Capacity Auto-Plan',  icon: 'auto_awesome',       route: '/timetable/capacity-auto-plan', permissions: ['TIMETABLE_CAPACITY_PLANNER_VIEW'] },
      { label: 'Assign Faculty',      icon: 'person_edit',        route: '/assign-faculty',      permissions: ['COURSE_VIEW', 'COURSE_MANAGE'] },
      { label: 'Lab Schedules',       icon: 'calendar_view_week', route: '/lab-schedules',       permissions: ['LAB_SCHEDULE_VIEW', 'LAB_SCHEDULE_CREATE', 'LAB_SCHEDULE_EDIT', 'LAB_SCHEDULE_DELETE', 'LAB_SCHEDULE_EXPORT', 'LAB_SCHEDULE_MANAGE'] },
      // -- Timetable build -- (Faculty Availability + Workload Rules first: TimetableStaffingService's
      // validateAssignment() checks both directly, so they gate Staffing, not the other way round)
      { label: 'Faculty Availability', icon: 'event_busy',        route: '/faculty-availability', permissions: ['FACULTY_AVAILABILITY_VIEW', 'FACULTY_AVAILABILITY_MANAGE'] },
      { label: 'Faculty Workload Rules', icon: 'rule',            route: '/timetable/workload-rules', permissions: ['TIMETABLE_WORKLOAD_RULES_VIEW', 'TIMETABLE_WORKLOAD_RULES_MANAGE'] },
      { label: 'Skeleton Builder',    icon: 'grid_on',            route: '/timetable/skeleton-builder', permissions: ['TIMETABLE_VIEW'] },
      { label: 'Staffing',            icon: 'assignment_ind',     route: '/timetable/staffing',   permissions: ['TIMETABLE_VIEW'] },
      { label: 'Conflict Inspector',  icon: 'fact_check',         route: '/timetable/conflict-inspector', permissions: ['TIMETABLE_CONFLICT_INSPECTOR_VIEW'] },
      { label: 'Timetable Draft Review', icon: 'auto_awesome',    route: '/timetable/draft-review', permissions: ['TIMETABLE_MANAGE'] },
      { label: 'Timetable',           icon: 'event_note',         route: '/timetable',           permissions: ['TIMETABLE_VIEW'] },
      { label: 'Resource Timetable',  icon: 'grid_view',          route: '/timetable/resource-grid', permissions: ['TIMETABLE_FACULTY_GRID_VIEW', 'TIMETABLE_CLASSROOM_GRID_VIEW'] },
      // -- Timetable ops (on-demand, after publish) --
      { label: 'Faculty Absence',     icon: 'person_off',         route: '/faculty-absence',     permissions: ['FACULTY_ABSENCE_MARK', 'FACULTY_ABSENCE_SUBSTITUTE_APPLY'] },
      { label: 'Staff Session Swap',  icon: 'swap_horiz',         route: '/timetable/staff-swap', permissions: ['TIMETABLE_STAFF_SWAP'] },
      { label: 'My Special Classes',  icon: 'event_available',    route: '/timetable/special-classes/my-requests', permissions: ['TIMETABLE_SPECIAL_CLASS_VIEW', 'TIMETABLE_SPECIAL_CLASS_REQUEST'] },
      { label: 'Special Class Approvals', icon: 'fact_check',     route: '/timetable/special-classes/approval-queue', permissions: ['TIMETABLE_SPECIAL_CLASS_APPROVE'] },
      { label: 'My Escort Duties',    icon: 'directions_bus',     route: '/timetable/escort-rotation/my-duties', permissions: ['TIMETABLE_ESCORT_ROTATION_VIEW'] },
      // -- Attendance & progress (on-demand) --
      { label: 'Attendance',          icon: 'fact_check',         route: '/attendance',          permissions: ['ATTENDANCE_VIEW', 'ATTENDANCE_CREATE', 'ATTENDANCE_EDIT', 'ATTENDANCE_DELETE', 'ATTENDANCE_EXPORT', 'ATTENDANCE_MANAGE'] },
      { label: 'Progress Report',     icon: 'insights',           route: '/progress-report',     permissions: ['PROGRESS_REPORT_VIEW'] },
      // -- Examinations (on-demand) --
      { label: 'Manage Exams',        icon: 'quiz',               route: '/examinations',        permissions: ['EXAMINATION_VIEW', 'EXAMINATION_CREATE', 'EXAMINATION_EDIT', 'EXAMINATION_DELETE', 'EXAMINATION_MANAGE'] },
      { label: 'Exam Results',        icon: 'grade',              route: '/exam-results',        permissions: ['EXAM_RESULT_VIEW', 'EXAM_RESULT_CREATE', 'EXAM_RESULT_EDIT', 'EXAM_RESULT_DELETE', 'EXAM_RESULT_EXPORT', 'EXAM_RESULT_MANAGE'] },
      // -- Promotion (term close) --
      { label: 'Student Promotion',   icon: 'move_up',            route: '/student-promotions',  permissions: ['STUDENT_PROMOTION_VIEW', 'STUDENT_PROMOTION_MANAGE'] },
    ],
  },
  // 6. Library
  {
    label: 'Library',
    icon: 'local_library',
    items: [
      { label: 'Issue Books',      icon: 'add_circle_outline', route: '/library/issues/new',     permissions: ['LIBRARY_QUICK_ISSUE'] },
      { label: 'Issue Explorer',   icon: 'book_online',        route: '/library/issues',         permissions: ['LIBRARY_ISSUE_VIEW', 'LIBRARY_ISSUE_CREATE', 'LIBRARY_ISSUE_EDIT', 'LIBRARY_ISSUE_DELETE', 'LIBRARY_ISSUE_EXPORT', 'LIBRARY_ISSUE_MANAGE'] },
      { label: 'Overdue Books',    icon: 'warning',            route: '/library/reports',        permissions: ['LIBRARY_REPORT_VIEW'] },
      { label: 'Book Explorer',    icon: 'menu_book',          route: '/library/books',          permissions: ['LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_CREATE', 'LIBRARY_CATALOGUE_EDIT', 'LIBRARY_CATALOGUE_DELETE', 'LIBRARY_CATALOGUE_EXPORT', 'LIBRARY_CATALOGUE_MANAGE'] },
      { label: 'Journal Explorer', icon: 'newspaper',          route: '/library/periodicals',    permissions: ['LIBRARY_PERIODICAL_VIEW', 'LIBRARY_PERIODICAL_CREATE', 'LIBRARY_PERIODICAL_EDIT', 'LIBRARY_PERIODICAL_DELETE', 'LIBRARY_PERIODICAL_EXPORT', 'LIBRARY_PERIODICAL_MANAGE'] },
      { label: 'My Library',       icon: 'person',             route: '/library/my-issues',      permissions: ['MY_LIBRARY_VIEW'] },
      { label: 'Fines',            icon: 'currency_rupee',     route: '/library/fines',          permissions: ['LIBRARY_FINE_VIEW', 'LIBRARY_FINE_CREATE', 'LIBRARY_FINE_EDIT', 'LIBRARY_FINE_DELETE', 'LIBRARY_FINE_EXPORT', 'LIBRARY_FINE_MANAGE'] },
      { label: 'Racks & Shelves',  icon: 'shelves',            route: '/library/racks',          permissions: ['LIBRARY_SHELF_VIEW', 'LIBRARY_SHELF_MANAGE'] },
      { label: 'Import',           icon: 'upload',             route: '/library/import',         permissions: ['LIBRARY_IMPORT', 'LIBRARY_PERIODICAL_IMPORT'] },
      { label: 'Library Settings', icon: 'settings',           route: '/library/settings',       permissions: ['LIBRARY_SETTINGS_EDIT', 'LIBRARY_SETTINGS_MANAGE'] },
    ],
  },
  // 7. Core Infrastructure — building/room hierarchy shared across Hostel, and future Stores/Library location features
  {
    label: 'Core Infrastructure',
    icon: 'location_city',
    items: [
      { label: 'Campus Infrastructure',   icon: 'apartment',    route: '/campus-infrastructure',   permissions: ['CAMPUS_INFRASTRUCTURE_VIEW', 'CAMPUS_INFRASTRUCTURE_MANAGE', 'HOSTEL_ROOM_VIEW', 'HOSTEL_ROOM_MANAGE'] },
      { label: 'Room Purpose Categories', icon: 'category',     route: '/room-purpose-categories', permissions: ['ROOM_PURPOSE_CATEGORY_VIEW', 'ROOM_PURPOSE_CATEGORY_MANAGE'] },
      { label: 'Room Sub-Types',          icon: 'meeting_room', route: '/room-sub-types',          permissions: ['ROOM_SUB_TYPE_VIEW', 'ROOM_SUB_TYPE_MANAGE'] },
      // Branch/Floor/Zone/Room Diagrams intentionally removed from nav — reached only via each
      // entity's own "Import Floor Plan" button inside Campus Setup now, never as standalone nav items.
    ],
  },
  // 8. Inventory Management — physical asset/equipment tracking
  {
    label: 'Inventory Management',
    icon: 'construction',
    items: [
      { label: 'Inventory',   icon: 'inventory_2', route: '/inventory',   permissions: ['INVENTORY_VIEW', 'INVENTORY_CREATE', 'INVENTORY_EDIT', 'INVENTORY_DELETE', 'INVENTORY_EXPORT', 'INVENTORY_MANAGE'] },
      { label: 'Maintenance', icon: 'build',       route: '/maintenance', permissions: ['MAINTENANCE_VIEW', 'MAINTENANCE_CREATE', 'MAINTENANCE_EDIT', 'MAINTENANCE_DELETE', 'MAINTENANCE_EXPORT', 'MAINTENANCE_MANAGE'] },
    ],
  },
  // 9. Hostel Management — hostel-only operational screens (building/room masters live under Core Infrastructure)
  {
    label: 'Hostel Management',
    icon: 'bed',
    items: [
      { label: 'Hostel Room Types', icon: 'bed',      route: '/hostel-room-types', permissions: ['HOSTEL_ROOM_TYPE_VIEW', 'HOSTEL_ROOM_TYPE_MANAGE'] },
      { label: 'Room Preferences',  icon: 'star',     route: '/room-preferences',  permissions: ['HOSTEL_ROOM_PREFERENCE_VIEW', 'HOSTEL_ROOM_PREFERENCE_MANAGE'] },
      { label: 'Room Allocation',   icon: 'king_bed', route: '/room-allocations',  permissions: ['HOSTEL_ROOM_ALLOCATION_VIEW', 'HOSTEL_ROOM_ALLOCATION_MANAGE'] },
    ],
  },
  // 10. Reports & Analytics
  {
    label: 'Reports & Analytics',
    icon: 'analytics',
    items: [
      { label: 'General Reports', icon: 'assessment',    route: '/reports',     permissions: ['REPORT_VIEW', 'REPORT_EXPORT'] },
      { label: 'Fee Reports',     icon: 'request_quote', route: '/fee-reports', permissions: ['FEE_REPORT_VIEW', 'FEE_REPORT_EXPORT'] },
    ],
  },
  // 11. Preferences
  {
    label: 'Preferences',
    icon: 'tune',
    items: [
      { label: 'Academic Calendar',  icon: 'event_note',        route: '/academic-calendar',       permissions: ['ACADEMIC_CALENDAR_VIEW', 'ACADEMIC_CALENDAR_MANAGE'] },
      { label: 'Academic Years',     icon: 'calendar_month',    route: '/academic-years',          permissions: ['ACADEMIC_YEAR_VIEW', 'ACADEMIC_YEAR_CREATE', 'ACADEMIC_YEAR_EDIT', 'ACADEMIC_YEAR_DELETE', 'ACADEMIC_YEAR_EXPORT', 'ACADEMIC_YEAR_MANAGE', 'SEMESTER_MANAGE'] },
      { label: 'Agents',             icon: 'support_agent',     route: '/agents',                  permissions: ['AGENT_VIEW', 'AGENT_CREATE', 'AGENT_EDIT', 'AGENT_DELETE', 'AGENT_EXPORT', 'AGENT_MANAGE'] },
      { label: 'Blood Groups',       icon: 'bloodtype',         route: '/blood-groups',            permissions: ['BLOOD_GROUP_VIEW', 'BLOOD_GROUP_CREATE', 'BLOOD_GROUP_EDIT', 'BLOOD_GROUP_DELETE', 'BLOOD_GROUP_EXPORT', 'BLOOD_GROUP_MANAGE'] },
      { label: 'Classrooms',         icon: 'meeting_room',      route: '/classrooms',              permissions: ['CLASSROOM_VIEW', 'CLASSROOM_MANAGE'] },
      { label: 'Clinical Venues',    icon: 'local_hospital',    route: '/clinical-venues',         permissions: ['CLINICAL_VENUE_VIEW', 'CLINICAL_VENUE_MANAGE'] },
      { label: 'Communities',        icon: 'people',            route: '/communities',             permissions: ['COMMUNITY_VIEW', 'COMMUNITY_CREATE', 'COMMUNITY_EDIT', 'COMMUNITY_DELETE', 'COMMUNITY_EXPORT', 'COMMUNITY_MANAGE'] },
      { label: 'Courses',            icon: 'menu_book',         route: '/courses',                 permissions: ['COURSE_VIEW', 'COURSE_CREATE', 'COURSE_EDIT', 'COURSE_DELETE', 'COURSE_EXPORT', 'COURSE_MANAGE'] },
      { label: 'Designations',       icon: 'badge',             route: '/designations',            permissions: ['DESIGNATION_VIEW', 'DESIGNATION_CREATE', 'DESIGNATION_EDIT', 'DESIGNATION_DELETE', 'DESIGNATION_EXPORT', 'DESIGNATION_MANAGE'] },
      { label: 'Equipment',          icon: 'devices',           route: '/equipment',               permissions: ['EQUIPMENT_VIEW', 'EQUIPMENT_CREATE', 'EQUIPMENT_EDIT', 'EQUIPMENT_DELETE', 'EQUIPMENT_EXPORT', 'EQUIPMENT_MANAGE'] },
      { label: 'Faculty',            icon: 'groups',            route: '/faculty',                 permissions: ['FACULTY_VIEW', 'FACULTY_CREATE', 'FACULTY_EDIT', 'FACULTY_DELETE', 'FACULTY_EXPORT', 'FACULTY_MANAGE'] },
      { label: 'Faculty Doc Config', icon: 'rule',              route: '/faculty/document-config', permissions: ['FACULTY_DOC_CONFIG_VIEW', 'FACULTY_DOC_CONFIG_MANAGE'] },
      { label: 'Fee Structures',     icon: 'account_balance',   route: '/fee-structures',          permissions: ['FEE_STRUCTURE_VIEW', 'FEE_STRUCTURE_CREATE', 'FEE_STRUCTURE_EDIT', 'FEE_STRUCTURE_DELETE', 'FEE_STRUCTURE_EXPORT', 'FEE_STRUCTURE_MANAGE'] },
      { label: 'Holiday Templates',  icon: 'event_repeat',      route: '/holiday-templates',       permissions: ['HOLIDAY_TEMPLATE_VIEW', 'HOLIDAY_TEMPLATE_MANAGE'] },
      { label: 'Institutions',       icon: 'corporate_fare',    route: '/institutions',            permissions: ['INSTITUTION_VIEW', 'INSTITUTION_CREATE', 'INSTITUTION_EDIT', 'INSTITUTION_DELETE', 'INSTITUTION_MANAGE'] },
      { label: 'Labs',               icon: 'science',           route: '/labs',                    permissions: ['LAB_VIEW', 'LAB_CREATE', 'LAB_EDIT', 'LAB_DELETE', 'LAB_EXPORT', 'LAB_MANAGE'] },
      { label: 'Location Master',    icon: 'public',            route: '/india-locations',         permissions: ['INDIA_LOCATION_VIEW', 'INDIA_LOCATION_CREATE', 'INDIA_LOCATION_EDIT', 'INDIA_LOCATION_DELETE', 'INDIA_LOCATION_EXPORT', 'INDIA_LOCATION_MANAGE'] },
      { label: 'Number Sequences',   icon: 'pin',               route: '/number-sequences',        permissions: ['NUMBER_SEQUENCE_VIEW', 'NUMBER_SERIES_VIEW', 'NUMBER_SERIES_MANAGE'] },
      { label: 'Periods',            icon: 'schedule',          route: '/periods',                 permissions: ['PERIOD_VIEW', 'PERIOD_MANAGE'] },
      { label: 'Programs',           icon: 'school',            route: '/programs',                permissions: ['PROGRAM_VIEW', 'PROGRAM_CREATE', 'PROGRAM_EDIT', 'PROGRAM_DELETE', 'PROGRAM_EXPORT', 'PROGRAM_MANAGE'] },
      { label: 'Referral Types',     icon: 'share',             route: '/referral-types',          permissions: ['REFERRAL_TYPE_VIEW', 'REFERRAL_TYPE_CREATE', 'REFERRAL_TYPE_EDIT', 'REFERRAL_TYPE_DELETE', 'REFERRAL_TYPE_EXPORT', 'REFERRAL_TYPE_MANAGE'] },
      { label: 'Scholarship Types',  icon: 'workspace_premium', route: '/scholarships',            permissions: ['SCHOLARSHIP_VIEW', 'SCHOLARSHIP_CREATE', 'SCHOLARSHIP_EDIT', 'SCHOLARSHIP_DELETE', 'SCHOLARSHIP_EXPORT', 'SCHOLARSHIP_MANAGE'] },
      { label: 'Settings',           icon: 'settings',          route: '/settings',                permissions: ['SETTINGS_VIEW', 'SETTINGS_CREATE', 'SETTINGS_EDIT', 'SETTINGS_DELETE', 'SETTINGS_MANAGE'] },
      { label: 'Specialities',       icon: 'business',          route: '/specialities',            permissions: ['DEPT_VIEW', 'DEPT_CREATE', 'DEPT_EDIT', 'DEPT_DELETE', 'DEPT_EXPORT', 'DEPT_MANAGE'] },
      { label: 'Staff Referrers',    icon: 'badge',             route: '/staff-referrers',         permissions: ['STAFF_REFERRER_VIEW', 'STAFF_REFERRER_CREATE', 'STAFF_REFERRER_EDIT', 'STAFF_REFERRER_DELETE', 'STAFF_REFERRER_EXPORT', 'STAFF_REFERRER_MANAGE'] },
      { label: 'Subjects',           icon: 'menu_book',         route: '/subjects',                permissions: ['SUBJECT_VIEW', 'SUBJECT_MANAGE'] },
    ],
  },
  // 12. User Management
  {
    label: 'User Management',
    icon: 'manage_accounts',
    items: [
      { label: 'Users',               icon: 'group',  route: '/user-management',  permissions: ['USER_VIEW'] },
      { label: 'Roles & Permissions', icon: 'shield', route: '/role-management',  permissions: ['ROLE_VIEW'] },
      { label: 'Permission Tiers',    icon: 'tune',   route: '/permission-tiers', permissions: ['PERMISSION_TIER_MANAGE'] },
    ],
  },
];
