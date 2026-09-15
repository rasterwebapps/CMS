import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  InventoryBin,
  InventoryBinRequest,
  InventoryRack,
  InventoryRackBinStatusUpdateResponse,
  InventoryRackRequest,
  Page,
} from './inventory-rack.model';

@Injectable({ providedIn: 'root' })
export class InventoryRackService {
  private readonly http = inject(HttpClient);
  private readonly racksUrl = `${environment.apiUrl}/inventory/racks`;
  private readonly binsUrl = `${environment.apiUrl}/inventory/bins`;

  // ── Racks ────────────────────────────────────────────────────

  getRacks(locationId?: number, activeOnly = false): Observable<InventoryRack[]> {
    let params = new HttpParams().set('activeOnly', activeOnly);
    if (locationId != null) params = params.set('locationId', locationId);
    return this.http.get<InventoryRack[]>(this.racksUrl, { params });
  }

  getRacksPage(p: { search?: string; locationId?: number | null; page?: number; size?: number; sort?: string; direction?: string }): Observable<Page<InventoryRack>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<InventoryRack>>(`${this.racksUrl}/page`, { params });
  }

  getRackById(id: number): Observable<InventoryRack> {
    return this.http.get<InventoryRack>(`${this.racksUrl}/${id}`);
  }

  createRack(request: InventoryRackRequest): Observable<InventoryRack> {
    return this.http.post<InventoryRack>(this.racksUrl, request);
  }

  updateRack(id: number, request: InventoryRackRequest): Observable<InventoryRack> {
    return this.http.put<InventoryRack>(`${this.racksUrl}/${id}`, request);
  }

  deleteRack(id: number): Observable<void> {
    return this.http.delete<void>(`${this.racksUrl}/${id}`);
  }

  updateRackStatus(id: number, isActive: boolean): Observable<InventoryRackBinStatusUpdateResponse> {
    return this.http.patch<InventoryRackBinStatusUpdateResponse>(`${this.racksUrl}/${id}/status`, { isActive });
  }

  checkRackNameExists(value: string, locationId: number, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value).set('locationId', locationId);
    if (excludeId != null) params = params.set('excludeId', excludeId);
    return this.http.get<boolean>(`${this.racksUrl}/name-exists`, { params });
  }

  checkRackCodeExists(value: string, locationId: number, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value).set('locationId', locationId);
    if (excludeId != null) params = params.set('excludeId', excludeId);
    return this.http.get<boolean>(`${this.racksUrl}/code-exists`, { params });
  }

  // ── Bins ─────────────────────────────────────────────────────

  getBins(rackId?: number, locationId?: number, activeOnly = false): Observable<InventoryBin[]> {
    let params = new HttpParams().set('activeOnly', activeOnly);
    if (rackId != null) params = params.set('rackId', rackId);
    if (locationId != null) params = params.set('locationId', locationId);
    return this.http.get<InventoryBin[]>(this.binsUrl, { params });
  }

  getBinsPage(p: { search?: string; rackId?: number | null; page?: number; size?: number; sort?: string; direction?: string }): Observable<Page<InventoryBin>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.rackId != null) params = params.set('rackId', p.rackId);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<InventoryBin>>(`${this.binsUrl}/page`, { params });
  }

  getBinById(id: number): Observable<InventoryBin> {
    return this.http.get<InventoryBin>(`${this.binsUrl}/${id}`);
  }

  createBin(request: InventoryBinRequest): Observable<InventoryBin> {
    return this.http.post<InventoryBin>(this.binsUrl, request);
  }

  updateBin(id: number, request: InventoryBinRequest): Observable<InventoryBin> {
    return this.http.put<InventoryBin>(`${this.binsUrl}/${id}`, request);
  }

  deleteBin(id: number): Observable<void> {
    return this.http.delete<void>(`${this.binsUrl}/${id}`);
  }

  updateBinStatus(id: number, isActive: boolean): Observable<InventoryRackBinStatusUpdateResponse> {
    return this.http.patch<InventoryRackBinStatusUpdateResponse>(`${this.binsUrl}/${id}/status`, { isActive });
  }

  checkBinNameExists(value: string, rackId: number, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value).set('rackId', rackId);
    if (excludeId != null) params = params.set('excludeId', excludeId);
    return this.http.get<boolean>(`${this.binsUrl}/name-exists`, { params });
  }

  checkBinCodeExists(value: string, rackId: number, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value).set('rackId', rackId);
    if (excludeId != null) params = params.set('excludeId', excludeId);
    return this.http.get<boolean>(`${this.binsUrl}/code-exists`, { params });
  }
}
