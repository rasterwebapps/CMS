import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CampusInfrastructureService } from '../campus-infrastructure.service';
import { Organization, Branch, Block, Floor, Zone, Room } from '../campus-infrastructure.model';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../../shared/icons';
import { ToastService } from '../../../../core/toast/toast.service';
import { PermissionService } from '../../../../core/permissions/permission.service';

@Component({
  selector: 'app-campus-infrastructure-list',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
  ],
  templateUrl: './campus-infrastructure-list.component.html',
  styleUrl: './campus-infrastructure-list.component.scss',
})
export class CampusInfrastructureListComponent implements OnInit {
  private readonly service = inject(CampusInfrastructureService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);

  protected readonly canManage = computed(() => this.permissionService.has('CAMPUS_INFRASTRUCTURE_MANAGE'));
  protected readonly loading = signal(false);

  protected readonly allOrganizations = signal<Organization[]>([]);
  protected readonly expandedOrganizationId = signal<number | null>(null);
  private readonly branchesByOrganization = signal<Map<number, Branch[]>>(new Map());

  protected readonly expandedBranchId = signal<number | null>(null);
  private readonly blocksByBranch = signal<Map<number, Block[]>>(new Map());

  protected readonly expandedBlockId = signal<number | null>(null);
  private readonly floorsByBlock = signal<Map<number, Floor[]>>(new Map());

  protected readonly expandedFloorId = signal<number | null>(null);
  private readonly zonesByFloor = signal<Map<number, Zone[]>>(new Map());

  protected readonly expandedZoneId = signal<number | null>(null);
  private readonly roomsByZone = signal<Map<number, Room[]>>(new Map());

  ngOnInit(): void {
    this.load();
  }

  protected getBranches(organizationId: number): Branch[] {
    return this.branchesByOrganization().get(organizationId) ?? [];
  }

  protected getBlocks(branchId: number): Block[] {
    return this.blocksByBranch().get(branchId) ?? [];
  }

  protected getFloors(blockId: number): Floor[] {
    return this.floorsByBlock().get(blockId) ?? [];
  }

  protected getZones(floorId: number): Zone[] {
    return this.zonesByFloor().get(floorId) ?? [];
  }

  protected getRooms(zoneId: number): Room[] {
    return this.roomsByZone().get(zoneId) ?? [];
  }

  protected toggleOrganization(organizationId: number): void {
    if (this.expandedOrganizationId() === organizationId) {
      this.expandedOrganizationId.set(null);
      return;
    }
    this.expandedOrganizationId.set(organizationId);
    if (!this.branchesByOrganization().has(organizationId)) {
      this.service.getBranchesByOrganization(organizationId).subscribe({
        next: (branches) => {
          const map = new Map(this.branchesByOrganization());
          map.set(organizationId, branches);
          this.branchesByOrganization.set(map);
        },
        error: () => this.toast.error('Failed to load branches'),
      });
    }
  }

  protected toggleBranch(branchId: number): void {
    if (this.expandedBranchId() === branchId) {
      this.expandedBranchId.set(null);
      return;
    }
    this.expandedBranchId.set(branchId);
    if (!this.blocksByBranch().has(branchId)) {
      this.service.getBlocksByBranch(branchId).subscribe({
        next: (blocks) => {
          const map = new Map(this.blocksByBranch());
          map.set(branchId, blocks);
          this.blocksByBranch.set(map);
        },
        error: () => this.toast.error('Failed to load blocks'),
      });
    }
  }

  protected toggleBlock(blockId: number): void {
    if (this.expandedBlockId() === blockId) {
      this.expandedBlockId.set(null);
      return;
    }
    this.expandedBlockId.set(blockId);
    if (!this.floorsByBlock().has(blockId)) {
      this.service.getFloorsByBlock(blockId).subscribe({
        next: (floors) => {
          const map = new Map(this.floorsByBlock());
          map.set(blockId, floors);
          this.floorsByBlock.set(map);
        },
        error: () => this.toast.error('Failed to load floors'),
      });
    }
  }

  protected toggleFloor(floorId: number): void {
    if (this.expandedFloorId() === floorId) {
      this.expandedFloorId.set(null);
      return;
    }
    this.expandedFloorId.set(floorId);
    if (!this.zonesByFloor().has(floorId)) {
      this.service.getZonesByFloor(floorId).subscribe({
        next: (zones) => {
          const map = new Map(this.zonesByFloor());
          map.set(floorId, zones);
          this.zonesByFloor.set(map);
        },
        error: () => this.toast.error('Failed to load zones'),
      });
    }
  }

  protected toggleZone(zoneId: number): void {
    if (this.expandedZoneId() === zoneId) {
      this.expandedZoneId.set(null);
      return;
    }
    this.expandedZoneId.set(zoneId);
    if (!this.roomsByZone().has(zoneId)) {
      this.service.getRoomsByZone(zoneId).subscribe({
        next: (rooms) => {
          const map = new Map(this.roomsByZone());
          map.set(zoneId, rooms);
          this.roomsByZone.set(map);
        },
        error: () => this.toast.error('Failed to load rooms'),
      });
    }
  }

  protected editOrganization(organization: Organization): void {
    void this.router.navigate(['/campus-infrastructure/organizations', organization.id, 'edit']);
  }

  protected editBranch(branch: Branch): void {
    void this.router.navigate(['/campus-infrastructure/branches', branch.id, 'edit']);
  }

  protected editBlock(block: Block): void {
    void this.router.navigate(['/campus-infrastructure/blocks', block.id, 'edit']);
  }

  protected editFloor(floor: Floor): void {
    void this.router.navigate(['/campus-infrastructure/floors', floor.id, 'edit']);
  }

  protected editZone(zone: Zone): void {
    void this.router.navigate(['/campus-infrastructure/zones', zone.id, 'edit']);
  }

  protected editRoom(room: Room): void {
    void this.router.navigate(['/campus-infrastructure/rooms', room.id, 'edit']);
  }

  protected toggleOrganizationStatus(organization: Organization): void {
    const nextAction = organization.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: `${nextAction} Organization`, message: `${nextAction} "${organization.name}" (${organization.code})?`, confirmText: nextAction, cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.service.updateOrganizationStatus(organization.id, { isActive: !organization.isActive }).subscribe({
        next: () => { this.toast.success(`Organization ${organization.isActive ? 'deactivated' : 'activated'}`); this.load(); },
        error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update organization status'),
      });
    });
  }

  protected toggleBranchStatus(branch: Branch): void {
    const nextAction = branch.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: `${nextAction} Branch`, message: `${nextAction} "${branch.name}" (${branch.code})?`, confirmText: nextAction, cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.service.updateBranchStatus(branch.id, { isActive: !branch.isActive }).subscribe({
        next: () => {
          this.toast.success(`Branch ${branch.isActive ? 'deactivated' : 'activated'}`);
          this.refreshBranches(branch.organizationId);
        },
        error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update branch status'),
      });
    });
  }

  protected toggleBlockStatus(block: Block): void {
    const nextAction = block.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: `${nextAction} Block`, message: `${nextAction} "${block.name}" (${block.code})?`, confirmText: nextAction, cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.service.updateBlockStatus(block.id, { isActive: !block.isActive }).subscribe({
        next: () => {
          this.toast.success(`Block ${block.isActive ? 'deactivated' : 'activated'}`);
          this.refreshBlocks(block.branchId);
        },
        error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update block status'),
      });
    });
  }

  protected toggleFloorStatus(floor: Floor): void {
    const nextAction = floor.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: `${nextAction} Floor`, message: `${nextAction} "${floor.name}"?`, confirmText: nextAction, cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.service.updateFloorStatus(floor.id, { isActive: !floor.isActive }).subscribe({
        next: () => {
          this.toast.success(`Floor ${floor.isActive ? 'deactivated' : 'activated'}`);
          this.refreshFloors(floor.blockId);
        },
        error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update floor status'),
      });
    });
  }

  protected toggleZoneStatus(zone: Zone): void {
    const nextAction = zone.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: `${nextAction} Zone`, message: `${nextAction} "${zone.name}"?`, confirmText: nextAction, cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.service.updateZoneStatus(zone.id, { isActive: !zone.isActive }).subscribe({
        next: () => {
          this.toast.success(`Zone ${zone.isActive ? 'deactivated' : 'activated'}`);
          this.refreshZones(zone.floorId);
        },
        error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update zone status'),
      });
    });
  }

  protected toggleRoomStatus(room: Room): void {
    const nextAction = room.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: `${nextAction} Room`, message: `${nextAction} "${room.roomNumber}"?`, confirmText: nextAction, cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.service.updateRoomStatus(room.id, { isActive: !room.isActive }).subscribe({
        next: () => {
          this.toast.success(`Room ${room.isActive ? 'deactivated' : 'activated'}`);
          this.refreshRooms(room.zoneId);
        },
        error: (err) => this.toast.error(err?.error?.message ?? 'Failed to update room status'),
      });
    });
  }

  protected handleEmptyAction(): void {
    if (this.canManage()) void this.router.navigate(['/campus-infrastructure/organizations/new']);
  }

  private refreshBranches(organizationId: number): void {
    this.service.getBranchesByOrganization(organizationId).subscribe((branches) => {
      const map = new Map(this.branchesByOrganization());
      map.set(organizationId, branches);
      this.branchesByOrganization.set(map);
    });
  }

  private refreshBlocks(branchId: number): void {
    this.service.getBlocksByBranch(branchId).subscribe((blocks) => {
      const map = new Map(this.blocksByBranch());
      map.set(branchId, blocks);
      this.blocksByBranch.set(map);
    });
  }

  private refreshFloors(blockId: number): void {
    this.service.getFloorsByBlock(blockId).subscribe((floors) => {
      const map = new Map(this.floorsByBlock());
      map.set(blockId, floors);
      this.floorsByBlock.set(map);
    });
  }

  private refreshZones(floorId: number): void {
    this.service.getZonesByFloor(floorId).subscribe((zones) => {
      const map = new Map(this.zonesByFloor());
      map.set(floorId, zones);
      this.zonesByFloor.set(map);
    });
  }

  private refreshRooms(zoneId: number): void {
    this.service.getRoomsByZone(zoneId).subscribe((rooms) => {
      const map = new Map(this.roomsByZone());
      map.set(zoneId, rooms);
      this.roomsByZone.set(map);
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.getOrganizations(false).subscribe({
      next: (organizations) => { this.allOrganizations.set(organizations); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load organizations'); this.loading.set(false); },
    });
  }
}
