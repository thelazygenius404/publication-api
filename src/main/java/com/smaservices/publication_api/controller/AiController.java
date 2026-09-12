package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.ai.AiDraftResponse;
import com.smaservices.publication_api.dto.ai.AiGenerateRequest;
import com.smaservices.publication_api.dto.ai.AiImproveRequest;
import com.smaservices.publication_api.service.AiContentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/ai")
@Tag(
        name = "AI",
        description = "Génération et amélioration de contenu avec Gemini"
)
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiContentService aiContentService;

    public AiController(
            AiContentService aiContentService) {

        this.aiContentService =
                aiContentService;
    }

    @PostMapping("/generate")
    public ResponseEntity<AiDraftResponse> generate(
            @Valid
            @RequestBody
            AiGenerateRequest request) {

        return ResponseEntity.ok(
                aiContentService.generate(
                        request
                )
        );
    }
    @PostMapping("/improve")
    public ResponseEntity<AiDraftResponse> improve(
            @Valid
            @RequestBody
            AiImproveRequest request) {

        return ResponseEntity.ok(
                aiContentService.improve(
                        request
                )
        );
    }
}