package com.nextrack.audit.domain.repository;

import com.nextrack.audit.domain.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

    List<AuditEvent> findByActorIdOrderByCreatedAtDesc(UUID actorId);
}
