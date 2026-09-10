package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.n8n.N8nExecutionContext;
import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.entity.ThirdPartyAccount;
import com.smaservices.publication_api.entity.enums.AccountStatus;
import com.smaservices.publication_api.entity.enums.DestinationType;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import com.smaservices.publication_api.repository.PublicationRepository;
import com.smaservices.publication_api.repository.ThirdPartyAccountRepository;
import com.smaservices.publication_api.security.EncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smaservices.publication_api.entity.enums.PublicationStatus;

@Service
public class N8nExecutionContextService {

    private final PublicationRepository publicationRepository;

    private final ThirdPartyAccountRepository
            thirdPartyAccountRepository;

    private final EncryptionService encryptionService;

    public N8nExecutionContextService(
            PublicationRepository publicationRepository,
            ThirdPartyAccountRepository thirdPartyAccountRepository,
            EncryptionService encryptionService) {

        this.publicationRepository =
                publicationRepository;

        this.thirdPartyAccountRepository =
                thirdPartyAccountRepository;

        this.encryptionService =
                encryptionService;
    }

    @Transactional(readOnly = true)
    public N8nExecutionContext getContext(
            Long publicationId) {

        Publication publication =
                publicationRepository
                        .findById(publicationId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Publication introuvable."
                                )
                        );

        PublicationStatus status =
                publication.getStatus();

        if (status == PublicationStatus.CANCELLED) {
            throw new IllegalStateException(
                    "La publication a été annulée."
            );
        }

        if (status == PublicationStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "La publication est déjà publiée."
            );
        }

        if (status == PublicationStatus.FAILED) {
            throw new IllegalStateException(
                    "La publication est en échec."
            );
        }
        Long userId =
                publication
                        .getContent()
                        .getUser()
                        .getId();

        DestinationType destination =
                publication.getDestination();

        return switch (destination) {

            case WORDPRESS ->
                    createWordPressContext(
                            publication,
                            userId
                    );

            case LINKEDIN ->
                    throw new IllegalStateException(
                            "L'intégration LinkedIn n'est pas encore configurée."
                    );
        };
    }

    private N8nExecutionContext createWordPressContext(
            Publication publication,
            Long userId) {

        ThirdPartyAccount account =
                thirdPartyAccountRepository
                        .findByUserIdAndType(
                                userId,
                                ThirdPartyType.WORDPRESS
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Aucun compte WordPress connecté."
                                )
                        );

        if (account.getStatus()
                != AccountStatus.CONNECTED) {

            throw new IllegalStateException(
                    "Le compte WordPress n'est pas actif."
            );
        }

        String credentials =
                encryptionService.decrypt(
                        account.getAccessTokenEnc()
                );

        int separatorIndex =
                credentials.indexOf(':');

        if (separatorIndex <= 0) {
            throw new IllegalStateException(
                    "Identifiants WordPress invalides."
            );
        }

        String username =
                credentials.substring(
                        0,
                        separatorIndex
                );

        String appPassword =
                credentials.substring(
                        separatorIndex + 1
                );

        return new N8nExecutionContext(
                publication.getId(),
                publication
                        .getDestination()
                        .name(),

                publication
                        .getContent()
                        .getTitle(),

                publication
                        .getContent()
                        .getBody(),

                publication.getScheduledAt(),

                account.getSiteUrl(),

                username,

                appPassword
        );
    }
}