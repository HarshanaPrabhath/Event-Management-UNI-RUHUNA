package com.management.event.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {


        String path = request.getServletPath();
        if (path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/signin")
                || path.startsWith("/api/auth/signout")

        ) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = parseJwt(request);

        if (token == null || token.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ renamed from userEmail to userRegNumber - we now store regNumber in JWT subject
        final String userRegNumber = jwtService.extractUserName(token);

        if (userRegNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userRegNumber);
            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    public String parseJwt(HttpServletRequest request) {
        String jwtFromCookie = jwtService.getJwtFromCookie(request);
        if (jwtFromCookie != null) {
            return jwtFromCookie;
        }

        String jwtFromHeader = jwtService.getJwtFromHeader(request);
        if (jwtFromHeader != null) {
            return jwtFromHeader;
        }
        return null;
    }
}