# Angular Component Skill

You are an expert at creating Angular components for the College Management System frontend. This skill helps you create well-structured, consistent Angular components following project conventions.

## Component Structure

When creating a new Angular component, follow this structure:

### 1. Create Component Files
For a component named `example`, create these files:
- `example.ts` - Component class
- `example.html` - Template
- `example.scss` - Styles
- `example.spec.ts` - Unit tests

### 2. Component Class Pattern

```typescript
import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
// Import Angular Material components as needed
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-example',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
  ],
  templateUrl: './example.html',
  styleUrl: './example.scss',
})
export class ExampleComponent {
  // Use inject() for dependency injection
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  
  // Use signals for reactive state
  private readonly _loading = signal(false);
  private readonly _data = signal<ExampleData[]>([]);
  
  // Expose readonly signals to template
  readonly loading = this._loading.asReadonly();
  readonly data = this._data.asReadonly();
  
  // Use computed for derived state
  readonly isEmpty = computed(() => this._data().length === 0);
  
  // Lifecycle methods
  ngOnInit(): void {
    this.loadData();
  }
  
  // Methods
  loadData(): void {
    this._loading.set(true);
    // Implementation...
  }
}
```

### 3. Template Pattern

```html
<div class="example-container">
  @if (loading()) {
    <mat-spinner></mat-spinner>
  } @else {
    <mat-card>
      <mat-card-header>
        <mat-card-title>Example</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        @for (item of data(); track item.id) {
          <div class="item">{{ item.name }}</div>
        } @empty {
          <p>No items found.</p>
        }
      </mat-card-content>
      <mat-card-actions>
        <button mat-button (click)="handleAction()">Action</button>
      </mat-card-actions>
    </mat-card>
  }
</div>
```

### 4. Style Pattern (SCSS)

```scss
.example-container {
  padding: 16px;
  max-width: 800px;
  margin: 0 auto;
}

.item {
  padding: 8px;
  margin-bottom: 8px;
  border-radius: 4px;
  
  &:hover {
    background-color: var(--mat-sys-surface-variant);
  }
}
```

### 5. Unit Test Pattern

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ExampleComponent } from './example';

describe('ExampleComponent', () => {
  let component: ExampleComponent;
  let fixture: ComponentFixture<ExampleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExampleComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExampleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
```

## Feature Module Organization

Place components under the appropriate feature folder:
```
frontend/src/app/features/
├── dashboard/          # Dashboard feature
├── students/           # Student management
├── faculty/            # Faculty management
├── courses/            # Course management
├── departments/        # Department management
└── labs/               # Lab management
```

## Common Angular Material Imports

```typescript
// Buttons
import { MatButtonModule } from '@angular/material/button';
import { MatIconButton } from '@angular/material/button';

// Layout
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';

// Forms
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

// Data Display
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';

// Feedback
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';

// Navigation
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTabsModule } from '@angular/material/tabs';
```

## Authentication Integration

To check user roles in components:

```typescript
import { AuthService } from '../../../core/auth/auth.service';

export class ProtectedComponent {
  private readonly authService = inject(AuthService);
  
  readonly isAdmin = this.authService.isAdmin;
  readonly currentUser = this.authService.userProfile;
}
```

In templates:
```html
@if (isAdmin()) {
  <button mat-button (click)="adminAction()">Admin Only</button>
}
```

## Routing

Add routes in `app.routes.ts`:

```typescript
export const routes: Routes = [
  {
    path: 'example',
    loadComponent: () => import('./features/example/example').then(m => m.ExampleComponent),
    canActivate: [authGuard],
  },
];
```
