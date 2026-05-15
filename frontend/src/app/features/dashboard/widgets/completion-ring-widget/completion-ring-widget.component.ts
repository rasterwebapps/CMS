import { Component, Input, OnInit, inject } from '@angular/core';
import { DocumentSlotsService } from '../../services/document-slots.service';
import { CompletionRingComponent } from '../completion-ring/completion-ring.component';
import { ProfileService } from '../../../../features/profile/profile.service';

@Component({
  selector: 'dash-widget-completion-ring',
  standalone: true,
  imports: [CompletionRingComponent],
  template: `
    <cms-completion-ring [progressPct]="docSlots.progressPct()" />
  `,
  styles: [':host { display: block; height: 100%; }'],
})
export class CompletionRingWidgetComponent implements OnInit {
  @Input() widgetKey?:   string;
  @Input() widgetLabel?: string;
  @Input() widgetIcon?:  string;

  protected readonly docSlots      = inject(DocumentSlotsService);
  private  readonly profileService = inject(ProfileService);

  ngOnInit(): void {
    if (this.docSlots.progressPct() > 0) return;
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
