package com.management.event.controller;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.club.AdminClubUpsertRequestDto;
import com.management.event.dto.club.ClubResponseDto;
import com.management.event.dto.club.ClubSecretaryResponseDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.User;
import com.management.event.exception.ForbiddenException;
import com.management.event.service.AdminSecretaryService;
import com.management.event.service.ClubAdminService;
import com.management.event.service.RoleUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/clubs")
public class AdminClubController {

    private final AuthenticatedUser authenticatedUser;
    private final ClubAdminService clubAdminService;
    private final AdminSecretaryService adminSecretaryService;

    @PostMapping
    public ResponseEntity<ClubResponseDto> create(@RequestBody AdminClubUpsertRequestDto req) {
        requireAdmin();
        return ResponseEntity.ok(clubAdminService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<ClubResponseDto>> listAll() {
        requireAdmin();
        return ResponseEntity.ok(clubAdminService.listAll());
    }

    @GetMapping("/secretaries")
    public ResponseEntity<List<ClubSecretaryResponseDto>> listAllSecretaries() {
        requireAdmin();
        return ResponseEntity.ok(adminSecretaryService.listAllSecretaries());
    }

    @PutMapping("/{clubId}")
    public ResponseEntity<ClubResponseDto> update(@PathVariable Long clubId, @RequestBody AdminClubUpsertRequestDto req) {
        requireAdmin();
        return ResponseEntity.ok(clubAdminService.update(clubId, req));
    }

    @DeleteMapping("/{clubId}")
    public ResponseEntity<String> delete(@PathVariable Long clubId) {
        requireAdmin();
        clubAdminService.delete(clubId);
        return ResponseEntity.ok("Deleted");
    }

    private void requireAdmin() {
        User user = authenticatedUser.getAuthenticatedUser();
        if (!RoleUtil.hasRole(user, AppRole.ROLE_ADMIN)) {
            throw new ForbiddenException("Admin privileges required");
        }
    }
}
