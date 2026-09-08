import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ServiceTicketService } from '../ticket.service';
import { ServiceTicketCreateRequest } from '../ticket.model';
import { ServiceTicketCategoryService } from '../../category/category.service';
import { ServiceTicketCategory } from '../../category/category.model';
import { InventoryLocationService } from '../../../location/inventory-location.service';
import { InventoryLocation } from '../../../location/inventory-location.model';
import { ToastService } from '../../../../../core/toast/toast.service';
import { scrollToFirstInvalid } from '../../../../../shared/utils/scroll-to-invalid';
import { cmsFieldError } from '../../../../../shared/validators/cms-validators';

@Component({
  selector: 'app-service-ticket-new',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './ticket-new.component.html',
  styleUrl: './ticket-new.component.scss',
})
export class ServiceTicketNewComponent implements OnInit {
  private readonly fb              = inject(FormBuilder);
  private readonly router          = inject(Router);
  private readonly ticketService   = inject(ServiceTicketService);
  private readonly categoryService = inject(ServiceTicketCategoryService);
  private readonly locationService = inject(InventoryLocationService);
  private readonly toast           = inject(ToastService);

  protected readonly saving     = signal(false);
  protected readonly locations  = signal<InventoryLocation[]>([]);
  protected readonly categories = signal<ServiceTicketCategory[]>([]);

  protected readonly form: FormGroup = this.fb.group({
    locationId:  [null as number | null, [Validators.required]],
    categoryId:  [null as number | null, [Validators.required]],
    requestedBy: ['', [Validators.required, Validators.maxLength(200)]],
    priority:    ['MEDIUM', [Validators.required]],
    description: ['', [Validators.required, Validators.maxLength(1000)]],
  });

  ngOnInit(): void {
    this.locationService.getAll(true).subscribe({ next: (l) => this.locations.set(l) });
    this.categoryService.getAll(true).subscribe({ next: (c) => this.categories.set(c) });
  }

  protected getErrorMessage(fieldName: string): string {
    const labels: Record<string, string> = {
      locationId: 'Location', categoryId: 'Category', requestedBy: 'Requested by', description: 'Description',
    };
    return cmsFieldError(this.form.get(fieldName), labels[fieldName] ?? fieldName);
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      scrollToFirstInvalid(this.form);
      return;
    }
    const v = this.form.value;
    const request: ServiceTicketCreateRequest = {
      locationId: v.locationId,
      categoryId: v.categoryId,
      requestedBy: v.requestedBy.trim(),
      priority: v.priority,
      description: v.description.trim(),
    };

    this.saving.set(true);
    this.ticketService.create(request).subscribe({
      next: (ticket) => {
        this.toast.success('Service ticket raised');
        this.saving.set(false);
        void this.router.navigate(['/inventory/ticket/tickets', ticket.id]);
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to raise service ticket');
        this.saving.set(false);
      },
    });
  }
}
