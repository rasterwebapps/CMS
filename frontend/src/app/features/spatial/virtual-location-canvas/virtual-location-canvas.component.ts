import { Component, ElementRef, OnDestroy, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { SpatialService } from '../spatial.service';
import {
  DiagramLevel,
  FloorPlan,
  PointGeometry,
  PolygonGeometry,
  RectangleGeometry,
  SpatialEquipmentSummary,
  SpatialInventoryItemSummary,
  VirtualLocation,
} from '../spatial.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconDeleteComponent } from '../../../shared/icons';
import { VirtualLocationFormFlyoutComponent } from '../virtual-location-form-flyout/virtual-location-form-flyout.component';

interface CanvasPoint { x: number; y: number; }

@Component({
  selector: 'app-virtual-location-canvas',
  standalone: true,
  imports: [
    RouterLink,
    MatDialogModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconDeleteComponent,
    VirtualLocationFormFlyoutComponent,
  ],
  templateUrl: './virtual-location-canvas.component.html',
  styleUrl: './virtual-location-canvas.component.scss',
})
export class VirtualLocationCanvasComponent implements OnInit, OnDestroy {
  @ViewChild('canvasSvg') canvasSvgRef?: ElementRef<SVGSVGElement>;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly spatialService = inject(SpatialService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);

  protected readonly canManage = computed(() => this.permissionService.has('SPATIAL_VIRTUAL_LOCATION_MANAGE'));

  protected readonly loading = signal(true);
  protected readonly floorPlan = signal<FloorPlan | null>(null);
  protected readonly imageUrl = signal<string | null>(null);
  protected readonly naturalWidth = signal(0);
  protected readonly naturalHeight = signal(0);
  protected readonly locations = signal<VirtualLocation[]>([]);

/** 'BRANCH' diagrams show Block markers; 'FLOOR' diagrams show Zone/Room/Equipment/InventoryItem
   *  markers; 'ZONE' diagrams show Room markers; 'ROOM' diagrams show Equipment/InventoryItem
   *  markers. Derived from the loaded FloorPlan's own entityType, not a route param. */
  protected readonly diagramLevel = computed<DiagramLevel>(() => {
    switch (this.floorPlan()?.entityType) {
      case 'BRANCH': return 'BRANCH';
      case 'ZONE': return 'ZONE';
      case 'ROOM': return 'ROOM';
      default: return 'FLOOR';
    }
  });
  protected readonly listRoute = computed(() => {
    switch (this.diagramLevel()) {
      case 'BRANCH': return '/branch-diagrams';
      case 'ZONE': return '/zone-diagrams';
      case 'ROOM': return '/room-diagrams';
      default: return '/floor-plans';
    }
  });

  /** Live status/quantity for markers linked to Equipment/InventoryItem, keyed by VirtualLocation.id. */
  protected readonly equipmentByLocation = signal<Map<number, SpatialEquipmentSummary>>(new Map());
  protected readonly inventoryByLocation = signal<Map<number, SpatialInventoryItemSummary>>(new Map());

  protected readonly showFormFlyout = signal(false);
  protected editingLocation: VirtualLocation | null = null;
  protected pendingPoint: CanvasPoint | null = null;

  protected floorPlanId!: number;
  private objectUrl: string | null = null;

  ngOnInit(): void {
    this.floorPlanId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  ngOnDestroy(): void {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
  }

  protected onCanvasClick(event: MouseEvent): void {
    if (!this.canManage()) return;
    const plan = this.floorPlan();
    if (!plan) return;
    if (!plan.isCalibrated) {
      this.toast.error('Calibrate this floor plan before placing virtual locations');
      return;
    }
    const svg = this.canvasSvgRef?.nativeElement;
    if (!svg) return;
    const point = svg.createSVGPoint();
    point.x = event.clientX;
    point.y = event.clientY;
    const ctm = svg.getScreenCTM();
    if (!ctm) return;
    const svgPoint = point.matrixTransform(ctm.inverse());

    this.editingLocation = null;
    this.pendingPoint = { x: svgPoint.x, y: svgPoint.y };
    this.showFormFlyout.set(true);
  }

  protected selectLocation(loc: VirtualLocation, event: Event): void {
    event.stopPropagation();
    if (!this.canManage()) return;
    this.editingLocation = loc;
    this.pendingPoint = null;
    this.showFormFlyout.set(true);
  }

  protected addFromToolbar(): void {
    if (!this.canManage()) return;
    const plan = this.floorPlan();
    if (!plan?.isCalibrated) {
      this.toast.error('Calibrate this floor plan before placing virtual locations');
      return;
    }
    this.editingLocation = null;
    this.pendingPoint = { x: this.naturalWidth() / 2, y: this.naturalHeight() / 2 };
    this.showFormFlyout.set(true);
  }

  protected closeFormFlyout(): void {
    this.showFormFlyout.set(false);
    this.editingLocation = null;
    this.pendingPoint = null;
  }

  protected onLocationSaved(): void {
    this.closeFormFlyout();
    this.loadLocations();
  }

  protected deleteLocation(loc: VirtualLocation, event: Event): void {
    event.stopPropagation();
    if (!this.canManage()) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Virtual Location',
        message: `Delete "${loc.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(loc);
    });
  }

  /** Block-linked markers on a Branch diagram jump straight into that Block's existing Skyline
   *  view inside Campus Setup — Blocks never get their own diagram, see BR-60. */
  protected viewSkyline(loc: VirtualLocation, event: Event): void {
    event.stopPropagation();
    const plan = this.floorPlan();
    if (!plan || loc.entityType !== 'BLOCK' || loc.entityId == null) return;
    void this.router.navigate(['/campus-infrastructure'], {
      queryParams: { branchId: plan.entityId, blockId: loc.entityId },
    });
  }

  protected pointGeometry(loc: VirtualLocation): PointGeometry | null {
    return loc.shapeType === 'POINT' ? this.safeParse<PointGeometry>(loc.geometryJson) : null;
  }

  protected rectangleGeometry(loc: VirtualLocation): RectangleGeometry | null {
    return loc.shapeType === 'RECTANGLE' ? this.safeParse<RectangleGeometry>(loc.geometryJson) : null;
  }

  protected polygonPointsAttr(loc: VirtualLocation): string {
    if (loc.shapeType !== 'POLYGON') return '';
    const geom = this.safeParse<PolygonGeometry>(loc.geometryJson);
    return geom ? geom.points.map((p) => `${p.x},${p.y}`).join(' ') : '';
  }

  protected labelAnchor(loc: VirtualLocation): CanvasPoint {
    if (loc.shapeType === 'POINT') {
      const g = this.pointGeometry(loc);
      return g ? { x: g.x, y: g.y - 14 } : { x: 0, y: 0 };
    }
    if (loc.shapeType === 'RECTANGLE') {
      const g = this.rectangleGeometry(loc);
      return g ? { x: g.x, y: g.y - 6 } : { x: 0, y: 0 };
    }
    const geom = this.safeParse<PolygonGeometry>(loc.geometryJson);
    if (!geom || geom.points.length === 0) return { x: 0, y: 0 };
    const first = geom.points[0];
    return { x: first.x, y: first.y - 6 };
  }

  protected statusDotAnchor(loc: VirtualLocation): CanvasPoint {
    const anchor = this.labelAnchor(loc);
    return { x: anchor.x - 8, y: anchor.y - 3 };
  }

  /** Equipment: color by status. InventoryItem: red when low stock, green otherwise. */
  protected statusDotColorClass(loc: VirtualLocation): string {
    const equipment = this.equipmentByLocation().get(loc.id);
    if (equipment) {
      switch (equipment.status) {
        case 'AVAILABLE': return 'sp-dot--green';
        case 'IN_USE': return 'sp-dot--blue';
        case 'UNDER_MAINTENANCE': return 'sp-dot--amber';
        case 'OUT_OF_ORDER': case 'DISPOSED': return 'sp-dot--red';
        default: return 'sp-dot--gray';
      }
    }
    const item = this.inventoryByLocation().get(loc.id);
    if (item) return item.lowStock ? 'sp-dot--red' : 'sp-dot--green';
    return '';
  }

  protected hasStatusDot(loc: VirtualLocation): boolean {
    return this.equipmentByLocation().has(loc.id) || this.inventoryByLocation().has(loc.id);
  }

  private performDelete(loc: VirtualLocation): void {
    this.spatialService.deleteVirtualLocation(loc.id).subscribe({
      next: () => {
        this.toast.success('Virtual location deleted successfully');
        this.loadLocations();
      },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete virtual location'),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.spatialService.getFloorPlanById(this.floorPlanId).subscribe({
      next: (plan) => {
        this.floorPlan.set(plan);
        this.loadImage(plan);
        this.loadLocations();
      },
      error: () => {
        this.toast.error('Failed to load floor plan');
        this.loading.set(false);
        void this.router.navigate(['/floor-plans']);
      },
    });
  }

  private loadImage(plan: FloorPlan): void {
    this.spatialService.downloadFloorPlanFile(plan.id).subscribe({
      next: (response) => {
        this.objectUrl = URL.createObjectURL(response.body!);
        const probe = new Image();
        probe.onload = () => {
          this.naturalWidth.set(probe.naturalWidth);
          this.naturalHeight.set(probe.naturalHeight);
          this.imageUrl.set(this.objectUrl);
          this.loading.set(false);
        };
        probe.src = this.objectUrl;
      },
      error: () => {
        this.toast.error('Failed to load the floor plan image');
        this.loading.set(false);
      },
    });
  }

  private loadLocations(): void {
    this.spatialService.getVirtualLocationsByFloorPlan(this.floorPlanId).subscribe({
      next: (locs) => {
        this.locations.set(locs);
        this.loadStatusBadges(locs);
      },
      error: () => this.toast.error('Failed to load virtual locations'),
    });
  }

  private loadStatusBadges(locs: VirtualLocation[]): void {
    const equipmentMap = new Map<number, SpatialEquipmentSummary>();
    const inventoryMap = new Map<number, SpatialInventoryItemSummary>();

    for (const loc of locs) {
      if (loc.entityType === 'EQUIPMENT' && loc.entityId != null) {
        this.spatialService.getEquipmentSummaryById(loc.entityId).subscribe({
          next: (eq) => {
            equipmentMap.set(loc.id, eq);
            this.equipmentByLocation.set(new Map(equipmentMap));
          },
        });
      } else if (loc.entityType === 'INVENTORY_ITEM' && loc.entityId != null) {
        this.spatialService.getInventoryItemSummaryById(loc.entityId).subscribe({
          next: (item) => {
            inventoryMap.set(loc.id, item);
            this.inventoryByLocation.set(new Map(inventoryMap));
          },
        });
      }
    }
  }

  private safeParse<T>(json: string): T | null {
    try {
      return JSON.parse(json) as T;
    } catch {
      return null;
    }
  }
}
