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
import { EnquiryService } from '../enquiry.service';
import { EnquiryRequest } from '../enquiry.model';
import { Agent } from '../../agent/agent.model';
import { AgentService } from '../../agent/agent.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-enquiry-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  templateUrl: './enquiry-form.component.html',
  styleUrl: './enquiry-form.component.scss',
})
export class EnquiryFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly agentService = inject(AgentService);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Enquiry');
  protected readonly programs = signal<{ id: number; name: string }[]>([]);
  protected readonly agents = signal<Agent[]>([]);
  protected readonly sources = ['WALK_IN', 'PHONE', 'ONLINE', 'AGENT_REFERRAL'];
  protected readonly statusOptions = ['NEW', 'CONTACTED', 'FEE_DISCUSSED', 'INTERESTED', 'NOT_INTERESTED', 'CLOSED'];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    email: [''],
    phone: [''],
    programId: [null],
    enquiryDate: ['', Validators.required],
    source: ['', Validators.required],
    status: ['NEW'],
    agentId: [null],
    assignedTo: [''],
    remarks: [''],
    feeDiscussedAmount: [null],
  });

  ngOnInit(): void {
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/programs`).subscribe({
      next: (data) => this.programs.set(data),
    });
    this.agentService.getActiveAgents().subscribe({
      next: (data) => this.agents.set(data),
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Enquiry');
      this.loading.set(true);
      this.enquiryService.getEnquiryById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            name: item.name, email: item.email, phone: item.phone, programId: item.programId,
            enquiryDate: item.enquiryDate, source: item.source, status: item.status,
            agentId: item.agentId, assignedTo: item.assignedTo, remarks: item.remarks,
            feeDiscussedAmount: item.feeDiscussedAmount,
          });
          this.loading.set(false);
        },
        error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); void this.router.navigate(['/enquiries']); },
      });
    }
  }

  protected isAgentReferral(): boolean {
    return this.form.get('source')?.value === 'AGENT_REFERRAL';
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: EnquiryRequest = {
      name: v.name.trim(), email: v.email || undefined, phone: v.phone || undefined,
      programId: v.programId || undefined, enquiryDate: v.enquiryDate, source: v.source,
      status: this.isEditMode() ? v.status : undefined, agentId: v.agentId || undefined,
      assignedTo: v.assignedTo || undefined, remarks: v.remarks || undefined,
      feeDiscussedAmount: v.feeDiscussedAmount || undefined,
    };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.enquiryService.updateEnquiry(this.itemId!, request) : this.enquiryService.createEnquiry(request);
    op$.subscribe({
      next: () => { this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 }); void this.router.navigate(['/enquiries']); },
      error: () => { this.snackBar.open('Failed to save', 'Close', { duration: 3000 }); this.saving.set(false); },
    });
  }
}
