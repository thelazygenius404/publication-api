package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.publication.PublicationCreateRequest;
import com.smaservices.publication_api.dto.publication.PublicationResponse;
import com.smaservices.publication_api.service.PublicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publications")
public class PublicationManagementController {

    private final PublicationService publicationService;

    public PublicationManagementController(
            PublicationService publicationService) {

        this.publicationService =
                publicationService;
    }

    @PostMapping
    public ResponseEntity<List<PublicationResponse>> create(
            @Valid
            @RequestBody
            PublicationCreateRequest request,
            Authentication authentication) {

        List<PublicationResponse> response =
                publicationService.create(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<PublicationResponse>> findAll(
            Authentication authentication) {

        return ResponseEntity.ok(
                publicationService.findAll(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicationResponse> findOne(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                publicationService.findOne(
                        authentication.getName(),
                        id
                )
        );
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PublicationResponse> cancel(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                publicationService.cancel(
                        authentication.getName(),
                        id
                )
        );
    }
}