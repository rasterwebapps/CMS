package com.cms.repository;

import com.cms.model.AcademicQualification;
import com.cms.model.Enquiry;
import com.cms.model.Student;
import com.cms.model.enums.QualificationType;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.StudentType;

import java.math.BigDecimal;

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

    // ---------------------------------------------------------------------
    // AI Smart Search (com.cms.ai) -- filters over the SearchFieldRegistry
    // allow-list only. Every method below backs exactly one allow-listed
    // field; do not add a method here without adding the matching entry to
    // SearchFieldRegistry, and never expose one without the other.
    // ---------------------------------------------------------------------

    public static Specification<Student> byFirstNameContains(String value) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("firstName")), likePattern(value));
    }

    public static Specification<Student> byLastNameContains(String value) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("lastName")), likePattern(value));
    }

    public static Specification<Student> byRollNumberContains(String value) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("rollNumber")), likePattern(value));
    }

    public static Specification<Student> byAdmissionNumberContains(String value) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("admissionNumber")), likePattern(value));
    }

    public static Specification<Student> byProgramNameContains(String value) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("program").get("name")), likePattern(value));
    }

    public static Specification<Student> byCourseNameContains(String value) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("course").get("name")), likePattern(value));
    }

    public static Specification<Student> bySpecialityNameContains(String value) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("speciality").get("name")), likePattern(value));
    }

    public static Specification<Student> byAdmissionCategory(String value) {
        return (root, query, cb) -> {
            try {
                var category = com.cms.model.enums.AdmissionCategory.valueOf(value.toUpperCase());
                return cb.equal(root.get("admissionCategory"), category);
            } catch (IllegalArgumentException e) {
                return cb.disjunction();
            }
        };
    }

    public static Specification<Student> byGender(String value) {
        return (root, query, cb) -> {
            try {
                var gender = com.cms.model.enums.Gender.valueOf(value.toUpperCase());
                return cb.equal(root.get("gender"), gender);
            } catch (IllegalArgumentException e) {
                return cb.disjunction();
            }
        };
    }

    /** Students with an {@link AcademicQualification} whose school/college name matches. */
    public static Specification<Student> byAcademicQualificationSchoolNameContains(String value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AcademicQualification> aq = sub.from(AcademicQualification.class);
            sub.select(aq.get("admission").get("student").get("id"))
               .where(cb.like(cb.lower(aq.get("schoolName")), likePattern(value)));
            return root.get("id").in(sub);
        };
    }

    public static Specification<Student> byAcademicQualificationBoardContains(String value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AcademicQualification> aq = sub.from(AcademicQualification.class);
            sub.select(aq.get("admission").get("student").get("id"))
               .where(cb.like(cb.lower(aq.get("universityOrBoard")), likePattern(value)));
            return root.get("id").in(sub);
        };
    }

    public static Specification<Student> byAcademicQualificationType(String value) {
        return (root, query, cb) -> {
            QualificationType type;
            try {
                type = QualificationType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return cb.disjunction();
            }
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AcademicQualification> aq = sub.from(AcademicQualification.class);
            sub.select(aq.get("admission").get("student").get("id"))
               .where(cb.equal(aq.get("qualificationType"), type));
            return root.get("id").in(sub);
        };
    }

    public static Specification<Student> byAcademicQualificationPercentageAtLeast(BigDecimal minPercentage) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AcademicQualification> aq = sub.from(AcademicQualification.class);
            sub.select(aq.get("admission").get("student").get("id"))
               .where(cb.greaterThanOrEqualTo(aq.get("percentage"), minPercentage));
            return root.get("id").in(sub);
        };
    }

    public static Specification<Student> byAcademicQualificationPassingPeriodContains(String value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AcademicQualification> aq = sub.from(AcademicQualification.class);
            sub.select(aq.get("admission").get("student").get("id"))
               .where(cb.like(cb.lower(aq.get("monthAndYearOfPassing")), likePattern(value)));
            return root.get("id").in(sub);
        };
    }

    private static String likePattern(String value) {
        return "%" + value.toLowerCase() + "%";
    }
}
