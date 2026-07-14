import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LibraryService } from '../library.service';
import {
  LibraryImportValidationResult,
  LibraryImportExecuteResult,
  LibraryImportRowError,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';

type ItemType = 'book' | 'journal';
type Step     = 'template' | 'upload' | 'result';
type Phase    = 'idle' | 'validating' | 'importing' | 'done';

@Component({
  selector: 'app-library-import',
  standalone: true,
  imports: [RouterLink, NgTemplateOutlet, FormsModule],
  templateUrl: './library-import.component.html',
  styleUrl:    './library-import.component.scss',
})
export class LibraryImportComponent implements OnInit {
  private readonly libraryService = inject(LibraryService);
  private readonly toast          = inject(ToastService);
  private readonly router         = inject(Router);
  private readonly route          = inject(ActivatedRoute);
  private readonly permissions    = inject(PermissionService);

  protected readonly canImportBooks    = computed(() => this.permissions.hasAny('LIBRARY_IMPORT'));
  protected readonly canImportJournals = computed(() => this.permissions.hasAny('LIBRARY_PERIODICAL_IMPORT'));
  protected readonly showToggle        = computed(() => this.canImportBooks() && this.canImportJournals());

  protected readonly itemType = signal<ItemType>('book');

  protected readonly activeStep       = signal<Step>('template');
  protected readonly phase            = signal<Phase>('idle');
  protected readonly selectedFile     = signal<File | null>(null);
  protected readonly skipErroredRows  = signal(true);
  protected readonly validationResult = signal<LibraryImportValidationResult | null>(null);
  protected readonly executeResult    = signal<LibraryImportExecuteResult | null>(null);

  protected readonly totalErrors = computed(() => {
    const vr = this.validationResult();
    const er = this.executeResult();
    return (vr?.errors.filter(e => e.severity === 'ERROR').length ?? 0) +
           (er?.errors.filter(e => e.severity === 'ERROR').length ?? 0);
  });

  protected readonly totalWarnings = computed(() =>
    this.validationResult()?.warnings.length ?? 0,
  );

  ngOnInit(): void {
    const requestedJournal = this.route.snapshot.queryParamMap.get('type') === 'journal';
    if (requestedJournal && this.canImportJournals()) {
      this.itemType.set('journal');
    } else if (!this.canImportBooks() && this.canImportJournals()) {
      // Only permitted for journals — lock to that even if ?type=book was requested.
      this.itemType.set('journal');
    }
  }

  protected setItemType(type: ItemType): void {
    if (this.itemType() === type) return;
    if (type === 'book' && !this.canImportBooks()) return;
    if (type === 'journal' && !this.canImportJournals()) return;
    this.itemType.set(type);
    this.reset();
  }

  protected downloadTemplate(): void {
    this.libraryService.downloadImportTemplate(this.itemType());
    this.activeStep.set('upload');
  }

  protected onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.selectedFile.set(file);
    this.validationResult.set(null);
    this.executeResult.set(null);
    this.phase.set('idle');
  }

  protected validate(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.phase.set('validating');
    this.libraryService.validateImport(this.itemType(), file, this.skipErroredRows()).subscribe({
      next: result => {
        this.validationResult.set(result);
        this.phase.set('idle');
        this.activeStep.set('result');
      },
      error: () => {
        this.toast.error('Validation failed — please check the file and try again');
        this.phase.set('idle');
      },
    });
  }

  protected execute(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.phase.set('importing');
    this.libraryService.executeImport(this.itemType(), file, this.skipErroredRows()).subscribe({
      next: result => {
        this.executeResult.set(result);
        this.phase.set('done');
        if (result.imported > 0) {
          const noun = this.itemType() === 'book' ? 'books' : 'journals';
          this.toast.success(`${result.imported} ${noun} imported successfully`);
        }
      },
      error: () => {
        this.toast.error('Import failed — please check the file and try again');
        this.phase.set('idle');
      },
    });
  }

  protected errorRows(errors: LibraryImportRowError[]): LibraryImportRowError[] {
    return errors.filter(e => e.severity === 'ERROR');
  }

  protected warningRows(errors: LibraryImportRowError[]): LibraryImportRowError[] {
    return errors.filter(e => e.severity === 'WARNING');
  }

  protected goToCatalogue(): void {
    void this.router.navigate([this.itemType() === 'book' ? '/library/books' : '/library/periodicals']);
  }

  protected reset(): void {
    this.selectedFile.set(null);
    this.validationResult.set(null);
    this.executeResult.set(null);
    this.phase.set('idle');
    this.activeStep.set('template');
  }
}
