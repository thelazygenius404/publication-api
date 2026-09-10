package com.smaservices.publication_api.repository;

import com.smaservices.publication_api.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentRepository
        extends JpaRepository<Content, Long> {

    List<Content>
    findByUserIdOrderByUpdatedAtDesc(
            Long userId
    );

    Optional<Content>
    findByIdAndUserId(
            Long id,
            Long userId
    );
}