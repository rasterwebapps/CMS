import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LibraryService } from '../library.service';
import { LibraryCirculationLookup, LibraryIssueRequest, LibraryItemType } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { StudentService } from '../../student/student.service';
import { Student } from '../../student/student.model';
import { FacultyService } from '../../faculty/faculty.service';
import { Faculty } from '../../faculty/faculty.model';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { LIBRARY_ISSUE_FORM_TOUR, LIBRARY_ISSUE_FORM_FLOW_MAP } from '../../../shared/tour/tours/library-circulation.tours';

@Component({
  selector: 'app-library-issue-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, CmsTourButtonComponent],
  templateUrl: './library-issue-form.component.html',
  styleUrl: './library-issue-form.component.scss',
})
export class LibraryIssueFormComponent implements OnInit {
  private readonly fb             = inject(FormBuilder);
  private readonly router         = inject(Router);
  private readonly libraryService = inject(LibraryService);
  private readonly studentService = inject(StudentService);
  private readonly facultyService = inject(FacultyService);
  private readonly toast          = inject(ToastService);
  private readonly destroyRef     = inject(DestroyRef);
  private readonly tourService    = inject(TourService);

  protected readonly saving        = signal(false);
  protected readonly loadingItem   = signal(false);
  protected readonly foundItem     = signal<LibraryCirculationLookup | null>(null);
  protected readonly itemError     = signal<string | null>(null);
  protected readonly students      = signal<Student[]>([]);
  protected readonly faculty       = signal<Faculty[]>([]);
  protected readonly today         = new Date().toISOString().split('T')[0];

  protected form!: FormGroup;

  protected readonly memberType = signal('STUDENT');
  protected readonly itemType   = signal<LibraryItemType>('BOOK');

  ngOnInit(): void {
    this.tourService.register('library-issue-form', LIBRARY_ISSUE_FORM_TOUR);
    this.tourService.registerFlowMap('library-issue-form', LIBRARY_ISSUE_FORM_FLOW_MAP);

    this.form = this.fb.group({
      itemType:        ['BOOK', Validators.required],
      accessionNumber: ['', Validators.required],
      memberType:      ['STUDENT', Validators.required],
      studentId:       [null],
      facultyId:       [null],
      issuedDate:      [this.today, Validators.required],
      remarks:         [''],
    });

    this.form.get('memberType')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => this.memberType.set(value));

    this.form.get('itemType')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        this.itemType.set(value);
        this.foundItem.set(null);
        this.itemError.set(null);
        this.form.get('accessionNumber')!.setValue('');
      });

    this.loadStudents();
    this.loadFaculty();
  }

  protected lookupItem(): void {
    const acc = this.form.get('accessionNumber')?.value?.trim();
    if (!acc) return;
    this.loadingItem.set(true);
    this.foundItem.set(null);
    this.itemError.set(null);
    this.libraryService.lookupByAccessionNumber(acc).subscribe({
      next: item => {
        if (item.itemType !== this.itemType()) {
          this.itemError.set(
            `"${acc}" is a ${item.itemType === 'BOOK' ? 'book' : 'journal'}, not a ${this.itemType() === 'BOOK' ? 'book' : 'journal'}`);
        } else if (item.status !== 'AVAILABLE') {
          this.itemError.set(`"${acc}" is not available (status: ${item.status})`);
        } else {
          this.foundItem.set(item);
        }
        this.loadingItem.set(false);
      },
      error: () => {
        this.itemError.set(`No book or journal found with accession number "${acc}"`);
        this.loadingItem.set(false);
      },
    });
  }

  protected save(): void {
    if (this.form.invalid || !this.foundItem()) return;

    const v = this.form.value;
    const memberType = v.memberType;

    if (memberType === 'STUDENT' && !v.studentId) {
      this.toast.error('Please select a student');
      return;
    }
    if (memberType === 'FACULTY' && !v.facultyId) {
      this.toast.error('Please select a faculty member');
      return;
    }

    const item = this.foundItem()!;
    const request: LibraryIssueRequest = {
      itemType:     item.itemType,
      bookId:       item.itemType === 'BOOK' ? item.itemId : undefined,
      periodicalId: item.itemType === 'JOURNAL' ? item.itemId : undefined,
      memberType:   memberType,
      studentId:    memberType === 'STUDENT' ? +v.studentId : undefined,
      facultyId:    memberType === 'FACULTY' ? +v.facultyId : undefined,
      issuedDate:   v.issuedDate,
      remarks:      v.remarks?.trim() || undefined,
    };

    this.saving.set(true);
    this.libraryService.issueBook(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success(`${item.itemType === 'BOOK' ? 'Book' : 'Journal'} issued successfully`);
          void this.router.navigate(['/library/issues']);
        },
        error: err => {
          this.toast.error(err?.error?.message ?? 'Failed to issue');
          this.saving.set(false);
        },
      });
  }

  protected cancel(): void {
    void this.router.navigate(['/library/issues']);
  }

  protected get f() { return this.form.controls; }

  private loadStudents(): void {
    this.studentService.getAll().subscribe({
      next: s => this.students.set(s.filter(st => st.status === 'ACTIVE')),
      error: () => {},
    });
  }

  private loadFaculty(): void {
    this.facultyService.getAll().subscribe({
      next: f => this.faculty.set(f.filter(fc => fc.status === 'ACTIVE')),
      error: () => {},
    });
  }
}
