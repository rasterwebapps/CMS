package com.cms.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.TimetableCoverageGap;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.repository.StudentTermEnrollmentRepository;

/** Finds every cohort in a term whose curriculum-required Theory/Lab/Clinical hours haven't all
 *  been placed as real {@code ClassSchedule} rows yet — the gate {@code
 *  TimetableGenerationService#approve} checks before publishing (OC-256). Reuses {@link
 *  TimetableSkeletonService#getCohortSkeleton} rather than querying independently, so this can
 *  never see a different picture than what Skeleton Builder itself already shows that cohort's
 *  admin via its "Total Unassigned" stat cards. */
@Service
@Transactional(readOnly = true)
public class TimetableCoverageService {

    private static final double TOLERANCE_HOURS = 0.05;

    private final TimetableSkeletonService timetableSkeletonService;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;

    public TimetableCoverageService(TimetableSkeletonService timetableSkeletonService,
                                     StudentTermEnrollmentRepository studentTermEnrollmentRepository) {
        this.timetableSkeletonService = timetableSkeletonService;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
    }

    public List<TimetableCoverageGap> findGaps(Long termInstanceId) {
        Set<Long> cohortIds = studentTermEnrollmentRepository
            .findDistinctCohortIdsByTermInstanceId(termInstanceId, EnrollmentStatus.ENROLLED);
        List<TimetableCoverageGap> gaps = new ArrayList<>();
        for (Long cohortId : cohortIds) {
            SkeletonBuilderResponse skeleton = timetableSkeletonService.getCohortSkeleton(termInstanceId, cohortId);
            Map<com.cms.model.enums.ClassSessionType, TimetableCoverageCalculator.HoursBreakdown> coverage =
                TimetableCoverageCalculator.computeCoverage(skeleton);
            coverage.forEach((sessionType, breakdown) -> {
                if (breakdown.unassigned() > TOLERANCE_HOURS) {
                    gaps.add(new TimetableCoverageGap(cohortId, skeleton.cohortName(), sessionType,
                        breakdown.total(), breakdown.assigned(), breakdown.unassigned()));
                }
            });
        }
        return gaps;
    }
}
