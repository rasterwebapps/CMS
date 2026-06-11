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
import { IndiaLocationService } from '../india-location.service';
import { Country, IndiaState, IndiaDistrict } from '../india-location.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { INDIA_LOCATION_LIST_TOUR } from '../../../shared/tour/tours/india-location.tours';
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
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsTourButtonComponent,
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
  private readonly tourService = inject(TourService);

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
      ? ['name', 'code', 'country', 'districtCount', 'isActive', 'actions']
      : ['name', 'code', 'country', 'districtCount', 'isActive'],
  );
  protected readonly dataSource = new MatTableDataSource<IndiaState>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>('table');

  // Countries panel
  protected readonly allCountries = signal<Country[]>([]);
  protected readonly expandedCountryId = signal<number | null>(null);
  protected readonly allStates = signal<IndiaState[]>([]);
  private readonly districtsByState = signal<Map<number, IndiaDistrict[]>>(new Map());
  protected readonly expandedStateId = signal<number | null>(null);

  protected readonly totalCount = computed(() => this.allStates().length);
  protected readonly activeCount = computed(() =>
    this.allStates().filter((s) => s.isActive).length,
  );
  protected readonly countryCount = computed(() => this.allCountries().length);

  protected readonly filteredStates = computed(() => {
    const q = this.searchValue().trim().toLowerCase();
    if (!q) return this.allStates();
    return this.allStates().filter(
      (s) =>
        s.name.toLowerCase().includes(q) ||
        s.code.toLowerCase().includes(q) ||
        (s.countryName ?? '').toLowerCase().includes(q),
    );
  });

  protected statesForCountry(countryId: number): IndiaState[] {
    return this.filteredStates().filter((s) => s.countryId === countryId);
  }

  ngOnInit(): void {
    this.tourService.register('india-location-list', INDIA_LOCATION_LIST_TOUR);
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

  protected toggleCountry(countryId: number): void {
    this.expandedCountryId.set(this.expandedCountryId() === countryId ? null : countryId);
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

  protected editCountry(item: Country): void {
    void this.router.navigate(['/india-locations/countries', item.id, 'edit']);
  }

  protected deleteCountry(item: Country): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Delete Country',
          message: `Delete "${item.name}"? All its states and districts will also be removed.`,
          confirmText: 'Delete',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.service.deleteCountry(item.id).subscribe({
            next: () => {
              this.toast.success('Country deleted');
              this.load();
            },
            error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete country'),
          });
        }
      });
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
      void this.router.navigate(['/india-locations/countries/new']);
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.getCountries(false).subscribe({
      next: (countries) => {
        this.allCountries.set(countries);
        // Expand India by default
        const india = countries.find((c) => c.isoCode === 'IN');
        if (india && this.expandedCountryId() === null) {
          this.expandedCountryId.set(india.id);
        }
      },
    });
    this.service.getStates(false).subscribe({
      next: (states) => {
        this.allStates.set(states);
        this.dataSource.data = states;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load location data');
        this.loading.set(false);
      },
    });
  }
}
