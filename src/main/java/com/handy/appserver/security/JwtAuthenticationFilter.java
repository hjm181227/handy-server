package com.handy.appserver.security;

import com.handy.appserver.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        
        log.debug("Request URI: {}", request.getRequestURI());
        log.debug("Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No valid Authorization header found, proceeding without authentication");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractEmail(jwt);
            log.debug("Extracted email from token: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                log.debug("User details loaded: {}", userDetails.getUsername());
                
                if (jwtService.validateToken(jwt, userDetails.getUsername())) {
                    log.debug("Token validated successfully");
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authentication set in SecurityContext");
                } else {
                    log.warn("Token validation failed for user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage(), e);
        }
        
        filterChain.doFilter(request, response);
    }
} 