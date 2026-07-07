import { Component, inject, signal, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LibraryService } from '../library.service';
import {
  LibraryBookImportValidationResult,
  LibraryBookImportExecuteResult,
  LibraryBookImportRowError,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';

type Step  = 'template' | 'upload' | 'result';
type Phase = 'idle' | 'validating' | 'importing' | 'done';

@Component({
  selector: 'app-library-book-import',
  standalone: true,
  imports: [RouterLink, NgTemplateOutlet, FormsModule],
  templateUrl: './library-book-import.component.html',
  styleUrl:    './library-book-import.component.scss',
})
export class LibraryBookImportComponent {
  private readonly libraryService = inject(LibraryService);
  private readonly toast          = inject(ToastService);
  private readonly router         = inject(Router);

  protected readonly activeStep       = signal<Step>('template');
  protected readonly phase            = signal<Phase>('idle');
  protected readonly selectedFile     = signal<File | null>(null);
  protected readonly skipErroredRows  = signal(true);
  protected readonly validationResult = signal<LibraryBookImportValidationResult | null>(null);
  protected readonly executeResult    = signal<LibraryBookImportExecuteResult | null>(null);

  protected readonly totalErrors = computed(() => {
    const vr = this.validationResult();
    const er = this.executeResult();
    return (vr?.errors.filter(e => e.severity === 'ERROR').length ?? 0) +
           (er?.errors.filter(e => e.severity === 'ERROR').length ?? 0);
  });

  protected readonly totalWarnings = computed(() =>
    this.validationResult()?.warnings.length ?? 0,
  );

  protected downloadTemplate(): void {
    this.libraryService.downloadImportTemplate();
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
    this.libraryService.validateImport(file, this.skipErroredRows()).subscribe({
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
    this.libraryService.executeImport(file, this.skipErroredRows()).subscribe({
      next: result => {
        this.executeResult.set(result);
        this.phase.set('done');
        if (result.booksImported > 0) {
          this.toast.success(`${result.booksImported} books imported successfully`);
        }
      },
      error: () => {
        this.toast.error('Import failed — please check the file and try again');
        this.phase.set('idle');
      },
    });
  }

  protected errorRows(errors: LibraryBookImportRowError[]): LibraryBookImportRowError[] {
    return errors.filter(e => e.severity === 'ERROR');
  }

  protected warningRows(errors: LibraryBookImportRowError[]): LibraryBookImportRowError[] {
    return errors.filter(e => e.severity === 'WARNING');
  }

  protected goToCatalogue(): void {
    void this.router.navigate(['/library/books']);
  }

  protected reset(): void {
    this.selectedFile.set(null);
    this.validationResult.set(null);
    this.executeResult.set(null);
    this.phase.set('idle');
    this.activeStep.set('template');
  }
}
