package com.ven.predicktions.service.impl;

import com.ven.predicktions.exception.ResourceNotFoundException;
import com.ven.predicktions.repository.UserRepository;
import com.ven.predicktions.service.AdminBootstrapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapServiceImpl implements AdminBootstrapService {

    private final UserRepository userRepository;

    public AdminBootstrapServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void promoteByEmail(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cannot bootstrap admin: user not found for email"
                        )
                )
                .promoteToAdmin();
    }
}
