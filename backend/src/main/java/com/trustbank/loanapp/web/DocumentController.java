package com.trustbank.loanapp.web;

import com.trustbank.loanapp.domain.DocumentType;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.security.CurrentUser;
import com.trustbank.loanapp.service.DocumentService;
import com.trustbank.loanapp.web.dto.LoanDtos.DocumentDto;
import io.swagger.v3.oas.annotations.Operation;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {
    private final DocumentService documents;
    public DocumentController(DocumentService documents) { this.documents = documents; }

    @Operation(summary = "Upload a PDF, JPEG or PNG document (maximum 5 MiB) to GridFS")
    @PostMapping(value = "/applications/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('BORROWER')")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto upload(@CurrentUser AuthUser user, @PathVariable UUID id,
            @RequestParam DocumentType docType, @RequestPart MultipartFile file) {
        return documents.upload(user.id(), id, docType, file);
    }

    @Operation(summary = "Download document bytes; application owner or authenticated staff only")
    @GetMapping("/documents/{id}/content")
    public ResponseEntity<Resource> download(@CurrentUser AuthUser user, @PathVariable UUID id) {
        var download = documents.download(user, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.filename(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new InputStreamResource(download.stream()));
    }
}
