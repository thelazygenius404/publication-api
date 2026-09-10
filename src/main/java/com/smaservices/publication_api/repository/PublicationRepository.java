package com.smaservices.publication_api.repository;

import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.entity.enums.PublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicationRepository
        extends JpaRepository<Publication, Long> {

    List<Publication>
    findByContentUserIdOrderByCreatedAtDesc(
            Long userId
    );

    Optional<Publication>
    findByIdAndContentUserId(
            Long id,
            Long userId
    );

    long countByContentUserIdAndStatus(
            Long userId,
            PublicationStatus status
    );
}