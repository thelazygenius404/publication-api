package com.smaservices.publication_api.dto.n8n;

import com.smaservices.publication_api.entity.enums.PublicationStatus;
import jakarta.validation.constraints.NotNull;

public class N8nCallbackRequest {

    @NotNull
    private Long publicationId;

    @NotNull
    private PublicationStatus status;

    private String externalId;

    private String n8nExecutionId;

    private String errorMessage;

    public Long getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(
            Long publicationId) {
        this.publicationId =
                publicationId;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(
            PublicationStatus status) {
        this.status = status;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(
            String externalId) {
        this.externalId =
                externalId;
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
}