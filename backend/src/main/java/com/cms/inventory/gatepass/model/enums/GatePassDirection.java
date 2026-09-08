package com.cms.inventory.gatepass.model.enums;

/**
 * Which way the item is crossing the gate. {@code OUTWARD} covers the common repair/loan/event
 * send-out case (an item leaving the premises, most often expected back); {@code INWARD} covers
 * an item entering that isn't going through a normal Goods Receipt — a vendor's own tool brought
 * in for an on-site repair job, a contractor's equipment, or similar. See the "Gate Pass slice"
 * decision-log entry.
 */
public enum GatePassDirection {
    OUTWARD,
    INWARD
}
