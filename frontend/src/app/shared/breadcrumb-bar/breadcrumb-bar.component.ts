import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { BreadcrumbService } from '../../core/breadcrumb/breadcrumb.service';

/**
 * Slim breadcrumb strip rendered globally inside the app shell, above the
 * router outlet. Consumes `BreadcrumbService.breadcrumbs()` and renders nothing
 * when there is only the root crumb (or none at all).
 */
@Component({
  selector: 'app-breadcrumb-bar',
  standalone: true,
  imports: [RouterLink, MatIconModule],
  templateUrl: './breadcrumb-bar.component.html',
  styleUrl: './breadcrumb-bar.component.scss',
})
export class BreadcrumbBarComponent {
  private readonly breadcrumbService = inject(BreadcrumbService);
  protected readonly breadcrumbs = this.breadcrumbService.breadcrumbs;
}
