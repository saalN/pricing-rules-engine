package com.priceforge.catalog.infrastructure.in.http.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.catalog.application.dto.ChangeProductCategoryResult;
import com.priceforge.catalog.application.dto.CreateProductResult;
import com.priceforge.catalog.application.dto.DeactivateProductResult;
import com.priceforge.catalog.application.dto.UpdateProductBasePriceCommand;
import com.priceforge.catalog.application.dto.UpdateProductBasePriceResult;
import com.priceforge.catalog.application.port.in.ChangeProductCategoryUseCase;
import com.priceforge.catalog.application.port.in.CreateProductUseCase;
import com.priceforge.catalog.application.port.in.DeactivateProductUseCase;
import com.priceforge.catalog.application.port.in.UpdateProductBasePriceUseCase;
import com.priceforge.catalog.domain.exception.DuplicateSkuException;
import com.priceforge.catalog.domain.exception.InvalidBasePriceException;
import com.priceforge.catalog.domain.exception.ProductNotActiveException;
import com.priceforge.catalog.domain.exception.ProductNotFoundException;
import com.priceforge.catalog.domain.model.ProductStatus;
import com.priceforge.catalog.infrastructure.in.http.dto.ChangeProductCategoryRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.CreateProductRequest;
import com.priceforge.catalog.infrastructure.in.http.dto.UpdateProductBasePriceRequest;
import com.priceforge.catalog.infrastructure.in.http.exception.CatalogExceptionHandler;
import com.priceforge.catalog.infrastructure.in.http.mapper.ProductHttpMapper;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ProductHttpMapper.class, CatalogExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateProductUseCase createProductUseCase;

    @MockitoBean
    private UpdateProductBasePriceUseCase updateProductBasePriceUseCase;

    @MockitoBean
    private DeactivateProductUseCase deactivateProductUseCase;

    @MockitoBean
    private ChangeProductCategoryUseCase changeProductCategoryUseCase;

    private static final Principal ADMIN_PRINCIPAL = () -> "admin@priceforge.com";

    @Test
    void createProductReturns201WithProductId() throws Exception {
        ProductId productId = ProductId.newId();
        when(createProductUseCase.execute(any())).thenReturn(new CreateProductResult(productId));

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProductRequest(
                    "SKU-1", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.productId").value(productId.value().toString()));
    }

    @Test
    void createProductRejectsBlankSkuWith400() throws Exception {
        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProductRequest(
                    "", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null))))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(createProductUseCase);
    }

    @Test
    void createProductRejectsNonPositiveBasePriceWith400() throws Exception {
        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProductRequest(
                    "SKU-1", "Widget", "A widget", BigDecimal.ZERO, "EUR", "URBAN", null))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createProductMapsDuplicateSkuTo409() throws Exception {
        when(createProductUseCase.execute(any())).thenThrow(new DuplicateSkuException("SKU-1"));

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProductRequest(
                    "SKU-1", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null))))
            .andExpect(status().isConflict());
    }

    @Test
    void createProductMapsInvalidBasePriceTo400() throws Exception {
        when(createProductUseCase.execute(any()))
            .thenThrow(new InvalidBasePriceException("Base price must be positive"));

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProductRequest(
                    "SKU-1", "Widget", "A widget", BigDecimal.TEN, "EUR", "URBAN", null))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateBasePriceUsesAuthenticatedPrincipalAsChangedBy() throws Exception {
        ProductId productId = ProductId.newId();
        when(updateProductBasePriceUseCase.execute(any())).thenReturn(
            new UpdateProductBasePriceResult(productId, BigDecimal.TEN, BigDecimal.valueOf(20), "EUR"));

        mockMvc.perform(patch("/products/{id}/base-price", productId.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProductBasePriceRequest(BigDecimal.valueOf(20))))
                .principal(ADMIN_PRINCIPAL))
            .andExpect(status().isOk());

        ArgumentCaptor<UpdateProductBasePriceCommand> captor =
            ArgumentCaptor.forClass(UpdateProductBasePriceCommand.class);
        verify(updateProductBasePriceUseCase).execute(captor.capture());
        assertThat(captor.getValue().changedBy()).isEqualTo("admin@priceforge.com");
    }

    @Test
    void updateBasePriceMapsProductNotFoundTo404() throws Exception {
        ProductId productId = ProductId.newId();
        when(updateProductBasePriceUseCase.execute(any())).thenThrow(new ProductNotFoundException(productId));

        mockMvc.perform(patch("/products/{id}/base-price", productId.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProductBasePriceRequest(BigDecimal.TEN)))
                .principal(ADMIN_PRINCIPAL))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateBasePriceMapsProductNotActiveTo409() throws Exception {
        ProductId productId = ProductId.newId();
        when(updateProductBasePriceUseCase.execute(any())).thenThrow(new ProductNotActiveException(productId));

        mockMvc.perform(patch("/products/{id}/base-price", productId.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProductBasePriceRequest(BigDecimal.TEN)))
                .principal(ADMIN_PRINCIPAL))
            .andExpect(status().isConflict());
    }

    @Test
    void deactivateReturns200WithStatus() throws Exception {
        ProductId productId = ProductId.newId();
        when(deactivateProductUseCase.execute(any()))
            .thenReturn(new DeactivateProductResult(productId, ProductStatus.INACTIVE));

        mockMvc.perform(post("/products/{id}/deactivate", productId.value()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void changeCategoryRejectsBlankCategoryWith400() throws Exception {
        mockMvc.perform(patch("/products/{id}/category", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangeProductCategoryRequest(""))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changeCategoryReturns200() throws Exception {
        ProductId productId = ProductId.newId();
        when(changeProductCategoryUseCase.execute(any()))
            .thenReturn(new ChangeProductCategoryResult(productId, "URBAN", "RURAL"));

        mockMvc.perform(patch("/products/{id}/category", productId.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangeProductCategoryRequest("RURAL"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.newCategory").value("RURAL"));
    }
}
