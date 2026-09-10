package com.smaservices.publication_api.dto.n8n;

import com.smaservices.publication_api.entity.enums.DestinationType;

import java.time.Instant;

public class N8nPublicationPayload {

    private final Long publicationId;
    private final DestinationType destination;
    private final Instant scheduledAt;

    private final String callbackUrl;
    private final String contextUrl;

    public N8nPublicationPayload(
            Long publicationId,
            DestinationType destination,
            Instant scheduledAt,
            String callbackUrl,
            String contextUrl) {

        this.publicationId =
                publicationId;

        this.destination =
                destination;

        this.scheduledAt =
                scheduledAt;

        this.callbackUrl =
                callbackUrl;

        this.contextUrl =
                contextUrl;
    }

    public Long getPublicationId() {
        return publicationId;
    }

    public DestinationType getDestination() {
        return destination;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getContextUrl() {
        return contextUrl;
    }
}