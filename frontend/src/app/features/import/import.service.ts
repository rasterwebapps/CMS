import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { ImportDefaults, ImportValidationResult, ImportExecuteResult } from './import.model';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/import`;

  downloadTemplate(): void {
    this.http.get(`${this.base}/template`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = 'cms_import_template.xlsx';
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  validate(file: File, defaults: ImportDefaults): Observable<ImportValidationResult> {
    const form = this.buildForm(file, defaults);
    return this.http.post<ImportValidationResult>(`${this.base}/validate`, form);
  }

  execute(file: File, defaults: ImportDefaults): Observable<ImportExecuteResult> {
    const form = this.buildForm(file, defaults);
    return this.http.post<ImportExecuteResult>(`${this.base}/execute`, form);
  }

  private buildForm(file: File, d: ImportDefaults): FormData {
    const form = new FormData();
    form.append('file', file, file.name);
    if (d.defaultJoiningAcademicYearId != null)
      form.append('defaultJoiningAcademicYearId', String(d.defaultJoiningAcademicYearId));
    form.append('defaultStudentType', d.defaultStudentType);
    if (d.defaultNationality) form.append('defaultNationality', d.defaultNationality);
    if (d.defaultState)       form.append('defaultState', d.defaultState);
    form.append('defaultSemester', String(d.defaultSemester));
    form.append('skipErroredRows', String(d.skipErroredRows));
    return form;
  }
}
