import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import { SpatialService } from '../spatial.service';
import { DiagramLevel, FloorPlan } from '../spatial.model';
import { CampusInfrastructureService } from '../../hostel/campus-infrastructure/campus-infrastructure.service';
import { Organization, Branch, Block, Floor, Zone, Room } from '../../hostel/campus-infrastructure/campus-infrastructure.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconDeleteComponent } from '../../../shared/icons';
import { FloorPlanFormFlyoutComponent } from '../floor-plan-form-flyout/floor-plan-form-flyout.component';
import { FloorPlanCalibrationFlyoutComponent } from '../floor-plan-calibration-flyout/floor-plan-calibration-flyout.component';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { tourKeyForDiagramLevel, buildFloorPlanListTour, buildFloorPlanListFlowMap } from '../../../shared/tour/tours/floor-plan-list.tours';

const LIST_ROUTE_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: '/branch-diagrams',
  FLOOR: '/floor-plans',
  ZONE: '/zone-diagrams',
  ROOM: '/room-diagrams',
};

/** [main, accented] halves of the page heading — kept split so the template can style the second
 *  half with `<em>` the same way every other list-screen title does. */
const PAGE_TITLE_PARTS_BY_LEVEL: Record<DiagramLevel, [string, string]> = {
  BRANCH: ['Branch', 'Diagrams'],
  FLOOR: ['Floor', 'Plans'],
  ZONE: ['Zone', 'Diagrams'],
  ROOM: ['Room', 'Diagrams'],
};

const PAGE_SUBTITLE_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: 'Upload a branch-wide diagram/SVG showing its Blocks, calibrate it, then place Block markers on it.',
  FLOOR: "Upload a floor's image/SVG plan, calibrate it against a real-world distance, then place virtual locations on it.",
  ZONE: "Upload a zone's image/SVG plan showing its Rooms, calibrate it, then place Room markers on it.",
  ROOM: "Upload a room's image/SVG plan, calibrate it, then place Equipment/Inventory Item markers on it.",
};

const PICKER_EMPTY_LABEL_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: 'Pick a Branch',
  FLOOR: 'Pick a Floor',
  ZONE: 'Pick a Zone',
  ROOM: 'Pick a Room',
};

const PICKER_EMPTY_SUBTITLE_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: 'Choose an Organization and Branch above to view or add its diagram.',
  FLOOR: 'Choose an Organization, Branch, Block, and Floor above to view or add its floor plans.',
  ZONE: 'Choose an Organization, Branch, Block, Floor, and Zone above to view or add its diagrams.',
  ROOM: 'Choose an Organization, Branch, Block, Floor, Zone, and Room above to view or add its diagrams.',
};

const ADD_BUTTON_LABEL_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: 'Add Diagram',
  FLOOR: 'Add Floor Plan',
  ZONE: 'Add Diagram',
  ROOM: 'Add Diagram',
};

const EMPTY_TITLE_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: 'No diagrams yet',
  FLOOR: 'No floor plans yet',
  ZONE: 'No diagrams yet',
  ROOM: 'No diagrams yet',
};

const EMPTY_SUBTITLE_BY_LEVEL: Record<DiagramLevel, string> = {
  BRANCH: "Add this branch's first diagram image or SVG to get started.",
  FLOOR: "Add this floor's first plan image or SVG to get started.",
  ZONE: "Add this zone's first diagram image or SVG to get started.",
  ROOM: "Add this room's first diagram image or SVG to get started.",
};

@Component({
  selector: 'app-floor-plan-list',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    MatDialogModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconDeleteComponent,
    FloorPlanFormFlyoutComponent,
    FloorPlanCalibrationFlyoutComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './floor-plan-list.component.html',
  styleUrl: './floor-plan-list.component.scss',
})
export class FloorPlanListComponent implements OnInit {
  private readonly spatialService = inject(SpatialService);
  private readonly campusService = inject(CampusInfrastructureService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly permissionService = inject(PermissionService);
  private readonly tourService = inject(TourService);

  protected readonly canManage = computed(() => this.permissionService.has('SPATIAL_FLOOR_PLAN_MANAGE'));

  /** BRANCH: diagrams showing Blocks. FLOOR: diagrams showing Zones/Rooms/Equipment/Inventory
   *  Items. ZONE: diagrams showing Rooms. ROOM: diagrams showing Equipment/Inventory Items.
   *  Block itself never gets its own diagram — see the Skyline view. */
  protected level: DiagramLevel = 'FLOOR';
  protected readonly tourKey = () => tourKeyForDiagramLevel(this.level);
  protected readonly pageTitleMain = computed(() => PAGE_TITLE_PARTS_BY_LEVEL[this.level][0]);
  protected readonly pageTitleAccent = computed(() => PAGE_TITLE_PARTS_BY_LEVEL[this.level][1]);
  protected readonly pageSubtitle = computed(() => PAGE_SUBTITLE_BY_LEVEL[this.level]);
  protected readonly pickerEmptyLabel = computed(() => PICKER_EMPTY_LABEL_BY_LEVEL[this.level]);
  protected readonly pickerEmptySubtitle = computed(() => PICKER_EMPTY_SUBTITLE_BY_LEVEL[this.level]);
  protected readonly addButtonLabel = computed(() => ADD_BUTTON_LABEL_BY_LEVEL[this.level]);
  protected readonly emptyTitle = computed(() => EMPTY_TITLE_BY_LEVEL[this.level]);
  protected readonly emptySubtitle = computed(() => EMPTY_SUBTITLE_BY_LEVEL[this.level]);

  /** Which picker selects to show, beyond the always-present Organization/Branch pair. */
  protected readonly showBlockFloorPicker = computed(() => this.level === 'FLOOR' || this.level === 'ZONE' || this.level === 'ROOM');
  protected readonly showZonePicker = computed(() => this.level === 'ZONE' || this.level === 'ROOM');
  protected readonly showRoomPicker = computed(() => this.level === 'ROOM');

  protected readonly organizations = signal<Organization[]>([]);
  protected readonly branches = signal<Branch[]>([]);
  protected readonly blocks = signal<Block[]>([]);
  protected readonly floors = signal<Floor[]>([]);
  protected readonly zones = signal<Zone[]>([]);
  protected readonly rooms = signal<Room[]>([]);

  protected selectedOrganizationId: number | null = null;
  protected selectedBranchId: number | null = null;
  protected selectedBlockId: number | null = null;
  protected selectedFloorId: number | null = null;
  protected selectedZoneId: number | null = null;
  protected selectedRoomId: number | null = null;

  /** The entity a diagram actually attaches to. Deliberately a plain method, not `computed()` —
   *  it reads plain (non-signal) fields, which a `computed()` can't track as a dependency;
   *  wrapping it in `computed()` froze it at its first-ever value (null) since nothing ever
   *  invalidated the memoized result. A plain method re-evaluates on every template read instead,
   *  same as ordinary Angular change detection did for these fields before. */
  protected selectedEntityId(): number | null {
    switch (this.level) {
      case 'BRANCH': return this.selectedBranchId;
      case 'FLOOR': return this.selectedFloorId;
      case 'ZONE': return this.selectedZoneId;
      case 'ROOM': return this.selectedRoomId;
    }
  }

  /** This screen (`/branch-diagrams`, `/floor-plans`, `/zone-diagrams`, `/room-diagrams`) is
   *  reached only via deep links from Campus Setup — it has no entry in the nav sidebar (see
   *  `project_oc155_campus_infra_consolidation` — only the diagram screens' 4 nav-config entries
   *  were removed, not the routes themselves). Without an on-screen way back, landing here strands
   *  the user with no path back to where they came from. Deep-links back to the current selection
   *  using the same query-param shape Campus Setup's `initializeFromQueryParamsOrDefault` already
   *  reads; falls back to a bare link (still lands on Campus Setup, just not deep-linked) before
   *  anything is picked. Plain method, not `computed()`, for the same reason as
   *  `selectedEntityId()` above. */
  protected campusSetupLink(): Record<string, number> {
    const id = this.selectedEntityId();
    if (id == null) return {};
    const paramName: Record<DiagramLevel, string> = { BRANCH: 'branchId', FLOOR: 'floorId', ZONE: 'zoneId', ROOM: 'roomId' };
    return { [paramName[this.level]]: id };
  }

  protected readonly loading = signal(false);
  protected readonly floorPlans = signal<FloorPlan[]>([]);

  protected readonly showFormFlyout = signal(false);
  protected editingPlan: FloorPlan | null = null;

  protected readonly showCalibrationFlyout = signal(false);
  protected calibratingPlan: FloorPlan | null = null;

  ngOnInit(): void {
    this.level = (this.route.snapshot.data['level'] as DiagramLevel) ?? 'FLOOR';
    this.tourService.register(this.tourKey(), buildFloorPlanListTour(this.level));
    this.tourService.registerFlowMap(this.tourKey(), buildFloorPlanListFlowMap(this.level));

    this.campusService.getOrganizations(false).subscribe({ next: (o) => this.organizations.set(o) });

    if (this.level === 'BRANCH') {
      const branchId = Number(this.route.snapshot.queryParamMap.get('branchId')) || null;
      if (branchId) this.preselectFromBranch(branchId);
    } else if (this.level === 'FLOOR') {
      const floorId = Number(this.route.snapshot.queryParamMap.get('floorId')) || null;
      if (floorId) this.preselectFromFloor(floorId);
    } else if (this.level === 'ZONE') {
      const zoneId = Number(this.route.snapshot.queryParamMap.get('zoneId')) || null;
      if (zoneId) this.preselectFromZone(zoneId);
    } else if (this.level === 'ROOM') {
      const roomId = Number(this.route.snapshot.queryParamMap.get('roomId')) || null;
      if (roomId) this.preselectFromRoom(roomId);
    }
  }

  protected onOrganizationChange(organizationId: number | null): void {
    this.selectedOrganizationId = organizationId;
    this.resetBelowOrganization();
    if (organizationId) {
      this.campusService.getBranchesByOrganization(organizationId).subscribe({ next: (b) => this.branches.set(b) });
    }
  }

  protected onBranchChange(branchId: number | null): void {
    this.selectedBranchId = branchId;
    this.resetBelowBranch();
    if (!branchId) return;

    if (this.level === 'BRANCH') {
      this.loadFloorPlans();
    } else {
      this.campusService.getBlocksByBranch(branchId).subscribe({ next: (b) => this.blocks.set(b) });
    }
  }

  protected onBlockChange(blockId: number | null): void {
    this.selectedBlockId = blockId;
    this.resetBelowBlock();
    if (blockId) {
      this.campusService.getFloorsByBlock(blockId).subscribe({ next: (f) => this.floors.set(f) });
    }
  }

  protected onFloorChange(floorId: number | null): void {
    this.selectedFloorId = floorId;
    this.resetBelowFloor();
    if (!floorId) return;

    if (this.level === 'FLOOR') {
      this.loadFloorPlans();
    } else {
      this.campusService.getZonesByFloor(floorId).subscribe({ next: (z) => this.zones.set(z) });
    }
  }

  protected onZoneChange(zoneId: number | null): void {
    this.selectedZoneId = zoneId;
    this.resetBelowZone();
    if (!zoneId) return;

    if (this.level === 'ZONE') {
      this.loadFloorPlans();
    } else {
      this.campusService.getRoomsByZone(zoneId).subscribe({ next: (r) => this.rooms.set(r) });
    }
  }

  protected onRoomChange(roomId: number | null): void {
    this.selectedRoomId = roomId;
    this.floorPlans.set([]);
    if (roomId) this.loadFloorPlans();
  }

  protected openAddFlyout(): void {
    this.editingPlan = null;
    this.showFormFlyout.set(true);
  }

  protected editPlan(plan: FloorPlan): void {
    this.editingPlan = plan;
    this.showFormFlyout.set(true);
  }

  protected closeFormFlyout(): void {
    this.showFormFlyout.set(false);
    this.editingPlan = null;
  }

  protected onPlanSaved(): void {
    this.closeFormFlyout();
    if (this.selectedEntityId()) this.loadFloorPlans();
  }

  protected calibratePlan(plan: FloorPlan): void {
    this.calibratingPlan = plan;
    this.showCalibrationFlyout.set(true);
  }

  protected closeCalibrationFlyout(): void {
    this.showCalibrationFlyout.set(false);
    this.calibratingPlan = null;
  }

  protected onCalibrationSaved(): void {
    this.closeCalibrationFlyout();
    if (this.selectedEntityId()) this.loadFloorPlans();
  }

  protected manageLocations(plan: FloorPlan): void {
    void this.router.navigate([LIST_ROUTE_BY_LEVEL[this.level], plan.id, 'locations']);
  }

  protected deletePlan(plan: FloorPlan): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Floor Plan',
        message: `Delete "${plan.name}"? Every virtual location placed on it will also be removed.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(plan);
    });
  }

  protected handleEmptyAction(): void {
    if (this.selectedEntityId()) this.openAddFlyout();
  }

  private performDelete(plan: FloorPlan): void {
    this.spatialService.deleteFloorPlan(plan.id).subscribe({
      next: () => {
        this.toast.success('Floor plan deleted successfully');
        if (this.selectedEntityId()) this.loadFloorPlans();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete floor plan'),
    });
  }

  private loadFloorPlans(): void {
    const entityId = this.selectedEntityId();
    if (!entityId) return;
    this.loading.set(true);
    this.spatialService.getFloorPlansByEntity(this.level, entityId).subscribe({
      next: (plans) => { this.floorPlans.set(plans); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load floor plans'); this.loading.set(false); },
    });
  }

  /** Walks Branch→Organization to pre-populate the two top selects, then loads the branch's
   *  diagrams — lets Campus Setup's "Import Floor Plan" button (Branch edit panel) jump straight
   *  here. One level shallower than `preselectFromFloor` below since Branch has no Block ancestor. */
  private preselectFromBranch(branchId: number): void {
    this.campusService.getBranchById(branchId).subscribe({
      next: (branch) => {
        this.campusService.getBranchesByOrganization(branch.organizationId).subscribe({
          next: (branches) => {
            this.selectedOrganizationId = branch.organizationId;
            this.branches.set(branches);
            this.selectedBranchId = branchId;
            this.loadFloorPlans();
          },
        });
      },
      error: () => this.toast.error('Failed to load that branch'),
    });
  }

  /** Walks Floor→Block→Branch→Organization to pre-populate every select, then loads the floor's
   *  plans — lets a "View Diagram" link elsewhere in the app jump straight to a floor's diagram. */
  private preselectFromFloor(floorId: number): void {
    this.campusService.getFloorById(floorId).subscribe({
      next: (floor) => {
        this.campusService.getBlockById(floor.blockId).subscribe({
          next: (block) => {
            this.campusService.getBranchById(block.branchId).subscribe({
              next: (branch) => {
                forkJoin({
                  branches: this.campusService.getBranchesByOrganization(branch.organizationId),
                  blocks: this.campusService.getBlocksByBranch(block.branchId),
                  floors: this.campusService.getFloorsByBlock(floor.blockId),
                }).subscribe({
                  next: ({ branches, blocks, floors }) => {
                    this.selectedOrganizationId = branch.organizationId;
                    this.branches.set(branches);
                    this.selectedBranchId = block.branchId;
                    this.blocks.set(blocks);
                    this.selectedBlockId = floor.blockId;
                    this.floors.set(floors);
                    this.selectedFloorId = floorId;
                    this.loadFloorPlans();
                  },
                });
              },
            });
          },
        });
      },
      error: () => this.toast.error('Failed to load that floor'),
    });
  }

  /** Same reverse-walk as `preselectFromFloor`, one level further: Zone→Floor→Block→Branch→
   *  Organization. */
  private preselectFromZone(zoneId: number): void {
    this.campusService.getZoneById(zoneId).subscribe({
      next: (zone) => {
        this.campusService.getFloorById(zone.floorId).subscribe({
          next: (floor) => {
            this.campusService.getBlockById(floor.blockId).subscribe({
              next: (block) => {
                this.campusService.getBranchById(block.branchId).subscribe({
                  next: (branch) => {
                    forkJoin({
                      branches: this.campusService.getBranchesByOrganization(branch.organizationId),
                      blocks: this.campusService.getBlocksByBranch(block.branchId),
                      floors: this.campusService.getFloorsByBlock(floor.blockId),
                      zones: this.campusService.getZonesByFloor(zone.floorId),
                    }).subscribe({
                      next: ({ branches, blocks, floors, zones }) => {
                        this.selectedOrganizationId = branch.organizationId;
                        this.branches.set(branches);
                        this.selectedBranchId = block.branchId;
                        this.blocks.set(blocks);
                        this.selectedBlockId = floor.blockId;
                        this.floors.set(floors);
                        this.selectedFloorId = zone.floorId;
                        this.zones.set(zones);
                        this.selectedZoneId = zoneId;
                        this.loadFloorPlans();
                      },
                    });
                  },
                });
              },
            });
          },
        });
      },
      error: () => this.toast.error('Failed to load that zone'),
    });
  }

  /** Same reverse-walk again, one level further still: Room→Zone→Floor→Block→Branch→
   *  Organization. */
  private preselectFromRoom(roomId: number): void {
    this.campusService.getRoomById(roomId).subscribe({
      next: (room) => {
        this.campusService.getZoneById(room.zoneId).subscribe({
          next: (zone) => {
            this.campusService.getFloorById(zone.floorId).subscribe({
              next: (floor) => {
                this.campusService.getBlockById(floor.blockId).subscribe({
                  next: (block) => {
                    this.campusService.getBranchById(block.branchId).subscribe({
                      next: (branch) => {
                        forkJoin({
                          branches: this.campusService.getBranchesByOrganization(branch.organizationId),
                          blocks: this.campusService.getBlocksByBranch(block.branchId),
                          floors: this.campusService.getFloorsByBlock(floor.blockId),
                          zones: this.campusService.getZonesByFloor(zone.floorId),
                          rooms: this.campusService.getRoomsByZone(room.zoneId),
                        }).subscribe({
                          next: ({ branches, blocks, floors, zones, rooms }) => {
                            this.selectedOrganizationId = branch.organizationId;
                            this.branches.set(branches);
                            this.selectedBranchId = block.branchId;
                            this.blocks.set(blocks);
                            this.selectedBlockId = floor.blockId;
                            this.floors.set(floors);
                            this.selectedFloorId = zone.floorId;
                            this.zones.set(zones);
                            this.selectedZoneId = room.zoneId;
                            this.rooms.set(rooms);
                            this.selectedRoomId = roomId;
                            this.loadFloorPlans();
                          },
                        });
                      },
                    });
                  },
                });
              },
            });
          },
        });
      },
      error: () => this.toast.error('Failed to load that room'),
    });
  }

  private resetBelowOrganization(): void {
    this.selectedBranchId = null;
    this.branches.set([]);
    this.resetBelowBranch();
  }

  private resetBelowBranch(): void {
    this.selectedBlockId = null;
    this.blocks.set([]);
    this.resetBelowBlock();
  }

  private resetBelowBlock(): void {
    this.selectedFloorId = null;
    this.floors.set([]);
    this.resetBelowFloor();
  }

  private resetBelowFloor(): void {
    this.selectedZoneId = null;
    this.zones.set([]);
    this.resetBelowZone();
  }

  private resetBelowZone(): void {
    this.selectedRoomId = null;
    this.rooms.set([]);
    this.floorPlans.set([]);
  }
}
