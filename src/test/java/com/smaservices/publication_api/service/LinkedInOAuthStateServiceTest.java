package com.smaservices.publication_api.service;

import com.smaservices.publication_api.exception.ApiException;
import com.smaservices.publication_api.security.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class LinkedInOAuthStateServiceTest {

    private EncryptionService encryptionService;
    private LinkedInOAuthStateService stateService;

    @BeforeEach
    void setUp() {

        encryptionService =
                new EncryptionService(
                        "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
                );

        stateService =
                new LinkedInOAuthStateService(
                        encryptionService
                );
    }

    @Test
    void createState_shouldBeUrlSafeAndResolveOriginalUser() {

        String state =
                stateService.createState(42L);

        assertNotNull(state);
        assertFalse(state.isBlank());

        assertFalse(state.contains("+"));
        assertFalse(state.contains("/"));
        assertFalse(state.contains("="));

        Long userId =
                stateService.validateAndGetUserId(
                        state
                );

        assertEquals(
                42L,
                userId
        );
    }

    @Test
    void validate_shouldRejectBlankState() {

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                stateService
                                        .validateAndGetUserId(
                                                " "
                                        )
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatus()
        );

        assertEquals(
                "LINKEDIN_OAUTH_STATE_INVALID",
                exception.getCode()
        );
    }

    @Test
    void validate_shouldRejectTamperedState() {

        String state =
                stateService.createState(42L);

        /*
         * Decode the outer Base64URL layer first.
         * This gives us the encrypted state string.
         */
        String encryptedState =
                new String(
                        Base64
                                .getUrlDecoder()
                                .decode(state),
                        StandardCharsets.UTF_8
                );

        /*
         * Modify a character in the middle of the encrypted payload.
         * Unlike changing the final Base64 character, this guarantees
         * that the encrypted value itself is modified.
         */
        int index =
                encryptedState.length() / 2;

        char current =
                encryptedState.charAt(index);

        char replacement =
                current == 'A'
                        ? 'B'
                        : 'A';

        String tamperedEncryptedState =
                encryptedState.substring(
                        0,
                        index
                )
                        + replacement
                        + encryptedState.substring(
                        index + 1
                );

        /*
         * Rebuild a syntactically valid OAuth state.
         */
        String tampered =
                Base64
                        .getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                tamperedEncryptedState
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        )
                        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                stateService
                                        .validateAndGetUserId(
                                                tampered
                                        )
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatus()
        );

        assertEquals(
                "LINKEDIN_OAUTH_STATE_INVALID",
                exception.getCode()
        );
    }

    @Test
    void validate_shouldRejectExpiredState() {

        long expiredAt =
                Instant.now()
                        .minusSeconds(60)
                        .getEpochSecond();

        String rawState =
                "linkedin|42|"
                        + expiredAt
                        + "|test-nonce";

        String encrypted =
                encryptionService.encrypt(
                        rawState
                );

        String encoded =
                Base64
                        .getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                encrypted.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                stateService
                                        .validateAndGetUserId(
                                                encoded
                                        )
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatus()
        );

        assertEquals(
                "LINKEDIN_OAUTH_STATE_EXPIRED",
                exception.getCode()
        );
    }
}