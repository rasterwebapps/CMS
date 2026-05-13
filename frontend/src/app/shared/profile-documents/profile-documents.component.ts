import {
  Component,
  computed,
  inject,
  Input,
  OnChanges,
  signal,
  SimpleChanges,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { FacultyService } from '../../features/faculty/faculty.service';
import { FACULTY_DOCUMENT_SLOTS } from '../../features/faculty/faculty.model';
import { AdmissionService } from '../../features/admission/admission.service';
import { ProgramService } from '../../features/program/program.service';
import { DocumentTypeInfo } from '../../features/program/program.model';
import { ToastService } from '../../core/toast/toast.service';
import { CmsStatusBadgeComponent } from '../status-badge/status-badge.component';
import { CmsEmptyStateComponent } from '../empty-state/empty-state.component';

export type ProfileEntityType = 'FACULTY' | 'STUDENT';

interface DocumentSlot {
  documentType: string;
  label: string;
  status: string;
  documentId?: number;
  hasFile: boolean;
  verifiedBy?: string;
  verifiedAt?: string;
  remarks?: string;
  isMandatory: boolean;
  saving: boolean;
}

@Component({
  selector: 'cms-profile-documents',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    CmsStatusBadgeComponent,
    CmsEmptyStateComponent,
  ],
  templateUrl: './profile-documents.component.html',
  styleUrl: './profile-documents.component.scss',
})
export class ProfileDocumentsComponent implements OnChanges {
  @Input({ required: true }) entityType!: ProfileEntityType;
  @Input({ required: true }) entityId!: number;
  @Input() programId?: number;
  @Input() canManage = false;
  @Input() canVerify = false;

  private readonly facultyService = inject(FacultyService);
  private readonly admissionService = inject(AdmissionService);
  private readonly programService = inject(ProgramService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(false);
  protected readonly slots = signal<DocumentSlot[]>([]);

  private readonly catalogue = signal<DocumentTypeInfo[]>([]);
  private labelMap = new Map<string, string>();

  protected readonly verifiedCount = computed(
    () => this.slots().filter((s) => s.status === 'VERIFIED').length,
  );
  protected readonly totalCount = computed(() => this.slots().length);
  protected readonly progressPct = computed(() =>
    this.totalCount() === 0 ? 0 : Math.round((this.verifiedCount() / this.totalCount()) * 100),
  );

  protected rejectingType: string | null = null;
  protected rejectReason = '';

  private fileInputs = new Map<string, HTMLInputElement>();

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['entityId'] || changes['entityType']) && this.entityId) {
      this.load();
    }
  }

  private load(): void {
    this.loading.set(true);

    if (this.entityType === 'FACULTY') {
      this.loadFaculty();
    } else {
      this.loadStudent();
    }
  }

  private loadFaculty(): void {
    forkJoin({
      catalogue: this.programService.getAllDocumentTypes().pipe(catchError(() => of<DocumentTypeInfo[]>([]))),
      requiredTypes: this.facultyService.getRequiredDocumentTypesForFaculty(this.entityId).pipe(
        catchError(() => of<string[]>([])),
      ),
      documents: this.facultyService.getDocuments(this.entityId).pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ catalogue, requiredTypes, documents }) => {
        this.catalogue.set(catalogue);
        this.labelMap = new Map(catalogue.map((t) => [t.code, t.label]));

        const required = new Set(requiredTypes);
        const byType = new Map(documents.map((d) => [d.documentType, d]));

        const slots: DocumentSlot[] = Array.from(required).map((type) => {
          const doc = byType.get(type);
          return {
            documentType: type,
            label: this.labelFor(type),
            status: doc?.status ?? 'NOT_UPLOADED',
            documentId: doc?.id,
            hasFile: doc != null && doc.fileName != null,
            verifiedBy: doc?.verifiedBy ?? undefined,
            verifiedAt: doc?.verifiedAt ?? undefined,
            remarks: doc?.remarks ?? undefined,
            isMandatory: true,
            saving: false,
          };
        });

        // Extra uploaded docs not in required set
        documents
          .filter((d) => !required.has(d.documentType))
          .forEach((d) => {
            slots.push({
              documentType: d.documentType,
              label: this.labelFor(d.documentType),
              status: d.status,
              documentId: d.id,
              hasFile: d.fileName != null,
              verifiedBy: d.verifiedBy ?? undefined,
              verifiedAt: d.verifiedAt ?? undefined,
              remarks: d.remarks ?? undefined,
              isMandatory: false,
              saving: false,
            });
          });

        this.slots.set(slots);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load documents');
        this.loading.set(false);
      },
    });
  }

  private loadStudent(): void {
    forkJoin({
      catalogue: this.programService.getAllDocumentTypes().pipe(catchError(() => of<DocumentTypeInfo[]>([]))),
      checklist: this.admissionService.getDocumentChecklist(this.entityId).pipe(
        catchError(() => of<Record<string, string>>({})),
      ),
      documents: this.admissionService.getDocuments(this.entityId).pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ catalogue, checklist, documents }) => {
        this.catalogue.set(catalogue);
        this.labelMap = new Map(catalogue.map((t) => [t.code, t.label]));

        const required = new Set(Object.keys(checklist));
        const byType = new Map(documents.map((d) => [d.documentType, d]));

        const slots: DocumentSlot[] = Array.from(required).map((type) => {
          const doc = byType.get(type);
          return {
            documentType: type,
            label: this.labelFor(type),
            status: doc?.verificationStatus ?? checklist[type] ?? 'NOT_UPLOADED',
            documentId: doc?.id,
            hasFile: doc?.hasFile ?? false,
            verifiedBy: doc?.verifiedBy ?? undefined,
            verifiedAt: doc?.verifiedAt ?? undefined,
            remarks: doc?.remarks ?? undefined,
            isMandatory: true,
            saving: false,
          };
        });

        documents
          .filter((d) => !required.has(d.documentType))
          .forEach((d) => {
            slots.push({
              documentType: d.documentType,
              label: this.labelFor(d.documentType),
              status: d.verificationStatus,
              documentId: d.id,
              hasFile: d.hasFile,
              verifiedBy: d.verifiedBy ?? undefined,
              verifiedAt: d.verifiedAt ?? undefined,
              remarks: d.remarks ?? undefined,
              isMandatory: false,
              saving: false,
            });
          });

        this.slots.set(slots);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load documents');
        this.loading.set(false);
      },
    });
  }

  protected triggerUpload(slot: DocumentSlot): void {
    if (!this.canManage || slot.status === 'VERIFIED' || slot.saving) return;
    let input = this.fileInputs.get(slot.documentType);
    if (!input) {
      input = document.createElement('input');
      input.type = 'file';
      input.accept = '.pdf,.jpg,.jpeg,.png,.gif,.bmp,.tiff,.doc,.docx';
      input.addEventListener('change', (e) => {
        const file = (e.target as HTMLInputElement).files?.[0];
        if (file) this.doUpload(slot, file);
        input!.value = '';
      });
      this.fileInputs.set(slot.documentType, input);
    }
    input.click();
  }

  private doUpload(slot: DocumentSlot, file: File): void {
    const MAX = 10 * 1024 * 1024;
    if (file.size > MAX) {
      this.toast.error(`${file.name} exceeds the 10 MB limit`);
      return;
    }
    this.updateSlot(slot.documentType, { saving: true });

    const upload$ = this.entityType === 'FACULTY'
      ? this.facultyService.uploadDocument(this.entityId, slot.documentType, file)
      : this.admissionService.uploadDocument(this.entityId, slot.documentType, file);

    upload$.subscribe({
      next: (saved) => {
        this.updateSlot(slot.documentType, {
          status: this.entityType === 'FACULTY'
            ? (saved as { status: string }).status
            : (saved as { verificationStatus: string }).verificationStatus,
          documentId: saved.id,
          hasFile: true,
          remarks: undefined,
          saving: false,
        });
        this.toast.success(`${slot.label} uploaded`);
      },
      error: () => {
        this.updateSlot(slot.documentType, { saving: false });
        this.toast.error(`Failed to upload ${slot.label}`);
      },
    });
  }

  protected download(slot: DocumentSlot): void {
    if (!slot.documentId) return;
    const dl$ = this.entityType === 'FACULTY'
      ? this.facultyService.downloadDocumentBlob(this.entityId, slot.documentId)
      : this.admissionService.downloadDocumentBlob(slot.documentId);

    dl$.subscribe({
      next: (response) => {
        const blob = response.body!;
        const contentDisposition = response.headers.get('Content-Disposition') ?? '';
        const match = contentDisposition.match(/filename\*?=(?:UTF-8'')?["']?([^;"'\n]+)/i);
        const fileName = match ? decodeURIComponent(match[1]) : slot.label;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.toast.error(`Failed to download ${slot.label}`),
    });
  }

  protected verify(slot: DocumentSlot): void {
    if (!this.canVerify || !slot.documentId || slot.status === 'VERIFIED') return;
    this.updateSlot(slot.documentType, { saving: true });
    this.verifyUpdate(slot, 'VERIFIED', undefined);
  }

  protected startReject(slot: DocumentSlot): void {
    this.rejectingType = slot.documentType;
    this.rejectReason = '';
  }

  protected confirmReject(slot: DocumentSlot): void {
    if (!this.rejectReason.trim()) return;
    this.updateSlot(slot.documentType, { saving: true });
    this.rejectingType = null;
    this.verifyUpdate(slot, 'REJECTED', this.rejectReason.trim());
    this.rejectReason = '';
  }

  protected cancelReject(): void {
    this.rejectingType = null;
    this.rejectReason = '';
  }

  private verifyUpdate(slot: DocumentSlot, status: string, remarks: string | undefined): void {
    if (!slot.documentId) return;

    if (this.entityType === 'FACULTY') {
      this.facultyService
        .updateDocumentStatus(this.entityId, slot.documentId, slot.documentType, status, remarks)
        .subscribe({
          next: (saved) => {
            this.updateSlot(slot.documentType, { status: saved.status, remarks: saved.remarks, saving: false });
            this.toast.success(status === 'VERIFIED' ? `${slot.label} verified` : `${slot.label} rejected`);
          },
          error: () => {
            this.updateSlot(slot.documentType, { saving: false });
            this.toast.error('Failed to update verification status');
          },
        });
    } else {
      this.admissionService.verifyDocument(slot.documentId, status, '').subscribe({
        next: (saved) => {
          this.updateSlot(slot.documentType, {
            status: saved.verificationStatus,
            verifiedBy: saved.verifiedBy ?? undefined,
            verifiedAt: saved.verifiedAt ?? undefined,
            saving: false,
          });
          this.toast.success(status === 'VERIFIED' ? `${slot.label} verified` : `${slot.label} rejected`);
        },
        error: () => {
          this.updateSlot(slot.documentType, { saving: false });
          this.toast.error('Failed to update verification status');
        },
      });
    }
  }

  protected statusColor(status: string): string {
    switch (status) {
      case 'VERIFIED': return 'success';
      case 'UPLOADED': return 'info';
      case 'REJECTED': return 'danger';
      case 'RETURNED': return 'warning';
      default: return 'default';
    }
  }

  protected statusLabel(status: string): string {
    switch (status) {
      case 'NOT_UPLOADED': return 'Not Uploaded';
      case 'UPLOADED': return 'Uploaded';
      case 'VERIFIED': return 'Verified';
      case 'REJECTED': return 'Rejected';
      case 'RETURNED': return 'Returned';
      default: return status;
    }
  }

  protected docIcon(type: string): string {
    if (type.includes('PHOTO') || type.includes('SIGNATURE')) return 'image';
    if (type.includes('DEGREE') || type.includes('MARKSHEET') || type.includes('CERTIFICATE')) return 'school';
    if (type.includes('AADHAR') || type.includes('PAN') || type.includes('PASSPORT')) return 'badge';
    if (type.includes('APPOINTMENT') || type.includes('JOINING') || type.includes('PROMOTION')) return 'work';
    if (type.includes('EXPERIENCE')) return 'history_edu';
    return 'description';
  }

  private labelFor(type: string): string {
    if (this.labelMap.has(type)) return this.labelMap.get(type)!;
    const slot = FACULTY_DOCUMENT_SLOTS.find((s) => s.type === type);
    if (slot) return slot.label;
    return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  }

  private updateSlot(documentType: string, patch: Partial<DocumentSlot>): void {
    this.slots.update((list) =>
      list.map((s) => (s.documentType === documentType ? { ...s, ...patch } : s)),
    );
  }
}
