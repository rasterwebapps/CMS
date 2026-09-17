import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { AnnouncementService } from '../../features/announcement/announcement.service';
import { Announcement } from '../../features/announcement/announcement.model';

/** Small reusable feed of the current user's own announcements (GET /announcements/my, resolved
 *  server-side by role/cohort/section/ward membership). Generic by design -- the Announcements
 *  module itself is institution-wide -- but only wired into the parent dashboard for now, per
 *  this round's scope; dropping it into another screen later needs no changes here. */
@Component({
  selector: 'app-announcement-widget',
  standalone: true,
  imports: [DatePipe, MatIconModule],
  templateUrl: './announcement-widget.component.html',
  styleUrl: './announcement-widget.component.scss',
})
export class AnnouncementWidgetComponent implements OnInit {
  private readonly announcementService = inject(AnnouncementService);

  protected readonly announcements = signal<Announcement[]>([]);
  protected readonly loading = signal(false);
  protected readonly expandedId = signal<number | null>(null);

  ngOnInit(): void {
    this.loading.set(true);
    this.announcementService.getMyFeed().subscribe({
      next: (rows) => { this.announcements.set(rows); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected toggle(announcement: Announcement): void {
    const isOpen = this.expandedId() === announcement.id;
    this.expandedId.set(isOpen ? null : announcement.id);
    if (!isOpen && !announcement.read) {
      this.announcementService.markRead(announcement.id).subscribe({
        next: () => this.announcements.update((list) =>
          list.map((a) => (a.id === announcement.id ? { ...a, read: true } : a)),
        ),
        error: () => {},
      });
    }
  }
}
