package com.ven.predicktions.config;

import com.ven.predicktions.service.AdminBootstrapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private AdminBootstrapService adminBootstrapService;

    @Test
    void shouldSkipEverythingWhenNothingIsConfigured() {
        new AdminBootstrapRunner(adminBootstrapService, "", "", "", "").run(null);

        verifyNoInteractions(adminBootstrapService);
    }

    @Test
    void shouldNotCreateInitialAdminWhenOnlySomeValuesAreSet() {
        // The runner forwards the (partial) configuration so the service can log
        // a precise warning instead of silently doing nothing.
        new AdminBootstrapRunner(adminBootstrapService, "", "root", "", "pw").run(null);

        verify(adminBootstrapService).createInitialAdmin("root", "", "pw");
    }

    @Test
    void shouldPromoteWhenBootstrapEmailConfigured() {
        new AdminBootstrapRunner(adminBootstrapService, "admin@example.com", "", "", "")
                .run(null);

        verify(adminBootstrapService).promoteByEmail("admin@example.com");
        verify(adminBootstrapService, never()).createInitialAdmin(any(), any(), any());
    }

    @Test
    void shouldCreateInitialAdminWhenConfigured() {
        new AdminBootstrapRunner(adminBootstrapService, "", "root-admin", "root@example.com", "pw")
                .run(null);

        verify(adminBootstrapService).createInitialAdmin("root-admin", "root@example.com", "pw");
        verify(adminBootstrapService, never()).promoteByEmail(any());
    }
}
