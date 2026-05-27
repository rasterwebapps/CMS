import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LowerCasePipe } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { EnquiryService } from '../enquiry.service';
import { ProgramService } from '../../program/program.service';
import { Enquiry, EnquiryDocument } from '../enquiry.model';
import { DocumentTypeInfo } from '../../program/program.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';


interface VerificationRow {
  documentType: string;
  document: EnquiryDocument | null;
  status: string;
  isMandatory: boolean;
  saving: boolean;
}

@Component({
  selector: 'app-document-verification-detail',
  standalone: true,
  imports: [LowerCasePipe, CmsStatusBadgeComponent],
  templateUrl: './document-verification-detail.component.html',
  styleUrl: './document-verification-detail.component.scss',
})
export class DocumentVerificationDetailComponent implements OnInit {
  private readonly route             = inject(ActivatedRoute);
  private readonly router            = inject(Router);
  private readonly enquiryService    = inject(EnquiryService);
  private readonly programService    = inject(ProgramService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast             = inject(ToastService);

  protected readonly loading   = signal(true);
  protected readonly enquiry   = signal<Enquiry | null>(null);
  protected readonly rows      = signal<VerificationRow[]>([]);

  /** Which document type is showing the inline rejection comment input. */
  protected readonly rejectingDocumentType = signal<string | null>(null);
  protected readonly rejectComment         = signal<string>('');

  private readonly labelMap       = signal<Map<string, string>>(new Map());
  private readonly mandatoryTypes = signal<ReadonlySet<string>>(new Set());

  protected readonly mandatoryRows = computed(() => this.rows().filter(r => r.isMandatory));
  protected readonly optionalRows  = computed(() => this.rows().filter(r => !r.isMandatory));

  protected readonly mandatoryTotal    = computed(() => this.mandatoryTypes().size);
  protected readonly verifiedCount     = computed(() => this.rows().filter(r => r.isMandatory && r.status === 'VERIFIED').length);
  protected readonly verifiedPct       = computed(() => {
    const total = this.mandatoryTotal();
    return total === 0 ? 0 : Math.round((this.verifiedCount() / total) * 100);
  });
  protected readonly allVerified = computed(() =>
    this.verifiedCount() === this.mandatoryTotal()
  );

  protected formatDocType(type: string): string {
    return this.labelMap().get(type) ?? type
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, c => c.toUpperCase());
  }

  protected getDocumentIcon(type: string): string {
    if (type.includes('MARKSHEET') || type.includes('TRANSCRIPT') || type.includes('DEGREE')
        || type.includes('PROVISIONAL') || type.includes('ELIGIBILITY')) return 'school';
    if (type.includes('PHOTO')) return 'face';
    if (type.includes('SIGNATURE')) return 'draw';
    if (type.includes('AADHAR')) return 'badge';
    if (type.includes('MEDICAL')) return 'medical_services';
    if (type.includes('AFFIDAVIT')) return 'gavel';
    if (type.includes('TC') || type.includes('TRANSFER')) return 'swap_horiz';
    if (type.includes('MIGRATION')) return 'flight_takeoff';
    return 'description';
  }

  protected formatFileSize(bytes: number | null | undefined): string {
    if (bytes == null) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  protected canVerify(): boolean {
    return this.permissionService.has('DOCUMENT_VERIFICATION_MANAGE');
  }

  protected initials(name: string): string {
    return (name ?? '').split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase() || '?';
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id || Number.isNaN(id)) {
      this.toast.warning('Invalid enquiry id');
      void this.router.navigate(['/enquiries/document-verification']);
      return;
    }
    this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.enquiryService.getEnquiryById(id).subscribe({
      next: enquiry => {
        if (enquiry.status !== 'DOCUMENTS_SUBMITTED') {
          this.toast.warning('This enquiry is not in Documents Submitted status');
          void this.router.navigate(['/enquiries/document-verification']);
          return;
        }
        this.enquiry.set(enquiry);
        this.loadCatalogueAndDocuments(id, enquiry.programId);
      },
      error: () => {
        this.toast.error('Failed to load enquiry');
        void this.router.navigate(['/enquiries/document-verification']);
      },
    });
  }

  private loadCatalogueAndDocuments(enquiryId: number, programId: number | null | undefined): void {
    forkJoin({
      catalogue: this.programService.getAllDocumentTypes(),
      programTypes: programId
        ? this.programService.getRequiredDocumentTypes(programId).pipe(catchError(() => of<string[]>([])))
        : of<string[]>([]),
      documents: this.enquiryService.getDocuments(enquiryId).pipe(
        catchError(() => { this.toast.error('Failed to load documents'); return of<EnquiryDocument[]>([]); })
      ),
    }).subscribe({
      next: ({ catalogue, programTypes, documents }) => {
        this.labelMap.set(new Map(catalogue.map((t: DocumentTypeInfo) => [t.code, t.label])));
        const catalogueOrder = new Map(catalogue.map((t: DocumentTypeInfo, i: number) => [t.code, i]));
        const sorted = [...programTypes].sort(
          (a, b) => (catalogueOrder.get(a) ?? 999) - (catalogueOrder.get(b) ?? 999),
        );
        this.mandatoryTypes.set(new Set(sorted));
        this.rows.set(this.buildRows(documents));
        this.loading.set(false);

        // If the program has no required document types there is nothing for
        // the admin to verify individually, so verifyDocument() is never called
        // and autoTransitionIfAllVerified() on the backend never fires.
        // Explicitly trigger the transition here so the enquiry moves to
        // DOCUMENTS_VERIFIED and appears in the Complete Admission screen.
        if (sorted.length === 0) {
          this.triggerCompletion(enquiryId);
        }
      },
      error: () => {
        this.toast.error('Failed to load document catalogue');
        this.loading.set(false);
      },
    });
  }

  private buildRows(documents: EnquiryDocument[]): VerificationRow[] {
    const byType   = new Map(documents.map(d => [d.documentType, d]));
    const mandatory = this.mandatoryTypes();

    const required: VerificationRow[] = Array.from(mandatory).map(type => {
      const doc = byType.get(type) ?? null;
      return { documentType: type, document: doc, status: doc?.status ?? 'NOT_UPLOADED', isMandatory: true, saving: false };
    });

    const orphans: VerificationRow[] = documents
      .filter(d => !mandatory.has(d.documentType))
      .map(d => ({ documentType: d.documentType, document: d, status: d.status, isMandatory: false, saving: false }));

    return [...required, ...orphans];
  }

  private updateRow(type: string, next: Partial<VerificationRow>): void {
    this.rows.update(rs => rs.map(r => r.documentType === type ? { ...r, ...next } : r));
  }

  // ── View file ─────────────────────────────────────────────────────────────

  protected viewFile(row: VerificationRow): void {
    const enquiryId = this.enquiry()?.id;
    if (!enquiryId || !row.document?.hasFile) return;
    this.enquiryService.downloadDocumentFile(enquiryId, row.document.id).subscribe({
      next: blob => {
        const url    = URL.createObjectURL(blob);
        const opened = window.open(url, '_blank');
        if (!opened) {
          const a = document.createElement('a');
          a.href = url; a.download = row.document?.fileName ?? row.documentType;
          document.body.appendChild(a); a.click(); document.body.removeChild(a);
        }
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
      },
      error: () => this.toast.error('Failed to load document'),
    });
  }

  // ── Verify ────────────────────────────────────────────────────────────────

  protected verify(row: VerificationRow): void {
    const enquiryId = this.enquiry()?.id;
    if (!enquiryId || !row.document || row.saving || !this.canVerify()) return;

    this.updateRow(row.documentType, { saving: true });
    this.enquiryService.verifyDocument(enquiryId, row.document.id).subscribe({
      next: saved => {
        this.updateRow(row.documentType, { document: saved, status: saved.status, saving: false });
        this.toast.success(`${this.formatDocType(row.documentType)} verified`);
        if (this.allVerified()) {
          setTimeout(() => void this.router.navigate(['/enquiries/document-verification']), 1800);
        }
      },
      error: () => {
        this.updateRow(row.documentType, { saving: false });
        this.toast.error(`Failed to verify ${this.formatDocType(row.documentType)}`);
      },
    });
  }

  // ── Reject ────────────────────────────────────────────────────────────────

  protected startReject(row: VerificationRow): void {
    if (row.saving || !this.canVerify()) return;
    this.rejectingDocumentType.set(row.documentType);
    this.rejectComment.set('');
  }

  protected confirmReject(row: VerificationRow): void {
    const comment    = this.rejectComment().trim();
    const enquiryId  = this.enquiry()?.id;
    if (!comment) { this.toast.warning('Please enter a rejection comment'); return; }
    if (!enquiryId || !row.document) return;

    this.updateRow(row.documentType, { saving: true });
    this.rejectingDocumentType.set(null);
    this.rejectComment.set('');

    this.enquiryService.rejectDocument(enquiryId, row.document.id, comment).subscribe({
      next: saved => {
        this.updateRow(row.documentType, { document: saved, status: saved.status, saving: false });
        this.toast.warning(`${this.formatDocType(row.documentType)} rejected — student must re-upload`);
      },
      error: () => {
        this.updateRow(row.documentType, { saving: false });
        this.toast.error(`Failed to reject ${this.formatDocType(row.documentType)}`);
      },
    });
  }

  protected cancelReject(): void {
    this.rejectingDocumentType.set(null);
    this.rejectComment.set('');
  }

  protected backToList(): void {
    void this.router.navigate(['/enquiries/document-verification']);
  }

  /**
   * Called when the program has zero required document types.
   * Calls the backend to trigger the DOCUMENTS_SUBMITTED → DOCUMENTS_VERIFIED
   * transition (which normally fires inside verifyDocument but is never reached
   * when there is nothing to verify).
   */
  private triggerCompletion(enquiryId: number): void {
    this.enquiryService.completeDocumentVerification(enquiryId).subscribe({
      next: () => {
        this.toast.success('No required documents — enquiry marked as Documents Verified');
        setTimeout(() => void this.router.navigate(['/enquiries/document-verification']), 1800);
      },
      error: () => {
        // Non-fatal: log a warning so the admin knows to check manually
        this.toast.warning('Could not auto-complete verification — please contact support');
      },
    });
  }
}
