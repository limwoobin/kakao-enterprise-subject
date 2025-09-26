package com.example.spotify_song_subject.controller;

import com.example.spotify_song_subject.controller.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * WebFlux 환경에서의 Global Exception Handler
 * @RestControllerAdvice를 사용하여 전역 예외 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * MissingRequestValueException 처리 (Required 파라미터 누락)
     * 400 Bad Request 반환
     */
    @ExceptionHandler(MissingRequestValueException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleMissingRequestValueException(MissingRequestValueException ex,
                                                                                  ServerWebExchange exchange) {

        log.warn("Missing request value: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getReason() != null ? ex.getReason() : "Required parameter is missing",
                "MISSING_PARAMETER",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse));
    }

    /**
     * Bean Validation 예외 처리 (WebFlux 환경)
     * @Valid 어노테이션으로 검증 실패 시 발생
     * 400 Bad Request 반환
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex,
                                                                         ServerWebExchange exchange) {

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errors);

        ErrorResponse errorResponse = ErrorResponse.of(
                errors,
                "VALIDATION_ERROR",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse));
    }

    /**
     * IllegalArgumentException 처리 (잘못된 요청 파라미터)
     * 400 Bad Request 반환
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException ex,
                                                                              ServerWebExchange exchange) {

        log.warn("Invalid request: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getMessage(),
                "BAD_REQUEST",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse));
    }

    /**
     * Exception 처리 (가장 일반적인 예외)
     * 다른 핸들러에서 처리되지 않은 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception ex,
                                                               ServerWebExchange exchange) {

        log.error("Exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(
                "Internal server error",
                "INTERNAL_SERVER_ERROR",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
    }

    /**
     * Throwable 처리 (Error와 Exception을 모두 포함하는 최상위)
     * OutOfMemoryError, StackOverflowError 등 치명적인 에러 포함
     */
    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<ErrorResponse>> handleThrowable(Throwable throwable,
                                                               ServerWebExchange exchange) {

        log.error("Critical error occurred: {}", throwable.getMessage(), throwable);

        ErrorResponse errorResponse = ErrorResponse.of(
                "System error occurred",
                "SYSTEM_ERROR",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
    }
}
