package com.bruno.inventory.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Every error leaves the API as RFC 9457 Problem Details, so clients only parse one format. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient stock", ex.getMessage());
    }

    /** Another request changed the same row first (see the @Version fields). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Concurrent update",
                "The resource was modified by another request, please retry");
    }

    /** Safety net for races the service checks cannot see, such as two creates with the same SKU. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", "The request conflicts with existing data");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
        problem.setProperty("errors", errors);
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
