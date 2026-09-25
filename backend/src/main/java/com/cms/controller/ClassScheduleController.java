package com.cms.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ClassScheduleRequest;
import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ScheduleConflictResponse;
import com.cms.model.Lab;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.TermInstanceRepository;
import com.cms.service.ClassScheduleExportService;
import com.cms.service.ClassScheduleService;
import com.cms.util.ExportSortUtils;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.ExportResponseFactory;

import jakarta.validation.Valid;

/** Route/permission codes intentionally kept as `/lab-schedules` and `LAB_SCHEDULE_*` — this
 *  screen now edits both THEORY and LAB sessions (see the sessionType field), but renaming the
 *  route or permission codes would mean re-touching every role's role_permissions row for no
 *  functional gain. See ClassSchedule for the underlying rename rationale. */
@RestController
@RequestMapping("/lab-schedules")
public class ClassScheduleController {

    /** Every allowed field must be a real, directly-queryable entity property — Room/Subject/
     *  Faculty/Start/End all resolve through a @ManyToOne relation (or, for Start/End, through
     *  Period), so per this codebase's sort-key gate they're left out here even though the UI
     *  shows those columns; sorting by them stays disabled on the grid and the export allow-list
     *  matches that same restriction so "sorted by" in a download always means what it says. */
    private static final Map<String, String> EXPORT_SORT_FIELDS = new LinkedHashMap<>();
    static {
        EXPORT_SORT_FIELDS.put("dayOrder", "Day");
        EXPORT_SORT_FIELDS.put("sessionType", "Type");
        EXPORT_SORT_FIELDS.put("batchName", "Batch");
    }

    private final ClassScheduleService classScheduleService;
    private final ClassScheduleExportService classScheduleExportService;
    private final LabRepository labRepository;
    private final FacultyRepository facultyRepository;
    private final TermInstanceRepository termInstanceRepository;

    public ClassScheduleController(ClassScheduleService classScheduleService,
                                    ClassScheduleExportService classScheduleExportService,
                                    LabRepository labRepository,
                                    FacultyRepository facultyRepository,
                                    TermInstanceRepository termInstanceRepository) {
        this.classScheduleService = classScheduleService;
        this.classScheduleExportService = classScheduleExportService;
        this.labRepository = labRepository;
        this.facultyRepository = facultyRepository;
        this.termInstanceRepository = termInstanceRepository;
    }

    @PostMapping
    @PreAuthorize("@perm.has('LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<ClassScheduleResponse> create(@Valid @RequestBody ClassScheduleRequest request) {
        ClassScheduleResponse response = classScheduleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("@perm.hasAny('LAB_SCHEDULE_VIEW', 'LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<List<ClassScheduleResponse>> findAll(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long termInstanceId,
            @RequestParam(required = false) String batchName,
            @RequestParam(required = false) DayOfWeek dayOfWeek) {
        List<ClassScheduleResponse> schedules;
        if (facultyId != null && termInstanceId != null) {
            // Faculty Detail's Lab Schedules tab -- scoped to both PUBLISHED and DRAFT, same
            // convention as everywhere else this session (see ClassScheduleService's javadoc).
            schedules = classScheduleService.findByFacultyIdAndTermInstanceId(facultyId, termInstanceId);
        } else if (labId != null) {
            schedules = classScheduleService.findByLabId(labId);
        } else if (facultyId != null) {
            schedules = classScheduleService.findByFacultyId(facultyId);
        } else if (batchName != null) {
            schedules = classScheduleService.findByBatchName(batchName);
        } else if (dayOfWeek != null) {
            schedules = classScheduleService.findByDayOfWeek(dayOfWeek);
        } else {
            schedules = classScheduleService.findAll();
        }
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('LAB_SCHEDULE_VIEW', 'LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<Page<ClassScheduleResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long termInstanceId,
            @RequestParam(required = false) DayOfWeek dayOfWeek,
            @RequestParam(required = false) ClassSessionType sessionType,
            @PageableDefault(size = 25, sort = "dayOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        // Default (no explicit sort param) reads Monday→Saturday then earliest period first,
        // matching how a weekly timetable is actually read; an explicit user sort (column click)
        // is respected as-is, with no forced secondary key.
        Pageable effective = pageable.getSort().getOrderFor("dayOrder") != null
            ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by(Sort.Direction.ASC, "period.startTime")))
            : pageable;
        return ResponseEntity.ok(classScheduleService.findPage(
            search, labId, facultyId, termInstanceId, dayOfWeek, sessionType, effective));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('LAB_SCHEDULE_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long termInstanceId,
            @RequestParam(required = false) DayOfWeek dayOfWeek,
            @RequestParam(required = false) ClassSessionType sessionType,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {

        Sort exportSort = ExportSortUtils.resolve(
            sort, direction, EXPORT_SORT_FIELDS.keySet(), "dayOrder", Sort.Direction.ASC);
        List<ClassScheduleResponse> data = classScheduleService.findAllMatching(
            search, labId, facultyId, termInstanceId, dayOfWeek, sessionType, exportSort);

        String labLabel = labId != null ? labRepository.findById(labId).map(Lab::getName).orElse(null) : null;
        String facultyLabel = facultyId != null
            ? facultyRepository.findById(facultyId).map(f -> f.getFirstName() + " " + f.getLastName()).orElse(null)
            : null;
        String termLabel = termInstanceId != null
            ? termInstanceRepository.findById(termInstanceId)
                .map(ti -> ti.getAcademicYear().getName() + " " + ti.getTermType()).orElse(null)
            : null;

        Sort.Order order = ExportSortUtils.firstOrder(exportSort, "dayOrder", Sort.Direction.ASC);
        ExportMetadata meta = ExportMetadata.of("Class Schedules Export")
            .filter("Search", search)
            .filter("Room", labLabel)
            .filter("Faculty", facultyLabel)
            .filter("Term", termLabel)
            .filter("Day", dayOfWeek != null ? dayOfWeek.name() : null)
            .filter("Type", sessionType != null ? sessionType.name() : null)
            .sort(EXPORT_SORT_FIELDS.get(order.getProperty()), order.getDirection());

        return ExportResponseFactory.respond(format, "class-schedules",
            () -> classScheduleExportService.toExcel(data, meta),
            () -> classScheduleExportService.toPdf(data, meta));
    }

    @GetMapping("/by-term/{termInstanceId}")
    @PreAuthorize("@perm.hasAny('LAB_SCHEDULE_VIEW', 'LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<List<ClassScheduleResponse>> findByTermInstance(@PathVariable Long termInstanceId) {
        return ResponseEntity.ok(classScheduleService.findByTermInstanceId(termInstanceId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('LAB_SCHEDULE_VIEW', 'LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<ClassScheduleResponse> findById(@PathVariable Long id) {
        ClassScheduleResponse response = classScheduleService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-conflicts")
    @PreAuthorize("@perm.has('LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<ScheduleConflictResponse> checkConflicts(
            @Valid @RequestBody ClassScheduleRequest request) {
        ScheduleConflictResponse response = classScheduleService.checkConflicts(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<ClassScheduleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ClassScheduleRequest request) {
        ClassScheduleResponse response = classScheduleService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('LAB_SCHEDULE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        classScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
