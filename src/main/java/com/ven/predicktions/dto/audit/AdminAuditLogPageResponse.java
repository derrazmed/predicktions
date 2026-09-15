package com.ven.predicktions.dto.audit;

import java.util.List;

public record AdminAuditLogPageResponse(List<AdminAuditLogResponse> content, int page,
                                        int size, long totalElements, int totalPages) {}
