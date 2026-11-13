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
@Table(name = "approvals")
@Getter
@Setter
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "loan_application_id", nullable = false)
    private UUID loanApplicationId;

    @Column(name = "decided_by", nullable = false)
    private UUID decidedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decision decision;

    private String comments;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;
}
