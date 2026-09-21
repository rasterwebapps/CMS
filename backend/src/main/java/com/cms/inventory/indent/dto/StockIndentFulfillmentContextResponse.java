package com.cms.inventory.indent.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * The context a store needs to make its fulfillment decision on an {@code APPROVED} line — its
 * own current stock, and other active locations currently holding surplus of the same product, so
 * "transfer in from a sub-location with abundant stock" is a real choice, not a guess. See {@code
 * StockIndentService.getFulfillmentContext}.
 */
public record StockIndentFulfillmentContextResponse(
    BigDecimal requestedQty,
    BigDecimal issuingLocationQtyOnHand,
    List<CandidateSourceLocation> candidateSourceLocations
) {
    public record CandidateSourceLocation(Long locationId, String locationVirtualName, BigDecimal qtyOnHand) {}
}
