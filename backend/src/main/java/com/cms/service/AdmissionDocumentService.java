package com.cms.service;

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
import com.cms.model.AdmissionDocument;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.repository.AdmissionDocumentRepository;
import com.cms.repository.AdmissionRepository;

@Service
@Transactional(readOnly = true)
public class AdmissionDocumentService {

    private final AdmissionDocumentRepository admissionDocumentRepository;
    private final AdmissionRepository admissionRepository;

    public AdmissionDocumentService(AdmissionDocumentRepository admissionDocumentRepository,
                                    AdmissionRepository admissionRepository) {
        this.admissionDocumentRepository = admissionDocumentRepository;
        this.admissionRepository = admissionRepository;
    }

    public List<AdmissionDocumentResponse> findByAdmissionId(Long admissionId) {
        return admissionDocumentRepository.findByAdmissionId(admissionId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public AdmissionDocumentResponse updateVerification(Long docId, DocumentVerificationStatus status, String verifiedBy) {
        AdmissionDocument document = admissionDocumentRepository.findById(docId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission document not found with id: " + docId));
        document.setVerificationStatus(status);
        document.setVerifiedBy(verifiedBy);
        document.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        AdmissionDocument updated = admissionDocumentRepository.save(document);
        return toResponse(updated);
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
        List<AdmissionDocument> documents = admissionDocumentRepository.findByAdmissionId(admissionId);
        for (AdmissionDocument doc : documents) {
            if (applicable.contains(doc.getDocumentType())) {
                checklist.put(doc.getDocumentType(), doc.getVerificationStatus());
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

    private AdmissionDocumentResponse toResponse(AdmissionDocument document) {
        return new AdmissionDocumentResponse(
            document.getId(),
            document.getAdmission().getId(),
            document.getDocumentType(),
            document.getFileName(),
            document.getStorageKey(),
            document.getUploadedAt(),
            document.getOriginalSubmitted(),
            document.getVerifiedBy(),
            document.getVerifiedAt(),
            document.getVerificationStatus(),
            document.getCreatedAt(),
            document.getUpdatedAt()
        );
    }
}
