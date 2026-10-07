package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.n8n.N8nExecutionContext;
import com.smaservices.publication_api.entity.Content;
import com.smaservices.publication_api.entity.ContentMedia;
import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.entity.ThirdPartyAccount;
import com.smaservices.publication_api.entity.enums.AccountStatus;
import com.smaservices.publication_api.entity.enums.DestinationType;
import com.smaservices.publication_api.entity.enums.PublicationStatus;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import com.smaservices.publication_api.exception.ApiException;
import com.smaservices.publication_api.repository.PublicationRepository;
import com.smaservices.publication_api.repository.ThirdPartyAccountRepository;
import com.smaservices.publication_api.security.EncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class N8nExecutionContextService {

    private final PublicationRepository
            publicationRepository;

    private final ThirdPartyAccountRepository
            thirdPartyAccountRepository;

    private final EncryptionService
            encryptionService;

    private final String
            linkedInApiVersion;

    private final String
            publicUrl;

    public N8nExecutionContextService(
            PublicationRepository publicationRepository,
            ThirdPartyAccountRepository thirdPartyAccountRepository,
            EncryptionService encryptionService,

            @Value("${app.linkedin.api-version}")
            String linkedInApiVersion,

            @Value("${app.public-url}")
            String publicUrl) {

        this.publicationRepository =
                publicationRepository;

        this.thirdPartyAccountRepository =
                thirdPartyAccountRepository;

        this.encryptionService =
                encryptionService;

        this.linkedInApiVersion =
                linkedInApiVersion;

        this.publicUrl =
                publicUrl.replaceAll(
                        "/+$",
                        ""
                );
    }

    @Transactional
    public N8nExecutionContext getContext(
            Long publicationId) {

        Publication publication =
                publicationRepository
                        .findById(publicationId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "PUBLICATION_NOT_FOUND",
                                                "Publication introuvable."
                                        )
                        );

        PublicationStatus status =
                publication.getStatus();

        if (
                status ==
                        PublicationStatus.CANCELLED
        ) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PUBLICATION_CANCELLED",
                    "La publication a été annulée."
            );
        }

        if (
                status ==
                        PublicationStatus.PUBLISHED
        ) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PUBLICATION_ALREADY_PUBLISHED",
                    "La publication est déjà publiée."
            );
        }

        if (
                status ==
                        PublicationStatus.FAILED
        ) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PUBLICATION_FAILED",
                    "La publication est en échec."
            );
        }

        Long userId =
                publication
                        .getContent()
                        .getUser()
                        .getId();

        DestinationType destination =
                publication
                        .getDestination();

        return switch (
                destination
                ) {

            case WORDPRESS ->
                    createWordPressContext(
                            publication,
                            userId
                    );

            case LINKEDIN ->
                    createLinkedInContext(
                            publication,
                            userId
                    );
        };
    }

    private N8nExecutionContext
    createLinkedInContext(
            Publication publication,
            Long userId) {

        ThirdPartyAccount account =
                thirdPartyAccountRepository
                        .findByUserIdAndType(
                                userId,
                                ThirdPartyType.LINKEDIN
                        )
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.CONFLICT,
                                                "LINKEDIN_ACCOUNT_NOT_CONNECTED",
                                                "Aucun compte LinkedIn connecté."
                                        )
                        );

        if (
                account.getStatus() !=
                        AccountStatus.CONNECTED
        ) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "LINKEDIN_ACCOUNT_INACTIVE",
                    "Le compte LinkedIn n'est pas actif."
            );
        }

        if (
                account.getAccessTokenExpiresAt()
                        != null
                        &&
                        !account
                                .getAccessTokenExpiresAt()
                                .isAfter(
                                        Instant.now()
                                )
        ) {

            account.setStatus(
                    AccountStatus.EXPIRED
            );

            thirdPartyAccountRepository
                    .save(account);

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "LINKEDIN_TOKEN_EXPIRED",
                    "Le jeton LinkedIn a expiré. Reconnectez le compte."
            );
        }

        String memberId =
                account
                        .getExternalAccountId();

        if (
                memberId == null ||
                        memberId.isBlank()
        ) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "LINKEDIN_MEMBER_ID_MISSING",
                    "L'identifiant du compte LinkedIn est absent."
            );
        }

        String accessToken =
                encryptionService
                        .decrypt(
                                account
                                        .getAccessTokenEnc()
                        );

        String authorUrn =
                "urn:li:person:"
                        + memberId;

        return buildContext(
                publication,
                null,
                null,
                accessToken,
                authorUrn,
                linkedInApiVersion
        );
    }

    private N8nExecutionContext
    createWordPressContext(
            Publication publication,
            Long userId) {

        ThirdPartyAccount account =
                thirdPartyAccountRepository
                        .findByUserIdAndType(
                                userId,
                                ThirdPartyType.WORDPRESS
                        )
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.CONFLICT,
                                                "WORDPRESS_ACCOUNT_NOT_CONNECTED",
                                                "Aucun compte WordPress connecté."
                                        )
                        );

        if (
                account.getStatus() !=
                        AccountStatus.CONNECTED
        ) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "WORDPRESS_ACCOUNT_INACTIVE",
                    "Le compte WordPress n'est pas actif."
            );
        }

        String credentials =
                encryptionService
                        .decrypt(
                                account
                                        .getAccessTokenEnc()
                        );

        int separatorIndex =
                credentials
                        .indexOf(':');

        if (
                separatorIndex <= 0
        ) {
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

        return buildContext(
                publication,
                account.getSiteUrl(),
                username,
                appPassword,
                null,
                null
        );
    }

    private N8nExecutionContext
    buildContext(
            Publication publication,
            String siteUrl,
            String username,
            String credential,
            String authorUrn,
            String apiVersion) {

        Content content =
                publication
                        .getContent();

        ContentMedia media =
                content
                        .getMediaItems()
                        .stream()
                        .findFirst()
                        .orElse(null);

        Long mediaId =
                media != null
                        ? media.getId()
                        : null;

        String mediaContentType =
                media != null
                        ? media.getContentType()
                        : null;

        String mediaOriginalFilename =
                media != null
                        ? media.getOriginalFilename()
                        : null;

        String mediaAltText =
                media != null
                        ? media.getAltText()
                        : null;

        String mediaDownloadUrl =
                media != null
                        ? publicUrl
                        + "/api/n8n/internal/media/"
                        + media.getId()
                        : null;

        return new N8nExecutionContext(
                publication.getId(),

                publication
                        .getDestination()
                        .name(),

                content.getTitle(),

                content.getBody(),

                publication.getScheduledAt(),

                siteUrl,

                username,

                credential,

                authorUrn,

                apiVersion,

                mediaId,

                mediaContentType,

                mediaOriginalFilename,

                mediaAltText,

                mediaDownloadUrl
        );
    }
}