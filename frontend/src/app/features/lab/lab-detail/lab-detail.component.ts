import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LabService } from '../lab.service';
import { Lab, LabInChargeAssignment, LabInChargeAssignmentRequest, LabInChargeRole } from '../lab.model';
import { AuthService } from '../../../core/auth/auth.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-lab-detail',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    PageHeaderComponent],
  templateUrl: './lab-detail.component.html',
  styleUrl: './lab-detail.component.scss',
})
export class LabDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly labService = inject(LabService);
  private readonly toast = inject(ToastService);
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
    assignedDate: [new Date().toISOString().split('T')[0], Validators.required],
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
        this.toast.error('Error loading lab details');
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
      this.assignmentForm.reset({ assignedDate: new Date().toISOString().split('T')[0] });
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
      assignedDate: formValue.assignedDate,
    };

    this.labService.assignInCharge(this.lab()!.id, request).subscribe({
      next: () => {
        this.toast.success('Assignment added successfully');
        this.toggleAssignmentForm();
        this.loadAssignments(this.lab()!.id);
      },
      error: () => {
        this.toast.error('Error adding assignment');
      },
    });
  }

  protected removeAssignment(assignmentId: number): void {
    if (!this.lab()) {
      return;
    }

    this.labService.removeAssignment(this.lab()!.id, assignmentId).subscribe({
      next: () => {
        this.toast.success('Assignment removed');
        this.loadAssignments(this.lab()!.id);
      },
      error: () => {
        this.toast.error('Error removing assignment');
      },
    });
  }

  protected formatDisplayDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }
}
