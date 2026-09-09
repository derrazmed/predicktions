package com.ven.predicktions.service;

import com.ven.predicktions.dto.user.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(UUID userId);
}