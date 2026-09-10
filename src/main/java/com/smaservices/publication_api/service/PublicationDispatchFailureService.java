package com.smaservices.publication_api.service;

import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.PublicationStatus;
import com.smaservices.publication_api.repository.PublicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class PublicationDispatchFailureService {

    private final PublicationRepository publicationRepository;
    private final AuditService auditService;

    public PublicationDispatchFailureService(
            PublicationRepository publicationRepository,
            AuditService auditService) {

        this.publicationRepository =
                publicationRepository;

        this.auditService =
                auditService;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void markDispatchFailed(
            Long publicationId,
            String technicalMessage) {

        Publication publication =
                publicationRepository
                        .findById(publicationId)
                        .orElse(null);

        if (publication == null) {
            return;
        }

        if (publication.getStatus()
                != PublicationStatus.PENDING
                &&
                publication.getStatus()
                        != PublicationStatus.SCHEDULED) {

            return;
        }

        publication.setStatus(
                PublicationStatus.FAILED
        );

        publication.setErrorMessage(
                "Impossible de transmettre la publication à n8n."
        );

        publicationRepository.save(
                publication
        );

        User user =
                publication
                        .getContent()
                        .getUser();

        auditService.log(
                user,
                "N8N_DISPATCH_FAILED",
                "Publication",
                publication.getId(),
                sanitizeMessage(
                        technicalMessage
                )
        );
    }

    private String sanitizeMessage(
            String message) {

        if (message == null ||
                message.isBlank()) {

            return "Erreur de communication avec n8n.";
        }

        return message.length() > 1000
                ? message.substring(0, 1000)
                : message;
    }
}