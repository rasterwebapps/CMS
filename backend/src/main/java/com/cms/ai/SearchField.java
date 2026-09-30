package com.cms.ai;

/**
 * One entry in the {@link SearchFieldRegistry} allow-list: a name the LLM is permitted to
 * reference, what kind of value it takes, and which operators are valid for it. This is the
 * safety boundary for AI Smart Search -- the LLM never sees a database column or table name,
 * only these field names, and the backend never executes a filter referencing a field outside
 * this registry.
 */
public record SearchField(
    String name,
    String description,
    SearchFieldType type,
    SearchOperator[] allowedOperators
) {
}
