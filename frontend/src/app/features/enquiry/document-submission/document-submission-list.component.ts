import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurrencyPipe } from '@angular/common';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { AuthService } from '../../../core/auth/auth.service';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-document-submission-list',
  standalone: true,
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    CurrencyPipe,
    PageHeaderComponent,
  ],
  templateUrl: './document-submission-list.component.html',
  styleUrl: './document-submission-list.component.scss',
})
export class DocumentSubmissionListComponent implements OnInit {
  private readonly enquiryService = inject(EnquiryService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(true);
  protected readonly submitting = signal<number | null>(null);
  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);

  protected readonly displayedColumns = [
    'name',
    'programName',
    'courseName',
    'status',
    'finalizedNetFee',
    'enquiryDate',
    'actions',
  ];

  protected isAdminOrFrontOffice(): boolean {
    return this.authService.hasRole('ROLE_ADMIN') || this.authService.hasRole('ROLE_FRONT_OFFICE');
  }

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.enquiryService.getDocumentPending().subscribe({
      next: (enquiries) => {
        this.dataSource.data = enquiries;
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load enquiries', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  protected viewEnquiry(item: Enquiry): void {
    void this.router.navigate(['/enquiries', item.id]);
  }

  protected submitDocuments(item: Enquiry): void {
    this.submitting.set(item.id);
    this.enquiryService.submitDocuments(item.id).subscribe({
      next: () => {
        this.snackBar.open('Documents submitted successfully', 'Close', { duration: 3000 });
        this.submitting.set(null);
        this.load();
      },
      error: (err) => {
        const message =
          err?.error?.message ?? err?.error?.missingDocumentTypes?.join(', ') ?? 'Failed to submit documents';
        this.snackBar.open(message, 'Close', { duration: 5000 });
        this.submitting.set(null);
      },
    });
  }
}
