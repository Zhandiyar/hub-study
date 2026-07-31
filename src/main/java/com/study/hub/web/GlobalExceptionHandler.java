package com.study.hub.web;

import com.study.hub.exception.AppRegistryUnavailableException;
import com.study.hub.exception.ApplicationGroupNotFoundException;
import com.study.hub.exception.ApplicationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleApplicationNotFound(
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .cacheControl(CacheControl.noStore())
                .body(problemDetailFactory.create(
                        HttpStatus.NOT_FOUND,
                        "Application not found",
                        "Partner application was not found",
                        ProblemType.APPLICATION_NOT_FOUND,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ApplicationGroupNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleApplicationGroupNotFound(
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .cacheControl(CacheControl.noStore())
                .body(problemDetailFactory.create(
                        HttpStatus.NOT_FOUND,
                        "Application group not found",
                        "Partner application group was not found",
                        ProblemType.APPLICATION_GROUP_NOT_FOUND,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AppRegistryUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleAppRegistryUnavailable(
            AppRegistryUnavailableException exception,
            HttpServletRequest request
    ) {
        log.error("App-registry is unavailable", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(problemDetailFactory.create(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Service temporarily unavailable",
                        "Required configuration service is temporarily unavailable",
                        ProblemType.DEPENDENCY_UNAVAILABLE,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                problemDetailFactory.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error",
                        "An unexpected error occurred",
                        ProblemType.INTERNAL,
                        request.getRequestURI()
                )
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, List<String>> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(this::fieldErrorMessage, Collectors.toList())
                ));
        return validationProblem(exception, headers, status, request, errors);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, List<String>> errors = exception.getParameterValidationResults()
                .stream()
                .collect(Collectors.toMap(
                        result -> result.getMethodParameter().getParameterName(),
                        result -> result.getResolvableErrors().stream()
                                .map(error -> error.getDefaultMessage() == null
                                        ? "invalid"
                                        : error.getDefaultMessage())
                                .toList(),
                        (first, second) -> first
                ));
        return validationProblem(exception, headers, status, request, errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail problem = problemDetailFactory.create(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "The request body is missing or contains invalid JSON",
                ProblemType.MALFORMED_REQUEST,
                requestUri(request)
        );
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private ResponseEntity<Object> validationProblem(
            Exception exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request,
            Map<String, List<String>> errors
    ) {
        ProblemDetail problem = problemDetailFactory.create(
                HttpStatus.BAD_REQUEST,
                "Validation error",
                "The request contains invalid data",
                ProblemType.VALIDATION,
                requestUri(request),
                errors
        );
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private String requestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "/";
    }

    private String fieldErrorMessage(FieldError error) {
        return error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage();
    }
}
