import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { LibraryService } from '../library.service';
import { LibrarySubjectCategory } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';

export interface LibrarySubjectCategoryDialogData {
  category?: LibrarySubjectCategory;
}

@Component({
  selector: 'app-library-subject-category-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatSlideToggleModule],
  templateUrl: './library-subject-category-dialog.component.html',
  styleUrl: './library-subject-category-dialog.component.scss',
})
export class LibrarySubjectCategoryDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<LibrarySubjectCategoryDialogComponent>);
  protected readonly data: LibrarySubjectCategoryDialogData = inject(MAT_DIALOG_DATA, { optional: true }) ?? {};
  private readonly libraryService = inject(LibraryService);
  private readonly toast = inject(ToastService);

  protected readonly saving = signal(false);
  protected readonly isEdit = signal(false);
  protected form!: FormGroup;

  ngOnInit(): void {
    const cat = this.data.category;
    this.isEdit.set(!!cat);

    this.form = this.fb.group({
      name: [cat?.name ?? '', [Validators.required, Validators.maxLength(100)]],
      code: [cat?.code ?? '', [Validators.maxLength(50)]],
      description: [cat?.description ?? '', [Validators.maxLength(500)]],
      isActive: [cat?.isActive ?? true],
    });
  }

  protected save(): void {
    if (this.form.invalid || this.saving()) return;

    this.saving.set(true);
    const val = this.form.value;
    const req = {
      name: val.name.trim(),
      code: val.code?.trim() || undefined,
      description: val.description?.trim() || undefined,
      isActive: val.isActive,
    };

    const action$ = this.isEdit()
      ? this.libraryService.updateSubjectCategory(this.data.category!.id, req)
      : this.libraryService.createSubjectCategory(req);

    action$.subscribe({
      next: (res) => {
        this.saving.set(false);
        this.toast.success(`Category "${res.name}" ${this.isEdit() ? 'updated' : 'created'} successfully`);
        this.dialogRef.close(res);
      },
      error: (err) => {
        this.saving.set(false);
        const msg = err.error?.message || err.error?.detail || err.message || 'Failed to save subject category';
        this.toast.error(msg);
      },
    });
  }

  protected cancel(): void {
    this.dialogRef.close();
  }
}
