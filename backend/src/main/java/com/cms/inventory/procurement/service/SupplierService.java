package com.cms.inventory.procurement.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.PermSecurityBean;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.procurement.dto.SupplierRequest;
import com.cms.inventory.procurement.dto.SupplierResponse;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.repository.SupplierRepository;

/**
 * See {@link Supplier}'s Javadoc for the masking and approve-permission design this service
 * implements.
 */
@Service
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PermSecurityBean perm;

    public SupplierService(SupplierRepository supplierRepository, PermSecurityBean perm) {
        this.supplierRepository = supplierRepository;
        this.perm = perm;
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        String code = requireTrimmed(request.supplierCode(), "Supplier code is required");
        String name = requireTrimmed(request.supplierName(), "Supplier name is required");
        if (supplierRepository.existsBySupplierCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("A supplier with the code '" + code + "' already exists");
        }

        Supplier supplier = new Supplier();
        supplier.setSupplierCode(code);
        applyFields(supplier, name, request);
        return toResponse(supplierRepository.save(supplier));
    }

    public List<SupplierResponse> findAll(boolean activeOnly) {
        List<Supplier> suppliers = activeOnly
            ? supplierRepository.findByIsActiveTrueOrderBySupplierNameAsc()
            : supplierRepository.findAllByOrderBySupplierNameAsc();
        return suppliers.stream().map(this::toResponse).toList();
    }

    public Page<SupplierResponse> findPage(String search, Pageable pageable) {
        Specification<Supplier> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("supplierName")), pattern),
                cb.like(cb.lower(root.get("supplierCode")), pattern)
            );
        };
        return supplierRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public SupplierResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = findOrThrow(id);
        String code = requireTrimmed(request.supplierCode(), "Supplier code is required");
        String name = requireTrimmed(request.supplierName(), "Supplier name is required");
        if (supplierRepository.existsBySupplierCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("A supplier with the code '" + code + "' already exists");
        }

        supplier.setSupplierCode(code);
        applyFields(supplier, name, request);
        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public void delete(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new ResourceNotFoundException("Supplier not found with id: " + id);
        }
        supplierRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Supplier supplier = findOrThrow(id);
        supplier.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Supplier saved = supplierRepository.save(supplier);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    /** Only path that ever sets {@code isApproved}/{@code approvalDate} — gated by its own
     *  {@code INVENTORY_SUPPLIER_APPROVE} permission at the controller, never bundled into the
     *  regular create/update path. */
    @Transactional
    public SupplierResponse approve(Long id) {
        Supplier supplier = findOrThrow(id);
        if (Boolean.TRUE.equals(supplier.getIsApproved())) {
            throw new IllegalArgumentException("This supplier is already approved");
        }
        supplier.setIsApproved(true);
        supplier.setApprovalDate(Instant.now());
        return toResponse(supplierRepository.save(supplier));
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        return excludeId != null
            ? supplierRepository.existsBySupplierCodeIgnoreCaseAndIdNot(trimmed, excludeId)
            : supplierRepository.existsBySupplierCodeIgnoreCase(trimmed);
    }

    Supplier findOrThrow(Long id) {
        return supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    private void applyFields(Supplier supplier, String name, SupplierRequest request) {
        supplier.setSupplierName(name);
        supplier.setState(requireTrimmed(request.state(), "State is required"));
        supplier.setTaxRegistrationId(trim(request.taxRegistrationId()));
        supplier.setLegalRegistrationNo(trim(request.legalRegistrationNo()));
        supplier.setBankAccountNumber(trim(request.bankAccountNumber()));
        supplier.setBankIfscCode(trim(request.bankIfscCode()));
        supplier.setBankName(trim(request.bankName()));
        supplier.setBankAccountHolder(trim(request.bankAccountHolder()));
        supplier.setContactPerson(trim(request.contactPerson()));
        supplier.setEmail(trim(request.email()));
        supplier.setPhone(trim(request.phone()));
        if (request.portalAccessEnabled() != null) supplier.setPortalAccessEnabled(request.portalAccessEnabled());
        if (request.isActive() != null) supplier.setIsActive(request.isActive());
    }

    private SupplierResponse toResponse(Supplier s) {
        boolean canSeeFull = perm.has("INVENTORY_SUPPLIER_MANAGE");
        return new SupplierResponse(
            s.getId(), s.getSupplierCode(), s.getSupplierName(), s.getState(),
            canSeeFull ? s.getTaxRegistrationId() : maskTail(s.getTaxRegistrationId()),
            canSeeFull ? s.getLegalRegistrationNo() : maskTail(s.getLegalRegistrationNo()),
            canSeeFull ? s.getBankAccountNumber() : maskTail(s.getBankAccountNumber()),
            s.getBankIfscCode(), s.getBankName(), s.getBankAccountHolder(), !canSeeFull,
            s.getContactPerson(), s.getEmail(), s.getPhone(),
            s.getIsApproved(), s.getApprovalDate(), s.getPortalAccessEnabled(), s.getIsActive(),
            s.getCreatedAt(), s.getUpdatedAt());
    }

    /** Shows only the last 4 characters (e.g. {@code "••••1234"}); {@code null}/short values are
     *  fully masked rather than risk revealing their length or content. */
    private static String maskTail(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.length() <= 4) return "••••";
        return "••••" + trimmed.substring(trimmed.length() - 4);
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
