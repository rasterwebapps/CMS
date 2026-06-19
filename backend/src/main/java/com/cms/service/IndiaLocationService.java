package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CountryRequest;
import com.cms.dto.CountryResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
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

@Service
@Transactional(readOnly = true)
public class IndiaLocationService {

    private final IndiaStateRepository stateRepository;
    private final IndiaDistrictRepository districtRepository;
    private final LocationCountryRepository countryRepository;

    public IndiaLocationService(IndiaStateRepository stateRepository,
                                IndiaDistrictRepository districtRepository,
                                LocationCountryRepository countryRepository) {
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.countryRepository = countryRepository;
    }

    // ─── Countries ────────────────────────────────────────────────────────────

    public List<CountryResponse> findAllCountries() {
        return countryRepository.findAllByOrderByNameAsc().stream()
            .map(this::toCountryResponse)
            .toList();
    }

    public List<CountryResponse> findActiveCountries() {
        return countryRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toCountryResponse)
            .toList();
    }

    public CountryResponse findCountryById(Long id) {
        return toCountryResponse(fetchCountry(id));
    }

    @Transactional
    public CountryResponse createCountry(CountryRequest request) {
        String name = requireTrimmed(request.name(), "Country name is required");
        String isoCode = requireTrimmed(request.isoCode(), "ISO code is required").toUpperCase();
        if (countryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Country '" + name + "' already exists");
        }
        if (countryRepository.existsByIsoCodeIgnoreCase(isoCode)) {
            throw new IllegalArgumentException("ISO code '" + isoCode + "' already exists");
        }
        LocationCountry country = new LocationCountry(name, isoCode);
        if (request.isActive() != null) country.setIsActive(request.isActive());
        return toCountryResponse(countryRepository.save(country));
    }

    @Transactional
    public CountryResponse updateCountry(Long id, CountryRequest request) {
        LocationCountry country = fetchCountry(id);
        String name = requireTrimmed(request.name(), "Country name is required");
        String isoCode = requireTrimmed(request.isoCode(), "ISO code is required").toUpperCase();
        if (countryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Country '" + name + "' already exists");
        }
        if (countryRepository.existsByIsoCodeIgnoreCaseAndIdNot(isoCode, id)) {
            throw new IllegalArgumentException("ISO code '" + isoCode + "' already exists");
        }
        country.setName(name);
        country.setIsoCode(isoCode);
        if (request.isActive() != null) country.setIsActive(request.isActive());
        return toCountryResponse(countryRepository.save(country));
    }

    @Transactional
    public void deleteCountry(Long id) {
        if (!countryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Country not found with id: " + id);
        }
        countryRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateCountryStatus(Long id, ActiveStatusUpdateRequest request) {
        LocationCountry country = fetchCountry(id);
        country.setIsActive(Boolean.TRUE.equals(request.isActive()));
        LocationCountry saved = countryRepository.save(country);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    // ─── States ──────────────────────────────────────────────────────────────

    public List<IndiaStateResponse> findAllStates() {
        return stateRepository.findAllByOrderByNameAsc().stream()
            .map(this::toStateResponse)
            .toList();
    }

    public List<IndiaStateResponse> findActiveStates() {
        return stateRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toStateResponse)
            .toList();
    }

    public List<IndiaStateResponse> findStatesByCountry(Long countryId) {
        if (!countryRepository.existsById(countryId)) {
            throw new ResourceNotFoundException("Country not found with id: " + countryId);
        }
        return stateRepository.findByCountryIdOrderByNameAsc(countryId).stream()
            .map(this::toStateResponse)
            .toList();
    }

    public List<IndiaStateResponse> findActiveStatesByCountry(Long countryId) {
        if (!countryRepository.existsById(countryId)) {
            throw new ResourceNotFoundException("Country not found with id: " + countryId);
        }
        return stateRepository.findByCountryIdAndIsActiveTrueOrderByNameAsc(countryId).stream()
            .map(this::toStateResponse)
            .toList();
    }

    public IndiaStateResponse findStateById(Long id) {
        return toStateResponse(fetchState(id));
    }

    @Transactional
    public IndiaStateResponse createState(IndiaStateRequest request) {
        LocationCountry country = resolveCountry(request.countryId());
        String name = requireTrimmed(request.name(), "State name is required");
        String code = requireTrimmed(request.code(), "State code is required").toUpperCase();
        if (stateRepository.existsByNameIgnoreCaseAndCountryId(name, country.getId())) {
            throw new IllegalArgumentException("State '" + name + "' already exists in " + country.getName());
        }
        if (stateRepository.existsByCodeIgnoreCaseAndCountryId(code, country.getId())) {
            throw new IllegalArgumentException("State code '" + code + "' already exists in " + country.getName());
        }
        IndiaState state = new IndiaState(name, code, country);
        if (request.isActive() != null) state.setIsActive(request.isActive());
        return toStateResponse(stateRepository.save(state));
    }

    @Transactional
    public IndiaStateResponse updateState(Long id, IndiaStateRequest request) {
        IndiaState state = fetchState(id);
        LocationCountry country = resolveCountry(request.countryId());
        String name = requireTrimmed(request.name(), "State name is required");
        String code = requireTrimmed(request.code(), "State code is required").toUpperCase();
        if (stateRepository.existsByNameIgnoreCaseAndCountryIdAndIdNot(name, country.getId(), id)) {
            throw new IllegalArgumentException("State '" + name + "' already exists in " + country.getName());
        }
        if (stateRepository.existsByCodeIgnoreCaseAndCountryIdAndIdNot(code, country.getId(), id)) {
            throw new IllegalArgumentException("State code '" + code + "' already exists in " + country.getName());
        }
        state.setName(name);
        state.setCode(code);
        state.setCountry(country);
        if (request.isActive() != null) state.setIsActive(request.isActive());
        return toStateResponse(stateRepository.save(state));
    }

    @Transactional
    public void deleteState(Long id) {
        if (!stateRepository.existsById(id)) {
            throw new ResourceNotFoundException("State not found with id: " + id);
        }
        stateRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStateStatus(Long id, ActiveStatusUpdateRequest request) {
        IndiaState state = fetchState(id);
        state.setIsActive(Boolean.TRUE.equals(request.isActive()));
        IndiaState saved = stateRepository.save(state);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    // ─── Districts ───────────────────────────────────────────────────────────

    public List<IndiaDistrictResponse> findDistrictsByState(Long stateId) {
        if (!stateRepository.existsById(stateId)) {
            throw new ResourceNotFoundException("State not found with id: " + stateId);
        }
        return districtRepository.findByStateIdOrderByNameAsc(stateId).stream()
            .map(this::toDistrictResponse)
            .toList();
    }

    public List<IndiaDistrictResponse> findActiveDistrictsByState(Long stateId) {
        if (!stateRepository.existsById(stateId)) {
            throw new ResourceNotFoundException("State not found with id: " + stateId);
        }
        return districtRepository.findByStateIdAndIsActiveTrueOrderByNameAsc(stateId).stream()
            .map(this::toDistrictResponse)
            .toList();
    }

    public IndiaDistrictResponse findDistrictById(Long id) {
        return toDistrictResponse(fetchDistrict(id));
    }

    @Transactional
    public IndiaDistrictResponse createDistrict(Long stateId, IndiaDistrictRequest request) {
        IndiaState state = fetchState(stateId);
        String name = requireTrimmed(request.name(), "District name is required");
        if (districtRepository.existsByStateIdAndNameIgnoreCase(stateId, name)) {
            throw new IllegalArgumentException("District '" + name + "' already exists in this state");
        }
        IndiaDistrict district = new IndiaDistrict(state, name);
        if (request.isActive() != null) district.setIsActive(request.isActive());
        return toDistrictResponse(districtRepository.save(district));
    }

    @Transactional
    public IndiaDistrictResponse updateDistrict(Long id, IndiaDistrictRequest request) {
        IndiaDistrict district = fetchDistrict(id);
        String name = requireTrimmed(request.name(), "District name is required");
        Long stateId = district.getState().getId();
        if (districtRepository.existsByStateIdAndNameIgnoreCaseAndIdNot(stateId, name, id)) {
            throw new IllegalArgumentException("District '" + name + "' already exists in this state");
        }
        // Allow moving district to another state
        if (request.stateId() != null && !request.stateId().equals(stateId)) {
            IndiaState newState = fetchState(request.stateId());
            district.setState(newState);
        }
        district.setName(name);
        if (request.isActive() != null) district.setIsActive(request.isActive());
        return toDistrictResponse(districtRepository.save(district));
    }

    @Transactional
    public void deleteDistrict(Long id) {
        if (!districtRepository.existsById(id)) {
            throw new ResourceNotFoundException("District not found with id: " + id);
        }
        districtRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateDistrictStatus(Long id, ActiveStatusUpdateRequest request) {
        IndiaDistrict district = fetchDistrict(id);
        district.setIsActive(Boolean.TRUE.equals(request.isActive()));
        IndiaDistrict saved = districtRepository.save(district);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Resolves the country: uses the provided countryId if not null, otherwise
     * falls back to India (iso_code = 'IN').
     */
    private LocationCountry resolveCountry(Long countryId) {
        if (countryId != null) {
            return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + countryId));
        }
        return countryRepository.findByIsoCode("IN")
            .orElseThrow(() -> new ResourceNotFoundException("India country record (iso_code=IN) not found"));
    }

    private LocationCountry fetchCountry(Long id) {
        return countryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + id));
    }

    private IndiaState fetchState(Long id) {
        return stateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("State not found with id: " + id));
    }

    private IndiaDistrict fetchDistrict(Long id) {
        return districtRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("District not found with id: " + id));
    }

    private CountryResponse toCountryResponse(LocationCountry c) {
        return new CountryResponse(c.getId(), c.getName(), c.getIsoCode(),
            c.getIsActive(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private IndiaStateResponse toStateResponse(IndiaState s) {
        LocationCountry c = s.getCountry();
        return new IndiaStateResponse(s.getId(), s.getName(), s.getCode(),
            s.getIsActive(), s.getCreatedAt(), s.getUpdatedAt(),
            c != null ? c.getId() : null,
            c != null ? c.getName() : null,
            c != null ? c.getIsoCode() : null);
    }

    private IndiaDistrictResponse toDistrictResponse(IndiaDistrict d) {
        return new IndiaDistrictResponse(d.getId(), d.getState().getId(), d.getState().getName(),
            d.getName(), d.getIsActive(), d.getCreatedAt(), d.getUpdatedAt());
    }

    private static String requireTrimmed(String s, String message) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(message);
        return s.trim();
    }
}

