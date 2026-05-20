package com.cms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "postal_address")
    private String postalAddress;

    private String street;

    private String city;

    private String district;

    private String state;

    private String pincode;

    public Address() {
    }

    public Address(Long countryId, String postalAddress, String street, String city,
                   String district, String state, String pincode) {
        this.countryId = countryId;
        this.postalAddress = postalAddress;
        this.street = street;
        this.city = city;
        this.district = district;
        this.state = state;
        this.pincode = pincode;
    }

    public Long getCountryId() { return countryId; }
    public void setCountryId(Long countryId) { this.countryId = countryId; }

    public String getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(String postalAddress) {
        this.postalAddress = postalAddress;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }
}
