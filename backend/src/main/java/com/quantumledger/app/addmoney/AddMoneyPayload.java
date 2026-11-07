package com.quantumledger.app.addmoney;

import com.quantumledger.app.payment.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddMoneyPayload(

        @NotNull
        @Positive(message = "must be greater than zero")
        Long amountMinor,

        @NotNull
        PaymentMethod method,

        /** UPI handle or bank code, depending on the method. Never store, never logged. */
        @NotBlank
        @Size(max = 100)
        String payerRef
) {
}
