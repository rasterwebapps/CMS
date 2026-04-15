package com.cms.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LabScheduleRequest;
import com.cms.dto.LabScheduleResponse;
import com.cms.dto.ScheduleConflictResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Subject;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.LabSchedule;
import com.cms.model.LabSlot;
import com.cms.model.Semester;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.SubjectRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.LabScheduleRepository;
import com.cms.repository.LabSlotRepository;
import com.cms.repository.SemesterRepository;

@Service
@Transactional(readOnly = true)
public class LabScheduleService {

    private final LabScheduleRepository labScheduleRepository;
    private final LabRepository labRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final LabSlotRepository labSlotRepository;
    private final SemesterRepository semesterRepository;

    public LabScheduleService(LabScheduleRepository labScheduleRepository,
                               LabRepository labRepository,
                               SubjectRepository subjectRepository,
                               FacultyRepository facultyRepository,
                               LabSlotRepository labSlotRepository,
                               SemesterRepository semesterRepository) {
        this.labScheduleRepository = labScheduleRepository;
        this.labRepository = labRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.labSlotRepository = labSlotRepository;
        this.semesterRepository = semesterRepository;
    }

    @Transactional
    public LabScheduleResponse create(LabScheduleRequest request) {
        Lab lab = labRepository.findById(request.labId())
            .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + request.labId()));

        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));

        Faculty faculty = facultyRepository.findById(request.facultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.facultyId()));

        LabSlot labSlot = labSlotRepository.findById(request.labSlotId())
            .orElseThrow(() -> new ResourceNotFoundException("Lab slot not found with id: " + request.labSlotId()));

        Semester semester = semesterRepository.findById(request.semesterId())
            .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + request.semesterId()));

        Boolean isActive = request.isActive() != null ? request.isActive() : true;

        LabSchedule labSchedule = new LabSchedule(
            lab, subject, faculty, labSlot,
            request.batchName(), request.dayOfWeek(),
            semester, isActive
        );

        LabSchedule saved = labScheduleRepository.save(labSchedule);
        return toResponse(saved);
    }

    public List<LabScheduleResponse> findAll() {
        return labScheduleRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public LabScheduleResponse findById(Long id) {
        LabSchedule labSchedule = labScheduleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab schedule not found with id: " + id));
        return toResponse(labSchedule);
    }

    public List<LabScheduleResponse> findByLabId(Long labId) {
        if (!labRepository.existsById(labId)) {
            throw new ResourceNotFoundException("Lab not found with id: " + labId);
        }
        return labScheduleRepository.findByLabId(labId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<LabScheduleResponse> findByFacultyId(Long facultyId) {
        if (!facultyRepository.existsById(facultyId)) {
            throw new ResourceNotFoundException("Faculty not found with id: " + facultyId);
        }
        return labScheduleRepository.findByFacultyId(facultyId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<LabScheduleResponse> findByBatchName(String batchName) {
        return labScheduleRepository.findByBatchName(batchName).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<LabScheduleResponse> findByDayOfWeek(DayOfWeek dayOfWeek) {
        return labScheduleRepository.findByDayOfWeek(dayOfWeek).stream()
            .map(this::toResponse)
            .toList();
    }

    public ScheduleConflictResponse checkConflicts(LabScheduleRequest request) {
        List<String> labConflicts = new ArrayList<>();
        List<String> facultyConflicts = new ArrayList<>();
        List<String> batchConflicts = new ArrayList<>();

        List<LabSchedule> conflictingLabSchedules = labScheduleRepository.findConflictingLabSchedules(
            request.labId(), request.dayOfWeek(), request.labSlotId());
        for (LabSchedule conflict : conflictingLabSchedules) {
            labConflicts.add(String.format("Lab is already scheduled for %s on %s",
                conflict.getSubject().getName(), conflict.getDayOfWeek()));
        }

        List<LabSchedule> conflictingFacultySchedules = labScheduleRepository.findConflictingFacultySchedules(
            request.facultyId(), request.dayOfWeek(), request.labSlotId());
        for (LabSchedule conflict : conflictingFacultySchedules) {
            facultyConflicts.add(String.format("Faculty is already scheduled for %s on %s",
                conflict.getSubject().getName(), conflict.getDayOfWeek()));
        }

        List<LabSchedule> conflictingBatchSchedules = labScheduleRepository.findConflictingBatchSchedules(
            request.batchName(), request.dayOfWeek(), request.labSlotId());
        for (LabSchedule conflict : conflictingBatchSchedules) {
            batchConflicts.add(String.format("Batch is already scheduled for %s on %s",
                conflict.getSubject().getName(), conflict.getDayOfWeek()));
        }

        boolean hasConflict = !labConflicts.isEmpty() || !facultyConflicts.isEmpty() || !batchConflicts.isEmpty();
        return new ScheduleConflictResponse(hasConflict, labConflicts, facultyConflicts, batchConflicts);
    }

    @Transactional
    public LabScheduleResponse update(Long id, LabScheduleRequest request) {
        LabSchedule labSchedule = labScheduleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab schedule not found with id: " + id));

        Lab lab = labRepository.findById(request.labId())
            .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + request.labId()));

        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));

        Faculty faculty = facultyRepository.findById(request.facultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.facultyId()));

        LabSlot labSlot = labSlotRepository.findById(request.labSlotId())
            .orElseThrow(() -> new ResourceNotFoundException("Lab slot not found with id: " + request.labSlotId()));

        Semester semester = semesterRepository.findById(request.semesterId())
            .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + request.semesterId()));

        labSchedule.setLab(lab);
        labSchedule.setSubject(subject);
        labSchedule.setFaculty(faculty);
        labSchedule.setLabSlot(labSlot);
        labSchedule.setBatchName(request.batchName());
        labSchedule.setDayOfWeek(request.dayOfWeek());
        labSchedule.setSemester(semester);

        if (request.isActive() != null) {
            labSchedule.setIsActive(request.isActive());
        }

        LabSchedule updated = labScheduleRepository.save(labSchedule);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!labScheduleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lab schedule not found with id: " + id);
        }
        labScheduleRepository.deleteById(id);
    }

    private LabScheduleResponse toResponse(LabSchedule ls) {
        return new LabScheduleResponse(
            ls.getId(),
            ls.getLab().getId(),
            ls.getLab().getName(),
            ls.getSubject().getId(),
            ls.getSubject().getName(),
            ls.getSubject().getCode(),
            ls.getFaculty().getId(),
            ls.getFaculty().getFirstName() + " " + ls.getFaculty().getLastName(),
            ls.getLabSlot().getId(),
            ls.getLabSlot().getName(),
            ls.getLabSlot().getStartTime(),
            ls.getLabSlot().getEndTime(),
            ls.getBatchName(),
            ls.getDayOfWeek(),
            ls.getSemester().getId(),
            ls.getSemester().getName(),
            ls.getIsActive(),
            ls.getCreatedAt(),
            ls.getUpdatedAt()
        );
    }
}
