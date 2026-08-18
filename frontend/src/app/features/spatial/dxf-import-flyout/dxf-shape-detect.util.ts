import type { IEntity, ILwpolylineEntity, IMtextEntity, IPolylineEntity, ITextEntity } from 'dxf-parser';
import { Bounds, cleanMtext, createSvgTransform } from './dxf-render.util';
import { DetectedShapeCandidate } from '../spatial.model';

/** Below this fraction of the drawing's total bounding-box area, a closed polyline is almost
 *  certainly noise (a hardware symbol, a small annotation loop) rather than a real room/zone/block
 *  boundary. Above the upper fraction, it's almost certainly the outer wall outline or a title-block
 *  border, not an individual sub-component. Both are heuristics, not exact — a false positive/negative
 *  is exactly why detected shapes go through staged review rather than being auto-committed. */
const MIN_AREA_FRACTION = 0.001;
const MAX_AREA_FRACTION = 0.9;

let tempIdCounter = 0;
function nextTempId(): string {
  tempIdCounter += 1;
  return `detected-${Date.now()}-${tempIdCounter}`;
}

function isClosedPolyline(entity: IEntity): entity is ILwpolylineEntity | IPolylineEntity {
  if (entity.type !== 'LWPOLYLINE' && entity.type !== 'POLYLINE') return false;
  const e = entity as ILwpolylineEntity | IPolylineEntity;
  return e.shape === true && (e.vertices?.length ?? 0) >= 3;
}

/** Shoelace formula — works in either DXF or SVG space since it's only ever used to compare areas
 *  against each other / against the drawing's own total area, never as an absolute physical value. */
function polygonArea(points: { x: number; y: number }[]): number {
  let sum = 0;
  for (let i = 0; i < points.length; i++) {
    const a = points[i];
    const b = points[(i + 1) % points.length];
    sum += a.x * b.y - b.x * a.y;
  }
  return Math.abs(sum) / 2;
}

function boundsOf(points: { x: number; y: number }[]): { minX: number; minY: number; maxX: number; maxY: number } {
  return {
    minX: Math.min(...points.map((p) => p.x)),
    minY: Math.min(...points.map((p) => p.y)),
    maxX: Math.max(...points.map((p) => p.x)),
    maxY: Math.max(...points.map((p) => p.y)),
  };
}

function insideBounds(x: number, y: number, b: { minX: number; minY: number; maxX: number; maxY: number }): boolean {
  return x >= b.minX && x <= b.maxX && y >= b.minY && y <= b.maxY;
}

interface Label { x: number; y: number; text: string; }

function collectLabels(entities: IEntity[]): Label[] {
  const labels: Label[] = [];
  for (const entity of entities) {
    if (entity.type === 'TEXT') {
      const e = entity as ITextEntity;
      if (e.startPoint && e.text) labels.push({ x: e.startPoint.x, y: e.startPoint.y, text: e.text });
    } else if (entity.type === 'MTEXT') {
      const e = entity as IMtextEntity;
      if (e.position && e.text) labels.push({ x: e.position.x, y: e.position.y, text: cleanMtext(e.text) });
    }
  }
  return labels;
}

/** Nearest label whose insertion point falls inside the candidate's own bounding box — a floor
 *  plan's room label is conventionally placed inside the room it names, so "inside the bbox" is a
 *  reliable enough signal without needing true point-in-polygon (the bbox is already a tight-enough
 *  proxy for a roughly rectangular room, and a wrong pick is just a wrong pre-fill the admin edits
 *  during staged review, never a silent auto-commit). */
function nearestLabelInside(bbox: { minX: number; minY: number; maxX: number; maxY: number }, labels: Label[]): string | null {
  const centerX = (bbox.minX + bbox.maxX) / 2;
  const centerY = (bbox.minY + bbox.maxY) / 2;
  let best: Label | null = null;
  let bestDist = Infinity;
  for (const label of labels) {
    if (!insideBounds(label.x, label.y, bbox)) continue;
    const dist = Math.hypot(label.x - centerX, label.y - centerY);
    if (dist < bestDist) {
      bestDist = dist;
      best = label;
    }
  }
  return best?.text.trim() || null;
}

/** Detects candidate sub-component boundaries from a DXF's own closed-polyline geometry — exact,
 *  vector-based, no image analysis needed (contrast the PDF path, which has to approximate this via
 *  OpenCV/Tesseract on a rasterized page). Coordinates are run through the same `createSvgTransform`
 *  `buildSvg` uses, so a candidate overlays exactly where its shape will render in the background. */
export function detectShapes(entities: IEntity[], bounds: Bounds): DetectedShapeCandidate[] {
  const totalArea = (bounds.maxX - bounds.minX) * (bounds.maxY - bounds.minY);
  if (!(totalArea > 0)) return [];

  const minArea = totalArea * MIN_AREA_FRACTION;
  const maxArea = totalArea * MAX_AREA_FRACTION;
  const { toSvgX, toSvgY } = createSvgTransform(bounds);
  const labels = collectLabels(entities);

  const candidates: DetectedShapeCandidate[] = [];
  for (const entity of entities) {
    if (!isClosedPolyline(entity)) continue;
    const vertices = (entity.vertices ?? []).map((v) => ({ x: v.x, y: v.y }));
    const area = polygonArea(vertices);
    if (area < minArea || area > maxArea) continue;

    const bbox = boundsOf(vertices);
    const name = nearestLabelInside(bbox, labels);
    const points = vertices.map((v) => ({ x: toSvgX(v.x), y: toSvgY(v.y) }));
    candidates.push({ tempId: nextTempId(), name, points });
  }
  return candidates;
}
