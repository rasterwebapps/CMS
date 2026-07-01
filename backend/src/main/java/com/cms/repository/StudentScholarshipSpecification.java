package com.cms.repository;

import org.springframework.data.jpa.domain.Specification;

import com.cms.model.StudentScholarship;
import com.cms.model.enums.ScholarshipStatus;

public final class StudentScholarshipSpecification {
    private StudentScholarshipSpecification() {}

    public static Specification<StudentScholarship> byStatus(ScholarshipStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<StudentScholarship> bySearch(String q) {
        return (root, query, cb) -> {
            String pattern = "%" + q.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("student").get("name")), pattern),
                cb.like(cb.lower(root.get("scholarshipType").get("name")), pattern)
            );
        };
    }
}
