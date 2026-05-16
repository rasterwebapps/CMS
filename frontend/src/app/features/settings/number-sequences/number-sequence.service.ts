import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { NumberSequence } from './number-sequence.model';

@Injectable({ providedIn: 'root' })
export class NumberSequenceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/number-sequences`;

  getAll(): Observable<NumberSequence[]> {
    return this.http.get<NumberSequence[]>(this.baseUrl);
  }
}

