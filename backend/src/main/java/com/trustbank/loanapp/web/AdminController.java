package com.trustbank.loanapp.web;

import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.security.CurrentUser;
import com.trustbank.loanapp.service.StaffService;
import com.trustbank.loanapp.web.dto.AuthDtos.StaffCreatedResponse;
import com.trustbank.loanapp.web.dto.AuthDtos.StaffCreateRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.StaffCredentialsRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.StaffRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.UserDto;
import com.trustbank.loanapp.web.dto.LoanDtos.AuditLogDto;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AdminController {

    private final StaffService staff;

    public AdminController(StaffService staff) {
        this.staff = staff;
    }

    /** Any staff member may see the staff directory (names on the dashboard); only admins may add to it. */
    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','CREDIT_ANALYST','BRANCH_MANAGER','ADMIN')")
    public List<UserDto> listStaff() {
        return staff.listStaff();
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create staff; email is the username, password is optional (generated when omitted)")
    public StaffCreatedResponse createStaff(@CurrentUser AuthUser user, @Valid @RequestBody StaffCreateRequest request) {
        return staff.createStaff(user.id(), request.details(), request.password());
    }

    @PutMapping("/staff/{id}/credentials")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set or replace staff password; keeps the email username and revokes existing sessions")
    public void setStaffCredentials(@CurrentUser AuthUser user, @PathVariable UUID id,
            @Valid @RequestBody StaffCredentialsRequest request) {
        staff.setStaffCredentials(user.id(), id, request.password());
    }

    @GetMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get an active staff account for editing")
    public UserDto getStaff(@PathVariable UUID id) {
        return staff.getStaff(id);
    }

    @PutMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edit staff details and role; preserves the existing password")
    public UserDto updateStaff(@CurrentUser AuthUser user, @PathVariable UUID id,
            @Valid @RequestBody StaffRequest request) {
        return staff.updateStaff(user.id(), id, request);
    }

    @DeleteMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete staff access and directory entry while preserving loan and audit history")
    public void deleteStaff(@CurrentUser AuthUser user, @PathVariable UUID id) {
        staff.deleteStaff(user.id(), id);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLogDto> auditLog(@RequestParam(defaultValue = "200") int limit) {
        return staff.auditLog(limit);
    }
}
