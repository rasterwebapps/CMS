import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { ProductImage } from './product-image.model';

@Injectable({ providedIn: 'root' })
export class ProductImageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/products`;

  findByProduct(productId: number): Observable<ProductImage[]> {
    return this.http.get<ProductImage[]>(`${this.baseUrl}/${productId}/images`);
  }

  upload(productId: number, file: File, setPrimary = false): Observable<ProductImage> {
    const formData = new FormData();
    formData.append('file', file);
    const params = new HttpParams().set('setPrimary', setPrimary);
    return this.http.post<ProductImage>(`${this.baseUrl}/${productId}/images`, formData, { params });
  }

  setPrimary(productId: number, imageId: number): Observable<ProductImage> {
    return this.http.post<ProductImage>(`${this.baseUrl}/${productId}/images/${imageId}/set-primary`, {});
  }

  delete(productId: number, imageId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${productId}/images/${imageId}`);
  }

  download(productId: number, imageId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.baseUrl}/${productId}/images/${imageId}/download`, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}
