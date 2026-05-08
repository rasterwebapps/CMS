import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { AdmissionService } from '../admission.service';
import {
  AcademicQualificationResponse,
  AdmissionDocumentResponse,
  AdmissionResponse,
} from '../admission.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsSkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ADMISSION_DETAIL_TOUR } from '../../../shared/tour/tours/admission.tours';

@Component({
  selector: 'app-admission-detail',
  standalone: true,
  imports: [
    AppDatePipe,
    RouterLink,
    FormsModule,
    MatTabsModule,
    MatIconModule,
    CmsSkeletonComponent,
    CmsStatusBadgeComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './admission-detail.component.html',
  styleUrl: './admission-detail.component.scss',
})
export class AdmissionDetailComponent implements OnInit {
  private static readonly TAB_INDEX_KEY = 'admission-detail-tab-index';

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly admissionService = inject(AdmissionService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  readonly loading = signal(true);
  readonly admission = signal<AdmissionResponse | null>(null);
  readonly qualifications = signal<AcademicQualificationResponse[]>([]);
  readonly documents = signal<AdmissionDocumentResponse[]>([]);
  readonly checklist = signal<Record<string, string>>({});

  readonly selectedTabIndex = signal(this.readSavedTabIndex());
  readonly expandedQuals = signal(new Set<number>());

  readonly initials = computed(() => computeInitials(this.admission()?.studentName));

  readonly verifiedDocsCount = computed(
    () => this.documents().filter((doc) => doc.verificationStatus === 'VERIFIED').length,
  );

  readonly verificationStatuses = ['UPLOADED', 'VERIFIED', 'REJECTED', 'NOT_UPLOADED'];

  ngOnInit(): void {
    this.tourService.register('admission-detail', ADMISSION_DETAIL_TOUR);
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAll(id);
  }

  private loadAll(id: number): void {
    this.admissionService.getById(id).subscribe({
      next: (a) => {
        this.admission.set(a);
        this.loadDocuments(id);
        this.loadQualifications(id);
      },
      error: () => {
        this.toast.error('Failed to load admission');
        this.loading.set(false);
      },
    });
  }

  private loadQualifications(id: number): void {
    this.admissionService.getQualifications(id).subscribe({
      next: (q) => this.qualifications.set(q),
      error: () => {},
    });
  }

  private loadDocuments(id: number): void {
    this.admissionService.getDocuments(id).subscribe({
      next: (docs) => {
        this.documents.set(docs);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.admissionService.getDocumentChecklist(id).subscribe({
      next: (cl) => this.checklist.set(cl),
      error: () => {},
    });
  }

  getChecklistEntries(): { type: string; status: string }[] {
    return Object.entries(this.checklist()).map(([type, status]) => ({ type, status }));
  }

  switchTab(index: number): void {
    this.selectedTabIndex.set(index);
    try {
      localStorage.setItem(AdmissionDetailComponent.TAB_INDEX_KEY, String(index));
    } catch {
      // Ignore storage failures (private browsing, quota, etc.).
    }
  }

  toggleQual(id: number): void {
    this.expandedQuals.update((set) => {
      const next = new Set(set);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  isQualExpanded(id: number): boolean {
    return this.expandedQuals().has(id);
  }

  yesNo(value: boolean | null): string {
    if (value == null) return '—';
    return value ? 'Yes' : 'No';
  }

  verifyDocument(doc: AdmissionDocumentResponse, newStatus: string): void {
    const verifiedBy = 'admin';
    this.admissionService.verifyDocument(doc.id, newStatus, verifiedBy).subscribe({
      next: (updated) => {
        this.documents.update((docs) => docs.map((d) => (d.id === updated.id ? updated : d)));
        this.toast.success('Document status updated');
        const admissionId = this.admission()?.id;
        if (admissionId) {
          this.admissionService.getDocumentChecklist(admissionId).subscribe({
            next: (cl) => this.checklist.set(cl),
            error: () => {},
          });
        }
      },
      error: () => this.toast.error('Failed to update document status'),
    });
  }

  uploadPlaceholder(): void {
    this.toast.info('File upload will be available soon');
  }

  edit(): void {
    const a = this.admission();
    if (a) void this.router.navigate(['/admissions', a.id, 'edit']);
  }

  private readSavedTabIndex(): number {
    try {
      const value = Number(localStorage.getItem(AdmissionDetailComponent.TAB_INDEX_KEY));
      return Number.isInteger(value) && value >= 0 && value <= 2 ? value : 0;
    } catch {
      return 0;
    }
  }
}
