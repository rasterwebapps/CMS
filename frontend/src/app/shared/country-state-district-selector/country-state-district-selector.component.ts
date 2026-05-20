import {
  Component,
  DestroyRef,
  OnInit,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { ReactiveFormsModule, FormGroup } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { IndiaLocationService } from '../../features/india-location/india-location.service';
import { Country, IndiaState, IndiaDistrict } from '../../features/india-location/india-location.model';

/**
 * Reusable cascading Country → State → District selector.
 *
 * Usage:
 *   <cms-country-state-district-selector
 *     [parentForm]="form"
 *     countryControlName="country"
 *     stateControlName="state"
 *     districtControlName="district"
 *   />
 *
 * When Country changes → State is cleared → Districts cleared.
 * When State changes  → District is cleared → Districts reloaded.
 * On load, if India is in the countries list it is pre-selected by default.
 */
@Component({
  selector: 'cms-country-state-district-selector',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './country-state-district-selector.component.html',
  styleUrl: './country-state-district-selector.component.scss',
})
export class CmsCountryStateDistrictSelectorComponent implements OnInit {
  private readonly locationService = inject(IndiaLocationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly parentForm = input.required<FormGroup>();
  readonly countryControlName = input<string>('country');
  readonly stateControlName = input<string>('state');
  readonly districtControlName = input<string>('district');
  readonly countryRequired = input<boolean>(false);
  readonly stateRequired = input<boolean>(false);
  readonly districtRequired = input<boolean>(false);

  protected readonly countries = signal<Country[]>([]);
  protected readonly states = signal<IndiaState[]>([]);
  protected readonly districts = signal<IndiaDistrict[]>([]);
  protected readonly loadingCountries = signal(false);
  protected readonly loadingStates = signal(false);
  protected readonly loadingDistricts = signal(false);

  protected readonly countryCtrl = computed(() =>
    this.parentForm().get(this.countryControlName()),
  );
  protected readonly stateCtrl = computed(() =>
    this.parentForm().get(this.stateControlName()),
  );
  protected readonly districtCtrl = computed(() =>
    this.parentForm().get(this.districtControlName()),
  );

  ngOnInit(): void {
    this.loadCountries();

    // React to country control changes → reload states
    const countryControl = this.parentForm().get(this.countryControlName());
    if (countryControl) {
      if (countryControl.value) {
        this.loadStatesForCountry(countryControl.value as number);
      }
      countryControl.valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((countryId: number | null) => {
          const stateControl = this.parentForm().get(this.stateControlName());
          const districtControl = this.parentForm().get(this.districtControlName());
          if (stateControl) stateControl.setValue('');
          if (districtControl) districtControl.setValue('');
          this.states.set([]);
          this.districts.set([]);
          if (countryId) this.loadStatesForCountry(countryId);
        });
    }

    // React to state control changes → reload districts
    const stateControl = this.parentForm().get(this.stateControlName());
    if (stateControl) {
      stateControl.valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((stateName: string | null) => {
          const districtControl = this.parentForm().get(this.districtControlName());
          if (districtControl) districtControl.setValue('');
          this.districts.set([]);
          if (stateName) this.loadDistrictsForStateName(stateName);
        });
    }
  }

  private loadCountries(): void {
    this.loadingCountries.set(true);
    this.locationService
      .getCountries(true)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => {
          this.loadingCountries.set(false);
          return EMPTY;
        }),
      )
      .subscribe((countries) => {
        this.countries.set(countries);
        this.loadingCountries.set(false);

        // Pre-select India as default if no value is set
        const countryControl = this.parentForm().get(this.countryControlName());
        if (countryControl && !countryControl.value) {
          const india = countries.find((c) => c.isoCode === 'IN');
          if (india) {
            countryControl.setValue(india.id, { emitEvent: true });
          }
        }
      });
  }

  private loadStatesForCountry(countryId: number): void {
    this.loadingStates.set(true);
    this.locationService
      .getStatesByCountry(countryId, true)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => {
          this.loadingStates.set(false);
          return EMPTY;
        }),
      )
      .subscribe((states) => {
        this.states.set(states);
        this.loadingStates.set(false);

        // If state control already has a value, reload districts
        const stateControl = this.parentForm().get(this.stateControlName());
        if (stateControl?.value) {
          this.loadDistrictsForStateName(stateControl.value as string);
        }
      });
  }

  private loadDistrictsForStateName(stateName: string): void {
    const state = this.states().find(
      (s) => s.name.toLowerCase() === stateName.toLowerCase(),
    );
    if (state) {
      this.loadDistrictsForState(state.id);
    }
  }

  private loadDistrictsForState(stateId: number): void {
    this.loadingDistricts.set(true);
    this.locationService
      .getDistricts(stateId, true)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => {
          this.loadingDistricts.set(false);
          return EMPTY;
        }),
      )
      .subscribe((districts) => {
        this.districts.set(districts);
        this.loadingDistricts.set(false);
      });
  }
}

