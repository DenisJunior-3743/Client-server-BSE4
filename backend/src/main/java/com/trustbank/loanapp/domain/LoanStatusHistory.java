package com.trustbank.loanapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "loan_status_history")
@Getter
@Setter
public class LoanStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "loan_application_id", nullable = false)
    private UUID loanApplicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private LoanStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private LoanStatus toStatus;

    @Column(name = "changed_by")
    private UUID changedBy;

    private String note;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
