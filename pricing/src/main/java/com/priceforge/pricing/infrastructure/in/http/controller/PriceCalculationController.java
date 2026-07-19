package com.priceforge.pricing.infrastructure.in.http.controller;

import com.priceforge.pricing.application.port.in.CalculatePriceUseCase;
import com.priceforge.pricing.application.port.in.SimulatePriceUseCase;
import com.priceforge.pricing.infrastructure.in.http.dto.CalculatePriceRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.PriceQuoteResponse;
import com.priceforge.pricing.infrastructure.in.http.dto.SimulatePriceRequest;
import com.priceforge.pricing.infrastructure.in.http.mapper.PriceCalculationHttpMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/price-calculations")
@Tag(name = "Pricing", description = "Price calculation and simulation (UC-06, UC-08)")
public class PriceCalculationController {

    private final CalculatePriceUseCase calculatePriceUseCase;
    private final SimulatePriceUseCase simulatePriceUseCase;
    private final PriceCalculationHttpMapper mapper;

    public PriceCalculationController(
            CalculatePriceUseCase calculatePriceUseCase,
            SimulatePriceUseCase simulatePriceUseCase,
            PriceCalculationHttpMapper mapper) {
        this.calculatePriceUseCase = calculatePriceUseCase;
        this.simulatePriceUseCase = simulatePriceUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "UC-06: Calculate final price")
    @PostMapping
    public PriceQuoteResponse calculate(@Valid @RequestBody CalculatePriceRequest request) {
        return mapper.toResponse(calculatePriceUseCase.execute(mapper.toCommand(request)));
    }

    @Operation(summary = "UC-08: What-if price simulator — persists nothing")
    @PostMapping("/simulate")
    public PriceQuoteResponse simulate(@Valid @RequestBody SimulatePriceRequest request) {
        return mapper.toResponse(simulatePriceUseCase.execute(mapper.toCommand(request)));
    }
}
