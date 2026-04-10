package com.shivsharan.backend.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.shivsharan.backend.Model.TokenBlacklist;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {
    
    Optional<TokenBlacklist> findByToken(String token);
    
    boolean existsByToken(String token);
    
    @Transactional
    @Modifying
    @Query("DELETE FROM TokenBlacklist t WHERE t.expiryTime < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
    
    @Query("SELECT COUNT(t) > 0 FROM TokenBlacklist t WHERE t.token = :token AND t.expiryTime > CURRENT_TIMESTAMP")
    boolean isTokenBlacklisted(@Param("token") String token);
}
