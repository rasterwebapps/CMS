import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { CampusInfrastructureService } from '../campus-infrastructure.service';
import { Block, Floor, GenderRestriction, Organization, ZoneRequest } from '../campus-infrastructure.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';
import { environment } from '../../../../../environments';

@Component({
  selector: 'app-zone-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, FormsModule, MatCheckboxModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, CmsPreviewCardComponent,
  ],
  templateUrl: './zone-form.component.html',
  styleUrl: './zone-form.component.scss',
})
export class ZoneFormComponent implements OnInit {
  private readonly fb      = inject(FormBuilder);
  private readonly route   = inject(ActivatedRoute);
  private readonly router  = inject(Router);
  private readonly service = inject(CampusInfrastructureService);
  private readonly toast   = inject(ToastService);
  private readonly http    = inject(HttpClient);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Zone');
  protected readonly organizations = signal<Organization[]>([]);
  protected readonly branches      = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly blocks        = signal<Block[]>([]);
  protected readonly floors        = signal<Floor[]>([]);
  protected readonly faculty       = signal<{ id: number; name: string }[]>([]);

  protected readonly previewName     = signal('');
  protected readonly previewIsHostel = signal(false);
  protected readonly previewGender   = signal<GenderRestriction | null>(null);

  protected selectedOrganizationId: number | null = null;
  protected selectedBranchId: number | null = null;
  protected selectedBlockId: number | null = null;

  private zoneId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:              ['', [Validators.required, Validators.maxLength(100)]],
    isHostel:          [false],
    genderRestriction: [null as GenderRestriction | null],
    wardenId:          [null as number | null],
    floorId:           [null as number | null, [Validators.required]],
  });

  ngOnInit(): void {
    this.service.getOrganizations(false).subscribe({ next: (o) => this.organizations.set(o) });
    this.http.get<{ id: number; fullName: string }[]>(`${environment.apiUrl}/faculty`).subscribe({
      next: (data) => this.faculty.set(data.map((f) => ({ id: f.id, name: f.fullName }))),
    });

    this.form.valueChanges.subscribe(v => {
      this.previewName.set((v.name ?? '').trim());
      this.previewIsHostel.set(!!v.isHostel);
      this.previewGender.set(v.genderRestriction ?? null);
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.zoneId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Zone');
      this.loadZone();
    } else {
      const floorId = Number(this.route.snapshot.queryParamMap.get('floorId')) || null;
      if (floorId) {
        this.service.getFloorById(floorId).subscribe({
          next: (f) => {
            this.selectedBlockId = f.blockId;
            this.loadFloorsForBlock(f.blockId);
            this.service.getBlockById(f.blockId).subscribe({
              next: (blk) => {
                this.selectedBranchId = blk.branchId;
                this.loadBlocksForBranch(blk.branchId);
                this.service.getBranchById(blk.branchId).subscribe({
                  next: (br) => {
                    this.selectedOrganizationId = br.organizationId;
                    this.loadBranchesForOrganization(br.organizationId);
                  },
                });
              },
            });
            this.form.patchValue({ floorId });
          },
        });
      }
    }
  }

  protected onOrganizationChange(organizationId: number | null): void {
    this.selectedOrganizationId = organizationId;
    this.selectedBranchId = null;
    this.selectedBlockId = null;
    this.branches.set([]);
    this.blocks.set([]);
    this.floors.set([]);
    this.form.patchValue({ floorId: null });
    if (organizationId) this.loadBranchesForOrganization(organizationId);
  }

  protected onBranchChange(branchId: number | null): void {
    this.selectedBranchId = branchId;
    this.selectedBlockId = null;
    this.blocks.set([]);
    this.floors.set([]);
    this.form.patchValue({ floorId: null });
    if (branchId) this.loadBlocksForBranch(branchId);
  }

  protected onBlockChange(blockId: number | null): void {
    this.selectedBlockId = blockId;
    this.form.patchValue({ floorId: null });
    this.floors.set([]);
    if (blockId) this.loadFloorsForBlock(blockId);
  }

  private loadBranchesForOrganization(organizationId: number): void {
    this.service.getBranchesByOrganization(organizationId).subscribe({ next: (b) => this.branches.set(b) });
  }

  private loadBlocksForBranch(branchId: number): void {
    this.service.getBlocksByBranch(branchId).subscribe({ next: (b) => this.blocks.set(b) });
  }

  private loadFloorsForBlock(blockId: number): void {
    this.service.getFloorsByBlock(blockId).subscribe({ next: (f) => this.floors.set(f) });
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }

    const floorId = this.form.value.floorId;
    const request: ZoneRequest = {
      name:              (this.form.value.name ?? '').trim(),
      isHostel:          !!this.form.value.isHostel,
      genderRestriction: this.form.value.genderRestriction ?? null,
      wardenId:          this.form.value.wardenId ?? null,
      floorId,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.service.updateZone(this.zoneId!, request)
      : this.service.createZone(floorId, request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Zone updated successfully' : 'Zone created successfully');
        this.saving.set(false);
        void this.router.navigate(['/campus-infrastructure']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update zone' : 'Failed to create zone'));
        this.saving.set(false);
      },
    });
  }

  private loadZone(): void {
    if (!this.zoneId) return;
    this.loading.set(true);
    this.service.getZoneById(this.zoneId).subscribe({
      next: (z) => {
        this.form.patchValue({
          name: z.name, isHostel: z.isHostel, genderRestriction: z.genderRestriction,
          wardenId: z.wardenId, floorId: z.floorId,
        });
        this.service.getFloorById(z.floorId).subscribe({
          next: (f) => {
            this.selectedBlockId = f.blockId;
            this.loadFloorsForBlock(f.blockId);
            this.service.getBlockById(f.blockId).subscribe({
              next: (blk) => {
                this.selectedBranchId = blk.branchId;
                this.loadBlocksForBranch(blk.branchId);
                this.service.getBranchById(blk.branchId).subscribe({
                  next: (br) => {
                    this.selectedOrganizationId = br.organizationId;
                    this.loadBranchesForOrganization(br.organizationId);
                  },
                });
              },
            });
          },
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load zone');
        void this.router.navigate(['/campus-infrastructure']);
      },
    });
  }
}
