package com.ven.predicktions.service;

import com.ven.predicktions.model.*;
import com.ven.predicktions.repository.AdminAuditLogRepository;
import com.ven.predicktions.service.impl.AdminAuditServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminAuditServiceTest {
    @Test
    void recordPersistsAuthenticatedAdminAndContext() {
        AdminAuditLogRepository repository = mock(AdminAuditLogRepository.class);
        AdminAuditService service = new AdminAuditServiceImpl(repository);
        UUID admin = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        service.record(admin, AdminAuditAction.POINTS_ADJUSTED,
                AdminAuditTargetType.USER, target, "Adjusted points by +10: correction");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(repository).save(captor.capture());
        AdminAuditLog log = captor.getValue();
        assertThat(log.getAdminId()).isEqualTo(admin);
        assertThat(log.getAction()).isEqualTo(AdminAuditAction.POINTS_ADJUSTED);
        assertThat(log.getTargetType()).isEqualTo(AdminAuditTargetType.USER);
        assertThat(log.getTargetId()).isEqualTo(target);
        assertThat(log.getDetails()).contains("correction");
    }

    @Test
    void recordRejectsMissingDetails() {
        AdminAuditLogRepository repository = mock(AdminAuditLogRepository.class);
        AdminAuditService service = new AdminAuditServiceImpl(repository);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.record(UUID.randomUUID(), AdminAuditAction.USER_ENABLED,
                        AdminAuditTargetType.USER, UUID.randomUUID(), " "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }
}
