package com.ven.predicktions.service;

import com.ven.predicktions.dto.user.AdminUserPageResponse;
import com.ven.predicktions.dto.user.AdminUserDetailsResponse;

import java.util.UUID;

public interface AdminUserService {

    AdminUserPageResponse getUsers(int page, int size);

    AdminUserDetailsResponse getUser(UUID userId);
}
