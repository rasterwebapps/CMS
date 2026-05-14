import { Component, computed, inject } from '@angular/core';
import { AdminDashboardComponent } from './admin/admin-dashboard.component';
import { FrontOfficeDashboardComponent } from './front-office/front-office-dashboard.component';
import { FacultyDashboardComponent } from './faculty/faculty-dashboard.component';
import { CashierDashboardComponent } from './cashier/cashier-dashboard.component';
import { StudentDashboardComponent } from './student/student-dashboard.component';
import { PermissionService } from '../../core/permissions/permission.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    AdminDashboardComponent,
    FrontOfficeDashboardComponent,
    FacultyDashboardComponent,
    CashierDashboardComponent,
    StudentDashboardComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent {
  protected readonly permissionService = inject(PermissionService);
  private readonly authService = inject(AuthService);

  protected readonly activeDashboard = computed<'admin' | 'front-office' | 'cashier' | 'faculty' | 'student' | null>(() => {
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
    if (this.permissionService.isRole('student')) {
      return 'student';
    }
    return null;
  });

  /** Signs the user out of Keycloak — shown on the "Account Not Configured" screen. */
  protected logout(): void {
    void this.authService.logout();
  }
}

