package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.publication.PublicationCreateRequest;
import com.smaservices.publication_api.dto.publication.PublicationResponse;
import com.smaservices.publication_api.entity.Content;
import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.ContentStatus;
import com.smaservices.publication_api.entity.enums.PublicationStatus;
import com.smaservices.publication_api.event.PublicationDispatchEvent;
import com.smaservices.publication_api.repository.ContentRepository;
import com.smaservices.publication_api.repository.PublicationRepository;
import com.smaservices.publication_api.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PublicationService {

    private final PublicationRepository publicationRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public PublicationService(
            PublicationRepository publicationRepository,
            ContentRepository contentRepository,
            UserRepository userRepository,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher) {

        this.publicationRepository =
                publicationRepository;

        this.contentRepository =
                contentRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;

        this.eventPublisher =
                eventPublisher;
    }

    @Transactional
    public List<PublicationResponse> create(
            String userEmail,
            PublicationCreateRequest request) {

        User user =
                findUser(userEmail);

        Content content =
                contentRepository
                        .findByIdAndUserId(
                                request.getContentId(),
                                user.getId()
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Contenu introuvable."
                                )
                        );

        if (content.getStatus()
                != ContentStatus.READY) {

            throw new IllegalStateException(
                    "Le contenu doit être validé avant publication."
            );
        }

        Instant scheduledAt =
                request.getScheduledAt();

        if (scheduledAt != null &&
                !scheduledAt.isAfter(
                        Instant.now()
                )) {

            throw new IllegalArgumentException(
                    "La date planifiée doit être dans le futur."
            );
        }

        PublicationStatus initialStatus =
                scheduledAt == null
                        ? PublicationStatus.PENDING
                        : PublicationStatus.SCHEDULED;

        List<Publication> publications =
                new ArrayList<>();

        for (var destination :
                request.getDestinations()) {

            Publication publication =
                    new Publication();

            publication.setContent(content);

            publication.setDestination(
                    destination
            );

            publication.setScheduledAt(
                    scheduledAt
            );

            publication.setStatus(
                    initialStatus
            );

            publications.add(
                    publicationRepository.save(
                            publication
                    )
            );
        }

        auditService.log(
                user,
                "PUBLICATION_CREATED",
                "Content",
                content.getId(),
                "Destinations : "
                        + request.getDestinations()
        );
        for (Publication publication : publications) {

            eventPublisher.publishEvent(
                    new PublicationDispatchEvent(
                            publication.getId(),
                            publication.getDestination(),
                            publication.getScheduledAt()
                    )
            );
        }
        return publications
                .stream()
                .map(PublicationResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicationResponse> findAll(
            String userEmail) {

        User user =
                findUser(userEmail);

        return publicationRepository
                .findByContentUserIdOrderByCreatedAtDesc(
                        user.getId()
                )
                .stream()
                .map(PublicationResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicationResponse findOne(
            String userEmail,
            Long publicationId) {

        User user =
                findUser(userEmail);

        Publication publication =
                findOwnedPublication(
                        publicationId,
                        user
                );

        return new PublicationResponse(
                publication
        );
    }

    @Transactional
    public PublicationResponse cancel(
            String userEmail,
            Long publicationId) {

        User user =
                findUser(userEmail);

        Publication publication =
                findOwnedPublication(
                        publicationId,
                        user
                );

        if (publication.getStatus()
                != PublicationStatus.PENDING &&
                publication.getStatus()
                        != PublicationStatus.SCHEDULED) {

            throw new IllegalStateException(
                    "Cette publication ne peut plus être annulée."
            );
        }

        publication.setStatus(
                PublicationStatus.CANCELLED
        );

        Publication saved =
                publicationRepository.save(
                        publication
                );

        auditService.log(
                user,
                "PUBLICATION_CANCELLED",
                "Publication",
                saved.getId(),
                "Publication annulée"
        );

        return new PublicationResponse(
                saved
        );
    }

    private User findUser(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Utilisateur non trouvé."
                        )
                );
    }

    private Publication findOwnedPublication(
            Long publicationId,
            User user) {

        return publicationRepository
                .findByIdAndContentUserId(
                        publicationId,
                        user.getId()
                )
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Publication introuvable."
                        )
                );
    }
}