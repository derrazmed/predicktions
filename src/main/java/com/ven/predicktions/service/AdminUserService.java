package com.ven.predicktions.service;

import com.ven.predicktions.dto.user.AdminUserPageResponse;
import com.ven.predicktions.dto.user.AdminUserDetailsResponse;
import com.ven.predicktions.dto.user.AdminUserResponse;
import com.ven.predicktions.model.Role;

import java.util.UUID;

public interface AdminUserService {

    AdminUserPageResponse getUsers(int page, int size);

    AdminUserDetailsResponse getUser(UUID userId);

    AdminUserResponse changeUserRole(UUID userId, Role role);
}
