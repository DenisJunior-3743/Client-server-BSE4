package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.LoanProduct;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanProductRepository extends JpaRepository<LoanProduct, UUID> {

    List<LoanProduct> findAllByOrderByNameAsc();

    List<LoanProduct> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
