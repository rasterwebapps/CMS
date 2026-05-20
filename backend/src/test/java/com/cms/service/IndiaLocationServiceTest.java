package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CountryRequest;
import com.cms.dto.CountryResponse;
import com.cms.dto.IndiaDistrictRequest;
import com.cms.dto.IndiaDistrictResponse;
import com.cms.dto.IndiaStateRequest;
import com.cms.dto.IndiaStateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.IndiaDistrict;
import com.cms.model.IndiaState;
import com.cms.model.LocationCountry;
import com.cms.repository.IndiaDistrictRepository;
import com.cms.repository.IndiaStateRepository;
import com.cms.repository.LocationCountryRepository;

@ExtendWith(MockitoExtension.class)
class IndiaLocationServiceTest {

    @Mock private IndiaStateRepository stateRepository;
    @Mock private IndiaDistrictRepository districtRepository;
    @Mock private LocationCountryRepository countryRepository;

    private IndiaLocationService service;

    @BeforeEach
    void setUp() {
        service = new IndiaLocationService(stateRepository, districtRepository, countryRepository);
    }

    // ─── Country tests ────────────────────────────────────────────────────────

    @Test
    void shouldCreateCountry() {
        CountryRequest req = new CountryRequest("United States", "US", true);
        LocationCountry saved = country(2L, "United States", "US");

        when(countryRepository.existsByNameIgnoreCase("United States")).thenReturn(false);
        when(countryRepository.existsByIsoCodeIgnoreCase("US")).thenReturn(false);
        when(countryRepository.save(any())).thenReturn(saved);

        CountryResponse res = service.createCountry(req);

        assertThat(res.name()).isEqualTo("United States");
        assertThat(res.isoCode()).isEqualTo("US");
        verify(countryRepository).save(any());
    }

    @Test
    void shouldThrowWhenDuplicateCountryName() {
        when(countryRepository.existsByNameIgnoreCase("India")).thenReturn(true);

        assertThatThrownBy(() -> service.createCountry(new CountryRequest("India", "IN", true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
        verify(countryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDuplicateCountryIsoCode() {
        when(countryRepository.existsByNameIgnoreCase("India2")).thenReturn(false);
        when(countryRepository.existsByIsoCodeIgnoreCase("IN")).thenReturn(true);

        assertThatThrownBy(() -> service.createCountry(new CountryRequest("India2", "IN", true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldFindAllCountries() {
        when(countryRepository.findAllByOrderByNameAsc())
            .thenReturn(List.of(country(1L, "India", "IN")));

        List<CountryResponse> list = service.findAllCountries();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).name()).isEqualTo("India");
    }

    @Test
    void shouldDeleteCountry() {
        when(countryRepository.existsById(2L)).thenReturn(true);

        service.deleteCountry(2L);

        verify(countryRepository).deleteById(2L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentCountry() {
        when(countryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteCountry(99L))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(countryRepository, never()).deleteById(any());
    }

    // ─── State tests ──────────────────────────────────────────────────────────

    @Test
    void shouldCreateState() {
        LocationCountry india = country(1L, "India", "IN");
        IndiaStateRequest req = new IndiaStateRequest("Tamil Nadu", "TN", true, null);
        IndiaState saved = state(1L, "Tamil Nadu", "TN", india);

        when(countryRepository.findByIsoCode("IN")).thenReturn(Optional.of(india));
        when(stateRepository.existsByNameIgnoreCaseAndCountryId("Tamil Nadu", 1L)).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCaseAndCountryId("TN", 1L)).thenReturn(false);
        when(stateRepository.save(any())).thenReturn(saved);

        IndiaStateResponse res = service.createState(req);

        assertThat(res.name()).isEqualTo("Tamil Nadu");
        assertThat(res.code()).isEqualTo("TN");
        verify(stateRepository).save(any());
    }

    @Test
    void shouldCreateStateForNonIndiaCountry() {
        LocationCountry usa = country(2L, "United States", "US");
        IndiaStateRequest req = new IndiaStateRequest("California", "CA", true, 2L);
        IndiaState saved = state(10L, "California", "CA", usa);

        when(countryRepository.findById(2L)).thenReturn(Optional.of(usa));
        when(stateRepository.existsByNameIgnoreCaseAndCountryId("California", 2L)).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCaseAndCountryId("CA", 2L)).thenReturn(false);
        when(stateRepository.save(any())).thenReturn(saved);

        IndiaStateResponse res = service.createState(req);

        assertThat(res.name()).isEqualTo("California");
        assertThat(res.countryName()).isEqualTo("United States");
    }

    @Test
    void shouldThrowWhenDuplicateStateName() {
        LocationCountry india = country(1L, "India", "IN");
        when(countryRepository.findByIsoCode("IN")).thenReturn(Optional.of(india));
        when(stateRepository.existsByNameIgnoreCaseAndCountryId("Tamil Nadu", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createState(new IndiaStateRequest("Tamil Nadu", "TN", true, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(stateRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDuplicateStateCode() {
        LocationCountry india = country(1L, "India", "IN");
        when(countryRepository.findByIsoCode("IN")).thenReturn(Optional.of(india));
        when(stateRepository.existsByNameIgnoreCaseAndCountryId("Tamil Nadu", 1L)).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCaseAndCountryId("TN", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createState(new IndiaStateRequest("Tamil Nadu", "TN", true, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldFindAllStates() {
        LocationCountry india = country(1L, "India", "IN");
        when(stateRepository.findAllByOrderByNameAsc()).thenReturn(List.of(state(1L, "Tamil Nadu", "TN", india)));

        List<IndiaStateResponse> list = service.findAllStates();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).name()).isEqualTo("Tamil Nadu");
    }

    @Test
    void shouldFindActiveStates() {
        LocationCountry india = country(1L, "India", "IN");
        when(stateRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(state(1L, "Tamil Nadu", "TN", india)));

        assertThat(service.findActiveStates()).hasSize(1);
    }

    @Test
    void shouldFindStateById() {
        LocationCountry india = country(1L, "India", "IN");
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state(1L, "Tamil Nadu", "TN", india)));

        IndiaStateResponse res = service.findStateById(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.countryName()).isEqualTo("India");
    }

    @Test
    void shouldThrowWhenStateNotFound() {
        when(stateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findStateById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateState() {
        LocationCountry india = country(1L, "India", "IN");
        IndiaState existing = state(1L, "Tamil Nadu", "TN", india);
        when(stateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(countryRepository.findByIsoCode("IN")).thenReturn(Optional.of(india));
        when(stateRepository.existsByNameIgnoreCaseAndCountryIdAndIdNot("Tamil Nadu Updated", 1L, 1L)).thenReturn(false);
        when(stateRepository.existsByCodeIgnoreCaseAndCountryIdAndIdNot("TNU", 1L, 1L)).thenReturn(false);
        when(stateRepository.save(any())).thenReturn(existing);

        service.updateState(1L, new IndiaStateRequest("Tamil Nadu Updated", "TNU", true, null));

        verify(stateRepository).save(any());
    }

    @Test
    void shouldDeleteState() {
        when(stateRepository.existsById(1L)).thenReturn(true);

        service.deleteState(1L);

        verify(stateRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentState() {
        when(stateRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteState(99L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(stateRepository, never()).deleteById(any());
    }

    // ─── District tests ───────────────────────────────────────────────────────

    @Test
    void shouldCreateDistrict() {
        LocationCountry india = country(1L, "India", "IN");
        IndiaState s = state(1L, "Tamil Nadu", "TN", india);
        IndiaDistrict saved = district(1L, s, "Chennai");

        when(stateRepository.findById(1L)).thenReturn(Optional.of(s));
        when(districtRepository.existsByStateIdAndNameIgnoreCase(1L, "Chennai")).thenReturn(false);
        when(districtRepository.save(any())).thenReturn(saved);

        IndiaDistrictResponse res = service.createDistrict(1L, new IndiaDistrictRequest(1L, "Chennai", true));

        assertThat(res.name()).isEqualTo("Chennai");
        assertThat(res.stateName()).isEqualTo("Tamil Nadu");
    }

    @Test
    void shouldThrowWhenDuplicateDistrict() {
        LocationCountry india = country(1L, "India", "IN");
        IndiaState s = state(1L, "Tamil Nadu", "TN", india);
        when(stateRepository.findById(1L)).thenReturn(Optional.of(s));
        when(districtRepository.existsByStateIdAndNameIgnoreCase(1L, "Chennai")).thenReturn(true);

        assertThatThrownBy(() -> service.createDistrict(1L, new IndiaDistrictRequest(1L, "Chennai", true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldFindActiveDistrictsByState() {
        LocationCountry india = country(1L, "India", "IN");
        IndiaState s = state(1L, "Tamil Nadu", "TN", india);
        when(stateRepository.existsById(1L)).thenReturn(true);
        when(districtRepository.findByStateIdAndIsActiveTrueOrderByNameAsc(1L))
            .thenReturn(List.of(district(1L, s, "Chennai")));

        List<IndiaDistrictResponse> list = service.findActiveDistrictsByState(1L);

        assertThat(list).hasSize(1);
    }

    @Test
    void shouldThrowWhenStateNotFoundForDistricts() {
        when(stateRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findActiveDistrictsByState(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteDistrict() {
        when(districtRepository.existsById(1L)).thenReturn(true);

        service.deleteDistrict(1L);

        verify(districtRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentDistrict() {
        when(districtRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteDistrict(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private LocationCountry country(Long id, String name, String isoCode) {
        LocationCountry c = new LocationCountry(name, isoCode);
        c.setId(id);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private IndiaState state(Long id, String name, String code, LocationCountry country) {
        IndiaState s = new IndiaState(name, code, country);
        s.setId(id);
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }

    private IndiaDistrict district(Long id, IndiaState state, String name) {
        IndiaDistrict d = new IndiaDistrict(state, name);
        d.setId(id);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return d;
    }
}
