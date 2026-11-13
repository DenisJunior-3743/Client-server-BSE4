package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.LoanProduct;
import com.trustbank.loanapp.event.AuditEvent;
import com.trustbank.loanapp.repo.LoanProductRepository;
import com.trustbank.loanapp.web.dto.DtoMapper;
import com.trustbank.loanapp.web.dto.LoanDtos.ProductDto;
import com.trustbank.loanapp.web.dto.LoanDtos.ProductRequest;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final LoanProductRepository products;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ProductService(LoanProductRepository products, ApplicationEventPublisher events, Clock clock) {
        this.products = products;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> list(boolean includeInactive) {
        List<LoanProduct> rows = includeInactive ? products.findAllByOrderByNameAsc()
                : products.findByActiveTrueOrderByNameAsc();
        return rows.stream().map(DtoMapper::product).toList();
    }

    @Transactional
    public ProductDto create(UUID actorId, ProductRequest request) {
        if (request.minAmount().compareTo(request.maxAmount()) > 0
                || request.minTermMonths() > request.maxTermMonths()) {
            throw ApiException.rule("MIN_EXCEEDS_MAX", "Minimum cannot exceed maximum");
        }
        if (products.existsByNameIgnoreCase(request.name().trim())) {
            throw ApiException.conflict("PRODUCT_EXISTS", "A product with this name already exists");
        }
        LoanProduct product = new LoanProduct();
        product.setName(request.name().trim());
        product.setType(request.type());
        product.setInterestRateAnnual(request.interestRateAnnual());
        product.setMinAmount(request.minAmount());
        product.setMaxAmount(request.maxAmount());
        product.setMinTermMonths(request.minTermMonths());
        product.setMaxTermMonths(request.maxTermMonths());
        product.setCreatedAt(clock.instant());
        product = products.save(product);
        events.publishEvent(new AuditEvent("LOAN_PRODUCT", product.getId(), "PRODUCT_CREATED", actorId,
                Map.of("name", product.getName())));
        return DtoMapper.product(product);
    }
}
