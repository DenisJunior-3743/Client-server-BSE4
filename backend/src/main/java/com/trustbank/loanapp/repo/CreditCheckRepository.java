package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.CreditCheck;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCheckRepository extends JpaRepository<CreditCheck, UUID> {

    Optional<CreditCheck> findFirstByLoanApplicationIdOrderByCheckedAtDesc(UUID loanApplicationId);
}
