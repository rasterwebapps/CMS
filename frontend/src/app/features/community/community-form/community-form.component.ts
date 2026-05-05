import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { CommunityService } from '../community.service';
import { CommunityRequest } from '../community.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

@Component({
  selector: 'app-community-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, MatSlideToggleModule],
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

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Community');

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    code: ['', [Validators.required, Validators.maxLength(50)]],
    description: ['', Validators.maxLength(255)],
    isActive: [true],
  });

  constructor() {
    // auto-uppercase the code field
    this.form.get('code')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: string) => {
        const upper = (v ?? '').toUpperCase().replace(/\s+/g, '_');
        if (upper !== v) {
          this.form.get('code')!.setValue(upper, { emitEvent: false });
        }
      });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Community');
      this.loadItem(this.itemId);
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }
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
        const msg = err?.error?.message ?? (this.isEditMode() ? 'Failed to update community' : 'Failed to create community');
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

  private buildRequest(): CommunityRequest {
    const v = this.form.value as CommunityRequest & { isActive: boolean };
    return {
      name: v.name,
      code: v.code,
      description: v.description || undefined,
      isActive: v.isActive,
    };
  }
}

