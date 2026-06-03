import { Component, Input, OnInit, WritableSignal, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';

interface StatCardData { key: string; value: string; badge: string; trendDelta: number | null; }

// Per-key accent colour tokens (CSS value → bound to --ca on the host element)
const ACCENT: Record<string, string> = {
  'stat-students':           'var(--cms-primary)',
  'stat-faculty':            '#22C55E',
  'stat-labs':               '#A78BFA',
  'stat-fee-collected':      '#F59E0B',
  'stat-outstanding':        '#EF4444',
  'stat-enquiries':          'var(--cms-primary)',
  'stat-admissions':         '#22C55E',
  'stat-specialities':        '#A78BFA',
  'stat-programs':           '#F59E0B',
  'stat-equipment':          'var(--cms-primary)',
  'stat-male-students':      '#3B82F6',
  'stat-female-students':    '#EC4899',
  'stat-management-quota':   '#8B5CF6',
  'stat-counselling-quota':  '#10B981',
  'stat-govt-lapsed-seats':          '#F59E0B',
  'stat-counselling-seats-fill':     '#10B981',
  'stat-management-seats-fill':      '#8B5CF6',
};

const ICONS: Record<string, string> = {
  'stat-students':           'person',
  'stat-faculty':            'groups',
  'stat-labs':               'science',
  'stat-fee-collected':      'payments',
  'stat-outstanding':        'warning',
  'stat-enquiries':          'contact_mail',
  'stat-admissions':         'how_to_reg',
  'stat-specialities':        'business',
  'stat-programs':           'school',
  'stat-equipment':          'devices',
  'stat-male-students':      'person',
  'stat-female-students':    'person_outline',
  'stat-management-quota':   'business_center',
  'stat-counselling-quota':  'groups',
  'stat-govt-lapsed-seats':          'event_seat',
  'stat-counselling-seats-fill':     'how_to_reg',
  'stat-management-seats-fill':      'business_center',
};

@Component({
  selector: 'dash-widget-stat-card',
  standalone: true,
  imports: [DecimalPipe, MatIconModule],
  templateUrl: './stat-card-widget.component.html',
  styleUrl:    './stat-card-widget.component.scss',
  host: {
    '[style.--ca]': 'accentColor',
  },
})
export class StatCardWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading       = signal(true);
  protected readonly error         = signal(false);
  protected readonly data          = signal<StatCardData | null>(null);
  protected readonly animatedValue = signal(0);
  protected readonly isNumeric     = signal(false);

  protected get accentColor(): string {
    return ACCENT[this.widgetKey ?? ''] ?? 'var(--cms-primary)';
  }

  protected get iconName(): string {
    return ICONS[this.widgetKey ?? ''] ?? this.widgetIcon ?? 'widgets';
  }

  ngOnInit(): void {
    const statKey = (this.widgetKey ?? '').replace('stat-', '');
    this.http.get<StatCardData>(`${environment.apiUrl}/dashboard/data/stat/${statKey}`).subscribe({
      next: d => {
        this.data.set(d);
        this.loading.set(false);
        const num = Number(d.value);
        if (!isNaN(num) && !d.value.includes('₹')) {
          this.isNumeric.set(true);
          this.animateCount(this.animatedValue, num);
        }
      },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }

  private animateCount(sig: WritableSignal<number>, target: number): void {
    const duration = 900;
    const start = performance.now();
    const tick = (now: number) => {
      const t    = Math.min((now - start) / duration, 1);
      const ease = 1 - Math.pow(1 - t, 3);
      sig.set(Math.round(ease * target));
      if (t < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }
}
