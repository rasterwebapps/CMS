import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProgramService } from '../program.service';
import { AssessmentPattern, DocumentTypeInfo, ProgramRequest, ProgramStatus } from '../program.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { PROGRAM_FORM_TOUR } from '../../../shared/tour/tours/program.tours';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';

@Component({
  selector: 'app-program-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CmsTourButtonComponent,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
  ],
  templateUrl: './program-form.component.html',
  styleUrl: './program-form.component.scss',
})
export class ProgramFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly programService = inject(ProgramService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Program');

  // ── Required documents state ────────────────────────────────
  protected readonly allDocumentTypes = signal<DocumentTypeInfo[]>([]);
  protected readonly selectedDocumentTypes = signal<Set<string>>(new Set());
  protected readonly documentSearch = signal('');
  protected readonly savingDocuments = signal(false);
  protected readonly loadingDocuments = signal(false);
  protected readonly documentPanelOpen = signal(false);

  protected readonly selectedDocumentTypesArray = computed(() =>
    Array.from(this.selectedDocumentTypes())
  );

  protected readonly groupedDocumentTypes = computed(() => {
    const term = this.documentSearch().trim().toLowerCase();
    const filtered = this.allDocumentTypes().filter(t =>
      !term || t.label.toLowerCase().includes(term) || t.code.toLowerCase().includes(term)
    );
    const groups = new Map<string, DocumentTypeInfo[]>();
    for (const t of filtered) {
      if (!groups.has(t.category)) groups.set(t.category, []);
      groups.get(t.category)!.push(t);
    }
    return Array.from(groups.entries())
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([category, items]) => ({ category, items }));
  });

  // ── Live preview signals ────────────────────────────────────
  protected readonly previewName              = signal('');
  protected readonly previewCode              = signal('');
  protected readonly previewDuration          = signal<number>(0);
  protected readonly previewStatus            = signal<ProgramStatus>('ACTIVE');
  protected readonly previewAssessmentPattern = signal<AssessmentPattern>('TERM_BASED');

  protected readonly TIPS: CmsTip[] = [
    { icon: 'tag',         title: 'Unique Code',       subtitle: 'Use 3–6 uppercase letters as an identifier (e.g., BAC, MAS).' },
    { icon: 'event',       title: 'Duration',           subtitle: 'Number of years a student takes to complete the program.' },
    { icon: 'school',      title: 'Assessment Pattern', subtitle: 'Term-based: installments per term. Yearly: one annual exam at end of year.' },
    { icon: 'toggle_on',   title: 'Status',             subtitle: 'Inactive programs are hidden from new admissions but kept for historical records.' },
  ];

  private programId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100), trimmedMinLength(2), noConsecutiveSpaces()]],
    code: ['', [Validators.required, Validators.maxLength(20), noInternalSpaces()]],
    durationYears: [null as number | null, [Validators.required, Validators.min(1), Validators.max(10)]],
    status: ['ACTIVE' as ProgramStatus, Validators.required],
    assessmentPattern: ['TERM_BASED' as AssessmentPattern, Validators.required],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewCode.set(stripSpaces(v.code ?? '').toUpperCase());
        this.previewDuration.set(Number(v.durationYears) || 0);
        this.previewStatus.set((v.status ?? 'ACTIVE') as ProgramStatus);
        this.previewAssessmentPattern.set((v.assessmentPattern ?? 'TERM_BASED') as AssessmentPattern);
      });
  }

  ngOnInit(): void {
    this.tourService.register('program-form', PROGRAM_FORM_TOUR);
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.programId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Program');
      this.loadProgram();
      // Documents loaded on-demand when panel opens
    }
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    if (upper !== input.value) {
      this.form.get('code')?.setValue(upper, { emitEvent: true });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: ProgramRequest = {
      name: (this.form.value.name ?? '').trim(),
      code: (this.form.value.code ?? '').trim(),
      durationYears: this.form.value.durationYears,
      status: this.form.value.status as ProgramStatus,
      assessmentPattern: this.form.value.assessmentPattern as AssessmentPattern,
    };

    this.saving.set(true);

    if (this.isEditMode()) {
      this.programService.update(this.programId!, request).subscribe({
        next: () => {
          this.saving.set(false);
          this.toast.success('Program updated successfully');
          void this.router.navigate(['/programs']);
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'Failed to update program');
          this.saving.set(false);
        },
      });
      return;
    }

    // Create mode: create the program first, then persist required documents (if any)
    const selectedTypes = Array.from(this.selectedDocumentTypes());

    this.programService.create(request).subscribe({
      next: (created) => {
        if (selectedTypes.length === 0) {
          this.saving.set(false);
          this.toast.success('Program created successfully');
          void this.router.navigate(['/programs']);
          return;
        }

        this.savingDocuments.set(true);
        this.programService.setRequiredDocumentTypes(created.id, selectedTypes).subscribe({
          next: (saved) => {
            this.selectedDocumentTypes.set(new Set(saved));
            this.savingDocuments.set(false);
            this.saving.set(false);
            this.toast.success('Program created successfully');
            void this.router.navigate(['/programs']);
          },
          error: (err) => {
            this.savingDocuments.set(false);
            this.saving.set(false);
            // Avoid duplicate program creation attempts: send the user to Edit screen to retry.
            this.toast.error(
              err?.error?.message
                ?? 'Program created, but failed to save required documents. Please retry in Edit.'
            );
            void this.router.navigate([`/programs/${created.id}/edit`]);
          },
        });
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to create program');
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Program Name',
    code: 'Code',
    durationYears: 'Duration',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), ProgramFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadProgram(): void {
    if (!this.programId) return;

    this.loading.set(true);
    this.programService.getById(this.programId).subscribe({
      next: (program) => {
        this.form.patchValue({
          name: program.name,
          code: program.code,
          durationYears: program.durationYears,
          status: program.status,
          assessmentPattern: program.assessmentPattern ?? 'TERM_BASED',
        });
        this.selectedDocumentTypes.set(new Set(program.requiredDocumentTypes ?? []));
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load program');
        void this.router.navigate(['/programs']);
      },
    });
  }

  protected toggleDocumentType(code: string, checked: boolean): void {
    const next = new Set(this.selectedDocumentTypes());
    if (checked) {
      next.add(code);
    } else {
      next.delete(code);
    }
    this.selectedDocumentTypes.set(next);
  }

  protected isDocumentSelected(code: string): boolean {
    return this.selectedDocumentTypes().has(code);
  }

  protected getDocumentLabel(code: string): string {
    const doc = this.allDocumentTypes().find(d => d.code === code);
    return doc ? doc.label : code;
  }

  protected onDocumentSearchInput(event: Event): void {
    this.documentSearch.set((event.target as HTMLInputElement).value);
  }

  protected openDocumentPanel(): void {
    if (!this.loadingDocuments() && this.allDocumentTypes().length === 0) {
      this.loadingDocuments.set(true);
      this.programService.getAllDocumentTypes().subscribe({
        next: (catalogue) => {
          this.allDocumentTypes.set(catalogue);
          this.loadingDocuments.set(false);
        },
        error: () => {
          this.loadingDocuments.set(false);
          this.toast.error('Failed to load document types');
        },
      });
    }
    this.documentPanelOpen.set(true);
  }

  protected closeDocumentPanel(): void {
    this.documentPanelOpen.set(false);
  }

  protected saveRequiredDocuments(): void {
    // For create mode, just close the panel - documents will be saved after program creation
    if (!this.isEditMode()) {
      this.toast.success('Document selection saved');
      this.closeDocumentPanel();
      return;
    }

    // For edit mode, save immediately
    if (!this.programId) return;

    this.savingDocuments.set(true);
    const types = Array.from(this.selectedDocumentTypes());

    this.programService.setRequiredDocumentTypes(this.programId, types).subscribe({
      next: (saved) => {
        this.selectedDocumentTypes.set(new Set(saved));
        this.savingDocuments.set(false);
        this.toast.success('Required documents updated');
        this.closeDocumentPanel();
      },
      error: (err) => {
        this.savingDocuments.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to save required documents');
      },
    });
  }
}
