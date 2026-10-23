package com.devsu.accounts.infrastructure.adapter.in.rest;

import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.devsu.accounts.domain.exception.CustomerNotAvailableException;
import com.devsu.accounts.domain.exception.DomainValidationException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Maps exceptions to RFC 9457 responses (ProblemDetail) without exposing internal details.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * The request is well formed but references a customer that cannot operate, so it cannot be processed.
	 */
	@ExceptionHandler(CustomerNotAvailableException.class)
	ProblemDetail handleCustomerNotAvailable(CustomerNotAvailableException ex) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Customer not available", ex.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation", ex);
		return problem(HttpStatus.CONFLICT, "Data conflict", "The operation violates a uniqueness or integrity constraint");
	}

	@ExceptionHandler(DomainValidationException.class)
	ProblemDetail handleDomainValidation(DomainValidationException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid data", ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unhandled error", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
				"An unexpected error occurred. Please try again later");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid data", "The request contains invalid fields");
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	/**
	 * Constraint violations on query or path parameters, e.g. {@code size=500}.
	 */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getParameterValidationResults().forEach(result -> result.getResolvableErrors()
				.forEach(error -> errors.putIfAbsent(result.getMethodParameter().getParameterName(),
						error.getDefaultMessage())));
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid data", "The request contains invalid parameters");
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "Malformed request", describe(ex)));
	}

	/**
	 * Explains why the JSON body could not be read, naming the offending field so clients can fix the request.
	 */
	private static String describe(HttpMessageNotReadableException ex) {
		JacksonException cause = findJacksonCause(ex);
		if (cause instanceof UnrecognizedPropertyException unknown) {
			return "Unknown field '" + unknown.getPropertyName() + "'";
		}
		if (cause instanceof InvalidFormatException invalid) {
			Class<?> targetType = invalid.getTargetType();
			String message = "Invalid value '" + invalid.getValue() + "' for field '" + fieldPath(invalid) + "'";
			if (targetType != null && targetType.isEnum()) {
				return message + ". Allowed values: " + Arrays.toString(targetType.getEnumConstants());
			}
			if (targetType != null && Temporal.class.isAssignableFrom(targetType)) {
				return message + ". Expected format: yyyy-MM-dd";
			}
			return message;
		}
		if (cause instanceof MismatchedInputException mismatched) {
			return "Invalid type for field '" + fieldPath(mismatched) + "'";
		}
		return "Malformed JSON request body";
	}

	private static JacksonException findJacksonCause(Throwable ex) {
		for (Throwable current = ex; current != null; current = current.getCause()) {
			if (current instanceof JacksonException jacksonException) {
				return jacksonException;
			}
		}
		return null;
	}

	private static String fieldPath(JacksonException ex) {
		return ex.getPath().stream()
				.map(JacksonException.Reference::getPropertyName)
				.filter(Objects::nonNull)
				.collect(Collectors.joining("."));
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
