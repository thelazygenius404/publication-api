package com.smaservices.publication_api.repository;

import com.smaservices.publication_api.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    List<AuditLog>
    findTop100ByUserIdOrderByCreatedAtDesc(
            Long userId
    );
}