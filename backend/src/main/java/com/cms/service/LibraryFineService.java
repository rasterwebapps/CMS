package com.cms.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

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

    public Page<LibraryFineDetailResponse> findPage(String search, FineStatus status, LibraryMemberType memberType, Pageable pageable) {
        Specification<LibraryFine> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            boolean needsIssueJoin = (search != null && !search.isBlank()) || memberType != null;
            Join<Object, Object> issue = needsIssueJoin ? root.join("issue", JoinType.LEFT) : null;
            if (search != null && !search.isBlank()) {
                String p = "%" + search.trim().toLowerCase() + "%";
                Join<Object, Object> book = issue.join("book", JoinType.LEFT);
                Join<Object, Object> periodical = issue.join("periodical", JoinType.LEFT);
                predicates.add(cb.or(
                    cb.like(cb.lower(book.get("title")), p),
                    cb.like(cb.lower(book.get("accessionNumber")), p),
                    cb.like(cb.lower(periodical.get("journalName")), p),
                    cb.like(cb.lower(periodical.get("accessionNumber")), p)
                ));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (memberType != null) predicates.add(cb.equal(issue.get("memberType"), memberType));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return fineRepository.findAll(spec, pageable).map(this::toDetail);
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
            issue.getItemType(),
            issue.getItemAccessionNumber(),
            issue.getItemTitle(),
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
