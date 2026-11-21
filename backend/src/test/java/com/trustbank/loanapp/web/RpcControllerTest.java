package com.trustbank.loanapp.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustbank.loanapp.domain.InstallmentStatus;
import com.trustbank.loanapp.domain.LoanApplication;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.repo.LoanApplicationRepository;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.service.LoanApplicationService;
import com.trustbank.loanapp.web.dto.LoanDtos.ApplicationDetailDto;
import com.trustbank.loanapp.web.dto.LoanDtos.ScheduleItemDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RpcControllerTest {
    private final AuthUser caller = new AuthUser(UUID.randomUUID(), "borrower@example.test", Role.BORROWER);
    private final UUID applicationId = UUID.randomUUID();
    private final LoanApplicationService loans = mock(LoanApplicationService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caller, null, List.of()));
        mvc = mvc(loans);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void callsExistingLoanServiceWithAuthenticatedPrincipalAndEchoesRequestId() throws Exception {
        when(loans.detail(caller, applicationId)).thenReturn(
                new ApplicationDetailDto(null, List.of(), null, null, null, null, List.of()));

        send(call("loan.getApplication", "{\"applicationId\": \"" + applicationId + "\"}", "\"detail-1\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("detail-1"))
                .andExpect(jsonPath("$.result.documents").isArray())
                .andExpect(jsonPath("$.error").doesNotExist());
        verify(loans).detail(caller, applicationId);
    }

    @Test
    void borrowerCannotReadAnotherBorrowersApplicationThroughEitherProcedure() throws Exception {
        LoanApplication application = new LoanApplication();
        application.setApplicantId(UUID.randomUUID());
        LoanApplicationRepository applications = mock(LoanApplicationRepository.class);
        when(applications.findById(applicationId)).thenReturn(Optional.of(application));
        // The authorization guard runs before any of the secondary repositories are accessed.
        mvc = mvc(new LoanApplicationService(applications, null, null, null, null, null, null, null,
                null, null, null, Clock.systemUTC()));

        for (String method : List.of("loan.getApplication", "loan.getRepaymentSchedule")) {
            send(call(method, "[\"" + applicationId + "\"]", "17"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(17))
                    .andExpect(jsonPath("$.error.code").value(-32004))
                    .andExpect(jsonPath("$.error.data.status").value(404))
                    .andExpect(jsonPath("$.result").doesNotExist());
        }
    }

    @Test
    void malformedMessagesAndUnsupportedMethodsNeverReachLoanService() throws Exception {
        send("{").andExpect(jsonPath("$.error.code").value(-32700));
        send("{} {}").andExpect(jsonPath("$.error.code").value(-32700));
        send("[]").andExpect(jsonPath("$.error.code").value(-32600));
        send(call("deleteAll", "[]", "\"unsupported\""))
                .andExpect(jsonPath("$.id").value("unsupported"))
                .andExpect(jsonPath("$.error.code").value(-32601));
        send(call("loan.getApplication", "{\"loanId\":\"123\"}", "6"))
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.error.code").value(-32602));
        verifyNoInteractions(loans);
    }

    @Test
    void batchUsesIdsToMatchResultsAndOmitsRepliesForNotifications() throws Exception {
        when(loans.schedule(caller, applicationId)).thenReturn(List.of(
                new ScheduleItemDto(1, LocalDate.of(2027, 1, 5), BigDecimal.TEN, BigDecimal.ZERO,
                        BigDecimal.TEN, BigDecimal.ZERO, InstallmentStatus.PENDING)));
        String params = "[\"" + applicationId + "\"]";
        String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"loan.getRepaymentSchedule\",\"params\":" + params + "}";

        send("[" + notification + "," + call("loan.getRepaymentSchedule", params, "7") + ",1]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].result[0].installmentNo").value(1))
                .andExpect(jsonPath("$[1].error.code").value(-32600))
                .andExpect(jsonPath("$[1].id").value(org.hamcrest.Matchers.nullValue()));
        send(notification).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void oversizedBatchDoesNoWork() throws Exception {
        String member = call("loan.getRepaymentSchedule", "[\"" + applicationId + "\"]", "1");
        send("[" + String.join(",", Collections.nCopies(11, member)) + "]")
                .andExpect(jsonPath("$.error.code").value(-32010));
        verifyNoInteractions(loans);
    }

    private static String call(String method, String params, String id) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\",\"params\":" + params + ",\"id\":" + id + "}";
    }

    private ResultActions send(String body) throws Exception {
        return mvc.perform(post("/api/v1/rpc").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static MockMvc mvc(LoanApplicationService service) {
        return MockMvcBuilders.standaloneSetup(new RpcController(service, new ObjectMapper().findAndRegisterModules()))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }
}
