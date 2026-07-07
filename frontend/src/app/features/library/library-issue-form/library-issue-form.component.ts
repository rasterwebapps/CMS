import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LibraryService } from '../library.service';
import { LibraryBook, LibraryIssueRequest } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { StudentService } from '../../student/student.service';
import { Student } from '../../student/student.model';
import { FacultyService } from '../../faculty/faculty.service';
import { Faculty } from '../../faculty/faculty.model';

@Component({
  selector: 'app-library-issue-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule],
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

  protected readonly saving        = signal(false);
  protected readonly loadingBook   = signal(false);
  protected readonly foundBook     = signal<LibraryBook | null>(null);
  protected readonly bookError     = signal<string | null>(null);
  protected readonly students      = signal<Student[]>([]);
  protected readonly faculty       = signal<Faculty[]>([]);
  protected readonly today         = new Date().toISOString().split('T')[0];

  protected form!: FormGroup;

  protected readonly memberType = signal('STUDENT');

  ngOnInit(): void {
    this.form = this.fb.group({
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

    this.loadStudents();
    this.loadFaculty();
  }

  protected lookupBook(): void {
    const acc = this.form.get('accessionNumber')?.value?.trim();
    if (!acc) return;
    this.loadingBook.set(true);
    this.foundBook.set(null);
    this.bookError.set(null);
    this.libraryService.checkAccessionNumberExists(acc).subscribe({
      next: res => {
        if (!res.exists) {
          this.bookError.set(`No book found with accession number "${acc}"`);
          this.loadingBook.set(false);
          return;
        }
        this.libraryService.getAll().subscribe({
          next: books => {
            const book = books.find(b => b.accessionNumber === acc);
            if (!book) {
              this.bookError.set(`Book "${acc}" not found`);
            } else if (book.status !== 'AVAILABLE') {
              this.bookError.set(`Book "${acc}" is not available (status: ${book.status})`);
            } else {
              this.foundBook.set(book);
            }
            this.loadingBook.set(false);
          },
          error: () => {
            this.bookError.set('Failed to look up book');
            this.loadingBook.set(false);
          },
        });
      },
    });
  }

  protected save(): void {
    if (this.form.invalid || !this.foundBook()) return;

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

    const request: LibraryIssueRequest = {
      bookId:     this.foundBook()!.id,
      memberType: memberType,
      studentId:  memberType === 'STUDENT' ? +v.studentId : undefined,
      facultyId:  memberType === 'FACULTY' ? +v.facultyId : undefined,
      issuedDate: v.issuedDate,
      remarks:    v.remarks?.trim() || undefined,
    };

    this.saving.set(true);
    this.libraryService.issueBook(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Book issued successfully');
          void this.router.navigate(['/library/issues']);
        },
        error: err => {
          this.toast.error(err?.error?.message ?? 'Failed to issue book');
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
