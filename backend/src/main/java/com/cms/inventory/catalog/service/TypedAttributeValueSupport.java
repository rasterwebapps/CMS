package com.cms.inventory.catalog.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import com.cms.inventory.catalog.model.CategoryAttribute;
import com.cms.inventory.catalog.model.TypedAttributeValue;

/**
 * Parses a plain form/API string per a {@code CategoryAttribute}'s declared {@code
 * AttributeDataType} into the right typed column of a {@link TypedAttributeValue} — the "typed
 * EAV storage" behavior (2026-09-11 DECISION_LOG entry), shared between {@code ProductService}
 * (a product's own attribute values) and {@code ProductVariantService} (a variant's) since
 * 2026-09-11's "ProductVariant" entry extracted this out of {@code ProductService} to avoid the
 * two duplicating it.
 */
final class TypedAttributeValueSupport {

    private TypedAttributeValueSupport() {}

    static void applyTypedValue(TypedAttributeValue target, CategoryAttribute def, String raw) {
        switch (def.getDataType()) {
            case NUMBER -> {
                try {
                    target.setNumberValue(new BigDecimal(raw));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Attribute '" + def.getName() + "' expects a number");
                }
            }
            case DATE -> {
                try {
                    target.setDateValue(LocalDate.parse(raw));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Attribute '" + def.getName() + "' expects a date (yyyy-MM-dd)");
                }
            }
            case BOOLEAN -> {
                if (!"true".equalsIgnoreCase(raw) && !"false".equalsIgnoreCase(raw)) {
                    throw new IllegalArgumentException("Attribute '" + def.getName() + "' expects true or false");
                }
                target.setBooleanValue(Boolean.parseBoolean(raw));
            }
            case ENUM -> {
                List<String> options = parseEnumOptions(def.getEnumOptions());
                if (!options.contains(raw)) {
                    throw new IllegalArgumentException("Attribute '" + def.getName() + "' must be one of: " + String.join(", ", options));
                }
                target.setTextValue(raw);
            }
            case TEXT -> target.setTextValue(raw);
        }
    }

    static List<String> parseEnumOptions(String enumOptions) {
        if (enumOptions == null) return List.of();
        return Arrays.stream(enumOptions.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
