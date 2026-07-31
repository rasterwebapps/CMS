import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CmsPreviewCardComponent } from '../../../../shared/preview-card/preview-card.component';
import { CampusInfrastructureService } from '../campus-infrastructure.service';
import { Block, Floor, Organization, Zone, RoomRequest } from '../campus-infrastructure.model';
import { HostelRoomTypeService } from '../../hostel-room-type/hostel-room-type.service';
import { HostelRoomType } from '../../hostel-room-type/hostel-room-type.model';
import { ToastService } from '../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../shared/utils/scroll-to-invalid';

@Component({
  selector: 'app-room-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, FormsModule, MatCheckboxModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, CmsPreviewCardComponent,
  ],
  templateUrl: './room-form.component.html',
  styleUrl: './room-form.component.scss',
})
export class RoomFormComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly service         = inject(CampusInfrastructureService);
  private readonly roomTypeService = inject(HostelRoomTypeService);
  private readonly toast           = inject(ToastService);

  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle  = signal('Add Room');

  protected readonly organizations = signal<Organization[]>([]);
  protected readonly branches      = signal<{ id: number; name: string; code: string }[]>([]);
  protected readonly blocks        = signal<Block[]>([]);
  protected readonly floors        = signal<Floor[]>([]);
  protected readonly zones         = signal<Zone[]>([]);
  protected readonly roomTypes     = signal<HostelRoomType[]>([]);

  protected selectedOrganizationId: number | null = null;
  protected selectedBranchId: number | null = null;
  protected selectedBlockId: number | null = null;
  protected selectedFloorId: number | null = null;

  protected readonly previewRoomNumber = signal('');
  protected readonly previewCapacity   = signal<number | null>(null);
  protected readonly previewIsHostel   = signal(false);

  private roomId: number | null = null;
  private existingHostelRoomId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    roomNumber:  ['', [Validators.required, Validators.maxLength(50)]],
    capacity:    [null as number | null],
    description: ['', [Validators.maxLength(500)]],
    zoneId:      [null as number | null, [Validators.required]],
    isHostelRoom: [false],
    roomTypeId:  [null as number | null],
  });

  ngOnInit(): void {
    this.service.getOrganizations(false).subscribe({ next: (o) => this.organizations.set(o) });
    this.roomTypeService.getAll(true).subscribe({ next: (rt) => this.roomTypes.set(rt) });

    this.form.valueChanges.subscribe(v => {
      this.previewRoomNumber.set((v.roomNumber ?? '').trim());
      this.previewCapacity.set(v.capacity ?? null);
      this.previewIsHostel.set(!!v.isHostelRoom);
    });

    this.form.get('isHostelRoom')?.valueChanges.subscribe((checked: boolean) => {
      const roomTypeCtrl = this.form.get('roomTypeId');
      if (checked) roomTypeCtrl?.setValidators(Validators.required);
      else roomTypeCtrl?.clearValidators();
      roomTypeCtrl?.updateValueAndValidity({ emitEvent: false });
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.roomId = Number(idParam);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Room');
      this.loadRoom();
    } else {
      const zoneId = Number(this.route.snapshot.queryParamMap.get('zoneId')) || null;
      if (zoneId) {
        this.service.getZoneById(zoneId).subscribe({
          next: (z) => {
            this.selectedFloorId = z.floorId;
            this.service.getFloorById(z.floorId).subscribe({
              next: (f) => {
                this.selectedBlockId = f.blockId;
                this.loadFloorsForBlock(f.blockId);
                this.loadZonesForFloor(z.floorId);
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
                this.form.patchValue({ zoneId });
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
    this.selectedBlockId = null;
    this.selectedFloorId = null;
    this.branches.set([]);
    this.blocks.set([]);
    this.floors.set([]);
    this.zones.set([]);
    this.form.patchValue({ zoneId: null });
    if (organizationId) this.loadBranchesForOrganization(organizationId);
  }

  protected onBranchChange(branchId: number | null): void {
    this.selectedBranchId = branchId;
    this.selectedBlockId = null;
    this.selectedFloorId = null;
    this.blocks.set([]);
    this.floors.set([]);
    this.zones.set([]);
    this.form.patchValue({ zoneId: null });
    if (branchId) this.loadBlocksForBranch(branchId);
  }

  protected onBlockChange(blockId: number | null): void {
    this.selectedBlockId = blockId;
    this.selectedFloorId = null;
    this.floors.set([]);
    this.zones.set([]);
    this.form.patchValue({ zoneId: null });
    if (blockId) this.loadFloorsForBlock(blockId);
  }

  protected onFloorChange(floorId: number | null): void {
    this.selectedFloorId = floorId;
    this.zones.set([]);
    this.form.patchValue({ zoneId: null });
    if (floorId) this.loadZonesForFloor(floorId);
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

  private loadZonesForFloor(floorId: number): void {
    this.service.getZonesByFloor(floorId).subscribe({ next: (z) => this.zones.set(z) });
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }

    const zoneId = this.form.value.zoneId;
    const request: RoomRequest = {
      roomNumber:  (this.form.value.roomNumber ?? '').trim(),
      capacity:    this.form.value.capacity ?? null,
      description: this.form.value.description?.trim() || undefined,
      zoneId,
    };

    this.saving.set(true);
    const op$ = this.isEditMode()
      ? this.service.updateRoom(this.roomId!, request)
      : this.service.createRoom(zoneId, request);

    op$.subscribe({
      next: (room) => this.applyHostelRoomDesignation(room.id),
      error: (err) => {
        this.toast.error(err?.error?.message ?? (this.isEditMode() ? 'Failed to update room' : 'Failed to create room'));
        this.saving.set(false);
      },
    });
  }

  private applyHostelRoomDesignation(roomId: number): void {
    const wantsHostel = !!this.form.value.isHostelRoom;
    const roomTypeId = this.form.value.roomTypeId;

    const done = () => {
      this.toast.success(this.isEditMode() ? 'Room updated successfully' : 'Room created successfully');
      this.saving.set(false);
      void this.router.navigate(['/campus-infrastructure/table']);
    };
    const fail = (err: unknown) => {
      const message = (err as { error?: { message?: string } })?.error?.message ?? 'Room saved, but the hostel-room designation failed to update';
      this.toast.error(message);
      this.saving.set(false);
    };

    if (wantsHostel && roomTypeId) {
      this.service.assignHostelRoom(roomId, { roomTypeId, isActive: true }).subscribe({ next: done, error: fail });
    } else if (!wantsHostel && this.existingHostelRoomId) {
      this.service.unassignHostelRoom(roomId).subscribe({ next: done, error: fail });
    } else {
      done();
    }
  }

  private loadRoom(): void {
    if (!this.roomId) return;
    this.loading.set(true);
    this.service.getRoomById(this.roomId).subscribe({
      next: (r) => {
        this.form.patchValue({
          roomNumber: r.roomNumber, capacity: r.capacity, description: r.description || '',
          zoneId: r.zoneId,
          isHostelRoom: !!r.hostelRoomId,
          roomTypeId: r.hostelRoomTypeId,
        });
        this.existingHostelRoomId = r.hostelRoomId;
        this.service.getZoneById(r.zoneId).subscribe({
          next: (z) => {
            this.selectedFloorId = z.floorId;
            this.loadZonesForFloor(z.floorId);
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
          },
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load room');
        void this.router.navigate(['/campus-infrastructure/table']);
      },
    });
  }
}
