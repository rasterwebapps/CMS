import { Injectable, inject } from '@angular/core';
import { Observable, catchError, from, map, of, switchMap } from 'rxjs';
import { LibraryService } from './library.service';
import { LibraryBarcodeLabelsRequest, LibraryItemType, LibraryPrinterActionResult, LibraryPrinterMode } from './library.model';

/**
 * Browser Print's fixed default local API endpoint — separate from the configurable
 * `barcode_printer_port` setting, which is only ever used server-side for NETWORK mode.
 * Do not conflate the two.
 */
const LOCAL_AGENT_ENDPOINT = 'http://localhost:9100/write';

/**
 * The one place that knows about the three barcode-print transports (BROWSER/NETWORK/
 * LOCAL_AGENT), so the book and periodical list/preview screens don't each duplicate the
 * branching. Callers check {@link getPrinterMode} first — BROWSER mode keeps using the
 * existing PNG/PDF + browser-print-dialog flow untouched; this service is only invoked for
 * NETWORK/LOCAL_AGENT.
 */
@Injectable({ providedIn: 'root' })
export class LibraryPrintTransportService {
  private readonly libraryService = inject(LibraryService);

  getPrinterMode(): Observable<LibraryPrinterMode> {
    return this.libraryService.getSettings().pipe(
      map(settings => {
        const mode = settings.find(s => s.settingKey === 'barcode_printer_mode')?.settingValue;
        return mode === 'NETWORK' || mode === 'LOCAL_AGENT' ? mode : 'BROWSER';
      }),
    );
  }

  sendSingle(itemType: LibraryItemType, id: number, mode: LibraryPrinterMode): Observable<LibraryPrinterActionResult> {
    if (mode === 'NETWORK') {
      return itemType === 'BOOK'
        ? this.libraryService.printBookBarcodeNetwork(id)
        : this.libraryService.printPeriodicalBarcodeNetwork(id);
    }
    const zpl$ = itemType === 'BOOK'
      ? this.libraryService.getBookBarcodeZpl(id)
      : this.libraryService.getPeriodicalBarcodeZpl(id);
    return zpl$.pipe(switchMap(zpl => this.forwardToLocalAgent(zpl)));
  }

  sendBatch(itemType: LibraryItemType, ids: number[], mode: LibraryPrinterMode): Observable<LibraryPrinterActionResult> {
    const request: LibraryBarcodeLabelsRequest = { ids };
    if (mode === 'NETWORK') {
      return itemType === 'BOOK'
        ? this.libraryService.printBookBarcodeLabelsNetwork(request)
        : this.libraryService.printPeriodicalBarcodeLabelsNetwork(request);
    }
    const zpl$ = itemType === 'BOOK'
      ? this.libraryService.getBookBarcodeLabelsZpl(request)
      : this.libraryService.getPeriodicalBarcodeLabelsZpl(request);
    return zpl$.pipe(switchMap(zpl => this.forwardToLocalAgent(zpl)));
  }

  private forwardToLocalAgent(zpl: string): Observable<LibraryPrinterActionResult> {
    return from(
      fetch(LOCAL_AGENT_ENDPOINT, { method: 'POST', body: zpl }).then(response => ({
        success: response.ok,
        message: response.ok ? undefined : `Local print agent responded with status ${response.status}`,
      })),
    ).pipe(
      catchError(() => of({
        success: false,
        message: 'Local print agent not detected — is Browser Print running on this machine?',
      })),
    );
  }
}
