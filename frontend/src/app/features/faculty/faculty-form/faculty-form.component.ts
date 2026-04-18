import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FacultyService } from '../faculty.service';
import {
  FacultyRequest,
  Designation,
  FacultyStatus,
  DESIGNATION_OPTIONS,
  FACULTY_STATUS_OPTIONS,
} from '../faculty.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';

@Component({
  selector: 'app-faculty-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './faculty-form.component.html',
  styleUrl: './faculty-form.component.scss',
})
export class FacultyFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly facultyService = inject(FacultyService);
  private readonly departmentService = inject(DepartmentService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Faculty');
  protected readonly departments = signal<Department[]>([]);
  protected readonly designationOptions = DESIGNATION_OPTIONS;
  protected readonly statusOptions = FACULTY_STATUS_OPTIONS;

  private facultyId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    employeeCode: ['', [Validators.required, Validators.maxLength(50)]],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    phone: ['', [Validators.maxLength(20)]],
    departmentId: [null as number | null, [Validators.required]],
    designation: [null as Designation | null, [Validators.required]],
    specialization: ['', [Validators.maxLength(255)]],
    labExpertise: ['', [Validators.maxLength(1000)]],
    joiningDate: ['', [Validators.required]],
    status: ['ACTIVE' as FacultyStatus],
  });

  ngOnInit(): void {
    this.loadDepartments();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.facultyId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Faculty');
      this.loadFaculty();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request: FacultyRequest = {
      employeeCode: (this.form.value.employeeCode ?? '').trim(),
      firstName: (this.form.value.firstName ?? '').trim(),
      lastName: (this.form.value.lastName ?? '').trim(),
      email: (this.form.value.email ?? '').trim(),
      phone: this.form.value.phone?.trim() || undefined,
      departmentId: this.form.value.departmentId,
      designation: this.form.value.designation,
      specialization: this.form.value.specialization?.trim() || undefined,
      labExpertise: this.form.value.labExpertise?.trim() || undefined,
      joiningDate: this.form.value.joiningDate,
      status: this.form.value.status || undefined,
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.facultyService.update(this.facultyId!, request)
      : this.facultyService.create(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Faculty updated successfully'
          : 'Faculty created successfully';
        this.snackBar.open(message, 'Close', { duration: 3000 });
        void this.router.navigate(['/faculty']);
      },
      error: () => {
        const message = this.isEditMode()
          ? 'Failed to update faculty'
          : 'Failed to create faculty';
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
    if (control.errors['email']) {
      return 'Please enter a valid email address';
    }
    if (control.errors['maxlength']) {
      const maxLength = control.errors['maxlength'].requiredLength;
      return `${this.getFieldLabel(fieldName)} must be at most ${maxLength} characters`;
    }

    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      employeeCode: 'Employee Code',
      firstName: 'First Name',
      lastName: 'Last Name',
      email: 'Email',
      phone: 'Phone',
      departmentId: 'Department',
      designation: 'Designation',
      specialization: 'Specialization',
      labExpertise: 'Lab Expertise',
      joiningDate: 'Joining Date',
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

  private loadFaculty(): void {
    if (!this.facultyId) return;

    this.loading.set(true);
    this.facultyService.getById(this.facultyId).subscribe({
      next: (faculty) => {
        this.form.patchValue({
          employeeCode: faculty.employeeCode,
          firstName: faculty.firstName,
          lastName: faculty.lastName,
          email: faculty.email,
          phone: faculty.phone || '',
          departmentId: faculty.departmentId,
          designation: faculty.designation,
          specialization: faculty.specialization || '',
          labExpertise: faculty.labExpertise || '',
          joiningDate: faculty.joiningDate,
          status: faculty.status,
        });
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load faculty', 'Close', { duration: 3000 });
        void this.router.navigate(['/faculty']);
      },
    });
  }
}
