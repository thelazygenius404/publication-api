package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.n8n.N8nCallbackRequest;
import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.PublicationStatus;
import com.smaservices.publication_api.repository.PublicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class N8nCallbackService {

    private final PublicationRepository publicationRepository;
    private final AuditService auditService;

    public N8nCallbackService(
            PublicationRepository publicationRepository,
            AuditService auditService) {

        this.publicationRepository =
                publicationRepository;

        this.auditService =
                auditService;
    }

    @Transactional
    public Publication handle(
            N8nCallbackRequest request) {

        Publication publication =
                publicationRepository
                        .findById(
                                request.getPublicationId()
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Publication introuvable."
                                )
                        );

        PublicationStatus requestedStatus =
                request.getStatus();

        validateTransition(
                publication,
                requestedStatus
        );

        PublicationStatus currentStatus =
                publication.getStatus();

        if (currentStatus == requestedStatus) {

            if (request.getN8nExecutionId() != null
                    && publication.getN8nExecutionId() == null) {

                publication.setN8nExecutionId(
                        request.getN8nExecutionId()
                );

                return publicationRepository.save(
                        publication
                );
            }

            return publication;
        }

        publication.setStatus(
                requestedStatus
        );

        if (request.getN8nExecutionId() != null) {
            publication.setN8nExecutionId(
                    request.getN8nExecutionId()
            );
        }

        switch (requestedStatus) {

            case PROCESSING -> {
                publication.setErrorMessage(null);
            }

            case PUBLISHED -> {

                if (publication.getPublishedAt() == null) {
                    publication.setPublishedAt(
                            Instant.now()
                    );
                }

                if (request.getExternalId() != null) {
                    publication.setExternalId(
                            request.getExternalId()
                    );
                }

                publication.setErrorMessage(null);
            }

            case FAILED -> {
                publication.setErrorMessage(
                        request.getErrorMessage()
                );
            }

            default ->
                    throw new IllegalArgumentException(
                            "Statut de callback n8n invalide."
                    );
        }

        Publication saved =
                publicationRepository.save(
                        publication
                );

        User user =
                publication
                        .getContent()
                        .getUser();

        auditService.log(
                user,
                "PUBLICATION_" + requestedStatus.name(),
                "Publication",
                saved.getId(),
                request.getErrorMessage()
        );

        return saved;
    }

    private void validateTransition(
            Publication publication,
            PublicationStatus requestedStatus) {

        if (requestedStatus !=
                PublicationStatus.PROCESSING
                &&
                requestedStatus !=
                        PublicationStatus.PUBLISHED
                &&
                requestedStatus !=
                        PublicationStatus.FAILED) {

            throw new IllegalArgumentException(
                    "n8n ne peut définir que PROCESSING, PUBLISHED ou FAILED."
            );
        }

        PublicationStatus current =
                publication.getStatus();

        boolean allowed =
                switch (current) {

                    case PENDING, SCHEDULED ->
                            requestedStatus ==
                                    PublicationStatus.PROCESSING
                                    ||
                                    requestedStatus ==
                                            PublicationStatus.FAILED;

                    case PROCESSING ->
                            requestedStatus ==
                                    PublicationStatus.PROCESSING
                                    ||
                                    requestedStatus ==
                                            PublicationStatus.PUBLISHED
                                    ||
                                    requestedStatus ==
                                            PublicationStatus.FAILED;

                    case PUBLISHED ->
                            requestedStatus ==
                                    PublicationStatus.PUBLISHED;

                    case FAILED ->
                            requestedStatus ==
                                    PublicationStatus.FAILED;

                    case CANCELLED ->
                            false;
                };

        if (allowed) {
            return;
        }

        if (current ==
                PublicationStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Une publication annulée ne peut plus être modifiée."
            );
        }

        if (current ==
                PublicationStatus.PUBLISHED) {

            throw new IllegalStateException(
                    "Une publication déjà publiée ne peut pas changer de statut."
            );
        }

        if (current ==
                PublicationStatus.FAILED) {

            throw new IllegalStateException(
                    "Une publication en échec ne peut plus changer de statut."
            );
        }

        throw new IllegalStateException(
                "Transition de publication invalide : "
                        + current
                        + " -> "
                        + requestedStatus
                        + "."
        );
    }
}