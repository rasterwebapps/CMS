package com.cms.dto;

import java.time.LocalDate;

/**
 * A lightweight summary of a single enquiry record for the Front Office dashboard.
 */
public record FrontOfficeEnquiryItem(
    Long id,
    String name,
    String programName,
    String referralTypeName,
    String status,
    LocalDate enquiryDate
) {}
