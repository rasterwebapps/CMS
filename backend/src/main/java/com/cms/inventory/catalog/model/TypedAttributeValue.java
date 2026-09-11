package com.cms.inventory.catalog.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The typed-EAV storage shape shared by {@link ProductAttributeValue} and {@link
 * com.cms.inventory.catalog.model.ProductVariantAttributeValue} — exactly one of {@code
 * textValue}/{@code numberValue}/{@code dateValue}/{@code booleanValue} is ever populated per row,
 * chosen by {@link #getAttribute()}'s declared {@code AttributeDataType}. Extracted as an
 * interface (2026-09-11 "ProductVariant" decision-log entry) so {@code
 * TypedAttributeValueSupport}'s parsing/validation and this interface's own {@link #renderValue()}
 * are written once and shared by both a product's own attribute values and a variant's — see the
 * original 2026-09-11 "Typed EAV storage" entry for why parsing/validation lives outside the
 * entity itself.
 */
public interface TypedAttributeValue {

    CategoryAttribute getAttribute();

    String getTextValue();
    void setTextValue(String textValue);

    BigDecimal getNumberValue();
    void setNumberValue(BigDecimal numberValue);

    LocalDate getDateValue();
    void setDateValue(LocalDate dateValue);

    Boolean getBooleanValue();
    void setBooleanValue(Boolean booleanValue);

    /** Renders whichever typed column is populated back to its plain-string form/API representation. */
    default String renderValue() {
        return switch (getAttribute().getDataType()) {
            case TEXT, ENUM -> getTextValue();
            case NUMBER -> getNumberValue() == null ? null : getNumberValue().stripTrailingZeros().toPlainString();
            case DATE -> getDateValue() == null ? null : getDateValue().toString();
            case BOOLEAN -> getBooleanValue() == null ? null : getBooleanValue().toString();
        };
    }
}
