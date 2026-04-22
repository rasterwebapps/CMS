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
   */
  printElement(target: ElementRef<HTMLElement> | HTMLElement): boolean {
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

    doc.open();
    doc.write(`<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>${document.title || 'Print'}</title>
    ${headParts.join('\n')}
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

    const triggerPrint = (): void => {
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

    if (iframe.contentWindow?.document.readyState === 'complete') {
      triggerPrint();
    } else {
      iframe.addEventListener('load', triggerPrint, { once: true });
      // Safety net if the load event doesn't fire (e.g. about:blank race).
      window.setTimeout(triggerPrint, 500);
    }
    return true;
  }
}
