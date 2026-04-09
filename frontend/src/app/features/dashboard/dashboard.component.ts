import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  imports: [MatCardModule, MatIconModule],
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
