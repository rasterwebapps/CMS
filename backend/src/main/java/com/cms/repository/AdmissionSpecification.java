package com.cms.repository;

import com.cms.model.Admission;
import com.cms.model.Enquiry;
import com.cms.model.Student;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.StudentType;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

public final class AdmissionSpecification {

    private AdmissionSpecification() {}

    public static Specification<Admission> distinct() {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType())) {
                query.distinct(true);
            }
            return null;
        };
    }

    public static Specification<Admission> byProgramId(Long programId) {
        return (root, query, cb) ->
            cb.equal(studentJoin(root).get("program").get("id"), programId);
    }

    public static Specification<Admission> byCourseId(Long courseId) {
        return (root, query, cb) ->
            cb.equal(studentJoin(root).get("course").get("id"), courseId);
    }

    public static Specification<Admission> byAcademicYearId(Long academicYearId) {
        return (root, query, cb) ->
            cb.equal(root.get("joiningAcademicYear").get("id"), academicYearId);
    }

    public static Specification<Admission> byStatus(String status) {
        return (root, query, cb) -> {
            try {
                StudentStatus s = StudentStatus.valueOf(status.toUpperCase());
                return cb.equal(studentJoin(root).get("status"), s);
            } catch (IllegalArgumentException e) {
                return cb.disjunction();
            }
        };
    }

    public static Specification<Admission> byStudentType(String studentType) {
        return (root, query, cb) -> {
            try {
                StudentType type = StudentType.valueOf(studentType.toUpperCase());
                Join<Admission, Student> s = studentJoin(root);
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Enquiry> enq = sub.from(Enquiry.class);
                sub.select(enq.get("convertedStudentId"))
                   .where(cb.equal(enq.get("studentType"), type));
                return s.get("id").in(sub);
            } catch (IllegalArgumentException e) {
                return cb.disjunction();
            }
        };
    }

    public static Specification<Admission> bySearch(String search) {
        return (root, query, cb) -> {
            Join<Admission, Student> s = studentJoin(root);
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(s.get("firstName")), pattern),
                cb.like(cb.lower(s.get("lastName")), pattern),
                cb.like(cb.lower(s.get("admissionNumber")), pattern),
                cb.like(cb.lower(s.get("rollNumber")), pattern)
            );
        };
    }

    private static Join<Admission, Student> studentJoin(Root<Admission> root) {
        for (Join<Admission, ?> j : root.getJoins()) {
            if ("student".equals(j.getAttribute().getName())) {
                @SuppressWarnings("unchecked")
                Join<Admission, Student> typed = (Join<Admission, Student>) j;
                return typed;
            }
        }
        return root.join("student", JoinType.INNER);
    }
}
