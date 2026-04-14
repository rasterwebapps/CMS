import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Syllabus,
  SyllabusRequest,
  Experiment,
  ExperimentRequest,
  LabCurriculumMapping,
  LabCurriculumMappingRequest,
} from './curriculum.model';

@Injectable({
  providedIn: 'root',
})
export class CurriculumService {
  private readonly http = inject(HttpClient);
  private readonly syllabiUrl = `${environment.apiUrl}/syllabi`;
  private readonly experimentsUrl = `${environment.apiUrl}/experiments`;
  private readonly mappingsUrl = `${environment.apiUrl}/curriculum-mappings`;

  // Syllabus
  getAllSyllabi(): Observable<Syllabus[]> {
    return this.http.get<Syllabus[]>(this.syllabiUrl);
  }

  getSyllabusById(id: number): Observable<Syllabus> {
    return this.http.get<Syllabus>(`${this.syllabiUrl}/${id}`);
  }

  createSyllabus(request: SyllabusRequest): Observable<Syllabus> {
    return this.http.post<Syllabus>(this.syllabiUrl, request);
  }

  updateSyllabus(id: number, request: SyllabusRequest): Observable<Syllabus> {
    return this.http.put<Syllabus>(`${this.syllabiUrl}/${id}`, request);
  }

  deleteSyllabus(id: number): Observable<void> {
    return this.http.delete<void>(`${this.syllabiUrl}/${id}`);
  }

  // Experiment
  getAllExperiments(): Observable<Experiment[]> {
    return this.http.get<Experiment[]>(this.experimentsUrl);
  }

  getExperimentById(id: number): Observable<Experiment> {
    return this.http.get<Experiment>(`${this.experimentsUrl}/${id}`);
  }

  getExperimentsByCourseId(courseId: number): Observable<Experiment[]> {
    return this.http.get<Experiment[]>(`${this.experimentsUrl}?courseId=${courseId}`);
  }

  createExperiment(request: ExperimentRequest): Observable<Experiment> {
    return this.http.post<Experiment>(this.experimentsUrl, request);
  }

  updateExperiment(id: number, request: ExperimentRequest): Observable<Experiment> {
    return this.http.put<Experiment>(`${this.experimentsUrl}/${id}`, request);
  }

  deleteExperiment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.experimentsUrl}/${id}`);
  }

  // Curriculum Mapping
  getAllMappings(): Observable<LabCurriculumMapping[]> {
    return this.http.get<LabCurriculumMapping[]>(this.mappingsUrl);
  }

  getMappingsByExperimentId(experimentId: number): Observable<LabCurriculumMapping[]> {
    return this.http.get<LabCurriculumMapping[]>(
      `${this.mappingsUrl}?experimentId=${experimentId}`,
    );
  }

  createMapping(request: LabCurriculumMappingRequest): Observable<LabCurriculumMapping> {
    return this.http.post<LabCurriculumMapping>(this.mappingsUrl, request);
  }

  updateMapping(
    id: number,
    request: LabCurriculumMappingRequest,
  ): Observable<LabCurriculumMapping> {
    return this.http.put<LabCurriculumMapping>(`${this.mappingsUrl}/${id}`, request);
  }

  deleteMapping(id: number): Observable<void> {
    return this.http.delete<void>(`${this.mappingsUrl}/${id}`);
  }
}
