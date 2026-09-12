package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.MessageResponse;
import com.smaservices.publication_api.dto.content.ContentCreateRequest;
import com.smaservices.publication_api.dto.content.ContentResponse;
import com.smaservices.publication_api.dto.content.ContentUpdateRequest;
import com.smaservices.publication_api.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/contents")
@Tag(name = "Contents")
@SecurityRequirement(name = "bearerAuth")
public class ContentController {

    private final ContentService contentService;

    public ContentController(
            ContentService contentService) {

        this.contentService =
                contentService;
    }

    @PostMapping
    public ResponseEntity<ContentResponse> create(
            @Valid
            @RequestBody
            ContentCreateRequest request,
            Authentication authentication) {

        ContentResponse response =
                contentService.create(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ContentResponse>> findAll(
            Authentication authentication) {

        return ResponseEntity.ok(
                contentService.findAll(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentResponse> findOne(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                contentService.findOne(
                        authentication.getName(),
                        id
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContentResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody
            ContentUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                contentService.update(
                        authentication.getName(),
                        id,
                        request
                )
        );
    }

    @PostMapping("/{id}/ready")
    public ResponseEntity<ContentResponse> markReady(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                contentService.markReady(
                        authentication.getName(),
                        id
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Long id,
            Authentication authentication) {

        contentService.delete(
                authentication.getName(),
                id
        );

        return ResponseEntity.ok(
                new MessageResponse(
                        "Contenu supprimé ou archivé."
                )
        );
    }
}