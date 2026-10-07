package com.smaservices.publication_api.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "content_media",
        indexes = {
                @Index(
                        name = "idx_content_media_content_id",
                        columnList = "content_id"
                )
        }
)
public class ContentMedia {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
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

    @Column(
            nullable = false,
            length = 255
    )
    private String originalFilename;

    @Column(
            nullable = false,
            unique = true,
            length = 255
    )
    private String storageFilename;

    @Column(
            nullable = false,
            length = 100
    )
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(length = 255)
    private String altText;

    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt =
                    Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(
            Content content) {

        this.content =
                content;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(
            String originalFilename) {

        this.originalFilename =
                originalFilename;
    }

    public String getStorageFilename() {
        return storageFilename;
    }

    public void setStorageFilename(
            String storageFilename) {

        this.storageFilename =
                storageFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(
            String contentType) {

        this.contentType =
                contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(
            long sizeBytes) {

        this.sizeBytes =
                sizeBytes;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(
            String altText) {

        this.altText =
                altText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}