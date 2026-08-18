import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { SpatialService } from '../spatial.service';
import {
  DiagramLevel,
  LINK_TYPES_BY_LEVEL,
  LINK_TYPE_LABELS,
  PointGeometry,
  RectangleGeometry,
  PolygonGeometry,
  ShapeType,
  SpatialEquipmentSummary,
  SpatialInventoryItemSummary,
  SpatialLinkType,
  VirtualLocation,
  VirtualLocationRequest,
  VirtualLocationStatus,
} from '../spatial.model';
import { CampusInfrastructureService } from '../../hostel/campus-infrastructure/campus-infrastructure.service';
import { Block, Room, Zone } from '../../hostel/campus-infrastructure/campus-infrastructure.model';
import { ToastService } from '../../../core/toast/toast.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

const LOCATION_TYPE_SUGGESTIONS = ['BED', 'WORKSTATION', 'DESK', 'EQUIPMENT', 'ZONE', 'GEOFENCE'];

interface LinkOption { id: number; label: string; }

@Component({
  selector: 'app-virtual-location-form-flyout',
  standalone: true,
  imports: [FormsModule, CmsFlyoutPanelComponent],
  templateUrl: './virtual-location-form-flyout.component.html',
  styleUrl: './virtual-location-form-flyout.component.scss',
})
export class VirtualLocationFormFlyoutComponent implements OnInit {
  @Input({ required: true }) floorPlanId!: number;
  /** null = create mode; a VirtualLocation = edit mode. */
  @Input() location: VirtualLocation | null = null;
  /** Only used in create mode — the canvas click that started this placement. */
  @Input() pendingPoint: { x: number; y: number } | null = null;
  /** Which entity kinds can be linked — BRANCH diagrams offer Block; FLOOR diagrams offer
   *  Zone/Room/Equipment/InventoryItem. See spatial.model.ts's LINK_TYPES_BY_LEVEL. */
  @Input() diagramLevel: DiagramLevel = 'FLOOR';
  /** The Branch or Floor id this diagram belongs to — used to fetch picker lists. */
  @Input({ required: true }) diagramEntityId!: number;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly saved = new EventEmitter<void>();

  private readonly spatialService = inject(SpatialService);
  private readonly campusService = inject(CampusInfrastructureService);
  private readonly toast = inject(ToastService);

  protected readonly locationTypeSuggestions = LOCATION_TYPE_SUGGESTIONS;
  protected readonly linkTypeLabels = LINK_TYPE_LABELS;
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);

  protected readonly availableLinkTypes = computed(() => LINK_TYPES_BY_LEVEL[this.diagramLevel]);

  protected name = '';
  protected locationType = '';
  protected moduleTag = '';
  protected shapeType: ShapeType = 'POINT';
  protected capacity: number | null = null;
  protected status: VirtualLocationStatus = 'ACTIVE';
  protected description = '';

  protected linkType: SpatialLinkType | null = null;
  protected linkedEntityId: number | null = null;
  protected readonly linkOptions = signal<LinkOption[]>([]);
  protected readonly loadingLinkOptions = signal(false);
  protected linkSearch = '';
  protected readonly filteredLinkOptions = computed(() => {
    const search = this.linkSearch.trim().toLowerCase();
    const options = this.linkOptions();
    return search ? options.filter((o) => o.label.toLowerCase().includes(search)) : options;
  });

  /** Live-fetched read-only context shown when editing a marker already linked to an
   *  Equipment/InventoryItem row — confirms the link points at the right thing. */
  protected readonly liveEquipment = signal<SpatialEquipmentSummary | null>(null);
  protected readonly liveInventoryItem = signal<SpatialInventoryItemSummary | null>(null);

  protected pointX = 0;
  protected pointY = 0;
  protected rectX = 0;
  protected rectY = 0;
  protected rectWidth = 10;
  protected rectHeight = 10;
  protected polygonPoints: { x: number; y: number }[] = [];

  private blocks: Block[] = [];
  private zones: Zone[] = [];
  private roomsList: Room[] = [];
  private equipmentList: SpatialEquipmentSummary[] = [];
  private inventoryList: SpatialInventoryItemSummary[] = [];

  ngOnInit(): void {
    if (this.location) {
      this.isEditMode.set(true);
      const loc = this.location;
      this.name = loc.name;
      this.locationType = loc.locationType;
      this.moduleTag = loc.moduleTag ?? '';
      this.shapeType = loc.shapeType;
      this.capacity = loc.capacity;
      this.status = loc.status;
      this.description = loc.description ?? '';
      this.loadGeometry(loc.shapeType, loc.geometryJson);

      if (loc.entityType && loc.entityId != null && this.availableLinkTypes().includes(loc.entityType as SpatialLinkType)) {
        this.linkType = loc.entityType as SpatialLinkType;
        this.linkedEntityId = loc.entityId;
        this.loadLiveContext(this.linkType, loc.entityId);
      }
    } else if (this.pendingPoint) {
      this.pointX = round2(this.pendingPoint.x);
      this.pointY = round2(this.pendingPoint.y);
      this.rectX = round2(this.pendingPoint.x);
      this.rectY = round2(this.pendingPoint.y);
      this.polygonPoints = [
        { x: round2(this.pendingPoint.x), y: round2(this.pendingPoint.y) },
        { x: round2(this.pendingPoint.x) + 20, y: round2(this.pendingPoint.y) },
        { x: round2(this.pendingPoint.x), y: round2(this.pendingPoint.y) + 20 },
      ];
    } else {
      this.polygonPoints = [{ x: 0, y: 0 }, { x: 20, y: 0 }, { x: 0, y: 20 }];
    }

    this.loadPickerLists();
  }

  protected onLinkTypeChange(): void {
    this.linkedEntityId = null;
    this.linkSearch = '';
    this.liveEquipment.set(null);
    this.liveInventoryItem.set(null);
    this.linkOptions.set(this.optionsForLinkType(this.linkType));
  }

  protected onLinkedEntityChange(): void {
    if (this.linkedEntityId == null || !this.linkType) return;
    if (!this.name.trim()) {
      const option = this.linkOptions().find((o) => o.id === this.linkedEntityId);
      if (option) this.name = option.label;
    }
    this.loadLiveContext(this.linkType, this.linkedEntityId);
  }

  protected addPolygonPoint(): void {
    const last = this.polygonPoints[this.polygonPoints.length - 1];
    this.polygonPoints = [...this.polygonPoints, { x: (last?.x ?? 0) + 10, y: (last?.y ?? 0) + 10 }];
  }

  protected removePolygonPoint(index: number): void {
    if (this.polygonPoints.length <= 3) return;
    this.polygonPoints = this.polygonPoints.filter((_, i) => i !== index);
  }

  protected canSubmit(): boolean {
    if (!this.name.trim() || !this.locationType.trim()) return false;
    if (this.shapeType === 'POLYGON' && this.polygonPoints.length < 3) return false;
    if (this.linkType && this.linkedEntityId == null) return false;
    return true;
  }

  protected onClose(): void {
    this.closed.emit();
  }

  protected onSubmit(): void {
    if (!this.canSubmit() || this.saving()) return;
    this.saving.set(true);

    const geometryJson = this.buildGeometryJson();
    const request: VirtualLocationRequest = {
      floorPlanId: this.floorPlanId,
      entityType: this.linkType,
      entityId: this.linkType ? this.linkedEntityId : null,
      name: this.name.trim(),
      locationType: this.locationType.trim(),
      moduleTag: this.moduleTag.trim() || null,
      shapeType: this.shapeType,
      geometryJson,
      capacity: this.capacity,
      status: this.status,
      description: this.description.trim() || null,
    };

    const op$ = this.isEditMode()
      ? this.spatialService.updateVirtualLocation(this.location!.id, request)
      : this.spatialService.createVirtualLocation(request);

    op$.subscribe({
      next: () => {
        this.toast.success(this.isEditMode() ? 'Virtual location updated successfully' : 'Virtual location placed successfully');
        this.saving.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to save virtual location');
        this.saving.set(false);
      },
    });
  }

  private loadPickerLists(): void {
    this.loadingLinkOptions.set(true);

    if (this.diagramLevel === 'BRANCH') {
      this.campusService.getBlocksByBranch(this.diagramEntityId).subscribe({
        next: (blocks) => {
          this.blocks = blocks;
          if (this.linkType === 'BLOCK') this.linkOptions.set(this.optionsForLinkType('BLOCK'));
          this.loadingLinkOptions.set(false);
        },
        error: () => this.loadingLinkOptions.set(false),
      });
      return;
    }

    forkJoin({
      zones: this.campusService.getZonesByFloor(this.diagramEntityId, true).pipe(catchError(() => of<Zone[]>([]))),
      equipment: this.spatialService.getEquipmentSummaries().pipe(catchError(() => of<SpatialEquipmentSummary[]>([]))),
      inventory: this.spatialService.getInventoryItemSummaries().pipe(catchError(() => of<SpatialInventoryItemSummary[]>([]))),
    }).subscribe({
      next: ({ zones, equipment, inventory }) => {
        this.zones = zones;
        this.equipmentList = equipment;
        this.inventoryList = inventory;

        if (zones.length === 0) {
          this.finishRoomLoad([]);
          return;
        }
        forkJoin(zones.map((z) => this.campusService.getRoomsByZone(z.id, true).pipe(catchError(() => of<Room[]>([])))))
          .subscribe({ next: (roomLists) => this.finishRoomLoad(roomLists.flat()) });
      },
      error: () => this.loadingLinkOptions.set(false),
    });
  }

  private finishRoomLoad(rooms: Room[]): void {
    this.roomsList = rooms;
    if (this.linkType) this.linkOptions.set(this.optionsForLinkType(this.linkType));
    this.loadingLinkOptions.set(false);
  }

  private optionsForLinkType(linkType: SpatialLinkType | null): LinkOption[] {
    switch (linkType) {
      case 'BLOCK': return this.blocks.map((b) => ({ id: b.id, label: b.name }));
      case 'ZONE': return this.zones.map((z) => ({ id: z.id, label: z.name }));
      case 'ROOM': return this.roomsList.map((r) => ({ id: r.id, label: r.roomNumber }));
      case 'EQUIPMENT': return this.equipmentList.map((e) => ({ id: e.id, label: `${e.name} (${e.assetCode})` }));
      case 'INVENTORY_ITEM': return this.inventoryList.map((i) => ({ id: i.id, label: `${i.name} (${i.itemCode})` }));
      default: return [];
    }
  }

  private loadLiveContext(linkType: SpatialLinkType, entityId: number): void {
    this.liveEquipment.set(null);
    this.liveInventoryItem.set(null);
    if (linkType === 'EQUIPMENT') {
      this.spatialService.getEquipmentSummaryById(entityId).subscribe({ next: (e) => this.liveEquipment.set(e) });
    } else if (linkType === 'INVENTORY_ITEM') {
      this.spatialService.getInventoryItemSummaryById(entityId).subscribe({ next: (i) => this.liveInventoryItem.set(i) });
    }
  }

  private loadGeometry(shapeType: ShapeType, geometryJson: string): void {
    try {
      const parsed = JSON.parse(geometryJson);
      if (shapeType === 'POINT') {
        const g = parsed as PointGeometry;
        this.pointX = g.x;
        this.pointY = g.y;
      } else if (shapeType === 'RECTANGLE') {
        const g = parsed as RectangleGeometry;
        this.rectX = g.x;
        this.rectY = g.y;
        this.rectWidth = g.width;
        this.rectHeight = g.height;
      } else {
        const g = parsed as PolygonGeometry;
        this.polygonPoints = g.points.length >= 3 ? g.points : [{ x: 0, y: 0 }, { x: 20, y: 0 }, { x: 0, y: 20 }];
      }
    } catch {
      // Shouldn't happen — backend validates geometryJson before persisting.
    }
  }

  private buildGeometryJson(): string {
    if (this.shapeType === 'POINT') {
      const g: PointGeometry = { x: this.pointX, y: this.pointY };
      return JSON.stringify(g);
    }
    if (this.shapeType === 'RECTANGLE') {
      const g: RectangleGeometry = { x: this.rectX, y: this.rectY, width: this.rectWidth, height: this.rectHeight };
      return JSON.stringify(g);
    }
    const g: PolygonGeometry = { points: this.polygonPoints };
    return JSON.stringify(g);
  }
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}
