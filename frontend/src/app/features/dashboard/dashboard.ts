import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  imports: [MatCardModule, MatIconModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent {
  protected readonly authService = inject(AuthService);

  protected readonly cards = [
    { title: 'Total Students', value: '—', icon: 'school', color: 'primary' },
    { title: 'Total Faculty', value: '—', icon: 'person', color: 'accent' },
    { title: 'Departments', value: '—', icon: 'business', color: 'warn' },
    { title: 'Active Courses', value: '—', icon: 'menu_book', color: 'primary' },
  ];
}
