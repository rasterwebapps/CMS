import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { LabScheduleService } from '../lab-schedule.service';
import { DAYS_OF_WEEK, LabScheduleRequest, LabSlot } from '../lab-schedule.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-lab-schedule-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatCheckboxModule, MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './lab-schedule-form.component.html',
  styleUrl: './lab-schedule-form.component.scss',
})
export class LabScheduleFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly labScheduleService = inject(LabScheduleService);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Lab Schedule');
  protected readonly labs = signal<{ id: number; name: string }[]>([]);
  protected readonly courses = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly faculty = signal<{ id: number; name: string }[]>([]);
  protected readonly labSlots = signal<LabSlot[]>([]);
  protected readonly semesters = signal<{ id: number; name: string }[]>([]);
  protected readonly daysOfWeek = DAYS_OF_WEEK;

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    labId: [null, Validators.required],
    courseId: [null, Validators.required],
    facultyId: [null, Validators.required],
    labSlotId: [null, Validators.required],
    batchName: ['', [Validators.required, Validators.maxLength(100)]],
    dayOfWeek: ['', Validators.required],
    semesterId: [null, Validators.required],
    isActive: [true],
  });

  ngOnInit(): void {
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/labs`).subscribe({
      next: (data) => this.labs.set(data),
      error: () => { this.snackBar.open('Failed to load labs', 'Close', { duration: 3000 }); },
    });
    this.http.get<{ id: number; name: string; code: string }[]>(`${environment.apiUrl}/courses`).subscribe({
      next: (data) => this.courses.set(data),
      error: () => { this.snackBar.open('Failed to load courses', 'Close', { duration: 3000 }); },
    });
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/faculty`).subscribe({
      next: (data) => this.faculty.set(data),
      error: () => { this.snackBar.open('Failed to load faculty', 'Close', { duration: 3000 }); },
    });
    this.labScheduleService.getAllSlots().subscribe({
      next: (data) => this.labSlots.set(data),
      error: () => { this.snackBar.open('Failed to load lab slots', 'Close', { duration: 3000 }); },
    });
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/semesters`).subscribe({
      next: (data) => this.semesters.set(data),
      error: () => { this.snackBar.open('Failed to load semesters', 'Close', { duration: 3000 }); },
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Lab Schedule');
      this.loading.set(true);
      this.labScheduleService.getById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({ labId: item.labId, courseId: item.courseId, facultyId: item.facultyId, labSlotId: item.labSlotId, batchName: item.batchName, dayOfWeek: item.dayOfWeek, semesterId: item.semesterId, isActive: item.isActive });
          this.loading.set(false);
        },
        error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); void this.router.navigate(['/lab-schedules']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: LabScheduleRequest = { labId: v.labId, courseId: v.courseId, facultyId: v.facultyId, labSlotId: v.labSlotId, batchName: v.batchName.trim(), dayOfWeek: v.dayOfWeek, semesterId: v.semesterId, isActive: v.isActive };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.labScheduleService.update(this.itemId!, request) : this.labScheduleService.create(request);
    op$.subscribe({
      next: () => { this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 }); void this.router.navigate(['/lab-schedules']); },
      error: () => { this.snackBar.open('Failed to save', 'Close', { duration: 3000 }); this.saving.set(false); },
    });
  }
}
