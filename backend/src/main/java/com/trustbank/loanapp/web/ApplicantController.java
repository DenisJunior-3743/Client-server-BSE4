package com.trustbank.loanapp.web;

import com.trustbank.loanapp.service.ApplicantDirectoryService;
import com.trustbank.loanapp.web.dto.AuthDtos.ApplicantPageDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applicants")
public class ApplicantController {
    private static final int MAX_PAGE_SIZE = 100;
    private final ApplicantDirectoryService applicants;

    public ApplicantController(ApplicantDirectoryService applicants) {
        this.applicants = applicants;
    }

    @GetMapping
    @Operation(summary = "List registered applicants and their saved profiles (admin only)",
            description = "Includes borrowers who have not applied for a loan. National IDs are masked. "
                    + "Page numbers start at zero.")
    public ResponseEntity<ApplicantPageDto> list(
            @RequestParam(defaultValue = "0") @Min(0) @Max(Integer.MAX_VALUE / MAX_PAGE_SIZE) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(applicants.list(page, size));
    }
}
