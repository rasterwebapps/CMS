package com.cms.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LibraryFineDetailResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Faculty;
import com.cms.model.LibraryFine;
import com.cms.model.LibraryIssue;
import com.cms.model.Student;
import com.cms.model.enums.FineStatus;
import com.cms.model.enums.LibraryMemberType;
import com.cms.repository.LibraryFineRepository;

@Service
public class LibraryFineService {

    private final LibraryFineRepository fineRepository;

    public LibraryFineService(LibraryFineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    public List<LibraryFineDetailResponse> findAll(FineStatus status) {
        List<LibraryFine> fines = status != null
            ? fineRepository.findByStatus(status)
            : fineRepository.findAll();
        return fines.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(this::toDetail)
            .toList();
    }

    @Transactional
    public LibraryFineDetailResponse waive(Long id, String remarks, String waivedBy) {
        LibraryFine fine = requireFine(id);
        if (fine.getStatus() != FineStatus.PENDING) {
            throw new IllegalStateException("Only PENDING fines can be waived");
        }
        fine.setStatus(FineStatus.WAIVED);
        fine.setWaivedBy(waivedBy);
        if (remarks != null && !remarks.isBlank()) fine.setRemarks(remarks);
        return toDetail(fineRepository.save(fine));
    }

    @Transactional
    public LibraryFineDetailResponse collect(Long id, String remarks, String collectedBy) {
        LibraryFine fine = requireFine(id);
        if (fine.getStatus() != FineStatus.PENDING) {
            throw new IllegalStateException("Only PENDING fines can be collected");
        }
        fine.setStatus(FineStatus.COLLECTED);
        fine.setCollectedAt(Instant.now());
        if (remarks != null && !remarks.isBlank()) fine.setRemarks(remarks);
        return toDetail(fineRepository.save(fine));
    }

    private LibraryFine requireFine(Long id) {
        return fineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fine not found with id: " + id));
    }

    private LibraryFineDetailResponse toDetail(LibraryFine fine) {
        LibraryIssue issue  = fine.getIssue();
        Student      student = issue.getStudent();
        Faculty      faculty = issue.getFaculty();

        String memberName = issue.getMemberType() == LibraryMemberType.STUDENT
            ? (student != null ? student.getFirstName() + " " + student.getLastName() : "Unknown")
            : (faculty != null ? faculty.getFirstName() + " " + faculty.getLastName() : "Unknown");

        String memberCode = issue.getMemberType() == LibraryMemberType.STUDENT
            ? (student != null ? student.getRollNumber() : null)
            : (faculty != null ? faculty.getEmployeeCode() : null);

        return new LibraryFineDetailResponse(
            fine.getId(),
            issue.getId(),
            issue.getBook().getAccessionNumber(),
            issue.getBook().getTitle(),
            issue.getMemberType(),
            memberName,
            memberCode,
            issue.getIssuedDate(),
            issue.getDueDate(),
            issue.getReturnedDate(),
            fine.getOverdueDays(),
            fine.getFinePerDay(),
            fine.getTotalFine(),
            fine.getStatus(),
            fine.getWaivedBy(),
            fine.getCollectedAt(),
            fine.getRemarks(),
            fine.getCreatedAt()
        );
    }
}
