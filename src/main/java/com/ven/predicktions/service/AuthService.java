package com.ven.predicktions.service;

import com.ven.predicktions.dto.auth.LoginRequest;
import com.ven.predicktions.dto.auth.LoginResponse;
import com.ven.predicktions.dto.auth.RegisterRequest;
import com.ven.predicktions.dto.auth.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);

}