package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SpecialityRequest;
import com.cms.dto.SpecialityResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Faculty;
import com.cms.model.Speciality;
import com.cms.repository.FacultyRepository;
import com.cms.repository.SpecialityRepository;

@Service
@Transactional(readOnly = true)
public class SpecialityService {

    private final SpecialityRepository specialityRepository;
    private final FacultyRepository facultyRepository;

    public SpecialityService(SpecialityRepository specialityRepository,
                             FacultyRepository facultyRepository) {
        this.specialityRepository = specialityRepository;
        this.facultyRepository = facultyRepository;
    }

    @Transactional
    public SpecialityResponse create(SpecialityRequest request) {
        String name = requireTrimmed(request.name(), "Speciality name is required");
        String code = requireTrimmed(request.code(), "Speciality code is required");

        if (specialityRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A speciality with the name '" + name + "' already exists");
        }
        if (specialityRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException(
                "A speciality with the code '" + code + "' already exists");
        }

        Speciality speciality = new Speciality(name, code, trim(request.description()), null, null);
        if (request.isActive() != null) {
            speciality.setIsActive(request.isActive());
        }
        applyHod(speciality, request.hodFacultyId());
        Speciality saved = specialityRepository.save(speciality);
        return toResponse(saved);
    }

    public List<SpecialityResponse> findAll() {
        return specialityRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<SpecialityResponse> findActive() {
        return specialityRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<SpecialityResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return specialityRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<Speciality> spec = (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)
            );
        return specialityRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public SpecialityResponse findById(Long id) {
        Speciality speciality = specialityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Speciality not found with id: " + id));
        return toResponse(speciality);
    }

    @Transactional
    public SpecialityResponse update(Long id, SpecialityRequest request) {
        Speciality speciality = specialityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Speciality not found with id: " + id));
        String name = requireTrimmed(request.name(), "Speciality name is required");
        String code = requireTrimmed(request.code(), "Speciality code is required");

        if (specialityRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A speciality with the name '" + name + "' already exists");
        }
        if (specialityRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException(
                "A speciality with the code '" + code + "' already exists");
        }

        speciality.setName(name);
        speciality.setCode(code);
        speciality.setDescription(trim(request.description()));
        if (request.isActive() != null) {
            speciality.setIsActive(request.isActive());
        }
        applyHod(speciality, request.hodFacultyId());

        Speciality updated = specialityRepository.save(speciality);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!specialityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Speciality not found with id: " + id);
        }
        specialityRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Speciality speciality = specialityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Speciality not found with id: " + id));
        speciality.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Speciality saved = specialityRepository.save(speciality);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return specialityRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return specialityRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) {
            return specialityRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return specialityRepository.existsByCodeIgnoreCase(trimmed);
    }

    private void applyHod(Speciality speciality, Long hodFacultyId) {
        if (hodFacultyId == null) {
            speciality.setHodFacultyId(null);
            speciality.setHodName(null);
        } else {
            Faculty faculty = facultyRepository.findById(hodFacultyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Faculty not found with id: " + hodFacultyId));
            speciality.setHodFacultyId(hodFacultyId);
            speciality.setHodName(faculty.getFullName());
        }
    }

    private SpecialityResponse toResponse(Speciality speciality) {
        return new SpecialityResponse(
            speciality.getId(),
            speciality.getName(),
            speciality.getCode(),
            speciality.getDescription(),
            speciality.getHodFacultyId(),
            speciality.getHodName(),
            speciality.getIsActive(),
            speciality.getCreatedAt(),
            speciality.getUpdatedAt()
        );
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }
}
