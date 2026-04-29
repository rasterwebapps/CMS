import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InventoryService } from '../inventory.service';
import { InventoryItemRequest } from '../inventory.model';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsPreviewCardComponent } from '../../../shared/preview-card/preview-card.component';
import { CmsTipsCardComponent, CmsTip } from '../../../shared/tips-card/tips-card.component';

@Component({
  selector: 'app-inventory-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, CmsTourButtonComponent,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    CmsPreviewCardComponent, CmsTipsCardComponent,
  ],
  templateUrl: './inventory-form.component.html',
  styleUrl: './inventory-form.component.scss',
})
export class InventoryFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly inventoryService = inject(InventoryService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Inventory Item');
  protected readonly labs = signal<{ id: number; name: string }[]>([]);

  protected readonly categories = [
    'CONSUMABLE', 'CHEMICAL', 'GLASSWARE', 'TOOL', 'ELECTRONIC_COMPONENT', 'STATIONERY', 'OTHER'];
  protected readonly units = [
    'PIECES', 'LITERS', 'KILOGRAMS', 'METERS', 'BOXES', 'PACKETS', 'SETS'];

  // Preview signals
  protected readonly previewName     = signal('');
  protected readonly previewLabId    = signal<number | null>(null);
  protected readonly previewCategory = signal('');
  protected readonly previewQty      = signal<number | null>(null);
  protected readonly previewUnit     = signal('');
  protected readonly previewMinStock = signal<number | null>(null);
  protected readonly previewLocation = signal('');
  protected readonly previewLabName  = computed(() => {
    const id = this.previewLabId();
    if (!id) return '';
    return this.labs().find(l => l.id === id)?.name ?? '';
  });
  protected readonly lowStock = computed(() => {
    const qty = this.previewQty();
    const min = this.previewMinStock();
    if (qty == null || min == null) return false;
    return qty <= min;
  });

  protected readonly TIPS: CmsTip[] = [
    { icon: 'science',     title: 'Item naming',     subtitle: 'Use the full descriptive name — abbreviations make stock searches harder.' },
    { icon: 'straighten',  title: 'Unit consistency', subtitle: 'Pick the unit you receive shipments in (e.g., Boxes vs. Pieces) for accurate counts.' },
    { icon: 'warning',     title: 'Reorder threshold', subtitle: 'Min. Stock triggers low-stock alerts — set above your typical lead-time consumption.' },
  ];

  private itemId: number | null = null;

  protected readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    labId: [null, Validators.required],
    category: ['', Validators.required],
    quantity: [null, [Validators.required, Validators.min(0)]],
    unit: ['', Validators.required],
    minimumStockLevel: [null, [Validators.min(0)]],
    location: ['', [Validators.maxLength(255)]],
    notes: ['', [Validators.maxLength(500)]],
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => {
        this.previewName.set((v.name ?? '').trim());
        this.previewLabId.set(v.labId ?? null);
        this.previewCategory.set(v.category ?? '');
        this.previewQty.set(v.quantity != null && v.quantity !== '' ? Number(v.quantity) : null);
        this.previewUnit.set(v.unit ?? '');
        this.previewMinStock.set(v.minimumStockLevel != null && v.minimumStockLevel !== '' ? Number(v.minimumStockLevel) : null);
        this.previewLocation.set((v.location ?? '').trim());
      });
  }

  ngOnInit(): void {
    this.http.get<{ id: number; name: string }[]>(`${environment.apiUrl}/labs`).subscribe({
      next: (data) => this.labs.set(data),
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.itemId = Number(id);
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Inventory Item');
      this.loading.set(true);
      this.inventoryService.getById(this.itemId).subscribe({
        next: (item) => {
          this.form.patchValue({
            name: item.name,
            labId: item.labId,
            category: item.category,
            quantity: item.quantity,
            unit: item.unit,
            minimumStockLevel: item.minimumStockLevel,
            location: item.location || '',
            notes: item.notes || '',
          });
          this.loading.set(false);
        },
        error: () => { this.toast.error('Failed to load'); void this.router.navigate(['/inventory']); },
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.value;
    const request: InventoryItemRequest = {
      name: v.name.trim(),
      labId: v.labId,
      category: v.category,
      quantity: v.quantity,
      unit: v.unit,
      minimumStockLevel: v.minimumStockLevel || undefined,
      location: v.location?.trim() || undefined,
      notes: v.notes?.trim() || undefined,
    };
    this.saving.set(true);
    const op$ = this.isEditMode() ? this.inventoryService.update(this.itemId!, request) : this.inventoryService.create(request);
    op$.subscribe({
      next: () => { this.toast.success(this.isEditMode() ? 'Updated' : 'Created'); void this.router.navigate(['/inventory']); },
      error: () => { this.toast.error('Failed to save'); this.saving.set(false); },
    });
  }
}
