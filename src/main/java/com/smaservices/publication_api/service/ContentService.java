package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.content.ContentCreateRequest;
import com.smaservices.publication_api.dto.content.ContentResponse;
import com.smaservices.publication_api.dto.content.ContentUpdateRequest;
import com.smaservices.publication_api.entity.Content;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.ContentStatus;
import com.smaservices.publication_api.repository.ContentRepository;
import com.smaservices.publication_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ContentService(
            ContentRepository contentRepository,
            UserRepository userRepository,
            AuditService auditService) {

        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ContentResponse create(
            String userEmail,
            ContentCreateRequest request) {

        User user = findUser(userEmail);

        Content content = new Content();

        content.setTitle(request.getTitle().trim());
        content.setBody(request.getBody().trim());
        content.setStatus(ContentStatus.DRAFT);
        content.setUser(user);

        Content saved = contentRepository.save(content);

        auditService.log(
                user,
                "CONTENT_CREATED",
                "Content",
                saved.getId(),
                "Brouillon créé"
        );

        return new ContentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContentResponse> findAll(String userEmail) {

        User user = findUser(userEmail);

        return contentRepository
                .findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(ContentResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContentResponse findOne(
            String userEmail,
            Long contentId) {

        User user = findUser(userEmail);

        return new ContentResponse(
                findOwnedContent(contentId, user)
        );
    }

    @Transactional
    public ContentResponse update(
            String userEmail,
            Long contentId,
            ContentUpdateRequest request) {

        User user = findUser(userEmail);

        Content content =
                findOwnedContent(contentId, user);

        if (content.getStatus() == ContentStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Un contenu archivé ne peut pas être modifié."
            );
        }

        content.setTitle(
                request.getTitle().trim()
        );

        content.setBody(
                request.getBody().trim()
        );

        // Toute modification invalide la validation précédente.
        content.setStatus(
                ContentStatus.DRAFT
        );

        Content saved =
                contentRepository.save(content);

        auditService.log(
                user,
                "CONTENT_UPDATED",
                "Content",
                saved.getId(),
                "Contenu modifié"
        );

        return new ContentResponse(saved);
    }

    @Transactional
    public ContentResponse markReady(
            String userEmail,
            Long contentId) {

        User user = findUser(userEmail);

        Content content =
                findOwnedContent(contentId, user);

        if (content.getStatus() == ContentStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Un contenu archivé ne peut pas être validé."
            );
        }

        if (content.getStatus() == ContentStatus.READY) {
            return new ContentResponse(content);
        }

        content.setStatus(
                ContentStatus.READY
        );

        Content saved =
                contentRepository.save(content);

        auditService.log(
                user,
                "CONTENT_READY",
                "Content",
                saved.getId(),
                "Contenu validé par l'utilisateur"
        );

        return new ContentResponse(saved);
    }

    @Transactional
    public void delete(
            String userEmail,
            Long contentId) {

        User user = findUser(userEmail);

        Content content =
                findOwnedContent(contentId, user);

        boolean hasPublications =
                !content.getPublications().isEmpty();

        if (hasPublications) {
            content.setStatus(
                    ContentStatus.ARCHIVED
            );

            contentRepository.save(content);

            auditService.log(
                    user,
                    "CONTENT_ARCHIVED",
                    "Content",
                    content.getId(),
                    "Archivage car le contenu possède déjà des publications"
            );

            return;
        }

        contentRepository.delete(content);

        auditService.log(
                user,
                "CONTENT_DELETED",
                "Content",
                contentId,
                "Brouillon supprimé"
        );
    }

    private User findUser(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Utilisateur non trouvé."
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
                        () -> new IllegalArgumentException(
                                "Contenu introuvable."
                        )
                );
    }
}