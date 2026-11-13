package com.trustbank.loanapp.web.dto;

import com.trustbank.loanapp.domain.CheckResult;
import com.trustbank.loanapp.domain.Decision;
import com.trustbank.loanapp.domain.DocumentType;
import com.trustbank.loanapp.domain.InstallmentStatus;
import com.trustbank.loanapp.domain.LoanStatus;
import com.trustbank.loanapp.domain.PaymentMethod;
import com.trustbank.loanapp.domain.ProductType;
import com.trustbank.loanapp.domain.VerificationStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request/reply messages for products, applications and the staff workflow.
 *
 * <p>Every staff command carries the {@code version} the officer was looking at. That is the whole
 * client side of optimistic concurrency control: "apply this only if nobody changed it since".
 */
public final class LoanDtos {

    private LoanDtos() {}

    // ---- products -----------------------------------------------------------------------------

    public record ProductDto(UUID id, String name, ProductType type, BigDecimal interestRateAnnual,
            BigDecimal minAmount, BigDecimal maxAmount, int minTermMonths, int maxTermMonths, boolean active) {}

    public record ProductRequest(
            @NotBlank @Size(min = 3, max = 80) String name,
            @NotNull ProductType type,
            @NotNull @Positive @DecimalMax("100") BigDecimal interestRateAnnual,
            @NotNull @Positive BigDecimal minAmount,
            @NotNull @Positive BigDecimal maxAmount,
            @Min(1) @Max(360) int minTermMonths,
            @Min(1) @Max(360) int maxTermMonths) {}

    // ---- applications -------------------------------------------------------------------------

    public record SubmitApplicationRequest(
            @NotNull UUID productId,
            @NotNull @Positive BigDecimal amountRequested,
            @Min(1) @Max(360) int termMonths,
            @NotBlank @Size(min = 10, max = 200, message = "Describe the purpose (10-200 characters)") String purpose,
            /* Extra form sections from the mobile app (business, collateral, witnesses...). Stored as JSONB. */
            @Size(max = 20) Map<String, Object> details) {}

    /**
     * One shape for both audiences. Fields that are internal to the bank ({@code assignedOfficerId},
     * {@code pendingDocuments}) are left null for borrowers and dropped from the JSON.
     */
    public record ApplicationDto(UUID id, String reference, UUID applicantId, String applicantName,
            String applicantPhone, UUID productId, ProductType productType, BigDecimal amountRequested,
            BigDecimal amountApproved, BigDecimal interestRateAnnual, int termMonths, String purpose,
            LoanStatus status, UUID assignedOfficerId, String decisionRemarks, String infoRequestMessage,
            Instant offerExpiresAt, Instant submittedAt, Instant updatedAt, long version, Long pendingDocuments) {}

    public record DocumentRequest(
            @NotNull DocumentType docType,
            @NotBlank @Size(max = 200) String fileName,
            @NotBlank @Pattern(regexp = "application/pdf|image/jpeg|image/png",
                    message = "File must be PDF/JPG/PNG under 5MB") String contentType,
            @Positive @Max(value = 5_242_880, message = "File must be PDF/JPG/PNG under 5MB") long sizeBytes) {}

    public record DocumentDto(UUID id, UUID loanApplicationId, DocumentType docType, String fileName,
            VerificationStatus verificationStatus, Instant uploadedAt, long version) {}

    public record CreditCheckDto(UUID id, UUID loanApplicationId, String bureauReference, int score,
            CheckResult result, Instant checkedAt) {}

    public record ApprovalDto(UUID id, UUID loanApplicationId, UUID decidedBy, Decision decision, String comments,
            Instant decidedAt) {}

    public record DisbursementDto(UUID id, UUID loanApplicationId, BigDecimal amount, PaymentMethod method,
            String status, Instant disbursedAt) {}

    public record HistoryDto(LoanStatus fromStatus, LoanStatus toStatus, String note, Instant changedAt) {}

    public record ApplicationDetailDto(ApplicationDto application, List<DocumentDto> documents,
            CreditCheckDto creditCheck, ApprovalDto approval, DisbursementDto disbursement,
            AuthDtos.ProfileDto applicantProfile, List<HistoryDto> history) {}

    public record InfoResponseRequest(@NotBlank @Size(min = 2, max = 500) String message) {}

    // ---- staff workflow commands (all optimistic) -----------------------------------------------

    public record AssignRequest(@NotNull Long version) {}

    public record StatusChangeRequest(@NotNull LoanStatus status, @NotNull Long version, @Size(max = 500) String note) {}

    public record CreditCheckRequest(@Min(300) @Max(850) int score, @NotNull CheckResult result,
            @NotNull Long version) {}

    public record DecisionRequest(@NotNull Decision decision, @Size(max = 500) String comments,
            @NotNull Long version, @Positive BigDecimal amountApproved) {}

    public record DisburseRequest(@NotNull PaymentMethod method, @NotNull Long version) {}

    public record DocumentVerificationRequest(@NotNull VerificationStatus status, @NotNull Long version) {}

    // ---- repayments ---------------------------------------------------------------------------

    public record RepaymentRequest(@NotNull @Positive BigDecimal amount, @NotNull PaymentMethod method) {}

    public record ScheduleItemDto(int installmentNo, LocalDate dueDate, BigDecimal principalDue,
            BigDecimal interestDue, BigDecimal amountDue, BigDecimal amountPaid, InstallmentStatus status) {}

    public record RepaymentReceiptDto(String paymentReference, BigDecimal amountPaid,
            BigDecimal outstandingBalance, LoanStatus loanStatus, Instant paidAt) {}

    // ---- notifications / audit ----------------------------------------------------------------

    public record NotificationDto(UUID id, UUID loanApplicationId, String message, Instant sentAt, Instant readAt) {}

    public record AuditLogDto(UUID id, String entityType, UUID entityId, String action, UUID performedBy,
            String performedByName, Instant performedAt, String details) {}
}
