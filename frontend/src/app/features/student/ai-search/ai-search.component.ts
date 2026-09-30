import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { StudentService } from '../student.service';
import { Student } from '../student.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';

const DEFAULT_PAGE_SIZE = 25;
const DISPLAYED_COLUMNS = ['name', 'rollAdmission', 'program', 'status'];

@Component({
  selector: 'app-ai-search',
  standalone: true,
  imports: [
    FormsModule,
    MatTableModule, MatPaginatorModule, MatProgressSpinnerModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './ai-search.component.html',
  styleUrl: './ai-search.component.scss',
})
export class AiSearchComponent {
  private readonly studentService = inject(StudentService);
  private readonly router = inject(Router);

  protected readonly displayedColumns = DISPLAYED_COLUMNS;

  protected readonly queryText = signal('');
  protected readonly lastSearchedQuery = signal<string | null>(null);
  protected readonly results = signal<Student[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  private page = 0;
  private readonly pageSize = DEFAULT_PAGE_SIZE;

  protected search(): void {
    const query = this.queryText().trim();
    if (!query) return;
    this.page = 0;
    this.runSearch(query);
  }

  protected onPage(event: PageEvent): void {
    const query = this.lastSearchedQuery();
    if (!query) return;
    this.page = event.pageIndex;
    this.runSearch(query, event.pageSize);
  }

  protected viewStudent(student: Student): void {
    void this.router.navigate(['/students', student.id]);
  }

  private runSearch(query: string, pageSize = this.pageSize): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.studentService.aiSearch(query, this.page, pageSize).subscribe({
      next: (result) => {
        this.results.set(result.content);
        this.totalElements.set(result.totalElements);
        this.lastSearchedQuery.set(query);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.results.set([]);
        this.totalElements.set(0);
        this.lastSearchedQuery.set(query);
        this.errorMessage.set(
          err.status === 400
            ? (err.error?.message || "Couldn't understand that query. Try rephrasing it.")
            : 'AI search is temporarily unavailable. Please try again shortly.',
        );
        this.loading.set(false);
      },
    });
  }
}
