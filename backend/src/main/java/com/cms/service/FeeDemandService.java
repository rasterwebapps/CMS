package com.cms.service;

import java.util.List;

import com.cms.dto.FeeDemandDto;
import com.cms.model.enums.DemandStatus;

public interface FeeDemandService {

    /**
     * Generates fee demands for all ENROLLED students in the given term instance.
     * Idempotent — skips students who already have a demand.
     *
     * @return the count of new demands created
     */
    int generateDemandsForTermInstance(Long termInstanceId);

    List<FeeDemandDto> getDemandsByTermInstance(Long termInstanceId);

    List<FeeDemandDto> getDemandsByTermInstanceAndStatus(Long termInstanceId, DemandStatus status);

    FeeDemandDto getDemandByEnrollment(Long enrollmentId);

    FeeDemandDto getById(Long id);

    List<FeeDemandDto> getOutstandingDemands(Long termInstanceId);

    List<FeeDemandDto> getDemandsByStudent(Long studentId);
}
