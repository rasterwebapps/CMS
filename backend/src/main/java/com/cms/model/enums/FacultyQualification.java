package com.cms.model.enums;

public enum FacultyQualification {
    UG("Under Graduate"),
    PG("Post Graduate"),
    MPHIL("M.Phil"),
    PHD("Ph.D"),
    OTHER("Other");

    private final String displayName;

    FacultyQualification(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
