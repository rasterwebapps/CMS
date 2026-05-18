package com.cms.dto;

/**
 * Fields a user may update on their own profile.
 * Null means leave unchanged; blank string clears the value.
 */
public record SelfUpdateRequest(
    String phone,
    String bloodGroup,
    String bio,
    String postalAddress,
    String street,
    String city,
    String district,
    String state,
    String pincode,
    String emergencyContactName,
    String emergencyContactRelationship,
    String emergencyContactPhone
) {}

