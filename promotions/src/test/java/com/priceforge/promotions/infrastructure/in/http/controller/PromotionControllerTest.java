package com.priceforge.promotions.infrastructure.in.http.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.promotions.application.dto.CreatePromotionResult;
import com.priceforge.promotions.application.port.in.CreatePromotionUseCase;
import com.priceforge.promotions.domain.exception.InvalidPromotionDiscountException;
import com.priceforge.promotions.domain.exception.InvalidPromotionValidityWindowException;
import com.priceforge.promotions.domain.model.DiscountType;
import com.priceforge.promotions.infrastructure.in.http.dto.CreatePromotionRequest;
import com.priceforge.promotions.infrastructure.in.http.exception.PromotionsExceptionHandler;
import com.priceforge.promotions.infrastructure.in.http.mapper.PromotionHttpMapper;
import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.PromotionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PromotionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PromotionHttpMapper.class, PromotionsExceptionHandler.class})
class PromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreatePromotionUseCase createPromotionUseCase;

    private CreatePromotionRequest request(BigDecimal discountValue) {
        return new CreatePromotionRequest(
            "Summer sale", "10% off", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS),
            1000, 1, null, "URBAN", null, DiscountType.PERCENTAGE, discountValue, "EUR", 10,
            CombinationMode.STACKABLE);
    }

    @Test
    void createReturns201WithThePromotionId() throws Exception {
        PromotionId id = PromotionId.newId();
        when(createPromotionUseCase.execute(any())).thenReturn(new CreatePromotionResult(id));

        mockMvc.perform(post("/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request(BigDecimal.TEN))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.promotionId").value(id.value().toString()));
    }

    @Test
    void createRejectsAMissingNameWith400() throws Exception {
        CreatePromotionRequest invalid = new CreatePromotionRequest(
            "", "d", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS),
            1000, 1, null, "URBAN", null, DiscountType.PERCENTAGE, BigDecimal.TEN, "EUR", 10,
            CombinationMode.STACKABLE);

        mockMvc.perform(post("/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createMapsInvalidDiscountTo400() throws Exception {
        when(createPromotionUseCase.execute(any()))
            .thenThrow(new InvalidPromotionDiscountException("would breach zero"));

        mockMvc.perform(post("/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request(BigDecimal.valueOf(99)))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createMapsInvalidValidityWindowTo400() throws Exception {
        when(createPromotionUseCase.execute(any()))
            .thenThrow(new InvalidPromotionValidityWindowException("validFrom must be before validUntil"));

        mockMvc.perform(post("/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request(BigDecimal.TEN))))
            .andExpect(status().isBadRequest());
    }
}
