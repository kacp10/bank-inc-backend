package com.bankinc.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/card/test");
    }

    @Test
    void handlesBusinessException() {
        BusinessException exception =
                new BusinessException(
                        "Card not found",
                        HttpStatus.NOT_FOUND
                );

        var response =
                handler.handleBusiness(exception, request);

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        ApiError body = response.getBody();

        assertNotNull(body);
        assertEquals(404, body.status());
        assertEquals("Not Found", body.error());
        assertEquals("Card not found", body.message());
        assertEquals("/card/test", body.path());
        assertNotNull(body.timestamp());
    }

    @Test
    void handlesValidationException() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(
                        new Object(),
                        "request"
                );

        bindingResult.addError(
                new FieldError(
                        "request",
                        "cardId",
                        "must not be blank"
                )
        );

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        null,
                        bindingResult
                );

        var response =
                handler.handleValidation(exception, request);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        ApiError body = response.getBody();

        assertNotNull(body);
        assertEquals(400, body.status());
        assertEquals("Bad Request", body.error());
        assertEquals(
                "cardId: must not be blank",
                body.message()
        );
        assertEquals("/card/test", body.path());
    }

    @Test
    void handlesIllegalStateException() {
        IllegalStateException exception =
                new IllegalStateException("Invalid state");

        var response =
                handler.handleState(exception, request);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        ApiError body = response.getBody();

        assertNotNull(body);
        assertEquals("Invalid state", body.message());
        assertEquals("/card/test", body.path());
    }
}