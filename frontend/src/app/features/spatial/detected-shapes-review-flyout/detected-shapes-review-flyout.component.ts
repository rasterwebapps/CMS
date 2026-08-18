import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, switchMap } from 'rxjs';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { SpatialService } from '../spatial.service';
import { DetectedShapeCandidate, DiagramLevel, PolygonGeometry, SpatialEquipmentSummary, SpatialInventoryItemSummary, VirtualLocation } from '../spatial.model';
import { CampusInfrastructureService } from '../../hostel/campus-infrastructure/campus-infrastructure.service';
import { GenderRestriction } from '../../hostel/campus-infrastructure/campus-infrastructure.model';
import { RoomPurposeCategoryService } from '../../hostel/room-purpose-category/room-purpose-category.service';
import { RoomPurposeCategory } from '../../hostel/room-purpose-category/room-purpose-category.model';
import { RoomSubTypeService } from '../../hostel/room-sub-type/room-sub-type.service';
import { RoomSubType } from '../../hostel/room-sub-type/room-sub-type.model';
import { ToastService } from '../../../core/toast/toast.service';

/** What a confirmed row at each diagram level actually becomes — matches BR-60's extension:
 *  BRANCH derives Blocks, FLOOR derives Zones, ZONE derives Rooms; ROOM has nothing left to
 *  create underneath it (a Room is a leaf), so it links an existing Equipment/InventoryItem
 *  catalog row instead. */
type EquipmentLinkKind = 'EQUIPMENT' | 'INVENTORY_ITEM';

interface ReviewRow {
  candidate: DetectedShapeCandidate;
  included: boolean;
  name: string;
  // BRANCH level → Block fields
  code: string;
  isHostel: boolean;
  genderRestriction: GenderRestriction | null;
  // ZONE level → Room fields (purposeCategoryId/subTypeId required by RoomRequest)
  purposeCategoryId: number | null;
  subTypeId: number | null;
  subTypes: RoomSubType[];
  // ROOM level → existing Equipment/InventoryItem link (null linkKind = skip this row)
  linkKind: EquipmentLinkKind | null;
  linkedEntityId: number | null;
  submitting: boolean;
  confirmed: boolean;
  error: string | null;
}

const codeSuggestionFrom = (name: string): string =>
  name.toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 20);

/**
 * Staged review for candidate sub-components detected from a DXF/PDF import (BR-60 extension) —
 * nothing here is ever auto-committed. The admin edits/confirms each row before it becomes a real
 * Block/Zone/Room row (or an Equipment/InventoryItem link) plus a placed VirtualLocation marker,
 * using the exact same create endpoints the manual Campus Setup builder and manual marker
 * placement already use. Rejecting a row (unchecking it, or just never confirming) makes no calls
 * at all.
 */
@Component({
  selector: 'app-detected-shapes-review-flyout',
  standalone: true,
  imports: [FormsModule, CmsFlyoutPanelComponent],
  templateUrl: './detected-shapes-review-flyout.component.html',
  styleUrl: './detected-shapes-review-flyout.component.scss',
})
export class DetectedShapesReviewFlyoutComponent implements OnInit {
  @Input({ required: true }) floorPlanId!: number;
  @Input({ required: true }) diagramLevel!: DiagramLevel;
  /** The Branch/Floor/Zone/Room id this diagram belongs to (`FloorPlan.entityId`) — exactly the
   *  parent path parameter the corresponding create endpoint needs. Unused at ROOM level, since
   *  nothing is created there, only linked. */
  @Input({ required: true }) parentEntityId!: number;
  @Input({ required: true }) detectedShapes: DetectedShapeCandidate[] = [];

  @Output() readonly closed = new EventEmitter<void>();

  private readonly spatialService = inject(SpatialService);
  private readonly campusService = inject(CampusInfrastructureService);
  private readonly categoryService = inject(RoomPurposeCategoryService);
  private readonly subTypeService = inject(RoomSubTypeService);
  private readonly toast = inject(ToastService);

  protected readonly rows = signal<ReviewRow[]>([]);
  protected readonly confirmingAll = signal(false);

  protected readonly purposeCategories = signal<RoomPurposeCategory[]>([]);
  protected readonly equipmentList = signal<SpatialEquipmentSummary[]>([]);
  protected readonly inventoryList = signal<SpatialInventoryItemSummary[]>([]);

  ngOnInit(): void {
    this.rows.set(this.detectedShapes.map((candidate) => ({
      candidate,
      included: true,
      name: candidate.name ?? '',
      code: candidate.name ? codeSuggestionFrom(candidate.name) : '',
      isHostel: false,
      genderRestriction: null,
      purposeCategoryId: null,
      subTypeId: null,
      subTypes: [],
      linkKind: null,
      linkedEntityId: null,
      submitting: false,
      confirmed: false,
      error: null,
    })));

    if (this.diagramLevel === 'ZONE') {
      this.categoryService.getAll(true).subscribe({ next: (categories) => this.purposeCategories.set(categories) });
    } else if (this.diagramLevel === 'ROOM') {
      this.spatialService.getEquipmentSummaries().subscribe({ next: (list) => this.equipmentList.set(list) });
      this.spatialService.getInventoryItemSummaries().subscribe({ next: (list) => this.inventoryList.set(list) });
    }
  }

  protected onNameChange(row: ReviewRow, value: string): void {
    row.name = value;
    if (this.diagramLevel === 'BRANCH' && !row.code.trim()) row.code = codeSuggestionFrom(value);
    this.rows.set([...this.rows()]);
  }

  protected onPurposeCategoryChange(row: ReviewRow, categoryId: number | null): void {
    row.purposeCategoryId = categoryId;
    row.subTypeId = null;
    row.subTypes = [];
    this.rows.set([...this.rows()]);
    if (categoryId) {
      this.subTypeService.getAll(categoryId, true).subscribe({
        next: (subTypes) => {
          row.subTypes = subTypes;
          this.rows.set([...this.rows()]);
        },
      });
    }
  }

  protected onLinkKindChange(row: ReviewRow, kind: EquipmentLinkKind | null): void {
    row.linkKind = kind;
    row.linkedEntityId = null;
    this.rows.set([...this.rows()]);
  }

  protected canConfirm(row: ReviewRow): boolean {
    if (!row.included || row.confirmed || row.submitting) return false;
    if (!row.name.trim()) return false;
    if (this.diagramLevel === 'BRANCH') return !!row.code.trim();
    if (this.diagramLevel === 'ZONE') return row.purposeCategoryId != null && row.subTypeId != null;
    if (this.diagramLevel === 'ROOM') return row.linkKind != null && row.linkedEntityId != null;
    return true; // FLOOR → Zone only needs a name
  }

  protected confirmRow(row: ReviewRow): void {
    if (!this.canConfirm(row)) return;
    row.submitting = true;
    row.error = null;
    this.rows.set([...this.rows()]);
    this.createEntityAndMarker(row).subscribe({
      next: () => {
        row.submitting = false;
        row.confirmed = true;
        this.rows.set([...this.rows()]);
      },
      error: (err) => {
        row.submitting = false;
        row.error = err?.error?.message ?? 'Failed to confirm this row';
        this.rows.set([...this.rows()]);
      },
    });
  }

  protected confirmAll(): void {
    if (this.confirmingAll()) return;
    this.confirmingAll.set(true);
    this.confirmNext(this.rows().filter((r) => this.canConfirm(r)), 0);
  }

  private confirmNext(queue: ReviewRow[], index: number): void {
    if (index >= queue.length) {
      this.confirmingAll.set(false);
      const failed = queue.filter((r) => r.error).length;
      if (failed === 0) {
        this.toast.success(`Confirmed ${queue.length} of ${queue.length} row${queue.length === 1 ? '' : 's'}`);
      } else {
        this.toast.error(`Confirmed ${queue.length - failed} of ${queue.length} rows — ${failed} failed, see row errors below`);
      }
      return;
    }
    const row = queue[index];
    row.submitting = true;
    row.error = null;
    this.rows.set([...this.rows()]);
    this.createEntityAndMarker(row).subscribe({
      next: () => {
        row.submitting = false;
        row.confirmed = true;
        this.rows.set([...this.rows()]);
        this.confirmNext(queue, index + 1);
      },
      error: (err) => {
        row.submitting = false;
        row.error = err?.error?.message ?? 'Failed to confirm this row';
        this.rows.set([...this.rows()]);
        this.confirmNext(queue, index + 1);
      },
    });
  }

  /** Creates the real Block/Zone/Room row (skipped at ROOM level, which only links an existing
   *  Equipment/InventoryItem), then places a VirtualLocation marker pointing at it — the same two
   *  calls the manual Campus Setup + manual marker flows already make, just chained together. */
  private createEntityAndMarker(row: ReviewRow): Observable<VirtualLocation> {
    const geometryJson: PolygonGeometry = { points: row.candidate.points };
    const name = row.name.trim();

    switch (this.diagramLevel) {
      case 'BRANCH':
        return this.campusService.createBlock(this.parentEntityId, {
          name,
          code: row.code.trim(),
          isHostel: row.isHostel,
          genderRestriction: row.genderRestriction,
        }).pipe(switchMap((block) => this.createMarker('BLOCK', block.id, geometryJson, name)));
      case 'FLOOR':
        return this.campusService.createZone(this.parentEntityId, { name })
          .pipe(switchMap((zone) => this.createMarker('ZONE', zone.id, geometryJson, name)));
      case 'ZONE':
        return this.campusService.createRoom(this.parentEntityId, {
          roomNumber: name,
          purposeCategoryId: row.purposeCategoryId,
          subTypeId: row.subTypeId,
        }).pipe(switchMap((room) => this.createMarker('ROOM', room.id, geometryJson, name)));
      case 'ROOM':
        return this.createMarker(row.linkKind!, row.linkedEntityId!, geometryJson, name);
    }
  }

  private createMarker(entityType: string, entityId: number, geometryJson: PolygonGeometry, name: string): Observable<VirtualLocation> {
    return this.spatialService.createVirtualLocation({
      floorPlanId: this.floorPlanId,
      entityType,
      entityId,
      name,
      locationType: entityType,
      shapeType: 'POLYGON',
      geometryJson: JSON.stringify(geometryJson),
    });
  }

  protected onExcludeToggle(row: ReviewRow, included: boolean): void {
    row.included = included;
    this.rows.set([...this.rows()]);
  }

  protected onDone(): void {
    this.closed.emit();
  }

  protected confirmedCount(): number {
    return this.rows().filter((r) => r.confirmed).length;
  }
}
