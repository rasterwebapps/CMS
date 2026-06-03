import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { requiresPermission } from './core/permissions/permission.guard';

const withAuth = [authGuard];
const withPermission = (...codes: string[]) => [authGuard, requiresPermission(...codes)];

export const routes: Routes = [
  {
    path: 'profile',
    canActivate: withAuth,
    // Apple Health redesign backed up in profile-health.component.* — swap import below to activate it
    loadComponent: () =>
      import('./features/profile/profile.component').then((m) => m.ProfileComponent),
  },
  {
    path: 'dashboard',
    canActivate: withAuth,
    loadComponent: () =>
      import('./features/dashboard/dashboard').then((m) => m.DashboardComponent),
  },
  {
    path: 'dashboard/configure',
    canActivate: withPermission('DASHBOARD_CUSTOMIZE'),
    loadComponent: () =>
      import('./features/dashboard/configure/dashboard-configure.component').then(
        (m) => m.DashboardConfigureComponent
      ),
  },
  {
    path: 'departments',
    canActivate: withPermission('DEPT_VIEW', 'DEPT_MANAGE'),
    loadComponent: () =>
      import('./features/department/department-list/department-list.component').then(
        (m) => m.DepartmentListComponent
      ),
  },
  {
    path: 'departments/new',
    canActivate: withPermission('DEPT_MANAGE'),
    loadComponent: () =>
      import('./features/department/department-form/department-form.component').then(
        (m) => m.DepartmentFormComponent
      ),
  },
  {
    path: 'departments/:id/edit',
    canActivate: withPermission('DEPT_MANAGE'),
    loadComponent: () =>
      import('./features/department/department-form/department-form.component').then(
        (m) => m.DepartmentFormComponent
      ),
  },
  {
    path: 'programs',
    canActivate: withPermission('PROGRAM_VIEW', 'PROGRAM_MANAGE'),
    loadComponent: () =>
      import('./features/program/program-list/program-list.component').then(
        (m) => m.ProgramListComponent
      ),
  },
  {
    path: 'programs/new',
    canActivate: withPermission('PROGRAM_MANAGE'),
    loadComponent: () =>
      import('./features/program/program-form/program-form.component').then(
        (m) => m.ProgramFormComponent
      ),
  },
  {
    path: 'programs/:id/edit',
    canActivate: withPermission('PROGRAM_MANAGE'),
    loadComponent: () =>
      import('./features/program/program-form/program-form.component').then(
        (m) => m.ProgramFormComponent
      ),
  },
  {
    path: 'courses',
    canActivate: withPermission('COURSE_VIEW', 'COURSE_MANAGE'),
    loadComponent: () =>
      import('./features/course/course-list/course-list.component').then(
        (m) => m.CourseListComponent
      ),
  },
  {
    path: 'courses/new',
    canActivate: withPermission('COURSE_MANAGE'),
    loadComponent: () =>
      import('./features/course/course-form/course-form.component').then(
        (m) => m.CourseFormComponent
      ),
  },
  {
    path: 'courses/:id/edit',
    canActivate: withPermission('COURSE_MANAGE'),
    loadComponent: () =>
      import('./features/course/course-form/course-form.component').then(
        (m) => m.CourseFormComponent
      ),
  },
  {
    path: 'academic-years',
    canActivate: withPermission('ACADEMIC_YEAR_VIEW', 'ACADEMIC_YEAR_MANAGE'),
    loadComponent: () =>
      import('./features/academic-year/academic-year-list/academic-year-list.component').then(
        (m) => m.AcademicYearListComponent
      ),
  },
  {
    path: 'academic-years/new',
    canActivate: withPermission('ACADEMIC_YEAR_MANAGE'),
    loadComponent: () =>
      import('./features/academic-year/academic-year-form/academic-year-form.component').then(
        (m) => m.AcademicYearFormComponent
      ),
  },
  {
    path: 'academic-years/:id/edit',
    canActivate: withPermission('ACADEMIC_YEAR_MANAGE'),
    loadComponent: () =>
      import('./features/academic-year/academic-year-form/academic-year-form.component').then(
        (m) => m.AcademicYearFormComponent
      ),
  },
  {
    path: 'academic-years/:id/detail',
    canActivate: withPermission('ACADEMIC_YEAR_VIEW', 'ACADEMIC_YEAR_MANAGE'),
    loadComponent: () =>
      import('./features/academic-year/academic-year-form/academic-year-form.component').then(
        (m) => m.AcademicYearFormComponent
      ),
  },
  {
    path: 'academic-calendar',
    canActivate: withPermission('ACADEMIC_YEAR_VIEW', 'ACADEMIC_YEAR_MANAGE'),
    loadComponent: () =>
      import('./features/academic-year/academic-calendar/academic-calendar.component').then(
        (m) => m.AcademicCalendarComponent
      ),
  },
  {
    path: 'number-sequences',
    canActivate: withPermission('NUMBER_SEQUENCE_VIEW'),
    loadComponent: () =>
      import('./features/settings/number-sequences/number-sequences-list.component').then(
        (m) => m.NumberSequencesListComponent
      ),
  },
  {
    path: 'labs',
    canActivate: withPermission('LAB_VIEW', 'LAB_MANAGE'),
    loadComponent: () =>
      import('./features/lab/lab-list/lab-list.component').then((m) => m.LabListComponent),
  },
  {
    path: 'labs/new',
    canActivate: withPermission('LAB_MANAGE'),
    loadComponent: () =>
      import('./features/lab/lab-form/lab-form.component').then((m) => m.LabFormComponent),
  },
  {
    path: 'labs/:id',
    canActivate: withPermission('LAB_VIEW', 'LAB_MANAGE'),
    loadComponent: () =>
      import('./features/lab/lab-detail/lab-detail.component').then((m) => m.LabDetailComponent),
  },
  {
    path: 'labs/:id/edit',
    canActivate: withPermission('LAB_MANAGE'),
    loadComponent: () =>
      import('./features/lab/lab-form/lab-form.component').then((m) => m.LabFormComponent),
  },
  {
    path: 'faculty',
    canActivate: withPermission('FACULTY_VIEW', 'FACULTY_MANAGE'),
    loadComponent: () =>
      import('./features/faculty/faculty-list/faculty-list.component').then(
        (m) => m.FacultyListComponent
      ),
  },
  {
    path: 'faculty/document-config',
    canActivate: withPermission('FACULTY_MANAGE'),
    loadComponent: () =>
      import('./features/faculty/faculty-doc-config/faculty-doc-config.component').then(
        (m) => m.FacultyDocConfigComponent
      ),
  },
  {
    path: 'faculty/new',
    canActivate: withPermission('FACULTY_MANAGE'),
    loadComponent: () =>
      import('./features/faculty/faculty-form/faculty-form.component').then(
        (m) => m.FacultyFormComponent
      ),
  },
  {
    path: 'faculty/:id',
    canActivate: withPermission('FACULTY_VIEW', 'FACULTY_MANAGE'),
    loadComponent: () =>
      import('./features/faculty/faculty-detail/faculty-detail.component').then(
        (m) => m.FacultyDetailComponent
      ),
  },
  {
    path: 'faculty/:id/edit',
    canActivate: withPermission('FACULTY_MANAGE'),
    loadComponent: () =>
      import('./features/faculty/faculty-form/faculty-form.component').then(
        (m) => m.FacultyFormComponent
      ),
  },
  {
    path: 'students',
    canActivate: withPermission('STUDENT_VIEW', 'STUDENT_CREATE', 'STUDENT_EDIT'),
    loadComponent: () =>
      import('./features/student/student-list/student-list.component').then(
        (m) => m.StudentListComponent
      ),
  },
  {
    path: 'students/new',
    canActivate: withPermission('STUDENT_CREATE'),
    loadComponent: () =>
      import('./features/student/student-form/student-form.component').then(
        (m) => m.StudentFormComponent
      ),
  },
  {
    path: 'students/roll-numbers',
    canActivate: withPermission('ROLL_NUMBER_ASSIGN'),
    loadComponent: () =>
      import('./features/student/roll-number-assignment/roll-number-assignment.component').then(
        (m) => m.RollNumberAssignmentComponent
      ),
  },
  {
    path: 'students/:id',
    canActivate: withPermission('STUDENT_VIEW', 'STUDENT_EDIT'),
    loadComponent: () =>
      import('./features/student/student-detail/student-detail.component').then(
        (m) => m.StudentDetailComponent
      ),
  },
  {
    path: 'students/:id/edit',
    canActivate: withPermission('STUDENT_EDIT'),
    loadComponent: () =>
      import('./features/student/student-form/student-form.component').then(
        (m) => m.StudentFormComponent
      ),
  },
  {
    path: 'scholarships',
    canActivate: [authGuard, requiresPermission('SCHOLARSHIP_MANAGE')],
    loadComponent: () =>
      import('./features/scholarship/scholarship-type-list/scholarship-type-list.component').then(
        (m) => m.ScholarshipTypeListComponent
      ),
  },
  {
    path: 'scholarships/new',
    canActivate: [authGuard, requiresPermission('SCHOLARSHIP_MANAGE')],
    loadComponent: () =>
      import('./features/scholarship/scholarship-type-form/scholarship-type-form.component').then(
        (m) => m.ScholarshipTypeFormComponent
      ),
  },
  {
    path: 'scholarships/:id/edit',
    canActivate: [authGuard, requiresPermission('SCHOLARSHIP_MANAGE')],
    loadComponent: () =>
      import('./features/scholarship/scholarship-type-form/scholarship-type-form.component').then(
        (m) => m.ScholarshipTypeFormComponent
      ),
  },
  {
    path: 'scholarship-applications',
    canActivate: [authGuard, requiresPermission('SCHOLARSHIP_APPROVE')],
    loadComponent: () =>
      import('./features/scholarship/scholarship-applications-list/scholarship-applications-list.component').then(
        (m) => m.ScholarshipApplicationsListComponent
      ),
  },
  {
    path: 'attendance',
    canActivate: withPermission('ATTENDANCE_VIEW', 'ATTENDANCE_MANAGE'),
    loadComponent: () =>
      import('./features/attendance/attendance-list/attendance-list.component').then(
        (m) => m.AttendanceListComponent
      ),
  },
  {
    path: 'attendance/mark',
    canActivate: withPermission('ATTENDANCE_MANAGE'),
    loadComponent: () =>
      import('./features/attendance/attendance-mark/attendance-mark.component').then(
        (m) => m.AttendanceMarkComponent
      ),
  },
  {
    path: 'fee-structures',
    canActivate: withPermission('FEE_STRUCTURE_VIEW', 'FEE_STRUCTURE_MANAGE'),
    loadComponent: () =>
      import('./features/finance/fee-structure-list/fee-structure-list.component').then(
        (m) => m.FeeStructureListComponent
      ),
  },
  {
    path: 'fee-structures/new',
    canActivate: withPermission('FEE_STRUCTURE_MANAGE'),
    loadComponent: () =>
      import('./features/finance/fee-structure-form/fee-structure-form.component').then(
        (m) => m.FeeStructureFormComponent
      ),
  },
  {
    path: 'fee-structures/edit',
    canActivate: withPermission('FEE_STRUCTURE_MANAGE'),
    loadComponent: () =>
      import('./features/finance/fee-structure-form/fee-structure-form.component').then(
        (m) => m.FeeStructureFormComponent
      ),
  },
  {
    path: 'fee-structures/:id/edit',
    canActivate: withPermission('FEE_STRUCTURE_MANAGE'),
    loadComponent: () =>
      import('./features/finance/fee-structure-form/fee-structure-form.component').then(
        (m) => m.FeeStructureFormComponent
      ),
  },
  {
    path: 'fee-collection',
    canActivate: withPermission('FEE_COLLECT'),
    loadComponent: () =>
      import('./features/finance/fee-collection/fee-collection.component').then(
        (m) => m.FeeCollectionComponent
      ),
  },
  {
    path: 'fee-payments',
    canActivate: withPermission('FEE_COLLECT'),
    loadComponent: () =>
      import('./features/finance/fee-collection/fee-collection.component').then(
        (m) => m.FeeCollectionComponent
      ),
  },
  {
    path: 'fee-payments/new',
    canActivate: withPermission('FEE_COLLECT'),
    loadComponent: () =>
      import('./features/finance/fee-payment-form/fee-payment-form.component').then(
        (m) => m.FeePaymentFormComponent
      ),
  },
  {
    path: 'receipts',
    canActivate: withPermission('RECEIPT_VIEW'),
    loadComponent: () =>
      import('./features/finance/receipts-list/receipts-list.component').then(
        (m) => m.ReceiptsListComponent
      ),
  },
  {
    path: 'equipment',
    canActivate: withPermission('EQUIPMENT_VIEW', 'EQUIPMENT_MANAGE'),
    loadComponent: () =>
      import('./features/equipment/equipment-list/equipment-list.component').then(
        (m) => m.EquipmentListComponent
      ),
  },
  {
    path: 'equipment/new',
    canActivate: withPermission('EQUIPMENT_MANAGE'),
    loadComponent: () =>
      import('./features/equipment/equipment-form/equipment-form.component').then(
        (m) => m.EquipmentFormComponent
      ),
  },
  {
    path: 'equipment/:id/edit',
    canActivate: withPermission('EQUIPMENT_MANAGE'),
    loadComponent: () =>
      import('./features/equipment/equipment-form/equipment-form.component').then(
        (m) => m.EquipmentFormComponent
      ),
  },
  {
    path: 'inventory',
    canActivate: withPermission('INVENTORY_VIEW', 'INVENTORY_MANAGE'),
    loadComponent: () =>
      import('./features/inventory/inventory-list/inventory-list.component').then(
        (m) => m.InventoryListComponent
      ),
  },
  {
    path: 'inventory/new',
    canActivate: withPermission('INVENTORY_MANAGE'),
    loadComponent: () =>
      import('./features/inventory/inventory-form/inventory-form.component').then(
        (m) => m.InventoryFormComponent
      ),
  },
  {
    path: 'inventory/:id/edit',
    canActivate: withPermission('INVENTORY_MANAGE'),
    loadComponent: () =>
      import('./features/inventory/inventory-form/inventory-form.component').then(
        (m) => m.InventoryFormComponent
      ),
  },
  {
    path: 'maintenance',
    canActivate: withPermission('MAINTENANCE_VIEW', 'MAINTENANCE_MANAGE'),
    loadComponent: () =>
      import('./features/maintenance/maintenance-list/maintenance-list.component').then(
        (m) => m.MaintenanceListComponent
      ),
  },
  {
    path: 'maintenance/new',
    canActivate: withPermission('MAINTENANCE_MANAGE'),
    loadComponent: () =>
      import('./features/maintenance/maintenance-form/maintenance-form.component').then(
        (m) => m.MaintenanceFormComponent
      ),
  },
  {
    path: 'maintenance/:id/edit',
    canActivate: withPermission('MAINTENANCE_MANAGE'),
    loadComponent: () =>
      import('./features/maintenance/maintenance-form/maintenance-form.component').then(
        (m) => m.MaintenanceFormComponent
      ),
  },
  {
    path: 'examinations',
    canActivate: withPermission('EXAMINATION_VIEW', 'EXAMINATION_MANAGE'),
    loadComponent: () =>
      import('./features/examination/examination-list/examination-list.component').then(
        (m) => m.ExaminationListComponent
      ),
  },
  {
    path: 'examinations/new',
    canActivate: withPermission('EXAMINATION_MANAGE'),
    loadComponent: () =>
      import('./features/examination/examination-form/examination-form.component').then(
        (m) => m.ExaminationFormComponent
      ),
  },
  {
    path: 'examinations/:id/edit',
    canActivate: withPermission('EXAMINATION_MANAGE'),
    loadComponent: () =>
      import('./features/examination/examination-form/examination-form.component').then(
        (m) => m.ExaminationFormComponent
      ),
  },
  {
    path: 'exam-results',
    canActivate: withPermission('EXAM_RESULT_VIEW', 'EXAM_RESULT_MANAGE'),
    loadComponent: () =>
      import('./features/examination/exam-result-list/exam-result-list.component').then(
        (m) => m.ExamResultListComponent
      ),
  },
  {
    path: 'syllabi',
    canActivate: withPermission('SYLLABUS_VIEW', 'SYLLABUS_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/syllabus-list/syllabus-list.component').then(
        (m) => m.SyllabusListComponent
      ),
  },
  {
    path: 'syllabi/new',
    canActivate: withPermission('SYLLABUS_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/syllabus-form/syllabus-form.component').then(
        (m) => m.SyllabusFormComponent
      ),
  },
  {
    path: 'syllabi/:id/edit',
    canActivate: withPermission('SYLLABUS_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/syllabus-form/syllabus-form.component').then(
        (m) => m.SyllabusFormComponent
      ),
  },
  {
    path: 'experiments',
    canActivate: withPermission('EXPERIMENT_VIEW', 'EXPERIMENT_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/experiment-list/experiment-list.component').then(
        (m) => m.ExperimentListComponent
      ),
  },
  {
    path: 'experiments/new',
    canActivate: withPermission('EXPERIMENT_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/experiment-form/experiment-form.component').then(
        (m) => m.ExperimentFormComponent
      ),
  },
  {
    path: 'experiments/:id/edit',
    canActivate: withPermission('EXPERIMENT_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/experiment-form/experiment-form.component').then(
        (m) => m.ExperimentFormComponent
      ),
  },
  {
    path: 'curriculum-mappings',
    canActivate: withPermission('COPO_VIEW', 'COPO_MANAGE', 'CURRICULUM_VIEW'),
    loadComponent: () =>
      import('./features/curriculum/co-po-mapping/co-po-mapping.component').then(
        (m) => m.CoPoMappingComponent
      ),
  },
  {
    path: 'curriculum-mappings/new',
    canActivate: withPermission('COPO_MANAGE', 'CURRICULUM_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/co-po-mapping-form/co-po-mapping-form.component').then(
        (m) => m.CoPoMappingFormComponent
      ),
  },
  {
    path: 'curriculum-mappings/:id/edit',
    canActivate: withPermission('COPO_MANAGE', 'CURRICULUM_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/co-po-mapping-form/co-po-mapping-form.component').then(
        (m) => m.CoPoMappingFormComponent
      ),
  },
  {
    path: 'curriculum-versions',
    canActivate: withPermission('CURRICULUM_VIEW', 'CURRICULUM_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/curriculum-version-list/curriculum-version-list.component').then(
        (m) => m.CurriculumVersionListComponent
      ),
  },
  {
    path: 'curriculum-versions/new',
    canActivate: withPermission('CURRICULUM_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/curriculum-version-form/curriculum-version-form.component').then(
        (m) => m.CurriculumVersionFormComponent
      ),
  },
  {
    path: 'curriculum-versions/:id/edit',
    canActivate: withPermission('CURRICULUM_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/curriculum-version-form/curriculum-version-form.component').then(
        (m) => m.CurriculumVersionFormComponent
      ),
  },
  {
    path: 'curriculum-map/:id',
    canActivate: withPermission('CURRICULUM_VIEW', 'CURRICULUM_MANAGE'),
    loadComponent: () =>
      import('./features/curriculum/curriculum-map/curriculum-map.component').then(
        (m) => m.CurriculumMapComponent
      ),
  },
  {
    path: 'lab-schedules',
    canActivate: withPermission('LAB_SCHEDULE_VIEW', 'LAB_SCHEDULE_MANAGE'),
    loadComponent: () =>
      import('./features/lab-schedule/lab-schedule-list/lab-schedule-list.component').then(
        (m) => m.LabScheduleListComponent
      ),
  },
  {
    path: 'lab-schedules/new',
    canActivate: withPermission('LAB_SCHEDULE_MANAGE'),
    loadComponent: () =>
      import('./features/lab-schedule/lab-schedule-form/lab-schedule-form.component').then(
        (m) => m.LabScheduleFormComponent
      ),
  },
  {
    path: 'lab-schedules/:id/edit',
    canActivate: withPermission('LAB_SCHEDULE_MANAGE'),
    loadComponent: () =>
      import('./features/lab-schedule/lab-schedule-form/lab-schedule-form.component').then(
        (m) => m.LabScheduleFormComponent
      ),
  },
  {
    path: 'fee-reports',
    canActivate: withPermission('FEE_REPORT_VIEW'),
    loadComponent: () =>
      import('./features/fee-reports/fee-reports-dashboard/fee-reports-dashboard.component').then(
        (m) => m.FeeReportsDashboardComponent
      ),
  },
  {
    path: 'reports',
    canActivate: withPermission('REPORT_VIEW'),
    loadComponent: () =>
      import('./features/reports/reports-dashboard/reports-dashboard.component').then(
        (m) => m.ReportsDashboardComponent
      ),
  },
  {
    path: 'import',
    canActivate: withPermission('IMPORT_DATA'),
    loadComponent: () =>
      import('./features/import/import.component').then((m) => m.ImportComponent),
  },
  {
    path: 'user-management',
    canActivate: withPermission('USER_VIEW'),
    loadComponent: () =>
      import('./features/user-management/user-management.component').then(
        (m) => m.UserManagementComponent
      ),
  },
  {
    path: 'role-management',
    canActivate: withPermission('ROLE_VIEW'),
    loadComponent: () =>
      import('./features/role-management/role-management.component').then(
        (m) => m.RoleManagementComponent
      ),
  },
  {
    path: 'settings',
    canActivate: withPermission('SETTINGS_VIEW', 'SETTINGS_MANAGE'),
    loadComponent: () =>
      import('./features/settings/system-configuration-list/system-configuration-list.component').then(
        (m) => m.SystemConfigurationListComponent
      ),
  },
  {
    path: 'settings/branding',
    canActivate: withPermission('SETTINGS_MANAGE'),
    loadComponent: () =>
      import('./features/settings/branding/branding.component').then(
        (m) => m.BrandingComponent
      ),
  },
  {
    path: 'settings/new',
    canActivate: withPermission('SETTINGS_MANAGE'),
    loadComponent: () =>
      import('./features/settings/system-configuration-form/system-configuration-form.component').then(
        (m) => m.SystemConfigurationFormComponent
      ),
  },
  {
    path: 'settings/:id/edit',
    canActivate: withPermission('SETTINGS_MANAGE'),
    loadComponent: () =>
      import('./features/settings/system-configuration-form/system-configuration-form.component').then(
        (m) => m.SystemConfigurationFormComponent
      ),
  },
  {
    path: 'agents',
    canActivate: withPermission('AGENT_VIEW', 'AGENT_MANAGE'),
    loadComponent: () =>
      import('./features/agent/agent-list/agent-list.component').then(
        (m) => m.AgentListComponent
      ),
  },
  {
    path: 'agents/new',
    canActivate: withPermission('AGENT_MANAGE'),
    loadComponent: () =>
      import('./features/agent/agent-form/agent-form.component').then(
        (m) => m.AgentFormComponent
      ),
  },
  {
    path: 'agents/:id/edit',
    canActivate: withPermission('AGENT_MANAGE'),
    loadComponent: () =>
      import('./features/agent/agent-form/agent-form.component').then(
        (m) => m.AgentFormComponent
      ),
  },
  {
    path: 'enquiries',
    canActivate: withPermission('ENQUIRY_VIEW', 'ENQUIRY_CREATE', 'ENQUIRY_EDIT'),
    loadComponent: () =>
      import('./features/enquiry/enquiry-list/enquiry-list.component').then(
        (m) => m.EnquiryListComponent
      ),
  },
  {
    path: 'enquiries/document-submission',
    canActivate: withPermission('DOCUMENT_SUBMISSION_VIEW', 'DOCUMENT_SUBMISSION_MANAGE'),
    loadComponent: () =>
      import('./features/enquiry/document-submission/document-submission-list.component').then(
        (m) => m.DocumentSubmissionListComponent
      ),
  },
  {
    path: 'enquiries/document-submission/:id',
    canActivate: withPermission('DOCUMENT_SUBMISSION_VIEW', 'DOCUMENT_SUBMISSION_MANAGE'),
    loadComponent: () =>
      import('./features/enquiry/document-collection/document-collection.component').then(
        (m) => m.DocumentCollectionComponent
      ),
  },
  {
    path: 'enquiries/document-verification',
    canActivate: withPermission('DOCUMENT_VERIFICATION_MANAGE'),
    loadComponent: () =>
      import('./features/enquiry/document-verification/document-verification-list.component').then(
        (m) => m.DocumentVerificationListComponent
      ),
  },
  {
    path: 'enquiries/document-verification/:id',
    canActivate: withPermission('DOCUMENT_VERIFICATION_MANAGE'),
    loadComponent: () =>
      import('./features/enquiry/document-verification/document-verification-detail.component').then(
        (m) => m.DocumentVerificationDetailComponent
      ),
  },
  {
    path: 'enquiries/admission-completion',
    canActivate: withPermission('ADMISSION_CREATE', 'ADMISSION_EDIT'),
    loadComponent: () =>
      import('./features/enquiry/admission-completion/admission-completion-list.component').then(
        (m) => m.AdmissionCompletionListComponent
      ),
  },
  {
    path: 'enquiries/new',
    canActivate: withPermission('ENQUIRY_CREATE'),
    loadComponent: () =>
      import('./features/enquiry/enquiry-form/enquiry-form.component').then(
        (m) => m.EnquiryFormComponent
      ),
  },
  {
    path: 'enquiries/:id/edit',
    canActivate: withPermission('ENQUIRY_EDIT'),
    loadComponent: () =>
      import('./features/enquiry/enquiry-form/enquiry-form.component').then(
        (m) => m.EnquiryFormComponent
      ),
  },
  {
    path: 'enquiries/:id/convert',
    canActivate: withPermission('ADMISSION_CREATE', 'ADMISSION_EDIT'),
    loadComponent: () =>
      import('./features/enquiry/enquiry-convert/enquiry-convert.component').then(
        (m) => m.EnquiryConvertComponent
      ),
  },
  {
    path: 'enquiries/:id',
    canActivate: withPermission('ENQUIRY_VIEW', 'ENQUIRY_EDIT'),
    loadComponent: () =>
      import('./features/enquiry/enquiry-detail/enquiry-detail.component').then(
        (m) => m.EnquiryDetailComponent
      ),
  },
  {
    path: 'admissions',
    canActivate: withPermission('ADMISSION_VIEW', 'ADMISSION_CREATE', 'ADMISSION_EDIT'),
    loadComponent: () =>
      import('./features/admission/admission-list/admission-list.component').then(
        (m) => m.AdmissionListComponent
      ),
  },
  {
    path: 'admissions/:id/edit',
    canActivate: withPermission('ADMISSION_EDIT'),
    loadComponent: () =>
      import('./features/admission/admission-form/admission-form.component').then(
        (m) => m.AdmissionFormComponent
      ),
  },
  {
    path: 'admissions/:id',
    canActivate: withPermission('ADMISSION_VIEW', 'ADMISSION_EDIT'),
    loadComponent: () =>
      import('./features/admission/admission-detail/admission-detail.component').then(
        (m) => m.AdmissionDetailComponent
      ),
  },
  {
    path: 'student-fees',
    canActivate: withPermission('STUDENT_FEE_VIEW', 'STUDENT_FEE_MANAGE'),
    loadComponent: () =>
      import('./features/finance/fee-explorer/fee-explorer.component').then(
        (m) => m.FeeExplorerComponent
      ),
  },
  {
    path: 'student-fees/finalize',
    canActivate: withPermission('FEE_FINALIZE'),
    loadComponent: () =>
      import('./features/finance/fee-finalization/fee-finalization.component').then(
        (m) => m.FeeFinalizationComponent
      ),
  },
  {
    path: 'student-fees/collect-payment',
    canActivate: withPermission('FEE_COLLECT'),
    loadComponent: () =>
      import('./features/finance/fee-collection/fee-collection.component').then(
        (m) => m.FeeCollectionComponent
      ),
  },
  {
    path: 'student-fees/:studentId',
    canActivate: withPermission('STUDENT_FEE_VIEW', 'STUDENT_FEE_MANAGE'),
    loadComponent: () =>
      import('./features/finance/student-fee-detail/student-fee-detail.component').then(
        (m) => m.StudentFeeDetailComponent
      ),
  },
  {
    path: 'referral-types',
    canActivate: withPermission('REFERRAL_TYPE_VIEW', 'REFERRAL_TYPE_MANAGE'),
    loadComponent: () =>
      import('./features/referral-type/referral-type-list/referral-type-list.component').then(
        (m) => m.ReferralTypeListComponent
      ),
  },
  {
    path: 'referral-types/new',
    canActivate: withPermission('REFERRAL_TYPE_MANAGE'),
    loadComponent: () =>
      import('./features/referral-type/referral-type-form/referral-type-form.component').then(
        (m) => m.ReferralTypeFormComponent
      ),
  },
  {
    path: 'referral-types/:id/edit',
    canActivate: withPermission('REFERRAL_TYPE_MANAGE'),
    loadComponent: () =>
      import('./features/referral-type/referral-type-form/referral-type-form.component').then(
        (m) => m.ReferralTypeFormComponent
      ),
  },
  // ── Masters: Community ───────────────────────────────────────────────────
  {
    path: 'communities',
    canActivate: [authGuard, requiresPermission('COMMUNITY_VIEW', 'COMMUNITY_MANAGE')],
    loadComponent: () =>
      import('./features/community/community-list/community-list.component').then(
        (m) => m.CommunityListComponent
      ),
  },
  {
    path: 'communities/new',
    canActivate: [authGuard, requiresPermission('COMMUNITY_MANAGE')],
    loadComponent: () =>
      import('./features/community/community-form/community-form.component').then(
        (m) => m.CommunityFormComponent
      ),
  },
  {
    path: 'communities/:id/edit',
    canActivate: [authGuard, requiresPermission('COMMUNITY_MANAGE')],
    loadComponent: () =>
      import('./features/community/community-form/community-form.component').then(
        (m) => m.CommunityFormComponent
      ),
  },
  // ── Masters: Blood Group ─────────────────────────────────────────────────
  {
    path: 'blood-groups',
    canActivate: [authGuard, requiresPermission('BLOOD_GROUP_VIEW', 'BLOOD_GROUP_MANAGE')],
    loadComponent: () =>
      import('./features/blood-group/blood-group-list/blood-group-list.component').then(
        (m) => m.BloodGroupListComponent
      ),
  },
  {
    path: 'blood-groups/new',
    canActivate: [authGuard, requiresPermission('BLOOD_GROUP_MANAGE')],
    loadComponent: () =>
      import('./features/blood-group/blood-group-form/blood-group-form.component').then(
        (m) => m.BloodGroupFormComponent
      ),
  },
  {
    path: 'blood-groups/:id/edit',
    canActivate: [authGuard, requiresPermission('BLOOD_GROUP_MANAGE')],
    loadComponent: () =>
      import('./features/blood-group/blood-group-form/blood-group-form.component').then(
        (m) => m.BloodGroupFormComponent
      ),
  },
  // ── India Locations / Location Master (Countries, States & Districts) ────────────────────
  {
    path: 'india-locations',
    canActivate: [authGuard, requiresPermission('INDIA_LOCATION_VIEW', 'INDIA_LOCATION_MANAGE')],
    loadComponent: () =>
      import('./features/india-location/india-location-list/india-location-list.component').then(
        (m) => m.IndiaLocationListComponent
      ),
  },
  {
    path: 'india-locations/countries/new',
    canActivate: [authGuard, requiresPermission('INDIA_LOCATION_MANAGE')],
    loadComponent: () =>
      import('./features/india-location/country-form/country-form.component').then(
        (m) => m.CountryFormComponent
      ),
  },
  {
    path: 'india-locations/countries/:id/edit',
    canActivate: [authGuard, requiresPermission('INDIA_LOCATION_MANAGE')],
    loadComponent: () =>
      import('./features/india-location/country-form/country-form.component').then(
        (m) => m.CountryFormComponent
      ),
  },
  {
    path: 'india-locations/states/new',
    canActivate: [authGuard, requiresPermission('INDIA_LOCATION_MANAGE')],
    loadComponent: () =>
      import('./features/india-location/india-state-form/india-state-form.component').then(
        (m) => m.IndiaStateFormComponent
      ),
  },
  {
    path: 'india-locations/states/:id/edit',
    canActivate: [authGuard, requiresPermission('INDIA_LOCATION_MANAGE')],
    loadComponent: () =>
      import('./features/india-location/india-state-form/india-state-form.component').then(
        (m) => m.IndiaStateFormComponent
      ),
  },
  {
    path: 'india-locations/states/:stateId/districts/new',
    canActivate: [authGuard, requiresPermission('INDIA_LOCATION_MANAGE')],
    loadComponent: () =>
      import('./features/india-location/india-district-form/india-district-form.component').then(
        (m) => m.IndiaDistrictFormComponent
      ),
  },
  {
    path: 'india-locations/districts/:id/edit',
    canActivate: [authGuard, requiresPermission('INDIA_LOCATION_MANAGE')],
    loadComponent: () =>
      import('./features/india-location/india-district-form/india-district-form.component').then(
        (m) => m.IndiaDistrictFormComponent
      ),
  },
  // ── Library ──────────────────────────────────────────────────────────────
  {
    path: 'library/books',
    canActivate: withPermission('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_CATALOGUE_MANAGE'),
    loadComponent: () =>
      import('./features/library/library-book-list/library-book-list.component').then(
        (m) => m.LibraryBookListComponent
      ),
  },
  {
    path: 'library/books/new',
    canActivate: withPermission('LIBRARY_CATALOGUE_MANAGE'),
    loadComponent: () =>
      import('./features/library/library-book-form/library-book-form.component').then(
        (m) => m.LibraryBookFormComponent
      ),
  },
  {
    path: 'library/books/import',
    canActivate: withPermission('LIBRARY_IMPORT'),
    loadComponent: () =>
      import('./features/library/library-book-import/library-book-import.component').then(
        (m) => m.LibraryBookImportComponent
      ),
  },
  {
    path: 'library/books/:id/edit',
    canActivate: withPermission('LIBRARY_CATALOGUE_MANAGE'),
    loadComponent: () =>
      import('./features/library/library-book-form/library-book-form.component').then(
        (m) => m.LibraryBookFormComponent
      ),
  },
  {
    path: 'library/issues',
    canActivate: withPermission('LIBRARY_ISSUE_MANAGE'),
    loadComponent: () =>
      import('./features/library/library-issue-list/library-issue-list.component').then(
        (m) => m.LibraryIssueListComponent
      ),
  },
  {
    path: 'library/issues/new',
    canActivate: withPermission('LIBRARY_ISSUE_MANAGE'),
    loadComponent: () =>
      import('./features/library/library-issue-form/library-issue-form.component').then(
        (m) => m.LibraryIssueFormComponent
      ),
  },
  {
    path: 'library/periodicals',
    canActivate: withPermission('LIBRARY_PERIODICAL_VIEW', 'LIBRARY_PERIODICAL_MANAGE'),
    loadComponent: () =>
      import('./features/library/library-periodical-list/library-periodical-list.component').then(
        (m) => m.LibraryPeriodicalListComponent
      ),
  },
  {
    path: 'library/periodicals/new',
    canActivate: withPermission('LIBRARY_PERIODICAL_MANAGE'),
    loadComponent: () =>
      import('./features/library/library-periodical-form/library-periodical-form.component').then(
        (m) => m.LibraryPeriodicalFormComponent
      ),
  },
  {
    path: 'library/periodicals/:id/edit',
    canActivate: withPermission('LIBRARY_PERIODICAL_MANAGE'),
    loadComponent: () =>
      import('./features/library/library-periodical-form/library-periodical-form.component').then(
        (m) => m.LibraryPeriodicalFormComponent
      ),
  },
  {
    path: 'library/settings',
    canActivate: withPermission('LIBRARY_SETTINGS_MANAGE'),
    loadComponent: () =>
      import('./features/library/library-settings/library-settings.component').then(
        (m) => m.LibrarySettingsComponent
      ),
  },
  {
    path: 'library/reports',
    canActivate: withPermission('LIBRARY_REPORT_VIEW'),
    loadComponent: () =>
      import('./features/library/library-reports/library-reports.component').then(
        (m) => m.LibraryReportsComponent
      ),
  },
  {
    path: 'library/my-issues',
    canActivate: withPermission('LIBRARY_ISSUE_VIEW', 'LIBRARY_ISSUE_MANAGE'),
    loadComponent: () =>
      import('./features/library/library-my-issues/library-my-issues.component').then(
        (m) => m.LibraryMyIssuesComponent
      ),
  },
  // ── Administration: User & Role Management ───────────────────────────────
  {
    path: 'user-management',
    canActivate: withPermission('USER_VIEW'),
    loadComponent: () =>
      import('./features/user-management/user-management.component').then(
        (m) => m.UserManagementComponent
      ),
  },
  {
    path: 'role-management',
    canActivate: withPermission('ROLE_VIEW'),
    loadComponent: () =>
      import('./features/role-management/role-management.component').then(
        (m) => m.RoleManagementComponent
      ),
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
