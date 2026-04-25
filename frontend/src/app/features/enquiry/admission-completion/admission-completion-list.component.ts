import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurrencyPipe } from '@angular/common';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/toast/toast.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-admission-completion-list',
  standalone: true,
  imports: [
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    CurrencyPipe,
    PageHeaderComponent,
  ],
  templateUrl: './admission-completion-list.component.html',
  styleUrl: './admission-completion-list.component.scss',
})
export class AdmissionCompletionListComponent implements OnInit {
  private readonly enquiryService = inject(EnquiryService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly enquiries = signal<Enquiry[]>([]);
  protected readonly searchQuery = signal('');

  protected readonly filteredData = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return this.enquiries();
    return this.enquiries().filter(e =>
      e.name?.toLowerCase().includes(q) ||
      e.programName?.toLowerCase().includes(q) ||
      e.courseName?.toLowerCase().includes(q) ||
      e.phone?.toLowerCase().includes(q),
    );
  });

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['name', 'programName', 'courseName', 'status', 'finalizedNetFee', 'enquiryDate', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Name',
    programName: 'Program',
    courseName: 'Course',
    status: 'Status',
    finalizedNetFee: 'Net Fee',
    enquiryDate: 'Enquiry Date',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'admission-completion-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));

  protected isAdminOrFrontOffice(): boolean {
    return this.authService.hasRole('ROLE_ADMIN') || this.authService.hasRole('ROLE_FRONT_OFFICE');
  }

  protected initials(name: string): string {
    return (name ?? '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(w => w[0].toUpperCase())
      .join('');
  }

  ngOnInit(): void {
    this.load();
  }

  private _loadColPrefs(): Set<string> {
    try {
      const s = localStorage.getItem(this.COLS_KEY);
      if (s) return new Set<string>(JSON.parse(s) as string[]);
    } catch { /* empty */ }
    return new Set<string>(this.ALL_COLS);
  }

  protected toggleColumn(col: string): void {
    this._visibleCols.update(s => {
      const next = new Set(s);
      if (next.size > 1 && next.has(col)) { next.delete(col); } else { next.add(col); }
      localStorage.setItem(this.COLS_KEY, JSON.stringify([...next]));
      return next;
    });
  }

  protected isColumnVisible(col: string): boolean { return this._visibleCols().has(col); }

  private load(): void {
    this.loading.set(true);
    this.enquiryService.getAdmissionPending().subscribe({
      next: (enquiries) => {
        this.enquiries.set(enquiries);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load enquiries');
        this.loading.set(false);
      },
    });
  }

  protected viewEnquiry(item: Enquiry): void {
    void this.router.navigate(['/enquiries', item.id]);
  }

  protected completeAdmission(item: Enquiry): void {
    void this.router.navigate(['/enquiries', item.id, 'convert']);
  }
}
