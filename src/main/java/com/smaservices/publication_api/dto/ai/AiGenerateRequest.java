package com.smaservices.publication_api.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiGenerateRequest {

    @NotBlank(message = "Le sujet est obligatoire.")
    @Size(
            max = 500,
            message = "Le sujet ne peut pas dépasser 500 caractères."
    )
    private String topic;

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

    public String getTopic() {
        return topic;
    }

    public void setTopic(
            String topic) {
        this.topic = topic;
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