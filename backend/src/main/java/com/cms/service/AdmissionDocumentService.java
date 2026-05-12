package com.cms.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AdmissionDocumentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.EnquiryDocument;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryDocumentRepository;

@Service
@Transactional(readOnly = true)
public class AdmissionDocumentService {

    private final EnquiryDocumentRepository documentRepository;
    private final AdmissionRepository admissionRepository;

    public AdmissionDocumentService(EnquiryDocumentRepository documentRepository,
                                    AdmissionRepository admissionRepository) {
        this.documentRepository = documentRepository;
        this.admissionRepository = admissionRepository;
    }

    public List<AdmissionDocumentResponse> findByAdmissionId(Long admissionId) {
        return documentRepository.findByAdmission_Id(admissionId).stream()
            .map(document -> toResponse(document, admissionId))
            .toList();
    }

    @Transactional
    public AdmissionDocumentResponse updateVerification(Long docId, DocumentVerificationStatus status, String verifiedBy) {
        EnquiryDocument document = documentRepository.findById(docId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + docId));
        document.setStatus(status);
        document.setVerifiedBy(verifiedBy);
        document.setVerifiedAt(Instant.now());
        EnquiryDocument updated = documentRepository.save(document);
        return toResponse(updated, null);
    }

    /**
     * Returns the document checklist for the admission. Only document types
     * required by the student's program are included; if the program does not
     * have an explicit mapping configured, the full {@link DocumentType}
     * catalogue is returned (legacy fallback).
     */
    public Map<DocumentType, DocumentVerificationStatus> getChecklist(Long admissionId) {
        Admission admission = admissionRepository.findById(admissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + admissionId));

        Set<DocumentType> applicable = resolveApplicableTypes(admission);

        Map<DocumentType, DocumentVerificationStatus> checklist = new EnumMap<>(DocumentType.class);
        for (DocumentType type : applicable) {
            checklist.put(type, DocumentVerificationStatus.NOT_UPLOADED);
        }
        List<EnquiryDocument> documents = documentRepository.findByAdmission_Id(admissionId);
        for (EnquiryDocument doc : documents) {
            if (applicable.contains(doc.getDocumentType())) {
                checklist.put(doc.getDocumentType(), doc.getStatus());
            }
        }
        return checklist;
    }

    private Set<DocumentType> resolveApplicableTypes(Admission admission) {
        Student student = admission.getStudent();
        if (student != null) {
            Program program = student.getProgram();
            if (program != null) {
                Set<DocumentType> configured = program.getRequiredDocumentTypes();
                if (configured != null && !configured.isEmpty()) {
                    return new HashSet<>(configured);
                }
            }
        }
        return new HashSet<>(Set.of(DocumentType.values()));
    }

    private AdmissionDocumentResponse toResponse(EnquiryDocument document, Long admissionId) {
        return new AdmissionDocumentResponse(
            document.getId(),
            admissionId,
            document.getDocumentType(),
            document.getFileName(),
            null,
            toUtcLocalDateTime(document.getUploadedAt()),
            null,
            document.getVerifiedBy(),
            toUtcLocalDateTime(document.getVerifiedAt()),
            document.getStatus(),
            document.getCreatedAt(),
            document.getUpdatedAt()
        );
    }

    private LocalDateTime toUtcLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
