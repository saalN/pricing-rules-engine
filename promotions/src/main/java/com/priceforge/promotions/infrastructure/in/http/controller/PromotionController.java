package com.priceforge.promotions.infrastructure.in.http.controller;

import com.priceforge.promotions.application.port.in.CreatePromotionUseCase;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionRequest;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionResponse;
import com.priceforge.promotions.infrastructure.in.http.mapper.PromotionHttpMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions")
@Tag(name = "Promotions", description = "Promotional campaign management (UC-10)")
public class PromotionController {

    private final CreatePromotionUseCase createPromotionUseCase;
    private final PromotionHttpMapper mapper;

    public PromotionController(CreatePromotionUseCase createPromotionUseCase, PromotionHttpMapper mapper) {
        this.createPromotionUseCase = createPromotionUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "UC-10: Create promotional campaign")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CreatePromotionResponse create(@Valid @RequestBody CreatePromotionRequest request) {
        return mapper.toResponse(createPromotionUseCase.execute(mapper.toCommand(request)));
    }
}
