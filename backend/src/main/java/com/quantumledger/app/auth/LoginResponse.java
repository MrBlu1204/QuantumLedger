package com.quantumledger.app.auth;

public record LoginResponse(Long userId, String fullName, String message) {
}
