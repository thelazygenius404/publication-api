package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.MessageResponse;
import com.smaservices.publication_api.dto.n8n.N8nExecutionContext;
import com.smaservices.publication_api.service.N8nExecutionContextService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Hidden
@RestController
@RequestMapping("/api/n8n/internal")
public class N8nInternalController {

    private final N8nExecutionContextService
            executionContextService;

    private final String sharedSecret;

    public N8nInternalController(
            N8nExecutionContextService executionContextService,

            @Value("${app.n8n.shared-secret}")
            String sharedSecret) {

        this.executionContextService =
                executionContextService;

        this.sharedSecret =
                sharedSecret;
    }

    @GetMapping("/publications/{id}/context")
    public ResponseEntity<?> getExecutionContext(
            @PathVariable Long id,

            @RequestHeader(
                    value = "X-N8N-SECRET",
                    required = false
            )
            String providedSecret) {

        if (!validSecret(providedSecret)) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            new MessageResponse(
                                    "Secret n8n invalide."
                            )
                    );
        }

        return ResponseEntity.ok(
                executionContextService
                        .getContext(id)
        );
    }

    private boolean validSecret(
            String providedSecret) {

        if (providedSecret == null ||
                providedSecret.isBlank()) {

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