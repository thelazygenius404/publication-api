package com.smaservices.publication_api.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiImproveRequest {

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(
            max = 255,
            message = "Le titre ne peut pas dépasser 255 caractères."
    )
    private String title;

    @NotBlank(message = "Le contenu est obligatoire.")
    private String body;

    @Size(
            max = 500,
            message = "L'instruction ne peut pas dépasser 500 caractères."
    )
    private String instruction;

    @Size(
            max = 50,
            message = "Le ton ne peut pas dépasser 50 caractères."
    )
    private String tone;

    @Size(
            max = 20,
            message = "La langue ne peut pas dépasser 20 caractères."
    )
    private String language;

    public String getTitle() {
        return title;
    }

    public void setTitle(
            String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(
            String body) {
        this.body = body;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(
            String instruction) {
        this.instruction = instruction;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(
            String tone) {
        this.tone = tone;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(
            String language) {
        this.language = language;
    }
}