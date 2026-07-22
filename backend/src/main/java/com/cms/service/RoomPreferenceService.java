package com.cms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.RoomPreferenceRequest;
import com.cms.dto.RoomPreferenceResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.HostelRoomType;
import com.cms.model.RoomPreference;
import com.cms.model.Student;
import com.cms.model.Zone;
import com.cms.model.enums.RoomPreferenceStatus;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.HostelRoomTypeRepository;
import com.cms.repository.RoomPreferenceRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.ZoneRepository;

@Service
@Transactional(readOnly = true)
public class RoomPreferenceService {

    private final RoomPreferenceRepository roomPreferenceRepository;
    private final EnquiryRepository enquiryRepository;
    private final StudentRepository studentRepository;
    private final HostelRoomTypeRepository hostelRoomTypeRepository;
    private final ZoneRepository zoneRepository;

    public RoomPreferenceService(RoomPreferenceRepository roomPreferenceRepository,
                                  EnquiryRepository enquiryRepository,
                                  StudentRepository studentRepository,
                                  HostelRoomTypeRepository hostelRoomTypeRepository,
                                  ZoneRepository zoneRepository) {
        this.roomPreferenceRepository = roomPreferenceRepository;
        this.enquiryRepository = enquiryRepository;
        this.studentRepository = studentRepository;
        this.hostelRoomTypeRepository = hostelRoomTypeRepository;
        this.zoneRepository = zoneRepository;
    }

    @Transactional
    public RoomPreferenceResponse create(RoomPreferenceRequest request) {
        if (request.enquiryId() == null && request.studentId() == null) {
            throw new IllegalArgumentException("A room preference must be linked to an enquiry or a student");
        }
        if (request.enquiryId() != null && roomPreferenceRepository.findByEnquiryId(request.enquiryId()).isPresent()) {
            throw new IllegalArgumentException("This enquiry already has a room preference — update it instead");
        }
        if (request.studentId() != null && roomPreferenceRepository.findByStudentId(request.studentId()).isPresent()) {
            throw new IllegalArgumentException("This student already has a room preference — update it instead");
        }

        RoomPreference preference = new RoomPreference();
        applyRequest(preference, request);
        if (request.status() != null) {
            preference.setStatus(request.status());
        }
        return toResponse(roomPreferenceRepository.save(preference));
    }

    public RoomPreferenceResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public RoomPreferenceResponse findByEnquiryId(Long enquiryId) {
        return roomPreferenceRepository.findByEnquiryId(enquiryId).map(this::toResponse).orElse(null);
    }

    public RoomPreferenceResponse findByStudentId(Long studentId) {
        return roomPreferenceRepository.findByStudentId(studentId).map(this::toResponse).orElse(null);
    }

    public Page<RoomPreferenceResponse> findPage(String search, RoomPreferenceStatus status, Pageable pageable) {
        Specification<RoomPreference> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("enquiry").get("name")), pattern),
                cb.like(cb.lower(root.get("student").get("firstName")), pattern),
                cb.like(cb.lower(root.get("student").get("lastName")), pattern)
            ));
        }
        if (status != null) {
            Specification<RoomPreference> statusSpec = (root, query, cb) -> cb.equal(root.get("status"), status);
            spec = spec.and(statusSpec);
        }
        return roomPreferenceRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public RoomPreferenceResponse update(Long id, RoomPreferenceRequest request) {
        RoomPreference preference = findOrThrow(id);
        applyRequest(preference, request);
        if (request.status() != null) {
            preference.setStatus(request.status());
        }
        return toResponse(roomPreferenceRepository.save(preference));
    }

    @Transactional
    public void delete(Long id) {
        if (!roomPreferenceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room preference not found with id: " + id);
        }
        roomPreferenceRepository.deleteById(id);
    }

    private void applyRequest(RoomPreference preference, RoomPreferenceRequest request) {
        if (request.enquiryId() != null) {
            Enquiry enquiry = enquiryRepository.findById(request.enquiryId())
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + request.enquiryId()));
            preference.setEnquiry(enquiry);
        }
        if (request.studentId() != null) {
            Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));
            preference.setStudent(student);
        }
        HostelRoomType roomType = hostelRoomTypeRepository.findById(request.preferredRoomTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Hostel room type not found with id: " + request.preferredRoomTypeId()));
        preference.setPreferredRoomType(roomType);

        if (request.preferredZoneId() != null) {
            Zone zone = zoneRepository.findById(request.preferredZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id: " + request.preferredZoneId()));
            preference.setPreferredZone(zone);
        } else {
            preference.setPreferredZone(null);
        }
        preference.setRemarks(trim(request.remarks()));
    }

    private RoomPreference findOrThrow(Long id) {
        return roomPreferenceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room preference not found with id: " + id));
    }

    private RoomPreferenceResponse toResponse(RoomPreference p) {
        Enquiry enquiry = p.getEnquiry();
        Student student = p.getStudent();
        Zone zone = p.getPreferredZone();
        return new RoomPreferenceResponse(
            p.getId(),
            enquiry != null ? enquiry.getId() : null,
            enquiry != null ? enquiry.getName() : null,
            student != null ? student.getId() : null,
            student != null ? (student.getFirstName() + " " + student.getLastName()).trim() : null,
            p.getPreferredRoomType().getId(),
            p.getPreferredRoomType().getName(),
            zone != null ? zone.getId() : null,
            zone != null ? zone.getName() : null,
            p.getStatus(),
            p.getRemarks(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
