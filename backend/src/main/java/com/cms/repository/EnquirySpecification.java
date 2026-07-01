package com.cms.repository;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import com.cms.model.Enquiry;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.StudentType;

public final class EnquirySpecification {

    private EnquirySpecification() {}

    public static Specification<Enquiry> byStatuses(Collection<EnquiryStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Enquiry> byStatus(EnquiryStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Enquiry> byProgramId(Long programId) {
        return (root, query, cb) -> cb.equal(root.get("program").get("id"), programId);
    }

    public static Specification<Enquiry> byCourseId(Long courseId) {
        return (root, query, cb) -> cb.equal(root.get("course").get("id"), courseId);
    }

    public static Specification<Enquiry> byStudentType(String studentType) {
        return (root, query, cb) -> {
            try {
                StudentType type = StudentType.valueOf(studentType.toUpperCase());
                return cb.equal(root.get("studentType"), type);
            } catch (IllegalArgumentException e) {
                return cb.disjunction();
            }
        };
    }

    public static Specification<Enquiry> bySearch(String q) {
        return (root, query, cb) -> {
            String pattern = "%" + q.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")),  pattern),
                cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("email"), "")), pattern)
            );
        };
    }

    public static Specification<Enquiry> byAcademicYearId(Long academicYearId) {
        return (root, query, cb) -> cb.equal(root.get("academicYear").get("id"), academicYearId);
    }

    public static Specification<Enquiry> byDateRangeOrActivePipeline(
            LocalDate from, LocalDate to, Collection<EnquiryStatus> terminalStatuses) {
        return (root, query, cb) -> cb.or(
            cb.between(root.get("enquiryDate"), from, to),
            root.get("status").in(terminalStatuses).not()
        );
    }
}
