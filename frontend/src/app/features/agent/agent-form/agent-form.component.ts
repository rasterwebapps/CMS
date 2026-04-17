import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AgentService } from '../agent.service';
import { AgentRequest } from '../agent.model';

@Component({
  selector: 'app-agent-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatSlideToggleModule,
  ],
  templateUrl: './agent-form.component.html',
  styleUrl: './agent-form.component.scss',
})
export class AgentFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly agentService = inject(AgentService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Agent');

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    phone: [''],
    email: [''],
    area: [''],
    locality: [''],
    allottedSeats: [null as number | null],
    isActive: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Agent');
      this.loading.set(true);
      this.agentService.getAgentById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({ name: item.name, phone: item.phone, email: item.email, area: item.area, locality: item.locality, allottedSeats: item.allottedSeats, isActive: item.isActive });
          this.loading.set(false);
        },
        error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); void this.router.navigate(['/agents']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: AgentRequest = { name: v.name.trim(), phone: v.phone || undefined, email: v.email || undefined, area: v.area || undefined, locality: v.locality || undefined, allottedSeats: v.allottedSeats ?? undefined, isActive: v.isActive };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.agentService.updateAgent(this.itemId!, request) : this.agentService.createAgent(request);
    op$.subscribe({
      next: () => { this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 }); void this.router.navigate(['/agents']); },
      error: () => { this.snackBar.open('Failed to save', 'Close', { duration: 3000 }); this.saving.set(false); },
    });
  }
}
