package com.smaservices.publication_api.service;

import com.smaservices.publication_api.exception.ApiException;
import com.smaservices.publication_api.security.EncryptionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class LinkedInOAuthStateService {

    private static final String PURPOSE =
            "linkedin";

    private final EncryptionService encryptionService;

    public LinkedInOAuthStateService(
            EncryptionService encryptionService) {

        this.encryptionService =
                encryptionService;
    }

    public String createState(
            Long userId) {

        Instant expiresAt =
                Instant.now()
                        .plus(
                                10,
                                ChronoUnit.MINUTES
                        );

        String rawState =
                PURPOSE
                        + "|"
                        + userId
                        + "|"
                        + expiresAt.getEpochSecond()
                        + "|"
                        + UUID.randomUUID();

        String encryptedState =
                encryptionService.encrypt(
                        rawState
                );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        encryptedState.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }

    public Long validateAndGetUserId(
            String encodedState) {

        if (encodedState == null
                || encodedState.isBlank()) {

            throw invalidState();
        }

        try {

            byte[] decoded =
                    Base64
                            .getUrlDecoder()
                            .decode(
                                    encodedState
                            );

            String encryptedState =
                    new String(
                            decoded,
                            StandardCharsets.UTF_8
                    );

            String rawState =
                    encryptionService.decrypt(
                            encryptedState
                    );

            String[] parts =
                    rawState.split(
                            "\\|",
                            4
                    );

            if (parts.length != 4
                    || !PURPOSE.equals(
                    parts[0]
            )
                    || parts[3].isBlank()) {

                throw invalidState();
            }

            Long userId =
                    Long.parseLong(
                            parts[1]
                    );

            long expiresAt =
                    Long.parseLong(
                            parts[2]
                    );

            if (Instant.now()
                    .isAfter(
                            Instant.ofEpochSecond(
                                    expiresAt
                            )
                    )) {

                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "LINKEDIN_OAUTH_STATE_EXPIRED",
                        "La tentative de connexion LinkedIn a expiré."
                );
            }

            return userId;

        } catch (ApiException exception) {

            throw exception;

        } catch (Exception exception) {

            throw invalidState();
        }
    }

    private ApiException invalidState() {

        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "LINKEDIN_OAUTH_STATE_INVALID",
                "État OAuth LinkedIn invalide."
        );
    }
}