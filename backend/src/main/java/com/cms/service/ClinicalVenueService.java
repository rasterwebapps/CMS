package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.ClinicalVenueRequest;
import com.cms.dto.ClinicalVenueResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClinicalVenue;
import com.cms.repository.ClinicalVenueRepository;

@Service
@Transactional(readOnly = true)
public class ClinicalVenueService {

    private final ClinicalVenueRepository clinicalVenueRepository;

    public ClinicalVenueService(ClinicalVenueRepository clinicalVenueRepository) {
        this.clinicalVenueRepository = clinicalVenueRepository;
    }

    @Transactional
    public ClinicalVenueResponse create(ClinicalVenueRequest request) {
        String name = requireTrimmed(request.name(), "Clinical venue name is required");

        if (clinicalVenueRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A clinical venue with the name '" + name + "' already exists");
        }

        ClinicalVenue venue = new ClinicalVenue(name, trim(request.hospitalName()), trim(request.department()),
            request.capacity());
        if (request.isActive() != null) {
            venue.setIsActive(request.isActive());
        }
        return toResponse(clinicalVenueRepository.save(venue));
    }

    public List<ClinicalVenueResponse> findAll() {
        return clinicalVenueRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ClinicalVenueResponse> findActive() {
        return clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<ClinicalVenueResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return clinicalVenueRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<ClinicalVenue> spec = (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("hospitalName")), pattern),
                cb.like(cb.lower(root.get("department")), pattern)
            );
        return clinicalVenueRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ClinicalVenueResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ClinicalVenueResponse update(Long id, ClinicalVenueRequest request) {
        ClinicalVenue venue = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Clinical venue name is required");

        if (clinicalVenueRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A clinical venue with the name '" + name + "' already exists");
        }

        venue.setName(name);
        venue.setHospitalName(trim(request.hospitalName()));
        venue.setDepartment(trim(request.department()));
        venue.setCapacity(request.capacity());
        if (request.isActive() != null) {
            venue.setIsActive(request.isActive());
        }
        return toResponse(clinicalVenueRepository.save(venue));
    }

    @Transactional
    public void delete(Long id) {
        if (!clinicalVenueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Clinical venue not found with id: " + id);
        }
        clinicalVenueRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        ClinicalVenue venue = findOrThrow(id);
        venue.setIsActive(Boolean.TRUE.equals(request.isActive()));
        ClinicalVenue saved = clinicalVenueRepository.save(venue);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return clinicalVenueRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        return clinicalVenueRepository.existsByNameIgnoreCase(trimmed);
    }

    private ClinicalVenue findOrThrow(Long id) {
        return clinicalVenueRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Clinical venue not found with id: " + id));
    }

    private ClinicalVenueResponse toResponse(ClinicalVenue v) {
        return new ClinicalVenueResponse(v.getId(), v.getName(), v.getHospitalName(), v.getDepartment(),
            v.getCapacity(), v.getIsActive(), v.getCreatedAt(), v.getUpdatedAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) throw new IllegalArgumentException(message);
        return t;
    }
}
