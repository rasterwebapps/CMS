package com.cms.service;

import java.util.List;

import com.cms.dto.CohortTermOption;
import com.cms.dto.PromotionExecuteRequest;
import com.cms.dto.PromotionExecuteResponse;
import com.cms.dto.PromotionPreviewRequest;
import com.cms.dto.PromotionPreviewResponse;
import com.cms.dto.StudentPromotionDecisionDto;

public interface StudentPromotionService {

    /** Term instances a cohort currently has ENROLLED students in — usually exactly one, but can
     *  be more than one after a partial promotion round (some students moved on, others detained
     *  and left behind). Lets the UI skip the academic-year/term cascade for the common case. */
    List<CohortTermOption> getActiveTermsForCohort(Long cohortId);

    /** The chronologically next term after the given one (same academic year's EVEN term after
     *  ODD, or the next academic year's ODD term after EVEN) — null if that term doesn't exist
     *  yet (e.g. the next academic year hasn't been created). */
    CohortTermOption suggestNextTerm(Long fromTermInstanceId);

    PromotionPreviewResponse previewPromotion(PromotionPreviewRequest request);

    PromotionExecuteResponse executePromotion(PromotionExecuteRequest request, String decidedBy);

    List<StudentPromotionDecisionDto> getHistoryByCohort(Long cohortId);

    List<StudentPromotionDecisionDto> getHistoryByStudent(Long studentId);
}
