package com.cms.inventory.catalog.model.enums;

/**
 * How a {@code Product}'s stock is tracked at the {@code StockBatch} level — replaces the
 * previously-ambiguous free-text {@code StockBatch.batchOrSerialNo}, which any product could
 * populate or leave blank with no declared meaning. See the 2026-09-11 DECISION_LOG entry.
 */
public enum StockTrackingMode {
    /** No batch/serial discipline enforced — {@code batchOrSerialNo} stays optional free text,
     *  exactly today's pre-existing behavior. The default for every product until explicitly
     *  opted into BATCH or SERIAL. */
    NONE,
    /** A batch number is required on every movement; one batch can hold many units (a received
     *  lot), so no quantity restriction. */
    BATCH,
    /** A serial number is required on every movement, and it identifies exactly one physical
     *  unit — quantity must be 1 per movement. */
    SERIAL
}
