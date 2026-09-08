package com.cms.inventory.ticket.model.enums;

/**
 * {@code OPEN → IN_PROGRESS → RESOLVED → CLOSED}, with {@code CANCELLED} reachable from
 * {@code OPEN} or {@code IN_PROGRESS} only — a ticket already {@code RESOLVED} or {@code CLOSED}
 * cannot be cancelled, matching this module's standing "cancel only from an early state" rule
 * (same shape as {@code CycleCount}'s own {@code CANCELLED}-only-from-{@code DRAFT} gate). See
 * the "Service Ticket slice" decision-log entry.
 */
public enum ServiceTicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED
}
