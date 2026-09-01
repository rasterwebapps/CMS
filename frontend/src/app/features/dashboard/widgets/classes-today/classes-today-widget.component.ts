import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../../environments';

interface ClassesTodayItem {
  classScheduleId: number;
  subjectName:     string;
  subjectCode:     string;
  sessionType:     'THEORY' | 'LAB' | 'CLINICAL' | 'LIBRARY';
  slotName:        string | null;
  startTime:       string | null;
  endTime:         string | null;
  roomName:        string | null;
  batchName:       string | null;
}

interface ClassesTodayData {
  classes: ClassesTodayItem[];
}

/** OC-127 gap-closure follow-up: replaces the generic WidgetPlaceholderComponent previously shown
 *  for the 'classes-today' registry slot — GET /dashboard/data/classes-today was a stub returning
 *  no real data and had no frontend consumer at all until now. */
@Component({
  selector: 'dash-widget-classes-today',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './classes-today-widget.component.html',
  styleUrl:    './classes-today-widget.component.scss',
  host: { '[style.--ca]': '"#6366F1"' },
})
export class ClassesTodayWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly error   = signal(false);
  protected readonly classes = signal<ClassesTodayItem[]>([]);

  ngOnInit(): void {
    this.http.get<ClassesTodayData>(`${environment.apiUrl}/dashboard/data/classes-today`).subscribe({
      next:  d  => { this.classes.set(d.classes); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }

  protected sessionTypeLabel(type: ClassesTodayItem['sessionType']): string {
    return type === 'THEORY' ? 'Theory' : type === 'LAB' ? 'Lab' : type === 'LIBRARY' ? 'Library' : 'Clinical';
  }
}
