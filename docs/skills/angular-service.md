# Angular Service Skill

You are an expert at creating Angular services for the College Management System frontend. This skill helps you create well-structured HTTP services and state management services.

## HTTP Service Pattern

### Basic HTTP Service

```typescript
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Student {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  departmentId: number;
  enrollmentDate: string;
}

export interface StudentRequest {
  firstName: string;
  lastName: string;
  email: string;
  departmentId: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/students`;
  
  // Reactive state using signals
  private readonly _students = signal<Student[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  
  readonly students = this._students.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  getAll(page: number = 0, size: number = 10): Observable<PageResponse<Student>> {
    this._loading.set(true);
    this._error.set(null);
    
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<Student>>(this.baseUrl, { params }).pipe(
      tap(response => {
        this._students.set(response.content);
        this._loading.set(false);
      }),
      catchError(error => {
        this._loading.set(false);
        this._error.set(error.message || 'Failed to load students');
        return throwError(() => error);
      })
    );
  }

  getById(id: number): Observable<Student> {
    return this.http.get<Student>(`${this.baseUrl}/${id}`);
  }

  create(request: StudentRequest): Observable<Student> {
    return this.http.post<Student>(this.baseUrl, request).pipe(
      tap(student => {
        this._students.update(students => [...students, student]);
      })
    );
  }

  update(id: number, request: StudentRequest): Observable<Student> {
    return this.http.put<Student>(`${this.baseUrl}/${id}`, request).pipe(
      tap(updated => {
        this._students.update(students =>
          students.map(s => s.id === id ? updated : s)
        );
      })
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this._students.update(students =>
          students.filter(s => s.id !== id)
        );
      })
    );
  }
}
```

## State Management Service Pattern

For complex state that spans multiple components:

```typescript
import { Injectable, signal, computed } from '@angular/core';

export interface AppState {
  sidenavOpen: boolean;
  currentTheme: 'light' | 'dark';
  notifications: Notification[];
}

@Injectable({ providedIn: 'root' })
export class AppStateService {
  // Private writable signals
  private readonly _sidenavOpen = signal(true);
  private readonly _currentTheme = signal<'light' | 'dark'>('light');
  private readonly _notifications = signal<Notification[]>([]);
  
  // Public readonly signals
  readonly sidenavOpen = this._sidenavOpen.asReadonly();
  readonly currentTheme = this._currentTheme.asReadonly();
  readonly notifications = this._notifications.asReadonly();
  
  // Computed values
  readonly unreadNotificationCount = computed(() =>
    this._notifications().filter(n => !n.read).length
  );
  
  // Actions
  toggleSidenav(): void {
    this._sidenavOpen.update(open => !open);
  }
  
  setTheme(theme: 'light' | 'dark'): void {
    this._currentTheme.set(theme);
    document.body.classList.toggle('dark-theme', theme === 'dark');
  }
  
  addNotification(notification: Notification): void {
    this._notifications.update(notifications => [...notifications, notification]);
  }
  
  markAsRead(id: string): void {
    this._notifications.update(notifications =>
      notifications.map(n =>
        n.id === id ? { ...n, read: true } : n
      )
    );
  }
  
  clearNotifications(): void {
    this._notifications.set([]);
  }
}
```

## Service File Organization

```
frontend/src/app/
├── core/
│   ├── auth/
│   │   └── auth.service.ts       # Authentication service
│   ├── services/
│   │   ├── api.service.ts        # Base HTTP service
│   │   └── notification.service.ts
│   └── state/
│       └── app-state.service.ts  # Global state
├── features/
│   ├── students/
│   │   ├── services/
│   │   │   └── student.service.ts
│   │   └── student-list/
│   ├── faculty/
│   │   └── services/
│   │       └── faculty.service.ts
│   └── ...
```

## Error Handling

Create a centralized error handling service:

```typescript
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class ErrorHandlerService {
  private readonly snackBar = inject(MatSnackBar);

  handleError(error: HttpErrorResponse): void {
    let message: string;
    
    if (error.status === 0) {
      message = 'Unable to connect to server. Please check your connection.';
    } else if (error.status === 401) {
      message = 'Your session has expired. Please log in again.';
    } else if (error.status === 403) {
      message = 'You do not have permission to perform this action.';
    } else if (error.status === 404) {
      message = 'The requested resource was not found.';
    } else if (error.status >= 500) {
      message = 'A server error occurred. Please try again later.';
    } else if (error.error?.message) {
      message = error.error.message;
    } else {
      message = 'An unexpected error occurred.';
    }
    
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar'],
    });
  }
}
```

## Testing Services

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { StudentService, Student } from './student.service';
import { environment } from '../../../../environments/environment';

describe('StudentService', () => {
  let service: StudentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        StudentService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    
    service = TestBed.inject(StudentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch students', () => {
    const mockStudents: Student[] = [
      { id: 1, firstName: 'John', lastName: 'Doe', email: 'john@example.com', departmentId: 1, enrollmentDate: '2024-01-01' },
    ];

    service.getAll().subscribe(response => {
      expect(response.content).toEqual(mockStudents);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/students?page=0&size=10`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: mockStudents, totalElements: 1, totalPages: 1, size: 10, number: 0 });
  });

  it('should create a student', () => {
    const newStudent = { firstName: 'Jane', lastName: 'Doe', email: 'jane@example.com', departmentId: 1 };
    const createdStudent: Student = { ...newStudent, id: 2, enrollmentDate: '2024-01-01' };

    service.create(newStudent).subscribe(student => {
      expect(student).toEqual(createdStudent);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/students`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newStudent);
    req.flush(createdStudent);
  });
});
```

## Best Practices

1. **Use `providedIn: 'root'`** for singleton services
2. **Use signals for reactive state** instead of BehaviorSubjects
3. **Expose readonly signals** to prevent external mutations
4. **Handle errors gracefully** with user-friendly messages
5. **Use environment variables** for API URLs
6. **Write unit tests** for all service methods
7. **Keep services focused** on a single responsibility
