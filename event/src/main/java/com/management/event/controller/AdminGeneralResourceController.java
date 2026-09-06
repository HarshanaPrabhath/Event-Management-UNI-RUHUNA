package com.management.event.controller;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.GeneralResourceResponseDto;
import com.management.event.dto.GeneralResourceUpsertRequestDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.User;
import com.management.event.exception.ForbiddenException;
import com.management.event.service.AdminGeneralResourceService;
import com.management.event.service.RoleUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/general-resources")
public class AdminGeneralResourceController {

    private final AuthenticatedUser authenticatedUser;
    private final AdminGeneralResourceService adminGeneralResourceService;

    @PostMapping
    public ResponseEntity<GeneralResourceResponseDto> create(@RequestBody GeneralResourceUpsertRequestDto req) {
        requireAdmin();
        return ResponseEntity.ok(adminGeneralResourceService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GeneralResourceResponseDto> update(@PathVariable Long id, @RequestBody GeneralResourceUpsertRequestDto req) {
        requireAdmin();
        return ResponseEntity.ok(adminGeneralResourceService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        requireAdmin();
        adminGeneralResourceService.delete(id);
        return ResponseEntity.ok("Deleted");
    }

    private void requireAdmin() {
        User user = authenticatedUser.getAuthenticatedUser();
        if (!RoleUtil.hasRole(user, AppRole.ROLE_ADMIN)) {
            throw new ForbiddenException("Admin privileges required");
        }
    }
}
