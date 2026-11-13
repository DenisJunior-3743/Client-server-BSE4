package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.LoanDocument;
import com.trustbank.loanapp.domain.VerificationStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, UUID> {

    List<LoanDocument> findByLoanApplicationIdOrderByUploadedAtAsc(UUID loanApplicationId);

    long countByLoanApplicationIdAndVerificationStatus(UUID loanApplicationId, VerificationStatus status);

    /** One grouped query for a whole page of applications (no N+1). Rows are [applicationId, count]. */
    @Query("select d.loanApplicationId, count(d) from LoanDocument d "
            + "where d.loanApplicationId in :ids and d.verificationStatus = com.trustbank.loanapp.domain.VerificationStatus.PENDING "
            + "group by d.loanApplicationId")
    List<Object[]> countPendingByApplicationIds(@Param("ids") Collection<UUID> ids);
}
