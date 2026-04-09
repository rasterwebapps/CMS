import { Component, signal, computed, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from './core/auth/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: string[];
}

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatListModule,
    MatMenuModule,
    MatTooltipModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly authService = inject(AuthService);

  protected readonly title = signal('College Management System');
  protected readonly sidenavOpened = signal(true);
  protected readonly darkTheme = signal(
    typeof localStorage !== 'undefined' && localStorage.getItem('cms-theme') === 'dark'
  );

  protected readonly username = this.authService.username;
  protected readonly authenticated = this.authService.authenticated;

  constructor() {
    if (this.darkTheme()) {
      document.documentElement.classList.add('dark-theme');
    }
  }

  private readonly allNavItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
  ];

  protected readonly navItems = computed(() => {
    return this.allNavItems.filter((item) => {
      if (!item.roles) return true;
      return this.authService.hasAnyRole(...item.roles);
    });
  });

  toggleSidenav(): void {
    this.sidenavOpened.update((opened) => !opened);
  }

  toggleTheme(): void {
    this.darkTheme.update((dark) => !dark);
    const htmlElement = document.documentElement;
    if (this.darkTheme()) {
      htmlElement.classList.add('dark-theme');
      htmlElement.classList.remove('light-theme');
      localStorage.setItem('cms-theme', 'dark');
    } else {
      htmlElement.classList.add('light-theme');
      htmlElement.classList.remove('dark-theme');
      localStorage.setItem('cms-theme', 'light');
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
