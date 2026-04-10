package com.shivsharan.backend.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shivsharan.backend.Model.OTP;
import com.shivsharan.backend.Model.User;

public interface OTPRepository extends JpaRepository<OTP, UUID> {
    Optional<OTP> findByUserAndOtpValueAndIsUsedFalse(User user, Integer otpValue);
    
    Optional<OTP> findLatestByUserAndIsUsedFalse(User user);
    
    List<OTP> findByUserAndIsUsedFalse(User user);
    
    @Query("SELECT o FROM OTP o WHERE o.user = :user AND o.isUsed = false AND o.expiryTime > CURRENT_TIMESTAMP ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OTP> findLatestValidOtpForUser(@Param("user") User user);
}
