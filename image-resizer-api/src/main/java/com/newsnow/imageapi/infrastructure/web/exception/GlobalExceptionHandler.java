package com.newsnow.imageapi.infrastructure.web.exception;

import com.newsnow.imageapi.application.dto.ErrorResponse;
import com.newsnow.imageapi.domain.port.out.ImageProcessingException;
import com.newsnow.imageapi.domain.port.out.ImageStorageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Manejador para nuestras excepciones específicas de dominio/puerto
    @ExceptionHandler({ImageProcessingException.class, ImageStorageException.class})
    public ResponseEntity<ErrorResponse> handleDomainExceptions(RuntimeException ex, WebRequest request) {
        logger.error("Domain/Port exception occurred: " + ex.getMessage(), ex); // Usa un logger real
        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, // O podríamos mapear a 503 Service Unavailable?
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Manejador para argumentos inválidos (e.g., validación fallida)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.warn("Invalid argument exception: " + ex.getMessage());
        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Manejador genérico para cualquier otra excepción no capturada
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllOtherExceptions(Exception ex, WebRequest request) {
        logger.error("An unexpected error occurred: " + ex.getMessage(), ex);
        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Please try again later.",
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Método Helper (puede ser privado o en una clase utilitaria)
    private ErrorResponse createErrorResponse(HttpStatus status, String message, String path) {
        return new ErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}