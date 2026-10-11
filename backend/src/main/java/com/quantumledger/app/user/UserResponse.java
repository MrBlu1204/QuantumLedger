package com.quantumledger.app.user;

import java.time.Instant;

public record UserResponse(
        Long userId,
        String fullName, String email,
        String countryCode, String phone,
        Instant createTime
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getUserId(), user.getFullName(), user.getEmailId(),
                user.getCountryCode(), user.getPhone(), user.getCreateTime());
    }
}
