package com.management.event.controller;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.UserSummaryDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.User;
import com.management.event.exception.ApiResponse;
import com.management.event.exception.ForbiddenException;
import com.management.event.repository.UserRepository;
import com.management.event.security.auth.AuthenticationService;
import com.management.event.security.request.RegisterRequest;
import com.management.event.security.response.UserInfoResponse;
import com.management.event.service.RoleUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AuthenticatedUser authenticatedUser;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    // Lets admin pick an account as a place/resource responsible person. Optionally narrow to a
    // role (e.g. ?role=TO for technical officers) and/or a name/regNumber/email search term.
    @GetMapping
    public ResponseEntity<List<UserSummaryDto>> listAll(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search
    ) {
        requireAdmin();

        List<User> users;
        if (StringUtils.hasText(role)) {
            AppRole appRole;
            try {
                appRole = AppRole.valueOf(role.trim().toUpperCase().startsWith("ROLE_")
                        ? role.trim().toUpperCase()
                        : "ROLE_" + role.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(List.of());
            }
            users = userRepository.findByRoleName(appRole);
        } else {
            users = userRepository.findAll();
        }

        String term = StringUtils.hasText(search) ? search.trim().toLowerCase() : null;
        List<UserSummaryDto> result = users.stream()
                .filter(u -> term == null
                        || u.getUserName().toLowerCase().contains(term)
                        || u.getRegNumber().toLowerCase().contains(term)
                        || u.getEmail().toLowerCase().contains(term))
                .map(u -> new UserSummaryDto(u.getRegNumber(), u.getUserName(), u.getEmail()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RegisterRequest request) {
        requireAdmin();

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

        UserInfoResponse userInfo = authenticationService.registerByAdmin(request);
        return ResponseEntity.ok(userInfo);
    }

    private void requireAdmin() {
        User user = authenticatedUser.getAuthenticatedUser();
        if (!RoleUtil.hasRole(user, AppRole.ROLE_ADMIN)) {
            throw new ForbiddenException("Admin privileges required");
        }
    }
}
