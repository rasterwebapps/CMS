import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { ScholarshipType } from '../scholarship.model';
import { ScholarshipService } from '../scholarship.service';

@Component({
  selector: 'app-scholarship-type-list',
  standalone: true,
  imports: [RouterLink, MatIconModule, MatProgressSpinnerModule, InrPipe, CmsEmptyStateComponent],
  templateUrl: './scholarship-type-list.component.html',
  styleUrl: './scholarship-type-list.component.scss',
})
export class ScholarshipTypeListComponent implements OnInit {
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly scholarships = signal<ScholarshipType[]>([]);

  protected readonly filteredScholarships = computed(() => {
    const q = this.searchValue().toLowerCase().trim();
    if (!q) return this.scholarships();
    return this.scholarships().filter(s =>
      s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q),
    );
  });

  ngOnInit(): void {
    this.load();
  }

  protected applySearch(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
  }

  protected clearSearch(): void {
    this.searchValue.set('');
  }

  protected edit(row: ScholarshipType): void {
    void this.router.navigate(['/scholarships', row.id, 'edit']);
  }

  protected deactivate(row: ScholarshipType): void {
    if (!confirm(`Deactivate ${row.name}?`)) return;
    this.scholarshipService.deactivateScholarshipType(row.id).subscribe({
      next: () => { this.toast.success('Scholarship deactivated'); this.load(); },
      error: () => this.toast.error('Failed to deactivate scholarship'),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.scholarshipService.getScholarshipTypes().subscribe({
      next: data => { this.scholarships.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load scholarships'); this.loading.set(false); },
    });
  }
}

