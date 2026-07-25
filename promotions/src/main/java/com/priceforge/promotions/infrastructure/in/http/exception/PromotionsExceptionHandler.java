package com.priceforge.promotions.infrastructure.in.http.exception;

import com.priceforge.promotions.domain.exception.InvalidPromotionDiscountException;
import com.priceforge.promotions.domain.exception.InvalidPromotionStateTransitionException;
import com.priceforge.promotions.domain.exception.InvalidPromotionValidityWindowException;
import com.priceforge.promotions.domain.exception.PromotionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.priceforge.promotions.infrastructure.in.http")
public class PromotionsExceptionHandler {

    @ExceptionHandler(InvalidPromotionDiscountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidDiscount(InvalidPromotionDiscountException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidPromotionValidityWindowException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidValidityWindow(InvalidPromotionValidityWindowException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PromotionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handlePromotionNotFound(PromotionNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidPromotionStateTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleInvalidStateTransition(InvalidPromotionStateTransitionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
