package com.priceforge.pricing.infrastructure.in.http.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.pricing.application.dto.CalculatePriceResult;
import com.priceforge.pricing.application.port.in.CalculatePriceUseCase;
import com.priceforge.pricing.application.port.in.SimulatePriceUseCase;
import com.priceforge.pricing.domain.exception.CurrencyMismatchException;
import com.priceforge.pricing.domain.exception.ProductNotFoundException;
import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.pricing.infrastructure.in.http.dto.CalculatePriceRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.SimulatePriceRequest;
import com.priceforge.pricing.infrastructure.in.http.exception.PricingExceptionHandler;
import com.priceforge.pricing.infrastructure.in.http.mapper.PriceCalculationHttpMapper;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PriceCalculationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PriceCalculationHttpMapper.class, PricingExceptionHandler.class})
class PriceCalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CalculatePriceUseCase calculatePriceUseCase;

    @MockitoBean
    private SimulatePriceUseCase simulatePriceUseCase;

    private CalculatePriceResult sampleResult(boolean simulated) {
        return new CalculatePriceResult(
            BigDecimal.valueOf(100), BigDecimal.valueOf(80), "EUR", List.of(), List.of(), List.of(), List.of(),
            Instant.now(), simulated);
    }

    @Test
    void calculateReturnsThePriceQuote() throws Exception {
        when(calculatePriceUseCase.execute(any())).thenReturn(sampleResult(false));

        CalculatePriceRequest request = new CalculatePriceRequest(
            UUID.randomUUID(), UUID.randomUUID(), CustomerSegment.VIP, 1, null, "URBAN", "EUR", null);

        mockMvc.perform(post("/price-calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.finalPriceAmount").value(80))
            .andExpect(jsonPath("$.simulated").value(false));
    }

    @Test
    void calculateMapsProductNotFoundTo404() throws Exception {
        when(calculatePriceUseCase.execute(any())).thenThrow(new ProductNotFoundException(ProductId.newId()));

        CalculatePriceRequest request = new CalculatePriceRequest(
            UUID.randomUUID(), UUID.randomUUID(), CustomerSegment.VIP, 1, null, "URBAN", "EUR", null);

        mockMvc.perform(post("/price-calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void calculateMapsCurrencyMismatchTo400() throws Exception {
        when(calculatePriceUseCase.execute(any())).thenThrow(new CurrencyMismatchException("USD", "EUR"));

        CalculatePriceRequest request = new CalculatePriceRequest(
            UUID.randomUUID(), UUID.randomUUID(), CustomerSegment.VIP, 1, null, "URBAN", "USD", null);

        mockMvc.perform(post("/price-calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void simulateReturnsAQuoteFlaggedAsSimulated() throws Exception {
        when(simulatePriceUseCase.execute(any())).thenReturn(sampleResult(true));

        SimulatePriceRequest request = new SimulatePriceRequest(
            UUID.randomUUID(), UUID.randomUUID(), CustomerSegment.VIP, 1, null, "URBAN", "EUR", null, null);

        mockMvc.perform(post("/price-calculations/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulated").value(true));
    }

    @Test
    void calculateRejectsANonPositiveQuantityWith400() throws Exception {
        CalculatePriceRequest request = new CalculatePriceRequest(
            UUID.randomUUID(), UUID.randomUUID(), CustomerSegment.VIP, 0, null, "URBAN", "EUR", null);

        mockMvc.perform(post("/price-calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
