import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  Organization,
  OrganizationRequest,
  Branch,
  BranchRequest,
  Block,
  BlockRequest,
  Floor,
  FloorRequest,
  Zone,
  ZoneRequest,
  Room,
  RoomRequest,
  HostelRoom,
  HostelRoomRequest,
  CampusInfraStatusUpdateRequest,
  CampusInfraStatusUpdateResponse,
} from './campus-infrastructure.model';

@Injectable({ providedIn: 'root' })
export class CampusInfrastructureService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/campus-infrastructure`;

  // ─── Organizations ───────────────────────────────────────────────────────

  getOrganizations(activeOnly = false): Observable<Organization[]> {
    return this.http.get<Organization[]>(`${this.baseUrl}/organizations?activeOnly=${activeOnly}`);
  }

  getOrganizationById(id: number): Observable<Organization> {
    return this.http.get<Organization>(`${this.baseUrl}/organizations/${id}`);
  }

  createOrganization(request: OrganizationRequest): Observable<Organization> {
    return this.http.post<Organization>(`${this.baseUrl}/organizations`, request);
  }

  updateOrganization(id: number, request: OrganizationRequest): Observable<Organization> {
    return this.http.put<Organization>(`${this.baseUrl}/organizations/${id}`, request);
  }

  deleteOrganization(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/organizations/${id}`);
  }

  updateOrganizationStatus(id: number, request: CampusInfraStatusUpdateRequest): Observable<CampusInfraStatusUpdateResponse> {
    return this.http.patch<CampusInfraStatusUpdateResponse>(`${this.baseUrl}/organizations/${id}/status`, request);
  }

  checkOrganizationNameExists(value: string, excludeId?: number): Observable<boolean> {
    const params = excludeId != null ? `&excludeId=${excludeId}` : '';
    return this.http.get<boolean>(`${this.baseUrl}/organizations/name-exists?value=${encodeURIComponent(value)}${params}`);
  }

  checkOrganizationCodeExists(value: string, excludeId?: number): Observable<boolean> {
    const params = excludeId != null ? `&excludeId=${excludeId}` : '';
    return this.http.get<boolean>(`${this.baseUrl}/organizations/code-exists?value=${encodeURIComponent(value)}${params}`);
  }

  // ─── Branches ────────────────────────────────────────────────────────────

  getBranchesByOrganization(organizationId: number, activeOnly = false): Observable<Branch[]> {
    return this.http.get<Branch[]>(`${this.baseUrl}/organizations/${organizationId}/branches?activeOnly=${activeOnly}`);
  }

  getBranchById(id: number): Observable<Branch> {
    return this.http.get<Branch>(`${this.baseUrl}/branches/${id}`);
  }

  createBranch(organizationId: number, request: BranchRequest): Observable<Branch> {
    return this.http.post<Branch>(`${this.baseUrl}/organizations/${organizationId}/branches`, request);
  }

  updateBranch(id: number, request: BranchRequest): Observable<Branch> {
    return this.http.put<Branch>(`${this.baseUrl}/branches/${id}`, request);
  }

  deleteBranch(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/branches/${id}`);
  }

  updateBranchStatus(id: number, request: CampusInfraStatusUpdateRequest): Observable<CampusInfraStatusUpdateResponse> {
    return this.http.patch<CampusInfraStatusUpdateResponse>(`${this.baseUrl}/branches/${id}/status`, request);
  }

  // ─── Blocks ──────────────────────────────────────────────────────────────

  getBlocksByBranch(branchId: number, activeOnly = false): Observable<Block[]> {
    return this.http.get<Block[]>(`${this.baseUrl}/branches/${branchId}/blocks?activeOnly=${activeOnly}`);
  }

  getBlockById(id: number): Observable<Block> {
    return this.http.get<Block>(`${this.baseUrl}/blocks/${id}`);
  }

  createBlock(branchId: number, request: BlockRequest): Observable<Block> {
    return this.http.post<Block>(`${this.baseUrl}/branches/${branchId}/blocks`, request);
  }

  updateBlock(id: number, request: BlockRequest): Observable<Block> {
    return this.http.put<Block>(`${this.baseUrl}/blocks/${id}`, request);
  }

  deleteBlock(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/blocks/${id}`);
  }

  updateBlockStatus(id: number, request: CampusInfraStatusUpdateRequest): Observable<CampusInfraStatusUpdateResponse> {
    return this.http.patch<CampusInfraStatusUpdateResponse>(`${this.baseUrl}/blocks/${id}/status`, request);
  }

  // ─── Floors ──────────────────────────────────────────────────────────────

  getFloorsByBlock(blockId: number, activeOnly = false): Observable<Floor[]> {
    return this.http.get<Floor[]>(`${this.baseUrl}/blocks/${blockId}/floors?activeOnly=${activeOnly}`);
  }

  getFloorById(id: number): Observable<Floor> {
    return this.http.get<Floor>(`${this.baseUrl}/floors/${id}`);
  }

  createFloor(blockId: number, request: FloorRequest): Observable<Floor> {
    return this.http.post<Floor>(`${this.baseUrl}/blocks/${blockId}/floors`, request);
  }

  updateFloor(id: number, request: FloorRequest): Observable<Floor> {
    return this.http.put<Floor>(`${this.baseUrl}/floors/${id}`, request);
  }

  deleteFloor(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/floors/${id}`);
  }

  updateFloorStatus(id: number, request: CampusInfraStatusUpdateRequest): Observable<CampusInfraStatusUpdateResponse> {
    return this.http.patch<CampusInfraStatusUpdateResponse>(`${this.baseUrl}/floors/${id}/status`, request);
  }

  // ─── Zones ───────────────────────────────────────────────────────────────

  /** Flat, campus-wide list of active zones — for pickers that need a zone choice without
   *  walking the Organization/Branch/Block/Floor hierarchy first. */
  getAllActiveZones(): Observable<Zone[]> {
    return this.http.get<Zone[]>(`${this.baseUrl}/zones`);
  }

  getZonesByFloor(floorId: number, activeOnly = false): Observable<Zone[]> {
    return this.http.get<Zone[]>(`${this.baseUrl}/floors/${floorId}/zones?activeOnly=${activeOnly}`);
  }

  getZoneById(id: number): Observable<Zone> {
    return this.http.get<Zone>(`${this.baseUrl}/zones/${id}`);
  }

  createZone(floorId: number, request: ZoneRequest): Observable<Zone> {
    return this.http.post<Zone>(`${this.baseUrl}/floors/${floorId}/zones`, request);
  }

  updateZone(id: number, request: ZoneRequest): Observable<Zone> {
    return this.http.put<Zone>(`${this.baseUrl}/zones/${id}`, request);
  }

  deleteZone(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/zones/${id}`);
  }

  updateZoneStatus(id: number, request: CampusInfraStatusUpdateRequest): Observable<CampusInfraStatusUpdateResponse> {
    return this.http.patch<CampusInfraStatusUpdateResponse>(`${this.baseUrl}/zones/${id}/status`, request);
  }

  // ─── Rooms ───────────────────────────────────────────────────────────────

  getRoomsByZone(zoneId: number, activeOnly = false): Observable<Room[]> {
    return this.http.get<Room[]>(`${this.baseUrl}/zones/${zoneId}/rooms?activeOnly=${activeOnly}`);
  }

  getRoomById(id: number): Observable<Room> {
    return this.http.get<Room>(`${this.baseUrl}/rooms/${id}`);
  }

  createRoom(zoneId: number, request: RoomRequest): Observable<Room> {
    return this.http.post<Room>(`${this.baseUrl}/zones/${zoneId}/rooms`, request);
  }

  updateRoom(id: number, request: RoomRequest): Observable<Room> {
    return this.http.put<Room>(`${this.baseUrl}/rooms/${id}`, request);
  }

  deleteRoom(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/rooms/${id}`);
  }

  updateRoomStatus(id: number, request: CampusInfraStatusUpdateRequest): Observable<CampusInfraStatusUpdateResponse> {
    return this.http.patch<CampusInfraStatusUpdateResponse>(`${this.baseUrl}/rooms/${id}/status`, request);
  }

  // ─── Hostel Room attachment ──────────────────────────────────────────────

  getHostelRoom(roomId: number): Observable<HostelRoom> {
    return this.http.get<HostelRoom>(`${this.baseUrl}/rooms/${roomId}/hostel-room`);
  }

  assignHostelRoom(roomId: number, request: HostelRoomRequest): Observable<HostelRoom> {
    return this.http.put<HostelRoom>(`${this.baseUrl}/rooms/${roomId}/hostel-room`, request);
  }

  unassignHostelRoom(roomId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/rooms/${roomId}/hostel-room`);
  }
}
