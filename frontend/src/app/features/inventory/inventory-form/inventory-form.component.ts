import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { InventoryService } from '../inventory.service';
import { InventoryItemRequest } from '../inventory.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-inventory-form',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCardModule, MatProgressSpinnerModule, MatSnackBarModule,
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
  private readonly snackBar = inject(MatSnackBar);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly isEditMode = signal(false);
  protected readonly pageTitle = signal('Add Inventory Item');
  protected readonly labs = signal<{ id: number; name: string }[]>([]);

  protected readonly categories = [
    'CONSUMABLE', 'CHEMICAL', 'GLASSWARE', 'TOOL', 'ELECTRONIC_COMPONENT', 'STATIONERY', 'OTHER',
  ];
  protected readonly units = [
    'PIECES', 'LITERS', 'KILOGRAMS', 'METERS', 'BOXES', 'PACKETS', 'SETS',
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
        error: () => { this.snackBar.open('Failed to load', 'Close', { duration: 3000 }); void this.router.navigate(['/inventory']); },
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
      next: () => { this.snackBar.open(this.isEditMode() ? 'Updated' : 'Created', 'Close', { duration: 3000 }); void this.router.navigate(['/inventory']); },
      error: () => { this.snackBar.open('Failed to save', 'Close', { duration: 3000 }); this.saving.set(false); },
    });
  }
}
