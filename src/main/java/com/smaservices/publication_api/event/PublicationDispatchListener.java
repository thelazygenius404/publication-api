package com.smaservices.publication_api.event;

import com.smaservices.publication_api.service.N8nWebhookService;
import com.smaservices.publication_api.service.PublicationDispatchFailureService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PublicationDispatchListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    PublicationDispatchListener.class
            );

    private final N8nWebhookService n8nWebhookService;

    private final PublicationDispatchFailureService
            failureService;

    public PublicationDispatchListener(
            N8nWebhookService n8nWebhookService,
            PublicationDispatchFailureService failureService) {

        this.n8nWebhookService =
                n8nWebhookService;

        this.failureService =
                failureService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            PublicationDispatchEvent event) {

        LOGGER.info(
                "AFTER_COMMIT received for publication {} destination {}",
                event.publicationId(),
                event.destination()
        );

        try {

            LOGGER.info(
                    "Dispatching publication {} to n8n",
                    event.publicationId()
            );

            n8nWebhookService.dispatch(
                    event
            );

            LOGGER.info(
                    "Publication {} accepted by n8n webhook",
                    event.publicationId()
            );

        } catch (Exception exception) {

            LOGGER.error(
                    "n8n dispatch failed for publication {}",
                    event.publicationId(),
                    exception
            );

            failureService.markDispatchFailed(
                    event.publicationId(),
                    exception.getMessage()
            );
        }
    }
}