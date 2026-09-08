package com.cms.inventory.procurement.dto;

/** Body for defer/reopen — a plain optional-notes action, same shape as
 *  {@code PurchaseRequisitionResolutionRequest}. */
public record WantedListResolutionRequest(String notes) {}
