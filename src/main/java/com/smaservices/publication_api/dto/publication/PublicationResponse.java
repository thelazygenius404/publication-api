package com.smaservices.publication_api.dto.publication;

import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.entity.enums.DestinationType;
import com.smaservices.publication_api.entity.enums.PublicationStatus;

import java.time.Instant;

public class PublicationResponse {

    private final Long id;
    private final Long contentId;
    private final String title;
    private final DestinationType destination;
    private final PublicationStatus status;
    private final Instant scheduledAt;
    private final Instant publishedAt;
    private final String externalId;
    private final String errorMessage;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String n8nExecutionId;

    public PublicationResponse(
            Publication publication) {

        this.id =
                publication.getId();

        this.contentId =
                publication
                        .getContent()
                        .getId();

        this.title =
                publication
                        .getContent()
                        .getTitle();

        this.destination =
                publication.getDestination();

        this.status =
                publication.getStatus();

        this.scheduledAt =
                publication.getScheduledAt();

        this.publishedAt =
                publication.getPublishedAt();

        this.externalId =
                publication.getExternalId();

        this.errorMessage =
                publication.getErrorMessage();

        this.createdAt =
                publication.getCreatedAt();

        this.updatedAt =
                publication.getUpdatedAt();

        this.n8nExecutionId =
                publication.getN8nExecutionId();
    }

    public Long getId() {
        return id;
    }

    public Long getContentId() {
        return contentId;
    }

    public String getTitle() {
        return title;
    }

    public DestinationType getDestination() {
        return destination;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public String getN8nExecutionId() {
        return n8nExecutionId;
    }
}