import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard').then((m) => m.DashboardComponent),
  },
  {
    path: 'departments',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/department/department-list/department-list.component').then(
        (m) => m.DepartmentListComponent
      ),
  },
  {
    path: 'departments/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/department/department-form/department-form.component').then(
        (m) => m.DepartmentFormComponent
      ),
  },
  {
    path: 'departments/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/department/department-form/department-form.component').then(
        (m) => m.DepartmentFormComponent
      ),
  },
  {
    path: 'programs',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/program/program-list/program-list.component').then(
        (m) => m.ProgramListComponent
      ),
  },
  {
    path: 'programs/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/program/program-form/program-form.component').then(
        (m) => m.ProgramFormComponent
      ),
  },
  {
    path: 'programs/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/program/program-form/program-form.component').then(
        (m) => m.ProgramFormComponent
      ),
  },
  {
    path: 'courses',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/course/course-list/course-list.component').then(
        (m) => m.CourseListComponent
      ),
  },
  {
    path: 'courses/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/course/course-form/course-form.component').then(
        (m) => m.CourseFormComponent
      ),
  },
  {
    path: 'courses/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/course/course-form/course-form.component').then(
        (m) => m.CourseFormComponent
      ),
  },
  {
    path: 'academic-years',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/academic-year/academic-year-list/academic-year-list.component').then(
        (m) => m.AcademicYearListComponent
      ),
  },
  {
    path: 'academic-years/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/academic-year/academic-year-form/academic-year-form.component').then(
        (m) => m.AcademicYearFormComponent
      ),
  },
  {
    path: 'academic-years/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/academic-year/academic-year-form/academic-year-form.component').then(
        (m) => m.AcademicYearFormComponent
      ),
  },
  {
    path: 'semesters',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/academic-year/semester-list/semester-list.component').then(
        (m) => m.SemesterListComponent
      ),
  },
  {
    path: 'semesters/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/academic-year/semester-form/semester-form.component').then(
        (m) => m.SemesterFormComponent
      ),
  },
  {
    path: 'semesters/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/academic-year/semester-form/semester-form.component').then(
        (m) => m.SemesterFormComponent
      ),
  },
  {
    path: 'academic-calendar',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/academic-year/academic-calendar/academic-calendar.component').then(
        (m) => m.AcademicCalendarComponent
      ),
  },
  {
    path: 'labs',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lab/lab-list/lab-list.component').then((m) => m.LabListComponent),
  },
  {
    path: 'labs/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lab/lab-form/lab-form.component').then((m) => m.LabFormComponent),
  },
  {
    path: 'labs/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lab/lab-detail/lab-detail.component').then((m) => m.LabDetailComponent),
  },
  {
    path: 'labs/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lab/lab-form/lab-form.component').then((m) => m.LabFormComponent),
  },
  {
    path: 'faculty',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/faculty/faculty-list/faculty-list.component').then(
        (m) => m.FacultyListComponent
      ),
  },
  {
    path: 'faculty/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/faculty/faculty-form/faculty-form.component').then(
        (m) => m.FacultyFormComponent
      ),
  },
  {
    path: 'faculty/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/faculty/faculty-detail/faculty-detail.component').then(
        (m) => m.FacultyDetailComponent
      ),
  },
  {
    path: 'faculty/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/faculty/faculty-form/faculty-form.component').then(
        (m) => m.FacultyFormComponent
      ),
  },
  {
    path: 'students',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/student/student-list/student-list.component').then(
        (m) => m.StudentListComponent
      ),
  },
  {
    path: 'students/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/student/student-form/student-form.component').then(
        (m) => m.StudentFormComponent
      ),
  },
  {
    path: 'students/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/student/student-detail/student-detail.component').then(
        (m) => m.StudentDetailComponent
      ),
  },
  {
    path: 'students/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/student/student-form/student-form.component').then(
        (m) => m.StudentFormComponent
      ),
  },
  {
    path: 'attendance',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/attendance/attendance-list/attendance-list.component').then(
        (m) => m.AttendanceListComponent
      ),
  },
  {
    path: 'attendance/mark',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/attendance/attendance-mark/attendance-mark.component').then(
        (m) => m.AttendanceMarkComponent
      ),
  },
  {
    path: 'fee-structures',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/finance/fee-structure-list/fee-structure-list.component').then(
        (m) => m.FeeStructureListComponent
      ),
  },
  {
    path: 'fee-structures/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/finance/fee-structure-form/fee-structure-form.component').then(
        (m) => m.FeeStructureFormComponent
      ),
  },
  {
    path: 'fee-structures/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/finance/fee-structure-form/fee-structure-form.component').then(
        (m) => m.FeeStructureFormComponent
      ),
  },
  {
    path: 'fee-payments',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/finance/fee-payment-list/fee-payment-list.component').then(
        (m) => m.FeePaymentListComponent
      ),
  },
  {
    path: 'fee-payments/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/finance/fee-payment-form/fee-payment-form.component').then(
        (m) => m.FeePaymentFormComponent
      ),
  },
  {
    path: 'equipment',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/equipment/equipment-list/equipment-list.component').then(
        (m) => m.EquipmentListComponent
      ),
  },
  {
    path: 'equipment/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/equipment/equipment-form/equipment-form.component').then(
        (m) => m.EquipmentFormComponent
      ),
  },
  {
    path: 'equipment/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/equipment/equipment-form/equipment-form.component').then(
        (m) => m.EquipmentFormComponent
      ),
  },
  {
    path: 'inventory',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/inventory/inventory-list/inventory-list.component').then(
        (m) => m.InventoryListComponent
      ),
  },
  {
    path: 'inventory/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/inventory/inventory-form/inventory-form.component').then(
        (m) => m.InventoryFormComponent
      ),
  },
  {
    path: 'inventory/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/inventory/inventory-form/inventory-form.component').then(
        (m) => m.InventoryFormComponent
      ),
  },
  {
    path: 'maintenance',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/maintenance/maintenance-list/maintenance-list.component').then(
        (m) => m.MaintenanceListComponent
      ),
  },
  {
    path: 'maintenance/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/maintenance/maintenance-form/maintenance-form.component').then(
        (m) => m.MaintenanceFormComponent
      ),
  },
  {
    path: 'maintenance/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/maintenance/maintenance-form/maintenance-form.component').then(
        (m) => m.MaintenanceFormComponent
      ),
  },
  {
    path: 'examinations',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/examination/examination-list/examination-list.component').then(
        (m) => m.ExaminationListComponent
      ),
  },
  {
    path: 'examinations/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/examination/examination-form/examination-form.component').then(
        (m) => m.ExaminationFormComponent
      ),
  },
  {
    path: 'examinations/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/examination/examination-form/examination-form.component').then(
        (m) => m.ExaminationFormComponent
      ),
  },
  {
    path: 'exam-results',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/examination/exam-result-list/exam-result-list.component').then(
        (m) => m.ExamResultListComponent
      ),
  },
  {
    path: 'syllabi',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/curriculum/syllabus-list/syllabus-list.component').then(
        (m) => m.SyllabusListComponent
      ),
  },
  {
    path: 'syllabi/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/curriculum/syllabus-form/syllabus-form.component').then(
        (m) => m.SyllabusFormComponent
      ),
  },
  {
    path: 'syllabi/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/curriculum/syllabus-form/syllabus-form.component').then(
        (m) => m.SyllabusFormComponent
      ),
  },
  {
    path: 'experiments',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/curriculum/experiment-list/experiment-list.component').then(
        (m) => m.ExperimentListComponent
      ),
  },
  {
    path: 'experiments/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/curriculum/experiment-form/experiment-form.component').then(
        (m) => m.ExperimentFormComponent
      ),
  },
  {
    path: 'experiments/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/curriculum/experiment-form/experiment-form.component').then(
        (m) => m.ExperimentFormComponent
      ),
  },
  {
    path: 'curriculum-mappings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/curriculum/co-po-mapping/co-po-mapping.component').then(
        (m) => m.CoPoMappingComponent
      ),
  },
  {
    path: 'curriculum-mappings/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/curriculum/co-po-mapping-form/co-po-mapping-form.component').then(
        (m) => m.CoPoMappingFormComponent
      ),
  },
  {
    path: 'curriculum-mappings/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/curriculum/co-po-mapping-form/co-po-mapping-form.component').then(
        (m) => m.CoPoMappingFormComponent
      ),
  },
  {
    path: 'lab-schedules',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lab-schedule/lab-schedule-list/lab-schedule-list.component').then(
        (m) => m.LabScheduleListComponent
      ),
  },
  {
    path: 'lab-schedules/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lab-schedule/lab-schedule-form/lab-schedule-form.component').then(
        (m) => m.LabScheduleFormComponent
      ),
  },
  {
    path: 'lab-schedules/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lab-schedule/lab-schedule-form/lab-schedule-form.component').then(
        (m) => m.LabScheduleFormComponent
      ),
  },
  {
    path: 'reports',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/reports/reports-dashboard/reports-dashboard.component').then(
        (m) => m.ReportsDashboardComponent
      ),
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/settings/system-configuration-list/system-configuration-list.component').then(
        (m) => m.SystemConfigurationListComponent
      ),
  },
  {
    path: 'settings/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/settings/system-configuration-form/system-configuration-form.component').then(
        (m) => m.SystemConfigurationFormComponent
      ),
  },
  {
    path: 'settings/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/settings/system-configuration-form/system-configuration-form.component').then(
        (m) => m.SystemConfigurationFormComponent
      ),
  },
  {
    path: 'agents',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/agent/agent-list/agent-list.component').then(
        (m) => m.AgentListComponent
      ),
  },
  {
    path: 'agents/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/agent/agent-form/agent-form.component').then(
        (m) => m.AgentFormComponent
      ),
  },
  {
    path: 'agents/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/agent/agent-form/agent-form.component').then(
        (m) => m.AgentFormComponent
      ),
  },
  {
    path: 'enquiries',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/enquiry/enquiry-list/enquiry-list.component').then(
        (m) => m.EnquiryListComponent
      ),
  },
  {
    path: 'enquiries/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/enquiry/enquiry-form/enquiry-form.component').then(
        (m) => m.EnquiryFormComponent
      ),
  },
  {
    path: 'enquiries/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/enquiry/enquiry-form/enquiry-form.component').then(
        (m) => m.EnquiryFormComponent
      ),
  },
  {
    path: 'student-fees',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/finance/fee-explorer/fee-explorer.component').then(
        (m) => m.FeeExplorerComponent
      ),
  },
  {
    path: 'student-fees/finalize',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/finance/fee-finalization/fee-finalization.component').then(
        (m) => m.FeeFinalizationComponent
      ),
  },
  {
    path: 'student-fees/:studentId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/finance/student-fee-detail/student-fee-detail.component').then(
        (m) => m.StudentFeeDetailComponent
      ),
  },
  {
    path: 'referral-types',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/referral-type/referral-type-list/referral-type-list.component').then(
        (m) => m.ReferralTypeListComponent
      ),
  },
  {
    path: 'referral-types/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/referral-type/referral-type-form/referral-type-form.component').then(
        (m) => m.ReferralTypeFormComponent
      ),
  },
  {
    path: 'referral-types/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/referral-type/referral-type-form/referral-type-form.component').then(
        (m) => m.ReferralTypeFormComponent
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
