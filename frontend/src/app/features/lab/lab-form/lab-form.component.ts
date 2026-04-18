import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { LabService } from '../lab.service';
import { LabRequest, LAB_TYPES, LAB_STATUSES } from '../lab.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';

@Component({
  selector: 'app-lab-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './lab-form.component.html',
  styleUrl: './lab-form.component.scss',
})
export class LabFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly labService = inject(LabService);
  private readonly departmentService = inject(DepartmentService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Lab');
  protected readonly departments = signal<Department[]>([]);

  protected readonly labTypes = LAB_TYPES;
  protected readonly labStatuses = LAB_STATUSES;

  private labId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    labType: ['', [Validators.required]],
    departmentId: ['', [Validators.required]],
    building: ['', [Validators.maxLength(100)]],
    roomNumber: ['', [Validators.maxLength(50)]],
    capacity: [1, [Validators.required, Validators.min(1), Validators.max(500)]],
    status: ['ACTIVE', [Validators.required]],
  });

  ngOnInit(): void {
    this.loadDepartments();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.labId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Lab');
      this.loadLab();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request: LabRequest = {
      name: (this.form.value.name ?? '').trim(),
      labType: this.form.value.labType,
      departmentId: Number(this.form.value.departmentId),
      building: this.form.value.building?.trim() || undefined,
      roomNumber: this.form.value.roomNumber?.trim() || undefined,
      capacity: Number(this.form.value.capacity),
      status: this.form.value.status,
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.labService.update(this.labId!, request)
      : this.labService.create(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode() ? 'Lab updated successfully' : 'Lab created successfully';
        this.snackBar.open(message, 'Close', { duration: 3000 });
        void this.router.navigate(['/labs']);
      },
      error: () => {
        const message = this.isEditMode() ? 'Failed to update lab' : 'Failed to create lab';
        this.snackBar.open(message, 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }

  protected getErrorMessage(fieldName: string): string {
    const control = this.form.get(fieldName);
    if (!control || !control.errors) {
      return '';
    }

    if (control.errors['required']) {
      return `${this.getFieldLabel(fieldName)} is required`;
    }
    if (control.errors['maxlength']) {
      const maxLength = control.errors['maxlength'].requiredLength;
      return `${this.getFieldLabel(fieldName)} must be at most ${maxLength} characters`;
    }
    if (control.errors['min']) {
      return `${this.getFieldLabel(fieldName)} must be at least ${control.errors['min'].min}`;
    }
    if (control.errors['max']) {
      return `${this.getFieldLabel(fieldName)} must be at most ${control.errors['max'].max}`;
    }

    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      name: 'Name',
      labType: 'Lab Type',
      departmentId: 'Department',
      building: 'Building',
      roomNumber: 'Room Number',
      capacity: 'Capacity',
      status: 'Status',
    };
    return labels[fieldName] || fieldName;
  }

  private loadDepartments(): void {
    this.departmentService.getAll().subscribe({
      next: (departments) => {
        this.departments.set(departments);
      },
      error: () => {
        this.snackBar.open('Failed to load departments', 'Close', { duration: 3000 });
      },
    });
  }

  private loadLab(): void {
    if (!this.labId) return;

    this.loading.set(true);
    this.labService.getById(this.labId).subscribe({
      next: (lab) => {
        this.form.patchValue({
          name: lab.name,
          labType: lab.labType,
          departmentId: lab.department.id,
          building: lab.building || '',
          roomNumber: lab.roomNumber || '',
          capacity: lab.capacity,
          status: lab.status,
        });
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load lab', 'Close', { duration: 3000 });
        void this.router.navigate(['/labs']);
      },
    });
  }
}
