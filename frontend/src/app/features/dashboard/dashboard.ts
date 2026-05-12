import { Component, computed, inject } from '@angular/core';
import { AdminDashboardComponent } from './admin/admin-dashboard.component';
import { FrontOfficeDashboardComponent } from './front-office/front-office-dashboard.component';
import { FacultyDashboardComponent } from './faculty/faculty-dashboard.component';
import { CashierDashboardComponent } from './cashier/cashier-dashboard.component';
import { PermissionService } from '../../core/permissions/permission.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    AdminDashboardComponent,
    FrontOfficeDashboardComponent,
    FacultyDashboardComponent,
    CashierDashboardComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent {
  private readonly permissionService = inject(PermissionService);

  protected readonly activeDashboard = computed<'admin' | 'front-office' | 'cashier' | 'faculty' | null>(() => {
    if (this.permissionService.isRole('devadmin', 'supportadmin', 'admin', 'collegeadmin', 'college_admin')) {
      return 'admin';
    }
    if (this.permissionService.isRole('frontoffice', 'front_office')) {
      return 'front-office';
    }
    if (this.permissionService.isRole('cashier')) {
      return 'cashier';
    }
    if (this.permissionService.isRole('faculty')) {
      return 'faculty';
    }
    return null;
  });
}

