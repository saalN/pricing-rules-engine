package com.priceforge.pricing.infrastructure.in.http.controller;

import com.priceforge.pricing.application.dto.DeactivatePricingRuleCommand;
import com.priceforge.pricing.application.port.in.CreatePricingRuleUseCase;
import com.priceforge.pricing.application.port.in.DeactivatePricingRuleUseCase;
import com.priceforge.pricing.application.port.in.UpdatePricingRuleUseCase;
import com.priceforge.pricing.infrastructure.in.http.dto.CreatePricingRuleResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.DeactivatePricingRuleResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.PricingRuleDefinitionRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.UpdatePricingRuleRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.UpdatePricingRuleResponse;
import com.priceforge.pricing.infrastructure.in.http.mapper.PricingRuleHttpMapper;
import com.priceforge.sharedkernel.PricingRuleId;
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
@RequestMapping("/pricing-rules")
@Tag(name = "Pricing", description = "Pricing rule management (UC-05, UC-09)")
public class PricingRuleController {

    private final CreatePricingRuleUseCase createPricingRuleUseCase;
    private final UpdatePricingRuleUseCase updatePricingRuleUseCase;
    private final DeactivatePricingRuleUseCase deactivatePricingRuleUseCase;
    private final PricingRuleHttpMapper mapper;

    public PricingRuleController(
            CreatePricingRuleUseCase createPricingRuleUseCase,
            UpdatePricingRuleUseCase updatePricingRuleUseCase,
            DeactivatePricingRuleUseCase deactivatePricingRuleUseCase,
            PricingRuleHttpMapper mapper) {
        this.createPricingRuleUseCase = createPricingRuleUseCase;
        this.updatePricingRuleUseCase = updatePricingRuleUseCase;
        this.deactivatePricingRuleUseCase = deactivatePricingRuleUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "UC-05: Create pricing rule")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CreatePricingRuleResponse createRule(@Valid @RequestBody PricingRuleDefinitionRequest request) {
        return mapper.toResponse(createPricingRuleUseCase.execute(mapper.toCommand(request)));
    }

    @Operation(summary = "UC-09: Modify pricing rule")
    @PatchMapping("/{ruleId}")
    public UpdatePricingRuleResponse updateRule(
            @PathVariable UUID ruleId, @Valid @RequestBody UpdatePricingRuleRequest request, Principal principal) {
        return mapper.toResponse(
            updatePricingRuleUseCase.execute(mapper.toCommand(ruleId, request, principal.getName())));
    }

    @Operation(summary = "UC-09: Deactivate pricing rule")
    @PostMapping("/{ruleId}/deactivate")
    public DeactivatePricingRuleResponse deactivateRule(@PathVariable UUID ruleId, Principal principal) {
        return mapper.toResponse(deactivatePricingRuleUseCase.execute(
            new DeactivatePricingRuleCommand(new PricingRuleId(ruleId), principal.getName())));
    }
}
