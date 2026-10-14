package com.quantumledger.app.auth;

import com.quantumledger.app.user.UserResponse;

public record LoginResponse(String tokenType, String accessToken, long expiresInSeconds,
                            String refreshToken,long refreshExpiresInSeconds,
                            UserResponse userResponse) {
}
