package com.smaservices.publication_api.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(
                        name = "idx_audit_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_audit_created",
                        columnList = "createdAt"
                )
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            nullable = false,
            length = 100
    )
    private String action;

    @Column(length = 100)
    private String entityType;

    private Long entityId;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt =
                Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(
            User user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(
            String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(
            String entityType) {
        this.entityType =
                entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(
            Long entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(
            String details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}