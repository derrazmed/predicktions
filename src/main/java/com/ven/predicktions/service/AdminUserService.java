package com.ven.predicktions.service;

import com.ven.predicktions.dto.user.AdminUserPageResponse;

public interface AdminUserService {

    AdminUserPageResponse getUsers(int page, int size);
}
