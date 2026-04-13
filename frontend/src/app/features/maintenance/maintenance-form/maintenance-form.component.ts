import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MaintenanceService } from '../maintenance.service';
import { MaintenanceRequestDto } from '../maintenance.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-maintenance-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  templateUrl: './maintenance-form.component.html',
  styleUrl: './maintenance-form.component.scss',
})
export class MaintenanceFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Maintenance Request');
  protected readonly equipment = signal<{ id: number; name: string }[]>([]);

  protected readonly priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  protected readonly statuses = ['REQUESTED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

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
        error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); void this.router.navigate(['/maintenance']); },
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
      next: () => { this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 }); void this.router.navigate(['/maintenance']); },
      error: () => { this.snackBar.open('Failed to save', 'Close', { duration: 3000 }); this.saving.set(false); },
    });
  }
}
