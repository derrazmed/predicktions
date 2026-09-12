package com.ven.predicktions.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class JoinCodeGenerator {

    static final int LENGTH = 8;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        StringBuilder joinCode = new StringBuilder(LENGTH);

        for (int i = 0; i < LENGTH; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());
            joinCode.append(ALPHABET.charAt(index));
        }

        return joinCode.toString();
    }
}
