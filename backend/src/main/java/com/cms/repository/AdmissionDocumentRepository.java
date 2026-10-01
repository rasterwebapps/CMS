package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.AdmissionDocument;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;

public interface AdmissionDocumentRepository extends JpaRepository<AdmissionDocument, Long> {

    List<AdmissionDocument> findByAdmissionId(Long admissionId);

    Optional<AdmissionDocument> findByAdmissionIdAndDocumentType(Long admissionId, DocumentType documentType);

    List<AdmissionDocument> findByAdmissionIdAndVerificationStatus(Long admissionId, DocumentVerificationStatus verificationStatus);

    boolean existsByAdmissionIdAndDocumentType(Long admissionId, DocumentType documentType);

    void deleteByAdmissionId(Long admissionId);
}
