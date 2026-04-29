import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MaintenanceService } from '../maintenance.service';
import { MaintenanceRequestDto } from '../maintenance.model';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';
import { InrPipe } from '../../../shared/pipes/inr.pipe';

@Component({
  selector: 'app-maintenance-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, CmsTourButtonComponent,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    CmsPreviewCardComponent, CmsTipsCardComponent, InrPipe],
  templateUrl: './maintenance-form.component.html',
  styleUrl: './maintenance-form.component.scss',
})
export class MaintenanceFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Maintenance Request');
  protected readonly equipment = signal<{ id: number; name: string }[]>([]);

  protected readonly priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  protected readonly statuses = ['REQUESTED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  // Preview signals
  protected readonly previewEquipmentId = signal<number | null>(null);
  protected readonly previewRequestedBy = signal('');
  protected readonly previewDescription = signal('');
  protected readonly previewPriority = signal('');
  protected readonly previewStatus = signal('REQUESTED');
  protected readonly previewTechnician = signal('');
  protected readonly previewCost = signal<number | null>(null);
  protected readonly previewCompletedDate = signal('');
  protected readonly previewEquipmentName = computed(() => {
    const e = this.equipment().find(x => x.id === this.previewEquipmentId());
    return e ? e.name : '';
  });

  protected readonly TIPS: CmsTip[] = [
    { icon: 'precision_manufacturing', title: 'Equipment',  subtitle: 'Pick the affected unit so technicians know exactly what to inspect.' },
    { icon: 'priority_high',           title: 'Priority',   subtitle: 'CRITICAL halts a lab — use sparingly. Most issues are MEDIUM.' },
    { icon: 'description',             title: 'Description',subtitle: 'Include symptoms, error codes & when the issue started.' },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    equipmentId: [null, Validators.required],
    requestedBy: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.required, Validators.maxLength(1000)]],
    priority: ['', Validators.required],
    status: ['REQUESTED'],
    assignedTechnician: ['', Validators.maxLength(255)],
    repairCost: [null, Validators.min(0)],
    completedDate: [''],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewEquipmentId.set(v.equipmentId ?? null);
        this.previewRequestedBy.set((v.requestedBy ?? '').trim());
        this.previewDescription.set((v.description ?? '').trim());
        this.previewPriority.set(v.priority || '');
        this.previewStatus.set(v.status || 'REQUESTED');
        this.previewTechnician.set((v.assignedTechnician ?? '').trim());
        this.previewCost.set(v.repairCost != null && v.repairCost !== '' ? Number(v.repairCost) : null);
        this.previewCompletedDate.set(v.completedDate || '');
      });
  }

  ngOnInit(): void {
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/equipment`).subscribe({
      next: (data) => this.equipment.set(data),
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Maintenance Request');
      this.loading.set(true);
      this.maintenanceService.getById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            equipmentId: item.equipmentId,
            requestedBy: item.requestedBy,
            description: item.description,
            priority: item.priority,
            status: item.status,
            assignedTechnician: item.assignedTechnician || '',
            repairCost: item.repairCost,
            completedDate: item.completedDate || '',
          });
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load'); void this.router.navigate(['/maintenance']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: MaintenanceRequestDto = {
      equipmentId: v.equipmentId,
      requestedBy: v.requestedBy.trim(),
      description: v.description.trim(),
      priority: v.priority,
      status: v.status || undefined,
      assignedTechnician: v.assignedTechnician?.trim() || undefined,
      repairCost: v.repairCost ?? undefined,
      completedDate: v.completedDate || undefined,
    };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.maintenanceService.update(this.itemId!, request) : this.maintenanceService.create(request);
    op$.subscribe({
      next: () => { this.toast.success(this.isEditMode() ? 'Updated' : 'Created'); void this.router.navigate(['/maintenance']); },
      error: () => { this.toast.error('Failed to save'); this.saving.set(false); },
    });
  }
}
