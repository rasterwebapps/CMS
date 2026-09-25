package com.cms.repository;

import org.springframework.data.jpa.domain.Specification;

import com.cms.model.ClassSchedule;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

import jakarta.persistence.criteria.JoinType;

public final class ClassScheduleSpecification {

    private ClassScheduleSpecification() {}

    public static Specification<ClassSchedule> byIsActiveTrue() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<ClassSchedule> byLabId(Long labId) {
        return (root, query, cb) -> cb.equal(root.get("lab").get("id"), labId);
    }

    public static Specification<ClassSchedule> byFacultyId(Long facultyId) {
        return (root, query, cb) -> cb.equal(root.get("faculty").get("id"), facultyId);
    }

    public static Specification<ClassSchedule> byTermInstanceId(Long termInstanceId) {
        return (root, query, cb) -> cb.equal(root.get("termInstance").get("id"), termInstanceId);
    }

    public static Specification<ClassSchedule> byDayOfWeek(DayOfWeek dayOfWeek) {
        return (root, query, cb) -> cb.equal(root.get("dayOfWeek"), dayOfWeek);
    }

    public static Specification<ClassSchedule> bySessionType(ClassSessionType sessionType) {
        return (root, query, cb) -> cb.equal(root.get("sessionType"), sessionType);
    }

    /** Searches subject name/code, faculty name, batch name, and every session type's room name
     *  (lab, classroom, clinical venue — exactly one is ever populated per row, per {@code
     *  ClassScheduleService#toResponse}). Uses explicit LEFT JOINs for every optional relation
     *  (lab/faculty/classroom/clinicalVenue) rather than {@code root.get(...)} — {@code Root.get()}
     *  on a nullable @ManyToOne silently becomes an INNER JOIN, which would drop every row whose
     *  FK is null (e.g. every LIBRARY/SPORTS row has no lab, every unstaffed skeleton row has no
     *  faculty) from the whole OR clause, not just that one branch. No {@code query.distinct()}
     *  needed (and it must stay that way — combined with the {@code dayOrder} CASE-expression
     *  default sort, Postgres rejects {@code SELECT DISTINCT ... ORDER BY <expr not in select
     *  list>} outright): every join here is @ManyToOne (one row on each side), so there's no
     *  fan-out to de-duplicate against in the first place. */
    public static Specification<ClassSchedule> bySearch(String q) {
        return (root, query, cb) -> {
            String pattern = "%" + q.toLowerCase() + "%";
            var subject = root.join("subject", JoinType.LEFT);
            var faculty = root.join("faculty", JoinType.LEFT);
            var lab = root.join("lab", JoinType.LEFT);
            var classroom = root.join("classroom", JoinType.LEFT);
            var clinicalVenue = root.join("clinicalVenue", JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(subject.get("name")), pattern),
                cb.like(cb.lower(subject.get("code")), pattern),
                cb.like(cb.lower(cb.coalesce(faculty.get("firstName"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(faculty.get("lastName"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("batchName"), "")), pattern),
                cb.like(cb.lower(lab.get("name")), pattern),
                cb.like(cb.lower(classroom.get("name")), pattern),
                cb.like(cb.lower(clinicalVenue.get("name")), pattern)
            );
        };
    }
}
