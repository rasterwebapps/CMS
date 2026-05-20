import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
  ViewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { IndiaLocationService } from '../india-location.service';
import { IndiaState, IndiaDistrict } from '../india-location.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';

@Component({
  selector: 'app-india-location-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    MatExpansionModule,
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
  ],
  templateUrl: './india-location-list.component.html',
  styleUrl: './india-location-list.component.scss',
})
export class IndiaLocationListComponent implements OnInit {
  private readonly service = inject(IndiaLocationService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) {
    if (v) this.dataSource.paginator = v;
  }
  @ViewChild(MatSort) set sort(v: MatSort) {
    if (v) this.dataSource.sort = v;
  }

  protected readonly canManage = computed(() =>
    this.permissionService.has('INDIA_LOCATION_MANAGE'),
  );
  protected readonly displayedColumns = computed(() =>
    this.canManage()
      ? ['name', 'code', 'districtCount', 'isActive', 'actions']
      : ['name', 'code', 'districtCount', 'isActive'],
  );
  protected readonly dataSource = new MatTableDataSource<IndiaState>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>('table');

  private readonly allStates = signal<IndiaState[]>([]);
  private readonly districtsByState = signal<Map<number, IndiaDistrict[]>>(new Map());
  protected readonly expandedStateId = signal<number | null>(null);

  protected readonly totalCount = computed(() => this.allStates().length);
  protected readonly activeCount = computed(() =>
    this.allStates().filter((s) => s.isActive).length,
  );

  protected readonly filteredStates = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allStates();
    return this.allStates().filter(
      (s) =>
        s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q),
    );
  });

  ngOnInit(): void {
    this.load();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.dataSource.filter = '';
  }

  protected getDistricts(stateId: number): IndiaDistrict[] {
    return this.districtsByState().get(stateId) ?? [];
  }

  protected toggleExpand(stateId: number): void {
    if (this.expandedStateId() === stateId) {
      this.expandedStateId.set(null);
      return;
    }
    this.expandedStateId.set(stateId);
    if (!this.districtsByState().has(stateId)) {
      this.service.getDistricts(stateId, false).subscribe({
        next: (districts) => {
          const map = new Map(this.districtsByState());
          map.set(stateId, districts);
          this.districtsByState.set(map);
        },
      });
    }
  }

  protected editState(item: IndiaState): void {
    void this.router.navigate(['/india-locations/states', item.id, 'edit']);
  }

  protected deleteState(item: IndiaState): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete State',
          message: `Delete "${item.name}"? All its districts will also be removed.`,
          confirmText: 'Delete',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.service.deleteState(item.id).subscribe({
            next: () => {
              this.toast.success('State deleted');
              this.load();
            },
            error: () => this.toast.error('Failed to delete state'),
          });
        }
      });
  }

  protected editDistrict(district: IndiaDistrict): void {
    void this.router.navigate(['/india-locations/districts', district.id, 'edit']);
  }

  protected deleteDistrict(district: IndiaDistrict): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete District',
          message: `Delete district "${district.name}" from ${district.stateName}?`,
          confirmText: 'Delete',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.service.deleteDistrict(district.id).subscribe({
            next: () => {
              this.toast.success('District deleted');
              const map = new Map(this.districtsByState());
              const updated = (map.get(district.stateId) ?? []).filter(
                (d) => d.id !== district.id,
              );
              map.set(district.stateId, updated);
              this.districtsByState.set(map);
            },
            error: () => this.toast.error('Failed to delete district'),
          });
        }
      });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else if (this.canManage()) {
      void this.router.navigate(['/india-locations/states/new']);
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.getStates(false).subscribe({
      next: (states) => {
        this.allStates.set(states);
        this.dataSource.data = states;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load states');
        this.loading.set(false);
      },
    });
  }
}

