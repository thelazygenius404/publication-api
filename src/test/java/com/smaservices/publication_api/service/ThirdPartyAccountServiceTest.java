package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.account.AccountConnectionState;
import com.smaservices.publication_api.dto.account.ThirdPartyAccountsResponse;
import com.smaservices.publication_api.entity.ThirdPartyAccount;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.AccountStatus;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import com.smaservices.publication_api.repository.ThirdPartyAccountRepository;
import com.smaservices.publication_api.repository.UserRepository;
import com.smaservices.publication_api.security.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThirdPartyAccountServiceTest {

    @Mock
    private ThirdPartyAccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    private ThirdPartyAccountService service;

    private User user;

    @BeforeEach
    void setUp() {

        service =
                new ThirdPartyAccountService(
                        accountRepository,
                        userRepository,
                        encryptionService
                );

        user =
                mock(User.class);

        when(
                user.getId()
        ).thenReturn(
                7L
        );
    }

    @Test
    void getAccountsStatus_shouldReturnNotConnectedWhenNoAccountsExist() {

        when(
                userRepository.findByEmail(
                        "user@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.WORDPRESS
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.LINKEDIN
                )
        ).thenReturn(
                Optional.empty()
        );

        ThirdPartyAccountsResponse response =
                service.getAccountsStatus(
                        "user@example.com"
                );

        assertFalse(
                response.wordpress()
                        .connected()
        );

        assertEquals(
                AccountConnectionState.NOT_CONNECTED,
                response.wordpress()
                        .status()
        );

        assertFalse(
                response.linkedin()
                        .connected()
        );

        assertEquals(
                AccountConnectionState.NOT_CONNECTED,
                response.linkedin()
                        .status()
        );
    }

    @Test
    void getAccountsStatus_shouldExposeSafeConnectedMetadata() {

        ThirdPartyAccount wordpress =
                new ThirdPartyAccount();

        wordpress.setType(
                ThirdPartyType.WORDPRESS
        );

        wordpress.setStatus(
                AccountStatus.CONNECTED
        );

        wordpress.setSiteUrl(
                "https://example.com"
        );

        wordpress.setAccessTokenEnc(
                "encrypted-wordpress-secret"
        );

        ThirdPartyAccount linkedin =
                new ThirdPartyAccount();

        linkedin.setType(
                ThirdPartyType.LINKEDIN
        );

        linkedin.setStatus(
                AccountStatus.CONNECTED
        );

        linkedin.setAccessTokenEnc(
                "encrypted-linkedin-token"
        );

        Instant expiresAt =
                Instant.now()
                        .plusSeconds(3600);

        linkedin.setAccessTokenExpiresAt(
                expiresAt
        );

        when(
                userRepository.findByEmail(
                        "user@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.WORDPRESS
                )
        ).thenReturn(
                Optional.of(wordpress)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.LINKEDIN
                )
        ).thenReturn(
                Optional.of(linkedin)
        );

        ThirdPartyAccountsResponse response =
                service.getAccountsStatus(
                        "user@example.com"
                );

        assertTrue(
                response.wordpress()
                        .connected()
        );

        assertEquals(
                "https://example.com",
                response.wordpress()
                        .siteUrl()
        );

        assertTrue(
                response.linkedin()
                        .connected()
        );

        assertEquals(
                expiresAt,
                response.linkedin()
                        .accessTokenExpiresAt()
        );

        assertNull(
                response.linkedin()
                        .siteUrl()
        );
    }

    @Test
    void getAccountsStatus_shouldMarkExpiredLinkedInAccount() {

        ThirdPartyAccount linkedin =
                new ThirdPartyAccount();

        linkedin.setType(
                ThirdPartyType.LINKEDIN
        );

        linkedin.setStatus(
                AccountStatus.CONNECTED
        );

        linkedin.setAccessTokenEnc(
                "encrypted-token"
        );

        linkedin.setAccessTokenExpiresAt(
                Instant.now()
                        .minusSeconds(60)
        );

        when(
                userRepository.findByEmail(
                        "user@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.WORDPRESS
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.LINKEDIN
                )
        ).thenReturn(
                Optional.of(linkedin)
        );

        ThirdPartyAccountsResponse response =
                service.getAccountsStatus(
                        "user@example.com"
                );

        assertEquals(
                AccountStatus.EXPIRED,
                linkedin.getStatus()
        );

        assertFalse(
                response.linkedin()
                        .connected()
        );

        assertEquals(
                AccountConnectionState.EXPIRED,
                response.linkedin()
                        .status()
        );

        verify(
                accountRepository
        ).save(linkedin);
    }

    @Test
    void disconnectLinkedInAccount_shouldDeleteAccount() {

        ThirdPartyAccount linkedin =
                new ThirdPartyAccount();

        when(
                userRepository.findByEmail(
                        "user@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.LINKEDIN
                )
        ).thenReturn(
                Optional.of(linkedin)
        );

        service.disconnectLinkedInAccount(
                "user@example.com"
        );

        verify(
                accountRepository
        ).delete(linkedin);
    }

    @Test
    void disconnectWordPressAccount_shouldDeleteAccount() {

        ThirdPartyAccount wordpress =
                new ThirdPartyAccount();

        when(
                userRepository.findByEmail(
                        "user@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.WORDPRESS
                )
        ).thenReturn(
                Optional.of(wordpress)
        );

        service.disconnectWordPressAccount(
                "user@example.com"
        );

        verify(
                accountRepository
        ).delete(wordpress);
    }
}