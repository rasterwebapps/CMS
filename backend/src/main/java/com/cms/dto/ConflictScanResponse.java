package com.cms.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Result of scanning one term's whole DRAFT+PUBLISHED {@code ClassSchedule} set for structural
 *  violations (room/faculty conflicts, workload caps, faculty availability, blocked periods,
 *  capacity fit) — the same checks {@link com.cms.service.TimetableStaffingService} already
 *  enforces reactively at staffing time, re-run here across everything already placed.
 *  {@code rows} only includes cells that have at least one violation. */
public record ConflictScanResponse(
    Long termInstanceId,
    String termLabel,
    Instant scannedAt,
    int scannedCellCount,
    int violationCellCount,
    int violationCount,
    Map<String, Integer> countsByCode,
    List<TimetableConflictRow> rows
) {
}
