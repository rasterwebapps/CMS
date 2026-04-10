import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LabService } from '../lab.service';
import { Lab, LabInChargeAssignment, LabInChargeAssignmentRequest, LabInChargeRole } from '../lab.model';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-lab-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './lab-detail.component.html',
  styleUrl: './lab-detail.component.scss',
})
export class LabDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly labService = inject(LabService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  protected readonly authService = inject(AuthService);

  protected readonly lab = signal<Lab | null>(null);
  protected readonly assignments = signal<LabInChargeAssignment[]>([]);
  protected readonly loading = signal(true);
  protected readonly showAssignmentForm = signal(false);

  protected readonly displayedColumns = ['assigneeName', 'role', 'assignedDate', 'actions'];
  protected readonly roles: LabInChargeRole[] = ['LAB_INCHARGE', 'TECHNICIAN'];

  protected readonly assignmentForm: FormGroup = this.fb.group({
    assigneeId: [null, [Validators.required, Validators.min(1)]],
    assigneeName: ['', [Validators.required, Validators.minLength(2)]],
    role: ['', Validators.required],
    assignedDate: [new Date(), Validators.required],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadLab(+id);
    }
  }

  private loadLab(id: number): void {
    this.loading.set(true);
    this.labService.getById(id).subscribe({
      next: (lab) => {
        this.lab.set(lab);
        this.loadAssignments(id);
      },
      error: () => {
        this.snackBar.open('Error loading lab details', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  private loadAssignments(labId: number): void {
    this.labService.getAssignments(labId).subscribe({
      next: (assignments) => {
        this.assignments.set(assignments);
        this.loading.set(false);
      },
      error: () => {
        this.assignments.set([]);
        this.loading.set(false);
      },
    });
  }

  protected getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'primary';
      case 'INACTIVE':
        return 'warn';
      case 'UNDER_MAINTENANCE':
        return 'accent';
      default:
        return '';
    }
  }

  protected getRoleLabel(role: string): string {
    switch (role) {
      case 'LAB_INCHARGE':
        return 'Lab In-Charge';
      case 'TECHNICIAN':
        return 'Technician';
      default:
        return role;
    }
  }

  protected toggleAssignmentForm(): void {
    this.showAssignmentForm.update((v) => !v);
    if (!this.showAssignmentForm()) {
      this.assignmentForm.reset({ assignedDate: new Date() });
    }
  }

  protected submitAssignment(): void {
    if (this.assignmentForm.invalid || !this.lab()) {
      return;
    }

    const formValue = this.assignmentForm.value;
    const request: LabInChargeAssignmentRequest = {
      assigneeId: formValue.assigneeId,
      assigneeName: formValue.assigneeName,
      role: formValue.role,
      assignedDate: this.formatDate(formValue.assignedDate),
    };

    this.labService.assignInCharge(this.lab()!.id, request).subscribe({
      next: () => {
        this.snackBar.open('Assignment added successfully', 'Close', { duration: 3000 });
        this.toggleAssignmentForm();
        this.loadAssignments(this.lab()!.id);
      },
      error: () => {
        this.snackBar.open('Error adding assignment', 'Close', { duration: 3000 });
      },
    });
  }

  protected removeAssignment(assignmentId: number): void {
    if (!this.lab()) {
      return;
    }

    this.labService.removeAssignment(this.lab()!.id, assignmentId).subscribe({
      next: () => {
        this.snackBar.open('Assignment removed', 'Close', { duration: 3000 });
        this.loadAssignments(this.lab()!.id);
      },
      error: () => {
        this.snackBar.open('Error removing assignment', 'Close', { duration: 3000 });
      },
    });
  }

  protected formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  protected formatDisplayDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }
}
