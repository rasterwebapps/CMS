import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurrencyPipe } from '@angular/common';
import { EnquiryService } from '../enquiry.service';
import { Enquiry, EnquiryDocument, EnquiryDocumentRequest } from '../enquiry.model';
import { AuthService } from '../../../core/auth/auth.service';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';

/**
 * All document types supported by the system. Mirrors the backend
 * `com.cms.model.enums.DocumentType` enum.
 */
const ALL_DOCUMENT_TYPES = [
  'TENTH_MARKSHEET',
  'ELEVENTH_MARKSHEET',
  'TWELFTH_MARKSHEET',
  'TRANSFER_CERTIFICATE',
  'COMMUNITY_CERTIFICATE',
  'INCOME_CERTIFICATE',
  'NATIVITY_CERTIFICATE',
  'MIGRATION_CERTIFICATE',
  'FIRST_GRADUATE_CERTIFICATE',
  'PASSPORT_PHOTO',
  'SIGNED_AFFIDAVIT',
  'UNDERTAKING_DOCUMENT',
  'AADHAR_CARD',
  'MEDICAL_FITNESS',
  'ELIGIBILITY_CERTIFICATE',
] as const;

/**
 * Mandatory document types that must be UPLOADED or VERIFIED before the
 * enquiry can transition to DOCUMENTS_SUBMITTED. Mirrors the backend
 * `EnquiryDocumentService.MANDATORY_DOCUMENTS` set.
 */
const MANDATORY_DOCUMENT_TYPES: ReadonlySet<string> = new Set([
  'TENTH_MARKSHEET',
  'TWELFTH_MARKSHEET',
  'TRANSFER_CERTIFICATE',
  'AADHAR_CARD',
  'PASSPORT_PHOTO',
]);

interface ChecklistRow {
  documentType: string;
  /** The persisted document record, or null if nothing collected yet. */
  document: EnquiryDocument | null;
  status: string;
  remarks: string;
  isMandatory: boolean;
  saving: boolean;
}

@Component({
  selector: 'app-document-collection',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    CurrencyPipe,
    PageHeaderComponent,
    CmsStatusBadgeComponent,
  ],
  templateUrl: './document-collection.component.html',
  styleUrl: './document-collection.component.scss',
})
export class DocumentCollectionComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly layoutService = inject(LayoutService);

  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly enquiry = signal<Enquiry | null>(null);
  protected readonly rows = signal<ChecklistRow[]>([]);

  /** Number of mandatory documents successfully uploaded or verified. */
  protected readonly mandatorySatisfiedCount = computed(() =>
    this.rows().filter(
      (r) => r.isMandatory && (r.status === 'UPLOADED' || r.status === 'VERIFIED'),
    ).length,
  );

  protected readonly mandatoryTotal = MANDATORY_DOCUMENT_TYPES.size;

  protected readonly canSubmit = computed(
    () => this.mandatorySatisfiedCount() === this.mandatoryTotal && !this.submitting(),
  );

  /** Display label for the document type (e.g., TENTH_MARKSHEET → Tenth Marksheet). */
  protected formatDocType(type: string): string {
    return type
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  protected isAdminOrFrontOffice(): boolean {
    return this.authService.isAdmin() || this.authService.isFrontOffice();
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id || Number.isNaN(id)) {
      this.snackBar.open('Invalid enquiry id', 'Close', { duration: 3000 });
      void this.router.navigate(['/enquiries/document-submission']);
      return;
    }
    this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.enquiryService.getEnquiryById(id).subscribe({
      next: (enquiry) => {
        if (enquiry.status !== 'FEES_PAID' && enquiry.status !== 'PARTIALLY_PAID') {
          this.snackBar.open(
            'Documents can only be collected for enquiries in FEES_PAID or PARTIALLY_PAID status',
            'Close',
            { duration: 5000 },
          );
          void this.router.navigate(['/enquiries/document-submission']);
          return;
        }
        this.enquiry.set(enquiry);
        this.loadDocuments(id);
      },
      error: () => {
        this.snackBar.open('Failed to load enquiry', 'Close', { duration: 3000 });
        this.loading.set(false);
        void this.router.navigate(['/enquiries/document-submission']);
      },
    });
  }

  private loadDocuments(enquiryId: number): void {
    this.enquiryService.getDocuments(enquiryId).subscribe({
      next: (docs) => {
        this.rows.set(this.buildChecklist(docs));
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load documents', 'Close', { duration: 3000 });
        this.rows.set(this.buildChecklist([]));
        this.loading.set(false);
      },
    });
  }

  private buildChecklist(documents: EnquiryDocument[]): ChecklistRow[] {
    const byType = new Map(documents.map((d) => [d.documentType, d]));
    return ALL_DOCUMENT_TYPES.map((type) => {
      const existing = byType.get(type) ?? null;
      return {
        documentType: type,
        document: existing,
        status: existing?.status ?? 'NOT_UPLOADED',
        remarks: existing?.remarks ?? '',
        isMandatory: MANDATORY_DOCUMENT_TYPES.has(type),
        saving: false,
      } satisfies ChecklistRow;
    });
  }

  /** Persists a row — creates a new EnquiryDocument or updates the existing one. */
  protected saveRow(row: ChecklistRow, newStatus: string): void {
    const enquiryId = this.enquiry()?.id;
    if (!enquiryId || !this.isAdminOrFrontOffice()) return;

    const request: EnquiryDocumentRequest = {
      documentType: row.documentType,
      status: newStatus,
      remarks: row.remarks?.trim() || undefined,
    };

    this.updateRow(row, { ...row, saving: true });

    const onSuccess = (saved: EnquiryDocument): void => {
      this.updateRow(row, {
        ...row,
        document: saved,
        status: saved.status,
        remarks: saved.remarks ?? '',
        saving: false,
      });
      this.snackBar.open(
        `${this.formatDocType(row.documentType)} marked as ${this.formatDocType(saved.status)}`,
        'Close',
        { duration: 2500 },
      );
    };
    const onError = (): void => {
      this.updateRow(row, { ...row, saving: false });
      this.snackBar.open(`Failed to update ${this.formatDocType(row.documentType)}`, 'Close', {
        duration: 3000,
      });
    };

    if (row.document) {
      this.enquiryService
        .updateDocument(enquiryId, row.document.id, request)
        .subscribe({ next: onSuccess, error: onError });
    } else {
      this.enquiryService.addDocument(enquiryId, request).subscribe({ next: onSuccess, error: onError });
    }
  }

  protected onRemarksBlur(row: ChecklistRow): void {
    if (!row.document) return; // Nothing persisted yet — remarks will be sent on first save.
    const persisted = (row.document.remarks ?? '').trim();
    const current = (row.remarks ?? '').trim();
    if (persisted === current) return;
    this.saveRow(row, row.status);
  }

  protected removeRow(row: ChecklistRow): void {
    const enquiryId = this.enquiry()?.id;
    if (!enquiryId || !row.document || !this.isAdminOrFrontOffice()) return;

    this.updateRow(row, { ...row, saving: true });
    this.enquiryService.deleteDocument(enquiryId, row.document.id).subscribe({
      next: () => {
        this.updateRow(row, {
          ...row,
          document: null,
          status: 'NOT_UPLOADED',
          remarks: '',
          saving: false,
        });
        this.snackBar.open(`${this.formatDocType(row.documentType)} cleared`, 'Close', {
          duration: 2500,
        });
      },
      error: () => {
        this.updateRow(row, { ...row, saving: false });
        this.snackBar.open(
          `Failed to clear ${this.formatDocType(row.documentType)}`,
          'Close',
          { duration: 3000 },
        );
      },
    });
  }

  private updateRow(target: ChecklistRow, next: ChecklistRow): void {
    this.rows.update((rs) => rs.map((r) => (r.documentType === target.documentType ? next : r)));
  }

  protected submitDocuments(): void {
    const enquiryId = this.enquiry()?.id;
    if (!enquiryId || !this.canSubmit()) return;

    this.submitting.set(true);
    this.enquiryService.submitDocuments(enquiryId).subscribe({
      next: () => {
        this.snackBar.open('Documents submitted successfully', 'Close', { duration: 3000 });
        this.submitting.set(false);
        void this.router.navigate(['/enquiries/document-submission']);
      },
      error: (err) => {
        const missing = err?.error?.missingDocumentTypes as string[] | undefined;
        const message =
          missing && missing.length > 0
            ? `Missing documents: ${missing.map((m) => this.formatDocType(m)).join(', ')}`
            : (err?.error?.message ?? 'Failed to submit documents');
        this.snackBar.open(message, 'Close', { duration: 6000 });
        this.submitting.set(false);
      },
    });
  }

  protected backToList(): void {
    void this.router.navigate(['/enquiries/document-submission']);
  }
}
