import { createWorker, OEM, PSM } from 'tesseract.js';
import { DetectedShapeCandidate } from '../spatial.model';

/** Local static assets (`public/assets/ocr/…`), never fetched from a public CDN — this app may be
 *  deployed on-prem with restricted outbound network access (see BR-60 Phase B notes), and OCR
 *  language/core files are multi-megabyte enough that a CDN dependency would be a real reliability
 *  risk for something this incidental to the feature. */
const TESSERACT_CORE_PATH = '/assets/ocr/tesseract-core';
const TESSERACT_WORKER_PATH = '/assets/ocr/tesseract-worker/worker.min.js';
const TESSERACT_LANG_PATH = '/assets/ocr/tesseract-lang';

/** Fraction of a candidate's own bounding-box size added as crop padding — room labels are usually
 *  near-centered but not pixel-perfectly inside the detected wall boundary, and OCR does noticeably
 *  worse when a label is clipped at its own edge. */
const CROP_PADDING_FRACTION = 0.08;

let tempIdCounter = 0;
function nextTempId(): string {
  tempIdCounter += 1;
  return `pdf-detected-${Date.now()}-${tempIdCounter}`;
}

interface RawCandidate {
  points: { x: number; y: number }[];
}

interface WorkerResponse {
  candidates?: RawCandidate[];
  error?: string;
}

/**
 * Detects candidate sub-component boundaries from a rasterized PDF page — the approximate,
 * raster-based counterpart to `dxf-shape-detect.util.ts`'s exact vector detection. `canvas` must
 * already be rendered at the same scale used to produce the final export PNG, so the returned
 * points land in the exact same pixel space the floor plan background will display in (no
 * rescaling needed downstream, same as the DXF path's shared `createSvgTransform`).
 *
 * Two heavy, independent pieces: contour detection runs in `shape-detect.worker.ts` (OpenCV.js,
 * off the main thread); OCR label-guessing runs via `tesseract.js`'s own `createWorker`, which
 * spawns its own dedicated Worker internally — so neither blocks the UI, without this function
 * needing to manage nested workers itself.
 */
export async function detectShapesFromCanvas(canvas: HTMLCanvasElement): Promise<DetectedShapeCandidate[]> {
  const ctx = canvas.getContext('2d');
  if (!ctx) return [];

  const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
  const rawCandidates = await runContourWorker(imageData);
  if (rawCandidates.length === 0) return [];

  const ocrWorker = await createWorker('eng', OEM.LSTM_ONLY, {
    corePath: TESSERACT_CORE_PATH,
    workerPath: TESSERACT_WORKER_PATH,
    langPath: TESSERACT_LANG_PATH,
  });

  try {
    // Room/zone labels are short, isolated text sitting on top of mostly-blank/graphic space —
    // SPARSE_TEXT ("find as much text as possible, no particular order") matches that far better
    // than the default assumption of a single block of prose.
    await ocrWorker.setParameters({ tessedit_pageseg_mode: PSM.SPARSE_TEXT });

    const candidates: DetectedShapeCandidate[] = [];
    for (const raw of rawCandidates) {
      const name = await ocrLabelFor(ocrWorker, canvas, raw.points);
      candidates.push({ tempId: nextTempId(), name, points: raw.points });
    }
    return candidates;
  } finally {
    await ocrWorker.terminate();
  }
}

function runContourWorker(imageData: ImageData): Promise<RawCandidate[]> {
  return new Promise((resolve, reject) => {
    const worker = new Worker(new URL('./shape-detect.worker', import.meta.url));
    worker.onmessage = (event: MessageEvent<WorkerResponse>) => {
      worker.terminate();
      if (event.data?.error) reject(new Error(event.data.error));
      else resolve(event.data?.candidates ?? []);
    };
    worker.onerror = (err) => {
      worker.terminate();
      reject(err instanceof ErrorEvent ? new Error(err.message) : new Error('Contour detection failed'));
    };
    worker.postMessage(
      { buffer: imageData.data.buffer, width: imageData.width, height: imageData.height },
      [imageData.data.buffer],
    );
  });
}

async function ocrLabelFor(
  ocrWorker: Awaited<ReturnType<typeof createWorker>>,
  sourceCanvas: HTMLCanvasElement,
  points: { x: number; y: number }[],
): Promise<string | null> {
  const crop = cropCanvasForPoints(sourceCanvas, points);
  if (!crop) return null;

  try {
    const { data } = await ocrWorker.recognize(crop);
    const text = data.text?.trim();
    return text ? text.replace(/\s+/g, ' ') : null;
  } catch {
    return null; // A single failed OCR crop shouldn't abort the whole batch — falls back to manual naming.
  }
}

function cropCanvasForPoints(source: HTMLCanvasElement, points: { x: number; y: number }[]): HTMLCanvasElement | null {
  if (points.length === 0) return null;

  const minX = Math.min(...points.map((p) => p.x));
  const minY = Math.min(...points.map((p) => p.y));
  const maxX = Math.max(...points.map((p) => p.x));
  const maxY = Math.max(...points.map((p) => p.y));

  const padX = (maxX - minX) * CROP_PADDING_FRACTION;
  const padY = (maxY - minY) * CROP_PADDING_FRACTION;

  const x = Math.max(0, Math.floor(minX - padX));
  const y = Math.max(0, Math.floor(minY - padY));
  const width = Math.min(source.width, Math.ceil(maxX + padX)) - x;
  const height = Math.min(source.height, Math.ceil(maxY + padY)) - y;
  if (width <= 0 || height <= 0) return null;

  const crop = document.createElement('canvas');
  crop.width = width;
  crop.height = height;
  const ctx = crop.getContext('2d');
  if (!ctx) return null;
  ctx.drawImage(source, x, y, width, height, 0, 0, width, height);
  return crop;
}
