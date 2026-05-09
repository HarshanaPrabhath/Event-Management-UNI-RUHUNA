package com.management.event.service;

import com.management.event.entity.AppRole;
import com.management.event.entity.Role;
import com.management.event.entity.User;

public final class RoleUtil {
    private RoleUtil() {}

    public static boolean hasRole(User user, AppRole role) {
        if (user == null || user.getRoles() == null) return false;
        for (Role r : user.getRoles()) {
            if (r != null && r.getRoleName() == role) return true;
        }
        return false;
    }
}

