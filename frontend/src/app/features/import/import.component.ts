import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ImportService } from './import.service';
import { ImportDefaults, ImportRowError, ImportValidationResult, ImportExecuteResult } from './import.model';
import { AcademicYearService } from '../academic-year/academic-year.service';
import { AcademicYear } from '../academic-year/academic-year.model';
import { ToastService } from '../../core/toast/toast.service';

type Step = 'template' | 'defaults' | 'upload' | 'result';
type Phase = 'idle' | 'validating' | 'importing' | 'done';

@Component({
  selector: 'app-import',
  standalone: true,
  imports: [FormsModule, NgTemplateOutlet, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './import.component.html',
  styleUrl:    './import.component.scss',
})
export class ImportComponent implements OnInit {
  private readonly importService     = inject(ImportService);
  private readonly academicYearSvc   = inject(AcademicYearService);
  private readonly toast             = inject(ToastService);

  protected readonly academicYears   = signal<AcademicYear[]>([]);
  protected readonly selectedFile    = signal<File | null>(null);
  protected readonly phase           = signal<Phase>('idle');
  protected readonly validationResult = signal<ImportValidationResult | null>(null);
  protected readonly executeResult    = signal<ImportExecuteResult | null>(null);
  protected readonly activeStep       = signal<Step>('template');

  protected readonly STUDENT_TYPES        = ['DAY_SCHOLAR', 'HOSTELER'];
  protected readonly ADMISSION_CATEGORIES = ['MANAGEMENT', 'COUNSELLING'];
  protected readonly SHEET_INFO = [
    { name: 'Students',       desc: 'One row per student — personal, demographic and address details' },
    { name: 'Qualifications', desc: 'Academic history — link by student email, multiple rows allowed' },
    { name: 'Fee History',    desc: 'Total fee + historical payment records per student' },
    { name: 'Reference',      desc: 'Valid codes and enum values from your system (read-only, for dropdown validation)' },
  ];

  protected defaults: ImportDefaults = {
    defaultJoiningAcademicYearId: null,
    defaultStudentType: 'DAY_SCHOLAR',
    defaultNationality: 'Indian',
    defaultState: '',
    defaultSemester: 1,
    defaultAdmissionCategory: 'MANAGEMENT',
    skipErroredRows: true,
  };

  protected readonly hasErrors = computed(() => {
    const vr = this.validationResult();
    const er = this.executeResult();
    const errors = vr ? vr.errors.filter(e => e.severity === 'ERROR') : [];
    const exErrors = er ? er.errors.filter(e => e.severity === 'ERROR') : [];
    return errors.length + exErrors.length;
  });

  protected readonly hasWarnings = computed(() => {
    const vr = this.validationResult();
    return vr ? vr.warnings.length : 0;
  });

  ngOnInit(): void {
    this.academicYearSvc.getAllAcademicYears().subscribe({
      next: (years) => {
        const sorted = [...years].sort((a, b) =>
          new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
        );
        this.academicYears.set(sorted);
        const current = sorted.find(y => y.isCurrent) ?? sorted[0];
        if (current) this.defaults = { ...this.defaults, defaultJoiningAcademicYearId: current.id };
      },
    });
  }

  protected downloadTemplate(): void {
    this.importService.downloadTemplate();
  }

  protected onFileChange(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.selectedFile.set(file);
    this.validationResult.set(null);
    this.executeResult.set(null);
    this.phase.set('idle');
    if (file) this.activeStep.set('upload');
  }

  protected clearFile(): void {
    this.selectedFile.set(null);
    this.validationResult.set(null);
    this.executeResult.set(null);
    this.phase.set('idle');
  }

  protected validate(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.phase.set('validating');
    this.validationResult.set(null);
    this.executeResult.set(null);
    this.importService.validate(file, this.defaults).subscribe({
      next: (r) => {
        this.validationResult.set(r);
        this.phase.set('idle');
        this.activeStep.set('result');
      },
      error: (err) => {
        this.toast.error('Validation failed: ' + (err?.error?.message ?? 'Unknown error'));
        this.phase.set('idle');
      },
    });
  }

  protected import(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.phase.set('importing');
    this.executeResult.set(null);
    this.importService.execute(file, this.defaults).subscribe({
      next: (r) => {
        this.executeResult.set(r);
        this.phase.set('done');
        this.activeStep.set('result');
        if (r.studentsImported > 0) {
          this.toast.success(`Import complete — ${r.studentsImported} student(s) imported.`);
        }
      },
      error: (err) => {
        this.toast.error('Import failed: ' + (err?.error?.message ?? 'Unknown error'));
        this.phase.set('idle');
      },
    });
  }

  protected reset(): void {
    this.selectedFile.set(null);
    this.validationResult.set(null);
    this.executeResult.set(null);
    this.phase.set('idle');
    this.activeStep.set('template');
  }

  protected goToStep(step: Step): void {
    this.activeStep.set(step);
  }

  protected errorsBySheet(errors: ImportRowError[], sheet: string): ImportRowError[] {
    return errors.filter(e => e.sheet === sheet && e.severity === 'ERROR');
  }

  protected warnsBySheet(errors: ImportRowError[], sheet: string): ImportRowError[] {
    return errors.filter(e => e.sheet === sheet && e.severity === 'WARNING');
  }

  protected allErrors(result: ImportValidationResult | ImportExecuteResult | null): ImportRowError[] {
    if (!result) return [];
    return 'errors' in result ? result.errors.filter(e => e.severity === 'ERROR') : [];
  }

  protected allWarnings(result: ImportValidationResult | null): ImportRowError[] {
    if (!result) return [];
    return result.warnings ?? [];
  }
}
