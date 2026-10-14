package com.quantumledger.app.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    public record IssuedRefreshToken(String value, long expiresInSeconds) {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final long ttlDays;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.security.jwt.refresh-token-ttl-days}") long ttlDays) {
        this.repository = repository;
        this.ttlDays = ttlDays;
    }

    /** Called at the time of login: starts a branch-new token family. */
    @Transactional
    public IssuedRefreshToken issueForNewSession(Long userId) {
        return issue(userId, UUID.randomUUID().toString());
    }

    /** Also used later for rotation, which keeps the same family. */
    @Transactional
    public IssuedRefreshToken issue(Long userId, String familyId){
        String rawToken = generateRawToken();
        Instant expiry = Instant.now().plus(ttlDays, ChronoUnit.DAYS);

        repository.save(new RefreshToken(userId, hash(rawToken), familyId, expiry));

        return new IssuedRefreshToken(rawToken, ttlDays * 24 * 60 * 60);
    }

    static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }


}
