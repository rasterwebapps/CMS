import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Country,
  CountryRequest,
  IndiaState,
  IndiaDistrict,
  IndiaStateRequest,
  IndiaDistrictRequest,
  IndiaLocationStatusUpdateRequest,
  IndiaLocationStatusUpdateResponse,
} from './india-location.model';

@Injectable({ providedIn: 'root' })
export class IndiaLocationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/india`;

  // ─── Countries ───────────────────────────────────────────────────────────

  getCountries(activeOnly = false): Observable<Country[]> {
    return this.http.get<Country[]>(`${this.baseUrl}/countries?activeOnly=${activeOnly}`);
  }

  getCountryById(id: number): Observable<Country> {
    return this.http.get<Country>(`${this.baseUrl}/countries/${id}`);
  }

  createCountry(request: CountryRequest): Observable<Country> {
    return this.http.post<Country>(`${this.baseUrl}/countries`, request);
  }

  updateCountry(id: number, request: CountryRequest): Observable<Country> {
    return this.http.put<Country>(`${this.baseUrl}/countries/${id}`, request);
  }

  deleteCountry(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/countries/${id}`);
  }

  updateCountryStatus(
    id: number,
    request: IndiaLocationStatusUpdateRequest,
  ): Observable<IndiaLocationStatusUpdateResponse> {
    return this.http.patch<IndiaLocationStatusUpdateResponse>(
      `${this.baseUrl}/countries/${id}/status`,
      request,
    );
  }

  /** Get states belonging to a specific country (cascade on country select). */
  getStatesByCountry(countryId: number, activeOnly = true): Observable<IndiaState[]> {
    return this.http.get<IndiaState[]>(
      `${this.baseUrl}/countries/${countryId}/states?activeOnly=${activeOnly}`,
    );
  }

  // ─── States ──────────────────────────────────────────────────────────────

  getStates(activeOnly = true): Observable<IndiaState[]> {
    return this.http.get<IndiaState[]>(`${this.baseUrl}/states?activeOnly=${activeOnly}`);
  }

  getStateById(id: number): Observable<IndiaState> {
    return this.http.get<IndiaState>(`${this.baseUrl}/states/${id}`);
  }

  createState(request: IndiaStateRequest): Observable<IndiaState> {
    return this.http.post<IndiaState>(`${this.baseUrl}/states`, request);
  }

  updateState(id: number, request: IndiaStateRequest): Observable<IndiaState> {
    return this.http.put<IndiaState>(`${this.baseUrl}/states/${id}`, request);
  }

  deleteState(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/states/${id}`);
  }

  updateStateStatus(
    id: number,
    request: IndiaLocationStatusUpdateRequest,
  ): Observable<IndiaLocationStatusUpdateResponse> {
    return this.http.patch<IndiaLocationStatusUpdateResponse>(
      `${this.baseUrl}/states/${id}/status`,
      request,
    );
  }

  // ─── Districts ───────────────────────────────────────────────────────────

  getDistricts(stateId: number, activeOnly = true): Observable<IndiaDistrict[]> {
    return this.http.get<IndiaDistrict[]>(
      `${this.baseUrl}/states/${stateId}/districts?activeOnly=${activeOnly}`,
    );
  }

  getDistrictById(id: number): Observable<IndiaDistrict> {
    return this.http.get<IndiaDistrict>(`${this.baseUrl}/districts/${id}`);
  }

  createDistrict(stateId: number, request: IndiaDistrictRequest): Observable<IndiaDistrict> {
    return this.http.post<IndiaDistrict>(`${this.baseUrl}/states/${stateId}/districts`, request);
  }

  updateDistrict(id: number, request: IndiaDistrictRequest): Observable<IndiaDistrict> {
    return this.http.put<IndiaDistrict>(`${this.baseUrl}/districts/${id}`, request);
  }

  deleteDistrict(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/districts/${id}`);
  }

  updateDistrictStatus(
    id: number,
    request: IndiaLocationStatusUpdateRequest,
  ): Observable<IndiaLocationStatusUpdateResponse> {
    return this.http.patch<IndiaLocationStatusUpdateResponse>(
      `${this.baseUrl}/districts/${id}/status`,
      request,
    );
  }
}

