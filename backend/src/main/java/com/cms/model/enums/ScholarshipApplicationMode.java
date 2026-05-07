package com.cms.model.enums;

/**
 * How a scholarship application is processed.
 *
 * <ul>
 *   <li>{@code INSTITUTION} — Internal scheme: college admin reviews and approves directly
 *       in this CMS (e.g. college merit scholarship, founder's grant).</li>
 *   <li>{@code GOVT_PORTAL} — External government scheme: actual application is submitted
 *       through a government portal (NSP, ePass Tamil Nadu, TNSMS, TNSCST). The CMS only
 *       tracks status — the college acts as a forwarding institution.</li>
 * </ul>
 */
public enum ScholarshipApplicationMode {
    INSTITUTION,
    GOVT_PORTAL
}

