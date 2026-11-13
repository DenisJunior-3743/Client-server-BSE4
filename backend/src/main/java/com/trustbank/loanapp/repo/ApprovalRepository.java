package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.Approval;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    Optional<Approval> findFirstByLoanApplicationIdOrderByDecidedAtDesc(UUID loanApplicationId);
}
