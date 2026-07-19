package com.priceforge.pricing.infrastructure.in.http.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.pricing.application.dto.CreatePricingRuleResult;
import com.priceforge.pricing.application.dto.DeactivatePricingRuleResult;
import com.priceforge.pricing.application.dto.UpdatePricingRuleResult;
import com.priceforge.pricing.application.port.in.CreatePricingRuleUseCase;
import com.priceforge.pricing.application.port.in.DeactivatePricingRuleUseCase;
import com.priceforge.pricing.application.port.in.UpdatePricingRuleUseCase;
import com.priceforge.pricing.domain.exception.InvalidDiscountException;
import com.priceforge.pricing.domain.model.CombinationMode;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRuleStatus;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.pricing.infrastructure.in.http.dto.PricingRuleDefinitionRequest;
import com.priceforge.pricing.infrastructure.in.http.dto.UpdatePricingRuleRequest;
import com.priceforge.pricing.infrastructure.in.http.exception.PricingExceptionHandler;
import com.priceforge.pricing.infrastructure.in.http.mapper.PricingRuleHttpMapper;
import com.priceforge.sharedkernel.PricingRuleId;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PricingRuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PricingRuleHttpMapper.class, PricingExceptionHandler.class})
class PricingRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreatePricingRuleUseCase createPricingRuleUseCase;

    @MockitoBean
    private UpdatePricingRuleUseCase updatePricingRuleUseCase;

    @MockitoBean
    private DeactivatePricingRuleUseCase deactivatePricingRuleUseCase;

    private static final Principal ADMIN_PRINCIPAL = () -> "admin@priceforge.com";

    private PricingRuleDefinitionRequest timeBasedRequest() {
        return new PricingRuleDefinitionRequest(
            PricingRuleType.TIME_BASED, LocalTime.of(22, 0), LocalTime.of(6, 0), null, null, null,
            DiscountType.PERCENTAGE, BigDecimal.valueOf(20), 10, CombinationMode.STACKABLE, null, null);
    }

    @Test
    void createRuleReturns201WithRuleId() throws Exception {
        PricingRuleId ruleId = PricingRuleId.newId();
        when(createPricingRuleUseCase.execute(any())).thenReturn(new CreatePricingRuleResult(ruleId));

        mockMvc.perform(post("/pricing-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timeBasedRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ruleId").value(ruleId.value().toString()));
    }

    @Test
    void createRuleMapsInvalidDiscountTo400() throws Exception {
        when(createPricingRuleUseCase.execute(any()))
            .thenThrow(new InvalidDiscountException("bad discount"));

        mockMvc.perform(post("/pricing-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timeBasedRequest())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createRuleRejectsANonPositiveDiscountValueWith400() throws Exception {
        PricingRuleDefinitionRequest invalid = new PricingRuleDefinitionRequest(
            PricingRuleType.TIME_BASED, LocalTime.of(22, 0), LocalTime.of(6, 0), null, null, null,
            DiscountType.PERCENTAGE, BigDecimal.ZERO, 10, CombinationMode.STACKABLE, null, null);

        mockMvc.perform(post("/pricing-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateRuleUsesTheAuthenticatedPrincipalAsChangedBy() throws Exception {
        PricingRuleId ruleId = PricingRuleId.newId();
        when(updatePricingRuleUseCase.execute(any()))
            .thenReturn(new UpdatePricingRuleResult(ruleId, "old", "new"));

        UpdatePricingRuleRequest request = new UpdatePricingRuleRequest(
            LocalTime.of(23, 0), LocalTime.of(5, 0), null, null, null,
            DiscountType.PERCENTAGE, BigDecimal.valueOf(30), 20, CombinationMode.EXCLUSIVE);

        mockMvc.perform(patch("/pricing-rules/{id}", ruleId.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(ADMIN_PRINCIPAL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.previousState").value("old"));
    }

    @Test
    void deactivateRuleReturns200WithStatus() throws Exception {
        PricingRuleId ruleId = PricingRuleId.newId();
        when(deactivatePricingRuleUseCase.execute(any()))
            .thenReturn(new DeactivatePricingRuleResult(ruleId, PricingRuleStatus.INACTIVE));

        mockMvc.perform(post("/pricing-rules/{id}/deactivate", ruleId.value()).principal(ADMIN_PRINCIPAL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }
}
