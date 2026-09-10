package com.smaservices.publication_api.dto.n8n;

import com.smaservices.publication_api.entity.Publication;

import java.time.Instant;

public class N8nPublicationPayload {

    private final Long publicationId;
    private final Long contentId;
    private final String destination;
    private final String title;
    private final String body;
    private final Instant scheduledAt;
    private final String callbackUrl;

    public N8nPublicationPayload(
            Publication publication,
            String callbackUrl) {

        this.publicationId =
                publication.getId();

        this.contentId =
                publication
                        .getContent()
                        .getId();

        this.destination =
                publication
                        .getDestination()
                        .name();

        this.title =
                publication
                        .getContent()
                        .getTitle();

        this.body =
                publication
                        .getContent()
                        .getBody();

        this.scheduledAt =
                publication.getScheduledAt();

        this.callbackUrl =
                callbackUrl;
    }

    public Long getPublicationId() {
        return publicationId;
    }

    public Long getContentId() {
        return contentId;
    }

    public String getDestination() {
        return destination;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }
}