package com.trustbank.loanapp.common;

import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    @RestController
    static class ContendedEndpoint {
        @PostMapping("/contended")
        void submit() {
            // Hibernate's PostgreSQL lock-timeout translation is broader than CannotAcquireLockException.
            throw new PessimisticLockingFailureException("duplicate key claim exceeded lock_timeout");
        }
        @PostMapping("/unavailable")
        void unavailable() {
            throw new DataAccessResourceFailureException("connection pool timeout");
        }
    }

    @Test
    void lockTimeoutReturnsRetryableServiceUnavailable() throws Exception {
        MockMvcBuilders.standaloneSetup(new ContendedEndpoint())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build()
                .perform(post("/contended"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "2"))
                .andExpect(jsonPath("$.code").value("SERVER_BUSY"));
    }
    @Test
    void unavailableConnectionReturnsRetryableServiceUnavailable() throws Exception {
        MockMvcBuilders.standaloneSetup(new ContendedEndpoint())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build()
                .perform(post("/unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "2"))
                .andExpect(jsonPath("$.code").value("SERVER_BUSY"));
    }
}
