import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProgramService } from '../program.service';
import { AssessmentPattern, DocumentRequirementsRequest, DocumentTypeInfo, ProgramRequest, ProgramStatus } from '../program.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { PROGRAM_FORM_TOUR, PROGRAM_FORM_FLOW_MAP } from '../../../shared/tour/tours/program.tours';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

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
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Program');

  // ── Document requirements state ────────────────────────────────
  protected readonly allDocumentTypes = signal<DocumentTypeInfo[]>([]);
  protected readonly mandatoryDocumentTypes = signal<Set<string>>(new Set());
  protected readonly optionalDocumentTypes = signal<Set<string>>(new Set());
  protected readonly documentSearch = signal('');
  protected readonly savingDocuments = signal(false);
  protected readonly loadingDocuments = signal(false);
  protected readonly documentPanelOpen = signal(false);
  protected readonly documentPanelTop = signal(0);

  /** Items selected in the Available column for transfer. */
  protected readonly selectedAvailable = signal<Set<string>>(new Set());

  protected readonly mandatoryDocumentTypesArray = computed(() =>
    Array.from(this.mandatoryDocumentTypes())
  );
  protected readonly optionalDocumentTypesArray = computed(() =>
    Array.from(this.optionalDocumentTypes())
  );

  /** Catalogue items not yet assigned to mandatory or optional. */
  protected readonly availableDocumentTypes = computed(() => {
    const mandatory = this.mandatoryDocumentTypes();
    const optional  = this.optionalDocumentTypes();
    return this.allDocumentTypes().filter(t => !mandatory.has(t.code) && !optional.has(t.code));
  });

  /** Available items filtered by search and grouped by category. */
  protected readonly filteredGroupedAvailable = computed(() => {
    const term = this.documentSearch().trim().toLowerCase();
    const filtered = this.availableDocumentTypes().filter(t =>
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

  /** Mandatory items sorted by label for display in the assigned panel. */
  protected readonly mandatoryLabeled = computed(() =>
    this.mandatoryDocumentTypesArray()
      .map(code => ({ code, label: this.getDocumentLabel(code) }))
      .sort((a, b) => a.label.localeCompare(b.label))
  );

  /** Optional items sorted by label for display in the assigned panel. */
  protected readonly optionalLabeled = computed(() =>
    this.optionalDocumentTypesArray()
      .map(code => ({ code, label: this.getDocumentLabel(code) }))
      .sort((a, b) => a.label.localeCompare(b.label))
  );

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
    { icon: 'cake',        title: 'Age Restriction',    subtitle: 'Government-mandated minimum age as of the cutoff date. Enforced at enquiry and admission.' },
  ];

  protected readonly MONTH_OPTIONS = [
    { value: 1, label: 'January' }, { value: 2, label: 'February' }, { value: 3, label: 'March' },
    { value: 4, label: 'April' },   { value: 5, label: 'May' },       { value: 6, label: 'June' },
    { value: 7, label: 'July' },    { value: 8, label: 'August' },    { value: 9, label: 'September' },
    { value: 10, label: 'October' },{ value: 11, label: 'November' }, { value: 12, label: 'December' },
  ];

  private programId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100), trimmedMinLength(2), noConsecutiveSpaces()]],
    code: ['', [Validators.required, Validators.maxLength(20), noInternalSpaces()]],
    durationYears: [null as number | null, [Validators.required, Validators.min(1), Validators.max(10)]],
    status: ['ACTIVE' as ProgramStatus, Validators.required],
    assessmentPattern: ['TERM_BASED' as AssessmentPattern, Validators.required],
    minimumAgeYears: [17, [Validators.required, Validators.min(1), Validators.max(100)]],
    ageCutoffDay: [31, [Validators.required, Validators.min(1), Validators.max(31)]],
    ageCutoffMonth: [12, Validators.required],
    usesClinicalShiftScheduling: [false],
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
    this.destroyRef.onDestroy(() => {
      this.setScrollLock(false);
    });
  }

  ngOnInit(): void {
    this.tourService.register('program-form', PROGRAM_FORM_TOUR);
    this.tourService.registerFlowMap('program-form', PROGRAM_FORM_FLOW_MAP);
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.programId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Program');
      this.loadProgram();
      // Documents loaded on-demand when panel opens
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/programs/name-exists`, () => this.programId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/programs/code-exists`, () => this.programId)
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
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
      minimumAgeYears: this.form.value.minimumAgeYears,
      ageCutoffDay: this.form.value.ageCutoffDay,
      ageCutoffMonth: this.form.value.ageCutoffMonth,
      usesClinicalShiftScheduling: this.form.value.usesClinicalShiftScheduling ?? false,
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

    // Create mode: create the program first, then persist document requirements (if any)
    const docReq: DocumentRequirementsRequest = {
      mandatory: Array.from(this.mandatoryDocumentTypes()),
      optional: Array.from(this.optionalDocumentTypes()),
    };
    const hasDocReq = docReq.mandatory.length > 0 || docReq.optional.length > 0;

    this.programService.create(request).subscribe({
      next: (created) => {
        if (!hasDocReq) {
          this.saving.set(false);
          this.toast.success('Program created successfully');
          void this.router.navigate(['/programs']);
          return;
        }

        this.savingDocuments.set(true);
        this.programService.setDocumentRequirements(created.id, docReq).subscribe({
          next: (saved) => {
            this.mandatoryDocumentTypes.set(new Set(saved.mandatory));
            this.optionalDocumentTypes.set(new Set(saved.optional));
            this.savingDocuments.set(false);
            this.saving.set(false);
            this.toast.success('Program created successfully');
            void this.router.navigate(['/programs']);
          },
          error: (err) => {
            this.savingDocuments.set(false);
            this.saving.set(false);
            this.toast.error(
              err?.error?.message
                ?? 'Program created, but failed to save document requirements. Please retry in Edit.'
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
    minimumAgeYears: 'Minimum Age',
    ageCutoffDay: 'Cutoff Day',
    ageCutoffMonth: 'Cutoff Month',
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
          minimumAgeYears: program.minimumAgeYears ?? 17,
          ageCutoffDay: program.ageCutoffDay ?? 31,
          ageCutoffMonth: program.ageCutoffMonth ?? 12,
          usesClinicalShiftScheduling: program.usesClinicalShiftScheduling ?? false,
        });
        this.mandatoryDocumentTypes.set(new Set(program.mandatoryDocumentTypes ?? []));
        this.optionalDocumentTypes.set(new Set(program.optionalDocumentTypes ?? []));
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load program');
        void this.router.navigate(['/programs']);
      },
    });
  }

  protected toggleAvailableSelection(code: string): void {
    const sel = new Set(this.selectedAvailable());
    sel.has(code) ? sel.delete(code) : sel.add(code);
    this.selectedAvailable.set(sel);
  }

  protected isAvailableSelected(code: string): boolean {
    return this.selectedAvailable().has(code);
  }

  protected moveToMandatory(): void {
    const sel = this.selectedAvailable();
    if (sel.size === 0) return;
    const mandatory = new Set(this.mandatoryDocumentTypes());
    for (const code of sel) mandatory.add(code);
    this.mandatoryDocumentTypes.set(mandatory);
    this.selectedAvailable.set(new Set());
  }

  protected moveToOptional(): void {
    const sel = this.selectedAvailable();
    if (sel.size === 0) return;
    const optional = new Set(this.optionalDocumentTypes());
    for (const code of sel) optional.add(code);
    this.optionalDocumentTypes.set(optional);
    this.selectedAvailable.set(new Set());
  }

  protected removeFromMandatory(code: string): void {
    const mandatory = new Set(this.mandatoryDocumentTypes());
    mandatory.delete(code);
    this.mandatoryDocumentTypes.set(mandatory);
  }

  protected removeFromOptional(code: string): void {
    const optional = new Set(this.optionalDocumentTypes());
    optional.delete(code);
    this.optionalDocumentTypes.set(optional);
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
    this.selectedAvailable.set(new Set());
    this.documentSearch.set('');
    this.updateDocumentPanelTop();
    this.setScrollLock(true);
    this.documentPanelOpen.set(true);
  }

  protected closeDocumentPanel(): void {
    this.selectedAvailable.set(new Set());
    this.setScrollLock(false);
    this.documentPanelOpen.set(false);
    this.documentPanelTop.set(0);
  }

  private updateDocumentPanelTop(): void {
    if (window.matchMedia('(max-width: 768px)').matches) {
      this.documentPanelTop.set(0);
      return;
    }
    const scroller = document.querySelector<HTMLElement>('main.app-content');
    this.documentPanelTop.set(scroller?.scrollTop ?? 0);
  }

  private setScrollLock(lock: boolean): void {
    const el = document.querySelector<HTMLElement>('main.app-content');
    if (el) el.style.overflow = lock ? 'hidden' : '';
  }

  protected saveRequiredDocuments(): void {
    if (!this.isEditMode()) {
      this.toast.success('Document requirements saved');
      this.closeDocumentPanel();
      return;
    }

    if (!this.programId) return;

    this.savingDocuments.set(true);
    const req: DocumentRequirementsRequest = {
      mandatory: Array.from(this.mandatoryDocumentTypes()),
      optional: Array.from(this.optionalDocumentTypes()),
    };

    this.programService.setDocumentRequirements(this.programId, req).subscribe({
      next: (saved) => {
        this.mandatoryDocumentTypes.set(new Set(saved.mandatory));
        this.optionalDocumentTypes.set(new Set(saved.optional));
        this.savingDocuments.set(false);
        this.toast.success('Document requirements updated');
        this.closeDocumentPanel();
      },
      error: (err) => {
        this.savingDocuments.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to save document requirements');
      },
    });
  }
}
