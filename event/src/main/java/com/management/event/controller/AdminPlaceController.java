package com.management.event.controller;

import com.management.event.config.AuthenticatedUser;
import com.management.event.dto.PlaceSendDto;
import com.management.event.dto.PlaceUpsertRequestDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.User;
import com.management.event.exception.ForbiddenException;
import com.management.event.service.AdminPlaceService;
import com.management.event.service.RoleUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/places")
public class AdminPlaceController {

    private final AuthenticatedUser authenticatedUser;
    private final AdminPlaceService adminPlaceService;

    @PostMapping
    public ResponseEntity<PlaceSendDto> create(@RequestBody PlaceUpsertRequestDto req) {
        requireAdmin();
        return ResponseEntity.ok(adminPlaceService.create(req));
    }

    @PutMapping("/{placeId}")
    public ResponseEntity<PlaceSendDto> update(@PathVariable Long placeId, @RequestBody PlaceUpsertRequestDto req) {
        requireAdmin();
        return ResponseEntity.ok(adminPlaceService.update(placeId, req));
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<String> delete(@PathVariable Long placeId) {
        requireAdmin();
        adminPlaceService.delete(placeId);
        return ResponseEntity.ok("Deleted");
    }

    @PostMapping(value = "/{placeId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlaceSendDto> uploadPhoto(@PathVariable Long placeId, @RequestParam("photo") MultipartFile photo) {
        requireAdmin();
        return ResponseEntity.ok(adminPlaceService.uploadPhoto(placeId, photo));
    }

    private void requireAdmin() {
        User user = authenticatedUser.getAuthenticatedUser();
        if (!RoleUtil.hasRole(user, AppRole.ROLE_ADMIN)) {
            throw new ForbiddenException("Admin privileges required");
        }
    }
}
