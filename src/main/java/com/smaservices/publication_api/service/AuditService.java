package com.smaservices.publication_api.service;

import com.smaservices.publication_api.entity.AuditLog;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(
            User user,
            String action,
            String entityType,
            Long entityId,
            String details) {

        AuditLog log = new AuditLog();

        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);

        auditLogRepository.save(log);
    }
}