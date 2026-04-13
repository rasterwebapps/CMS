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
import { FinanceService } from '../finance.service';
import { FeeStructureRequest } from '../finance.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-fee-structure-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  templateUrl: './fee-structure-form.component.html',
  styleUrl: './fee-structure-form.component.scss',
})
export class FeeStructureFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly financeService = inject(FinanceService);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Fee Structure');
  protected readonly programs = signal<{ id: number; name: string }[]>([]);

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    programId: [null, Validators.required],
    semester: [null, [Validators.required, Validators.min(1), Validators.max(8)]],
    amount: [null, [Validators.required, Validators.min(0)]],
    labFeeComponent: [null],
    dueDate: [''],
  });

  ngOnInit(): void {
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/programs`).subscribe({
      next: (data) => this.programs.set(data),
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Fee Structure');
      this.loading.set(true);
      this.financeService.getFeeStructureById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({ name: item.name, programId: item.programId, semester: item.semester, amount: item.amount, labFeeComponent: item.labFeeComponent, dueDate: item.dueDate || '' });
          this.loading.set(false);
        },
        error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); void this.router.navigate(['/fee-structures']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: FeeStructureRequest = { name: v.name.trim(), programId: v.programId, semester: v.semester, amount: v.amount, labFeeComponent: v.labFeeComponent || undefined, dueDate: v.dueDate || undefined };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.financeService.updateFeeStructure(this.itemId!, request) : this.financeService.createFeeStructure(request);
    op$.subscribe({
      next: () => { this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 }); void this.router.navigate(['/fee-structures']); },
      error: () => { this.snackBar.open('Failed to save', 'Close', { duration: 3000 }); this.saving.set(false); },
    });
  }
}
