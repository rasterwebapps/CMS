export interface NavItem {
  label: string;
  icon: string;
  route: string;
  /** DB permission codes — show only if user holds at least one. Empty/absent = visible to all. */
  permissions?: string[];
  /**
   * Feature-module codes (see core/modules/module.service.ts) — show only if this deployment
   * has enabled at least one. Empty/absent = "core", always visible regardless of module
   * enablement (matching the backend's PermSecurityBean treatment of unmapped permission codes).
   * Module gate is ANDed with the permission check above and always wins: a disabled module
   * hides an item even for a user who holds the underlying permission.
   */
  modules?: string[];
}

export interface NavGroup {
  label: string;
  icon: string;
  items: NavItem[];
  /** DB permission codes — show only if user holds at least one. Empty/absent = visible to all. */
  permissions?: string[];
  /** Feature-module codes — see {@link NavItem.modules}. Gates the whole group at once. */
  modules?: string[];
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
 *
 * Every group/item that belongs to a toggleable feature module (see
 * docs/module-architecture/MODULE_REGISTRY.md for the full table) carries a `modules` tag.
 * Preferences deliberately stays ONE top-level group (its items are masters that belong to many
 * different modules) rather than being split apart into each owning module's own group — its
 * items are clustered by owning module below, each individually tagged, so module-disabled
 * masters still disappear correctly even though the group itself is never module-gated.
 */
export const NAV_ENTRIES: NavEntry[] = [
  // 1. Overview — primary landmarks, always visible (core)
  {
    label: 'Overview',
    icon: 'home',
    items: [
      { label: 'Dashboard',  icon: 'dashboard', route: '/dashboard' },
      { label: 'My Profile', icon: 'id_card',   route: '/profile' },
      { label: 'My Timetable', icon: 'event_note', route: '/my-timetable', permissions: ['TIMETABLE_VIEW'], modules: ['ACADEMICS'] },
    ],
  },
  // 2. Admission Management
  {
    label: 'Admission Management',
    icon: 'how_to_reg',
    modules: ['ADMISSIONS'],
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
    modules: ['STUDENT_MGMT'],
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
    modules: ['FINANCE'],
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
    modules: ['ACADEMICS'],
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
    modules: ['LIBRARY'],
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
    modules: ['CORE_INFRA'],
    items: [
      { label: 'Campus Infrastructure',   icon: 'apartment',    route: '/campus-infrastructure',   permissions: ['CAMPUS_INFRASTRUCTURE_VIEW', 'CAMPUS_INFRASTRUCTURE_MANAGE', 'HOSTEL_ROOM_VIEW', 'HOSTEL_ROOM_MANAGE'] },
      { label: 'Room Purpose Categories', icon: 'category',     route: '/room-purpose-categories', permissions: ['ROOM_PURPOSE_CATEGORY_VIEW', 'ROOM_PURPOSE_CATEGORY_MANAGE'] },
      { label: 'Room Sub-Types',          icon: 'meeting_room', route: '/room-sub-types',          permissions: ['ROOM_SUB_TYPE_VIEW', 'ROOM_SUB_TYPE_MANAGE'] },
      // Branch/Floor/Zone/Room Diagrams intentionally removed from nav — reached only via each
      // entity's own "Import Floor Plan" button inside Campus Setup now, never as standalone nav items.
    ],
  },
  // 8. Inventory Management — physical asset/equipment tracking (legacy lab-consumables feature;
  // stays as-is until its own migration onto Stock Management below is scheduled — see
  // docs/inventory-management/DECISION_LOG.md). Unified with Stock Management and Purchasing &
  // Suppliers below under the one toggleable INVENTORY module — see docs/module-architecture/.
  {
    label: 'Inventory Management',
    icon: 'construction',
    modules: ['INVENTORY'],
    items: [
      { label: 'Inventory',   icon: 'inventory_2', route: '/inventory',   permissions: ['INVENTORY_VIEW', 'INVENTORY_CREATE', 'INVENTORY_EDIT', 'INVENTORY_DELETE', 'INVENTORY_EXPORT', 'INVENTORY_MANAGE'] },
      { label: 'Maintenance', icon: 'build',       route: '/maintenance', permissions: ['MAINTENANCE_VIEW', 'MAINTENANCE_CREATE', 'MAINTENANCE_EDIT', 'MAINTENANCE_DELETE', 'MAINTENANCE_EXPORT', 'MAINTENANCE_MANAGE'] },
    ],
  },
  // 8b. Stock Management — the new Release 3 Inventory Management module (Catalog, Stock,
  // Procurement, Assets, …, built out phase by phase). Deliberately a separate top-level group
  // from "Inventory Management" above rather than folding in — that label already belongs to the
  // legacy feature this module eventually retires. Both share the one toggleable INVENTORY module.
  {
    label: 'Stock Management',
    icon: 'warehouse',
    modules: ['INVENTORY'],
    items: [
      { label: 'Stock Balance',     icon: 'inventory',   route: '/inventory/stock/balances', permissions: ['INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_MANAGE'] },
      { label: 'Cycle Counts',      icon: 'fact_check',  route: '/inventory/stock/cycle-counts', permissions: ['INVENTORY_CYCLE_COUNT_VIEW', 'INVENTORY_CYCLE_COUNT_MANAGE', 'INVENTORY_CYCLE_COUNT_APPROVE'] },
      { label: 'Products',          icon: 'inventory_2', route: '/inventory/products',   permissions: ['INVENTORY_PRODUCT_VIEW', 'INVENTORY_PRODUCT_MANAGE'] },
      { label: 'Categories',        icon: 'category',   route: '/inventory/categories', permissions: ['INVENTORY_CATEGORY_VIEW', 'INVENTORY_CATEGORY_MANAGE'] },
      { label: 'Units of Measure',  icon: 'straighten', route: '/inventory/uoms',        permissions: ['INVENTORY_UOM_VIEW', 'INVENTORY_UOM_MANAGE'] },
      { label: 'Locations',         icon: 'store',       route: '/inventory/locations',  permissions: ['INVENTORY_LOCATION_VIEW', 'INVENTORY_LOCATION_MANAGE'] },
    ],
  },
  // 8c. Purchasing & Suppliers — Phase 2 of the Inventory Management module. Its own top-level
  // group, per the same "new Inventory nav/menus get their own entry" convention Stock Management
  // above followed. See docs/inventory-management/DECISION_LOG.md's "Phase 2 kickoff" entry.
  // Also shares the one toggleable INVENTORY module.
  {
    label: 'Purchasing & Suppliers',
    icon: 'shopping_cart',
    modules: ['INVENTORY'],
    items: [
      { label: 'Suppliers',         icon: 'storefront',  route: '/inventory/procurement/suppliers',      permissions: ['INVENTORY_SUPPLIER_VIEW', 'INVENTORY_SUPPLIER_MANAGE', 'INVENTORY_SUPPLIER_APPROVE'] },
      { label: 'Rate Contracts',    icon: 'handshake',   route: '/inventory/procurement/rate-contracts', permissions: ['INVENTORY_RATE_CONTRACT_VIEW', 'INVENTORY_RATE_CONTRACT_MANAGE'] },
      { label: 'Vendor Product Rates', icon: 'local_shipping', route: '/inventory/procurement/vendor-product-mappings', permissions: ['INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW', 'INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE'] },
      { label: 'Purchase Requisitions', icon: 'assignment', route: '/inventory/procurement/purchase-requisitions', permissions: ['INVENTORY_PURCHASE_REQUISITION_VIEW', 'INVENTORY_PURCHASE_REQUISITION_MANAGE', 'INVENTORY_PURCHASE_REQUISITION_APPROVE'] },
      { label: 'Wanted List',       icon: 'inventory_2', route: '/inventory/procurement/wanted-list',     permissions: ['INVENTORY_WANTED_LIST_VIEW', 'INVENTORY_WANTED_LIST_MANAGE', 'INVENTORY_WANTED_LIST_CONVERT', 'INVENTORY_WANTED_LIST_RUN'] },
      { label: 'Purchase Orders',   icon: 'receipt_long', route: '/inventory/procurement/purchase-orders', permissions: ['INVENTORY_PURCHASE_ORDER_VIEW', 'INVENTORY_PURCHASE_ORDER_MANAGE', 'INVENTORY_PURCHASE_ORDER_FORCE_CLOSE'] },
      { label: 'Tax Rules',         icon: 'percent',     route: '/inventory/procurement/tax-rules',      permissions: ['INVENTORY_TAX_RULE_VIEW', 'INVENTORY_TAX_RULE_MANAGE'] },
    ],
  },
  // 8d. Receiving & Stock Movement — Phase 3 of the Inventory Management module. Own top-level
  // group, same "new Inventory sub-area gets its own entry" convention. See docs/inventory-
  // management/DECISION_LOG.md's "Goods Receipt slice" entry.
  {
    label: 'Receiving & Stock Movement',
    icon: 'move_to_inbox',
    modules: ['INVENTORY'],
    items: [
      { label: 'Goods Receipts', icon: 'move_to_inbox', route: '/inventory/receiving/goods-receipts', permissions: ['INVENTORY_GRN_VIEW', 'INVENTORY_GRN_MANAGE', 'INVENTORY_GRN_CONFIRM'] },
      { label: 'Stock Transfers', icon: 'sync_alt', route: '/inventory/stock/transfers', permissions: ['INVENTORY_STOCK_TRANSFER_VIEW', 'INVENTORY_STOCK_TRANSFER_MANAGE'] },
      { label: 'Supplier Returns', icon: 'keyboard_return', route: '/inventory/receiving/supplier-returns', permissions: ['INVENTORY_SUPPLIER_RETURN_VIEW', 'INVENTORY_SUPPLIER_RETURN_MANAGE'] },
    ],
  },
  // 8e. Requests, Issues & Returns — Phase 4 of the Inventory Management module. Own top-level
  // group, same "new Inventory sub-area gets its own entry" convention. See docs/inventory-
  // management/DECISION_LOG.md's "Stock Issue Request slice" entry.
  {
    label: 'Requests, Issues & Returns',
    icon: 'outbound',
    modules: ['INVENTORY'],
    items: [
      { label: 'Stock Issue Requests', icon: 'outbound', route: '/inventory/issue/stock-issue-requests', permissions: ['INVENTORY_ISSUE_REQUEST_VIEW', 'INVENTORY_ISSUE_REQUEST_MANAGE', 'INVENTORY_ISSUE_REQUEST_APPROVE'] },
      { label: 'Loanable Item Issues', icon: 'assignment_return', route: '/inventory/issue/loanable-item-issues', permissions: ['INVENTORY_LOAN_ISSUE_VIEW', 'INVENTORY_LOAN_ISSUE_MANAGE', 'INVENTORY_LOAN_ISSUE_RETURN'] },
    ],
  },
  // 8f. Equipment & Asset Management — Phase 5 of the Inventory Management module. Own
  // top-level group, same "new Inventory sub-area gets its own entry" convention. See docs/
  // inventory-management/DECISION_LOG.md's "Asset register slice" entry.
  {
    label: 'Equipment & Asset Management',
    icon: 'precision_manufacturing',
    modules: ['INVENTORY'],
    items: [
      { label: 'Asset Register', icon: 'inventory_2', route: '/inventory/asset/assets', permissions: ['INVENTORY_ASSET_VIEW', 'INVENTORY_ASSET_MANAGE'] },
      { label: 'Maintenance Schedules', icon: 'build', route: '/inventory/asset/maintenance-schedules', permissions: ['INVENTORY_ASSET_MAINTENANCE_VIEW', 'INVENTORY_ASSET_MAINTENANCE_MANAGE'] },
      { label: 'Service Contracts', icon: 'handshake', route: '/inventory/asset/service-contracts', permissions: ['INVENTORY_ASSET_MAINTENANCE_VIEW', 'INVENTORY_ASSET_MAINTENANCE_MANAGE'] },
    ],
  },
  // 8g. Budgets & Approvals — Phase 6 of the Inventory Management module. Own top-level
  // group, same "new Inventory sub-area gets its own entry" convention. See docs/inventory-
  // management/DECISION_LOG.md's "Budget allocation slice" entry.
  {
    label: 'Budgets & Approvals',
    icon: 'account_balance_wallet',
    modules: ['INVENTORY'],
    items: [
      { label: 'Budgets', icon: 'account_balance_wallet', route: '/inventory/budget/budgets', permissions: ['INVENTORY_BUDGET_VIEW', 'INVENTORY_BUDGET_MANAGE'] },
      { label: 'Approval Workflows', icon: 'rule', route: '/inventory/approval/workflows', permissions: ['INVENTORY_APPROVAL_WORKFLOW_VIEW', 'INVENTORY_APPROVAL_WORKFLOW_MANAGE'] },
      { label: 'Approvals', icon: 'fact_check', route: '/inventory/approval/instances', permissions: ['INVENTORY_APPROVAL_VIEW', 'INVENTORY_APPROVAL_ACT'] },
    ],
  },
  // 8h. Gate Pass, Vendor-Owned Stock & Service Requests — Phase 7 of the Inventory Management
  // module. Own top-level group, same "new Inventory sub-area gets its own entry" convention.
  // See docs/inventory-management/DECISION_LOG.md's "Gate Pass slice" entry.
  {
    label: 'Gate Pass & Service Requests',
    icon: 'local_shipping',
    modules: ['INVENTORY'],
    items: [
      { label: 'Gate Passes', icon: 'local_shipping', route: '/inventory/gate-pass/gate-passes', permissions: ['INVENTORY_GATE_PASS_VIEW', 'INVENTORY_GATE_PASS_MANAGE', 'INVENTORY_GATE_PASS_APPROVE', 'INVENTORY_GATE_PASS_VERIFY', 'INVENTORY_GATE_PASS_RETURN'] },
    ],
  },
  // 9. Hostel Management — hostel-only operational screens (building/room masters live under Core Infrastructure)
  {
    label: 'Hostel Management',
    icon: 'bed',
    modules: ['HOSTEL'],
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
    modules: ['REPORTS'],
    items: [
      { label: 'General Reports', icon: 'assessment',    route: '/reports',     permissions: ['REPORT_VIEW', 'REPORT_EXPORT'] },
      { label: 'Fee Reports',     icon: 'request_quote', route: '/fee-reports', permissions: ['FEE_REPORT_VIEW', 'FEE_REPORT_EXPORT'] },
    ],
  },
  // 11. Preferences — masters/configuration for every module. Deliberately kept as ONE nav group
  // (not split apart into each owning module's own group) — see docs/module-architecture/
  // DECISION_LOG.md. Items are clustered by owning module below, each individually tagged with
  // `modules`, so a disabled module's masters still disappear even though the group as a whole
  // is never module-gated (some items here are core/shared and carry no module tag at all).
  {
    label: 'Preferences',
    icon: 'tune',
    items: [
      // -- Admission Management masters --
      { label: 'Agents',             icon: 'support_agent',     route: '/agents',                  permissions: ['AGENT_VIEW', 'AGENT_CREATE', 'AGENT_EDIT', 'AGENT_DELETE', 'AGENT_EXPORT', 'AGENT_MANAGE'], modules: ['ADMISSIONS'] },
      { label: 'Institutions',       icon: 'corporate_fare',    route: '/institutions',            permissions: ['INSTITUTION_VIEW', 'INSTITUTION_CREATE', 'INSTITUTION_EDIT', 'INSTITUTION_DELETE', 'INSTITUTION_MANAGE'], modules: ['ADMISSIONS'] },
      { label: 'Referral Types',     icon: 'share',             route: '/referral-types',          permissions: ['REFERRAL_TYPE_VIEW', 'REFERRAL_TYPE_CREATE', 'REFERRAL_TYPE_EDIT', 'REFERRAL_TYPE_DELETE', 'REFERRAL_TYPE_EXPORT', 'REFERRAL_TYPE_MANAGE'], modules: ['ADMISSIONS'] },
      { label: 'Staff Referrers',    icon: 'badge',             route: '/staff-referrers',         permissions: ['STAFF_REFERRER_VIEW', 'STAFF_REFERRER_CREATE', 'STAFF_REFERRER_EDIT', 'STAFF_REFERRER_DELETE', 'STAFF_REFERRER_EXPORT', 'STAFF_REFERRER_MANAGE'], modules: ['ADMISSIONS'] },
      // -- Student Management masters --
      { label: 'Blood Groups',       icon: 'bloodtype',         route: '/blood-groups',            permissions: ['BLOOD_GROUP_VIEW', 'BLOOD_GROUP_CREATE', 'BLOOD_GROUP_EDIT', 'BLOOD_GROUP_DELETE', 'BLOOD_GROUP_EXPORT', 'BLOOD_GROUP_MANAGE'], modules: ['STUDENT_MGMT'] },
      { label: 'Communities',        icon: 'people',            route: '/communities',             permissions: ['COMMUNITY_VIEW', 'COMMUNITY_CREATE', 'COMMUNITY_EDIT', 'COMMUNITY_DELETE', 'COMMUNITY_EXPORT', 'COMMUNITY_MANAGE'], modules: ['STUDENT_MGMT'] },
      { label: 'Scholarship Types',  icon: 'workspace_premium', route: '/scholarships',            permissions: ['SCHOLARSHIP_VIEW', 'SCHOLARSHIP_CREATE', 'SCHOLARSHIP_EDIT', 'SCHOLARSHIP_DELETE', 'SCHOLARSHIP_EXPORT', 'SCHOLARSHIP_MANAGE'], modules: ['STUDENT_MGMT'] },
      // -- Finance masters --
      { label: 'Fee Structures',     icon: 'account_balance',   route: '/fee-structures',          permissions: ['FEE_STRUCTURE_VIEW', 'FEE_STRUCTURE_CREATE', 'FEE_STRUCTURE_EDIT', 'FEE_STRUCTURE_DELETE', 'FEE_STRUCTURE_EXPORT', 'FEE_STRUCTURE_MANAGE'], modules: ['FINANCE'] },
      // -- Academics masters --
      { label: 'Academic Calendar',  icon: 'event_note',        route: '/academic-calendar',       permissions: ['ACADEMIC_CALENDAR_VIEW', 'ACADEMIC_CALENDAR_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Academic Years',     icon: 'calendar_month',    route: '/academic-years',          permissions: ['ACADEMIC_YEAR_VIEW', 'ACADEMIC_YEAR_CREATE', 'ACADEMIC_YEAR_EDIT', 'ACADEMIC_YEAR_DELETE', 'ACADEMIC_YEAR_EXPORT', 'ACADEMIC_YEAR_MANAGE', 'SEMESTER_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Classrooms',         icon: 'meeting_room',      route: '/classrooms',              permissions: ['CLASSROOM_VIEW', 'CLASSROOM_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Clinical Venues',    icon: 'local_hospital',    route: '/clinical-venues',         permissions: ['CLINICAL_VENUE_VIEW', 'CLINICAL_VENUE_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Courses',            icon: 'menu_book',         route: '/courses',                 permissions: ['COURSE_VIEW', 'COURSE_CREATE', 'COURSE_EDIT', 'COURSE_DELETE', 'COURSE_EXPORT', 'COURSE_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Faculty',            icon: 'groups',            route: '/faculty',                 permissions: ['FACULTY_VIEW', 'FACULTY_CREATE', 'FACULTY_EDIT', 'FACULTY_DELETE', 'FACULTY_EXPORT', 'FACULTY_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Faculty Doc Config', icon: 'rule',              route: '/faculty/document-config', permissions: ['FACULTY_DOC_CONFIG_VIEW', 'FACULTY_DOC_CONFIG_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Holiday Templates',  icon: 'event_repeat',      route: '/holiday-templates',       permissions: ['HOLIDAY_TEMPLATE_VIEW', 'HOLIDAY_TEMPLATE_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Labs',               icon: 'science',           route: '/labs',                    permissions: ['LAB_VIEW', 'LAB_CREATE', 'LAB_EDIT', 'LAB_DELETE', 'LAB_EXPORT', 'LAB_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Periods',            icon: 'schedule',          route: '/periods',                 permissions: ['PERIOD_VIEW', 'PERIOD_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Programs',           icon: 'school',            route: '/programs',                permissions: ['PROGRAM_VIEW', 'PROGRAM_CREATE', 'PROGRAM_EDIT', 'PROGRAM_DELETE', 'PROGRAM_EXPORT', 'PROGRAM_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Specialities',       icon: 'business',          route: '/specialities',            permissions: ['DEPT_VIEW', 'DEPT_CREATE', 'DEPT_EDIT', 'DEPT_DELETE', 'DEPT_EXPORT', 'DEPT_MANAGE'], modules: ['ACADEMICS'] },
      { label: 'Subjects',           icon: 'menu_book',         route: '/subjects',                permissions: ['SUBJECT_VIEW', 'SUBJECT_MANAGE'], modules: ['ACADEMICS'] },
      // -- Inventory Management masters --
      { label: 'Equipment',          icon: 'devices',           route: '/equipment',               permissions: ['EQUIPMENT_VIEW', 'EQUIPMENT_CREATE', 'EQUIPMENT_EDIT', 'EQUIPMENT_DELETE', 'EQUIPMENT_EXPORT', 'EQUIPMENT_MANAGE'], modules: ['INVENTORY'] },
      // -- Core / shared masters (no module tag — always visible regardless of enabled modules) --
      { label: 'Designations',       icon: 'badge',             route: '/designations',            permissions: ['DESIGNATION_VIEW', 'DESIGNATION_CREATE', 'DESIGNATION_EDIT', 'DESIGNATION_DELETE', 'DESIGNATION_EXPORT', 'DESIGNATION_MANAGE'] },
      { label: 'Location Master',    icon: 'public',            route: '/india-locations',         permissions: ['INDIA_LOCATION_VIEW', 'INDIA_LOCATION_CREATE', 'INDIA_LOCATION_EDIT', 'INDIA_LOCATION_DELETE', 'INDIA_LOCATION_EXPORT', 'INDIA_LOCATION_MANAGE'] },
      { label: 'Number Sequences',   icon: 'pin',               route: '/number-sequences',        permissions: ['NUMBER_SEQUENCE_VIEW', 'NUMBER_SERIES_VIEW', 'NUMBER_SERIES_MANAGE'] },
      { label: 'Settings',           icon: 'settings',          route: '/settings',                permissions: ['SETTINGS_VIEW', 'SETTINGS_CREATE', 'SETTINGS_EDIT', 'SETTINGS_DELETE', 'SETTINGS_MANAGE'] },
    ],
  },
  // 12. User Management — always visible (core)
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
