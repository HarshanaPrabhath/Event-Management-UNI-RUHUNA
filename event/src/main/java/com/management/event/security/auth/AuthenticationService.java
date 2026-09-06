package com.management.event.security.auth;

import com.management.event.entity.AppRole;
import com.management.event.entity.Role;
import com.management.event.entity.User;
import com.management.event.repository.RoleRepository;
import com.management.event.repository.UserRepository;
import com.management.event.security.config.JwtService;
import com.management.event.security.request.LoginRequest;
import com.management.event.security.request.RegisterRequest;
import com.management.event.security.response.RegisterResponse;
import com.management.event.security.response.UserInfoResponse;
import com.management.event.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;

    public RegisterResponse register(RegisterRequest request) {
        User savedUser = createUser(request);
        ResponseCookie jwtCookie = jwtService.generateJwtCookie(UserDetailsImpl.build(savedUser));
        return new RegisterResponse(toUserInfoResponse(savedUser), jwtCookie);
    }

    // Used by admins to create accounts on behalf of others. Unlike register(), this must NOT
    // issue a JWT cookie - doing so would overwrite the calling admin's own session cookie in
    // the browser, silently logging them out and back in as the newly created (lower-privileged) user.
    public UserInfoResponse registerByAdmin(RegisterRequest request) {
        User savedUser = createUser(request);
        return toUserInfoResponse(savedUser);
    }

    private User createUser(RegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Set<String> strRoles = request.getRole() != null ? request.getRole() : new HashSet<>();
        Set<Role> roles = new HashSet<>();

        if (strRoles.isEmpty()) {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found"));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin" ->
                            roles.add(roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                                    .orElseThrow(() -> new RuntimeException("Error: Role not found: admin")));
                    case "secretary" ->
                            roles.add(roleRepository.findByRoleName(AppRole.ROLE_SECRETARY)
                                    .orElseThrow(() -> new RuntimeException("Error: Role not found: secretary")));
                    case "senior_treasurer", "senior-treasurer", "senior treasurer" ->
                            roles.add(roleRepository.findByRoleName(AppRole.ROLE_SENIOR_TRESURER)
                                    .orElseThrow(() -> new RuntimeException("Error: Role not found: senior_treasurer")));
                    case "lecturer" ->
                            roles.add(roleRepository.findByRoleName(AppRole.ROLE_LECTURER)
                                    .orElseThrow(() -> new RuntimeException("Error: Role not found: lecturer")));
                    case "dean" ->
                            roles.add(roleRepository.findByRoleName(AppRole.ROLE_DEAN)
                                    .orElseThrow(() -> new RuntimeException("Error: Role not found: dean")));
                    case "to", "technical_officer", "technical-officer", "technical officer" ->
                            roles.add(roleRepository.findByRoleName(AppRole.ROLE_TO)
                                    .orElseThrow(() -> new RuntimeException("Error: Role not found: to")));
                    default ->
                            roles.add(roleRepository.findByRoleName(AppRole.ROLE_USER)
                                    .orElseThrow(() -> new RuntimeException("Error: Role not found: user")));
                }
            });
        }

        User user = new User();
        user.setUserName(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRegNumber(request.getRegNumber());
        user.setPassword(encodedPassword);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    private UserInfoResponse toUserInfoResponse(User user) {
        return UserInfoResponse.builder()
                .username(user.getUserName())
                .email(user.getEmail())
                .regNumber(user.getRegNumber())
                .roles(user.getRoles()
                        .stream()
                        .map(role -> role.getRoleName().name())
                        .collect(Collectors.toList()))
                .build();
    }

    public ResponseEntity<UserInfoResponse> authenticate(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getRegNumber(),
                request.getPassword()
        ));

        User user = userRepository.findByRegNumber(request.getRegNumber()).orElseThrow();
        ResponseCookie jwtCookie = jwtService.generateJwtCookie(UserDetailsImpl.build(user));

        UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                .username(user.getUserName())
                .email(user.getEmail())
                .regNumber(user.getRegNumber())
                .roles(user.getRoles().stream()
                        .map(role -> role.getRoleName().name())
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(userInfoResponse);
    }

    public ResponseEntity<String> signOut() {
        ResponseCookie clearedCookie = jwtService.getClearJwtCoockie();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .body("User signed out successfully");
    }
}
