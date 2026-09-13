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

    public AdminBootstrapRunner(
            AdminBootstrapService adminBootstrapService,
            @Value("${admin.bootstrap.email:}") String bootstrapEmail
    ) {
        this.adminBootstrapService = adminBootstrapService;
        this.bootstrapEmail = bootstrapEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapEmail.isBlank()) {
            return;
        }

        adminBootstrapService.promoteByEmail(bootstrapEmail);
    }
}
