package com.cms.ai;

import java.util.List;

/**
 * A validated, structured filter parsed from a natural-language query -- the only shape a
 * {@link StudentSearchIntentParser} result may take before it's translated into a
 * {@code Specification<Student>} by {@link StudentSearchIntentTranslator}. Every
 * {@link Condition#field()} is guaranteed (by the parser) to exist in
 * {@link SearchFieldRegistry} and every {@link Condition#operator()} is guaranteed to be
 * allowed for that field.
 */
public record SearchIntent(List<Condition> conditions) {

    public record Condition(String field, SearchOperator operator, String value) {
    }
}
