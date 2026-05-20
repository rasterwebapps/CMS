package com.cms.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.ProfileIdentity;
import com.cms.dto.DocumentFileDownload;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.DocumentType;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.repository.FacultyDocumentRepository;

/**
 * Self-service document operations for the currently authenticated user.
 *
 * Faculty and students may upload, replace, delete, and download their own
 * documents via /profile/me/documents/* without requiring admin permissions.
 * Mutation is blocked once a document reaches VERIFIED status (BR-30).
 */
@Service
@Transactional(readOnly = true)
public class ProfileDocumentService {

    private final ProfileService profileService;
    private final FacultyDocumentService facultyDocumentService;
    private final AdmissionDocumentService admissionDocumentService;
    private final FacultyDocumentRepository facultyDocumentRepository;
    private final EnquiryDocumentRepository enquiryDocumentRepository;

    public ProfileDocumentService(ProfileService profileService,
                                   FacultyDocumentService facultyDocumentService,
                                   AdmissionDocumentService admissionDocumentService,
                                   FacultyDocumentRepository facultyDocumentRepository,
                                   EnquiryDocumentRepository enquiryDocumentRepository) {
        this.profileService               = profileService;
        this.facultyDocumentService       = facultyDocumentService;
        this.admissionDocumentService     = admissionDocumentService;
        this.facultyDocumentRepository    = facultyDocumentRepository;
        this.enquiryDocumentRepository    = enquiryDocumentRepository;
    }

    // ── Upload / replace ───────────────────────────────────────────────────────

    @Transactional
    public Object uploadMyDocument(DocumentType documentType, MultipartFile file) {
        ProfileIdentity me = profileService.resolveCurrentUser();
        if ("FACULTY".equals(me.entityType())) {
            if (me.entityId() == null) throw unsupported();
            return facultyDocumentService.uploadFile(me.entityId(), documentType, null, file);
        }
        if ("STUDENT".equals(me.entityType())) {
            if (me.admissionId() == null) throw unsupported();
            return admissionDocumentService.uploadFile(me.admissionId(), documentType, null, file);
        }
        throw unsupported();
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteMyDocument(Long documentId) {
        ProfileIdentity me = profileService.resolveCurrentUser();
        if ("FACULTY".equals(me.entityType())) {
            assertFacultyOwns(me.entityId(), documentId);
            facultyDocumentService.deleteDocument(documentId); // VERIFIED guard is inside the service
            return;
        }
        if ("STUDENT".equals(me.entityType())) {
            assertAdmissionOwns(me.admissionId(), documentId);
            admissionDocumentService.deleteDocument(documentId); // VERIFIED guard is inside the service
            return;
        }
        throw unsupported();
    }

    // ── Download ───────────────────────────────────────────────────────────────

    public ResponseEntity<Resource> downloadMyDocument(Long documentId) {
        ProfileIdentity me = profileService.resolveCurrentUser();
        DocumentFileDownload dl;
        if ("FACULTY".equals(me.entityType())) {
            assertFacultyOwns(me.entityId(), documentId);
            dl = facultyDocumentService.getFileForDownload(documentId);
        } else if ("STUDENT".equals(me.entityType())) {
            assertAdmissionOwns(me.admissionId(), documentId);
            dl = admissionDocumentService.getFileForDownload(documentId);
        } else {
            throw unsupported();
        }

        ByteArrayResource resource = new ByteArrayResource(dl.data());
        String encoded = URLEncoder.encode(dl.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "inline; filename=\"" + sanitize(dl.fileName())
            + "\"; filename*=UTF-8''" + encoded;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .contentType(MediaType.parseMediaType(dl.contentType()))
            .contentLength(dl.data().length)
            .body(resource);
    }

    // ── Ownership guards ───────────────────────────────────────────────────────

    private void assertFacultyOwns(Long facultyId, Long documentId) {
        if (facultyId == null) throw unsupported();
        boolean owns = facultyDocumentRepository.findByFacultyId(facultyId)
            .stream().anyMatch(d -> d.getId().equals(documentId));
        if (!owns) throw new ResourceNotFoundException("Document not found");
    }

    private void assertAdmissionOwns(Long admissionId, Long documentId) {
        if (admissionId == null) throw unsupported();
        boolean owns = enquiryDocumentRepository.findByAdmission_Id(admissionId)
            .stream().anyMatch(d -> d.getId().equals(documentId));
        if (!owns) throw new ResourceNotFoundException("Document not found");
    }

    private static String sanitize(String name) {
        return name != null ? name.replaceAll("[\\\\\"\\r\\n]", "_") : "document";
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Self-service documents are not available for this account type");
    }
}
