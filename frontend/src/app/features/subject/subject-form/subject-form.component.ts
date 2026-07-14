import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SubjectService } from '../subject.service';
import { SubjectRequest } from '../subject.model';
import { SpecialityService } from '../../speciality/speciality.service';
import { Speciality } from '../../speciality/speciality.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { noConsecutiveSpaces, noInternalSpaces, trimmedMinLength, cmsFieldError, stripSpaces } from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-subject-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
  ],
  templateUrl: './subject-form.component.html',
  styleUrl: './subject-form.component.scss',
})
export class SubjectFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly subjectService = inject(SubjectService);
  private readonly specialityService = inject(SpecialityService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Subject');
  protected readonly specialities = signal<Speciality[]>([]);

  // Live preview signals
  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');
  protected readonly previewTermNumber = signal<number | null>(null);
  protected readonly previewCredits = signal<number | null>(null);

  protected readonly TIPS: CmsTip[] = [
    { icon: 'tag',     title: 'Subject Code',   subtitle: 'Use a short uppercase identifier unique across all subjects.' },
    { icon: 'share',   title: 'Shared Subject', subtitle: 'A subject is not tied to one course — map it into any curriculum via Curriculum Map, including across multiple programs.' },
    { icon: 'business', title: 'Speciality',    subtitle: 'Optional — which speciality teaches this subject, if applicable.' },
    { icon: 'event',   title: 'Term Number',    subtitle: 'The term this subject is typically taught in, independent of any specific curriculum version.' },
  ];

  private subjectId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255), trimmedMinLength(2), noConsecutiveSpaces()]],
    code: ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    credits: [null as number | null, [Validators.required, Validators.min(1), Validators.max(20)]],
    theoryCredits: [null as number | null, [Validators.required, Validators.min(0), Validators.max(20)]],
    labCredits: [null as number | null, [Validators.required, Validators.min(0), Validators.max(20)]],
    specialityId: [null as number | null],
    termNumber: [null as number | null, [Validators.required, Validators.min(1), Validators.max(12)]],
    isActive: [true],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewCode.set(stripSpaces(v.code ?? '').toUpperCase());
        this.previewTermNumber.set(v.termNumber ?? null);
        this.previewCredits.set(v.credits ?? null);
      });
  }

  protected onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    if (upper !== input.value) {
      this.form.get('code')?.setValue(upper, { emitEvent: true });
    }
  }

  ngOnInit(): void {
    this.loadSpecialities();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.subjectId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Subject');
      this.loadSubject();
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/subjects/name-exists`, () => this.subjectId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/subjects/code-exists`, () => this.subjectId)
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }

    const request: SubjectRequest = {
      name: (this.form.value.name ?? '').trim(),
      code: (this.form.value.code ?? '').trim(),
      credits: this.form.value.credits,
      theoryCredits: this.form.value.theoryCredits,
      labCredits: this.form.value.labCredits,
      specialityId: this.form.value.specialityId ?? null,
      termNumber: this.form.value.termNumber,
      isActive: this.form.value.isActive,
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.subjectService.update(this.subjectId!, request)
      : this.subjectService.create(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Subject updated successfully'
          : 'Subject created successfully';
        this.toast.success(message);
        void this.router.navigate(['/subjects']);
      },
      error: (err) => {
        const message = err?.error?.message
          ?? (this.isEditMode() ? 'Failed to update subject' : 'Failed to create subject');
        this.toast.error(message);
        this.saving.set(false);
      },
    });
  }

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Subject Name',
    code: 'Code',
    credits: 'Credits',
    theoryCredits: 'Theory Credits',
    labCredits: 'Lab Credits',
    termNumber: 'Term Number',
  };

  protected getErrorMessage(fieldName: string): string {
    return cmsFieldError(this.form.get(fieldName), SubjectFormComponent.FIELD_LABELS[fieldName] ?? fieldName);
  }

  private loadSpecialities(): void {
    this.specialityService.getAll().subscribe({
      next: (specialities) => {
        this.specialities.set(specialities);
      },
      error: () => {
        this.toast.error('Failed to load specialities');
      },
    });
  }

  private loadSubject(): void {
    if (!this.subjectId) return;

    this.loading.set(true);
    this.subjectService.getById(this.subjectId).subscribe({
      next: (subject) => {
        this.form.patchValue({
          name: subject.name,
          code: subject.code,
          credits: subject.credits,
          theoryCredits: subject.theoryCredits,
          labCredits: subject.labCredits,
          specialityId: subject.speciality?.id ?? null,
          termNumber: subject.termNumber,
          isActive: subject.isActive,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load subject');
        void this.router.navigate(['/subjects']);
      },
    });
  }
}
