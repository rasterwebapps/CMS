package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.IndiaDistrictRequest;
import com.cms.dto.IndiaDistrictResponse;
import com.cms.dto.IndiaStateRequest;
import com.cms.dto.IndiaStateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.IndiaDistrict;
import com.cms.model.IndiaState;
import com.cms.repository.IndiaDistrictRepository;
import com.cms.repository.IndiaStateRepository;

@Service
@Transactional(readOnly = true)
public class IndiaLocationService {

    private final IndiaStateRepository stateRepository;
    private final IndiaDistrictRepository districtRepository;

    public IndiaLocationService(IndiaStateRepository stateRepository,
                                IndiaDistrictRepository districtRepository) {
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
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

    public IndiaStateResponse findStateById(Long id) {
        return toStateResponse(fetchState(id));
    }

    @Transactional
    public IndiaStateResponse createState(IndiaStateRequest request) {
        String name = requireTrimmed(request.name(), "State name is required");
        String code = requireTrimmed(request.code(), "State code is required").toUpperCase();
        if (stateRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("State '" + name + "' already exists");
        }
        if (stateRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("State code '" + code + "' already exists");
        }
        IndiaState state = new IndiaState(name, code);
        if (request.isActive() != null) state.setIsActive(request.isActive());
        return toStateResponse(stateRepository.save(state));
    }

    @Transactional
    public IndiaStateResponse updateState(Long id, IndiaStateRequest request) {
        IndiaState state = fetchState(id);
        String name = requireTrimmed(request.name(), "State name is required");
        String code = requireTrimmed(request.code(), "State code is required").toUpperCase();
        if (stateRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("State '" + name + "' already exists");
        }
        if (stateRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("State code '" + code + "' already exists");
        }
        state.setName(name);
        state.setCode(code);
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

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private IndiaState fetchState(Long id) {
        return stateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("State not found with id: " + id));
    }

    private IndiaDistrict fetchDistrict(Long id) {
        return districtRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("District not found with id: " + id));
    }

    private IndiaStateResponse toStateResponse(IndiaState s) {
        return new IndiaStateResponse(s.getId(), s.getName(), s.getCode(),
            s.getIsActive(), s.getCreatedAt(), s.getUpdatedAt());
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

