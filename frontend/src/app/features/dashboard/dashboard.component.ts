import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';

@Component({
  selector: 'app-dashboard',
  imports: [
    PageHeaderComponent,
    MatIconModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly authService = inject(AuthService);

  protected readonly username = this.authService.username;
  protected readonly roles = this.authService.roles;

  protected readonly cards = [
    {
      title: 'Students',
      icon: 'people',
      value: '—',
      subtitle: 'Total enrolled',
    },
    {
      title: 'Faculty',
      icon: 'school',
      value: '—',
      subtitle: 'Active members',
    },
    {
      title: 'Departments',
      icon: 'business',
      value: '—',
      subtitle: 'Active departments',
    },
    {
      title: 'Lab Utilization',
      icon: 'science',
      value: '—',
      subtitle: 'Current utilization',
    },
  ];
}
