package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.LoanApplication;
import com.trustbank.loanapp.domain.LoanStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    // ix_loan_applications_applicant
    List<LoanApplication> findByApplicantIdOrderBySubmittedAtDesc(UUID applicantId, Pageable page);

    // ix_loan_applications_status
    List<LoanApplication> findByStatusInOrderBySubmittedAtDesc(Collection<LoanStatus> statuses, Pageable page);

    List<LoanApplication> findAllByOrderBySubmittedAtDesc(Pageable page);

    @Query(value = "SELECT nextval('loan_reference_seq')", nativeQuery = true)
    long nextReferenceNumber();

    // ix_loan_applications_open_offers (partial index on status = 'APPROVED')
    @Query("select a.id from LoanApplication a where a.status = com.trustbank.loanapp.domain.LoanStatus.APPROVED "
            + "and a.offerExpiresAt < :now")
    List<UUID> findIdsOfExpiredOffers(@Param("now") Instant now, Pageable page);

    // ix_repayment_schedules_due (partial index on status <> 'PAID')
    @Query("select distinct s.loanApplicationId from RepaymentSchedule s, LoanApplication a "
            + "where a.id = s.loanApplicationId and a.status = com.trustbank.loanapp.domain.LoanStatus.ACTIVE "
            + "and s.status <> com.trustbank.loanapp.domain.InstallmentStatus.PAID and s.dueDate < :today")
    List<UUID> findIdsOfActiveLoansWithOverdueInstallments(@Param("today") LocalDate today, Pageable page);
}
