package com.management.event.controller;

import com.management.event.dto.UserSummaryDto;
import com.management.event.entity.AppRole;
import com.management.event.repository.UserRepository;
import com.management.event.exception.ApiResponse;
import com.management.event.security.auth.AuthenticationService;
import com.management.event.security.request.LoginRequest;
import com.management.event.security.request.RegisterRequest;
import com.management.event.security.response.RegisterResponse;
import com.management.event.security.response.UserInfoResponse;
import com.management.event.security.services.UserDetailsImpl;
import com.management.event.security.services.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        if (userRepository.existsByRegNumber(request.getRegNumber())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ApiResponse("Registration number already exists", false));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ApiResponse("Email is already taken", false));
        }

        RegisterResponse registerResponse = authenticationService.register(request);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, registerResponse.getJwtCookie().toString())
                .body(registerResponse.getUserInfo());
    }

    @PostMapping("/signin")
    public ResponseEntity<UserInfoResponse> authenticate(@RequestBody LoginRequest request) {
        return authenticationService.authenticate(request);
    }

    @GetMapping("/username")
    public String username(Authentication authentication) {
        if (authentication != null) {
            return authentication.getName();
        }
        return "Not Found User";
    }

    @GetMapping("/user")
    public ResponseEntity<UserInfoResponse> currentUser(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(item -> item.getAuthority())
                .toList();

        UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                .username(userDetails.getDisplayName())
                .email(userDetails.getEmail())
                .regNumber(userDetails.getRegNumber())
                .roles(roles)
                .build();

        return ResponseEntity.ok(userInfoResponse);
    }

    @GetMapping("/responsible-persons")
    public ResponseEntity<List<UserSummaryDto>> getResponsiblePersons() {
        List<AppRole> excluded = List.of(AppRole.ROLE_USER, AppRole.ROLE_ADMIN);
        List<UserSummaryDto> users = userRepository.findUsersExcludingRoles(excluded)
                .stream()
                .map(u -> new UserSummaryDto(u.getRegNumber(), u.getUserName(), u.getEmail()))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/signout")
    public ResponseEntity<String> signout() {
        return authenticationService.signOut();
    }
}
