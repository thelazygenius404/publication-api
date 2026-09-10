package com.smaservices.publication_api.dto.publication;

import com.smaservices.publication_api.entity.enums.DestinationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

public class PublicationCreateRequest {

    @NotNull
    private Long contentId;

    @NotEmpty
    private Set<DestinationType> destinations;

    private Instant scheduledAt;

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(
            Long contentId) {
        this.contentId = contentId;
    }

    public Set<DestinationType> getDestinations() {
        return destinations;
    }

    public void setDestinations(
            Set<DestinationType> destinations) {
        this.destinations = destinations;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(
            Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
}