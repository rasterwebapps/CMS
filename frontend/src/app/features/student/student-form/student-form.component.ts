import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments';
import { StudentService } from '../student.service';
import { StudentRequest } from '../student.model';

interface Program {
  id: number;
  name: string;
}

@Component({
  selector: 'app-student-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
  ],
  templateUrl: './student-form.component.html',
  styleUrl: './student-form.component.scss',
})
export class StudentFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly studentService = inject(StudentService);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Student');
  protected readonly programs = signal<Program[]>([]);

  protected readonly statusOptions = ['ACTIVE', 'INACTIVE', 'GRADUATED', 'DROPPED'];
  protected readonly genderOptions = ['MALE', 'FEMALE', 'OTHER'];
  protected readonly communityOptions = ['SC', 'ST', 'BC', 'MBC', 'DNC', 'OC', 'OTHERS'];
  protected readonly bloodGroupOptions = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

  private studentId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    rollNumber: ['', [Validators.required, Validators.maxLength(50)]],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    programId: [null, [Validators.required]],
    semester: [1, [Validators.required, Validators.min(1), Validators.max(12)]],
    admissionDate: ['', [Validators.required]],
    labBatch: [''],
    status: ['ACTIVE'],
    dateOfBirth: [''],
    gender: [''],
    nationality: [''],
    religion: [''],
    communityCategory: [''],
    caste: [''],
    bloodGroup: [''],
    fatherName: [''],
    motherName: [''],
    parentMobile: [''],
    postalAddress: [''],
    street: [''],
    city: [''],
    district: [''],
    state: [''],
    pincode: [''],
  });

  ngOnInit(): void {
    this.loadPrograms();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.studentId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Student');
      this.loadStudent();
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.value;
    const request: StudentRequest = {
      rollNumber: v.rollNumber.trim(),
      firstName: v.firstName.trim(),
      lastName: v.lastName.trim(),
      email: v.email.trim(),
      phone: v.phone?.trim() || undefined,
      programId: v.programId,
      semester: v.semester,
      admissionDate: v.admissionDate,
      labBatch: v.labBatch?.trim() || undefined,
      status: v.status || undefined,
      dateOfBirth: v.dateOfBirth || undefined,
      gender: v.gender || undefined,
      nationality: v.nationality?.trim() || undefined,
      religion: v.religion?.trim() || undefined,
      communityCategory: v.communityCategory || undefined,
      caste: v.caste?.trim() || undefined,
      bloodGroup: v.bloodGroup || undefined,
      fatherName: v.fatherName?.trim() || undefined,
      motherName: v.motherName?.trim() || undefined,
      parentMobile: v.parentMobile?.trim() || undefined,
      address: {
        postalAddress: v.postalAddress?.trim() || undefined,
        street: v.street?.trim() || undefined,
        city: v.city?.trim() || undefined,
        district: v.district?.trim() || undefined,
        state: v.state?.trim() || undefined,
        pincode: v.pincode?.trim() || undefined,
      },
    };

    this.saving.set(true);

    const operation$ = this.isEditMode()
      ? this.studentService.update(this.studentId!, request)
      : this.studentService.create(request);

    operation$.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Student updated successfully'
          : 'Student created successfully';
        this.snackBar.open(message, 'Close', { duration: 3000 });
        void this.router.navigate(['/students']);
      },
      error: () => {
        const message = this.isEditMode()
          ? 'Failed to update student'
          : 'Failed to create student';
        this.snackBar.open(message, 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }

  protected getErrorMessage(fieldName: string): string {
    const control = this.form.get(fieldName);
    if (!control?.errors) return '';
    if (control.errors['required']) return `${this.getFieldLabel(fieldName)} is required`;
    if (control.errors['email']) return 'Invalid email address';
    if (control.errors['min']) return `${this.getFieldLabel(fieldName)} is too small`;
    if (control.errors['max']) return `${this.getFieldLabel(fieldName)} is too large`;
    if (control.errors['maxlength']) {
      return `${this.getFieldLabel(fieldName)} must be at most ${control.errors['maxlength'].requiredLength} characters`;
    }
    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      rollNumber: 'Roll Number',
      firstName: 'First Name',
      lastName: 'Last Name',
      email: 'Email',
      programId: 'Program',
      semester: 'Semester',
      admissionDate: 'Admission Date',
    };
    return labels[fieldName] || fieldName;
  }

  private loadPrograms(): void {
    this.http.get<Program[]>(`${environment.apiUrl}/programs`).subscribe({
      next: (programs) => this.programs.set(programs),
      error: () => this.snackBar.open('Failed to load programs', 'Close', { duration: 3000 }),
    });
  }

  private loadStudent(): void {
    if (!this.studentId) return;
    this.loading.set(true);
    this.studentService.getById(this.studentId).subscribe({
      next: (student) => {
        this.form.patchValue({
          rollNumber: student.rollNumber,
          firstName: student.firstName,
          lastName: student.lastName,
          email: student.email,
          phone: student.phone || '',
          programId: student.programId,
          semester: student.semester,
          admissionDate: student.admissionDate,
          labBatch: student.labBatch || '',
          status: student.status,
          dateOfBirth: student.dateOfBirth || '',
          gender: student.gender || '',
          nationality: student.nationality || '',
          religion: student.religion || '',
          communityCategory: student.communityCategory || '',
          caste: student.caste || '',
          bloodGroup: student.bloodGroup || '',
          fatherName: student.fatherName || '',
          motherName: student.motherName || '',
          parentMobile: student.parentMobile || '',
          postalAddress: student.postalAddress || '',
          street: student.street || '',
          city: student.city || '',
          district: student.district || '',
          state: student.state || '',
          pincode: student.pincode || '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load student', 'Close', { duration: 3000 });
        void this.router.navigate(['/students']);
      },
    });
  }
}
