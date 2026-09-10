package com.smaservices.publication_api.entity;

import com.smaservices.publication_api.entity.enums.DestinationType;
import com.smaservices.publication_api.entity.enums.PublicationStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "publications",
        indexes = {
                @Index(
                        name = "idx_publication_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_publication_scheduled_at",
                        columnList = "scheduledAt"
                )
        }
)
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "content_id",
            nullable = false
    )
    private Content content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DestinationType destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationStatus status =
            PublicationStatus.PENDING;

    private Instant scheduledAt;

    private Instant publishedAt;

    @Column(length = 500)
    private String externalId;

    @Column(length = 255)
    private String n8nExecutionId;

    @Column(length = 2000)
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now =
                Instant.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt =
                Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(
            Content content) {
        this.content = content;
    }

    public DestinationType getDestination() {
        return destination;
    }

    public void setDestination(
            DestinationType destination) {
        this.destination = destination;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(
            PublicationStatus status) {
        this.status = status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(
            Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(
            Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(
            String externalId) {
        this.externalId = externalId;
    }

    public String getN8nExecutionId() {
        return n8nExecutionId;
    }

    public void setN8nExecutionId(
            String n8nExecutionId) {
        this.n8nExecutionId =
                n8nExecutionId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(
            String errorMessage) {
        this.errorMessage =
                errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}