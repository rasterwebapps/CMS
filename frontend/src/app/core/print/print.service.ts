import { ElementRef, Injectable } from '@angular/core';

/**
 * Helper for printing a single DOM subtree (e.g. a fee receipt) without
 * disturbing the rest of the application. The target element is cloned into a
 * hidden `<iframe>`, the host page's stylesheets are mirrored, and the iframe
 * triggers `window.print()`.
 *
 * Falls back to `window.print()` for the whole page when no element is given.
 *
 * @example
 * ```ts
 * @ViewChild('receipt', { read: ElementRef }) receipt!: ElementRef;
 * private readonly print = inject(PrintService);
 * downloadPdf(): void {
 *   this.print.printElement(this.receipt);
 * }
 * ```
 */
@Injectable({ providedIn: 'root' })
export class PrintService {
  /** Print the entire current route using the browser's standard dialog. */
  printRoute(): void {
    if (typeof window !== 'undefined') {
      window.print();
    }
  }

  /**
   * Clone `target` into a hidden iframe and print just that subtree.
   * Returns `true` when the print job was dispatched.
   *
   * `pageSizeMm`, when given, pins the print page to that exact physical size
   * (e.g. a die-cut label) instead of the browser's default A4/Letter — without
   * it, small-format printers (barcode/label printers) misprint because the
   * content is laid out for a full sheet.
   */
  printElement(
    target: ElementRef<HTMLElement> | HTMLElement,
    pageSizeMm?: { widthMm: number; heightMm: number },
  ): boolean {
    if (typeof document === 'undefined' || typeof window === 'undefined') {
      return false;
    }
    const node = target instanceof ElementRef ? target.nativeElement : target;
    if (!node) return false;

    const iframe = document.createElement('iframe');
    iframe.setAttribute('aria-hidden', 'true');
    iframe.style.position = 'fixed';
    iframe.style.right = '0';
    iframe.style.bottom = '0';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.border = '0';
    document.body.appendChild(iframe);

    const doc = iframe.contentDocument ?? iframe.contentWindow?.document;
    if (!doc) {
      iframe.remove();
      return false;
    }

    // Mirror existing <link rel="stylesheet"> and <style> tags so the cloned
    // markup renders with the host page's tokens and component styles.
    const headParts: string[] = [];
    document.querySelectorAll('link[rel="stylesheet"], style').forEach((el) => {
      headParts.push(el.outerHTML);
    });

    const pageStyle = pageSizeMm
      ? `<style>
          @page { size: ${pageSizeMm.widthMm}mm ${pageSizeMm.heightMm}mm; margin: 0; }
          html, body { margin: 0; padding: 0; }
          .print-root { width: ${pageSizeMm.widthMm}mm; height: ${pageSizeMm.heightMm}mm; display: flex; align-items: center; justify-content: center; overflow: hidden; }
          /*
           * !important throughout: the cloned element keeps whatever on-screen preview
           * class it had (e.g. a flyout's own padding/background/border-radius for
           * displaying the image in a card). Those rules are more specific than a plain
           * ".print-root img" selector once Angular's per-component scoped-style
           * attribute selectors are in the mix, so a caller's preview styling can outrank
           * this reset and push content past the exact page-sized box — which prints as
           * a silent overflow onto a second page rather than anything visibly broken here.
           */
          .print-root img {
            max-width: 100% !important;
            max-height: 100% !important;
            width: auto !important;
            height: auto !important;
            margin: 0 !important;
            padding: 0 !important;
            border: 0 !important;
            border-radius: 0 !important;
            background: transparent !important;
            box-sizing: border-box !important;
          }
        </style>`
      : '';

    doc.open();
    doc.write(`<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>${document.title || 'Print'}</title>
    ${headParts.join('\n')}
    ${pageStyle}
  </head>
  <body class="${document.body.className}">
    <div class="print-root">${node.outerHTML}</div>
  </body>
</html>`);
    doc.close();

    const cleanup = (): void => {
      // Defer removal so Safari has time to render the print preview.
      window.setTimeout(() => iframe.remove(), 1000);
    };

    let printed = false;
    const triggerPrint = (): void => {
      if (printed) return;
      printed = true;
      try {
        const win = iframe.contentWindow;
        if (!win) {
          cleanup();
          return;
        }
        win.focus();
        win.print();
      } finally {
        cleanup();
      }
    };

    // With a print-preview dialog, the browser naturally waits for the user to
    // click Print, which hides any image-decoding race. Some deployments run
    // Chrome with --kiosk --disable-print-preview for unattended label/receipt
    // printers, which fires the print job the instant the load/timeout below
    // triggers — so a blob: URL image (e.g. a barcode PNG) that hasn't finished
    // decoding yet gets silently rasterized as blank. Wait for every <img> in
    // the print document to finish (or fail) loading before printing.
    const imagesReady = (): Promise<void> => {
      const images = Array.from(doc.images);
      if (images.length === 0) return Promise.resolve();
      const settled = images.map(img => img.complete
        ? Promise.resolve()
        : new Promise<void>(resolve => {
            img.addEventListener('load', () => resolve(), { once: true });
            img.addEventListener('error', () => resolve(), { once: true });
          }));
      const timeout = new Promise<void>(resolve => window.setTimeout(resolve, 2000));
      return Promise.race([Promise.all(settled).then(() => undefined), timeout]);
    };

    const triggerPrintWhenReady = (): void => {
      imagesReady().then(triggerPrint);
    };

    if (iframe.contentWindow?.document.readyState === 'complete') {
      triggerPrintWhenReady();
    } else {
      iframe.addEventListener('load', triggerPrintWhenReady, { once: true });
      // Safety net if the load event doesn't fire (e.g. about:blank race).
      // The `printed` flag prevents a duplicate call when the load event also fires.
      window.setTimeout(triggerPrintWhenReady, 500);
    }
    return true;
  }
}
