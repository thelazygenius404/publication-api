package com.smaservices.publication_api.dto.media;

import com.smaservices.publication_api.entity.ContentMedia;

import java.time.Instant;

public class ContentMediaResponse {

    private final Long id;
    private final Long contentId;
    private final String originalFilename;
    private final String contentType;
    private final long sizeBytes;
    private final String altText;
    private final Instant createdAt;
    private final String fileUrl;

    public ContentMediaResponse(
            ContentMedia media) {

        this.id =
                media.getId();

        this.contentId =
                media
                        .getContent()
                        .getId();

        this.originalFilename =
                media.getOriginalFilename();

        this.contentType =
                media.getContentType();

        this.sizeBytes =
                media.getSizeBytes();

        this.altText =
                media.getAltText();

        this.createdAt =
                media.getCreatedAt();

        this.fileUrl =
                "/api/contents/"
                        + contentId
                        + "/media/"
                        + id
                        + "/file";
    }

    public Long getId() {
        return id;
    }

    public Long getContentId() {
        return contentId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getAltText() {
        return altText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getFileUrl() {
        return fileUrl;
    }
}