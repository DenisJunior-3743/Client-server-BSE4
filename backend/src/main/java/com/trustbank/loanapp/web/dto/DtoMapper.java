package com.trustbank.loanapp.web.dto;

import com.trustbank.loanapp.domain.ApplicantProfile;
import com.trustbank.loanapp.domain.Approval;
import com.trustbank.loanapp.domain.CreditCheck;
import com.trustbank.loanapp.domain.Disbursement;
import com.trustbank.loanapp.domain.LoanApplication;
import com.trustbank.loanapp.domain.LoanDocument;
import com.trustbank.loanapp.domain.LoanProduct;
import com.trustbank.loanapp.domain.LoanStatusHistory;
import com.trustbank.loanapp.domain.Notification;
import com.trustbank.loanapp.domain.RepaymentSchedule;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.web.dto.AuthDtos.ProfileDto;
import com.trustbank.loanapp.web.dto.AuthDtos.UserDto;
import com.trustbank.loanapp.web.dto.LoanDtos.ApplicationDto;
import com.trustbank.loanapp.web.dto.LoanDtos.ApprovalDto;
import com.trustbank.loanapp.web.dto.LoanDtos.CreditCheckDto;
import com.trustbank.loanapp.web.dto.LoanDtos.DisbursementDto;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentDto;
import com.trustbank.loanapp.web.dto.LoanDtos.HistoryDto;
import com.trustbank.loanapp.web.dto.LoanDtos.NotificationDto;
import com.trustbank.loanapp.web.dto.LoanDtos.ProductDto;
import com.trustbank.loanapp.web.dto.LoanDtos.ScheduleItemDto;

/**
 * Entity -> DTO conversion. This is the privacy duty in code: an entity never crosses the network,
 * so a column added to a table tomorrow cannot leak by accident. Static and stateless.
 */
public final class DtoMapper {

    private DtoMapper() {}

    public static UserDto user(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(), u.getBranch(),
                u.getAvatarColor(), u.getCreatedAt(), u.isPhoneVerified());
    }

    public static ProfileDto profile(ApplicantProfile p, boolean maskNationalId) {
        String nin = maskNationalId ? maskNin(p.getNationalId()) : p.getNationalId();
        return new ProfileDto(p.getId(), p.getUserId(), nin, p.getDateOfBirth(), p.getEmploymentInfo(),
                p.getIncomeMonthly(), p.getAddress());
    }

    public static ProductDto product(LoanProduct p) {
        return new ProductDto(p.getId(), p.getName(), p.getType(), p.getInterestRateAnnual(), p.getMinAmount(),
                p.getMaxAmount(), p.getMinTermMonths(), p.getMaxTermMonths(), p.isActive());
    }

    /** Staff view: includes who the applicant is and which officer holds the file. */
    public static ApplicationDto applicationForStaff(LoanApplication a, User applicant, Long pendingDocuments) {
        return new ApplicationDto(a.getId(), a.getReference(), a.getApplicantId(),
                applicant == null ? null : applicant.getFullName(), applicant == null ? null : applicant.getPhone(),
                a.getProductId(), a.getProductType(), a.getAmountRequested(), a.getAmountApproved(),
                a.getInterestRateAnnual(), a.getTermMonths(), a.getPurpose(), a.getStatus(),
                a.getAssignedOfficerId(), a.getDecisionRemarks(), a.getInfoRequestMessage(), a.getOfferExpiresAt(),
                a.getSubmittedAt(), a.getUpdatedAt(), a.getVersion(), pendingDocuments);
    }

    /** Borrower view: the borrower's own data only, no internal assignment or queue information. */
    public static ApplicationDto applicationForBorrower(LoanApplication a) {
        return new ApplicationDto(a.getId(), a.getReference(), a.getApplicantId(), null, null, a.getProductId(),
                a.getProductType(), a.getAmountRequested(), a.getAmountApproved(), a.getInterestRateAnnual(),
                a.getTermMonths(), a.getPurpose(), a.getStatus(), null, a.getDecisionRemarks(),
                a.getInfoRequestMessage(), a.getOfferExpiresAt(), a.getSubmittedAt(), a.getUpdatedAt(),
                a.getVersion(), null);
    }

    public static DocumentDto document(LoanDocument d) {
        return new DocumentDto(d.getId(), d.getLoanApplicationId(), d.getDocType(), d.getFileName(),
                d.getVerificationStatus(), d.getUploadedAt(), d.getVersion());
    }

    public static CreditCheckDto creditCheck(CreditCheck c) {
        return new CreditCheckDto(c.getId(), c.getLoanApplicationId(), c.getBureauReference(), c.getScore(),
                c.getResult(), c.getCheckedAt());
    }

    public static ApprovalDto approval(Approval a) {
        return new ApprovalDto(a.getId(), a.getLoanApplicationId(), a.getDecidedBy(), a.getDecision(),
                a.getComments(), a.getDecidedAt());
    }

    public static DisbursementDto disbursement(Disbursement d) {
        return new DisbursementDto(d.getId(), d.getLoanApplicationId(), d.getAmount(), d.getMethod(),
                d.getStatus(), d.getDisbursedAt());
    }

    public static HistoryDto history(LoanStatusHistory h, boolean includeNote) {
        return new HistoryDto(h.getFromStatus(), h.getToStatus(), includeNote ? h.getNote() : null,
                h.getChangedAt());
    }

    public static ScheduleItemDto scheduleItem(RepaymentSchedule s) {
        return new ScheduleItemDto(s.getInstallmentNo(), s.getDueDate(), s.getPrincipalDue(), s.getInterestDue(),
                s.getAmountDue(), s.getAmountPaid(), s.getStatus());
    }

    public static NotificationDto notification(Notification n) {
        return new NotificationDto(n.getId(), n.getLoanApplicationId(), n.getMessage(), n.getSentAt(),
                n.getReadAt());
    }

    /** CM12345678A12X -> CM**********2X */
    static String maskNin(String nin) {
        if (nin == null || nin.length() < 6) {
            return "****";
        }
        return nin.substring(0, 2) + "*".repeat(nin.length() - 4) + nin.substring(nin.length() - 2);
    }
}
