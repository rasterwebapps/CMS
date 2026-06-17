package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StaffReferrerRequest;
import com.cms.dto.StaffReferrerResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.StaffReferrer;
import com.cms.repository.StaffReferrerRepository;

@Service
@Transactional(readOnly = true)
public class StaffReferrerService {

    private final StaffReferrerRepository repository;

    public StaffReferrerService(StaffReferrerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StaffReferrerResponse create(StaffReferrerRequest request) {
        String name = requireTrimmed(request.name(), "Name is required");
        if (repository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A staff referrer with the name '" + name + "' already exists");
        }
        StaffReferrer entity = new StaffReferrer();
        applyFields(entity, request, name);
        return toResponse(repository.save(entity));
    }

    public List<StaffReferrerResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public List<StaffReferrerResponse> findActive() {
        return repository.findByIsActiveTrue().stream().map(this::toResponse).toList();
    }

    public StaffReferrerResponse findById(Long id) {
        return toResponse(repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Staff referrer not found: " + id)));
    }

    @Transactional
    public StaffReferrerResponse update(Long id, StaffReferrerRequest request) {
        StaffReferrer entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Staff referrer not found: " + id));
        String name = requireTrimmed(request.name(), "Name is required");
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("A staff referrer with the name '" + name + "' already exists");
        }
        applyFields(entity, request, name);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Staff referrer not found: " + id);
        }
        repository.deleteById(id);
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return repository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return repository.existsByNameIgnoreCase(trimmed);
    }

    private void applyFields(StaffReferrer e, StaffReferrerRequest r, String name) {
        e.setName(name);
        e.setPhone(trim(r.phone()));
        e.setEmail(trim(r.email()));
        e.setInstitution(trim(r.institution()));
        e.setCommissionAmount(r.commissionAmount());
        e.setIsActive(r.isActive() != null ? r.isActive() : Boolean.TRUE.equals(e.getIsActive() == null ? true : e.getIsActive()));
        e.setPanNumber(trim(r.panNumber()));
        e.setAadhaarNumber(trim(r.aadhaarNumber()));
        e.setBankAccountNumber(trim(r.bankAccountNumber()));
        e.setBankIfscCode(trim(r.bankIfscCode()));
        e.setBankBranch(trim(r.bankBranch()));
        e.setBankName(trim(r.bankName()));
        e.setBankAccountHolder(trim(r.bankAccountHolder()));
        e.setBankAccountType(r.bankAccountType());
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

    private StaffReferrerResponse toResponse(StaffReferrer e) {
        return new StaffReferrerResponse(
            e.getId(), e.getName(), e.getPhone(), e.getEmail(), e.getInstitution(),
            e.getCommissionAmount(), e.getIsActive(),
            e.getPanNumber(), e.getAadhaarNumber(),
            e.getBankAccountNumber(), e.getBankIfscCode(), e.getBankBranch(),
            e.getBankName(), e.getBankAccountHolder(), e.getBankAccountType(),
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
