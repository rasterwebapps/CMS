package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StaffReferrerRequest;
import com.cms.dto.StaffReferrerResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Institution;
import com.cms.model.StaffReferrer;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import com.cms.repository.CommissionPayoutRepository;
import com.cms.repository.InstitutionRepository;
import com.cms.repository.StaffReferrerRepository;

@Service
@Transactional(readOnly = true)
public class StaffReferrerService {

    private final StaffReferrerRepository repository;
    private final CommissionPayoutRepository commissionPayoutRepository;
    private final InstitutionRepository institutionRepository;

    public StaffReferrerService(StaffReferrerRepository repository,
                                CommissionPayoutRepository commissionPayoutRepository,
                                InstitutionRepository institutionRepository) {
        this.repository = repository;
        this.commissionPayoutRepository = commissionPayoutRepository;
        this.institutionRepository = institutionRepository;
    }

    @Transactional
    public StaffReferrerResponse create(StaffReferrerRequest request) {
        String name = requireTrimmed(request.name(), "Name is required");
        String employeeCode = requireTrimmed(request.employeeCode(), "Employee code is required");
        Institution institution = findInstitutionOrThrow(request.institutionId());

        if (repository.existsByInstitutionIdAndNameIgnoreCase(institution.getId(), name)) {
            throw new IllegalArgumentException(
                "A staff referrer with the name '" + name + "' already exists at this institution");
        }
        if (repository.existsByInstitutionIdAndEmployeeCodeIgnoreCase(institution.getId(), employeeCode)) {
            throw new IllegalArgumentException(
                "A staff referrer with the employee code '" + employeeCode + "' already exists at this institution");
        }
        StaffReferrer entity = new StaffReferrer();
        applyFields(entity, request, name, employeeCode, institution);
        return toResponse(repository.save(entity));
    }

    public List<StaffReferrerResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public List<StaffReferrerResponse> findAll(String search) {
        Specification<StaffReferrer> spec = Specification.where(null);
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                Join<StaffReferrer, Institution> institution = root.join("institution", JoinType.INNER);
                return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern),
                    cb.like(cb.lower(root.get("employeeCode")), pattern),
                    cb.like(cb.lower(institution.get("name")), pattern)
                );
            });
        }
        return repository.findAll(spec).stream().map(this::toResponse).toList();
    }

    public Page<StaffReferrerResponse> findPage(String search, Pageable pageable) {
        Specification<StaffReferrer> spec = Specification.where(null);
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                Join<StaffReferrer, Institution> institution = root.join("institution", JoinType.INNER);
                return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern),
                    cb.like(cb.lower(root.get("employeeCode")), pattern),
                    cb.like(cb.lower(institution.get("name")), pattern)
                );
            });
        }
        return repository.findAll(spec, pageable).map(this::toResponse);
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
        String employeeCode = requireTrimmed(request.employeeCode(), "Employee code is required");
        Institution institution = findInstitutionOrThrow(request.institutionId());

        if (repository.existsByInstitutionIdAndNameIgnoreCaseAndIdNot(institution.getId(), name, id)) {
            throw new IllegalArgumentException(
                "A staff referrer with the name '" + name + "' already exists at this institution");
        }
        if (repository.existsByInstitutionIdAndEmployeeCodeIgnoreCaseAndIdNot(institution.getId(), employeeCode, id)) {
            throw new IllegalArgumentException(
                "A staff referrer with the employee code '" + employeeCode + "' already exists at this institution");
        }
        applyFields(entity, request, name, employeeCode, institution);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Staff referrer not found: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        StaffReferrer entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Staff referrer not found: " + id));
        if (Boolean.FALSE.equals(request.isActive()) && commissionPayoutRepository.existsByStaffReferrerId(id)) {
            throw new LifecycleConflictException(
                "Cannot deactivate Staff Referrer: commission payouts are associated with it.",
                "ACTIVE_REFERENCE_EXISTS",
                "StaffReferrer",
                id,
                null
            );
        }
        entity.setIsActive(request.isActive());
        StaffReferrer updated = repository.save(entity);
        return new ActiveStatusUpdateResponse(updated.getId(), updated.getIsActive(), updated.getUpdatedAt());
    }

    @Transactional
    public StaffReferrerResponse deactivate(Long id) {
        updateStatus(id, new ActiveStatusUpdateRequest(false, null));
        return findById(id);
    }

    @Transactional
    public StaffReferrerResponse reactivate(Long id) {
        updateStatus(id, new ActiveStatusUpdateRequest(true, null));
        return findById(id);
    }

    public boolean nameExists(String name, Long institutionId, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (institutionId == null || trimmed.isEmpty()) return false;
        if (excludeId != null) {
            return repository.existsByInstitutionIdAndNameIgnoreCaseAndIdNot(institutionId, trimmed, excludeId);
        }
        return repository.existsByInstitutionIdAndNameIgnoreCase(institutionId, trimmed);
    }

    public boolean employeeCodeExists(String employeeCode, Long institutionId, Long excludeId) {
        String trimmed = employeeCode == null ? "" : employeeCode.trim();
        if (institutionId == null || trimmed.isEmpty()) return false;
        if (excludeId != null) {
            return repository.existsByInstitutionIdAndEmployeeCodeIgnoreCaseAndIdNot(institutionId, trimmed, excludeId);
        }
        return repository.existsByInstitutionIdAndEmployeeCodeIgnoreCase(institutionId, trimmed);
    }

    private Institution findInstitutionOrThrow(Long institutionId) {
        if (institutionId == null) {
            throw new IllegalArgumentException("Institution is required");
        }
        return institutionRepository.findById(institutionId)
            .orElseThrow(() -> new ResourceNotFoundException("Institution not found: " + institutionId));
    }

    private void applyFields(StaffReferrer e, StaffReferrerRequest r, String name, String employeeCode, Institution institution) {
        e.setName(name);
        e.setPhone(trim(r.phone()));
        e.setEmail(trim(r.email()));
        e.setEmployeeCode(employeeCode);
        e.setInstitution(institution);
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
            e.getId(), e.getName(), e.getPhone(), e.getEmail(), e.getEmployeeCode(),
            e.getInstitution().getId(), e.getInstitution().getName(),
            e.getCommissionAmount(), e.getIsActive(),
            e.getPanNumber(), e.getAadhaarNumber(),
            e.getBankAccountNumber(), e.getBankIfscCode(), e.getBankBranch(),
            e.getBankName(), e.getBankAccountHolder(), e.getBankAccountType(),
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
