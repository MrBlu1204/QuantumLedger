package com.quantumledger.app.common;

import com.quantumledger.app.transfer.ReceiverNotFoundException;
import com.quantumledger.app.transfer.TransferBusyException;
import com.quantumledger.app.transfer.TransferRejectedException;
import com.quantumledger.app.transfer.TransferRejection;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTransferTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void everyRejectionBecomesA422CarryingItsOwnCode(){
        for(TransferRejection rejection : TransferRejection.values()){
            ResponseEntity<Map<String, Object>> response =
                    handler.handleTransferRejected(new TransferRejectedException(rejection));

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            assertEquals(rejection.name(), response.getBody().get("error"));
        }
    }

    @Test
    void anUnknownReceiverBecomesA404(){
        ResponseEntity<Map<String,Object>> response = handler.handleReceiverNotFound(new ReceiverNotFoundException());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("RECEIVER_NOT_FOUND", response.getBody().get("error"));
    }

    @Test
    void aBusySystemBecomesA530WithARetryAfterHeader(){
        ResponseEntity<Map<String, Object>> response =
                handler.handleTransferBusy(new TransferBusyException(new CannotAcquireLockException("deadlock")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("1", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals("TEMPORARILY_UNAVAILABLE", response.getBody().get("error"));
    }
}
