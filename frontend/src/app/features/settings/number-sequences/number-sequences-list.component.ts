import { Component, OnInit, ViewChild, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { NUMBER_SEQUENCES_TOUR } from '../../../shared/tour/tours/number-sequences.tours';
import { NumberSequence } from './number-sequence.model';
import { NumberSequenceService } from './number-sequence.service';

@Component({
  selector: 'app-number-sequences-list',
  standalone: true,
  imports: [
    FormsModule,
    AppDatePipe,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './number-sequences-list.component.html',
  styleUrl: './number-sequences-list.component.scss',
})
export class NumberSequencesListComponent implements OnInit {
  private readonly numberSequenceService = inject(NumberSequenceService);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }

  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = [
    'seriesName',
    'scopeKey',
    'lastGeneratedNumber',
    'nextPreviewNumber',
    'lastSequence',
    'updatedAt',
  ];
  protected readonly dataSource = new MatTableDataSource<NumberSequence>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  private readonly allSequences = signal<NumberSequence[]>([]);

  protected readonly filteredSequences = computed(() => {
    const search = this.searchValue().trim().toLowerCase();
    if (!search) return this.allSequences();
    return this.allSequences().filter((sequence) =>
      [
        sequence.seriesCode,
        sequence.seriesName,
        sequence.scopeType,
        sequence.scopeKey,
        sequence.lastGeneratedNumber,
        sequence.nextPreviewNumber,
        sequence.description ?? '',
      ]
        .join(' ')
        .toLowerCase()
        .includes(search)
    );
  });

  protected readonly totalCount = computed(() => this.allSequences().length);
  protected readonly filteredCount = computed(() => this.filteredSequences().length);
  protected readonly hasActiveFilters = computed(() => this.searchValue().trim() !== '');

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredSequences();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });
  }

  ngOnInit(): void {
    this.tourService.register('number-sequences-list', NUMBER_SEQUENCES_TOUR);
    this.load();
  }

  protected clearSearch(): void {
    this.searchValue.set('');
  }

  private load(): void {
    this.loading.set(true);
    this.numberSequenceService.getAll().subscribe({
      next: (data) => {
        this.allSequences.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load number sequences');
        this.loading.set(false);
      },
    });
  }
}

