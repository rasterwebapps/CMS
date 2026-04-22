import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AdmissionService } from '../admission.service';
import { ADMISSION_STATUSES, QUALIFICATION_TYPES } from '../admission.model';
import { StudentService } from '../../student/student.service';
import { Student } from '../../student/student.model';
import { LayoutService } from '../../../core/layout/layout.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-admission-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    PageHeaderComponent,
  ],
  templateUrl: './admission-form.component.html',
  styleUrl: './admission-form.component.scss',
})
export class AdmissionFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly admissionService = inject(AdmissionService);
  private readonly studentService = inject(StudentService);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly layoutService = inject(LayoutService);

  protected readonly students = signal<Student[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEdit = signal(false);
  protected readonly statuses = ADMISSION_STATUSES;
  protected readonly qualificationTypes = QUALIFICATION_TYPES;

  protected readonly qualColumns = ['qualificationType', 'schoolName', 'percentage', 'monthAndYearOfPassing', 'actions'];

  protected readonly form: FormGroup = this.fb.group({
    studentId: [null, Validators.required],
    academicYearFrom: [new Date().getFullYear(), Validators.required],
    academicYearTo: [new Date().getFullYear() + 1, Validators.required],
    applicationDate: [new Date().toISOString().split('T')[0], Validators.required],
    status: ['SUBMITTED'],
    declarationPlace: [''],
    declarationDate: [''],
    parentConsentGiven: [false],
    applicantConsentGiven: [false],
    qualifications: this.fb.array([]),
  });

  get qualifications(): FormArray {
    return this.form.get('qualifications') as FormArray;
  }

  ngOnInit(): void {
    this.studentService.getAll().subscribe({ next: (s) => this.students.set(s) });
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.isEdit.set(true);
      this.loading.set(true);
      this.admissionService.getById(id).subscribe({
        next: (a) => {
          this.form.patchValue(a);
          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Failed to load admission', 'Close', { duration: 3000 });
          void this.router.navigate(['/admissions']);
        },
      });
    }
  }

  protected addQualification(): void {
    this.qualifications.push(this.fb.group({
      qualificationType: ['', Validators.required],
      schoolName: [''],
      majorSubject: [''],
      totalMarks: [null],
      percentage: [null],
      monthAndYearOfPassing: [''],
      universityOrBoard: [''],
    }));
  }

  protected removeQualification(i: number): void {
    this.qualifications.removeAt(i);
  }

  protected getQualGroup(i: number): FormGroup {
    return this.qualifications.at(i) as FormGroup;
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const { qualifications, ...admissionData } = this.form.value;
    const save$ = this.isEdit()
      ? this.admissionService.update(id, admissionData)
      : this.admissionService.create(admissionData);

    save$.subscribe({
      next: (admission) => {
        const quals: unknown[] = qualifications ?? [];
        if (!this.isEdit() && quals.length > 0) {
          let remaining = quals.length;
          quals.forEach((q) => {
            this.admissionService.addQualification(admission.id, q as Parameters<AdmissionService['addQualification']>[1]).subscribe({
              next: () => { if (--remaining === 0) this.finish(); },
              error: () => { if (--remaining === 0) this.finish(); },
            });
          });
        } else {
          this.finish();
        }
      },
      error: () => {
        this.snackBar.open('Failed to save admission', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
    });
  }

  private finish(): void {
    this.snackBar.open('Admission saved successfully', 'Close', { duration: 3000 });
    void this.router.navigate(['/admissions']);
  }
}
