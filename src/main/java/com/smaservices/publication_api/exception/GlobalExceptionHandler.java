package com.smaservices.publication_api.exception;

import com.smaservices.publication_api.dto.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException exception,
            HttpServletRequest request) {

        HttpStatus status =
                exception.getStatus();

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getCode(),
                        exception.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message =
                exception
                        .getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::formatFieldError)
                        .collect(
                                Collectors.joining(", ")
                        );

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST
                                .getReasonPhrase(),
                        "VALIDATION_ERROR",
                        message,
                        request.getRequestURI()
                );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST
                                .getReasonPhrase(),
                        "BAD_REQUEST",
                        exception.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(
            IllegalStateException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleIllegalStateException(
            IllegalStateException exception,
            HttpServletRequest request) {

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT
                                .getReasonPhrase(),
                        "INVALID_STATE",
                        exception.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse>
    handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        LOGGER.error(
                "Unhandled exception on {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR
                                .value(),
                        HttpStatus.INTERNAL_SERVER_ERROR
                                .getReasonPhrase(),
                        "INTERNAL_ERROR",
                        "Une erreur interne est survenue.",
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(response);
    }

    private String formatFieldError(
            FieldError fieldError) {

        String message =
                fieldError.getDefaultMessage();

        if (message == null ||
                message.isBlank()) {

            message =
                    "valeur invalide";
        }

        return fieldError.getField()
                + ": "
                + message;
    }
}