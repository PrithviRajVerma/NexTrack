package com.nextrack.audit.application.service;

import com.nextrack.audit.api.dto.response.AuditEventResponse;
import com.nextrack.audit.domain.entity.AuditEvent;
import com.nextrack.audit.domain.repository.AuditEventRepository;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@NamedInterface
@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEvent recordEvent(
            UUID actorId,
            String entityType,
            UUID entityId,
            String action,
            String beforePayload,
            String afterPayload,
            String traceId
    ) {
        AuditEvent event = new AuditEvent(
                actorId,
                entityType,
                entityId,
                action,
                beforePayload,
                afterPayload,
                traceId
        );
        return auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAuditHistory(String entityType, UUID entityId) {
        return auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(event -> new AuditEventResponse(
                        event.getId(),
                        event.getActorId(),
                        event.getEntityType(),
                        event.getEntityId(),
                        event.getAction(),
                        event.getBeforePayload(),
                        event.getAfterPayload(),
                        event.getTraceId(),
                        event.getCreatedAt()
                ))
                .toList();
    }
}
