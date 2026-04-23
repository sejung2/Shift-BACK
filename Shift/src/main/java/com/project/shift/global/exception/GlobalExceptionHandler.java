package com.project.shift.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ProblemDetail createProblemDetail(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    // 400 Bad Request
    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException e, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    // 404 Not Found
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    // 409 Conflict
    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException e, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    // 422 Unprocessable Entity (입력값 검증 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return createProblemDetail(HttpStatus.BAD_REQUEST, detail, request);
    }

    // 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    // 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception e, HttpServletRequest request) {
        log.error("[SERVER ERROR] {} {}", request.getMethod(), request.getRequestURI(), e);
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", request);
    }
}
