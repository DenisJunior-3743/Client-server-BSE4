package com.trustbank.loanapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "loan_applications")
@Getter
@Setter
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String reference;

    @Column(name = "applicant_id", nullable = false, updatable = false)
    private UUID applicantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Column(name = "amount_requested", nullable = false)
    private BigDecimal amountRequested;

    @Column(name = "amount_approved")
    private BigDecimal amountApproved;

    @Column(name = "interest_rate_annual", nullable = false)
    private BigDecimal interestRateAnnual;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(nullable = false)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(name = "assigned_officer_id")
    private UUID assignedOfficerId;

    @Column(name = "decision_remarks")
    private String decisionRemarks;

    @Column(name = "info_request_message")
    private String infoRequestMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> metadata = new LinkedHashMap<>();

    /** Firm deadline for the borrower to accept an approved offer. */
    @Column(name = "offer_expires_at")
    private Instant offerExpiresAt;

    @Column(name = "offer_accepted_at")
    private Instant offerAcceptedAt;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Optimistic concurrency control. Hibernate issues
     * {@code UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?}; if another
     * transaction changed the row first, zero rows match and the update fails fast. No row lock is
     * held while an officer reads the screen and thinks.
     */
    @Version
    private long version;
}
