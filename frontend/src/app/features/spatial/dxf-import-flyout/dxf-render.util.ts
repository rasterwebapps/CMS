import type { IEntity, IArcEntity, ICircleEntity, ILineEntity, ILwpolylineEntity, IMtextEntity, IPolylineEntity, ITextEntity } from 'dxf-parser';

/** Common architectural-drawing subset — everything else (HATCH, SPLINE, DIMENSION, INSERT
 *  blocks, nested geometry) is silently skipped rather than failing the whole import. */
const SUPPORTED_TYPES = new Set(['LINE', 'LWPOLYLINE', 'POLYLINE', 'CIRCLE', 'ARC', 'TEXT', 'MTEXT']);

export interface Bounds {
  minX: number;
  minY: number;
  maxX: number;
  maxY: number;
}

/** AutoCAD $INSUNITS codes → meters-per-drawing-unit. Codes not listed here (0 = unitless, or
 *  anything absent) mean the file carries no reliable real-world scale. */
const INSUNITS_TO_METERS: Record<number, number> = {
  1: 0.0254,     // inches
  2: 0.3048,     // feet
  3: 1609.344,   // miles
  4: 0.001,      // millimeters
  5: 0.01,       // centimeters
  6: 1,          // meters
  7: 1000,       // kilometers
  10: 0.9144,    // yards
};

export function insunitsToMetersPerUnit(insunits: number | undefined): number | null {
  if (insunits == null) return null;
  return INSUNITS_TO_METERS[insunits] ?? null;
}

export function supportedEntities(entities: IEntity[]): IEntity[] {
  return entities.filter((e) => SUPPORTED_TYPES.has(e.type));
}

export function computeBounds(entities: IEntity[]): Bounds | null {
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  let touched = false;

  const consider = (x: number, y: number) => {
    if (!Number.isFinite(x) || !Number.isFinite(y)) return;
    touched = true;
    if (x < minX) minX = x;
    if (x > maxX) maxX = x;
    if (y < minY) minY = y;
    if (y > maxY) maxY = y;
  };

  for (const entity of entities) {
    switch (entity.type) {
      case 'LINE': {
        const e = entity as ILineEntity;
        e.vertices?.forEach((v) => consider(v.x, v.y));
        break;
      }
      case 'LWPOLYLINE':
      case 'POLYLINE': {
        const e = entity as ILwpolylineEntity | IPolylineEntity;
        e.vertices?.forEach((v) => consider(v.x, v.y));
        break;
      }
      case 'CIRCLE':
      case 'ARC': {
        const e = entity as ICircleEntity | IArcEntity;
        if (e.center) {
          consider(e.center.x - e.radius, e.center.y - e.radius);
          consider(e.center.x + e.radius, e.center.y + e.radius);
        }
        break;
      }
      case 'TEXT': {
        const e = entity as ITextEntity;
        if (e.startPoint) consider(e.startPoint.x, e.startPoint.y);
        break;
      }
      case 'MTEXT': {
        const e = entity as IMtextEntity;
        if (e.position) consider(e.position.x, e.position.y);
        break;
      }
    }
  }

  return touched ? { minX, minY, maxX, maxY } : null;
}

export function extractLayerNames(entities: IEntity[]): string[] {
  const names = new Set<string>();
  for (const entity of entities) {
    names.add(entity.layer || '0');
  }
  return Array.from(names).sort();
}

export interface SvgTransform {
  width: number;
  height: number;
  toSvgX: (x: number) => number;
  toSvgY: (y: number) => number;
}

/** The single DXF→SVG coordinate mapping (screen space: Y increasing downward, origin at the
 *  drawing's bounding-box top-left) — shared by `buildSvg` below and `detectShapes`
 *  (`dxf-shape-detect.util.ts`) so a detected candidate's geometry lands exactly where its shape
 *  is drawn in the rendered background, pixel for pixel. */
export function createSvgTransform(bounds: Bounds, padding = 10): SvgTransform {
  return {
    width: bounds.maxX - bounds.minX + padding * 2,
    height: bounds.maxY - bounds.minY + padding * 2,
    toSvgX: (x: number) => x - bounds.minX + padding,
    toSvgY: (y: number) => bounds.maxY - y + padding,
  };
}

/** Builds a self-contained SVG string in screen space (Y increasing downward, origin at the
 *  drawing's bounding-box top-left) from the given entities, skipping any not on a visible layer. */
export function buildSvg(entities: IEntity[], visibleLayers: Set<string>, bounds: Bounds, padding = 10): string {
  const { width, height, toSvgX, toSvgY } = createSvgTransform(bounds, padding);

  const parts: string[] = [];
  for (const entity of entities) {
    const layer = entity.layer || '0';
    if (!visibleLayers.has(layer)) continue;

    switch (entity.type) {
      case 'LINE': {
        const e = entity as ILineEntity;
        const [a, b] = e.vertices ?? [];
        if (a && b) {
          parts.push(`<line x1="${toSvgX(a.x)}" y1="${toSvgY(a.y)}" x2="${toSvgX(b.x)}" y2="${toSvgY(b.y)}" />`);
        }
        break;
      }
      case 'LWPOLYLINE':
      case 'POLYLINE': {
        const e = entity as ILwpolylineEntity | IPolylineEntity;
        const points = (e.vertices ?? []).map((v) => `${toSvgX(v.x)},${toSvgY(v.y)}`).join(' ');
        if (points) {
          const tag = e.shape ? 'polygon' : 'polyline';
          parts.push(`<${tag} points="${points}" />`);
        }
        break;
      }
      case 'CIRCLE': {
        const e = entity as ICircleEntity;
        if (e.center) {
          parts.push(`<circle cx="${toSvgX(e.center.x)}" cy="${toSvgY(e.center.y)}" r="${e.radius}" />`);
        }
        break;
      }
      case 'ARC': {
        const e = entity as IArcEntity;
        if (e.center) {
          parts.push(buildArcPath(e, toSvgX, toSvgY));
        }
        break;
      }
      case 'TEXT': {
        const e = entity as ITextEntity;
        if (e.startPoint && e.text) {
          const rotation = -(e.rotation ?? 0);
          const x = toSvgX(e.startPoint.x);
          const y = toSvgY(e.startPoint.y);
          const size = e.textHeight || 2.5;
          parts.push(
            `<text x="${x}" y="${y}" font-size="${size}" transform="rotate(${rotation} ${x} ${y})">${escapeXml(e.text)}</text>`,
          );
        }
        break;
      }
      case 'MTEXT': {
        const e = entity as IMtextEntity;
        if (e.position && e.text) {
          const x = toSvgX(e.position.x);
          const y = toSvgY(e.position.y);
          const size = e.height || 2.5;
          parts.push(`<text x="${x}" y="${y}" font-size="${size}">${escapeXml(cleanMtext(e.text))}</text>`);
        }
        break;
      }
    }
  }

  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" width="${width}" height="${height}">`
    + `<rect x="0" y="0" width="${width}" height="${height}" fill="#ffffff" />`
    + `<g fill="none" stroke="#1a1a1a" stroke-width="${Math.max(width, height) / 600}">${parts.join('')}</g>`
    + `</svg>`;
}

function buildArcPath(e: IArcEntity, toSvgX: (x: number) => number, toSvgY: (y: number) => number): string {
  const startRad = (e.startAngle * Math.PI) / 180;
  const endRad = (e.endAngle * Math.PI) / 180;
  const x1 = toSvgX(e.center.x + e.radius * Math.cos(startRad));
  const y1 = toSvgY(e.center.y + e.radius * Math.sin(startRad));
  const x2 = toSvgX(e.center.x + e.radius * Math.cos(endRad));
  const y2 = toSvgY(e.center.y + e.radius * Math.sin(endRad));
  const angleLength = e.angleLength ?? Math.abs(e.endAngle - e.startAngle);
  const largeArc = angleLength > 180 ? 1 : 0;
  return `<path d="M ${x1} ${y1} A ${e.radius} ${e.radius} 0 ${largeArc} 1 ${x2} ${y2}" fill="none" />`;
}

export function cleanMtext(text: string): string {
  return text
    .replace(/\\P/gi, ' ')
    .replace(/\{[^}]*\}/g, '')
    .replace(/\\[A-Za-z][^;]*;/g, '')
    .trim();
}

function escapeXml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}
