import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { EnquiryService } from '../enquiry.service';
import { Enquiry, EnquiryDocument, EnquiryDocumentRequest } from '../enquiry.model';
import { ProgramService } from '../../program/program.service';
import { DocumentTypeInfo } from '../../program/program.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { DOCUMENT_COLLECTION_TOUR } from '../../../shared/tour/tours/enquiry.tours';

const MAX_DOCUMENT_UPLOAD_BYTES = 10 * 1024 * 1024;
const ALLOWED_UPLOAD_MIME_TYPES = new Set([
  'application/pdf',
  'image/jpeg',
  'image/png',
]);
const ALLOWED_UPLOAD_EXTENSIONS = new Set(['pdf', 'jpg', 'jpeg', 'png']);


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
    InrPipe,
    FormsModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsTourButtonComponent],
  templateUrl: './document-collection.component.html',
  styleUrl: './document-collection.component.scss',
})
export class DocumentCollectionComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly programService = inject(ProgramService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  /**
   * When true the component is in "verify" mode — opened from document
   * verification for a DOCUMENTS_SUBMITTED enquiry.
   * In this mode:
   *  - VERIFIED documents are locked (no edits allowed).
   *  - The "Submit Documents" button is replaced by "Back to Admission".
   */
  protected readonly verifyMode = signal(false);

  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly enquiry = signal<Enquiry | null>(null);
  protected readonly rows = signal<ChecklistRow[]>([]);

  /** Tracks which document type is currently showing the inline rejection-reason prompt. */
  protected readonly rejectingDocumentType = signal<string | null>(null);
  /** Backing value for the inline rejection-reason input. */
  protected readonly rejectReason = signal<string>('');

  /** Catalogue of all document types (with display labels), loaded from the backend. */
  private readonly documentCatalogue = signal<DocumentTypeInfo[]>([]);
  private readonly labelMap = signal<Map<string, string>>(new Map());

  /** Mandatory document types resolved from the program configuration. */
  private readonly mandatoryTypes = signal<ReadonlySet<string>>(new Set());
  /** Optional document types resolved from the program configuration. */
  private readonly optionalTypes = signal<ReadonlySet<string>>(new Set());

  /** Number of mandatory documents successfully uploaded or verified. */
  protected readonly mandatorySatisfiedCount = computed(
    () =>
      this.rows().filter(
        (r) => r.isMandatory && (r.status === 'UPLOADED' || r.status === 'VERIFIED'),
      ).length,
  );

  /** Number of mandatory documents that have been VERIFIED (for verify-mode progress). */
  protected readonly mandatoryVerifiedCount = computed(
    () => this.rows().filter((r) => r.isMandatory && r.status === 'VERIFIED').length,
  );

  /** True when all mandatory documents are VERIFIED — enables "Back to Admission" in verify mode. */
  protected readonly allMandatoryVerified = computed(
    () => this.mandatoryVerifiedCount() === this.mandatoryTotal(),
  );

  protected readonly mandatoryTotal = computed(() => this.mandatoryTypes().size);

  protected readonly canSubmit = computed(
    () => this.mandatorySatisfiedCount() === this.mandatoryTotal()
      && !this.submitting(),
  );

  protected readonly mandatoryRows = computed(() => this.rows().filter((r) => r.isMandatory));
  protected readonly optionalRows = computed(() => this.rows().filter((r) => !r.isMandatory));

  protected readonly mandatoryProgressPct = computed(() => {
    const total = this.mandatoryTotal();
    if (total === 0) return 0;
    return Math.round((this.mandatorySatisfiedCount() / total) * 100);
  });

  /** Display label for a document type code, sourced from the backend catalogue. */
  protected formatDocType(type: string): string {
    return this.labelMap().get(type) ?? type
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  protected canManageDocuments(): boolean {
    return this.permissionService.has('DOCUMENT_SUBMISSION_MANAGE');
  }

  protected initials(name: string): string {
    return (
      (name ?? '')
        .split(' ')
        .slice(0, 2)
        .map((w) => w[0])
        .join('')
        .toUpperCase() || '?'
    );
  }

  protected getDocumentIcon(type: string): string {
    if (type.includes('MARKSHEET') || type.includes('TRANSCRIPT') || type.includes('DEGREE')
        || type.includes('GENUINENESS') || type.includes('PROVISIONAL') || type.includes('ELIGIBILITY')) return 'school';
    if (type.includes('PHOTO')) return 'face';
    if (type.includes('SIGNATURE')) return 'draw';
    if (type.includes('AADHAR')) return 'badge';
    if (type.includes('MEDICAL')) return 'medical_services';
    if (type.includes('AFFIDAVIT')) return 'gavel';
    if (type.includes('TC') || type.includes('TRANSFER')) return 'swap_horiz';
    if (type.includes('MIGRATION')) return 'flight_takeoff';
    return 'description';
  }

  ngOnInit(): void {
    this.tourService.register('document-collection', DOCUMENT_COLLECTION_TOUR);
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id || Number.isNaN(id)) {
      this.toast.warning('Invalid enquiry id');
      void this.router.navigate(['/enquiries/document-submission']);
      return;
    }
    // Query param ?mode=verify means we came from the Complete Admission list.
    const mode = this.route.snapshot.queryParamMap.get('mode');
    this.verifyMode.set(mode === 'verify');
    this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.enquiryService.getEnquiryById(id).subscribe({
      next: (enquiry) => {
        const allowedStatuses = this.verifyMode()
          ? ['FEES_PAID', 'PARTIALLY_PAID', 'DOCUMENTS_SUBMITTED']
          : ['FEES_PAID', 'PARTIALLY_PAID'];

        if (!allowedStatuses.includes(enquiry.status)) {
          this.toast.warning('Documents can only be collected for enquiries in FEES_PAID or PARTIALLY_PAID status');
          void this.router.navigate(['/enquiries/document-submission']);
          return;
        }
        this.enquiry.set(enquiry);
        this.loadCatalogueAndDocuments(id, enquiry.programId);
      },
      error: () => {
        this.toast.error('Failed to load enquiry');
        this.loading.set(false);
        void this.router.navigate(['/enquiries/document-submission']);
      },
    });
  }

  /**
   * Loads the document catalogue, the program's required document types,
   * and the persisted documents in parallel.
   */
  private loadCatalogueAndDocuments(enquiryId: number, programId: number | null | undefined): void {
    const emptyReqs = { mandatory: [] as string[], optional: [] as string[] };
    forkJoin({
      catalogue: this.programService.getAllDocumentTypes(),
      requirements: programId
        ? this.programService.getDocumentRequirements(programId).pipe(catchError(() => of(emptyReqs)))
        : of(emptyReqs),
      documents: this.enquiryService.getDocuments(enquiryId).pipe(
        catchError(() => {
          this.toast.error('Failed to load documents');
          return of<EnquiryDocument[]>([]);
        }),
      ),
    }).subscribe({
      next: ({ catalogue, requirements, documents }) => {
        this.documentCatalogue.set(catalogue);
        this.labelMap.set(new Map(catalogue.map((t) => [t.code, t.label])));
        const catalogueOrder = new Map(catalogue.map((t, i) => [t.code, i]));
        const sorted = [...requirements.mandatory].sort(
          (a, b) => (catalogueOrder.get(a) ?? 999) - (catalogueOrder.get(b) ?? 999),
        );
        this.mandatoryTypes.set(new Set(sorted));
        this.optionalTypes.set(new Set(requirements.optional));
        this.rows.set(this.buildChecklist(documents));
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load document catalogue');
        this.loading.set(false);
      },
    });
  }

  private buildChecklist(documents: EnquiryDocument[]): ChecklistRow[] {
    const byType = new Map(documents.map((d) => [d.documentType, d]));
    const mandatory = this.mandatoryTypes();
    const optional = this.optionalTypes();

    const requiredRows: ChecklistRow[] = Array.from(mandatory).map((type) => {
      const existing = byType.get(type) ?? null;
      return {
        documentType: type,
        document: existing,
        status: existing?.status ?? 'NOT_UPLOADED',
        remarks: existing?.remarks ?? '',
        isMandatory: true,
        saving: false,
      } satisfies ChecklistRow;
    });

    const optionalRows: ChecklistRow[] = Array.from(optional).map((type) => {
      const existing = byType.get(type) ?? null;
      return {
        documentType: type,
        document: existing,
        status: existing?.status ?? 'NOT_UPLOADED',
        remarks: existing?.remarks ?? '',
        isMandatory: false,
        saving: false,
      } satisfies ChecklistRow;
    });

    // Safety net: uploaded docs whose type is not in either mandatory or optional config.
    const allConfigured = new Set([...mandatory, ...optional]);
    const orphanRows: ChecklistRow[] = documents
      .filter((d) => !allConfigured.has(d.documentType))
      .map((d) => ({
        documentType: d.documentType,
        document: d,
        status: d.status,
        remarks: d.remarks ?? '',
        isMandatory: false,
        saving: false,
      } satisfies ChecklistRow));

    return [...requiredRows, ...optionalRows, ...orphanRows];
  }

  /** Persists a row — creates a new EnquiryDocument or updates the existing one. */
  protected saveRow(row: ChecklistRow, newStatus: string): void {
    const enquiryId = this.enquiry()?.id;
    if (!enquiryId || !this.canManageDocuments() || this.isRowLocked(row)) return;

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
      this.toast.info(`${this.formatDocType(row.documentType)} marked as ${this.formatDocType(saved.status)}`);
    };
    const onError = (): void => {
      this.updateRow(row, { ...row, saving: false });
      this.toast.error(`Failed to update ${this.formatDocType(row.documentType)}`);
    };

    if (row.document) {
      this.enquiryService
        .updateDocument(enquiryId, row.document.id, request)
        .subscribe({ next: onSuccess, error: onError });
    } else {
      this.enquiryService
        .addDocument(enquiryId, request)
        .subscribe({ next: onSuccess, error: onError });
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
    if (!enquiryId || !row.document || !this.canManageDocuments() || this.isRowLocked(row)) return;

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
        this.toast.success(`${this.formatDocType(row.documentType)} cleared`);
      },
      error: (err) => {
        this.updateRow(row, { ...row, saving: false });
        this.toast.error(err?.error?.message ?? `Failed to clear ${this.formatDocType(row.documentType)}`);
      },
    });
  }

  /**
   * Opens the native file browser for the given row. Triggered by the
   * Browse / Upload button in the document checklist.
   */
  protected onBrowseFile(row: ChecklistRow, input: HTMLInputElement): void {
    if (row.saving || !this.canManageDocuments() || this.isRowLocked(row)) return;
    input.value = '';
    input.click();
  }

  /**
   * Handles a chosen file from the native picker — uploads it to the backend
   * and replaces the row with the persisted document (which now carries the
   * file metadata).
   */
  protected onFileSelected(row: ChecklistRow, event: Event): void {
    const enquiryId = this.enquiry()?.id;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = ''; // Allow re-selecting the same file later
    if (!enquiryId || !file || !this.canManageDocuments() || this.isRowLocked(row)) return;

    // Mirror backend MAX_FILE_SIZE_BYTES (10 MB) for fast user feedback.
    if (file.size > MAX_DOCUMENT_UPLOAD_BYTES) {
      this.toast.warning('Only PDF, JPG, PNG files are allowed (max 10 MB)');
      return;
    }

    const extension = file.name.includes('.')
      ? file.name.split('.').pop()?.toLowerCase() ?? ''
      : '';
    const hasAllowedMime = ALLOWED_UPLOAD_MIME_TYPES.has(file.type.toLowerCase());
    const hasAllowedExtension = ALLOWED_UPLOAD_EXTENSIONS.has(extension);
    if (!hasAllowedMime && !hasAllowedExtension) {
      this.toast.warning('Only PDF, JPG, PNG files are allowed (max 10 MB)');
      return;
    }

    this.updateRow(row, { ...row, saving: true });
    this.enquiryService
      .uploadDocumentFile(enquiryId, row.documentType, file, row.remarks?.trim() || undefined)
      .subscribe({
        next: (saved) => {
          this.updateRow(row, {
            ...row,
            document: saved,
            status: saved.status,
            remarks: saved.remarks ?? '',
            saving: false,
          });
          this.toast.success(
            `${this.formatDocType(row.documentType)}: ${saved.fileName} uploaded`,
          );
        },
        error: (err) => {
          this.updateRow(row, { ...row, saving: false });
          const message =
            err?.error?.message ?? `Failed to upload ${this.formatDocType(row.documentType)}`;
          this.toast.error(message);
        },
      });
  }

  /**
   * Opens the stored document binary in a new browser tab. Falls back to a
   * download if the browser cannot render the MIME type inline.
   */
  protected viewFile(row: ChecklistRow): void {
    const enquiryId = this.enquiry()?.id;
    if (!enquiryId || !row.document?.hasFile) return;
    this.enquiryService.downloadDocumentFile(enquiryId, row.document.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const opened = window.open(url, '_blank');
        if (!opened) {
          // Pop-up blocked — fall back to a download.
          this.triggerDownload(blob, row.document?.fileName ?? row.documentType);
        }
        // Revoke after a short delay so the new tab has time to load it.
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
      },
      error: () => {
        this.toast.error('Failed to load document');
      },
    });
  }

  /** Downloads the stored document binary as a file on the user's device. */
  protected downloadFile(row: ChecklistRow): void {
    const enquiryId = this.enquiry()?.id;
    if (!enquiryId || !row.document?.hasFile) return;
    this.enquiryService.downloadDocumentFile(enquiryId, row.document.id).subscribe({
      next: (blob) => this.triggerDownload(blob, row.document?.fileName ?? row.documentType),
      error: () => {
        this.toast.error('Failed to download document');
      },
    });
  }

  private triggerDownload(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  /** Formats a byte count as a human-readable string (e.g. "1.4 MB"). */
  protected formatFileSize(bytes: number | null | undefined): string {
    if (bytes == null) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private updateRow(target: ChecklistRow, next: ChecklistRow): void {
    this.rows.update((rs) => rs.map((r) => (r.documentType === target.documentType ? next : r)));
  }

  /** In verify mode, VERIFIED documents are locked — no status/file changes allowed. */
  protected isRowLocked(row: ChecklistRow): boolean {
    return this.verifyMode() && row.status === 'VERIFIED';
  }

  // ── Rejection reason flow ─────────────────────────────────────────────────

  /** Opens the inline rejection-reason prompt for the given row. */
  protected startReject(row: ChecklistRow): void {
    if (row.saving || !this.canManageDocuments() || this.isRowLocked(row)) return;
    this.rejectingDocumentType.set(row.documentType);
    this.rejectReason.set(row.remarks ?? '');
  }

  /**
   * Validates that a reason has been entered, then persists the rejection.
   * The reason is saved as the row's remarks so it appears in the rejection banner.
   */
  protected confirmReject(row: ChecklistRow): void {
    const reason = this.rejectReason().trim();
    if (!reason) {
      this.toast.warning('Please provide a rejection reason before rejecting the document');
      return;
    }
    // Merge the reason into the row so saveRow picks it up as the remarks value.
    const rowWithReason: ChecklistRow = { ...row, remarks: reason };
    this.updateRow(row, rowWithReason);
    this.saveRow(rowWithReason, 'REJECTED');
    this.rejectingDocumentType.set(null);
    this.rejectReason.set('');
  }

  /** Dismisses the rejection-reason prompt without saving. */
  protected cancelReject(): void {
    this.rejectingDocumentType.set(null);
    this.rejectReason.set('');
  }

  protected submitDocuments(): void {
    const enquiryId = this.enquiry()?.id;
    if (!enquiryId || !this.canSubmit()) return;

    this.submitting.set(true);
    this.enquiryService.submitDocuments(enquiryId).subscribe({
      next: () => {
        this.toast.success('Documents submitted successfully');
        this.submitting.set(false);
        void this.router.navigate(['/enquiries/document-submission']);
      },
      error: (err) => {
        const missing = err?.error?.missingDocumentTypes as string[] | undefined;
        const message =
          missing && missing.length > 0
            ? `Missing documents: ${missing.map((m) => this.formatDocType(m)).join(', ')}`
            : (err?.error?.message ?? 'Failed to submit documents');
        this.toast.error(message);
        this.submitting.set(false);
      },
    });
  }

  protected backToList(): void {
    if (this.verifyMode()) {
      void this.router.navigate(['/enquiries/admission-completion']);
    } else {
      void this.router.navigate(['/enquiries/document-submission']);
    }
  }
}
