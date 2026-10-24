package com.quantumledger.app.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TransferRequest (

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "must be a valid 10-digit Indian mobile number")
    String receiverPhone,

    @Pattern(regexp = "^\\+91$", message = "only +91 is supported for now")
    String receiverCountryCode,

    @NotNull
    @Positive(message ="must be greater than zero")
    Long amountMinor
) { }
