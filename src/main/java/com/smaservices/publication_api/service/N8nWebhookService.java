package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.n8n.N8nPublicationPayload;
import com.smaservices.publication_api.event.PublicationDispatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Service
public class N8nWebhookService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    N8nWebhookService.class
            );

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

        HttpClient httpClient =
                HttpClient
                        .newBuilder()
                        .version(
                                HttpClient.Version.HTTP_1_1
                        )
                        .connectTimeout(
                                Duration.ofSeconds(3)
                        )
                        .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        httpClient
                );

        requestFactory.setReadTimeout(
                Duration.ofSeconds(10)
        );

        this.restClient =
                RestClient
                        .builder()
                        .requestFactory(
                                requestFactory
                        )
                        .build();
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

        LOGGER.info(
                "POST n8n webhook for publication {} to {}",
                event.publicationId(),
                webhookUrl
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