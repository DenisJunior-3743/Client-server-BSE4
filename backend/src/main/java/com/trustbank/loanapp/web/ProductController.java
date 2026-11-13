package com.trustbank.loanapp.web;

import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.security.CurrentUser;
import com.trustbank.loanapp.service.ProductService;
import com.trustbank.loanapp.web.dto.LoanDtos.ProductDto;
import com.trustbank.loanapp.web.dto.LoanDtos.ProductRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    /** Borrowers see what they can apply for; staff also see retired products. */
    @GetMapping
    public List<ProductDto> list(@CurrentUser AuthUser user) {
        return products.list(user.isStaff());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@CurrentUser AuthUser user, @Valid @RequestBody ProductRequest request) {
        return products.create(user.id(), request);
    }
}
