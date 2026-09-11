import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  ProductVariant,
  ProductVariantRequest,
  ProductVariantStatusUpdateRequest,
  ProductVariantStatusUpdateResponse,
} from './product-variant.model';

@Injectable({ providedIn: 'root' })
export class ProductVariantService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  private productUrl(productId: number): string {
    return `${this.apiUrl}/inventory/products/${productId}/variants`;
  }

  findByProduct(productId: number): Observable<ProductVariant[]> {
    return this.http.get<ProductVariant[]>(this.productUrl(productId));
  }

  getById(productId: number, id: number): Observable<ProductVariant> {
    return this.http.get<ProductVariant>(`${this.productUrl(productId)}/${id}`);
  }

  create(productId: number, request: ProductVariantRequest): Observable<ProductVariant> {
    return this.http.post<ProductVariant>(this.productUrl(productId), request);
  }

  update(productId: number, id: number, request: ProductVariantRequest): Observable<ProductVariant> {
    return this.http.put<ProductVariant>(`${this.productUrl(productId)}/${id}`, request);
  }

  delete(productId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.productUrl(productId)}/${id}`);
  }

  updateStatus(productId: number, id: number, request: ProductVariantStatusUpdateRequest): Observable<ProductVariantStatusUpdateResponse> {
    return this.http.patch<ProductVariantStatusUpdateResponse>(`${this.productUrl(productId)}/${id}/status`, request);
  }

  checkCodeExists(productId: number, value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.productUrl(productId)}/code-exists`, { params });
  }

  checkBarcodeExists(productId: number, value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.productUrl(productId)}/barcode-exists`, { params });
  }

  /** Not nested under a product — a scan workflow doesn't know the parent productId upfront. */
  findByBarcode(value: string): Observable<ProductVariant> {
    const params = new HttpParams().set('value', value);
    return this.http.get<ProductVariant>(`${this.apiUrl}/inventory/product-variants/by-barcode`, { params });
  }

  /** A printable label PNG. A plain `<img src>` can't carry the auth header — fetch as a blob
   *  and bind an object URL, same pattern as ProductService.getBarcodePng. */
  getBarcodePng(variantId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/inventory/product-variants/${variantId}/barcode.png`, { responseType: 'blob' });
  }
}
