package com.auctor.definition.api.rest.exception;

import com.auctor.definition.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler.
 */
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler exceptionHandler;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }
    
    @Test
    void shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input");
        
        // When
        ProblemDetail problemDetail = exceptionHandler.handleIllegalArgumentException(exception);
        
        // Then
        assertNotNull(problemDetail);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        assertEquals("Bad Request", problemDetail.getTitle());
        assertEquals("Invalid input", problemDetail.getDetail());
        assertEquals(URI.create("https://api.auctor.com/errors/bad-request"), problemDetail.getType());
    }
    
    @Test
    void shouldHandleEntityNotFoundException() {
        // Given
        EntityNotFoundException exception = new EntityNotFoundException("Policy", "test-id");
        
        // When
        ProblemDetail problemDetail = exceptionHandler.handleEntityNotFoundException(exception);
        
        // Then
        assertNotNull(problemDetail);
        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        assertEquals("Not Found", problemDetail.getTitle());
        assertTrue(problemDetail.getDetail().contains("Policy"));
        assertTrue(problemDetail.getDetail().contains("test-id"));
        assertEquals(URI.create("https://api.auctor.com/errors/not-found"), problemDetail.getType());
    }
    
    @Test
    void shouldHandleOptimisticLockingFailureException() {
        // Given
        OptimisticLockingFailureException exception = new OptimisticLockingFailureException("Version mismatch");
        
        // When
        ProblemDetail problemDetail = exceptionHandler.handleOptimisticLockingFailureException(exception);
        
        // Then
        assertNotNull(problemDetail);
        assertEquals(HttpStatus.CONFLICT.value(), problemDetail.getStatus());
        assertEquals("Conflict", problemDetail.getTitle());
        assertTrue(problemDetail.getDetail().contains("modified"));
        assertTrue(problemDetail.getDetail().contains("retry"));
        assertEquals(URI.create("https://api.auctor.com/errors/conflict"), problemDetail.getType());
    }
    
    @Test
    void shouldHandleMethodArgumentNotValidException() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("object", "name", "must not be blank");
        FieldError fieldError2 = new FieldError("object", "email", "must be valid email");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        
        // When
        ProblemDetail problemDetail = exceptionHandler.handleMethodArgumentNotValidException(exception);
        
        // Then
        assertNotNull(problemDetail);
        assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        assertEquals("Validation Error", problemDetail.getTitle());
        assertEquals("Validation failed", problemDetail.getDetail());
        assertEquals(URI.create("https://api.auctor.com/errors/validation-error"), problemDetail.getType());
        
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problemDetail.getProperties().get("errors");
        assertNotNull(errors);
        assertEquals(2, errors.size());
        assertEquals("must not be blank", errors.get("name"));
        assertEquals("must be valid email", errors.get("email"));
    }
    
    @Test
    void shouldHandleGenericException() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");
        
        // When
        ProblemDetail problemDetail = exceptionHandler.handleGenericException(exception);
        
        // Then
        assertNotNull(problemDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
        assertEquals("Internal Server Error", problemDetail.getTitle());
        assertEquals("An unexpected error occurred", problemDetail.getDetail());
        assertEquals(URI.create("https://api.auctor.com/errors/internal-server-error"), problemDetail.getType());
    }
    
    @Test
    void shouldHandleNullPointerException() {
        // Given
        NullPointerException exception = new NullPointerException("Null value");
        
        // When
        ProblemDetail problemDetail = exceptionHandler.handleGenericException(exception);
        
        // Then
        assertNotNull(problemDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
    }
    
    @Test
    void shouldHandleEmptyValidationErrors() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        
        // When
        ProblemDetail problemDetail = exceptionHandler.handleMethodArgumentNotValidException(exception);
        
        // Then
        assertNotNull(problemDetail);
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problemDetail.getProperties().get("errors");
        assertTrue(errors.isEmpty());
    }
    
    @Test
    void shouldHandleMultipleFieldErrorsOnSameField() {
        // Given - last error should win
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("object", "name", "first error");
        FieldError error2 = new FieldError("object", "name", "second error");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        
        // When
        ProblemDetail problemDetail = exceptionHandler.handleMethodArgumentNotValidException(exception);
        
        // Then
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problemDetail.getProperties().get("errors");
        assertEquals(1, errors.size());
        assertEquals("second error", errors.get("name"));
    }
    
    @Test
    void shouldIncludeEntityTypeInNotFoundException() {
        // Given
        EntityNotFoundException exception = new EntityNotFoundException("Workflow", "workflow-123");
        
        // When
        ProblemDetail problemDetail = exceptionHandler.handleEntityNotFoundException(exception);
        
        // Then
        assertTrue(problemDetail.getDetail().contains("Workflow"));
    }
    
    @Test
    void shouldIncludeEntityIdInNotFoundException() {
        // Given
        EntityNotFoundException exception = new EntityNotFoundException("Policy", "policy-456");
        
        // When
        ProblemDetail problemDetail = exceptionHandler.handleEntityNotFoundException(exception);
        
        // Then
        assertTrue(problemDetail.getDetail().contains("policy-456"));
    }
}
