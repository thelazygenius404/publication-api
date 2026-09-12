package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.n8n.N8nExecutionContext;
import com.smaservices.publication_api.entity.Content;
import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.entity.ThirdPartyAccount;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.AccountStatus;
import com.smaservices.publication_api.entity.enums.DestinationType;
import com.smaservices.publication_api.entity.enums.PublicationStatus;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import com.smaservices.publication_api.exception.ApiException;
import com.smaservices.publication_api.repository.PublicationRepository;
import com.smaservices.publication_api.repository.ThirdPartyAccountRepository;
import com.smaservices.publication_api.security.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class N8nExecutionContextServiceTest {

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private ThirdPartyAccountRepository accountRepository;

    @Mock
    private EncryptionService encryptionService;

    private N8nExecutionContextService service;

    @BeforeEach
    void setUp() {

        service =
                new N8nExecutionContextService(
                        publicationRepository,
                        accountRepository,
                        encryptionService,
                        "202608"
                );
    }

    @Test
    void getContext_shouldBuildLinkedInContext() {

        User user =
                user(7L);

        Content content =
                content(user);

        Publication publication =
                publication(
                        content,
                        DestinationType.LINKEDIN
                );

        ThirdPartyAccount account =
                linkedInAccount();

        account.setExternalAccountId(
                "member-123"
        );

        account.setAccessTokenExpiresAt(
                Instant.now()
                        .plusSeconds(3600)
        );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.LINKEDIN
                )
        ).thenReturn(
                Optional.of(account)
        );

        when(
                encryptionService.decrypt(
                        "encrypted-token"
                )
        ).thenReturn(
                "plain-access-token"
        );

        N8nExecutionContext context =
                service.getContext(10L);

        assertEquals(
                "LINKEDIN",
                context.getDestination()
        );

        assertEquals(
                "plain-access-token",
                context.getCredential()
        );

        assertEquals(
                "urn:li:person:member-123",
                context.getAuthorUrn()
        );

        assertEquals(
                "202608",
                context.getApiVersion()
        );

        assertNull(
                context.getSiteUrl()
        );

        assertNull(
                context.getUsername()
        );
    }

    @Test
    void getContext_shouldRejectMissingLinkedInAccount() {

        User user =
                user(7L);

        Publication publication =
                publication(
                        content(user),
                        DestinationType.LINKEDIN
                );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.LINKEDIN
                )
        ).thenReturn(
                Optional.empty()
        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> service.getContext(10L)
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatus()
        );

        assertEquals(
                "LINKEDIN_ACCOUNT_NOT_CONNECTED",
                exception.getCode()
        );
    }

    @Test
    void getContext_shouldRejectInactiveLinkedInAccount() {

        User user =
                user(7L);

        Publication publication =
                publication(
                        content(user),
                        DestinationType.LINKEDIN
                );

        ThirdPartyAccount account =
                linkedInAccount();

        account.setStatus(
                AccountStatus.EXPIRED
        );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.LINKEDIN
                )
        ).thenReturn(
                Optional.of(account)
        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> service.getContext(10L)
                );

        assertEquals(
                "LINKEDIN_ACCOUNT_INACTIVE",
                exception.getCode()
        );

        verifyNoInteractions(
                encryptionService
        );
    }

    @Test
    void getContext_shouldRejectExpiredLinkedInToken() {

        User user =
                user(7L);

        Publication publication =
                publication(
                        content(user),
                        DestinationType.LINKEDIN
                );

        ThirdPartyAccount account =
                linkedInAccount();

        account.setAccessTokenExpiresAt(
                Instant.now()
                        .minusSeconds(60)
        );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.LINKEDIN
                )
        ).thenReturn(
                Optional.of(account)
        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> service.getContext(10L)
                );

        assertEquals(
                "LINKEDIN_TOKEN_EXPIRED",
                exception.getCode()
        );

        verifyNoInteractions(
                encryptionService
        );

        assertEquals(
                AccountStatus.EXPIRED,
                account.getStatus()
        );

        verify(
                accountRepository
        ).save(account);
    }

    @Test
    void getContext_shouldRejectMissingLinkedInMemberId() {

        User user =
                user(7L);

        Publication publication =
                publication(
                        content(user),
                        DestinationType.LINKEDIN
                );

        ThirdPartyAccount account =
                linkedInAccount();

        account.setExternalAccountId(null);

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        when(
                accountRepository.findByUserIdAndType(
                        7L,
                        ThirdPartyType.LINKEDIN
                )
        ).thenReturn(
                Optional.of(account)
        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> service.getContext(10L)
                );

        assertEquals(
                "LINKEDIN_MEMBER_ID_MISSING",
                exception.getCode()
        );

        verifyNoInteractions(
                encryptionService
        );
    }

    private ThirdPartyAccount linkedInAccount() {

        ThirdPartyAccount account =
                new ThirdPartyAccount();

        account.setType(
                ThirdPartyType.LINKEDIN
        );

        account.setStatus(
                AccountStatus.CONNECTED
        );

        account.setAccessTokenEnc(
                "encrypted-token"
        );

        account.setExternalAccountId(
                "member-123"
        );

        account.setAccessTokenExpiresAt(
                Instant.now()
                        .plusSeconds(3600)
        );

        return account;
    }

    private Publication publication(
            Content content,
            DestinationType destination) {

        Publication publication =
                new Publication();

        publication.setContent(content);

        publication.setDestination(
                destination
        );

        publication.setStatus(
                PublicationStatus.PENDING
        );

        return publication;
    }

    private Content content(
            User user) {

        Content content =
                new Content();

        content.setUser(user);
        content.setTitle("Titre");
        content.setBody("Contenu");

        return content;
    }

    private User user(
            Long id) {

        User user =
                mock(User.class);

        when(
                user.getId()
        ).thenReturn(id);

        return user;
    }
}