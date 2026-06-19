package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.model.IndiaDistrict;
import com.cms.model.IndiaState;
import com.cms.model.LocationCountry;
import com.cms.repository.IndiaDistrictRepository;
import com.cms.repository.IndiaStateRepository;
import com.cms.repository.LocationCountryRepository;

@ExtendWith(MockitoExtension.class)
class IndiaLocationLifecycleStatusToggleTest {

    @Mock
    private IndiaStateRepository stateRepository;
    @Mock
    private IndiaDistrictRepository districtRepository;
    @Mock
    private LocationCountryRepository countryRepository;

    private IndiaLocationService indiaLocationService;

    @BeforeEach
    void setUp() {
        indiaLocationService = new IndiaLocationService(stateRepository, districtRepository, countryRepository);
    }

    @Test
    void shouldDeactivateCountryUsingStatusEndpoint() {
        LocationCountry country = createCountry(1L, true);
        when(countryRepository.findById(1L)).thenReturn(Optional.of(country));
        when(countryRepository.save(any(LocationCountry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = indiaLocationService.updateCountryStatus(1L, new ActiveStatusUpdateRequest(false, "retired"));

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void shouldDeactivateStateUsingStatusEndpoint() {
        IndiaState state = createState(1L, true);
        when(stateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(stateRepository.save(any(IndiaState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = indiaLocationService.updateStateStatus(1L, new ActiveStatusUpdateRequest(false, "retired"));

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void shouldActivateDistrictUsingStatusEndpoint() {
        IndiaDistrict district = createDistrict(1L, false);
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(districtRepository.save(any(IndiaDistrict.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = indiaLocationService.updateDistrictStatus(1L, new ActiveStatusUpdateRequest(true, "reopen"));

        assertThat(response.isActive()).isTrue();
    }

    private LocationCountry createCountry(Long id, boolean isActive) {
        LocationCountry country = new LocationCountry("India", "IN");
        country.setId(id);
        country.setIsActive(isActive);
        Instant now = Instant.now();
        country.setCreatedAt(now);
        country.setUpdatedAt(now);
        return country;
    }

    private IndiaState createState(Long id, boolean isActive) {
        IndiaState state = new IndiaState("Kerala", "KL", createCountry(10L, true));
        state.setId(id);
        state.setIsActive(isActive);
        Instant now = Instant.now();
        state.setCreatedAt(now);
        state.setUpdatedAt(now);
        return state;
    }

    private IndiaDistrict createDistrict(Long id, boolean isActive) {
        IndiaDistrict district = new IndiaDistrict(createState(20L, true), "Ernakulam");
        district.setId(id);
        district.setIsActive(isActive);
        Instant now = Instant.now();
        district.setCreatedAt(now);
        district.setUpdatedAt(now);
        return district;
    }
}

