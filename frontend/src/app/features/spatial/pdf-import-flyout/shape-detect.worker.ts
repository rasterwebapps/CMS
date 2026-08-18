/// <reference lib="webworker" />

/**
 * Off-main-thread contour detection for PDF-derived floor plans (BR-60 Phase B) — the raster
 * counterpart to `dxf-shape-detect.util.ts`'s exact vector detection. OpenCV.js is CPU-heavy
 * enough (grayscale + threshold + findContours + approxPolyDP over a multi-megapixel page render)
 * to freeze the UI if run on the main thread, hence this dedicated worker — the first one in this
 * app (see BR-60's Phase B notes).
 *
 * OpenCV.js ships as a single ~13MB static asset (`public/assets/ocr/opencv/opencv.js`, not an npm
 * import) so it never touches the JS bundle/budget — loaded here via a *computed* dynamic import
 * specifier (not a string literal) specifically so esbuild treats it as an opaque runtime URL
 * instead of trying to resolve/bundle it at build time (verified empirically; a literal path in
 * `import()` makes the build fail with "Could not resolve").
 *
 * Angular's esbuild-based worker bundling always instantiates workers with `{ type: 'module' }`
 * regardless of what's passed to `new Worker(...)` at the call site — so `importScripts()` (the
 * classic-worker-only API most opencv.js/tesseract.js examples use) is not available here; a
 * dynamic `import()` of the UMD build is used instead. opencv.js's own UMD wrapper already handles
 * a Web Worker environment (`typeof importScripts === 'function'` branch, present on
 * `WorkerGlobalScope` regardless of worker type) and assigns `globalThis.cv`, so this works the
 * same as loading it via a classic worker's `importScripts` would have.
 */

const OPENCV_URL = '/assets/ocr/opencv/opencv.js';

/** Same heuristic thresholds `dxf-shape-detect.util.ts` uses, expressed as a fraction of the whole
 *  image area rather than the DXF's own bounding-box area — same reasoning: too small is noise,
 *  too large is the outer wall outline, and a wrong guess either way is caught at staged review,
 *  never auto-committed. */
const MIN_AREA_FRACTION = 0.001;
const MAX_AREA_FRACTION = 0.9;

/** Defensive cap — real floor plans detect well under this once area-filtered; a pathological scan
 *  (heavy noise, dense hatching mistaken for many small closed loops) could otherwise hand the
 *  review UI an unusable number of rows. */
const MAX_CANDIDATES = 200;

interface DetectRequest {
  buffer: ArrayBuffer;
  width: number;
  height: number;
}

interface RawCandidate {
  points: { x: number; y: number }[];
}

let cvReady: Promise<any> | null = null;

async function loadCv(): Promise<any> {
  if (!cvReady) {
    cvReady = (async () => {
      // Deliberately routed through a variable, not a string literal, so esbuild can't statically
      // resolve/bundle this at build time (a literal `import('/assets/...')` fails the build with
      // "Could not resolve" since esbuild doesn't know about the public/ web root) — this way it
      // stays a genuine runtime dynamic import against the deployed server's static asset path.
      const url = OPENCV_URL;
      await import(url);
      const cvModule = (self as any).cv;
      const cv = cvModule instanceof Promise ? await cvModule : cvModule;
      return cv;
    })();
  }
  return cvReady;
}

self.onmessage = async (event: MessageEvent<DetectRequest>) => {
  try {
    const cv = await loadCv();
    const candidates = detectShapes(cv, event.data);
    (self as any).postMessage({ candidates });
  } catch (err) {
    (self as any).postMessage({ error: err instanceof Error ? err.message : 'Detection failed' });
  }
};

function detectShapes(cv: any, request: DetectRequest): RawCandidate[] {
  const { buffer, width, height } = request;
  const imageData = { data: new Uint8ClampedArray(buffer), width, height };

  const src = cv.matFromImageData(imageData);
  const gray = new cv.Mat();
  const thresh = new cv.Mat();
  const contours = new cv.MatVector();
  const hierarchy = new cv.Mat();

  const results: { area: number; candidate: RawCandidate }[] = [];

  try {
    cv.cvtColor(src, gray, cv.COLOR_RGBA2GRAY);
    // Architectural line drawings are near-universally dark lines on a light background — Otsu
    // picks the split point automatically rather than guessing a fixed brightness cutoff, and the
    // INV flag makes the drawn lines/walls the "foreground" findContours traces around.
    cv.threshold(gray, thresh, 0, 255, cv.THRESH_BINARY_INV + cv.THRESH_OTSU);

    cv.findContours(thresh, contours, hierarchy, cv.RETR_TREE, cv.CHAIN_APPROX_SIMPLE);

    const totalArea = width * height;
    const minArea = totalArea * MIN_AREA_FRACTION;
    const maxArea = totalArea * MAX_AREA_FRACTION;
    // hierarchy is a single-row Mat, 4 int32 columns per contour: [next, previous, firstChild, parent].
    const hierarchyData = contours.size() > 0 ? hierarchy.data32S : new Int32Array(0);

    for (let i = 0; i < contours.size(); i++) {
      const firstChild = hierarchyData[i * 4 + 2];
      if (firstChild !== -1) continue; // not a leaf region — a room's interior has no nested contour

      const contour = contours.get(i);
      const area = cv.contourArea(contour);
      if (area >= minArea && area <= maxArea) {
        const approx = new cv.Mat();
        try {
          const perimeter = cv.arcLength(contour, true);
          cv.approxPolyDP(contour, approx, 0.01 * perimeter, true);
          if (approx.rows >= 3) {
            const points: { x: number; y: number }[] = [];
            for (let p = 0; p < approx.rows; p++) {
              points.push({ x: approx.data32S[p * 2], y: approx.data32S[p * 2 + 1] });
            }
            results.push({ area, candidate: { points } });
          }
        } finally {
          approx.delete();
        }
      }
      contour.delete();
    }
  } finally {
    src.delete();
    gray.delete();
    thresh.delete();
    contours.delete();
    hierarchy.delete();
  }

  return results
    .sort((a, b) => b.area - a.area)
    .slice(0, MAX_CANDIDATES)
    .map((r) => r.candidate);
}
