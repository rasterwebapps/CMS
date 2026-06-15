package com.cms.repository;

import com.cms.model.Enquiry;
import com.cms.model.Student;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.StudentType;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

public final class StudentSpecification {

    private StudentSpecification() {}

    public static Specification<Student> byProgramId(Long programId) {
        return (root, query, cb) ->
            cb.equal(root.get("program").get("id"), programId);
    }

    public static Specification<Student> byCourseId(Long courseId) {
        return (root, query, cb) ->
            cb.equal(root.get("course").get("id"), courseId);
    }

    public static Specification<Student> byAcademicYearId(Long academicYearId) {
        return (root, query, cb) ->
            cb.equal(root.get("cohort").get("admissionAcademicYear").get("id"), academicYearId);
    }

    public static Specification<Student> byStatus(String status) {
        return (root, query, cb) -> {
            try {
                StudentStatus s = StudentStatus.valueOf(status.toUpperCase());
                return cb.equal(root.get("status"), s);
            } catch (IllegalArgumentException e) {
                return cb.disjunction();
            }
        };
    }

    public static Specification<Student> byStudentType(String studentType) {
        return (root, query, cb) -> {
            try {
                StudentType type = StudentType.valueOf(studentType.toUpperCase());
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Enquiry> enq = sub.from(Enquiry.class);
                sub.select(enq.get("convertedStudentId"))
                   .where(cb.equal(enq.get("studentType"), type));
                return root.get("id").in(sub);
            } catch (IllegalArgumentException e) {
                return cb.disjunction();
            }
        };
    }

    public static Specification<Student> bySearch(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(root.get("admissionNumber")), pattern),
                cb.like(cb.lower(root.get("rollNumber")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }
}
