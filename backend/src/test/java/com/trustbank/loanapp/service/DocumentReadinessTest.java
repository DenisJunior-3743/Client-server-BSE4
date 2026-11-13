package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocumentReadinessTest {
    private LoanDocument doc(DocumentType type, VerificationStatus status, String fileId) {
        var d = new LoanDocument(); d.setDocType(type); d.setVerificationStatus(status); d.setGridfsFileId(fileId); return d;
    }
    private List<LoanDocument> verified() {
        return new ArrayList<>(List.of(doc(DocumentType.NATIONAL_ID, VerificationStatus.VERIFIED, "id"),
                doc(DocumentType.PAYSLIP, VerificationStatus.VERIFIED, "pay"),
                doc(DocumentType.BANK_STATEMENT, VerificationStatus.VERIFIED, "bank")));
    }
    @Test void missingDocumentsCannotAdvance() {
        assertEquals("DOCUMENTS_MISSING", assertThrows(ApiException.class,
                () -> DocumentReadiness.requireVerified(List.of())).getCode());
    }
    @Test void metadataWithoutFileCannotAdvance() {
        var docs = verified(); docs.getFirst().setGridfsFileId(null);
        assertThrows(ApiException.class, () -> DocumentReadiness.requireVerified(docs));
    }
    @Test void replacementRequiresVerificationAndSupersedesRejectedOldFile() {
        var docs = verified(); docs.addFirst(doc(DocumentType.NATIONAL_ID, VerificationStatus.REJECTED, "old"));
        assertDoesNotThrow(() -> DocumentReadiness.requireVerified(docs));
        docs.add(doc(DocumentType.NATIONAL_ID, VerificationStatus.PENDING, "replacement"));
        assertThrows(ApiException.class, () -> DocumentReadiness.requireVerified(docs));
    }
}
