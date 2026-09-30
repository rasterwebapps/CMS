package com.cms.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.cms.model.Student;

/**
 * Verifies every {@link SearchFieldRegistry} entry maps to a real {@code Specification<Student>}
 * -- catches the case where a field is added to the registry (so the parser will accept it) but
 * its translator branch is missing or mistyped. Does not execute any query against a database:
 * {@code Specification} is just a lambda until a repository runs it, so these are pure,
 * deterministic unit tests.
 */
class StudentSearchIntentTranslatorTest {

    private final StudentSearchIntentTranslator translator = new StudentSearchIntentTranslator();

    @Test
    void translate_producesANonNullSpecificationForEveryAllowListedField() {
        for (SearchField field : SearchFieldRegistry.all().values()) {
            SearchOperator operator = field.allowedOperators()[0];
            String value = sampleValueFor(field);

            SearchIntent intent = new SearchIntent(
                List.of(new SearchIntent.Condition(field.name(), operator, value)));

            Specification<Student> spec = translator.translate(intent);

            assertThat(spec)
                .as("Specification for field '%s'", field.name())
                .isNotNull();
        }
    }

    @Test
    void translate_composesMultipleConditionsIntoOneSpecification() {
        SearchIntent intent = new SearchIntent(List.of(
            new SearchIntent.Condition("previousSchoolOrCollegeName", SearchOperator.CONTAINS, "St. Mary's School"),
            new SearchIntent.Condition("status", SearchOperator.EQUALS, "ACTIVE")
        ));

        assertThat(translator.translate(intent)).isNotNull();
    }

    @Test
    void translate_rejectsANonNumericValueForQualificationPercentage() {
        SearchIntent intent = new SearchIntent(List.of(
            new SearchIntent.Condition("qualificationPercentage", SearchOperator.GTE, "not-a-number")
        ));

        assertThatThrownBy(() -> translator.translate(intent))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static String sampleValueFor(SearchField field) {
        return switch (field.type()) {
            case NUMBER -> "75";
            case ENUM -> switch (field.name()) {
                case "status" -> "ACTIVE";
                case "admissionCategory" -> "MANAGEMENT";
                case "gender" -> "MALE";
                case "qualificationType" -> "DEGREE";
                default -> throw new IllegalStateException("Unmapped enum field: " + field.name());
            };
            case TEXT -> "sample";
        };
    }
}
