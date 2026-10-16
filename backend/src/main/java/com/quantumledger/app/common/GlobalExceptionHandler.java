package com.quantumledger.app.common;

import com.quantumledger.app.account.AccountNotFoundException;
import com.quantumledger.app.auth.AccountNotActiveException;
import com.quantumledger.app.auth.InvalidCredentialsException;
import com.quantumledger.app.auth.InvalidRefreshTokenException;
import com.quantumledger.app.user.DuplicateUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
    public ResponseEntity<Map<String,Object>> handleIntegrity(DataIntegrityViolationException ex){
        Throwable rootCause = ex.getMostSpecificCause();
        log.error("Database integrity error: {}", rootCause.getMessage(), rootCause);

        //MySQL error code 1062 = duplicate entry on a unique key

        if(rootCause instanceof java.sql.SQLIntegrityConstraintViolationException sql && sql.getErrorCode() == 1062){

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error","DUPLICATE_USER","message","Email or phone already registered"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error","DATA_ERROR","message","Could not register user"));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String,Object>> handleInvalidCredentials(InvalidCredentialsException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error","INVALID_CREDENTIALS","message",ex.getMessage()));
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<Map<String,Object>> handleAccountNotActive(AccountNotActiveException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error","ACCOUNT_NOT_ACTIVE","message",ex.getMessage()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String,Object>> handleInvalidRefresh(InvalidRefreshTokenException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error","INVALID_REFRESH_TOKEN","message",ex.getMessage()));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String,Object>> handleAccountNotFound(AccountNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error","ACCOUNT_NOT_FOUND","message",ex.getMessage()));
    }

}
