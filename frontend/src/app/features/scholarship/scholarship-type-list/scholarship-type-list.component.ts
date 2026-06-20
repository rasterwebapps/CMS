import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { SCHOLARSHIP_TYPE_LIST_TOUR } from '../../../shared/tour/tours/scholarship.tours';
import { ScholarshipType } from '../scholarship.model';
import { ScholarshipService } from '../scholarship.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';

@Component({
  selector: 'app-scholarship-type-list',
  standalone: true,
  imports: [RouterLink, MatIconModule, MatProgressSpinnerModule, MatTableModule, MatSortModule, InrPipe, CmsEmptyStateComponent, CmsStatusBadgeComponent, CmsTourButtonComponent, CmsRowActionButtonComponent],
  templateUrl: './scholarship-type-list.component.html',
  styleUrl: './scholarship-type-list.component.scss',
})
export class ScholarshipTypeListComponent implements OnInit {
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly scholarships = signal<ScholarshipType[]>([]);
  protected readonly displayedColumns = ['code', 'name', 'discountType', 'discountValue', 'renewalRequired', 'active', 'actions'];
  protected readonly sortState = signal<Sort>({ active: '', direction: '' });

  protected readonly filteredScholarships = computed(() => {
    const q = this.searchValue().toLowerCase().trim();
    const rows = !q ? this.scholarships() : this.scholarships().filter(s =>
      s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q),
    );

    return this.sortRows(rows, this.sortState());
  });

  ngOnInit(): void {
    this.tourService.register('scholarship-type-list', SCHOLARSHIP_TYPE_LIST_TOUR);
    this.load();
  }

  protected applySearch(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
  }

  protected clearSearch(): void {
    this.searchValue.set('');
  }

  protected onSort(sort: Sort): void {
    this.sortState.set(sort);
  }

  protected edit(row: ScholarshipType): void {
    void this.router.navigate(['/scholarships', row.id, 'edit']);
  }

  protected toggleStatus(row: ScholarshipType): void {
    const nextAction = row.active ? 'Deactivate' : 'Activate';
    if (!confirm(`${nextAction} ${row.name}?`)) return;
    const request$ = row.active
      ? this.scholarshipService.deactivateScholarshipType(row.id)
      : this.scholarshipService.reactivateScholarshipType(row.id);
    request$.subscribe({
      next: () => {
        this.toast.success(`Scholarship ${row.active ? 'deactivated' : 'activated'}`);
        this.load();
      },
      error: (err) => this.toast.error(
        err?.error?.message ?? `Failed to ${row.active ? 'deactivate' : 'activate'} scholarship`,
      ),
    });
  }

  private sortRows(rows: ScholarshipType[], sort: Sort): ScholarshipType[] {
    if (!sort.active || !sort.direction) return rows;
    const factor = sort.direction === 'asc' ? 1 : -1;
    return [...rows].sort((a, b) => {
      const av = this.sortValue(a, sort.active);
      const bv = this.sortValue(b, sort.active);
      if (av === bv) return 0;
      if (av == null) return 1;
      if (bv == null) return -1;
      return av < bv ? -1 * factor : factor;
    });
  }

  private sortValue(row: ScholarshipType, column: string): string | number | boolean | null | undefined {
    switch (column) {
      case 'discountValue': return row.discountValue ?? 0;
      case 'renewalRequired': return row.renewalRequired;
      case 'active': return row.active;
      default: return String((row as unknown as Record<string, unknown>)[column] ?? '').toLowerCase();
    }
  }

  private load(): void {
    this.loading.set(true);
    this.scholarshipService.getScholarshipTypes().subscribe({
      next: data => { this.scholarships.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load scholarships'); this.loading.set(false); },
    });
  }
}

