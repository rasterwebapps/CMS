package com.cms.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonClinicalShiftHours;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.dto.SkeletonSubjectResponse;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

/** Server-side port of the Skeleton Builder frontend's own {@code hoursSummary()} computed signal
 *  (skeleton-builder.component.ts) — same grouping/capping rules, kept deliberately in lockstep so
 *  a term the frontend shows as fully covered can never disagree with what gates Approve. THEORY/
 *  LAB/CLINICAL only; Library has no curriculum-hours budget (matches the frontend, which excludes
 *  it from this same card). Always computed unscoped (the frontend's "ALL sections" mode) since a
 *  publish-time gate must see the cohort's whole term, not whatever section tab an admin happened
 *  to have open.
 *
 * <p>If the frontend's aggregation ever changes, mirror the change here — there is no shared
 *  runtime between the Angular client and this service. */
public final class TimetableCoverageCalculator {

    private TimetableCoverageCalculator() {
    }

    public record HoursBreakdown(double total, double assigned, double unassigned, double extra) {
        static final HoursBreakdown ZERO = new HoursBreakdown(0, 0, 0, 0);
    }

    private static final List<ClassSessionType> BUDGETED_TYPES =
        List.of(ClassSessionType.THEORY, ClassSessionType.LAB, ClassSessionType.CLINICAL);

    public static Map<ClassSessionType, HoursBreakdown> computeCoverage(SkeletonBuilderResponse skeleton) {
        // A cohort with zero placed cells (PENDING) reads as fully empty, Clinical included, even
        // though its Clinical Shift Group duty roster is configured independently of Run Automation
        // and would otherwise already count as "covered" here -- mirrors the same gate on the
        // frontend's hoursSummary() (OC-263).
        List<SkeletonClinicalShiftHours> clinicalShiftHours =
            skeleton.cells().isEmpty() ? List.of() : skeleton.clinicalShiftHours();

        Map<ClassSessionType, Double> total = new EnumMap<>(ClassSessionType.class);
        Map<ClassSessionType, Double> assigned = new EnumMap<>(ClassSessionType.class);
        Map<ClassSessionType, Double> extra = new EnumMap<>(ClassSessionType.class);
        Map<ClassSessionType, Double> unassigned = new EnumMap<>(ClassSessionType.class);
        for (ClassSessionType type : BUDGETED_TYPES) {
            total.put(type, 0.0);
            assigned.put(type, 0.0);
            extra.put(type, 0.0);
            unassigned.put(type, 0.0);
        }

        // Group subjects into logical units: every member of an elective group is a parallel
        // alternative competing for the same shared slot and is only counted once, matching the
        // Curriculum Map's own per-term hours total.
        Map<String, List<SkeletonSubjectResponse>> subjectGroups = new LinkedHashMap<>();
        for (SkeletonSubjectResponse subject : skeleton.subjects()) {
            String key = subject.electiveGroupId() != null
                ? "elective:" + subject.electiveGroupId()
                : "subject:" + subject.courseOfferingId();
            subjectGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(subject);
        }

        for (List<SkeletonSubjectResponse> group : subjectGroups.values()) {
            java.util.Set<Long> offeringIds = new java.util.LinkedHashSet<>();
            for (SkeletonSubjectResponse s : group) offeringIds.add(s.courseOfferingId());
            boolean isElectiveGroup = group.get(0).electiveGroupId() != null;

            for (ClassSessionType type : BUDGETED_TYPES) {
                List<SkeletonSubjectBudget> rows = new ArrayList<>();
                for (SkeletonSubjectResponse s : group) {
                    for (SkeletonSubjectBudget b : s.budgets()) {
                        if (b.sessionType() == type) rows.add(b);
                    }
                }
                if (rows.isEmpty()) continue;

                Map<Long, List<SkeletonSubjectBudget>> bySection = new LinkedHashMap<>();
                for (SkeletonSubjectBudget row : rows) {
                    // A null cohortSectionId (whole-cohort row) is kept as its own bucket via a
                    // sentinel — Java Maps handle a null key fine, but a sentinel keeps the
                    // (sectionId == null) checks below symmetric with the TS source.
                    Long key = row.cohortSectionId();
                    bySection.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
                }

                double subjectTotal = 0;
                double subjectAssigned = 0;
                for (Map.Entry<Long, List<SkeletonSubjectBudget>> entry : bySection.entrySet()) {
                    Long sectionId = entry.getKey();
                    List<SkeletonSubjectBudget> sectionRows = entry.getValue();
                    subjectTotal += sectionRows.get(0).totalHours();

                    List<SkeletonCellResponse> sectionCells = new ArrayList<>();
                    for (SkeletonCellResponse c : skeleton.cells()) {
                        if (c.courseOfferingId() != null && offeringIds.contains(c.courseOfferingId())
                            && c.sessionType() == type
                            && (java.util.Objects.equals(c.cohortSectionId(), sectionId) || c.cohortSectionId() == null)) {
                            sectionCells.add(c);
                        }
                    }
                    List<SkeletonCellResponse> countedCells;
                    if (isElectiveGroup) {
                        Map<String, SkeletonCellResponse> deduped = new LinkedHashMap<>();
                        for (SkeletonCellResponse c : sectionCells) {
                            deduped.put(c.dayOfWeek() + "|" + c.periodId(), c);
                        }
                        countedCells = new ArrayList<>(deduped.values());
                    } else {
                        countedCells = sectionCells;
                    }

                    double cellAssigned = 0;
                    for (SkeletonCellResponse c : countedCells) {
                        cellAssigned += hoursBetween(c) * occurrencesFor(c, skeleton);
                    }

                    double shiftAssigned = 0;
                    if (type == ClassSessionType.CLINICAL && sectionId != null) {
                        for (SkeletonClinicalShiftHours h : clinicalShiftHours) {
                            if (offeringIds.contains(h.courseOfferingId()) && sectionId.equals(h.cohortSectionId())) {
                                shiftAssigned += h.assignedHours();
                            }
                        }
                    }

                    subjectAssigned += isElectiveGroup
                        ? cellAssigned + shiftAssigned
                        : (cellAssigned + shiftAssigned) / sectionRows.size();
                }

                if (type == ClassSessionType.CLINICAL) {
                    for (SkeletonClinicalShiftHours h : clinicalShiftHours) {
                        if (offeringIds.contains(h.courseOfferingId()) && h.cohortSectionId() == null) {
                            subjectAssigned += h.assignedHours();
                        }
                    }
                }

                total.merge(type, subjectTotal, Double::sum);
                assigned.merge(type, Math.min(subjectAssigned, subjectTotal), Double::sum);
                extra.merge(type, Math.max(0, subjectAssigned - subjectTotal), Double::sum);
                unassigned.merge(type, Math.max(0, subjectTotal - subjectAssigned), Double::sum);
            }
        }

        Map<ClassSessionType, HoursBreakdown> result = new EnumMap<>(ClassSessionType.class);
        for (ClassSessionType type : BUDGETED_TYPES) {
            result.put(type, new HoursBreakdown(total.get(type), assigned.get(type), unassigned.get(type), extra.get(type)));
        }
        return result;
    }

    private static double hoursBetween(SkeletonCellResponse cell) {
        return Duration.between(cell.startTime(), cell.endTime()).toMinutes() / 60.0;
    }

    private static long occurrencesFor(SkeletonCellResponse cell, SkeletonBuilderResponse skeleton) {
        return cell.dayOfWeek() == DayOfWeek.SATURDAY ? skeleton.workingSaturdayCount() : skeleton.weeksInTerm();
    }
}
