package com.shivsharan.backend.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shivsharan.backend.Model.OTP;
import com.shivsharan.backend.Model.User;
import com.shivsharan.backend.Repository.OTPRepository;
import com.shivsharan.backend.Repository.UserRepository;
import com.shivsharan.backend.mailUtils.mailService;


@Service
public class OTPService {

    @Autowired
    private mailService mailService;

    @Autowired
    private SecureRandom random;

    @Autowired
    private OTPRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    private static final int OTP_EXPIRY_MINUTES = 5;

    public boolean sendOTP(String username) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return false;
            }

            User user = userOptional.get();
            int otpValue = generateOTP();

            invalidateOldOtps(user);

            OTP otp = OTP.builder()
                    .user(user)
                    .otpValue(otpValue)
                    .expiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                    .isUsed(false)
                    .build();

            otpRepository.save(otp);

            return mailService.sendOTP(user.getEmail(), otpValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean validateOTP(String username, int otpValue) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return false;
            }

            User user = userOptional.get();
            Optional<OTP> otpOptional = otpRepository.findByUserAndOtpValueAndIsUsedFalse(user, otpValue);

            if (otpOptional.isEmpty()) {
                return false;
            }

            OTP otp = otpOptional.get();

            if (otp.isExpired()) {
                return false;
            }

            otp.setIsUsed(true);
            otpRepository.save(otp);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void invalidateOldOtps(User user) {
        try {
            otpRepository.findByUserAndIsUsedFalse(user).forEach(otp -> {
                otp.setIsUsed(true);
                otpRepository.save(otp);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int generateOTP() {
        return 100000 + random.nextInt(900000);
    }

    public boolean validateOTPWithoutUsing(String username, int otpValue) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return false;
            }

            User user = userOptional.get();
            Optional<OTP> otpOptional = otpRepository.findByUserAndOtpValueAndIsUsedFalse(user, otpValue);

            if (otpOptional.isEmpty()) {
                return false;
            }

            OTP otp = otpOptional.get();
            return !otp.isExpired();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
