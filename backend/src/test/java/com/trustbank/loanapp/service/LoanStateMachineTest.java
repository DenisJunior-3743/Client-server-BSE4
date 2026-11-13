package com.trustbank.loanapp.service;
import com.trustbank.loanapp.common.StaleVersionException;
import com.trustbank.loanapp.domain.LoanApplication;
import com.trustbank.loanapp.repo.LoanStatusHistoryRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LoanStateMachineTest {
    @Test void staleOfficerVersionIsRejected() {
        var loan=new LoanApplication(); loan.setVersion(4);
        var machine=new LoanStateMachine(mock(LoanStatusHistoryRepository.class),mock(ApplicationEventPublisher.class),Clock.systemUTC());
        assertDoesNotThrow(()->machine.requireVersion(loan,4L));
        assertThrows(StaleVersionException.class,()->machine.requireVersion(loan,3L));
        assertThrows(StaleVersionException.class,()->machine.requireVersion(loan,null));
    }
}
