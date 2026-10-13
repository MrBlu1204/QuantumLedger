package com.quantumledger.app.auth;

import com.quantumledger.app.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class JwtService {

    public record AccessToken(String value, long expiresInSeconds) {}

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long accessTtlMinutes;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${app.security.jwt.issuer}") String issuer,
                      @Value("${app.security.jwt.access-token-ttl-minutes}") long accessTtlMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public AccessToken createAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTtlMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getUserId()))
                .issuedAt(now)
                .expiresAt(expiry)
                .id(UUID.randomUUID().toString())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();

        return new AccessToken(token, accessTtlMinutes * 60);
    }
}
