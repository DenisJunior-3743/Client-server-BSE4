package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.Repayment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentRepository extends JpaRepository<Repayment, UUID> {
}
