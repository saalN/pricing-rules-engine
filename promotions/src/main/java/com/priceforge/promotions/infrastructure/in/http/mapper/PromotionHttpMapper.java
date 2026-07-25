package com.priceforge.promotions.infrastructure.in.http.mapper;

import com.priceforge.promotions.application.dto.CreatePromotionCommand;
import com.priceforge.promotions.application.dto.CreatePromotionResult;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionRequest;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionResponse;
import org.springframework.stereotype.Component;

@Component
public class PromotionHttpMapper {

    public CreatePromotionCommand toCommand(CreatePromotionRequest request) {
        return new CreatePromotionCommand(
            request.name(),
            request.description(),
            request.validFrom(),
            request.validUntil(),
            request.globalUsageLimit(),
            request.perUserUsageLimit(),
            request.segment(),
            request.category(),
            request.minimumPurchaseAmount(),
            request.discountType(),
            request.discountValue(),
            request.currency(),
            request.priority(),
            request.combinationMode());
    }

    public CreatePromotionResponse toResponse(CreatePromotionResult result) {
        return new CreatePromotionResponse(result.id().value());
    }
}
