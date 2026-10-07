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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ContentMediaService {

    private final ContentMediaRepository
            contentMediaRepository;

    private final ContentRepository
            contentRepository;

    private final UserRepository
            userRepository;

    private final MediaStorageService
            mediaStorageService;

    private final AuditService
            auditService;

    public ContentMediaService(
            ContentMediaRepository contentMediaRepository,
            ContentRepository contentRepository,
            UserRepository userRepository,
            MediaStorageService mediaStorageService,
            AuditService auditService) {

        this.contentMediaRepository =
                contentMediaRepository;

        this.contentRepository =
                contentRepository;

        this.userRepository =
                userRepository;

        this.mediaStorageService =
                mediaStorageService;

        this.auditService =
                auditService;
    }

    @Transactional
    public ContentMediaResponse upload(
            String userEmail,
            Long contentId,
            MultipartFile file,
            String altText) {

        User user =
                findUser(
                        userEmail
                );

        Content content =
                findOwnedContent(
                        contentId,
                        user
                );

        ensureEditable(
                content
        );

        String normalizedAltText =
                normalizeAltText(
                        altText
                );

        MediaStorageService.StoredMediaFile
                storedFile =
                mediaStorageService.store(
                        file
                );

        try {

            ContentMedia media =
                    new ContentMedia();

            media.setContent(
                    content
            );

            media.setOriginalFilename(
                    storedFile.originalFilename()
            );

            media.setStorageFilename(
                    storedFile.storageFilename()
            );

            media.setContentType(
                    storedFile.contentType()
            );

            media.setSizeBytes(
                    storedFile.sizeBytes()
            );

            media.setAltText(
                    normalizedAltText
            );

            ContentMedia saved =
                    contentMediaRepository
                            .save(media);

            resetContentToDraft(
                    content
            );

            auditService.log(
                    user,
                    "CONTENT_MEDIA_ADDED",
                    "ContentMedia",
                    saved.getId(),
                    "Média ajouté au contenu #"
                            + content.getId()
            );

            return new ContentMediaResponse(
                    saved
            );

        } catch (RuntimeException exception) {

            mediaStorageService
                    .deleteQuietly(
                            storedFile
                                    .storageFilename()
                    );

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ContentMediaResponse> findAll(
            String userEmail,
            Long contentId) {

        User user =
                findUser(
                        userEmail
                );

        findOwnedContent(
                contentId,
                user
        );

        return contentMediaRepository
                .findByContentIdOrderByCreatedAtAsc(
                        contentId
                )
                .stream()
                .map(
                        ContentMediaResponse::new
                )
                .toList();
    }

    @Transactional
    public ContentMediaResponse updateAltText(
            String userEmail,
            Long contentId,
            Long mediaId,
            String altText) {

        User user =
                findUser(
                        userEmail
                );

        Content content =
                findOwnedContent(
                        contentId,
                        user
                );

        ensureEditable(
                content
        );

        ContentMedia media =
                findMedia(
                        mediaId,
                        contentId
                );

        media.setAltText(
                normalizeAltText(
                        altText
                )
        );

        ContentMedia saved =
                contentMediaRepository
                        .save(media);

        resetContentToDraft(
                content
        );

        auditService.log(
                user,
                "CONTENT_MEDIA_UPDATED",
                "ContentMedia",
                saved.getId(),
                "Texte alternatif du média modifié"
        );

        return new ContentMediaResponse(
                saved
        );
    }

    @Transactional
    public void delete(
            String userEmail,
            Long contentId,
            Long mediaId) {

        User user =
                findUser(
                        userEmail
                );

        Content content =
                findOwnedContent(
                        contentId,
                        user
                );

        ensureEditable(
                content
        );

        ContentMedia media =
                findMedia(
                        mediaId,
                        contentId
                );

        String storageFilename =
                media.getStorageFilename();

        contentMediaRepository
                .delete(media);

        contentMediaRepository
                .flush();

        mediaStorageService
                .delete(
                        storageFilename
                );

        resetContentToDraft(
                content
        );

        auditService.log(
                user,
                "CONTENT_MEDIA_DELETED",
                "ContentMedia",
                mediaId,
                "Média supprimé du contenu #"
                        + contentId
        );
    }

    @Transactional(readOnly = true)
    public MediaDownload download(
            String userEmail,
            Long contentId,
            Long mediaId) {

        User user =
                findUser(
                        userEmail
                );

        findOwnedContent(
                contentId,
                user
        );

        ContentMedia media =
                findMedia(
                        mediaId,
                        contentId
                );

        Resource resource =
                mediaStorageService
                        .loadAsResource(
                                media.getStorageFilename()
                        );

        return new MediaDownload(
                resource,
                media.getOriginalFilename(),
                media.getContentType(),
                media.getSizeBytes()
        );
    }

    private void resetContentToDraft(
            Content content) {

        if (content.getStatus()
                == ContentStatus.READY) {

            content.setStatus(
                    ContentStatus.DRAFT
            );

            contentRepository.save(
                    content
            );
        }
    }

    private void ensureEditable(
            Content content) {

        if (content.getStatus()
                == ContentStatus.ARCHIVED) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CONTENT_ARCHIVED",
                    "Un contenu archivé ne peut pas être modifié."
            );
        }
    }

    private String normalizeAltText(
            String altText) {

        if (altText == null) {

            return null;
        }

        String normalized =
                altText.trim();

        if (normalized.isBlank()) {

            return null;
        }

        if (normalized.length() > 255) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "MEDIA_ALT_TEXT_TOO_LONG",
                    "Le texte alternatif ne peut pas dépasser 255 caractères."
            );
        }

        return normalized;
    }

    private ContentMedia findMedia(
            Long mediaId,
            Long contentId) {

        return contentMediaRepository
                .findByIdAndContentId(
                        mediaId,
                        contentId
                )
                .orElseThrow(
                        () -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "MEDIA_NOT_FOUND",
                                "Média introuvable."
                        )
                );
    }

    private Content findOwnedContent(
            Long contentId,
            User user) {

        return contentRepository
                .findByIdAndUserId(
                        contentId,
                        user.getId()
                )
                .orElseThrow(
                        () -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "CONTENT_NOT_FOUND",
                                "Contenu introuvable."
                        )
                );
    }

    private User findUser(
            String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "USER_NOT_FOUND",
                                "Utilisateur introuvable."
                        )
                );
    }

    public record MediaDownload(
            Resource resource,
            String originalFilename,
            String contentType,
            long sizeBytes) {
    }
}