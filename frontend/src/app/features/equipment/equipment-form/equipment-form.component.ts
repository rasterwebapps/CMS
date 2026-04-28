import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EquipmentService } from '../equipment.service';
import { EquipmentRequest } from '../equipment.model';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { EQUIPMENT_FORM_TOUR } from '../../../shared/tour/tours/equipment.tours';

@Component({
  selector: 'app-equipment-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    CmsTourButtonComponent],
  templateUrl: './equipment-form.component.html',
  styleUrl: './equipment-form.component.scss',
})
export class EquipmentFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly equipmentService = inject(EquipmentService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Equipment');
  protected readonly labs = signal<{ id: number; name: string }[]>([]);
  protected readonly categories = ['COMPUTER', 'NETWORKING', 'ELECTRONICS', 'MECHANICAL', 'GENERAL'];
  protected readonly statuses = ['AVAILABLE', 'IN_USE', 'UNDER_REPAIR', 'DAMAGED', 'DISPOSED'];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    model: ['', [Validators.maxLength(255)]],
    serialNumber: ['', [Validators.maxLength(100)]],
    labId: [null, Validators.required],
    category: ['', Validators.required],
    status: ['AVAILABLE'],
    purchaseDate: [''],
    purchaseCost: [null, [Validators.min(0)]],
    warrantyExpiry: [''],
  });

  ngOnInit(): void {
    this.tourService.register('equipment-form', EQUIPMENT_FORM_TOUR);
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/labs`).subscribe({
      next: (data) => this.labs.set(data),
      error: () => { this.toast.error('Failed to load labs'); },
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Equipment');
      this.loading.set(true);
      this.equipmentService.getById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({ name: item.name, model: item.model || '', serialNumber: item.serialNumber || '', labId: item.labId, category: item.category, status: item.status, purchaseDate: item.purchaseDate || '', purchaseCost: item.purchaseCost, warrantyExpiry: item.warrantyExpiry || '' });
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load'); void this.router.navigate(['/equipment']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: EquipmentRequest = { name: v.name.trim(), model: v.model?.trim() || undefined, serialNumber: v.serialNumber?.trim() || undefined, labId: v.labId, category: v.category, status: v.status || undefined, purchaseDate: v.purchaseDate || undefined, purchaseCost: v.purchaseCost ?? undefined, warrantyExpiry: v.warrantyExpiry || undefined };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.equipmentService.update(this.itemId!, request) : this.equipmentService.create(request);
    op$.subscribe({
      next: () => { this.toast.success(this.isEditMode() ? 'Updated' : 'Created'); void this.router.navigate(['/equipment']); },
      error: () => { this.toast.error('Failed to save'); this.saving.set(false); },
    });
  }
}
