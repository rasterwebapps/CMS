import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

interface QuickLink { label: string; route: string; icon: string; }

@Component({
  selector: 'dash-widget-student-quicklinks',
  standalone: true,
  imports: [MatIconModule, RouterLink],
  templateUrl: './student-quicklinks-widget.component.html',
  styleUrl:    './student-quicklinks-widget.component.scss',
})
export class StudentQuickLinksWidgetComponent {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  protected readonly links: QuickLink[] = [
    { label: 'My Documents',  route: '/profile',      icon: 'folder_open' },
    { label: 'My Profile',    route: '/profile',      icon: 'person'      },
    { label: 'My Fees',       route: '/student-fees', icon: 'payments'    },
    { label: 'Attendance',    route: '/attendance',   icon: 'fact_check'  },
    { label: 'Exam Results',  route: '/exam-results', icon: 'grade'       },
    { label: 'Academic Calendar', route: '/academic-calendar', icon: 'calendar_month' },
  ];
}
