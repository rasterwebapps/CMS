import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Agent,
  AgentRequest,
  AgentStatusUpdateRequest,
  AgentStatusUpdateResponse,
  AgentCommissionGuideline,
  AgentCommissionGuidelineRequest,
  Page,
} from './agent.model';

@Injectable({
  providedIn: 'root',
})
export class AgentService {
  private readonly http = inject(HttpClient);
  private readonly agentUrl = `${environment.apiUrl}/agents`;
  private readonly guidelineUrl = `${environment.apiUrl}/agent-commission-guidelines`;

  getAgents(): Observable<Agent[]> {
    return this.http.get<Agent[]>(this.agentUrl);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Agent>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort)   params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Agent>>(`${this.agentUrl}/page`, { params });
  }

  getActiveAgents(): Observable<Agent[]> {
    return this.http.get<Agent[]>(`${this.agentUrl}?active=true`);
  }

  getAgentById(id: number): Observable<Agent> {
    return this.http.get<Agent>(`${this.agentUrl}/${id}`);
  }

  createAgent(request: AgentRequest): Observable<Agent> {
    return this.http.post<Agent>(this.agentUrl, request);
  }

  updateAgent(id: number, request: AgentRequest): Observable<Agent> {
    return this.http.put<Agent>(`${this.agentUrl}/${id}`, request);
  }

  deleteAgent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.agentUrl}/${id}`);
  }

  deactivateAgent(id: number): Observable<AgentStatusUpdateResponse> {
    return this.updateStatus(id, { isActive: false });
  }

  reactivateAgent(id: number): Observable<AgentStatusUpdateResponse> {
    return this.updateStatus(id, { isActive: true });
  }

  updateStatus(
    id: number,
    request: AgentStatusUpdateRequest,
  ): Observable<AgentStatusUpdateResponse> {
    return this.http.patch<AgentStatusUpdateResponse>(`${this.agentUrl}/${id}/status`, request);
  }

  getGuidelines(): Observable<AgentCommissionGuideline[]> {
    return this.http.get<AgentCommissionGuideline[]>(this.guidelineUrl);
  }

  getGuidelinesByAgent(agentId: number): Observable<AgentCommissionGuideline[]> {
    return this.http.get<AgentCommissionGuideline[]>(`${this.guidelineUrl}?agentId=${agentId}`);
  }

  getGuidelineById(id: number): Observable<AgentCommissionGuideline> {
    return this.http.get<AgentCommissionGuideline>(`${this.guidelineUrl}/${id}`);
  }

  createGuideline(
    request: AgentCommissionGuidelineRequest
  ): Observable<AgentCommissionGuideline> {
    return this.http.post<AgentCommissionGuideline>(this.guidelineUrl, request);
  }

  updateGuideline(
    id: number,
    request: AgentCommissionGuidelineRequest
  ): Observable<AgentCommissionGuideline> {
    return this.http.put<AgentCommissionGuideline>(`${this.guidelineUrl}/${id}`, request);
  }

  deleteGuideline(id: number): Observable<void> {
    return this.http.delete<void>(`${this.guidelineUrl}/${id}`);
  }

  exportAgents(format: 'excel' | 'pdf', filters: { search?: string | null } = {}): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (filters.search) params = params.set('search', filters.search);
    return this.http.get(`${this.agentUrl}/export`, { params, responseType: 'blob' });
  }
}
