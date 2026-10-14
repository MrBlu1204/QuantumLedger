package com.quantumledger.app.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.status = 'USED', t.usedAt = :now "
    + "where t.refreshTokenId = :id and t.status = 'ACTIVE'")
    int markUsedIfActive(@Param("id") Long id, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.status = 'REVOKED' "
    + "where t.familyId = :familyId and t.status = 'ACTIVE'")
    int revokeActiveInFamily(@Param("familyId") String familyId);
}
