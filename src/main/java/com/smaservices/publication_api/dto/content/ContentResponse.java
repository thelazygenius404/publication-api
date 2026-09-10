package com.smaservices.publication_api.dto.content;

import com.smaservices.publication_api.entity.Content;
import com.smaservices.publication_api.entity.enums.ContentStatus;

import java.time.Instant;

public class ContentResponse {

    private final Long id;
    private final String title;
    private final String body;
    private final ContentStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ContentResponse(Content content) {
        this.id = content.getId();
        this.title = content.getTitle();
        this.body = content.getBody();
        this.status = content.getStatus();
        this.createdAt = content.getCreatedAt();
        this.updatedAt = content.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}