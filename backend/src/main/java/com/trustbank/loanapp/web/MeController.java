package com.trustbank.loanapp.web;

import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.security.CurrentUser;
import com.trustbank.loanapp.service.AccountService;
import com.trustbank.loanapp.web.dto.AuthDtos.ChangePasswordRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.ProfileDto;
import com.trustbank.loanapp.web.dto.AuthDtos.ProfileRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.UpdateMeRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.UserDto;
import com.trustbank.loanapp.web.dto.LoanDtos.NotificationDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Everything here acts on the caller's own records; the id always comes from the token, never the URL. */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final AccountService account;

    public MeController(AccountService account) {
        this.account = account;
    }

    @GetMapping
    public UserDto me(@CurrentUser AuthUser user) {
        return account.me(user.id());
    }

    @PutMapping
    public UserDto update(@CurrentUser AuthUser user, @Valid @RequestBody UpdateMeRequest request) {
        return account.updateMe(user.id(), request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUser AuthUser user, @Valid @RequestBody ChangePasswordRequest request) {
        account.changePassword(user.id(), request);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('BORROWER')")
    public ProfileDto profile(@CurrentUser AuthUser user) {
        return account.profile(user.id());
    }

    /** PUT = idempotent by definition: sending the same profile twice leaves the same profile. */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('BORROWER')")
    public ProfileDto saveProfile(@CurrentUser AuthUser user, @Valid @RequestBody ProfileRequest request) {
        return account.saveProfile(user.id(), request);
    }

    @GetMapping("/notifications")
    public List<NotificationDto> notifications(@CurrentUser AuthUser user) {
        return account.notifications(user.id());
    }

    @PostMapping("/notifications/{id}/read")
    public NotificationDto markRead(@CurrentUser AuthUser user, @PathVariable UUID id) {
        return account.markRead(user.id(), id);
    }
}
