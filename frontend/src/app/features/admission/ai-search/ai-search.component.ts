import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AdmissionService } from '../admission.service';
import { AdmissionDocumentRagResult } from '../admission.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';

@Component({
  selector: 'app-admission-document-ai-search',
  standalone: true,
  imports: [FormsModule, MatProgressSpinnerModule, CmsEmptyStateComponent],
  templateUrl: './ai-search.component.html',
  styleUrl: './ai-search.component.scss',
})
export class AdmissionDocumentAiSearchComponent {
  private readonly admissionService = inject(AdmissionService);
  private readonly router = inject(Router);

  protected readonly queryText = signal('');
  protected readonly lastSearchedQuery = signal<string | null>(null);
  protected readonly results = signal<AdmissionDocumentRagResult[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected search(): void {
    const query = this.queryText().trim();
    if (!query) return;

    this.loading.set(true);
    this.errorMessage.set(null);
    this.admissionService.aiSearchDocuments(query).subscribe({
      next: (results) => {
        this.results.set(results);
        this.lastSearchedQuery.set(query);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.results.set([]);
        this.lastSearchedQuery.set(query);
        this.errorMessage.set(
          err.status === 400
            ? (err.error?.message || "Couldn't understand that question. Try rephrasing it.")
            : 'AI document search is temporarily unavailable. Please try again shortly.',
        );
        this.loading.set(false);
      },
    });
  }

  protected viewDocumentSource(result: AdmissionDocumentRagResult): void {
    if (result.admissionId == null) return;
    void this.router.navigate(['/admissions', result.admissionId]);
  }

  /** Same snake_case -> Title Case fallback used by document-verification-detail.component.ts. */
  protected formatDocType(type: string): string {
    return type
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  protected formatSimilarity(similarity: number): string {
    return `${Math.round(similarity * 100)}% match`;
  }
}
