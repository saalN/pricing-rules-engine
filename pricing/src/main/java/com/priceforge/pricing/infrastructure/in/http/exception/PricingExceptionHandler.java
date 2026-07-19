package com.priceforge.pricing.infrastructure.in.http.exception;

import com.priceforge.pricing.domain.exception.CurrencyMismatchException;
import com.priceforge.pricing.domain.exception.InvalidDiscountException;
import com.priceforge.pricing.domain.exception.PricingRuleNotActiveException;
import com.priceforge.pricing.domain.exception.PricingRuleNotFoundException;
import com.priceforge.pricing.domain.exception.ProductNotActiveException;
import com.priceforge.pricing.domain.exception.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.priceforge.pricing.infrastructure.in.http")
public class PricingExceptionHandler {

    @ExceptionHandler(InvalidDiscountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidDiscount(InvalidDiscountException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleCurrencyMismatch(CurrencyMismatchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PricingRuleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handlePricingRuleNotFound(PricingRuleNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleProductNotFound(ProductNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PricingRuleNotActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handlePricingRuleNotActive(PricingRuleNotActiveException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ProductNotActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleProductNotActive(ProductNotActiveException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
