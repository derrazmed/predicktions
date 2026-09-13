package com.ven.predicktions.service.impl;

import com.ven.predicktions.dto.user.UserResponse;
import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.model.User;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.UserService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}