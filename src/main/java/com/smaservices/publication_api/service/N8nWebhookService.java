package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.n8n.N8nPublicationPayload;
import com.smaservices.publication_api.event.PublicationDispatchEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class N8nWebhookService {

    private final RestClient restClient;

    private final String webhookUrl;
    private final String sharedSecret;
    private final String publicUrl;

    public N8nWebhookService(
            @Value("${app.n8n.webhook-url}")
            String webhookUrl,

            @Value("${app.n8n.shared-secret}")
            String sharedSecret,

            @Value("${app.public-url}")
            String publicUrl) {

        this.webhookUrl =
                webhookUrl;

        this.sharedSecret =
                sharedSecret;

        this.publicUrl =
                publicUrl;

        this.restClient =
                RestClient.create();
    }

    public void dispatch(
            PublicationDispatchEvent event) {

        String callbackUrl =
                publicUrl
                        + "/api/n8n/callback";

        String contextUrl =
                publicUrl
                        + "/api/n8n/internal/publications/"
                        + event.publicationId()
                        + "/context";

        N8nPublicationPayload payload =
                new N8nPublicationPayload(
                        event.publicationId(),
                        event.destination(),
                        event.scheduledAt(),
                        callbackUrl,
                        contextUrl
                );

        restClient
                .post()
                .uri(webhookUrl)
                .header(
                        "X-N8N-SECRET",
                        sharedSecret
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}