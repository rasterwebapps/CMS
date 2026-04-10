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
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
