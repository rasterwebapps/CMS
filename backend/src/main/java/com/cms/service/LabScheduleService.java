package com.cms.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LabScheduleRequest;
import com.cms.dto.LabScheduleResponse;
import com.cms.dto.ScheduleConflictResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.LabSchedule;
import com.cms.model.LabSlot;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BatchRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.LabScheduleRepository;
import com.cms.repository.LabSlotRepository;
import com.cms.repository.SubjectRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class LabScheduleService {

    private final LabScheduleRepository labScheduleRepository;
    private final LabRepository labRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final LabSlotRepository labSlotRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final BatchRepository batchRepository;

    public LabScheduleService(LabScheduleRepository labScheduleRepository,
                               LabRepository labRepository,
                               SubjectRepository subjectRepository,
                               FacultyRepository facultyRepository,
                               LabSlotRepository labSlotRepository,
                               TermInstanceRepository termInstanceRepository,
                               BatchRepository batchRepository) {
        this.labScheduleRepository = labScheduleRepository;
        this.labRepository = labRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.labSlotRepository = labSlotRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.batchRepository = batchRepository;
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
        TermInstance termInstance = termInstanceRepository.findById(request.termInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + request.termInstanceId()));

        Boolean isActive = request.isActive() != null ? request.isActive() : true;
        LabSchedule labSchedule = new LabSchedule(
            lab, subject, faculty, labSlot,
            request.batchName(), request.dayOfWeek(),
            termInstance, isActive
        );
        labSchedule.setBatch(resolveBatch(request.batchId()));
        return toResponse(labScheduleRepository.save(labSchedule));
    }

    private Batch resolveBatch(Long batchId) {
        if (batchId == null) {
            return null;
        }
        return batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));
    }

    public List<LabScheduleResponse> findAll() {
        return labScheduleRepository.findAll().stream().map(this::toResponse).toList();
    }

    public LabScheduleResponse findById(Long id) {
        return toResponse(labScheduleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab schedule not found with id: " + id)));
    }

    public List<LabScheduleResponse> findByLabId(Long labId) {
        if (!labRepository.existsById(labId)) {
            throw new ResourceNotFoundException("Lab not found with id: " + labId);
        }
        return labScheduleRepository.findByLabId(labId).stream().map(this::toResponse).toList();
    }

    public List<LabScheduleResponse> findByFacultyId(Long facultyId) {
        if (!facultyRepository.existsById(facultyId)) {
            throw new ResourceNotFoundException("Faculty not found with id: " + facultyId);
        }
        return labScheduleRepository.findByFacultyId(facultyId).stream().map(this::toResponse).toList();
    }

    public List<LabScheduleResponse> findByBatchName(String batchName) {
        return labScheduleRepository.findByBatchName(batchName).stream().map(this::toResponse).toList();
    }

    public List<LabScheduleResponse> findByDayOfWeek(DayOfWeek dayOfWeek) {
        return labScheduleRepository.findByDayOfWeek(dayOfWeek).stream().map(this::toResponse).toList();
    }

    public List<LabScheduleResponse> findByTermInstanceId(Long termInstanceId) {
        if (!termInstanceRepository.existsById(termInstanceId)) {
            throw new ResourceNotFoundException("Term instance not found with id: " + termInstanceId);
        }
        return labScheduleRepository.findByTermInstanceId(termInstanceId).stream().map(this::toResponse).toList();
    }

    public ScheduleConflictResponse checkConflicts(LabScheduleRequest request) {
        List<String> labConflicts = new ArrayList<>();
        List<String> facultyConflicts = new ArrayList<>();
        List<String> batchConflicts = new ArrayList<>();

        for (LabSchedule c : labScheduleRepository.findConflictingLabSchedules(
                request.labId(), request.dayOfWeek(), request.labSlotId())) {
            labConflicts.add(String.format("Lab is already scheduled for %s on %s",
                c.getSubject().getName(), c.getDayOfWeek()));
        }
        for (LabSchedule c : labScheduleRepository.findConflictingFacultySchedules(
                request.facultyId(), request.dayOfWeek(), request.labSlotId())) {
            facultyConflicts.add(String.format("Faculty is already scheduled for %s on %s",
                c.getSubject().getName(), c.getDayOfWeek()));
        }
        for (LabSchedule c : labScheduleRepository.findConflictingBatchSchedules(
                request.batchName(), request.dayOfWeek(), request.labSlotId())) {
            batchConflicts.add(String.format("Batch is already scheduled for %s on %s",
                c.getSubject().getName(), c.getDayOfWeek()));
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
        TermInstance termInstance = termInstanceRepository.findById(request.termInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + request.termInstanceId()));

        labSchedule.setLab(lab);
        labSchedule.setSubject(subject);
        labSchedule.setFaculty(faculty);
        labSchedule.setLabSlot(labSlot);
        labSchedule.setBatchName(request.batchName());
        labSchedule.setBatch(resolveBatch(request.batchId()));
        labSchedule.setDayOfWeek(request.dayOfWeek());
        labSchedule.setTermInstance(termInstance);
        if (request.isActive() != null) {
            labSchedule.setIsActive(request.isActive());
        }
        return toResponse(labScheduleRepository.save(labSchedule));
    }

    @Transactional
    public void delete(Long id) {
        if (!labScheduleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lab schedule not found with id: " + id);
        }
        labScheduleRepository.deleteById(id);
    }

    private LabScheduleResponse toResponse(LabSchedule ls) {
        TermInstance ti = ls.getTermInstance();
        String label = ti.getAcademicYear().getName() + " " + ti.getTermType();
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
            ls.getBatch() != null ? ls.getBatch().getId() : null,
            ls.getDayOfWeek(),
            ti.getId(),
            label,
            ls.getIsActive(),
            ls.getCreatedAt(),
            ls.getUpdatedAt()
        );
    }
}
