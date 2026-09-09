package com.ven.predicktions.service;

import com.ven.predicktions.dto.auth.RegisterRequest;
import com.ven.predicktions.dto.auth.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
}