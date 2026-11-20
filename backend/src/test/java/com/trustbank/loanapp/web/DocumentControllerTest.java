package com.trustbank.loanapp.web;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.common.GlobalExceptionHandler;
import com.trustbank.loanapp.domain.DocumentType;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.VerificationStatus;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.service.DocumentService;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentControllerTest {
    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity(proxyTargetClass = true)
    static class Config {
        @Bean DocumentService documents() { return mock(DocumentService.class); }
        @Bean DocumentController controller(DocumentService documents) { return new DocumentController(documents); }
    }
    final UUID borrowerId = UUID.randomUUID(), applicationId = UUID.randomUUID(), documentId = UUID.randomUUID();
    final byte[] bytes = {(byte)137,80,78,71,13,10,26,10};
    final MockMultipartFile image = new MockMultipartFile("file", "collateral.png", "image/png", bytes);
    AnnotationConfigApplicationContext context;
    DocumentService documents;
    MockMvc mvc;

    @BeforeEach void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        documents = context.getBean(DocumentService.class);
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(DocumentController.class))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        authenticate(Role.BORROWER);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); context.close(); }
    void authenticate(Role role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(borrowerId, "upload-test@example.com", role), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
    String path() { return "/api/v1/applications/" + applicationId + "/documents"; }

    @Test void multipartFileAndDocTypeReachTheServiceWithOriginalBytes() throws Exception {
        when(documents.upload(eq(borrowerId), eq(applicationId), eq(DocumentType.COLLATERAL_PROOF), any()))
                .thenReturn(new DocumentDto(documentId, applicationId, DocumentType.COLLATERAL_PROOF,
                        "collateral.png", VerificationStatus.PENDING, Instant.now(), 0,
                        "/api/v1/documents/" + documentId + "/content", "image/png", bytes.length));
        mvc.perform(multipart(path()).file(image).param("docType", "COLLATERAL_PROOF"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.docType").value("COLLATERAL_PROOF"))
                .andExpect(jsonPath("$.contentType").value("image/png"));
        var part = ArgumentCaptor.forClass(MultipartFile.class);
        verify(documents).upload(eq(borrowerId), eq(applicationId), eq(DocumentType.COLLATERAL_PROOF), part.capture());
        assertEquals("file", part.getValue().getName());
        assertArrayEquals(bytes, part.getValue().getBytes());
    }
    @Test void storageFailureIsNotReportedAsAMultipartParsingError() throws Exception {
        when(documents.upload(any(), any(), any(), any())).thenThrow(new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE, "DOCUMENT_STORAGE_UNAVAILABLE", "Document storage is unavailable"));
        mvc.perform(multipart(path()).file(image).param("docType", "NATIONAL_ID"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DOCUMENT_STORAGE_UNAVAILABLE"));
    }
    @Test void missingFilePartIsRejected() throws Exception {
        mvc.perform(multipart(path()).param("docType", "COLLATERAL_PROOF"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_UPLOAD"));
        verifyNoInteractions(documents);
    }
    @Test void jsonIsRejectedBecauseThisEndpointRequiresMultipart() throws Exception {
        mvc.perform(post(path()).contentType("application/json").content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(documents);
    }
    @Test void staffCannotUploadAsABorrower() throws Exception {
        for (Role role : List.of(Role.ADMIN, Role.LOAN_OFFICER, Role.CREDIT_ANALYST, Role.BRANCH_MANAGER)) {
            authenticate(role);
            mvc.perform(multipart(path()).file(image).param("docType", "COLLATERAL_PROOF"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(documents);
    }
}
