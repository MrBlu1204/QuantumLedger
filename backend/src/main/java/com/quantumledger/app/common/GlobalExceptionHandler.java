package com.quantumledger.app.common;

import com.quantumledger.app.user.DuplicateUserException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex){
        Map<String,Object> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fields.put(e.getField(),e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("error","VALIDATION_FAILED", "fields",fields));

    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<Map<String,Object>> handleDuplicate(DuplicateUserException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error","DUPLICATE_USER","message",ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,Object>> handleDataIntegrity(DataIntegrityViolationException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error","DUPLICATE_USER","message","Email or phone is already registered"));
    }
}
