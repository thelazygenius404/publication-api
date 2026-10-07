package com.smaservices.publication_api.dto.media;

import jakarta.validation.constraints.Size;

public class ContentMediaAltTextRequest {

    @Size(
            max = 255,
            message = "Le texte alternatif ne peut pas dépasser 255 caractères."
    )
    private String altText;

    public String getAltText() {
        return altText;
    }

    public void setAltText(
            String altText) {

        this.altText =
                altText;
    }
}