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
  FACULTY_QUALIFICATION_OPTIONS,
} from '../faculty.model';
import { DesignationService } from '../../designation/designation.service';
import { DesignationMaster } from '../../designation/designation.model';
import { SpecialityService } from '../../speciality/speciality.service';
import { Speciality } from '../../speciality/speciality.model';
import { ProgramService } from '../../program/program.service';
import { DocumentTypeInfo } from '../../program/program.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FACULTY_DOC_CONFIG_TOUR, FACULTY_DOC_CONFIG_FLOW_MAP } from '../../../shared/tour/tours/faculty-doc-config.tours';

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
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
  ],
  templateUrl: './faculty-doc-config.component.html',
  styleUrl: './faculty-doc-config.component.scss',
})
export class FacultyDocConfigComponent implements OnInit {
  private readonly facultyService = inject(FacultyService);
  private readonly specialityService = inject(SpecialityService);
  private readonly designationService = inject(DesignationService);
  private readonly programService = inject(ProgramService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);

  protected readonly rules = signal<FacultyDocumentTypeRequirement[]>([]);
  protected readonly specialities = signal<Speciality[]>([]);
  protected readonly designations = signal<DesignationMaster[]>([]);
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

  protected readonly FACULTY_QUALIFICATION_OPTIONS = FACULTY_QUALIFICATION_OPTIONS;

  protected newDocumentType = '';
  protected newDesignationId: number | null = null;
  protected newSpecialityId: number | null = null;
  protected newQualification: FacultyQualification | '' = '';

  protected get canAddRule(): boolean {
    return (
      !!this.newDocumentType &&
      (!!this.newDesignationId || !!this.newSpecialityId || !!this.newQualification)
    );
  }

  ngOnInit(): void {
    this.tourService.register('faculty-doc-config', FACULTY_DOC_CONFIG_TOUR);
    this.tourService.registerFlowMap('faculty-doc-config', FACULTY_DOC_CONFIG_FLOW_MAP);
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    let pending = 4;
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

    this.specialityService.getAll().subscribe({
      next: (specialities) => { this.specialities.set(specialities); done(); },
      error: () => done(),
    });

    this.designationService.getAll().subscribe({
      next: (designations) => { this.designations.set(designations); done(); },
      error: () => done(),
    });

    this.programService.getAllDocumentTypes().subscribe({
      next: (types) => { this.allDocumentTypes.set(types); done(); },
      error: () => done(),
    });
  }

  protected addRule(): void {
    if (!this.canAddRule) return;

    const request: FacultyDocumentTypeRequirementRequest = {
      documentType: this.newDocumentType,
      designationId: this.newDesignationId ?? undefined,
      specialityId: this.newSpecialityId ?? undefined,
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
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to add requirement rule');
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
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to remove requirement rule'),
    });
  }

  protected criterionLabel(rule: FacultyDocumentTypeRequirement): string {
    if (rule.designationId) return 'Designation';
    if (rule.specialityId) return 'Speciality';
    if (rule.qualification) return 'Qualification';
    return '—';
  }

  protected criterionValue(rule: FacultyDocumentTypeRequirement): string {
    if (rule.designationId) return rule.designationName ?? '—';
    if (rule.specialityId) return rule.specialityName ?? '—';
    if (rule.qualification) return rule.qualificationLabel ?? rule.qualification;
    return '—';
  }

  private resetForm(): void {
    this.newDocumentType = '';
    this.newDesignationId = null;
    this.newSpecialityId = null;
    this.newQualification = '';
  }
}
