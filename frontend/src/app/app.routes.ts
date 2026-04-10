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
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
