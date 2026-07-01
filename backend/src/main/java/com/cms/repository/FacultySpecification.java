package com.cms.repository;

import com.cms.model.Faculty;
import com.cms.model.FacultyDocument;
import com.cms.model.FacultyDocumentTypeRequirement;
import com.cms.model.Speciality;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.FacultyStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

public final class FacultySpecification {

    private FacultySpecification() {}

    public static Specification<Faculty> distinct() {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType())) {
                query.distinct(true);
            }
            return null;
        };
    }

    public static Specification<Faculty> bySearch(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.toLowerCase() + "%";
            Join<Faculty, Speciality> spec = getOrJoinSpeciality(root);
            return cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))), pattern),
                cb.like(cb.lower(root.get("employeeCode")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(spec.get("name")), pattern)
            );
        };
    }

    public static Specification<Faculty> bySpecialityId(Long specialityId) {
        return (root, query, cb) -> cb.equal(root.get("speciality").get("id"), specialityId);
    }

    public static Specification<Faculty> byStatus(FacultyStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Faculty> byDocumentReview(String filter) {
        return switch (filter.toUpperCase()) {
            case "NEEDS_VERIFICATION" -> needsVerification();
            case "REJECTED"           -> rejected();
            case "NO_DOCUMENTS"       -> noDocuments();
            case "HAS_ANY_DOCUMENTS"  -> hasAnyDocuments();
            case "MISSING_REQUIRED"   -> missingRequired();
            case "FULLY_VERIFIED"     -> fullyVerified();
            default                   -> null;
        };
    }

    // ── Document review helpers ───────────────────────────────────

    private static Specification<Faculty> needsVerification() {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<FacultyDocument> fd = sub.from(FacultyDocument.class);
            sub.select(cb.literal(1));
            sub.where(
                cb.equal(fd.get("faculty").get("id"), root.get("id")),
                cb.equal(fd.get("status"), DocumentVerificationStatus.UPLOADED),
                cb.isNotNull(fd.get("fileName"))
            );
            return cb.exists(sub);
        };
    }

    private static Specification<Faculty> rejected() {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<FacultyDocument> fd = sub.from(FacultyDocument.class);
            sub.select(cb.literal(1));
            sub.where(
                cb.equal(fd.get("faculty").get("id"), root.get("id")),
                cb.equal(fd.get("status"), DocumentVerificationStatus.REJECTED),
                cb.isNotNull(fd.get("fileName"))
            );
            return cb.exists(sub);
        };
    }

    private static Specification<Faculty> noDocuments() {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<FacultyDocument> fd = sub.from(FacultyDocument.class);
            sub.select(cb.literal(1));
            sub.where(
                cb.equal(fd.get("faculty").get("id"), root.get("id")),
                cb.isNotNull(fd.get("fileName"))
            );
            return cb.not(cb.exists(sub));
        };
    }

    private static Specification<Faculty> hasAnyDocuments() {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<FacultyDocument> fd = sub.from(FacultyDocument.class);
            sub.select(cb.literal(1));
            sub.where(
                cb.equal(fd.get("faculty").get("id"), root.get("id")),
                cb.isNotNull(fd.get("fileName"))
            );
            return cb.exists(sub);
        };
    }

    private static Specification<Faculty> missingRequired() {
        // EXISTS a requirement matching this faculty that has no uploaded document
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<FacultyDocumentTypeRequirement> req = sub.from(FacultyDocumentTypeRequirement.class);
            sub.select(cb.literal(1));

            // Nested subquery: no uploaded doc of this required type for this faculty
            Subquery<Integer> docSub = sub.subquery(Integer.class);
            Root<FacultyDocument> fd = docSub.from(FacultyDocument.class);
            docSub.select(cb.literal(1));
            docSub.where(
                cb.equal(fd.get("faculty").get("id"), root.get("id")),
                cb.equal(fd.get("documentType"), req.get("documentType")),
                cb.isNotNull(fd.get("fileName"))
            );

            sub.where(requirementMatchesFaculty(req, root, cb), cb.not(cb.exists(docSub)));
            return cb.exists(sub);
        };
    }

    private static Specification<Faculty> fullyVerified() {
        // 1. At least one requirement matches faculty
        // 2. No matching requirement lacks a VERIFIED document
        // 3. No UPLOADED (pending) docs
        // 4. No REJECTED docs
        return (root, query, cb) -> {
            // Condition A: has at least one matching requirement
            Subquery<Integer> hasReqSub = query.subquery(Integer.class);
            Root<FacultyDocumentTypeRequirement> reqA = hasReqSub.from(FacultyDocumentTypeRequirement.class);
            hasReqSub.select(cb.literal(1));
            hasReqSub.where(requirementMatchesFaculty(reqA, root, cb));

            // Condition B: no matching requirement is missing a VERIFIED doc
            Subquery<Integer> missingVerifiedSub = query.subquery(Integer.class);
            Root<FacultyDocumentTypeRequirement> reqB = missingVerifiedSub.from(FacultyDocumentTypeRequirement.class);
            missingVerifiedSub.select(cb.literal(1));
            Subquery<Integer> verifiedDocSub = missingVerifiedSub.subquery(Integer.class);
            Root<FacultyDocument> fdB = verifiedDocSub.from(FacultyDocument.class);
            verifiedDocSub.select(cb.literal(1));
            verifiedDocSub.where(
                cb.equal(fdB.get("faculty").get("id"), root.get("id")),
                cb.equal(fdB.get("documentType"), reqB.get("documentType")),
                cb.isNotNull(fdB.get("fileName")),
                cb.equal(fdB.get("status"), DocumentVerificationStatus.VERIFIED)
            );
            missingVerifiedSub.where(requirementMatchesFaculty(reqB, root, cb), cb.not(cb.exists(verifiedDocSub)));

            // Condition C: no pending docs
            Subquery<Integer> pendingSub = query.subquery(Integer.class);
            Root<FacultyDocument> fdPending = pendingSub.from(FacultyDocument.class);
            pendingSub.select(cb.literal(1));
            pendingSub.where(
                cb.equal(fdPending.get("faculty").get("id"), root.get("id")),
                cb.equal(fdPending.get("status"), DocumentVerificationStatus.UPLOADED),
                cb.isNotNull(fdPending.get("fileName"))
            );

            // Condition D: no rejected docs
            Subquery<Integer> rejectedSub = query.subquery(Integer.class);
            Root<FacultyDocument> fdRejected = rejectedSub.from(FacultyDocument.class);
            rejectedSub.select(cb.literal(1));
            rejectedSub.where(
                cb.equal(fdRejected.get("faculty").get("id"), root.get("id")),
                cb.equal(fdRejected.get("status"), DocumentVerificationStatus.REJECTED),
                cb.isNotNull(fdRejected.get("fileName"))
            );

            return cb.and(
                cb.exists(hasReqSub),
                cb.not(cb.exists(missingVerifiedSub)),
                cb.not(cb.exists(pendingSub)),
                cb.not(cb.exists(rejectedSub))
            );
        };
    }

    private static Predicate requirementMatchesFaculty(
            Root<FacultyDocumentTypeRequirement> req,
            Root<Faculty> faculty,
            CriteriaBuilder cb) {
        Predicate byDesignation = cb.and(
            cb.isNotNull(req.get("designation")),
            cb.isNotNull(faculty.get("designation")),
            cb.equal(req.get("designation").get("id"), faculty.get("designation").get("id"))
        );
        Predicate bySpeciality = cb.and(
            cb.isNotNull(req.get("speciality")),
            cb.equal(req.get("speciality").get("id"), faculty.get("speciality").get("id"))
        );
        Predicate byQualification = cb.and(
            cb.isNotNull(req.get("qualification")),
            cb.isNotNull(faculty.get("highestQualification")),
            cb.equal(req.get("qualification"), faculty.get("highestQualification"))
        );
        return cb.or(byDesignation, bySpeciality, byQualification);
    }

    private static Join<Faculty, Speciality> getOrJoinSpeciality(Root<Faculty> root) {
        for (Join<Faculty, ?> j : root.getJoins()) {
            if ("speciality".equals(j.getAttribute().getName())) {
                @SuppressWarnings("unchecked")
                Join<Faculty, Speciality> typed = (Join<Faculty, Speciality>) j;
                return typed;
            }
        }
        return root.join("speciality", JoinType.INNER);
    }
}
