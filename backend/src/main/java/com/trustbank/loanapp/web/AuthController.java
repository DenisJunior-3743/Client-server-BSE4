package com.trustbank.loanapp.web;

import com.trustbank.loanapp.service.AuthService;
import com.trustbank.loanapp.web.dto.AuthDtos.LoginRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.LoginResponse;
import com.trustbank.loanapp.web.dto.AuthDtos.OtpIssuedResponse;
import com.trustbank.loanapp.web.dto.AuthDtos.OtpResendRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.OtpVerifyRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.RegisterRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints (no token yet). Controllers hold no state: one instance serves every request
 * thread, so anything request-specific stays in parameters and locals.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/otp/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        auth.verifyOtp(request.email(), request.code());
    }

    @PostMapping("/otp/resend")
    public OtpIssuedResponse resendOtp(@Valid @RequestBody OtpResendRequest request) {
        return auth.resendOtp(request.email());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }
}
