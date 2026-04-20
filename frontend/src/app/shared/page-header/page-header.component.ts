import { Component, Input, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { BreadcrumbService } from '../../core/breadcrumb/breadcrumb.service';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [RouterLink, MatIconModule],
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.scss',
})
export class PageHeaderComponent {
  @Input() title = '';
  @Input() subtitle = '';

  protected readonly breadcrumbService = inject(BreadcrumbService);
  protected readonly breadcrumbs = this.breadcrumbService.breadcrumbs;
}
