package com.cms.ai;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.cms.model.Student;
import com.cms.repository.StudentSpecification;

/**
 * Translates a validated {@link SearchIntent} into a {@code Specification<Student>}, mapping
 * each allow-listed field name to the matching {@code StudentSpecification} method. Every
 * branch here corresponds 1:1 to an entry in {@link SearchFieldRegistry} -- if you add a field
 * to the registry, add its case here too.
 */
@Component
public class StudentSearchIntentTranslator {

    public Specification<Student> translate(SearchIntent intent) {
        Specification<Student> spec = Specification.where(null);
        for (SearchIntent.Condition condition : intent.conditions()) {
            spec = spec.and(toSpecification(condition));
        }
        return spec;
    }

    private Specification<Student> toSpecification(SearchIntent.Condition condition) {
        String value = condition.value();
        return switch (condition.field()) {
            case "firstName" -> StudentSpecification.byFirstNameContains(value);
            case "lastName" -> StudentSpecification.byLastNameContains(value);
            case "rollNumber" -> StudentSpecification.byRollNumberContains(value);
            case "admissionNumber" -> StudentSpecification.byAdmissionNumberContains(value);
            case "programName" -> StudentSpecification.byProgramNameContains(value);
            case "courseName" -> StudentSpecification.byCourseNameContains(value);
            case "specialityName" -> StudentSpecification.bySpecialityNameContains(value);
            case "status" -> StudentSpecification.byStatus(value);
            case "admissionCategory" -> StudentSpecification.byAdmissionCategory(value);
            case "gender" -> StudentSpecification.byGender(value);
            case "previousSchoolOrCollegeName" -> StudentSpecification.byAcademicQualificationSchoolNameContains(value);
            case "previousBoardOrUniversity" -> StudentSpecification.byAcademicQualificationBoardContains(value);
            case "qualificationType" -> StudentSpecification.byAcademicQualificationType(value);
            case "qualificationPercentage" -> StudentSpecification.byAcademicQualificationPercentageAtLeast(parsePercentage(value));
            case "qualificationPassingPeriod" -> StudentSpecification.byAcademicQualificationPassingPeriodContains(value);
            // Unreachable in practice: StudentSearchIntentParser only ever produces conditions
            // whose field passed SearchFieldRegistry.isAllowed(...) -- this default exists only
            // to keep the switch exhaustive and safe if the registry and this translator ever
            // drift out of sync.
            default -> throw new IllegalArgumentException("Couldn't understand that query. Try rephrasing it.");
        };
    }

    private static BigDecimal parsePercentage(String value) {
        try {
            return new BigDecimal(value.replace("%", "").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't understand that query. Try rephrasing it.");
        }
    }
}
