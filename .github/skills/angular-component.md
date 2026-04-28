# Angular Component Skill

You are an expert at creating Angular components for the College Management System frontend. This skill helps you create well-structured, consistent Angular components following project conventions.

## Design System & UI/UX Guidelines

All components must follow the CMS modern design language. The global design tokens are defined in `frontend/src/styles.scss` as CSS custom properties (prefixed `--cms-`).

### Design Principles

1. **Color Palette**: Use soft `--cms-bg-shell` (slate-50) for page backgrounds. Use `--cms-bg-card` (white) for content cards. Use `--cms-primary` (indigo-600) for primary actions. Avoid harsh borders.
2. **Typography**: Use `Inter`, `Roboto`, or `system-ui` font stack. Ensure strong visual hierarchy — subtle gray for table headers (uppercase, small, letter-spaced), bold dark text for primary data.
3. **Depth & Elevation**: Use `--cms-shadow-md` for cards; avoid flat outlined cards. Use smooth rounded corners (`--cms-radius-lg` = 16px for cards, `--cms-radius-sm` = 8px for buttons/inputs).
4. **Interactions**: Add `transition: var(--cms-transition)` (all 0.2s ease-in-out) for hover states on buttons, table rows, and navigation links.
5. **Spacing**: Use generous padding (32px–40px page padding, 20px–24px card padding). Tables should breathe with 14px–20px cell padding.

### Component Patterns

#### Page Layout (List Pages) — MLP Pattern

**All list/master-entry screens use the MLP (Master List Page) layout.** Never use the old `feature-list-page` / `page-header` structure. Never use focus mode.

```html
<div class="mlp-page">
  <!-- Page header -->
  <div class="mlp-hdr anim-rise">
    <div class="mlp-hdr-left">
      <h1 class="mlp-title">Resource <em>Name</em></h1>
      <p class="mlp-sub">Brief description</p>
      @if (!loading()) {
        <div class="mlp-meta">
          <span class="mlp-stat"><span class="mlp-stat-dot mlp-stat-dot--blue"></span>{{ total() }} Total</span>
        </div>
      }
    </div>
    <button class="btn-primary" routerLink="/resource/new">
      <svg ...><!-- plus icon --></svg>
      Add Resource
    </button>
  </div>

  <!-- Toolbar: filters + search + view toggle -->
  <div class="mlp-toolbar anim-rise anim-rise--d1">
    <select class="mlp-filter-select" aria-label="Filter by ...">
      <option value="">All</option>
    </select>
    <div class="search-bar" role="search">
      <!-- SVG search icon, input, clear button -->
    </div>
    <!-- ALWAYS use <cms-view-toggle> last in the toolbar — never raw mlp-seg -->
    <cms-view-toggle [mode]="viewMode()" storageKey="resource-list-view-mode" (modeChange)="setViewMode($event)" />
  </div>

  <!-- Table view — content-card + mlp-table-card fills full screen height, paginator at bottom -->
  <div class="content-card mlp-table-card">
    <table mat-table [dataSource]="dataSource" matSort class="modern-table">
      <!-- columns -->
      <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
      <tr mat-row *matRowDef="let row; columns: displayedColumns" class="table-row"></tr>
      <tr class="mat-row" *matNoDataRow>
        <td class="mat-cell" [attr.colspan]="displayedColumns.length" style="padding: 0">
          <cms-empty-state icon="category" title="No items yet" subtitle="Add your first item to get started" />
        </td>
      </tr>
    </table>
    <mat-paginator [pageSizeOptions]="[5, 10, 25, 50]" [pageSize]="10" showFirstLastButtons />
  </div>
</div>
```

**Layout guarantees provided by global CSS (do not override):**
- `.mlp-page` → `min-height: calc(100vh - 91px)` — page fills the full viewport.
- `.content-card.mlp-table-card` → `flex: 1` + `mat-paginator { margin-top: auto }` — card stretches to bottom of screen; paginator is always pinned at the bottom regardless of row count.
- `CmsViewToggleComponent :host` → `margin-left: auto` — toggle is always right-aligned in the toolbar.

**No component-level SCSS is needed for these layout concerns.** Global styles handle them.

#### Content Card

Use custom `.content-card` class instead of `<mat-card>`:

```scss
.content-card {
  background: var(--cms-bg-card);
  border-radius: var(--cms-radius-lg);
  box-shadow: var(--cms-shadow-md);
  border: 1px solid var(--cms-border-light);
  overflow: hidden;
}
```

#### Search Bar (Command-bar style)

Use a custom search input instead of `mat-form-field` for search fields:

```html
<div class="search-bar">
  <mat-icon class="search-bar__icon">search</mat-icon>
  <input class="search-bar__input" placeholder="Search..." type="text" />
</div>
```

```scss
.search-bar {
  display: flex;
  align-items: center;
  background: var(--cms-bg-muted);
  border-radius: var(--cms-radius-sm);
  border: 1.5px solid transparent;
  padding: 0 12px;
  height: 40px;
  transition: var(--cms-transition);

  &:focus-within {
    border-color: var(--cms-primary);
    box-shadow: 0 0 0 3px var(--cms-primary-ring);
    background: var(--cms-bg-card);
  }
}
```

#### Primary & Secondary Buttons

Use custom button classes instead of `mat-flat-button`:

```scss
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--cms-primary);
  color: #fff;
  border: none;
  border-radius: var(--cms-radius-sm);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: var(--cms-transition);
  box-shadow: var(--cms-shadow-sm);

  &:hover { background: var(--cms-primary-hover); box-shadow: var(--cms-shadow-md); transform: translateY(-1px); }
}

.btn-secondary {
  padding: 10px 20px;
  background: var(--cms-bg-card);
  color: var(--cms-text-secondary);
  border: 1px solid var(--cms-border-default);
  border-radius: var(--cms-radius-sm);
  cursor: pointer;
  transition: var(--cms-transition);

  &:hover { background: var(--cms-bg-hover); }
}
```

#### Data Table Styling

```scss
.modern-table {
  width: 100%;

  th.mat-header-cell {
    font-size: 0.6875rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--cms-text-table-header);
    background: var(--cms-bg-muted);
    border-bottom: 1px solid var(--cms-border-default);
    padding: 12px 20px;
  }

  td.mat-cell {
    padding: 14px 20px;
    border-bottom: 1px solid var(--cms-border-light);
    font-size: 0.875rem;
    color: var(--cms-text-secondary);
  }

  tr.table-row {
    transition: var(--cms-transition);
    &:hover { background-color: var(--cms-bg-hover); }
  }
}
```

#### Badge / Pill Components

Use global `.cms-badge` classes defined in `styles.scss`:

```html
<span class="cms-badge cms-badge--blue">4 total</span>
<span class="cms-badge cms-badge--gray">3T</span>
<span class="cms-badge cms-badge--green">Active</span>
<span class="cms-badge cms-badge--amber">Sem 3</span>
```

#### Row Actions (Hover-reveal)

```scss
.row-actions {
  opacity: 0.35;
  transition: var(--cms-transition);
}

tr.table-row:hover .row-actions { opacity: 1; }

.action-btn {
  width: 32px; height: 32px;
  border: none; background: transparent;
  border-radius: var(--cms-radius-sm);
  color: var(--cms-text-muted);
  transition: var(--cms-transition);

  &:hover { background: var(--cms-bg-hover); color: var(--cms-primary); }
  &--danger:hover { background: #fef2f2; color: #dc2626; }
}
```

#### Form Layout (Form Pages)

```html
<div class="form-card">
  <form class="entity-form">
    <div class="form-section">
      <h3 class="form-section__title">Section Title</h3>
      <div class="form-grid">
        <!-- mat-form-field inputs here -->
      </div>
    </div>
    <div class="form-actions">
      <button class="btn-secondary">Cancel</button>
      <button class="btn-primary">Save</button>
    </div>
  </form>
</div>
```

```scss
.form-card {
  background: var(--cms-bg-card);
  border-radius: var(--cms-radius-lg);
  box-shadow: var(--cms-shadow-md);
  border: 1px solid var(--cms-border-light);
  padding: 32px;
}

.form-section__title {
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--cms-text-table-header);
  border-bottom: 1px solid var(--cms-border-light);
  padding-bottom: 10px;
}
```

#### Empty State

```html
<div class="empty-state__content">
  <mat-icon class="empty-state__icon">icon_name</mat-icon>
  <p class="empty-state__text">No items available</p>
</div>
```

### Dark Mode

All `--cms-*` tokens automatically adapt to dark mode via CSS custom property overrides in `styles.scss`. Never hardcode light-mode colors; always use the design tokens.

### Responsive Design

- Pages: `padding: 32px 40px` on desktop, `20px 16px` on mobile (breakpoint 768px)
- Form grids: Use CSS Grid with responsive column rules
- Tables: Wrap in `.table-wrapper { overflow-x: auto; }`
- Toolbars: Use `flex-wrap: wrap` with column direction on small screens (640px)

---

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
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-example',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
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

### 3. Template Pattern (List Page)

Use the MLP pattern — see "Page Layout (List Pages)" above. Minimal example:

```html
<div class="mlp-page">
  <div class="mlp-hdr anim-rise">
    <div class="mlp-hdr-left">
      <h1 class="mlp-title">Examples <em>List</em></h1>
      <p class="mlp-sub">Manage all examples</p>
    </div>
    <button class="btn-primary" routerLink="/examples/new">Add Example</button>
  </div>

  <div class="mlp-toolbar anim-rise anim-rise--d1">
    <div class="search-bar" role="search">
      <input class="search-bar__input" [value]="searchValue()" (input)="applyFilter($event)"
             placeholder="Search examples..." type="text" aria-label="Search examples" />
    </div>
    <cms-view-toggle [mode]="viewMode()" storageKey="example-list-view-mode" (modeChange)="setViewMode($event)" />
  </div>

  @if (loading()) {
    <div class="mlp-loading-grid">
      @for (n of [1,2,3,4,5,6]; track n) { <div class="mlp-card-skeleton"></div> }
    </div>
  } @else if (viewMode() === 'card') {
    <div class="mlp-cards-grid">
      @for (item of filteredItems(); track item.id) {
        <div class="mlp-card">{{ item.name }}</div>
      }
    </div>
  } @else {
    <div class="content-card mlp-table-card">
      <table mat-table [dataSource]="dataSource" matSort class="modern-table">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
          <td mat-cell *matCellDef="let item">{{ item.name }}</td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef class="actions-header"></th>
          <td mat-cell *matCellDef="let item">
            <div class="row-actions">
              <button class="action-btn" (click)="edit(item)">Edit</button>
              <button class="action-btn action-btn--danger" (click)="delete(item)">Delete</button>
            </div>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns" class="table-row"></tr>
        <tr class="mat-row" *matNoDataRow>
          <td class="mat-cell" [attr.colspan]="displayedColumns.length" style="padding:0">
            <cms-empty-state icon="category" title="No examples yet" subtitle="Add your first example" />
          </td>
        </tr>
      </table>
      <mat-paginator [pageSizeOptions]="[5, 10, 25, 50]" [pageSize]="10" showFirstLastButtons />
    </div>
  }
</div>
```

### 4. Style Pattern (SCSS)

For list screens, **no page-level SCSS padding or layout is needed** — all layout is handled by global MLP classes in `styles.scss`. Only add component-specific overrides (card content, custom badge colors, etc.):

```scss
// ✅ Correct — only component-specific styles
.resource-card__title { font-weight: 600; }

// ❌ Wrong — do not override MLP layout in component SCSS
// .mlp-page { padding: ... }
// .content-card { flex: ... }
```

For form/detail pages, use the `.detail-page` global class and add local card styles as needed.

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
