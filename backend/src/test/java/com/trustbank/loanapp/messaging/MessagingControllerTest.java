package com.trustbank.loanapp.messaging;

import com.trustbank.loanapp.common.GlobalExceptionHandler;
import com.trustbank.loanapp.domain.Role;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MessagingControllerTest {
    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity(proxyTargetClass = true)
    static class Config {
        @Bean LoanMessageStore store() { return mock(LoanMessageStore.class); }
        @Bean MessagingController controller(LoanMessageStore store, ObjectProvider<LoanOutboxRelay> relay) {
            return new MessagingController(store, relay, false, false);
        }
    }
    AnnotationConfigApplicationContext context;
    MockMvc mvc;
    LoanMessageStore store;
    @BeforeEach void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        store = context.getBean(LoanMessageStore.class);
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(MessagingController.class))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @AfterEach void close() { SecurityContextHolder.clearContext(); context.close(); }
    void authenticate(Role role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-user", null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
    @Test void nonAdministratorsCannotInspectPublishOrRetry() throws Exception {
        for (Role role : Role.values()) {
            if (role == Role.ADMIN) continue;
            authenticate(role);
            mvc.perform(get("/api/v1/admin/messaging")).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/messaging/publish")).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/messaging/" + UUID.randomUUID() + "/retry"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(store);
    }
    @Test void disabledBrokerCannotBePublishedButUnconsumedEventCanBeQueuedForLater() throws Exception {
        authenticate(Role.ADMIN);
        mvc.perform(post("/api/v1/admin/messaging/publish")).andExpect(status().isUnprocessableEntity());
        UUID id = UUID.randomUUID();
        when(store.requestRetry(id)).thenReturn(true);
        mvc.perform(post("/api/v1/admin/messaging/" + id + "/retry"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.queued").value(true));
        mvc.perform(post("/api/v1/admin/messaging/" + UUID.randomUUID() + "/retry"))
                .andExpect(status().isConflict());
    }
}
