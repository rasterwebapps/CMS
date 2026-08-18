export type UnitSystem = 'METERS' | 'FEET';
export type OriginAnchor = 'TOP_LEFT' | 'BOTTOM_LEFT' | 'CENTER';
export type ShapeType = 'POINT' | 'RECTANGLE' | 'POLYGON';
export type VirtualLocationStatus = 'ACTIVE' | 'INACTIVE';

/** Which level of the physical hierarchy a FloorPlan diagram represents. A Branch-level diagram
 *  shows Blocks; a Floor-level diagram shows Zones/Rooms/Equipment/Inventory Items; a Zone-level
 *  diagram shows Rooms; a Room-level diagram shows Equipment/Inventory Items. Block itself
 *  deliberately has no diagram — it stays on the existing Campus Setup Skyline view. */
export type DiagramLevel = 'BRANCH' | 'FLOOR' | 'ZONE' | 'ROOM';

/** The entity kinds a VirtualLocation marker can link to, keyed to VirtualLocation.entityType.
 *  Which of these are offered depends on the diagram's level (see DiagramLevel). */
export type SpatialLinkType = 'BLOCK' | 'ZONE' | 'ROOM' | 'EQUIPMENT' | 'INVENTORY_ITEM';

export const LINK_TYPES_BY_LEVEL: Record<DiagramLevel, SpatialLinkType[]> = {
  BRANCH: ['BLOCK'],
  FLOOR: ['ZONE', 'ROOM', 'EQUIPMENT', 'INVENTORY_ITEM'],
  ZONE: ['ROOM'],
  ROOM: ['EQUIPMENT', 'INVENTORY_ITEM'],
};

export const LINK_TYPE_LABELS: Record<SpatialLinkType, string> = {
  BLOCK: 'Block',
  ZONE: 'Zone',
  ROOM: 'Room',
  EQUIPMENT: 'Equipment',
  INVENTORY_ITEM: 'Inventory Item',
};

export interface FloorPlan {
  id: number;
  entityType: string;
  entityId: number;
  name: string;
  originalFileName: string | null;
  originalContentType: string | null;
  unitSystem: UnitSystem;
  originAnchor: OriginAnchor;
  originX: number;
  originY: number;
  viewboxWidth: number | null;
  viewboxHeight: number | null;
  scaleFactor: number | null;
  calibrationPoint1X: number | null;
  calibrationPoint1Y: number | null;
  calibrationPoint2X: number | null;
  calibrationPoint2Y: number | null;
  calibrationPhysicalLength: number | null;
  isCalibrated: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Shared by the multipart create call and the metadata-only update call. */
export interface FloorPlanMetadataFields {
  entityType: string;
  entityId: number;
  name: string;
  unitSystem: UnitSystem;
  originAnchor: OriginAnchor;
  originX: number;
  originY: number;
  viewboxWidth?: number | null;
  viewboxHeight?: number | null;
}

export interface FloorPlanCalibrationRequest {
  point1X: number;
  point1Y: number;
  point2X: number;
  point2Y: number;
  physicalLength: number;
}

export interface VirtualLocation {
  id: number;
  floorPlanId: number;
  entityType: string | null;
  entityId: number | null;
  name: string;
  locationType: string;
  moduleTag: string | null;
  shapeType: ShapeType;
  geometryJson: string;
  capacity: number | null;
  status: VirtualLocationStatus;
  description: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface VirtualLocationRequest {
  floorPlanId: number;
  entityType?: string | null;
  entityId?: number | null;
  name: string;
  locationType: string;
  moduleTag?: string | null;
  shapeType: ShapeType;
  geometryJson: string;
  capacity?: number | null;
  status?: VirtualLocationStatus;
  description?: string | null;
}

/** Flat SVG-style geometry shapes — see BR-60. Coordinates are in the same pixel space the
 *  floor plan image is displayed/calibrated in, not physical units. */
export interface PointGeometry { x: number; y: number; }
export interface RectangleGeometry { x: number; y: number; width: number; height: number; }
export interface PolygonGeometry { points: { x: number; y: number }[]; }

/**
 * Minimal summaries matching the REAL backend `EquipmentResponse`/`InventoryItemResponse` DTOs.
 * Deliberately NOT reusing `features/equipment/equipment.model.ts` / `features/inventory/inventory.model.ts` —
 * those two frontend models have drifted from their backend DTOs (missing `assetCode`/`itemCode`/`lowStock`,
 * wrong field names like `purchaseCost` vs `purchasePrice`) and fixing them is a separate, unrelated concern.
 */
export type EquipmentStatus = 'AVAILABLE' | 'IN_USE' | 'UNDER_MAINTENANCE' | 'OUT_OF_ORDER' | 'DISPOSED';

export interface SpatialEquipmentSummary {
  id: number;
  name: string;
  assetCode: string;
  status: EquipmentStatus;
  labName: string;
}

export interface SpatialInventoryItemSummary {
  id: number;
  name: string;
  itemCode: string;
  quantity: number;
  minimumQuantity: number | null;
  unit: string;
  lowStock: boolean;
  labName: string;
}

/**
 * A candidate sub-component detected from an imported DXF/PDF's geometry (closed shape) and,
 * where available, a nearby text label — before an admin has reviewed/confirmed it into a real
 * Block/Zone/Room row (or an Equipment/InventoryItem marker link) and a placed VirtualLocation
 * marker. `points` are already in the same SVG/pixel space the floor plan background renders in,
 * so a candidate overlays exactly where it will end up once confirmed. Never persisted as-is —
 * purely a client-side staging shape consumed by the detected-shapes-review-flyout.
 */
export interface DetectedShapeCandidate {
  tempId: string;
  /** Nearest matched TEXT/MTEXT label (DXF) or OCR guess (PDF); null prompts the admin to type one. */
  name: string | null;
  points: { x: number; y: number }[];
}
