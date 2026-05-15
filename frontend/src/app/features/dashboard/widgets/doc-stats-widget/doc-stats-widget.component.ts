import { Component, Input, OnInit, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { DocumentSlotsService } from '../../services/document-slots.service';
import { DocumentStatsRowComponent } from '../document-stats-row/document-stats-row.component';
import { ProfileService } from '../../../../features/profile/profile.service';

@Component({
  selector: 'dash-widget-doc-stats',
  standalone: true,
  imports: [MatIconModule, DocumentStatsRowComponent],
  template: `
    <cms-document-stats-row
      [stats]="docSlots.docStats()"
      [progressPct]="docSlots.progressPct()"
    />
  `,
  styles: [':host { display: block; }'],
})
export class DocStatsWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  protected readonly docSlots      = inject(DocumentSlotsService);
  private  readonly profileService = inject(ProfileService);

  ngOnInit(): void {
    if (this.docSlots.docStats().total > 0) return;
    this.profileService.getMyProfile().subscribe({
      next: id => {
        if (id.entityType === 'FACULTY' && id.entityId) {
          this.docSlots.loadFaculty(id.entityId);
        } else if (id.entityType === 'STUDENT' && id.entityId) {
          this.docSlots.loadStudent(id.entityId);
        }
      },
    });
  }
}
