package com.quantumledger.app.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Size(max = 5)
        String title,

        @NotBlank @Size(max = 50)
        String firstName,

        @Size(max = 50)
        String lastName,

        @NotBlank @Email @Size(max = 225)
        String email,

        @Pattern(regexp = "^\\+91$", message = "only +91 is supported for now")
        String countryCode,

        @NotBlank
        @Pattern( regexp = "^[6-9]\\d{9}$",
                message = "must be a valid 10-digit Indian mobile number")
        String phone,

        @NotBlank @Size(min = 9, max = 72)
        String password
) {
}
