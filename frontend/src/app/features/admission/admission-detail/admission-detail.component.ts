import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { FormsModule } from '@angular/forms';
import { AdmissionService } from '../admission.service';
import {
  AdmissionResponse,
  AcademicQualificationResponse,
  AdmissionDocumentResponse,
} from '../admission.model';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-admission-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatChipsModule],
  templateUrl: './admission-detail.component.html',
  styleUrl: './admission-detail.component.scss',
})
export class AdmissionDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly admissionService = inject(AdmissionService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly admission = signal<AdmissionResponse | null>(null);
  protected readonly qualifications = signal<AcademicQualificationResponse[]>([]);
  protected readonly documents = signal<AdmissionDocumentResponse[]>([]);
  protected readonly checklist = signal<Record<string, string>>({});

  protected readonly qualColumns = ['type', 'school', 'subject', 'marks', 'percentage', 'passing', 'board'];
  protected readonly docColumns = ['type', 'status', 'verifiedBy', 'actions'];

  protected readonly verificationStatuses = ['UPLOADED', 'VERIFIED', 'REJECTED', 'NOT_UPLOADED'];

  ngOnInit(): void {
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

  protected getChecklistEntries(): { type: string; status: string }[] {
    return Object.entries(this.checklist()).map(([type, status]) => ({ type, status }));
  }

  protected verifyDocument(doc: AdmissionDocumentResponse, newStatus: string): void {
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

  protected uploadPlaceholder(): void {
    this.toast.info('File upload will be available soon');
  }

  protected edit(): void {
    const a = this.admission();
    if (a) void this.router.navigate(['/admissions', a.id, 'edit']);
  }

  protected back(): void {
    void this.router.navigate(['/admissions']);
  }
}
