package com.management.event.config;

import com.management.event.entity.User;
import com.management.event.exception.ApiException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class AuthenticatedUser {

    private final UserRepository userRepository;

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ApiException("Authenticated user not found");
        }

        return userRepository.findByRegNumber(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "regNumber", authentication.getName()));
    }

}
