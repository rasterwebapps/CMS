package com.cms.dto;

public record AddressRequest(
    Long countryId,
    String postalAddress,
    String street,
    String city,
    String district,
    String state,
    String pincode
) {}
