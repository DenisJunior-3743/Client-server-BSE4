package com.trustbank.loanapp.web;

import com.trustbank.loanapp.common.GlobalExceptionHandler;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.service.StaffService;
import com.trustbank.loanapp.web.dto.DtoMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminControllerTest {
    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity(proxyTargetClass = true)
    static class Config {
        @Bean StaffService staffService() { return mock(StaffService.class); }
        @Bean AdminController adminController(StaffService staff) { return new AdminController(staff); }
    }

    final UUID adminId = UUID.randomUUID(), staffId = UUID.randomUUID();
    AnnotationConfigApplicationContext context;
    StaffService staff;
    MockMvc mvc;
    final String payload = """
            {"fullName":"Updated Staff","email":"staff@bank.com","phone":"0771234567",
             "role":"CREDIT_ANALYST","branch":"Kampala"}
            """;

    @BeforeEach void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        staff = context.getBean(StaffService.class);
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(AdminController.class))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        authenticate(Role.ADMIN);
    }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    void authenticate(Role role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(adminId, "admin@bank.com", role), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    @Test void adminCanFetchAndUpdateStaff() throws Exception {
        User user = new User(); user.setId(staffId); user.setFullName("Updated Staff");
        user.setEmail("staff@bank.com"); user.setRole(Role.CREDIT_ANALYST);
        when(staff.getStaff(staffId)).thenReturn(DtoMapper.user(user));
        when(staff.updateStaff(eq(adminId), eq(staffId), any())).thenReturn(DtoMapper.user(user));
        mvc.perform(get("/api/v1/staff/" + staffId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(staffId.toString()));
        mvc.perform(put("/api/v1/staff/" + staffId).contentType("application/json").content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CREDIT_ANALYST"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(staff).updateStaff(eq(adminId), eq(staffId), argThat(request -> request.branch().equals("Kampala")));
    }

    @Test void deleteReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/v1/staff/" + staffId)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(staff).deleteStaff(adminId, staffId);
    }

    @Test void invalidDetailsAreRejectedBeforeMutation() throws Exception {
        mvc.perform(put("/api/v1/staff/" + staffId).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(staff);
    }

    @Test void adminCanSetPasswordWithoutReceivingOrEditingUsername() throws Exception {
        mvc.perform(put("/api/v1/staff/" + staffId + "/credentials").contentType("application/json")
                .content("{\"password\":\"NewPassword123\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(staff).setStaffCredentials(adminId, staffId, "NewPassword123");
    }

    @Test void weakMissingAndLongCredentialsAreRejected() throws Exception {
        for (String json : List.of("{}", "{\"password\":\"\"}", "{\"password\":\"weak\"}",
                "{\"password\":\"NoDigitsHere\"}", "{\"password\":\"" + "Aa1".repeat(25) + "\"}")) {
            mvc.perform(put("/api/v1/staff/" + staffId + "/credentials").contentType("application/json").content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.password").exists());
        }
        verifyNoInteractions(staff);
    }

    @Test void createSupportsOptionalInitialPassword() throws Exception {
        mvc.perform(post("/api/v1/staff").contentType("application/json").content(payload))
                .andExpect(status().isCreated());
        verify(staff).createStaff(eq(adminId), any(), isNull());
        String withPassword = payload.stripTrailing().replaceFirst("}\\s*$", ",\"password\":\"ChosenPassword123\"}");
        mvc.perform(post("/api/v1/staff").contentType("application/json").content(withPassword))
                .andExpect(status().isCreated());
        verify(staff).createStaff(eq(adminId), any(), eq("ChosenPassword123"));
    }

    @Test void nonAdminsCannotReadEditFormOrMutateStaff() throws Exception {
        for (Role role : List.of(Role.BORROWER, Role.LOAN_OFFICER, Role.CREDIT_ANALYST, Role.BRANCH_MANAGER)) {
            authenticate(role);
            mvc.perform(get("/api/v1/staff/" + staffId)).andExpect(status().isForbidden());
            mvc.perform(put("/api/v1/staff/" + staffId).contentType("application/json").content(payload))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/v1/staff/" + staffId)).andExpect(status().isForbidden());
            mvc.perform(put("/api/v1/staff/" + staffId + "/credentials").contentType("application/json")
                    .content("{\"password\":\"NewPassword123\"}")).andExpect(status().isForbidden());
        }
        verifyNoInteractions(staff);
    }
}
