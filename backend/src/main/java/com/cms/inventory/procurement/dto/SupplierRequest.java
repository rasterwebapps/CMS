package com.cms.inventory.procurement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(

    @NotBlank(message = "Supplier code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String supplierCode,

    @NotBlank(message = "Supplier name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String supplierName,

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    String state,

    @Size(max = 50, message = "Tax registration id must not exceed 50 characters")
    String taxRegistrationId,

    @Size(max = 50, message = "Legal registration no. must not exceed 50 characters")
    String legalRegistrationNo,

    @Size(max = 40, message = "Bank account number must not exceed 40 characters")
    String bankAccountNumber,

    @Size(max = 20, message = "Bank IFSC code must not exceed 20 characters")
    String bankIfscCode,

    @Size(max = 150, message = "Bank name must not exceed 150 characters")
    String bankName,

    @Size(max = 150, message = "Bank account holder must not exceed 150 characters")
    String bankAccountHolder,

    @Size(max = 150, message = "Contact person must not exceed 150 characters")
    String contactPerson,

    @Email(message = "Enter a valid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    String email,

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    String phone,

    Boolean portalAccessEnabled,

    Boolean isActive
) {}
