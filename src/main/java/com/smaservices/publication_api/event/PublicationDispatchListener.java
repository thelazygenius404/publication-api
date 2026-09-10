package com.smaservices.publication_api.event;

import com.smaservices.publication_api.service.N8nWebhookService;
import com.smaservices.publication_api.service.PublicationDispatchFailureService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PublicationDispatchListener {

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

        try {

            n8nWebhookService.dispatch(
                    event
            );

        } catch (Exception exception) {

            failureService.markDispatchFailed(
                    event.publicationId(),
                    exception.getMessage()
            );
        }
    }
}