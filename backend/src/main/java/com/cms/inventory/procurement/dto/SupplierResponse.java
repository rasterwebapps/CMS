package com.cms.inventory.procurement.dto;

import java.time.Instant;

/**
 * {@code taxRegistrationId}, {@code legalRegistrationNo}, and {@code bankAccountNumber} come back
 * masked to their last 4 characters (e.g. {@code "••••1234"}) unless the caller holds {@code
 * INVENTORY_SUPPLIER_MANAGE} — see {@code SupplierService.toResponse}. {@code bankMasked}
 * tells the frontend whether that masking was applied, so a view-only user isn't shown a "this
 * field is empty" state for data that's actually just hidden from them.
 */
public record SupplierResponse(
    Long id,
    String supplierCode,
    String supplierName,
    String taxRegistrationId,
    String legalRegistrationNo,
    String bankAccountNumber,
    String bankIfscCode,
    String bankName,
    String bankAccountHolder,
    boolean bankMasked,
    String contactPerson,
    String email,
    String phone,
    Boolean isApproved,
    Instant approvalDate,
    Boolean portalAccessEnabled,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
