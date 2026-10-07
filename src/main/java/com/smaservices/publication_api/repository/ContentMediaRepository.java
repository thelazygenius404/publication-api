package com.smaservices.publication_api.repository;

import com.smaservices.publication_api.entity.ContentMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentMediaRepository
        extends JpaRepository<ContentMedia, Long> {

    List<ContentMedia>
    findByContentIdOrderByCreatedAtAsc(
            Long contentId
    );

    Optional<ContentMedia>
    findByIdAndContentId(
            Long id,
            Long contentId
    );
}