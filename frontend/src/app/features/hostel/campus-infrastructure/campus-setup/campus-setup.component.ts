import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
import { CampusInfrastructureService } from '../campus-infrastructure.service';
import { Block, Branch, Floor, HostelRoom, Organization, Room, Zone } from '../campus-infrastructure.model';
import { CampusLevelGridBadge, CampusLevelGridComponent, CampusLevelGridItem } from './campus-level-grid/campus-level-grid.component';
import { CampusPanelLevel, CampusSidePanelComponent } from './campus-side-panel/campus-side-panel.component';
import { CampusSkylineComponent, SkylineBlock, SkylineFloor, SkylineZone } from './campus-skyline/campus-skyline.component';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { PermissionService } from '../../../../core/permissions/permission.service';
import { TourService } from '../../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../../shared/tour/tour-button.component';
import { CAMPUS_SETUP_TOUR, CAMPUS_SETUP_FLOW_MAP } from '../../../../shared/tour/tours/campus-setup.tours';

interface Crumb {
  label: string;
  onClick: () => void;
}

/**
 * Campus Setup — the visual campus builder. Organization tabs up top, then a breadcrumb-driven
 * drill-down (Branch › Block › Floor › Zone › Room) on the left with a persistent
 * side panel on the right that owns every "add" and "jump to" action.
 *
 * Rebuilt from the original drag-and-drop skyline diagram: that version nested inline "+" popovers
 * (`campus-inline-add`) several flex/drag layers deep, which broke out of their card layout under
 * a lingering-transform containing-block bug. Moving every add action into one fixed side panel
 * removes that whole bug class instead of chasing the specific ancestor.
 *
 * The single create/edit surface for the whole Org→Branch→Block→Floor→Zone→Room hierarchy,
 * including full-detail fields (hostel flags, gender restriction, warden, capacity…) and floor-plan
 * import — the former standalone browse tree (`campus-infrastructure-list`) and the six per-level
 * full-detail forms (`organization-form`, `branch-form`, etc.) were retired once this panel reached
 * parity with them.
 */
@Component({
  selector: 'app-campus-setup',
  standalone: true,
  imports: [
    MatIconModule,
    MatTooltipModule,
    CampusLevelGridComponent,
    CampusSidePanelComponent,
    CampusSkylineComponent,
    CmsStatusBadgeComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './campus-setup.component.html',
  styleUrl: './campus-setup.component.scss',
})
export class CampusSetupComponent implements OnInit {
  private readonly service = inject(CampusInfrastructureService);
  private readonly toast = inject(ToastService);
  private readonly permissionService = inject(PermissionService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tourService = inject(TourService);
  protected readonly canManage = computed(() => this.permissionService.has('CAMPUS_INFRASTRUCTURE_MANAGE'));

  protected readonly organizations = signal<Organization[]>([]);
  protected readonly selectedOrganizationId = signal<number | null>(null);
  protected readonly loadingOrgs = signal(true);

  protected readonly branches = signal<Branch[]>([]);
  protected readonly blocks = signal<Block[]>([]);
  protected readonly floors = signal<Floor[]>([]);
  protected readonly zones = signal<Zone[]>([]);
  protected readonly rooms = signal<Room[]>([]);
  protected readonly hostelRoom = signal<HostelRoom | null>(null);

  protected readonly loadingBranches = signal(false);
  protected readonly loadingBlocks = signal(false);
  protected readonly loadingFloors = signal(false);
  protected readonly loadingZones = signal(false);
  protected readonly loadingRooms = signal(false);

  protected readonly skylineBlocks = signal<SkylineBlock[]>([]);
  protected readonly loadingSkyline = signal(false);

  protected readonly selectedBranchId = signal<number | null>(null);
  protected readonly selectedBlockId = signal<number | null>(null);
  protected readonly selectedFloorId = signal<number | null>(null);
  protected readonly selectedZoneId = signal<number | null>(null);
  protected readonly selectedRoomId = signal<number | null>(null);

  /** Separate from the "selected*" drill position above: hovering/tapping a card's edit pencil
   *  shows that entity's properties in the side panel *without* navigating the main viewport into
   *  its children — the two used to be the same click, now they're independent. Room has no
   *  separate editing state since it's a leaf (selecting one already means viewing its properties,
   *  same as before). Cleared on every navigation (see `clearEditing`) and after a successful save
   *  (`onPanelSaved`), so the panel falls back to that level's normal "Add child" form. */
  protected readonly editingLevel = signal<'organization' | 'branch' | 'block' | 'floor' | 'zone' | null>(null);
  protected readonly editingId = signal<number | null>(null);

  protected readonly selectedOrganization = computed(() =>
    this.organizations().find((o) => o.id === this.selectedOrganizationId()) ?? null
  );
  protected readonly selectedBranch = computed(() => this.branches().find((b) => b.id === this.selectedBranchId()) ?? null);
  protected readonly selectedBlock = computed(() => this.blocks().find((b) => b.id === this.selectedBlockId()) ?? null);
  protected readonly selectedFloor = computed(() => this.floors().find((f) => f.id === this.selectedFloorId()) ?? null);
  protected readonly selectedZone = computed(() => this.zones().find((z) => z.id === this.selectedZoneId()) ?? null);
  protected readonly selectedRoom = computed(() => this.rooms().find((r) => r.id === this.selectedRoomId()) ?? null);

  protected readonly editingOrganization = computed(() =>
    this.editingLevel() === 'organization' ? this.organizations().find((o) => o.id === this.editingId()) ?? null : null
  );
  protected readonly editingBranch = computed(() =>
    this.editingLevel() === 'branch' ? this.branches().find((b) => b.id === this.editingId()) ?? null : null
  );
  protected readonly editingBlock = computed(() =>
    this.editingLevel() === 'block' ? this.blocks().find((b) => b.id === this.editingId()) ?? null : null
  );
  protected readonly editingFloor = computed(() =>
    this.editingLevel() === 'floor' ? this.floors().find((f) => f.id === this.editingId()) ?? null : null
  );
  protected readonly editingZone = computed(() =>
    this.editingLevel() === 'zone' ? this.zones().find((z) => z.id === this.editingId()) ?? null : null
  );

  protected readonly breadcrumbs = computed<Crumb[]>(() => {
    // The root crumb reads as the Organization's own name (e.g. "SKS College Of Nursing") now that
    // it's no longer shown separately in the toolbar (`campus-setup__org-name`, removed) — 'Branches'
    // is only a fallback for the moment before the Organization has loaded.
    const rootLabel = this.selectedOrganization()?.name ?? 'Branches';
    const crumbs: Crumb[] = [{ label: rootLabel, onClick: () => this.selectBranch(null) }];
    const branch = this.selectedBranch();
    if (branch) crumbs.push({ label: branch.name, onClick: () => this.selectBlock(null) });
    const block = this.selectedBlock();
    if (block) crumbs.push({ label: block.name, onClick: () => this.selectFloor(null) });
    const floor = this.selectedFloor();
    if (floor) crumbs.push({ label: floor.name, onClick: () => this.selectZone(null) });
    const zone = this.selectedZone();
    if (zone) crumbs.push({ label: zone.name, onClick: () => this.selectRoom(null) });
    const room = this.selectedRoom();
    if (room) crumbs.push({ label: room.roomNumber, onClick: () => {} });
    return crumbs;
  });

  /** Mirrors the viewport's own @if/@else-if drill-down order so the tag always names
   *  what's actually on screen, not just the last-clicked entity's name. */
  protected readonly currentLevelLabel = computed<string>(() => {
    if (this.selectedRoom()) return 'Room';
    if (this.selectedZone()) return 'Rooms';
    if (this.selectedFloor()) return 'Zones';
    if (this.selectedBlock()) return 'Floors';
    if (this.selectedBranch()) return 'Blocks';
    return 'Branches';
  });

  protected readonly searchQuery = signal('');
  private readonly normalizedQuery = computed(() => this.searchQuery().trim().toLowerCase());

  protected readonly branchItems = computed<CampusLevelGridItem[]>(() => {
    const q = this.normalizedQuery();
    return this.branches()
      .filter((b) => !q || b.name.toLowerCase().includes(q) || b.code.toLowerCase().includes(q))
      .map((b) => ({ id: b.id, title: b.name, subtitle: b.code, icon: 'domain', isActive: b.isActive }));
  });
  protected readonly zoneItems = computed<CampusLevelGridItem[]>(() => {
    const q = this.normalizedQuery();
    return this.zones()
      .filter((z) => !q || z.name.toLowerCase().includes(q))
      .map((z) => {
        const badges: CampusLevelGridBadge[] = [];
        if (z.isHostel) badges.push({ label: 'Hostel', tone: 'hostel' });
        if (z.genderRestriction === 'BOYS') badges.push({ label: 'Boys', tone: 'boys' });
        if (z.genderRestriction === 'GIRLS') badges.push({ label: 'Girls', tone: 'girls' });
        return {
          id: z.id,
          title: z.name,
          subtitle: z.wardenName ? `Warden: ${z.wardenName}` : '',
          icon: 'grid_view',
          isActive: z.isActive,
          badges,
        };
      });
  });
  protected readonly roomItems = computed<CampusLevelGridItem[]>(() => {
    const q = this.normalizedQuery();
    return this.rooms()
      .filter((r) => !q || r.roomNumber.toLowerCase().includes(q))
      .map((r) => {
        const badges: CampusLevelGridBadge[] = [];
        if (r.hostelRoomTypeName) badges.push({ label: r.hostelRoomTypeName, tone: 'hostel' });
        const subtitle = [r.purposeCategoryName, r.subTypeName].filter(Boolean).join(' · ');
        return {
          id: r.id,
          title: r.roomNumber,
          subtitle,
          icon: 'meeting_room',
          isActive: r.isActive,
          badges,
          stat: r.capacity ? `${r.capacity} cap` : undefined,
        };
      });
  });

  /** Skyline shows the whole branch tree at once, so search has to filter it as a tree, not a
   *  flat list: a Block/Floor/Zone whose own name matches keeps all of its children; one that
   *  doesn't match only keeps the branches of the tree that themselves contain a match. */
  protected readonly filteredSkylineBlocks = computed<SkylineBlock[]>(() => {
    const q = this.normalizedQuery();
    const blocks = this.skylineBlocks();
    if (!q) return blocks;

    const matches = (text: string) => text.toLowerCase().includes(q);

    return blocks
      .map((sb): SkylineBlock | null => {
        const blockMatches = matches(sb.block.name);
        const floors = sb.floors
          .map((sf): SkylineFloor | null => {
            const floorMatches = matches(sf.floor.name);
            const zones = sf.zones
              .map((sz): SkylineZone | null => {
                const zoneMatches = matches(sz.zone.name);
                if (blockMatches || floorMatches || zoneMatches) return sz;
                const rooms = sz.rooms.filter((r) => matches(r.roomNumber));
                return rooms.length > 0 ? { zone: sz.zone, rooms } : null;
              })
              .filter((z): z is SkylineZone => z !== null);
            if (blockMatches || floorMatches) return sf;
            return zones.length > 0 ? { floor: sf.floor, zones } : null;
          })
          .filter((f): f is SkylineFloor => f !== null);
        if (blockMatches) return sb;
        return floors.length > 0 ? { block: sb.block, floors } : null;
      })
      .filter((b): b is SkylineBlock => b !== null);
  });

  /** The "Floors" level view reuses the same Skyline component, zoomed to just the selected block —
   *  a single-building slice of the already-loaded (and already search-filtered) branch tree, so
   *  opening it needs no extra fetch. Empty array (not the whole branch's blocks) when nothing
   *  matches the current search, so the component's own empty state renders correctly. */
  protected readonly selectedBlockSkyline = computed<SkylineBlock[]>(() => {
    const id = this.selectedBlockId();
    if (id == null) return [];
    const match = this.filteredSkylineBlocks().find((sb) => sb.block.id === id);
    return match ? [match] : [];
  });

  /** The side panel owns its own create/update/status/hostel-assignment calls (see
   *  `campus-side-panel.component.ts`) — it just reports what changed so this component can
   *  refetch the affected list(s) and, for a new child, select it. */
  protected onPanelCreated(event: { level: CampusPanelLevel; id: number }): void {
    switch (event.level) {
      case 'organization':
        this.service.getOrganizations(false).subscribe({
          next: (organizations) => {
            this.organizations.set(organizations);
            this.selectOrganization(event.id);
          },
          error: () => this.toast.error('Failed to reload organizations'),
        });
        break;
      case 'branch':
        if (this.selectedOrganizationId() != null) this.loadBranches(this.selectedOrganizationId()!);
        this.selectBranch(event.id);
        break;
      case 'block':
        if (this.selectedBranchId() != null) {
          this.loadBlocks(this.selectedBranchId()!);
          this.loadSkyline(this.selectedBranchId()!);
        }
        this.selectBlock(event.id);
        break;
      case 'floor':
        if (this.selectedBlockId() != null) this.loadFloors(this.selectedBlockId()!);
        if (this.selectedBranchId() != null) this.loadSkyline(this.selectedBranchId()!);
        this.selectFloor(event.id);
        break;
      case 'zone':
        if (this.selectedFloorId() != null) this.loadZones(this.selectedFloorId()!);
        if (this.selectedBranchId() != null) this.loadSkyline(this.selectedBranchId()!);
        this.selectZone(event.id);
        break;
      case 'room':
        if (this.selectedZoneId() != null) this.loadRooms(this.selectedZoneId()!);
        if (this.selectedBranchId() != null) this.loadSkyline(this.selectedBranchId()!);
        this.selectRoom(event.id);
        break;
    }
  }

  protected onPanelSaved(event: { level: CampusPanelLevel }): void {
    this.clearEditing();
    switch (event.level) {
      case 'organization':
        this.service.getOrganizations(false).subscribe({
          next: (organizations) => this.organizations.set(organizations),
          error: () => this.toast.error('Failed to reload organizations'),
        });
        break;
      case 'branch':
        if (this.selectedOrganizationId() != null) this.loadBranches(this.selectedOrganizationId()!);
        break;
      case 'block':
        if (this.selectedBranchId() != null) {
          this.loadBlocks(this.selectedBranchId()!);
          this.loadSkyline(this.selectedBranchId()!);
        }
        break;
      case 'floor':
        if (this.selectedBlockId() != null) this.loadFloors(this.selectedBlockId()!);
        if (this.selectedBranchId() != null) this.loadSkyline(this.selectedBranchId()!);
        break;
      case 'zone':
        if (this.selectedFloorId() != null) this.loadZones(this.selectedFloorId()!);
        if (this.selectedBranchId() != null) this.loadSkyline(this.selectedBranchId()!);
        break;
      case 'room':
        if (this.selectedZoneId() != null) this.loadRooms(this.selectedZoneId()!);
        if (this.selectedBranchId() != null) this.loadSkyline(this.selectedBranchId()!);
        if (this.selectedRoomId() != null) this.loadHostelRoom(this.selectedRoomId()!);
        break;
    }
  }

  ngOnInit(): void {
    this.tourService.register('campus-setup', CAMPUS_SETUP_TOUR);
    this.tourService.registerFlowMap('campus-setup', CAMPUS_SETUP_FLOW_MAP);

    this.service.getOrganizations(false).subscribe({
      next: (organizations) => {
        this.organizations.set(organizations);
        this.loadingOrgs.set(false);
        this.initializeFromQueryParamsOrDefault(organizations);
      },
      error: () => {
        this.loadingOrgs.set(false);
        this.toast.error('Failed to load organizations');
      },
    });
  }

  /** Lets a "View Skyline" link elsewhere in the app (the Branch Diagram canvas, BR-60 Phase 1)
   *  land directly on a specific Block's zoomed floor-stack, instead of always defaulting to the
   *  first Organization's Branches-level view. */
  private initializeFromQueryParamsOrDefault(organizations: Organization[]): void {
    const branchId = Number(this.route.snapshot.queryParamMap.get('branchId')) || null;
    const blockId = Number(this.route.snapshot.queryParamMap.get('blockId')) || null;

    if (branchId) {
      this.service.getBranchById(branchId).subscribe({
        next: (branch) => {
          this.selectOrganization(branch.organizationId);
          this.selectBranch(branchId);
          if (blockId) this.selectBlock(blockId);
        },
        error: () => {
          if (organizations.length > 0) this.selectOrganization(organizations[0].id);
        },
      });
      return;
    }

    if (organizations.length > 0) this.selectOrganization(organizations[0].id);
  }

  /** New, additive entry point from the Skyline's floor rows (BR-60 Phase 1) — opens that Floor's
   *  spatial diagram in a separate screen rather than switching this page's own Grid view. */
  protected viewFloorDiagram(floorId: number): void {
    void this.router.navigate(['/floor-plans'], { queryParams: { floorId } });
  }

  /** Same additive pattern as `viewFloorDiagram`, one level down each (BR-60 DXF/PDF-derivation
   *  extension) — opens the Zone's or Room's own spatial diagram screen. */
  protected viewZoneDiagram(zoneId: number): void {
    void this.router.navigate(['/zone-diagrams'], { queryParams: { zoneId } });
  }

  protected viewRoomDiagram(roomId: number): void {
    void this.router.navigate(['/room-diagrams'], { queryParams: { roomId } });
  }

  /** Branch-level counterpart, only reachable via the side panel's "Import Floor Plan" button (no
   *  card-level "view diagram" affordance exists for Branches, unlike Floor/Zone/Room). */
  protected viewBranchDiagram(branchId: number): void {
    void this.router.navigate(['/branch-diagrams'], { queryParams: { branchId } });
  }

  /** The side panel's "Import Floor Plan" action (Branch/Floor/Zone/Room, never Block) — dispatches
   *  to the matching standalone diagram screen, preselected to the entity currently being edited. */
  protected onImportFloorPlan(event: { level: 'branch' | 'floor' | 'zone' | 'room'; id: number }): void {
    switch (event.level) {
      case 'branch':
        this.viewBranchDiagram(event.id);
        break;
      case 'floor':
        this.viewFloorDiagram(event.id);
        break;
      case 'zone':
        this.viewZoneDiagram(event.id);
        break;
      case 'room':
        this.viewRoomDiagram(event.id);
        break;
    }
  }

  protected selectOrganization(organizationId: number): void {
    this.selectedOrganizationId.set(organizationId);
    this.selectBranch(null);
    this.loadBranches(organizationId);
  }

  /** Shows an entity's properties in the side panel without touching the drill position — called
   *  from a card's edit pencil, never from clicking the rest of the card (that still navigates). */
  private startEditing(level: 'organization' | 'branch' | 'block' | 'floor' | 'zone', id: number): void {
    this.editingLevel.set(level);
    this.editingId.set(id);
  }

  private clearEditing(): void {
    this.editingLevel.set(null);
    this.editingId.set(null);
  }

  protected editOrganization(id: number): void {
    this.startEditing('organization', id);
  }

  /** Clears the current organization selection so the side panel's "no organization" branch (its
   *  `organizationId() == null` check) shows the Add Organization form — mirrors how every other
   *  level's "add" affordance is really just "nothing of that type is selected/being edited". */
  protected addOrganization(): void {
    this.clearEditing();
    this.selectedOrganizationId.set(null);
    this.selectBranch(null);
  }

  protected editBranch(id: number): void {
    this.startEditing('branch', id);
  }

  protected editBlock(id: number): void {
    this.startEditing('block', id);
  }

  protected editFloor(id: number): void {
    this.startEditing('floor', id);
  }

  protected editZone(id: number): void {
    this.startEditing('zone', id);
  }

  protected selectBranch(id: number | null): void {
    this.clearEditing();
    this.searchQuery.set('');
    this.selectedBranchId.set(id);
    this.selectBlock(null);
    this.blocks.set([]);
    this.skylineBlocks.set([]);
    if (id != null) {
      this.loadBlocks(id);
      this.loadSkyline(id);
    }
  }

  protected onSearchChange(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  protected clearSearch(): void {
    this.searchQuery.set('');
  }

  protected emptyLabelFor(defaultText: string): string {
    return this.searchQuery() ? `No matches for '${this.searchQuery()}'.` : defaultText;
  }

  protected selectBlock(id: number | null): void {
    this.clearEditing();
    this.searchQuery.set('');
    this.selectedBlockId.set(id);
    this.selectFloor(null);
    this.floors.set([]);
    if (id != null) this.loadFloors(id);
  }

  protected selectFloor(id: number | null): void {
    this.clearEditing();
    this.searchQuery.set('');
    this.selectedFloorId.set(id);
    this.selectZone(null);
    this.zones.set([]);
    if (id != null) this.loadZones(id);
  }

  protected selectZone(id: number | null): void {
    this.clearEditing();
    this.searchQuery.set('');
    this.selectedZoneId.set(id);
    this.selectRoom(null);
    this.rooms.set([]);
    if (id != null) this.loadRooms(id);
  }

  protected selectRoom(id: number | null): void {
    this.clearEditing();
    this.selectedRoomId.set(id);
    this.hostelRoom.set(null);
    if (id != null) this.loadHostelRoom(id);
  }

  /**
   * Skyline is used at two levels: the Branches-drilldown "Blocks" view (every Block in the branch,
   * each drawn as a building) and, once a Block is selected, a "Floors" view zoomed to that one
   * building (`selectedBlockSkyline`) — the same component and the same two outputs both times.
   * Selecting a Block from the multi-building view is a plain selection (drills into its zoomed
   * Skyline). Opening a Floor — from either the multi-building or the zoomed single-building view —
   * populates the ancestor chain from the already-loaded skyline tree (so the side panel's fields
   * load correctly without a re-fetch) and lands on the Grid view for that Floor's Zones.
   */
  protected selectBlockFromSkyline(sb: SkylineBlock): void {
    this.selectBlock(sb.block.id);
  }

  protected openFloorFromSkyline(sb: SkylineBlock, sf: SkylineFloor): void {
    this.clearEditing();
    this.selectedBlockId.set(sb.block.id);
    this.floors.set(sb.floors.map((f) => f.floor));
    this.selectedFloorId.set(sf.floor.id);
    this.zones.set(sf.zones.map((z) => z.zone));
    this.selectedZoneId.set(null);
    this.rooms.set([]);
    this.selectedRoomId.set(null);
    this.hostelRoom.set(null);
  }

  /** `CampusSkylineComponent` already mutated its own local copy for optimistic drag feedback (see
   *  its `localBlocks`/`onBlockDrop`) — this just persists that order and, on failure, refetches the
   *  skyline tree so the child's `localBlocks` effect re-syncs from the real (unreordered) data,
   *  snapping the optimistic drag back rather than leaving the UI showing an order that didn't save. */
  protected onBlocksReordered(orderedIds: number[]): void {
    const branchId = this.selectedBranchId();
    if (branchId == null) return;
    this.service.reorderBlocks(branchId, orderedIds).subscribe({
      next: () => this.loadSkyline(branchId),
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to reorder blocks');
        this.loadSkyline(branchId);
      },
    });
  }

  private loadBranches(organizationId: number): void {
    this.loadingBranches.set(true);
    this.service.getBranchesByOrganization(organizationId).subscribe({
      next: (branches) => {
        this.branches.set(branches);
        this.loadingBranches.set(false);
      },
      error: () => {
        this.loadingBranches.set(false);
        this.toast.error('Failed to load branches');
      },
    });
  }

  private loadBlocks(branchId: number): void {
    this.loadingBlocks.set(true);
    this.service.getBlocksByBranch(branchId).subscribe({
      next: (blocks) => {
        this.blocks.set(blocks);
        this.loadingBlocks.set(false);
      },
      error: () => {
        this.loadingBlocks.set(false);
        this.toast.error('Failed to load blocks');
      },
    });
  }

  private loadFloors(blockId: number): void {
    this.loadingFloors.set(true);
    this.service.getFloorsByBlock(blockId).subscribe({
      next: (floors) => {
        this.floors.set([...floors].sort((a, b) => a.floorNumber - b.floorNumber));
        this.loadingFloors.set(false);
      },
      error: () => {
        this.loadingFloors.set(false);
        this.toast.error('Failed to load floors');
      },
    });
  }

  private loadZones(floorId: number): void {
    this.loadingZones.set(true);
    this.service.getZonesByFloor(floorId).subscribe({
      next: (zones) => {
        this.zones.set(zones);
        this.loadingZones.set(false);
      },
      error: () => {
        this.loadingZones.set(false);
        this.toast.error('Failed to load zones');
      },
    });
  }

  private loadRooms(zoneId: number): void {
    this.loadingRooms.set(true);
    this.service.getRoomsByZone(zoneId).subscribe({
      next: (rooms) => {
        this.rooms.set(rooms);
        this.loadingRooms.set(false);
      },
      error: () => {
        this.loadingRooms.set(false);
        this.toast.error('Failed to load rooms');
      },
    });
  }

  private loadHostelRoom(roomId: number): void {
    this.service.getHostelRoom(roomId).subscribe({
      next: (hostelRoom) => this.hostelRoom.set(hostelRoom),
      error: () => this.hostelRoom.set(null),
    });
  }

  /** Builds the whole-branch skyline tree — every Block's Floors, every Floor's Zones, every
   *  Zone's Rooms — fetched in parallel at each level so the "view everything at once" skyline
   *  doesn't wait on a serial chain of requests. */
  private loadSkyline(branchId: number): void {
    this.loadingSkyline.set(true);
    this.service
      .getBlocksByBranch(branchId)
      .pipe(switchMap((blocks) => (blocks.length === 0 ? of([]) : forkJoin(blocks.map((b) => this.buildSkylineBlock(b))))))
      .subscribe({
        next: (skylineBlocks) => {
          this.skylineBlocks.set(skylineBlocks);
          this.loadingSkyline.set(false);
        },
        error: () => {
          this.loadingSkyline.set(false);
          this.toast.error('Failed to load skyline view');
        },
      });
  }

  private buildSkylineBlock(block: Block): Observable<SkylineBlock> {
    return this.service.getFloorsByBlock(block.id).pipe(
      switchMap((floors) => {
        const sorted = [...floors].sort((a, b) => a.floorNumber - b.floorNumber);
        if (sorted.length === 0) return of<SkylineFloor[]>([]);
        return forkJoin(sorted.map((f) => this.buildSkylineFloor(f)));
      }),
      map((floors) => ({ block, floors }))
    );
  }

  private buildSkylineFloor(floor: Floor): Observable<SkylineFloor> {
    return this.service.getZonesByFloor(floor.id).pipe(
      switchMap((zones) => (zones.length === 0 ? of<SkylineZone[]>([]) : forkJoin(zones.map((z) => this.buildSkylineZone(z))))),
      map((zones) => ({ floor, zones }))
    );
  }

  private buildSkylineZone(zone: Zone): Observable<SkylineZone> {
    return this.service.getRoomsByZone(zone.id).pipe(map((rooms) => ({ zone, rooms })));
  }
}
