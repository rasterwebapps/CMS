import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { CmsFlyoutPanelComponent } from '../../../shared/flyout-panel/flyout-panel.component';
import { ToastService } from '../../../core/toast/toast.service';
import { LibraryService } from '../library.service';
import { Library, LibraryRack, LibraryShelf, LibraryBookTransferResult } from '../library.model';

export interface LibraryBookTransferDialogBook {
  id: number;
  title: string;
  accessionNumber: string;
}

export interface LibraryBookTransferDialogData {
  books: LibraryBookTransferDialogBook[];
}

type Step = 'SELECT' | 'RESULT';

@Component({
  selector: 'app-library-book-transfer-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    CmsFlyoutPanelComponent,
  ],
  templateUrl: './library-book-transfer-dialog.component.html',
  styleUrl: './library-book-transfer-dialog.component.scss',
})
export class LibraryBookTransferDialogComponent implements OnInit {
  readonly data = input.required<LibraryBookTransferDialogData>();
  readonly closed = output<LibraryBookTransferResult | undefined>();

  private readonly libraryService = inject(LibraryService);
  private readonly toast = inject(ToastService);

  protected step = signal<Step>('SELECT');
  protected loading = signal(false);
  protected submitting = signal(false);

  protected libraries = signal<Library[]>([]);
  protected racks = signal<LibraryRack[]>([]);
  protected shelves = signal<LibraryShelf[]>([]);

  protected selectedLibraryId: number | null = null;
  protected selectedRackId: number | null = null;
  protected selectedShelfId: number | null = null;
  protected notes = '';

  protected result = signal<LibraryBookTransferResult | null>(null);
  protected singleBookError = signal<string | null>(null);

  protected get isBulk(): boolean {
    return this.data().books.length > 1;
  }

  ngOnInit(): void {
    this.loading.set(true);
    this.libraryService.getLibraries().subscribe({
      next: (libraries) => {
        this.libraries.set(libraries);
        this.selectedLibraryId = libraries[0]?.id ?? null;
        this.loading.set(false);
        if (this.selectedLibraryId) this.onLibraryChange();
      },
      error: () => {
        this.toast.error('Failed to load libraries');
        this.loading.set(false);
      },
    });
  }

  protected onLibraryChange(): void {
    this.selectedRackId = null;
    this.selectedShelfId = null;
    this.shelves.set([]);
    if (!this.selectedLibraryId) { this.racks.set([]); return; }
    this.libraryService.getRacks(this.selectedLibraryId, true).subscribe({
      next: (racks) => this.racks.set(racks),
      error: () => this.toast.error('Failed to load racks'),
    });
  }

  protected onRackChange(): void {
    this.selectedShelfId = null;
    if (!this.selectedRackId) { this.shelves.set([]); return; }
    this.libraryService.getShelves(this.selectedRackId, undefined, true).subscribe({
      next: (shelves) => this.shelves.set(shelves),
      error: () => this.toast.error('Failed to load shelves'),
    });
  }

  protected submitTransfer(): void {
    if (!this.selectedShelfId) return;
    this.submitting.set(true);
    const notes = this.notes.trim() || undefined;

    if (this.isBulk) {
      this.libraryService
        .bulkTransferBooks({ bookIds: this.data().books.map((b) => b.id), newShelfId: this.selectedShelfId, notes })
        .subscribe({
          next: (result) => {
            this.result.set(result);
            this.step.set('RESULT');
            this.submitting.set(false);
          },
          error: (err) => {
            this.toast.error(err?.error?.message ?? 'Failed to transfer books');
            this.submitting.set(false);
          },
        });
    } else {
      const bookId = this.data().books[0].id;
      this.libraryService.transferBook(bookId, { newShelfId: this.selectedShelfId, notes }).subscribe({
        next: () => {
          this.result.set({ succeededBookIds: [bookId], failed: [] });
          this.step.set('RESULT');
          this.submitting.set(false);
        },
        error: (err) => {
          this.singleBookError.set(err?.error?.message ?? 'Failed to transfer book');
          this.result.set({ succeededBookIds: [], failed: [{ bookId, reason: err?.error?.message ?? 'Transfer failed' }] });
          this.step.set('RESULT');
          this.submitting.set(false);
        },
      });
    }
  }

  protected bookTitle(bookId: number): string {
    return this.data().books.find((b) => b.id === bookId)?.title ?? `Book #${bookId}`;
  }

  protected close(): void {
    const r = this.result();
    this.closed.emit(r && r.succeededBookIds.length > 0 ? r : undefined);
  }
}
