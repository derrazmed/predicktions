package com.ven.predicktions.config;

import com.ven.predicktions.service.AdminBootstrapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapService adminBootstrapService;
    private final String bootstrapEmail;
    private final String initialName;
    private final String initialEmail;
    private final String initialPassword;

    public AdminBootstrapRunner(
            AdminBootstrapService adminBootstrapService,
            @Value("${admin.bootstrap.email:}") String bootstrapEmail,
            @Value("${admin.initial.name:}") String initialName,
            @Value("${admin.initial.email:}") String initialEmail,
            @Value("${admin.initial.password:}") String initialPassword
    ) {
        this.adminBootstrapService = adminBootstrapService;
        this.bootstrapEmail = bootstrapEmail;
        this.initialName = initialName;
        this.initialEmail = initialEmail;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isBlank(bootstrapEmail)) {
            adminBootstrapService.promoteByEmail(bootstrapEmail);
        }

        // Create the initial administrator only when it has been explicitly
        // configured. The service re-checks the values and the database state.
        if (isBlank(initialName) && isBlank(initialEmail) && isBlank(initialPassword)) {
            return;
        }

        adminBootstrapService.createInitialAdmin(initialName, initialEmail, initialPassword);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
