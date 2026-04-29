import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AgentService } from '../agent.service';
import { AgentRequest } from '../agent.model';
import { ReferralTypeService } from '../../referral-type/referral-type.service';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { AGENT_FORM_TOUR } from '../../../shared/tour/tours/agent.tours';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';

@Component({
  selector: 'app-agent-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSlideToggleModule, CmsTourButtonComponent,
    CmsPreviewCardComponent, CmsTipsCardComponent,
  ],
  templateUrl: './agent-form.component.html',
  styleUrl: './agent-form.component.scss',
})
export class AgentFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly agentService = inject(AgentService);
  private readonly referralTypeService = inject(ReferralTypeService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Agent');

  protected readonly previewName     = signal('');
  protected readonly previewPhone    = signal('');
  protected readonly previewEmail    = signal('');
  protected readonly previewArea     = signal('');
  protected readonly previewLocality = signal('');
  protected readonly previewSeats    = signal<number | null>(null);
  protected readonly previewComm     = signal<number | null>(null);
  protected readonly previewActive   = signal(true);
  protected readonly previewInitials = computed(() => {
    const n = this.previewName();
    if (!n) return '';
    const parts = n.split(/\s+/).filter(Boolean);
    return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase();
  });

  protected readonly TIPS: CmsTip[] = [
    { icon: 'person',         title: 'Identity',   subtitle: 'Use the agent\'s legal name as it should appear on receipts and reports.' },
    { icon: 'location_on',    title: 'Coverage',   subtitle: 'Area + Locality help cluster admissions geographically for analytics.' },
    { icon: 'currency_rupee', title: 'Commission', subtitle: 'Override the master Referral Type amount only if this agent has a special rate.' },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    phone: [''],
    email: [''],
    area: [''],
    locality: [''],
    allottedSeats: [null as number | null],
    commissionAmount: [null as number | null, [Validators.min(0)]],
    isActive: [true],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewPhone.set((v.phone ?? '').trim());
        this.previewEmail.set((v.email ?? '').trim());
        this.previewArea.set((v.area ?? '').trim());
        this.previewLocality.set((v.locality ?? '').trim());
        this.previewSeats.set(v.allottedSeats != null && v.allottedSeats !== '' ? Number(v.allottedSeats) : null);
        this.previewComm.set(v.commissionAmount != null && v.commissionAmount !== '' ? Number(v.commissionAmount) : null);
        this.previewActive.set(!!v.isActive);
      });
  }

  ngOnInit(): void {
    this.tourService.register('agent-form', AGENT_FORM_TOUR);
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Agent');
      this.loading.set(true);
      this.agentService.getAgentById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            name: item.name, phone: item.phone, email: item.email,
            area: item.area, locality: item.locality,
            allottedSeats: item.allottedSeats,
            commissionAmount: item.commissionAmount,
            isActive: item.isActive,
          });
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load'); void this.router.navigate(['/agents']); },
      });
    } else {
      // Pre-fill commission from the system-defined referral type
      this.referralTypeService.getActiveReferralTypes().subscribe({
        next: (types) => {
          const systemType = types.find((t) => t.isSystemDefined);
          if (systemType?.hasCommission && systemType.commissionAmount) {
            this.form.patchValue({ commissionAmount: systemType.commissionAmount });
          }
        },
        error: () => {},
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: AgentRequest = {
      name: v.name.trim(),
      phone: v.phone || undefined,
      email: v.email || undefined,
      area: v.area || undefined,
      locality: v.locality || undefined,
      allottedSeats: v.allottedSeats ?? undefined,
      commissionAmount: v.commissionAmount ?? undefined,
      isActive: v.isActive,
    };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.agentService.updateAgent(this.itemId!, request) : this.agentService.createAgent(request);
    op$.subscribe({
      next: () => { this.toast.success(this.isEditMode() ? 'Updated' : 'Created'); void this.router.navigate(['/agents']); },
      error: () => { this.toast.error('Failed to save'); this.saving.set(false); },
    });
  }
}
