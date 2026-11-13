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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "credit_checks")
@Getter
@Setter
public class CreditCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "loan_application_id", nullable = false)
    private UUID loanApplicationId;

    @Column(name = "bureau_reference", nullable = false)
    private String bureauReference;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckResult result;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", nullable = false)
    private Map<String, Object> rawResponse = new LinkedHashMap<>();

    /** Null when the automated worker ran the check. */
    @Column(name = "checked_by")
    private UUID checkedBy;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;
}
