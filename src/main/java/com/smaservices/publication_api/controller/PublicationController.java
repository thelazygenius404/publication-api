package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.PublicationRequest;
import com.smaservices.publication_api.service.WordPressPublishService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/publish")
public class PublicationController {

    private final WordPressPublishService publishService;

    public PublicationController(WordPressPublishService publishService) {
        this.publishService = publishService;
    }

    @PostMapping("/wordpress")
    public ResponseEntity<?> publishToWordPress(@RequestBody PublicationRequest request, Authentication authentication) {
        try {
            String response = publishService.publishPost(authentication.getName(), request.getTitle(), request.getContent());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de la publication : " + e.getMessage());
        }
    }
}