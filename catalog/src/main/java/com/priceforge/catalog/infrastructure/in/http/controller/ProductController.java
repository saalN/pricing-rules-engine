package com.priceforge.catalog.infrastructure.in.http.controller;

import com.priceforge.catalog.application.port.in.ChangeProductCategoryUseCase;
import com.priceforge.catalog.application.port.in.CreateProductUseCase;
import com.priceforge.catalog.application.port.in.DeactivateProductUseCase;
import com.priceforge.catalog.application.port.in.UpdateProductBasePriceUseCase;
import com.priceforge.catalog.infrastructure.in.http.dto.ChangeProductCategoryRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.ChangeProductCategoryResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.DeactivateProductResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.UpdateProductBasePriceRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.UpdateProductBasePriceResponse;
import com.priceforge.catalog.infrastructure.in.http.mapper.ProductHttpMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@Tag(name = "Catalog", description = "Product catalog management (UC-01 to UC-04)")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductBasePriceUseCase updateProductBasePriceUseCase;
    private final DeactivateProductUseCase deactivateProductUseCase;
    private final ChangeProductCategoryUseCase changeProductCategoryUseCase;
    private final ProductHttpMapper mapper;

    public ProductController(
            CreateProductUseCase createProductUseCase,
            UpdateProductBasePriceUseCase updateProductBasePriceUseCase,
            DeactivateProductUseCase deactivateProductUseCase,
            ChangeProductCategoryUseCase changeProductCategoryUseCase,
            ProductHttpMapper mapper) {
        this.createProductUseCase = createProductUseCase;
        this.updateProductBasePriceUseCase = updateProductBasePriceUseCase;
        this.deactivateProductUseCase = deactivateProductUseCase;
        this.changeProductCategoryUseCase = changeProductCategoryUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "UC-01: Create product")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CreateProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return mapper.toResponse(createProductUseCase.execute(mapper.toCommand(request)));
    }

    @Operation(summary = "UC-02: Update product base price")
    @PatchMapping("/{productId}/base-price")
    public UpdateProductBasePriceResponse updateBasePrice(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductBasePriceRequest request,
            Principal principal) {
        return mapper.toResponse(updateProductBasePriceUseCase.execute(
            mapper.toCommand(productId, request, principal.getName())));
    }

    @Operation(summary = "UC-03: Deactivate product")
    @PostMapping("/{productId}/deactivate")
    public DeactivateProductResponse deactivate(@PathVariable UUID productId) {
        return mapper.toResponse(
            deactivateProductUseCase.execute(mapper.toDeactivateCommand(productId)));
    }

    @Operation(summary = "UC-04: Categorize product")
    @PatchMapping("/{productId}/category")
    public ChangeProductCategoryResponse changeCategory(
            @PathVariable UUID productId, @Valid @RequestBody ChangeProductCategoryRequest request) {
        return mapper.toResponse(
            changeProductCategoryUseCase.execute(mapper.toCommand(productId, request)));
    }
}
