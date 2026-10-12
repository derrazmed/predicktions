package com.ven.predicktions.service;

public interface AdminBootstrapService {

    void promoteByEmail(String email);

    /**
     * Creates the initial administrator account when no administrator exists
     * yet. Configuration values are never defaulted: if any of them is null or
     * blank, no account is created.
     *
     * @param name        the desired username ({@code ADMIN_INITIAL_NAME})
     * @param email       the desired email ({@code ADMIN_INITIAL_EMAIL})
     * @param rawPassword the raw password, hashed before it is stored
     * @return what happened during the attempt
     */
    InitialAdminOutcome createInitialAdmin(String name, String email, String rawPassword);
}
