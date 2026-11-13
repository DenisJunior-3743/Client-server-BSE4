package com.trustbank.loanapp.service;
import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.config.AppProperties;
import com.trustbank.loanapp.domain.*;
import com.trustbank.loanapp.repo.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DeadlineTest {
    private final Instant now=Instant.parse("2026-11-13T09:00:00Z");
    private final Clock clock=Clock.fixed(now,ZoneOffset.UTC);
    @Test void otpIsRejectedAtExactExpiry() {
        var codes=mock(VerificationCodeRepository.class); var users=mock(UserRepository.class);
        UUID id=UUID.randomUUID(); var row=new VerificationCode(); row.setExpiresAt(now);
        when(codes.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(id,VerificationCode.PHONE_VERIFICATION)).thenReturn(Optional.of(row));
        var props=new AppProperties(null,null,null,new AppProperties.Otp(Duration.ofMinutes(5),5,false),null,null,null,null,false);
        assertEquals(OtpService.Outcome.EXPIRED,new OtpService(codes,users,props,clock).verify(id,"123456"));
        assertNull(row.getConsumedAt()); verifyNoInteractions(users);
    }
    @Test void offerIsRejectedAtExactExpiryWithoutChangingState() {
        var applications=mock(LoanApplicationRepository.class); var machine=mock(LoanStateMachine.class);
        UUID owner=UUID.randomUUID(),id=UUID.randomUUID(); var loan=new LoanApplication();
        loan.setApplicantId(owner); loan.setStatus(LoanStatus.APPROVED); loan.setOfferExpiresAt(now);
        when(applications.findById(id)).thenReturn(Optional.of(loan));
        var service=new LoanApplicationService(applications,null,null,null,null,null,null,null,null,null,machine,clock);
        var error=assertThrows(ApiException.class,()->service.acceptOffer(owner,id));
        assertEquals(410,error.getStatus().value()); assertEquals("DEADLINE_EXPIRED",error.getCode());
        assertNull(loan.getOfferAcceptedAt()); verifyNoInteractions(machine);
    }
}
