package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.n8n.N8nCallbackRequest;
import com.smaservices.publication_api.entity.Content;
import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.PublicationStatus;
import com.smaservices.publication_api.repository.PublicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class N8nCallbackServiceTest {

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private AuditService auditService;

    private N8nCallbackService callbackService;

    private User user;

    @BeforeEach
    void setUp() {

        callbackService =
                new N8nCallbackService(
                        publicationRepository,
                        auditService
                );

        user = new User();
        user.setEmail("user@example.com");
    }

    @Test
    void handle_shouldMovePendingPublicationToProcessing() {

        Publication publication =
                publication(
                        PublicationStatus.PENDING
                );

        publication.setErrorMessage(
                "Ancienne erreur"
        );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        when(
                publicationRepository.save(publication)
        ).thenReturn(publication);

        N8nCallbackRequest request =
                request(
                        PublicationStatus.PROCESSING
                );

        request.setN8nExecutionId(
                "exec-100"
        );

        Publication result =
                callbackService.handle(request);

        assertEquals(
                PublicationStatus.PROCESSING,
                result.getStatus()
        );

        assertEquals(
                "exec-100",
                result.getN8nExecutionId()
        );

        assertNull(
                result.getErrorMessage()
        );

        verify(
                publicationRepository
        ).save(publication);

        verify(
                auditService
        ).log(
                eq(user),
                eq("PUBLICATION_PROCESSING"),
                eq("Publication"),
                nullable(Long.class),
                isNull()
        );
    }

    @Test
    void handle_shouldPublishProcessingPublication() {

        Publication publication =
                publication(
                        PublicationStatus.PROCESSING
                );

        publication.setErrorMessage(
                "Ancienne erreur"
        );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        when(
                publicationRepository.save(publication)
        ).thenReturn(publication);

        N8nCallbackRequest request =
                request(
                        PublicationStatus.PUBLISHED
                );

        request.setExternalId(
                "wp-post-123"
        );

        request.setN8nExecutionId(
                "exec-200"
        );

        Publication result =
                callbackService.handle(request);

        assertEquals(
                PublicationStatus.PUBLISHED,
                result.getStatus()
        );

        assertNotNull(
                result.getPublishedAt()
        );

        assertEquals(
                "wp-post-123",
                result.getExternalId()
        );

        assertEquals(
                "exec-200",
                result.getN8nExecutionId()
        );

        assertNull(
                result.getErrorMessage()
        );

        verify(
                publicationRepository
        ).save(publication);

        verify(
                auditService
        ).log(
                eq(user),
                eq("PUBLICATION_PUBLISHED"),
                eq("Publication"),
                nullable(Long.class),
                isNull()
        );
    }

    @Test
    void handle_shouldMarkProcessingPublicationAsFailed() {

        Publication publication =
                publication(
                        PublicationStatus.PROCESSING
                );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        when(
                publicationRepository.save(publication)
        ).thenReturn(publication);

        N8nCallbackRequest request =
                request(
                        PublicationStatus.FAILED
                );

        request.setErrorMessage(
                "WordPress indisponible"
        );

        Publication result =
                callbackService.handle(request);

        assertEquals(
                PublicationStatus.FAILED,
                result.getStatus()
        );

        assertEquals(
                "WordPress indisponible",
                result.getErrorMessage()
        );

        verify(
                auditService
        ).log(
                eq(user),
                eq("PUBLICATION_FAILED"),
                eq("Publication"),
                nullable(Long.class),
                eq("WordPress indisponible")
        );
    }

    @Test
    void handle_shouldBeIdempotentForDuplicatePublishedCallback() {

        Publication publication =
                publication(
                        PublicationStatus.PUBLISHED
                );

        Instant publishedAt =
                Instant.now()
                        .minus(
                                10,
                                ChronoUnit.MINUTES
                        );

        publication.setPublishedAt(
                publishedAt
        );

        publication.setExternalId(
                "wp-post-123"
        );

        publication.setN8nExecutionId(
                "exec-200"
        );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        N8nCallbackRequest request =
                request(
                        PublicationStatus.PUBLISHED
                );

        request.setExternalId(
                "different-id"
        );

        request.setN8nExecutionId(
                "different-exec"
        );

        Publication result =
                callbackService.handle(request);

        assertSame(
                publication,
                result
        );

        assertEquals(
                publishedAt,
                result.getPublishedAt()
        );

        assertEquals(
                "wp-post-123",
                result.getExternalId()
        );

        assertEquals(
                "exec-200",
                result.getN8nExecutionId()
        );

        verify(
                publicationRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                auditService
        );
    }

    @Test
    void handle_shouldSaveExecutionIdWithoutDuplicateAudit() {

        Publication publication =
                publication(
                        PublicationStatus.PROCESSING
                );

        assertNull(
                publication.getN8nExecutionId()
        );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        when(
                publicationRepository.save(publication)
        ).thenReturn(publication);

        N8nCallbackRequest request =
                request(
                        PublicationStatus.PROCESSING
                );

        request.setN8nExecutionId(
                "exec-late"
        );

        Publication result =
                callbackService.handle(request);

        assertEquals(
                "exec-late",
                result.getN8nExecutionId()
        );

        verify(
                publicationRepository
        ).save(publication);

        verifyNoInteractions(
                auditService
        );
    }

    @Test
    void handle_shouldRejectCallbackForCancelledPublication() {

        Publication publication =
                publication(
                        PublicationStatus.CANCELLED
                );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        N8nCallbackRequest request =
                request(
                        PublicationStatus.PROCESSING
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                callbackService.handle(
                                        request
                                )
                );

        assertEquals(
                "Une publication annulée ne peut plus être modifiée.",
                exception.getMessage()
        );

        verify(
                publicationRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                auditService
        );
    }

    @Test
    void handle_shouldRejectRegressionFromPublishedToFailed() {

        Publication publication =
                publication(
                        PublicationStatus.PUBLISHED
                );

        when(
                publicationRepository.findById(10L)
        ).thenReturn(
                Optional.of(publication)
        );

        N8nCallbackRequest request =
                request(
                        PublicationStatus.FAILED
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                callbackService.handle(
                                        request
                                )
                );

        assertEquals(
                "Une publication déjà publiée ne peut pas changer de statut.",
                exception.getMessage()
        );

        verify(
                publicationRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                auditService
        );
    }

    private N8nCallbackRequest request(
            PublicationStatus status) {

        N8nCallbackRequest request =
                new N8nCallbackRequest();

        request.setPublicationId(10L);
        request.setStatus(status);

        return request;
    }

    private Publication publication(
            PublicationStatus status) {

        Content content =
                new Content();

        content.setTitle("Titre");
        content.setBody("Contenu");
        content.setUser(user);

        Publication publication =
                new Publication();

        publication.setContent(content);
        publication.setStatus(status);

        return publication;
    }
}