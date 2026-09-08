import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { ProductService } from '../../features/inventory/product/product.service';
import { Product } from '../../features/inventory/product/product.model';

/**
 * Searchable Product autocomplete — debounced server-side search against the existing
 * ProductService (no dedicated picker endpoint), for forms that need to link a single Product
 * (e.g. VendorProductMapping, RateContractLine). Plain `[(selectedProductId)]` binding rather than
 * a full ControlValueAccessor, matching this codebase's existing plain-select picker pattern
 * (`cms-room-picker`). See the "VendorProductMapping slice" decision-log entry.
 *
 * Usage:
 *   <cms-product-picker [(selectedProductId)]="productId" (selectedProductChange)="onProduct($event)" />
 */
@Component({
  selector: 'cms-product-picker',
  standalone: true,
  imports: [ReactiveFormsModule, MatAutocompleteModule],
  templateUrl: './product-picker.component.html',
})
export class CmsProductPickerComponent implements OnInit, OnChanges {
  private readonly productService = inject(ProductService);

  @Input() selectedProductId: number | null = null;
  @Input() label = 'Product';
  @Input() disabled = false;

  @Output() selectedProductIdChange = new EventEmitter<number | null>();
  @Output() selectedProductChange = new EventEmitter<Product | null>();

  protected readonly searchControl = new FormControl<string | Product>('');
  protected readonly options = signal<Product[]>([]);

  ngOnInit(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((value) => {
          const search = typeof value === 'string' ? value : '';
          if (!search.trim()) {
            this.options.set([]);
            return [];
          }
          return this.productService.getPage({ search, size: 20 });
        })
      )
      .subscribe({ next: (page) => this.options.set(page.content) });

    if (this.disabled) this.searchControl.disable();
    this.loadInitialSelection();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['disabled']) {
      if (this.disabled) this.searchControl.disable();
      else this.searchControl.enable();
    }
    if (changes['selectedProductId'] && !changes['selectedProductId'].firstChange) {
      this.loadInitialSelection();
    }
  }

  protected readonly displayFn = (value: string | Product): string => {
    if (!value || typeof value === 'string') return value ?? '';
    return `${value.productCode} — ${value.productName}`;
  };

  protected onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const product = event.option.value as Product;
    this.selectedProductId = product.id;
    this.selectedProductIdChange.emit(product.id);
    this.selectedProductChange.emit(product);
  }

  private loadInitialSelection(): void {
    if (this.selectedProductId == null) {
      this.searchControl.setValue('', { emitEvent: false });
      return;
    }
    this.productService.getById(this.selectedProductId).subscribe({
      next: (product) => this.searchControl.setValue(product, { emitEvent: false }),
      error: () => this.searchControl.setValue('', { emitEvent: false }),
    });
  }
}
