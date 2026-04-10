package com.shivsharan.backend.Auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class userNamePasswordFilter extends OncePerRequestFilter {

    @Autowired
    private AuthenticationManager authManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && (path.equals("/api/signUp") || path.equals("/api/refresh-token") || path.equals("/api/trySending")
                || path.startsWith("/api/signUp") || path.startsWith("/api/refresh-token") || path.startsWith("/api/trySending"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = request.getHeader("username");
        String password = request.getHeader("password");

        if (username == null || username.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication auth = new CustomUsernamePasswordAuthentication(username, password);
            authManager.authenticate(auth);
        } catch (AuthenticationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authentication failed: " + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
