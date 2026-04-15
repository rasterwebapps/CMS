import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Agent,
  AgentRequest,
  AgentCommissionGuideline,
  AgentCommissionGuidelineRequest,
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
}
