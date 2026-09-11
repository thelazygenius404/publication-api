package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.publication.PublicationCreateRequest;
import com.smaservices.publication_api.dto.publication.PublicationResponse;
import com.smaservices.publication_api.entity.Content;
import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.ContentStatus;
import com.smaservices.publication_api.entity.enums.DestinationType;
import com.smaservices.publication_api.entity.enums.PublicationStatus;
import com.smaservices.publication_api.event.PublicationDispatchEvent;
import com.smaservices.publication_api.repository.ContentRepository;
import com.smaservices.publication_api.repository.PublicationRepository;
import com.smaservices.publication_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private User user;

    private PublicationService publicationService;

    @BeforeEach
    void setUp() {

        publicationService =
                new PublicationService(
                        publicationRepository,
                        contentRepository,
                        userRepository,
                        auditService,
                        eventPublisher
                );

        when(
                userRepository.findByEmail(
                        "user@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                user.getId()
        ).thenReturn(1L);
    }

    @Test
    void create_shouldCreateOnePublicationAndOneEventPerDestination() {

        Content content =
                readyContent();

        when(
                contentRepository.findByIdAndUserId(
                        10L,
                        1L
                )
        ).thenReturn(
                Optional.of(content)
        );

        when(
                publicationRepository.save(
                        any(Publication.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        PublicationCreateRequest request =
                new PublicationCreateRequest();

        request.setContentId(10L);

        request.setDestinations(
                Set.of(
                        DestinationType.WORDPRESS,
                        DestinationType.LINKEDIN
                )
        );

        List<PublicationResponse> responses =
                publicationService.create(
                        "user@example.com",
                        request
                );

        assertEquals(
                2,
                responses.size()
        );

        assertTrue(
                responses.stream()
                        .allMatch(
                                response ->
                                        response.getStatus()
                                                == PublicationStatus.PENDING
                        )
        );

        verify(
                publicationRepository,
                times(2)
        ).save(
                any(Publication.class)
        );

        verify(
                auditService,
                times(2)
        ).log(
                eq(user),
                eq("PUBLICATION_CREATED"),
                eq("Publication"),
                nullable(Long.class),
                anyString()
        );

        ArgumentCaptor<PublicationDispatchEvent>
                eventCaptor =
                ArgumentCaptor.forClass(
                        PublicationDispatchEvent.class
                );

        verify(
                eventPublisher,
                times(2)
        ).publishEvent(
                eventCaptor.capture()
        );

        List<PublicationDispatchEvent> events =
                eventCaptor.getAllValues();

        assertEquals(
                2,
                events.size()
        );

        assertEquals(
                Set.of(
                        DestinationType.WORDPRESS,
                        DestinationType.LINKEDIN
                ),
                events.stream()
                        .map(
                                PublicationDispatchEvent::destination
                        )
                        .collect(
                                java.util.stream.Collectors.toSet()
                        )
        );
    }

    @Test
    void create_shouldUseScheduledStatusForFuturePublication() {

        Content content =
                readyContent();

        when(
                contentRepository.findByIdAndUserId(
                        10L,
                        1L
                )
        ).thenReturn(
                Optional.of(content)
        );

        when(
                publicationRepository.save(
                        any(Publication.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        Instant scheduledAt =
                Instant.now()
                        .plus(
                                1,
                                ChronoUnit.HOURS
                        );

        PublicationCreateRequest request =
                new PublicationCreateRequest();

        request.setContentId(10L);

        request.setDestinations(
                Set.of(
                        DestinationType.WORDPRESS
                )
        );

        request.setScheduledAt(
                scheduledAt
        );

        List<PublicationResponse> responses =
                publicationService.create(
                        "user@example.com",
                        request
                );

        assertEquals(
                1,
                responses.size()
        );

        assertEquals(
                PublicationStatus.SCHEDULED,
                responses.get(0).getStatus()
        );

        assertEquals(
                scheduledAt,
                responses.get(0).getScheduledAt()
        );

        verify(
                eventPublisher,
                times(1)
        ).publishEvent(
                any(
                        PublicationDispatchEvent.class
                )
        );
    }

    @Test
    void create_shouldRejectContentThatIsNotReady() {

        Content content =
                new Content();

        content.setTitle(
                "Brouillon"
        );

        content.setBody(
                "Contenu"
        );

        content.setStatus(
                ContentStatus.DRAFT
        );

        when(
                contentRepository.findByIdAndUserId(
                        10L,
                        1L
                )
        ).thenReturn(
                Optional.of(content)
        );

        PublicationCreateRequest request =
                new PublicationCreateRequest();

        request.setContentId(10L);

        request.setDestinations(
                Set.of(
                        DestinationType.WORDPRESS
                )
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                publicationService.create(
                                        "user@example.com",
                                        request
                                )
                );

        assertEquals(
                "Le contenu doit être validé avant publication.",
                exception.getMessage()
        );

        verify(
                publicationRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void create_shouldRejectScheduledDateInPast() {

        Content content =
                readyContent();

        when(
                contentRepository.findByIdAndUserId(
                        10L,
                        1L
                )
        ).thenReturn(
                Optional.of(content)
        );

        PublicationCreateRequest request =
                new PublicationCreateRequest();

        request.setContentId(10L);

        request.setDestinations(
                Set.of(
                        DestinationType.WORDPRESS
                )
        );

        request.setScheduledAt(
                Instant.now()
                        .minus(
                                1,
                                ChronoUnit.HOURS
                        )
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                publicationService.create(
                                        "user@example.com",
                                        request
                                )
                );

        assertEquals(
                "La date planifiée doit être dans le futur.",
                exception.getMessage()
        );

        verify(
                publicationRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                eventPublisher
        );
    }

    private Content readyContent() {

        Content content =
                new Content();

        content.setTitle(
                "Publication prête"
        );

        content.setBody(
                "Contenu prêt"
        );

        content.setStatus(
                ContentStatus.READY
        );

        return content;
    }
}