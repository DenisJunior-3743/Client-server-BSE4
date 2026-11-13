package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

/** Replacement uploads supersede earlier files of the same type during document review. */
final class DocumentReadiness {
    private static final Set<DocumentType> REQUIRED = Set.of(DocumentType.NATIONAL_ID,
            DocumentType.PAYSLIP, DocumentType.BANK_STATEMENT);
    private DocumentReadiness() {}

    static void requireVerified(List<LoanDocument> uploadedInOrder) {
        var latest = new EnumMap<DocumentType, LoanDocument>(DocumentType.class);
        for (var document : uploadedInOrder) latest.put(document.getDocType(), document);
        if (!latest.keySet().containsAll(REQUIRED))
            throw ApiException.rule("DOCUMENTS_MISSING", "Upload National ID, payslip and bank statement before the credit check");
        if (latest.values().stream().anyMatch(d -> d.getGridfsFileId() == null
                || d.getVerificationStatus() != VerificationStatus.VERIFIED))
            throw ApiException.rule("DOCUMENTS_NOT_VERIFIED", "The latest file of every document type must be uploaded and verified before the credit check");
    }
}
