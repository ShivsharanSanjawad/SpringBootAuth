package com.shivsharan.backend.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.shivsharan.backend.Auth.JwtUtility;
import com.shivsharan.backend.DTO.JwtTokenResponse;
import com.shivsharan.backend.DTO.LoginRequest;
import com.shivsharan.backend.DTO.RefreshTokenRequest;
import com.shivsharan.backend.DTO.SendOtpRequest;
import com.shivsharan.backend.DTO.UserDTO;
import com.shivsharan.backend.DTO.VerifyOtpRequest;
import com.shivsharan.backend.Model.TokenBlacklist;
import com.shivsharan.backend.Model.User;
import com.shivsharan.backend.Repository.TokenBlacklistRepository;
import com.shivsharan.backend.Repository.UserRepository;
import com.shivsharan.backend.Service.OTPService;
import com.shivsharan.backend.Service.UserService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api")
public class UserController {

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtility jwtUtility;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OTPService otpService;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    // ==================== PUBLIC ENDPOINTS ====================

    @PostMapping("/signUp")
    public ResponseEntity<?> signUp(@RequestBody UserDTO userDTO) {
        ResponseEntity<?> createResponse = userService.createUser(userDTO);
        if (!createResponse.getStatusCode().equals(HttpStatus.CREATED)) {
            return createResponse;
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    userDTO.getUsername(),
                    userDTO.getPassword()
                )
            );

            if (authentication.isAuthenticated()) {
            String accessToken = jwtUtility.generateToken(userDTO.getUsername());
            String refreshToken = jwtUtility.generateRefreshToken(userDTO.getUsername());

            JwtTokenResponse tokenResponse = JwtTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600000L)
                .username(userDTO.getUsername())
                .build();

            return ResponseEntity.ok(tokenResponse);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new HashMap<String, String>() {{
                put("error", "SignUp failed");
                }});

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new HashMap<String, String>() {{
                        put("error", "SignUp failed");
                    }});
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            if (authentication.isAuthenticated()) {
                String accessToken = jwtUtility.generateToken(loginRequest.getUsername());
                String refreshToken = jwtUtility.generateRefreshToken(loginRequest.getUsername());

                JwtTokenResponse tokenResponse = JwtTokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .expiresIn(3600000L)
                        .username(loginRequest.getUsername())
                        .build();

                return ResponseEntity.ok(tokenResponse);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new HashMap<String, String>() {{
                        put("error", "Login failed");
                    }});

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new HashMap<String, String>() {{
                        put("error", "Login failed");
                    }});
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest sendOtpRequest) {
        try {
            boolean sent = otpService.sendOTP(sendOtpRequest.getUsername());
            
            if (sent) {
                return ResponseEntity.ok()
                    .body(new HashMap<String, String>() {{
                        put("message", "OTP sent successfully to registered email");
                    }});
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to send OTP. User not found or email service unavailable.");
                    }});
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new HashMap<String, String>() {{
                    put("error", "Failed to send OTP: " + e.getMessage());
                }});
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        try {
            boolean isValid = otpService.validateOTP(verifyOtpRequest.getUsername(), verifyOtpRequest.getOtp());
            
            if (isValid) {
                String accessToken = jwtUtility.generateToken(verifyOtpRequest.getUsername());
                String refreshToken = jwtUtility.generateRefreshToken(verifyOtpRequest.getUsername());

                JwtTokenResponse tokenResponse = JwtTokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(3600000L)
                    .username(verifyOtpRequest.getUsername())
                    .build();

                return ResponseEntity.ok(tokenResponse);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new HashMap<String, String>() {{
                        put("error", "Invalid or expired OTP");
                    }});
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new HashMap<String, String>() {{
                    put("error", "OTP verification failed: " + e.getMessage());
                }});
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            String refreshToken = refreshTokenRequest.getRefreshToken();

            if (jwtUtility.validateToken(refreshToken)) {
                String username = jwtUtility.getUsernameFromToken(refreshToken);

                String newAccessToken = jwtUtility.generateToken(username);
                String newRefreshToken = jwtUtility.generateRefreshToken(username);

                JwtTokenResponse tokenResponse = JwtTokenResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .expiresIn(3600000L)
                        .username(username)
                        .build();

                return ResponseEntity.ok(tokenResponse);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid refresh token");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new HashMap<String, String>() {{
                        put("error", "Token refresh failed");
                    }});
        }
    }

    // ==================== USER ENDPOINTS (Authenticated Users) ====================

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String username = jwtUtility.getUsernameFromToken(token);
            LocalDateTime expiryTime = jwtUtility.getExpirationDateFromToken(token);

            if (username != null && expiryTime != null) {
                TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                    .token(token)
                    .username(username)
                    .expiryTime(expiryTime)
                    .build();
                
                tokenBlacklistRepository.save(blacklistedToken);

                return ResponseEntity.ok()
                    .body(new HashMap<String, String>() {{
                        put("message", "Logged out successfully");
                    }});
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new HashMap<String, String>() {{
                    put("error", "Invalid token");
                }});

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new HashMap<String, String>() {{
                    put("error", "Logout failed: " + e.getMessage());
                }});
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String username = jwtUtility.getUsernameFromToken(token);

            if (username != null) {
                Optional<User> userOptional = userRepository.findByUsername(username);
                User user = userOptional.orElse(null);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                return ResponseEntity.ok()
                        .body(new HashMap<String, Object>() {{
                            put("username", username);
                            put("authorities", userDetails.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.toList()));
                            if (user != null) {
                                put("description", user.getDescription());
                                put("college", user.getCollege() != null ? user.getCollege().name() : null);
                                put("gender", user.getGender() != null ? user.getGender().name() : null);
                                put("profileImageUrl", buildProfileImageUrl(user.getProfileImagePath()));
                            }
                        }});
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid token");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Failed to retrieve user info");
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String gender,
            @RequestPart(required = false) MultipartFile image
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User updated = userService.updateProfile(userOptional.get(), description, college, gender, image);
            return ResponseEntity.ok()
                    .body(new HashMap<String, Object>() {{
                        put("message", "Profile updated");
                        put("description", updated.getDescription());
                        put("college", updated.getCollege() != null ? updated.getCollege().name() : null);
                        put("gender", updated.getGender() != null ? updated.getGender().name() : null);
                        put("profileImageUrl", buildProfileImageUrl(updated.getProfileImagePath()));
                    }});
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Profile update failed");
                    }});
        }
    }

    private String buildProfileImageUrl(String profileImagePath) {
        if (profileImagePath == null || profileImagePath.isBlank()) {
            return null;
        }
        String trimmed = profileImagePath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String baseUrl = publicBaseUrl != null ? publicBaseUrl.trim() : "";
        if (!baseUrl.isEmpty()) {
            return baseUrl.replaceAll("/+$", "") + "/" + profileImagePath;
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/")
                .path(profileImagePath)
                .toUriString();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User user = userOptional.get();

            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new HashMap<String, String>() {{
                            put("error", "Old password is incorrect");
                        }});
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return ResponseEntity.ok()
                    .body(new HashMap<String, String>() {{
                        put("message", "Password changed successfully");
                    }});

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to change password: " + e.getMessage());
                    }});
        }
    }

    // ==================== ADMIN ENDPOINTS (Admin Only) ====================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            return ResponseEntity.ok()
                    .body(new HashMap<String, Object>() {{
                        put("totalUsers", users.size());
                        put("users", users.stream().map(u -> new HashMap<String, Object>() {{
                            put("id", u.getId());
                            put("username", u.getUsername());
                            put("role", u.getRole());
                        }}).collect(Collectors.toList()));
                    }});

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to retrieve users: " + e.getMessage());
                    }});
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User user = userOptional.get();
            return ResponseEntity.ok()
                    .body(new HashMap<String, Object>() {{
                        put("id", user.getId());
                        put("username", user.getUsername());
                        put("role", user.getRole());
                    }});

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to retrieve user: " + e.getMessage());
                    }});
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/{username}/promote")
    public ResponseEntity<?> promoteUserToAdmin(@PathVariable String username) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User user = userOptional.get();
            user.setRole(com.shivsharan.backend.Model.AcessLevel.ADMIN);
            userRepository.save(user);

            return ResponseEntity.ok()
                    .body(new HashMap<String, String>() {{
                        put("message", "User promoted to ADMIN successfully");
                        put("username", username);
                    }});

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to promote user: " + e.getMessage());
                    }});
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/{username}/demote")
    public ResponseEntity<?> demoteAdminToUser(@PathVariable String username) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User user = userOptional.get();
            user.setRole(com.shivsharan.backend.Model.AcessLevel.USER);
            userRepository.save(user);

            return ResponseEntity.ok()
                    .body(new HashMap<String, String>() {{
                        put("message", "User demoted to USER successfully");
                        put("username", username);
                    }});

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to demote user: " + e.getMessage());
                    }});
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/users/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User user = userOptional.get();
            userRepository.delete(user);

            return ResponseEntity.ok()
                    .body(new HashMap<String, String>() {{
                        put("message", "User deleted successfully");
                        put("username", username);
                    }});

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to delete user: " + e.getMessage());
                    }});
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/statistics")
    public ResponseEntity<?> getAdminStatistics() {
        try {
            List<User> allUsers = userRepository.findAll();
            long adminCount = allUsers.stream()
                    .filter(u -> u.getRole() == com.shivsharan.backend.Model.AcessLevel.ADMIN)
                    .count();
            long userCount = allUsers.stream()
                    .filter(u -> u.getRole() == com.shivsharan.backend.Model.AcessLevel.USER)
                    .count();

            return ResponseEntity.ok()
                    .body(new HashMap<String, Object>() {{
                        put("totalUsers", allUsers.size());
                        put("adminCount", adminCount);
                        put("userCount", userCount);
                    }});

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to retrieve statistics: " + e.getMessage());
                    }});
        }
    }
}
