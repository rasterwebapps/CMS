package com.cms.dto;

import com.cms.model.enums.AdmissionQuota;

public record CohortQuotaStatusRequest(AdmissionQuota quota, boolean closed) {}
