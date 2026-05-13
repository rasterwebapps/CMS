package com.cms.dto;

import java.util.List;

public record ProgramTransferAnalysis(
    Long studentId,
    String studentName,
    Long oldProgramId,
    String oldProgramName,
    Long newProgramId,
    String newProgramName,
    List<ProgramTransferDocumentInfo> retainedDocuments,
    List<ProgramTransferDocumentInfo> irrelevantDocuments,
    List<ProgramTransferDocumentInfo> missingDocuments
) {}
