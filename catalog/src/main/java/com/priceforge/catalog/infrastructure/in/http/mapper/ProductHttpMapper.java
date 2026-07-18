package com.priceforge.catalog.infrastructure.in.http.mapper;

import com.priceforge.catalog.application.dto.ChangeProductCategoryCommand;
import com.priceforge.catalog.application.dto.ChangeProductCategoryResult;
import com.priceforge.catalog.application.dto.CreateProductCommand;
import com.priceforge.catalog.application.dto.CreateProductResult;
import com.priceforge.catalog.application.dto.DeactivateProductCommand;
import com.priceforge.catalog.application.dto.DeactivateProductResult;
import com.priceforge.catalog.application.dto.UpdateProductBasePriceCommand;
import com.priceforge.catalog.application.dto.UpdateProductBasePriceResult;
import com.priceforge.catalog.infrastructure.in.http.dto.ChangeProductCategoryRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.ChangeProductCategoryResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.DeactivateProductResponse;
import com.priceforge.catalog.infrastructure.in.http.dto.UpdateProductBasePriceRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.UpdateProductBasePriceResponse;
import com.priceforge.sharedkernel.ProductId;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductHttpMapper {

    public CreateProductCommand toCommand(CreateProductRequest request) {
        return new CreateProductCommand(
            request.sku(),
            request.name(),
            request.description(),
            request.basePriceAmount(),
            request.currencyCode(),
            request.category(),
            request.customAttributes());
    }

    public CreateProductResponse toResponse(CreateProductResult result) {
        return new CreateProductResponse(result.productId().value());
    }

    public UpdateProductBasePriceCommand toCommand(
            UUID productId, UpdateProductBasePriceRequest request, String changedBy) {
        return new UpdateProductBasePriceCommand(
            new ProductId(productId), request.newBasePriceAmount(), changedBy);
    }

    public UpdateProductBasePriceResponse toResponse(UpdateProductBasePriceResult result) {
        return new UpdateProductBasePriceResponse(
            result.productId().value(),
            result.previousBasePriceAmount(),
            result.newBasePriceAmount(),
            result.currency());
    }

    public DeactivateProductCommand toDeactivateCommand(UUID productId) {
        return new DeactivateProductCommand(new ProductId(productId));
    }

    public DeactivateProductResponse toResponse(DeactivateProductResult result) {
        return new DeactivateProductResponse(result.productId().value(), result.status().name());
    }

    public ChangeProductCategoryCommand toCommand(UUID productId, ChangeProductCategoryRequest request) {
        return new ChangeProductCategoryCommand(new ProductId(productId), request.newCategory());
    }

    public ChangeProductCategoryResponse toResponse(ChangeProductCategoryResult result) {
        return new ChangeProductCategoryResponse(
            result.productId().value(), result.previousCategory(), result.newCategory());
    }
}
