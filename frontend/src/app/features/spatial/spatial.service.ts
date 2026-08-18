import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  FloorPlan,
  FloorPlanCalibrationRequest,
  FloorPlanMetadataFields,
  SpatialEquipmentSummary,
  SpatialInventoryItemSummary,
  VirtualLocation,
  VirtualLocationRequest,
} from './spatial.model';

@Injectable({ providedIn: 'root' })
export class SpatialService {
  private readonly http = inject(HttpClient);
  private readonly floorPlansUrl = `${environment.apiUrl}/spatial/floor-plans`;
  private readonly virtualLocationsUrl = `${environment.apiUrl}/spatial/virtual-locations`;
  private readonly equipmentUrl = `${environment.apiUrl}/equipment`;
  private readonly inventoryUrl = `${environment.apiUrl}/inventory`;

  // ─── Floor Plans ─────────────────────────────────────────────────────────

  getFloorPlansByEntity(entityType: string, entityId: number): Observable<FloorPlan[]> {
    const params = new HttpParams().set('entityType', entityType).set('entityId', entityId);
    return this.http.get<FloorPlan[]>(this.floorPlansUrl, { params });
  }

  getFloorPlanById(id: number): Observable<FloorPlan> {
    return this.http.get<FloorPlan>(`${this.floorPlansUrl}/${id}`);
  }

  createFloorPlan(fields: FloorPlanMetadataFields, file: File): Observable<FloorPlan> {
    const formData = this.buildMetadataFormData(fields);
    formData.append('file', file);
    return this.http.post<FloorPlan>(this.floorPlansUrl, formData);
  }

  updateFloorPlan(id: number, fields: FloorPlanMetadataFields): Observable<FloorPlan> {
    return this.http.put<FloorPlan>(`${this.floorPlansUrl}/${id}`, fields);
  }

  replaceFloorPlanFile(id: number, file: File): Observable<FloorPlan> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<FloorPlan>(`${this.floorPlansUrl}/${id}/file`, formData);
  }

  calibrateFloorPlan(id: number, request: FloorPlanCalibrationRequest): Observable<FloorPlan> {
    return this.http.post<FloorPlan>(`${this.floorPlansUrl}/${id}/calibrate`, request);
  }

  deleteFloorPlan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.floorPlansUrl}/${id}`);
  }

  downloadFloorPlanFile(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.floorPlansUrl}/${id}/download`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  // ─── Virtual Locations ───────────────────────────────────────────────────

  getVirtualLocationsByFloorPlan(floorPlanId: number): Observable<VirtualLocation[]> {
    const params = new HttpParams().set('floorPlanId', floorPlanId);
    return this.http.get<VirtualLocation[]>(this.virtualLocationsUrl, { params });
  }

  createVirtualLocation(request: VirtualLocationRequest): Observable<VirtualLocation> {
    return this.http.post<VirtualLocation>(this.virtualLocationsUrl, request);
  }

  updateVirtualLocation(id: number, request: VirtualLocationRequest): Observable<VirtualLocation> {
    return this.http.put<VirtualLocation>(`${this.virtualLocationsUrl}/${id}`, request);
  }

  deleteVirtualLocation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.virtualLocationsUrl}/${id}`);
  }

  // ─── Equipment / Inventory summaries (for marker linking + status badges) ──
  // Hit these endpoints directly rather than reusing features/equipment or
  // features/inventory's services/models — see SpatialEquipmentSummary's doc comment.

  getEquipmentSummaries(): Observable<SpatialEquipmentSummary[]> {
    return this.http.get<SpatialEquipmentSummary[]>(this.equipmentUrl);
  }

  getEquipmentSummaryById(id: number): Observable<SpatialEquipmentSummary> {
    return this.http.get<SpatialEquipmentSummary>(`${this.equipmentUrl}/${id}`);
  }

  getInventoryItemSummaries(): Observable<SpatialInventoryItemSummary[]> {
    return this.http.get<SpatialInventoryItemSummary[]>(this.inventoryUrl);
  }

  getInventoryItemSummaryById(id: number): Observable<SpatialInventoryItemSummary> {
    return this.http.get<SpatialInventoryItemSummary>(`${this.inventoryUrl}/${id}`);
  }

  private buildMetadataFormData(fields: FloorPlanMetadataFields): FormData {
    const formData = new FormData();
    formData.append('entityType', fields.entityType);
    formData.append('entityId', String(fields.entityId));
    formData.append('name', fields.name);
    formData.append('unitSystem', fields.unitSystem);
    formData.append('originAnchor', fields.originAnchor);
    formData.append('originX', String(fields.originX));
    formData.append('originY', String(fields.originY));
    if (fields.viewboxWidth != null) formData.append('viewboxWidth', String(fields.viewboxWidth));
    if (fields.viewboxHeight != null) formData.append('viewboxHeight', String(fields.viewboxHeight));
    return formData;
  }
}
