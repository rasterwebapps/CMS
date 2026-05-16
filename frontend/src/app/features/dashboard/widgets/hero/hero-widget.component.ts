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

  protected readonly loading       = signal(true);
  protected readonly error         = signal(false);
  protected readonly data          = signal<HeroWidgetData | null>(null);
  protected readonly animatedStats = signal<string[]>([]);

  protected readonly greeting = computed(() => {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  });

  ngOnInit(): void {
    this.http.get<HeroWidgetData>(`${environment.apiUrl}/dashboard/data/hero`).subscribe({
      next: d => {
        this.data.set(d);
        this.loading.set(false);
        // Initialise with raw values, then animate numeric ones
        this.animatedStats.set(d.quickStats.map(s => s.value));
        d.quickStats.forEach((stat, i) => {
          const num = Number(stat.value);
          if (!isNaN(num) && stat.value.trim() !== '') this.animateStatAt(i, num);
        });
      },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }

  private animateStatAt(index: number, target: number): void {
    const duration = 900;
    const start    = performance.now();
    const tick = (now: number) => {
      const t    = Math.min((now - start) / duration, 1);
      const ease = 1 - Math.pow(1 - t, 3);
      this.animatedStats.update(arr => {
        const next = [...arr];
        next[index] = String(Math.round(ease * target));
        return next;
      });
      if (t < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }
}
