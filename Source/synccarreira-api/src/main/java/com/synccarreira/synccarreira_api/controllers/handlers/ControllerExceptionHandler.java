package com.synccarreira.synccarreira_api.controllers.handlers;

import com.synccarreira.synccarreira_api.dto.CustomError;
import com.synccarreira.synccarreira_api.dto.ValidationError;
import com.synccarreira.synccarreira_api.services.exceptions.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import tools.jackson.databind.DatabindException;

import java.time.Instant;

@ControllerAdvice
public class ControllerExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<CustomError> resourceNotFound(ResourceNotFoundException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.NOT_FOUND;
		CustomError err = new CustomError(Instant.now(), status.value(), e.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}
	
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<CustomError> entityNotFound(EntityNotFoundException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.NOT_FOUND;
		CustomError err = new CustomError(Instant.now(), status.value(), e.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<CustomError> illegalArgument(IllegalArgumentException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
		CustomError err = new CustomError(Instant.now(), status.value(), e.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<CustomError> dataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.CONFLICT;
		CustomError err = new CustomError(Instant.now(), status.value(), "Operação viola uma restrição de integridade dos dados (ex.: registro duplicado)", request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(DatabaseException.class)
	public ResponseEntity<CustomError> database(DatabaseException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		CustomError err = new CustomError(Instant.now(), status.value(), e.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<CustomError> methodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
		ValidationError err = new ValidationError(Instant.now(), status.value(), "Dados inválidos", request.getRequestURI());
		for (FieldError f : e.getBindingResult().getFieldErrors()) {
			err.addError(f.getField(), f.getDefaultMessage());
		}
		return ResponseEntity.status(status).body(err);
	}

	// Corpo ausente ou JSON com sintaxe inválida é erro na forma da requisição (400).
	// JSON válido com valor incompatível (ex.: opção inexistente de um enum, texto em campo numérico) é 422,
	// como as demais validações de campo. A mensagem é genérica para não expor detalhes internos do Jackson.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<CustomError> httpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
		boolean invalidValue = e.getMostSpecificCause() instanceof DatabindException;
		HttpStatus status = invalidValue ? HttpStatus.UNPROCESSABLE_CONTENT : HttpStatus.BAD_REQUEST;
		String message = invalidValue
				? "Valor inválido em um ou mais campos da requisição."
				: "Corpo da requisição ausente ou com JSON malformado.";
		CustomError err = new CustomError(Instant.now(), status.value(), message, request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}

	// Transação derrubada pelo banco por bloqueio (deadlock ou espera excessiva) mesmo depois das novas tentativas
	@ExceptionHandler(PessimisticLockingFailureException.class)
	public ResponseEntity<CustomError> pessimisticLockingFailure(PessimisticLockingFailureException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.CONFLICT;
		CustomError err = new CustomError(Instant.now(), status.value(),
				"Os dados estavam sendo alterados por outra operação ao mesmo tempo. Tente novamente.", request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<CustomError> illegalState(IllegalStateException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.CONFLICT;
		CustomError err = new CustomError(Instant.now(), status.value(), e.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<CustomError> conflict(ConflictException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.CONFLICT;
		CustomError err = new CustomError(Instant.now(), status.value(), e.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<CustomError> business(BusinessException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
		CustomError err = new CustomError(Instant.now(), status.value(), e.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<CustomError> forbidden(ForbiddenException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.FORBIDDEN;
		CustomError err = new CustomError(Instant.now(), status.value(), e.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(EmailException.class)
	public ResponseEntity<CustomError> email(EmailException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		CustomError err = new CustomError(Instant.now(), status.value(), e.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
	public ResponseEntity<CustomError> notAuthenticated(AuthenticationCredentialsNotFoundException e, HttpServletRequest request) {
		HttpStatus status = HttpStatus.UNAUTHORIZED;
		CustomError err = new CustomError(Instant.now(), status.value(), e.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}
}
