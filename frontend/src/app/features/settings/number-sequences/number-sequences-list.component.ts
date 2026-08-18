import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconDeleteComponent, CmsIconEditComponent } from '../../../shared/icons';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { NUMBER_SEQUENCES_TOUR, NUMBER_SEQUENCES_FLOW_MAP } from '../../../shared/tour/tours/number-sequences.tours';
import { NumberSeriesDefinition } from './number-series-definition.model';
import { NumberSeriesDefinitionService } from './number-series-definition.service';

@Component({
  selector: 'app-number-sequences-list',
  standalone: true,
  imports: [
    RouterLink,
    TitleCasePipe,
    DecimalPipe,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconDeleteComponent,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './number-sequences-list.component.html',
  styleUrl: './number-sequences-list.component.scss',
})
export class NumberSequencesListComponent implements OnInit, OnDestroy {
  private readonly seriesService = inject(NumberSeriesDefinitionService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);
  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  protected readonly displayedColumns = [
    'seriesName', 'scopeType', 'currentPeriod', 'currentLastGenerated', 'currentNextPreview', 'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<NumberSeriesDefinition>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  private allItems: NumberSeriesDefinition[] = [];

  ngOnInit(): void {
    this.tourService.register('number-sequences-list', NUMBER_SEQUENCES_TOUR);
    this.tourService.registerFlowMap('number-sequences-list', NUMBER_SEQUENCES_FLOW_MAP);
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(q => this.applyLocalFilter(q));
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected clearSearch(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected edit(item: NumberSeriesDefinition): void {
    void this.router.navigate(['/number-sequences', item.id, 'edit']);
  }

  protected canDelete(item: NumberSeriesDefinition): boolean {
    return item.canEditScopeType; // canEditScopeType = true means no counters exist yet
  }

  protected delete(item: NumberSeriesDefinition): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Series',
        message: `Delete "${item.seriesName}"? This cannot be undone.`,
        confirmText: 'Delete', cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(ok => { if (ok) this.doDelete(item); });
  }

  private doDelete(item: NumberSeriesDefinition): void {
    this.seriesService.delete(item.id).subscribe({
      next: () => { this.toast.success('Series deleted'); this.load(); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Cannot delete — numbers already generated.'),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.seriesService.getAll().subscribe({
      next: items => {
        this.allItems = items;
        this.applyLocalFilter(this.searchValue());
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load number series'); this.loading.set(false); },
    });
  }

  private applyLocalFilter(q: string): void {
    const lower = q.trim().toLowerCase();
    this.dataSource.data = lower
      ? this.allItems.filter(i =>
          i.seriesCode.toLowerCase().includes(lower) ||
          i.seriesName.toLowerCase().includes(lower) ||
          i.scopeType.toLowerCase().includes(lower) ||
          (i.description ?? '').toLowerCase().includes(lower))
      : this.allItems;
  }
}
