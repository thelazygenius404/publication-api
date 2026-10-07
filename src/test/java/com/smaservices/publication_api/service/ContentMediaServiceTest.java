package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.media.ContentMediaResponse;
import com.smaservices.publication_api.entity.Content;
import com.smaservices.publication_api.entity.ContentMedia;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.ContentStatus;
import com.smaservices.publication_api.exception.ApiException;
import com.smaservices.publication_api.repository.ContentMediaRepository;
import com.smaservices.publication_api.repository.ContentRepository;
import com.smaservices.publication_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentMediaServiceTest {

    @Mock
    private ContentMediaRepository
            contentMediaRepository;

    @Mock
    private ContentRepository
            contentRepository;

    @Mock
    private UserRepository
            userRepository;

    @Mock
    private MediaStorageService
            mediaStorageService;

    @Mock
    private AuditService
            auditService;

    @Mock
    private User user;

    @Mock
    private Content content;

    private ContentMediaService
            contentMediaService;

    @BeforeEach
    void setUp() {

        contentMediaService =
                new ContentMediaService(
                        contentMediaRepository,
                        contentRepository,
                        userRepository,
                        mediaStorageService,
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
        ).thenReturn(7L);
    }

    @Test
    void upload_shouldPersistMediaAndResetReadyContentToDraft() {

        prepareOwnedContent(
                ContentStatus.READY
        );

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        "image/jpeg",
                        new byte[]{
                                1,
                                2,
                                3
                        }
                );

        when(
                mediaStorageService.store(
                        file
                )
        ).thenReturn(
                new MediaStorageService.StoredMediaFile(
                        "photo.jpg",
                        "generated.jpg",
                        "image/jpeg",
                        3L
                )
        );

        when(
                contentMediaRepository.save(
                        any(ContentMedia.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ContentMediaResponse response =
                contentMediaService.upload(
                        "user@example.com",
                        23L,
                        file,
                        " Illustration "
                );

        assertEquals(
                23L,
                response.getContentId()
        );

        assertEquals(
                "photo.jpg",
                response.getOriginalFilename()
        );

        assertEquals(
                "image/jpeg",
                response.getContentType()
        );

        assertEquals(
                3L,
                response.getSizeBytes()
        );

        assertEquals(
                "Illustration",
                response.getAltText()
        );

        verify(
                content
        ).setStatus(
                ContentStatus.DRAFT
        );

        verify(
                contentRepository
        ).save(content);

        verify(
                auditService
        ).log(
                eq(user),
                eq("CONTENT_MEDIA_ADDED"),
                eq("ContentMedia"),
                nullable(Long.class),
                contains(
                        "23"
                )
        );
    }

    @Test
    void upload_shouldKeepDraftContentAsDraft() {

        prepareOwnedContent(
                ContentStatus.DRAFT
        );

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        "image/jpeg",
                        new byte[]{
                                1,
                                2,
                                3
                        }
                );

        when(
                mediaStorageService.store(
                        file
                )
        ).thenReturn(
                new MediaStorageService.StoredMediaFile(
                        "photo.jpg",
                        "generated.jpg",
                        "image/jpeg",
                        3L
                )
        );

        when(
                contentMediaRepository.save(
                        any(ContentMedia.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        contentMediaService.upload(
                "user@example.com",
                23L,
                file,
                null
        );

        verify(
                content,
                never()
        ).setStatus(
                any()
        );

        verify(
                contentRepository,
                never()
        ).save(content);
    }

    @Test
    void upload_shouldRejectArchivedContent() {

        prepareOwnedContent(
                ContentStatus.ARCHIVED
        );

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        "image/jpeg",
                        new byte[]{
                                1,
                                2,
                                3
                        }
                );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                contentMediaService
                                        .upload(
                                                "user@example.com",
                                                23L,
                                                file,
                                                null
                                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatus()
        );

        assertEquals(
                "CONTENT_ARCHIVED",
                exception.getCode()
        );

        verifyNoInteractions(
                mediaStorageService
        );

        verifyNoInteractions(
                contentMediaRepository
        );
    }

    @Test
    void upload_shouldCleanupStoredFileWhenDatabaseSaveFails() {

        prepareOwnedContent(
                ContentStatus.DRAFT
        );

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        "image/jpeg",
                        new byte[]{
                                1,
                                2,
                                3
                        }
                );

        when(
                mediaStorageService.store(
                        file
                )
        ).thenReturn(
                new MediaStorageService.StoredMediaFile(
                        "photo.jpg",
                        "generated.jpg",
                        "image/jpeg",
                        3L
                )
        );

        when(
                contentMediaRepository.save(
                        any(ContentMedia.class)
                )
        ).thenThrow(
                new RuntimeException(
                        "database error"
                )
        );

        assertThrows(
                RuntimeException.class,
                () ->
                        contentMediaService
                                .upload(
                                        "user@example.com",
                                        23L,
                                        file,
                                        null
                                )
        );

        verify(
                mediaStorageService
        ).deleteQuietly(
                "generated.jpg"
        );
    }

    @Test
    void findAll_shouldReturnMediaOwnedByContentOwner() {

        prepareOwnedContent(
                ContentStatus.DRAFT
        );

        ContentMedia media =
                media(
                        "photo.jpg",
                        "stored.jpg",
                        "image/jpeg",
                        "Description"
                );

        when(
                contentMediaRepository
                        .findByContentIdOrderByCreatedAtAsc(
                                23L
                        )
        ).thenReturn(
                List.of(media)
        );

        List<ContentMediaResponse> response =
                contentMediaService
                        .findAll(
                                "user@example.com",
                                23L
                        );

        assertEquals(
                1,
                response.size()
        );

        assertEquals(
                "photo.jpg",
                response.get(0)
                        .getOriginalFilename()
        );
    }

    @Test
    void updateAltText_shouldUpdateAndResetReadyToDraft() {

        prepareOwnedContent(
                ContentStatus.READY
        );

        ContentMedia media =
                media(
                        "photo.jpg",
                        "stored.jpg",
                        "image/jpeg",
                        "Ancien texte"
                );

        when(
                contentMediaRepository
                        .findByIdAndContentId(
                                1L,
                                23L
                        )
        ).thenReturn(
                Optional.of(media)
        );

        when(
                contentMediaRepository.save(
                        media
                )
        ).thenReturn(
                media
        );

        ContentMediaResponse response =
                contentMediaService
                        .updateAltText(
                                "user@example.com",
                                23L,
                                1L,
                                " Nouveau texte "
                        );

        assertEquals(
                "Nouveau texte",
                response.getAltText()
        );

        assertEquals(
                "Nouveau texte",
                media.getAltText()
        );

        verify(
                content
        ).setStatus(
                ContentStatus.DRAFT
        );

        verify(
                contentRepository
        ).save(content);

        verify(
                auditService
        ).log(
                eq(user),
                eq("CONTENT_MEDIA_UPDATED"),
                eq("ContentMedia"),
                nullable(Long.class),
                anyString()
        );
    }

    @Test
    void updateAltText_shouldRejectTextLongerThan255Characters() {

        prepareOwnedContent(
                ContentStatus.DRAFT
        );

        ContentMedia media =
                media(
                        "photo.jpg",
                        "stored.jpg",
                        "image/jpeg",
                        null
                );

        when(
                contentMediaRepository
                        .findByIdAndContentId(
                                1L,
                                23L
                        )
        ).thenReturn(
                Optional.of(media)
        );

        String longAltText =
                "A".repeat(256);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                contentMediaService
                                        .updateAltText(
                                                "user@example.com",
                                                23L,
                                                1L,
                                                longAltText
                                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatus()
        );

        assertEquals(
                "MEDIA_ALT_TEXT_TOO_LONG",
                exception.getCode()
        );

        verify(
                contentMediaRepository,
                never()
        ).save(any());
    }

    @Test
    void delete_shouldRemoveDatabaseEntityAndPhysicalFile() {

        prepareOwnedContent(
                ContentStatus.READY
        );

        ContentMedia media =
                media(
                        "photo.jpg",
                        "stored.jpg",
                        "image/jpeg",
                        null
                );

        when(
                contentMediaRepository
                        .findByIdAndContentId(
                                1L,
                                23L
                        )
        ).thenReturn(
                Optional.of(media)
        );

        contentMediaService.delete(
                "user@example.com",
                23L,
                1L
        );

        verify(
                contentMediaRepository
        ).delete(media);

        verify(
                contentMediaRepository
        ).flush();

        verify(
                mediaStorageService
        ).delete(
                "stored.jpg"
        );

        verify(
                content
        ).setStatus(
                ContentStatus.DRAFT
        );

        verify(
                contentRepository
        ).save(content);

        verify(
                auditService
        ).log(
                eq(user),
                eq("CONTENT_MEDIA_DELETED"),
                eq("ContentMedia"),
                eq(1L),
                contains(
                        "23"
                )
        );
    }

    @Test
    void delete_shouldRejectArchivedContent() {

        prepareOwnedContent(
                ContentStatus.ARCHIVED
        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                contentMediaService
                                        .delete(
                                                "user@example.com",
                                                23L,
                                                1L
                                        )
                );

        assertEquals(
                "CONTENT_ARCHIVED",
                exception.getCode()
        );

        verify(
                contentMediaRepository,
                never()
        ).delete(any());

        verifyNoInteractions(
                mediaStorageService
        );
    }

    @Test
    void download_shouldReturnStoredResource() {

        prepareOwnedContent(
                ContentStatus.DRAFT
        );

        ContentMedia media =
                media(
                        "photo.jpg",
                        "stored.jpg",
                        "image/jpeg",
                        null
                );

        when(
                contentMediaRepository
                        .findByIdAndContentId(
                                1L,
                                23L
                        )
        ).thenReturn(
                Optional.of(media)
        );

        ByteArrayResource resource =
                new ByteArrayResource(
                        new byte[]{
                                1,
                                2,
                                3
                        }
                );

        when(
                mediaStorageService
                        .loadAsResource(
                                "stored.jpg"
                        )
        ).thenReturn(
                resource
        );

        ContentMediaService.MediaDownload download =
                contentMediaService
                        .download(
                                "user@example.com",
                                23L,
                                1L
                        );

        assertSame(
                resource,
                download.resource()
        );

        assertEquals(
                "photo.jpg",
                download.originalFilename()
        );

        assertEquals(
                "image/jpeg",
                download.contentType()
        );

        assertEquals(
                3L,
                download.sizeBytes()
        );
    }

    @Test
    void operation_shouldRejectContentOwnedByAnotherUser() {

        when(
                contentRepository.findByIdAndUserId(
                        23L,
                        7L
                )
        ).thenReturn(
                Optional.empty()
        );

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        "image/jpeg",
                        new byte[]{
                                1,
                                2,
                                3
                        }
                );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                contentMediaService
                                        .upload(
                                                "user@example.com",
                                                23L,
                                                file,
                                                null
                                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatus()
        );

        assertEquals(
                "CONTENT_NOT_FOUND",
                exception.getCode()
        );

        verifyNoInteractions(
                mediaStorageService
        );
    }

    @Test
    void updateAltText_shouldRejectUnknownMedia() {

        prepareOwnedContent(
                ContentStatus.DRAFT
        );

        when(
                contentMediaRepository
                        .findByIdAndContentId(
                                99L,
                                23L
                        )
        ).thenReturn(
                Optional.empty()
        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                contentMediaService
                                        .updateAltText(
                                                "user@example.com",
                                                23L,
                                                99L,
                                                "Description"
                                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatus()
        );

        assertEquals(
                "MEDIA_NOT_FOUND",
                exception.getCode()
        );
    }

    private void prepareOwnedContent(
            ContentStatus status) {

        when(
                contentRepository.findByIdAndUserId(
                        23L,
                        7L
                )
        ).thenReturn(
                Optional.of(content)
        );

        lenient()
                .when(
                        content.getId()
                )
                .thenReturn(
                        23L
                );

        lenient()
                .when(
                        content.getStatus()
                )
                .thenReturn(
                        status
                );
    }

    private ContentMedia media(
            String originalFilename,
            String storageFilename,
            String contentType,
            String altText) {

        ContentMedia media =
                new ContentMedia();

        media.setContent(
                content
        );

        media.setOriginalFilename(
                originalFilename
        );

        media.setStorageFilename(
                storageFilename
        );

        media.setContentType(
                contentType
        );

        media.setSizeBytes(
                3L
        );

        media.setAltText(
                altText
        );

        return media;
    }
}