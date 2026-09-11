package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.content.ContentResponse;
import com.smaservices.publication_api.dto.content.ContentUpdateRequest;
import com.smaservices.publication_api.entity.Content;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.ContentStatus;
import com.smaservices.publication_api.repository.ContentRepository;
import com.smaservices.publication_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private User user;

    private ContentService contentService;

    @BeforeEach
    void setUp() {

        contentService = new ContentService(
                contentRepository,
                userRepository,
                auditService
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
    void update_shouldResetReadyContentToDraft() {

        Content content = content(
                ContentStatus.READY
        );

        when(
                contentRepository.findByIdAndUserId(
                        10L,
                        1L
                )
        ).thenReturn(
                Optional.of(content)
        );

        when(
                contentRepository.save(content)
        ).thenReturn(content);

        ContentUpdateRequest request =
                new ContentUpdateRequest();

        request.setTitle(
                "  Nouveau titre  "
        );

        request.setBody(
                "  Nouveau contenu  "
        );

        ContentResponse response =
                contentService.update(
                        "user@example.com",
                        10L,
                        request
                );

        assertEquals(
                ContentStatus.DRAFT,
                response.getStatus()
        );

        assertEquals(
                "Nouveau titre",
                response.getTitle()
        );

        assertEquals(
                "Nouveau contenu",
                response.getBody()
        );

        verify(
                contentRepository
        ).save(content);

        verify(
                auditService
        ).log(
                eq(user),
                eq("CONTENT_UPDATED"),
                eq("Content"),
                nullable(Long.class),
                eq("Contenu modifié")
        );
    }

    @Test
    void update_shouldKeepDraftContentAsDraft() {

        Content content = content(
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

        when(
                contentRepository.save(content)
        ).thenReturn(content);

        ContentUpdateRequest request =
                new ContentUpdateRequest();

        request.setTitle(
                "Titre modifié"
        );

        request.setBody(
                "Contenu modifié"
        );

        ContentResponse response =
                contentService.update(
                        "user@example.com",
                        10L,
                        request
                );

        assertEquals(
                ContentStatus.DRAFT,
                response.getStatus()
        );

        verify(
                contentRepository
        ).save(content);
    }

    @Test
    void update_shouldRejectArchivedContent() {

        Content content = content(
                ContentStatus.ARCHIVED
        );

        when(
                contentRepository.findByIdAndUserId(
                        10L,
                        1L
                )
        ).thenReturn(
                Optional.of(content)
        );

        ContentUpdateRequest request =
                new ContentUpdateRequest();

        request.setTitle(
                "Titre"
        );

        request.setBody(
                "Contenu"
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> contentService.update(
                                "user@example.com",
                                10L,
                                request
                        )
                );

        assertEquals(
                "Un contenu archivé ne peut pas être modifié.",
                exception.getMessage()
        );

        verify(
                contentRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                auditService
        );
    }

    @Test
    void markReady_shouldChangeDraftToReady() {

        Content content = content(
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

        when(
                contentRepository.save(content)
        ).thenReturn(content);

        ContentResponse response =
                contentService.markReady(
                        "user@example.com",
                        10L
                );

        assertEquals(
                ContentStatus.READY,
                response.getStatus()
        );

        verify(
                contentRepository
        ).save(content);

        verify(
                auditService
        ).log(
                eq(user),
                eq("CONTENT_READY"),
                eq("Content"),
                nullable(Long.class),
                eq(
                        "Contenu validé par l'utilisateur"
                )
        );
    }

    @Test
    void markReady_shouldBeIdempotentWhenAlreadyReady() {

        Content content = content(
                ContentStatus.READY
        );

        when(
                contentRepository.findByIdAndUserId(
                        10L,
                        1L
                )
        ).thenReturn(
                Optional.of(content)
        );

        ContentResponse response =
                contentService.markReady(
                        "user@example.com",
                        10L
                );

        assertEquals(
                ContentStatus.READY,
                response.getStatus()
        );

        verify(
                contentRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                auditService
        );
    }

    @Test
    void markReady_shouldRejectArchivedContent() {

        Content content = content(
                ContentStatus.ARCHIVED
        );

        when(
                contentRepository.findByIdAndUserId(
                        10L,
                        1L
                )
        ).thenReturn(
                Optional.of(content)
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> contentService.markReady(
                                "user@example.com",
                                10L
                        )
                );

        assertEquals(
                "Un contenu archivé ne peut pas être validé.",
                exception.getMessage()
        );

        verify(
                contentRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                auditService
        );
    }

    private Content content(
            ContentStatus status) {

        Content content = new Content();

        content.setTitle(
                "Titre initial"
        );

        content.setBody(
                "Contenu initial"
        );

        content.setStatus(status);

        return content;
    }
}