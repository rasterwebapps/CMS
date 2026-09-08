import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { CategoryAttribute, CategoryAttributeRequest } from './category-attribute.model';

@Injectable({ providedIn: 'root' })
export class CategoryAttributeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/categories`;

  findByCategory(categoryId: number): Observable<CategoryAttribute[]> {
    return this.http.get<CategoryAttribute[]>(`${this.baseUrl}/${categoryId}/attributes`);
  }

  create(categoryId: number, request: CategoryAttributeRequest): Observable<CategoryAttribute> {
    return this.http.post<CategoryAttribute>(`${this.baseUrl}/${categoryId}/attributes`, request);
  }

  update(categoryId: number, attributeId: number, request: CategoryAttributeRequest): Observable<CategoryAttribute> {
    return this.http.put<CategoryAttribute>(`${this.baseUrl}/${categoryId}/attributes/${attributeId}`, request);
  }

  delete(categoryId: number, attributeId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${categoryId}/attributes/${attributeId}`);
  }
}
