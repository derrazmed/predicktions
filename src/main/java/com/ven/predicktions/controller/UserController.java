package com.ven.predicktions.controller;

import com.ven.predicktions.dto.user.UserResponse;
import com.ven.predicktions.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
                userService.getCurrentUser(userId)
        );
    }
}