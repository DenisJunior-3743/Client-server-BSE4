package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.LoanStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanStatusHistoryRepository extends JpaRepository<LoanStatusHistory, UUID> {

    List<LoanStatusHistory> findByLoanApplicationIdOrderByChangedAtAsc(UUID loanApplicationId);
}
