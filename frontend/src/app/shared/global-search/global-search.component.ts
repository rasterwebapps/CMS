import {
  Component,
  inject,
  signal,
  ElementRef,
  ViewChild,
  HostListener,
  OnDestroy,
} from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';
import { LowerCasePipe } from '@angular/common';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../environments';

export interface SearchResultItem {
  type: string;
  id: number;
  label: string;
  sublabel: string;
  route: string;
}

export interface SearchResponse {
  results: SearchResultItem[];
}

const TYPE_ICONS: Record<string, string> = {
  STUDENT: 'school',
  FACULTY: 'groups',
  ENQUIRY: 'contact_mail',
  DEPARTMENT: 'business',
};

@Component({
  selector: 'app-global-search',
  standalone: true,
  imports: [MatIconModule, LowerCasePipe],
  templateUrl: './global-search.component.html',
  styleUrl: './global-search.component.scss',
})
export class GlobalSearchComponent implements OnDestroy {
  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly expanded = signal(false);
  protected readonly loading = signal(false);
  protected readonly results = signal<SearchResultItem[]>([]);
  protected readonly activeIndex = signal(-1);
  protected readonly query = signal('');

  private readonly search$ = new Subject<string>();
  private readonly sub = this.search$
    .pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((q) => {
        if (q.trim().length < 2) {
          this.results.set([]);
          this.loading.set(false);
          return of(null);
        }
        this.loading.set(true);
        const params = new HttpParams().set('q', q.trim()).set('limit', '10');
        return this.http.get<SearchResponse>(`${environment.apiUrl}/search`, { params });
      }),
    )
    .subscribe({
      next: (resp) => {
        this.loading.set(false);
        if (resp) {
          this.results.set(resp.results);
          this.activeIndex.set(-1);
        }
      },
      error: () => {
        this.loading.set(false);
      },
    });

  protected typeIcon(type: string): string {
    return TYPE_ICONS[type] ?? 'search';
  }

  protected onInput(event: Event): void {
    const q = (event.target as HTMLInputElement).value;
    this.query.set(q);
    this.search$.next(q);
  }

  protected onFocus(): void {
    this.expanded.set(true);
  }

  protected open(): void {
    this.expanded.set(true);
    setTimeout(() => this.searchInput?.nativeElement.focus(), 50);
  }

  protected close(): void {
    this.expanded.set(false);
    this.results.set([]);
    this.activeIndex.set(-1);
    this.query.set('');
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
    }
  }

  protected onKeyDown(event: KeyboardEvent): void {
    const list = this.results();
    if (event.key === 'Escape') {
      this.close();
      return;
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeIndex.update((i) => Math.min(i + 1, list.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeIndex.update((i) => Math.max(i - 1, 0));
    } else if (event.key === 'Enter') {
      const idx = this.activeIndex();
      if (idx >= 0 && idx < list.length) {
        this.navigate(list[idx]);
      }
    }
  }

  protected navigate(item: SearchResultItem): void {
    this.router.navigateByUrl(item.route);
    this.close();
  }

  @HostListener('document:keydown', ['$event'])
  onGlobalKeyDown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault();
      this.open();
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const el = event.target as HTMLElement;
    if (this.expanded() && !el.closest('app-global-search')) {
      this.close();
    }
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
