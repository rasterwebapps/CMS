package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Collectively converts several selected Wanted List lines — all for the same location — into
 * one new Purchase Requisition, mirroring standard MRP "collective conversion" of Planned Orders.
 * {@code qty} lets the planner adjust the suggested quantity before it's firmed into a real
 * request; omitted means "use the line's own suggestedQty as computed".
 */
public record WantedListConvertRequest(
    @NotEmpty @Valid List<Line> lines,
    LocalDate requisitionDate,
    String notes
) {
    public record Line(
        @NotNull Long itemId,
        @Positive BigDecimal qty
    ) {}
}
