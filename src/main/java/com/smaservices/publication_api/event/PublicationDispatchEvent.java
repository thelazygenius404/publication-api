package com.smaservices.publication_api.event;

import com.smaservices.publication_api.entity.enums.DestinationType;

import java.time.Instant;

public record PublicationDispatchEvent(
        Long publicationId,
        DestinationType destination,
        Instant scheduledAt
) {
}