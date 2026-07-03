import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ToastService } from '../../../core/toast/toast.service';
import { NumberSeriesDefinition, SCOPE_TYPE_OPTIONS } from './number-series-definition.model';
import { NumberSeriesDefinitionService } from './number-series-definition.service';

@Component({
  selector: 'app-number-series-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './number-series-form.component.html',
  styleUrl: './number-series-form.component.scss',
})
export class NumberSeriesFormComponent implements OnInit {
  private readonly route  = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb     = inject(FormBuilder);
  private readonly svc    = inject(NumberSeriesDefinitionService);
  private readonly toast  = inject(ToastService);

  protected readonly scopeTypeOptions = SCOPE_TYPE_OPTIONS;
  protected readonly isEdit    = signal(false);
  protected readonly loading   = signal(false);
  protected readonly saving    = signal(false);
  protected readonly preview   = signal<string | null>(null);
  protected readonly scopeLocked = signal(false);
  private editId: number | null = null;

  protected readonly form = this.fb.group({
    seriesCode:      ['', [Validators.required, Validators.pattern('^[A-Z0-9_]+$'), Validators.maxLength(50)]],
    seriesName:      ['', [Validators.required, Validators.maxLength(100)]],
    scopeType:       ['CALENDAR_YEAR', Validators.required],
    prefix:          [''],
    separator:       ['-'],
    sequencePadding: [4, [Validators.required, Validators.min(1), Validators.max(10)]],
    description:     [''],
  });

  get previewFormatHint(): string {
    const f = this.form.value;
    const prefix   = f.prefix || '(none)';
    const sep      = f.separator ?? '';
    const padding  = f.sequencePadding ?? 4;
    const seqStr   = '0'.repeat(padding);
    const scope    = f.scopeType === 'NONE' ? '' : '{scope}';
    const parts    = [f.prefix ? prefix : null, scope || null, seqStr].filter(Boolean);
    return parts.join(sep) || seqStr;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit.set(true);
      this.editId = Number(id);
      this.loadForEdit(this.editId);
    }
  }

  private loadForEdit(id: number): void {
    this.loading.set(true);
    this.svc.getById(id).subscribe({
      next: (item: NumberSeriesDefinition) => {
        this.scopeLocked.set(!item.canEditScopeType);
        this.form.patchValue({
          seriesCode:      item.seriesCode,
          seriesName:      item.seriesName,
          scopeType:       item.scopeType,
          prefix:          item.prefix ?? '',
          separator:       item.separator,
          sequencePadding: item.sequencePadding,
          description:     item.description ?? '',
        });
        this.form.get('seriesCode')!.disable();
        if (!item.canEditScopeType) this.form.get('scopeType')!.disable();
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load series'); this.loading.set(false); },
    });
  }

  protected save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    const payload = {
      seriesCode:      raw.seriesCode!,
      seriesName:      raw.seriesName!,
      scopeType:       raw.scopeType!,
      prefix:          raw.prefix || null,
      separator:       raw.separator ?? '-',
      sequencePadding: raw.sequencePadding!,
      description:     raw.description || null,
    };
    this.saving.set(true);
    const req$ = this.editId
      ? this.svc.update(this.editId, payload)
      : this.svc.create(payload);
    req$.subscribe({
      next: () => {
        this.toast.success(this.editId ? 'Series updated' : 'Series created');
        void this.router.navigate(['/number-sequences']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Save failed');
        this.saving.set(false);
      },
    });
  }
}
