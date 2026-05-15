import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';

interface QuickStat   { label: string; value: string; }
interface HeroWidgetData {
  username:   string;
  roleLabel:  string;
  academicYear: string;
  quickStats: QuickStat[];
}

@Component({
  selector: 'dash-widget-hero',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './hero-widget.component.html',
  styleUrl:    './hero-widget.component.scss',
})
export class HeroWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly data    = signal<HeroWidgetData | null>(null);

  protected readonly greeting = computed(() => {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  });

  ngOnInit(): void {
    this.http.get<HeroWidgetData>(`${environment.apiUrl}/dashboard/data/hero`).subscribe({
      next:  d  => { this.data.set(d);  this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}
