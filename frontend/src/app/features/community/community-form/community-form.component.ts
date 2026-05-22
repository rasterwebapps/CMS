import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { CommunityService } from '../community.service';
import { CommunityRequest } from '../community.model';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import {
  noConsecutiveSpaces,
  noInternalSpaces,
  trimmedMinLength,
  cmsFieldError,
  stripSpaces,
} from '../../../shared/validators/cms-validators';
import { environment } from '../../../../environments';
import { uniqueFieldValidator } from '../../../shared/validators/unique-field.validator';

@Component({
  selector: 'app-community-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    CmsPreviewCardComponent,
    CmsTipsCardComponent,
  ],
  templateUrl: './community-form.component.html',
  styleUrl: './community-form.component.scss',
})
export class CommunityFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly communityService = inject(CommunityService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);

  protected readonly previewName = signal('');
  protected readonly previewCode = signal('');
  protected readonly previewDesc = signal('');
  protected readonly previewActive = signal(true);

  protected readonly TIPS: CmsTip[] = [
    {
      icon: 'groups',
      title: 'Community Name',
      subtitle: 'Use the official readable category name used in student records.',
    },
    {
      icon: 'tag',
      title: 'Short Code',
      subtitle: 'Codes are auto-uppercase and should match admission/reporting conventions.',
    },
    {
      icon: 'visibility',
      title: 'Active Status',
      subtitle: 'Inactive communities stay in history but are hidden from new student forms.',
    },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: [
      '',
      [Validators.required, trimmedMinLength(2), Validators.maxLength(100), noConsecutiveSpaces()],
    ],
    code: ['', [Validators.required, Validators.maxLength(50), noInternalSpaces()]],
    description: ['', Validators.maxLength(255)],
    isActive: [true],
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v) => {
      const cleaned = stripSpaces(v.code ?? '').toUpperCase();
      if (cleaned !== (v.code ?? '')) {
        this.form.get('code')!.setValue(cleaned, { emitEvent: false });
      }

      this.previewName.set((v.name ?? '').trim());
      this.previewCode.set(cleaned);
      this.previewDesc.set((v.description ?? '').trim());
      this.previewActive.set(!!v.isActive);
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.loadItem(this.itemId);
    }
    this.setupUniquenessValidators();
  }

  private setupUniquenessValidators(): void {
    const nameCtrl = this.form.get('name');
    if (nameCtrl) {
      nameCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/communities/name-exists`, () => this.itemId)
      );
      nameCtrl.updateValueAndValidity({ emitEvent: false });
    }
    const codeCtrl = this.form.get('code');
    if (codeCtrl) {
      codeCtrl.setAsyncValidators(
        uniqueFieldValidator(this.http, `${environment.apiUrl}/communities/code-exists`, () => this.itemId)
      );
      codeCtrl.updateValueAndValidity({ emitEvent: false });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    this.saving.set(true);
    const request = this.buildRequest();
    const op = this.isEditMode()
      ? this.communityService.updateCommunity(this.itemId!, request)
      : this.communityService.createCommunity(request);
    op.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Community updated' : 'Community created');
        void this.router.navigate(['/communities']);
      },
      error: (err) => {
        const msg =
          err?.error?.message ??
          (this.isEditMode() ? 'Failed to update community' : 'Failed to create community');
        this.toast.error(msg);
        this.saving.set(false);
      },
    });
  }

  private loadItem(id: number): void {
    this.loading.set(true);
    this.communityService.getCommunityById(id).subscribe({
      next: (item) => {
        this.form.patchValue({
          name: item.name,
          code: item.code,
          description: item.description ?? '',
          isActive: item.isActive,
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load community');
        this.loading.set(false);
      },
    });
  }

  protected getFieldError(field: string): string {
    const labels: Record<string, string> = {
      name: 'Name',
      code: 'Code',
      description: 'Description',
    };
    return cmsFieldError(this.form.get(field), labels[field] ?? field);
  }

  private buildRequest(): CommunityRequest {
    const v = this.form.value as CommunityRequest & { isActive: boolean };
    const description = v.description?.trim();
    return {
      name: v.name.trim(),
      code: v.code.trim(),
      description: description || undefined,
      isActive: v.isActive,
    };
  }
}
