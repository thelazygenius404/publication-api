package com.smaservices.publication_api.dto.n8n;

import java.time.Instant;

public class N8nExecutionContext {

    private final Long publicationId;
    private final String destination;

    private final String title;
    private final String body;

    private final Instant scheduledAt;

    private final String siteUrl;
    private final String username;
    private final String credential;

    private final String authorUrn;
    private final String apiVersion;

    private final Long mediaId;
    private final String mediaContentType;
    private final String mediaOriginalFilename;
    private final String mediaAltText;
    private final String mediaDownloadUrl;

    public N8nExecutionContext(
            Long publicationId,
            String destination,
            String title,
            String body,
            Instant scheduledAt,
            String siteUrl,
            String username,
            String credential,
            String authorUrn,
            String apiVersion,
            Long mediaId,
            String mediaContentType,
            String mediaOriginalFilename,
            String mediaAltText,
            String mediaDownloadUrl) {

        this.publicationId =
                publicationId;

        this.destination =
                destination;

        this.title =
                title;

        this.body =
                body;

        this.scheduledAt =
                scheduledAt;

        this.siteUrl =
                siteUrl;

        this.username =
                username;

        this.credential =
                credential;

        this.authorUrn =
                authorUrn;

        this.apiVersion =
                apiVersion;

        this.mediaId =
                mediaId;

        this.mediaContentType =
                mediaContentType;

        this.mediaOriginalFilename =
                mediaOriginalFilename;

        this.mediaAltText =
                mediaAltText;

        this.mediaDownloadUrl =
                mediaDownloadUrl;
    }

    public Long getPublicationId() {
        return publicationId;
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

    public String getSiteUrl() {
        return siteUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getCredential() {
        return credential;
    }

    public String getAuthorUrn() {
        return authorUrn;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public String getMediaContentType() {
        return mediaContentType;
    }

    public String getMediaOriginalFilename() {
        return mediaOriginalFilename;
    }

    public String getMediaAltText() {
        return mediaAltText;
    }

    public String getMediaDownloadUrl() {
        return mediaDownloadUrl;
    }
}