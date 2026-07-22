import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CampusInfrastructureService } from '../campus-infrastructure.service';
import { Block, FloorRequest, GenderRestriction, Organization } from '../campus-infrastructure.model';
import { ToastService } from '../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';

@Component({
  selector: 'app-floor-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, FormsModule, MatCheckboxModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, CmsPreviewCardComponent,
  ],
  templateUrl: './floor-form.component.html',
  styleUrl: './floor-form.component.scss',
})
export class FloorFormComponent implements OnInit {
  private readonly fb       = inject(FormBuilder);
  private readonly route    = inject(ActivatedRoute);
  private readonly router   = inject(Router);
  private readonly service  = inject(CampusInfrastructureService);
  private readonly toast    = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Floor');
  protected readonly organizations = signal<Organization[]>([]);
  protected readonly branches      = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly blocks        = signal<Block[]>([]);

  protected selectedOrganizationId: number | null = null;
  protected selectedBranchId: number | null = null;

  protected readonly previewName     = signal('');
  protected readonly previewIsHostel = signal(false);
  protected readonly previewGender   = signal<GenderRestriction | null>(null);

  private floorId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name:              ['', [Validators.required, Validators.maxLength(100)]],
    floorNumber:       [null, [Validators.required]],
    isHostel:          [false],
    genderRestriction: [null as GenderRestriction | null],
    blockId:           [null as number | null, [Validators.required]],
  });

  ngOnInit(): void {
    this.service.getOrganizations(false).subscribe({ next: (o) => this.organizations.set(o) });

    this.form.valueChanges.subscribe(v => {
      this.previewName.set((v.name ?? '').trim());
      this.previewIsHostel.set(!!v.isHostel);
      this.previewGender.set(v.genderRestriction ?? null);
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.floorId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Floor');
      this.loadFloor();
    } else {
      const blockId = Number(this.route.snapshot.queryParamMap.get('blockId')) || null;
      if (blockId) {
        this.service.getBlockById(blockId).subscribe({
          next: (blk) => {
            this.selectedBranchId = blk.branchId;
            this.service.getBranchById(blk.branchId).subscribe({
              next: (br) => {
                this.selectedOrganizationId = br.organizationId;
                this.loadBranchesForOrganization(br.organizationId);
                this.loadBlocksForBranch(blk.branchId);
                this.form.patchValue({ blockId });
              },
            });
          },
        });
      }
    }
  }

  protected onOrganizationChange(organizationId: number | null): void {
    this.selectedOrganizationId = organizationId;
    this.selectedBranchId = null;
    this.branches.set([]);
    this.blocks.set([]);
    this.form.patchValue({ blockId: null });
    if (organizationId) this.loadBranchesForOrganization(organizationId);
  }

  protected onBranchChange(branchId: number | null): void {
    this.selectedBranchId = branchId;
    this.blocks.set([]);
    this.form.patchValue({ blockId: null });
    if (branchId) this.loadBlocksForBranch(branchId);
  }

  private loadBranchesForOrganization(organizationId: number): void {
    this.service.getBranchesByOrganization(organizationId).subscribe({ next: (b) => this.branches.set(b) });
  }

  private loadBlocksForBranch(branchId: number): void {
    this.service.getBlocksByBranch(branchId).subscribe({ next: (b) => this.blocks.set(b) });
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }

    const blockId = this.form.value.blockId;
    const request: FloorRequest = {
      name:              (this.form.value.name ?? '').trim(),
      floorNumber:       this.form.value.floorNumber,
      isHostel:          !!this.form.value.isHostel,
      genderRestriction: this.form.value.genderRestriction ?? null,
      blockId,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.service.updateFloor(this.floorId!, request)
      : this.service.createFloor(blockId, request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Floor updated successfully' : 'Floor created successfully');
        this.saving.set(false);
        void this.router.navigate(['/campus-infrastructure']);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update floor' : 'Failed to create floor'));
        this.saving.set(false);
      },
    });
  }

  private loadFloor(): void {
    if (!this.floorId) return;
    this.loading.set(true);
    this.service.getFloorById(this.floorId).subscribe({
      next: (f) => {
        this.form.patchValue({
          name: f.name, floorNumber: f.floorNumber, isHostel: f.isHostel,
          genderRestriction: f.genderRestriction, blockId: f.blockId,
        });
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
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load floor');
        void this.router.navigate(['/campus-infrastructure']);
      },
    });
  }
}
