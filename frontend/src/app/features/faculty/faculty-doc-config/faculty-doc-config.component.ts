import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FacultyService } from '../faculty.service';
import {
  FacultyDocumentTypeRequirement,
  FacultyDocumentTypeRequirementRequest,
  FacultyQualification,
  DESIGNATION_OPTIONS,
  FACULTY_QUALIFICATION_OPTIONS,
} from '../faculty.model';
import { DepartmentService } from '../../department/department.service';
import { Department } from '../../department/department.model';
import { ProgramService } from '../../program/program.service';
import { DocumentTypeInfo } from '../../program/program.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';

@Component({
  selector: 'app-faculty-doc-config',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTooltipModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './faculty-doc-config.component.html',
  styleUrl: './faculty-doc-config.component.scss',
})
export class FacultyDocConfigComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly departmentService = inject(DepartmentService);
  private readonly programService = inject(ProgramService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);

  protected readonly rules = signal<FacultyDocumentTypeRequirement[]>([]);
  protected readonly departments = signal<Department[]>([]);
  protected readonly allDocumentTypes = signal<DocumentTypeInfo[]>([]);

  protected readonly displayedColumns = [
    'documentType',
    'criterion',
    'criterionValue',
    'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<FacultyDocumentTypeRequirement>([]);

  protected readonly facultyDocumentTypes = computed(() =>
    this.allDocumentTypes().filter(
      (dt) =>
        dt.category === 'Faculty — appointment & service' ||
        dt.category === 'Faculty — qualifications' ||
        dt.category === 'Faculty — experience' ||
        dt.category === 'Faculty — identity & misc' ||
        dt.category === 'Identity',
    ),
  );

  protected readonly DESIGNATION_OPTIONS = DESIGNATION_OPTIONS;
  protected readonly FACULTY_QUALIFICATION_OPTIONS = FACULTY_QUALIFICATION_OPTIONS;

  protected newDocumentType = '';
  protected newDesignation = '';
  protected newDepartmentId: number | null = null;
  protected newQualification: FacultyQualification | '' = '';

  protected get canAddRule(): boolean {
    return (
      !!this.newDocumentType &&
      (!!this.newDesignation || !!this.newDepartmentId || !!this.newQualification)
    );
  }

  ngOnInit(): void {
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    let pending = 3;
    const done = () => {
      if (--pending === 0) this.loading.set(false);
    };

    this.facultyService.getDocumentTypeRequirements().subscribe({
      next: (rules) => {
        this.rules.set(rules);
        this.dataSource.data = rules;
        done();
      },
      error: () => {
        this.toast.error('Failed to load document type requirements');
        done();
      },
    });

    this.departmentService.getAll().subscribe({
      next: (depts) => {
        this.departments.set(depts);
        done();
      },
      error: () => done(),
    });

    this.programService.getAllDocumentTypes().subscribe({
      next: (types) => {
        this.allDocumentTypes.set(types);
        done();
      },
      error: () => done(),
    });
  }

  protected addRule(): void {
    if (!this.canAddRule) return;

    const request: FacultyDocumentTypeRequirementRequest = {
      documentType: this.newDocumentType,
      designation: this.newDesignation || undefined,
      departmentId: this.newDepartmentId ?? undefined,
      qualification: (this.newQualification as FacultyQualification) || undefined,
    };

    this.saving.set(true);
    this.facultyService.createDocumentTypeRequirement(request).subscribe({
      next: (created) => {
        this.rules.update((list) => [...list, created]);
        this.dataSource.data = this.rules();
        this.resetForm();
        this.saving.set(false);
        this.toast.success('Requirement rule added');
      },
      error: () => {
        this.toast.error('Failed to add requirement rule');
        this.saving.set(false);
      },
    });
  }

  protected confirmDelete(rule: FacultyDocumentTypeRequirement): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Remove Requirement',
        message: `Remove the requirement for <strong>${rule.documentTypeLabel}</strong>?`,
        confirmLabel: 'Remove',
        confirmColor: 'warn',
      },
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) this.deleteRule(rule.id);
    });
  }

  private deleteRule(id: number): void {
    this.facultyService.deleteDocumentTypeRequirement(id).subscribe({
      next: () => {
        this.rules.update((list) => list.filter((r) => r.id !== id));
        this.dataSource.data = this.rules();
        this.toast.success('Requirement rule removed');
      },
      error: () => this.toast.error('Failed to remove requirement rule'),
    });
  }

  protected criterionLabel(rule: FacultyDocumentTypeRequirement): string {
    if (rule.designation) return 'Designation';
    if (rule.departmentId) return 'Department';
    if (rule.qualification) return 'Qualification';
    return '—';
  }

  protected criterionValue(rule: FacultyDocumentTypeRequirement): string {
    if (rule.designation) return this.formatDesignation(rule.designation);
    if (rule.departmentId) return rule.departmentName ?? '—';
    if (rule.qualification) return rule.qualificationLabel ?? rule.qualification;
    return '—';
  }

  private formatDesignation(d: string): string {
    return d
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  private resetForm(): void {
    this.newDocumentType = '';
    this.newDesignation = '';
    this.newDepartmentId = null;
    this.newQualification = '';
  }
}
