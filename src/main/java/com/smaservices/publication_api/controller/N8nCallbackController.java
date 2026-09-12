package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.MessageResponse;
import com.smaservices.publication_api.dto.n8n.N8nCallbackRequest;
import com.smaservices.publication_api.dto.publication.PublicationResponse;
import com.smaservices.publication_api.entity.Publication;
import com.smaservices.publication_api.service.N8nCallbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Hidden
@RestController
@RequestMapping("/api/n8n")
public class N8nCallbackController {

    private final N8nCallbackService callbackService;
    private final String sharedSecret;

    public N8nCallbackController(
            N8nCallbackService callbackService,

            @Value("${app.n8n.shared-secret}")
            String sharedSecret) {

        this.callbackService =
                callbackService;

        this.sharedSecret =
                sharedSecret;
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestHeader(
                    value = "X-N8N-SECRET",
                    required = false
            )
            String providedSecret,

            @Valid
            @RequestBody
            N8nCallbackRequest request) {

        if (!validSecret(providedSecret)) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            new MessageResponse(
                                    "Secret n8n invalide."
                            )
                    );
        }

        Publication publication =
                callbackService.handle(
                        request
                );

        return ResponseEntity.ok(
                new PublicationResponse(
                        publication
                )
        );
    }

    private boolean validSecret(
            String providedSecret) {

        if (providedSecret == null) {
            return false;
        }

        return MessageDigest.isEqual(
                sharedSecret.getBytes(
                        StandardCharsets.UTF_8
                ),
                providedSecret.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }
}