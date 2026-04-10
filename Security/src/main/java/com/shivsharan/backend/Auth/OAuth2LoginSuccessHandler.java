package com.shivsharan.backend.Auth;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.shivsharan.backend.Model.User;
import com.shivsharan.backend.Repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtility jwtUtility;

    @Value("${oauth.success-redirect:/}")
    private String successRedirect;

    @Value("${security.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh.expiration:604800000}")
    private long refreshExpirationMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        String username = buildUsername(registrationId, oauthUser);
        String email = extractEmail(oauthUser, registrationId, username);
        String profileImageUrl = extractProfileImageUrl(oauthUser, registrationId);

        User user = resolveOrCreateUser(username, email, profileImageUrl);

        String accessToken = jwtUtility.generateToken(user.getUsername());
        String refreshToken = jwtUtility.generateRefreshToken(user.getUsername());

        addCookie(response, "access_token", accessToken, jwtExpirationMs);
        addCookie(response, "refresh_token", refreshToken, refreshExpirationMs);

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", successRedirect);
    }

    private User resolveOrCreateUser(String username, String email, String profileImageUrl) {
        Optional<User> existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();
            applyProfileImageIfMissing(user, profileImageUrl);
            return userRepository.save(user);
        }

        Optional<User> existingByUsername = userRepository.findByUsername(username);
        if (existingByUsername.isPresent()) {
            User user = existingByUsername.get();
            applyProfileImageIfMissing(user, profileImageUrl);
            return userRepository.save(user);
        }

        String randomPassword = UUID.randomUUID().toString();
        User user = new User(username, passwordEncoder.encode(randomPassword), email);
        user.setVerified(true);
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            user.setProfileImagePath(profileImageUrl.trim());
        }
        return userRepository.save(user);
    }

    private String buildUsername(String registrationId, OAuth2User oauthUser) {
        if ("google".equalsIgnoreCase(registrationId)) {
            String email = oauthUser.getAttribute("email");
            if (email != null && email.contains("@")) {
                return "google_" + email.substring(0, email.indexOf('@'));
            }
        }
        String login = oauthUser.getAttribute("login");
        if (login == null || login.isBlank()) {
            login = oauthUser.getName();
        }
        if (login == null || login.isBlank()) {
            login = "user";
        }
        return registrationId + "_" + login;
    }

    private String extractEmail(OAuth2User oauthUser, String registrationId, String username) {
        Object email = oauthUser.getAttribute("email");
        if (email != null && !email.toString().isBlank()) {
            return email.toString();
        }
        return username + "@users.noreply." + registrationId + ".local";
    }

    private String extractProfileImageUrl(OAuth2User oauthUser, String registrationId) {
        if ("google".equalsIgnoreCase(registrationId)) {
            Object picture = oauthUser.getAttribute("picture");
            if (picture != null && !picture.toString().isBlank()) {
                return picture.toString();
            }
        }
        return null;
    }

    private void applyProfileImageIfMissing(User user, String profileImageUrl) {
        if ((user.getProfileImagePath() == null || user.getProfileImagePath().isBlank())
                && profileImageUrl != null && !profileImageUrl.isBlank()) {
            user.setProfileImagePath(profileImageUrl.trim());
        }
    }

    private void addCookie(HttpServletResponse response, String name, String value, long maxAgeMs) {
        long maxAgeSeconds = Math.max(1, maxAgeMs / 1000);
        StringBuilder cookie = new StringBuilder();
        cookie.append(name).append("=").append(value)
                .append("; Path=/")
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; HttpOnly")
                .append("; SameSite=Lax");
        if (secureCookie) {
            cookie.append("; Secure");
        }
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
