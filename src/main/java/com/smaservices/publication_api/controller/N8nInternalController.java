package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.MessageResponse;
import com.smaservices.publication_api.dto.n8n.N8nExecutionContext;
import com.smaservices.publication_api.service.N8nExecutionContextService;
import com.smaservices.publication_api.service.N8nMediaService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    private final N8nMediaService
            n8nMediaService;

    private final String
            sharedSecret;

    public N8nInternalController(
            N8nExecutionContextService executionContextService,
            N8nMediaService n8nMediaService,

            @Value("${app.n8n.shared-secret}")
            String sharedSecret) {

        this.executionContextService =
                executionContextService;

        this.n8nMediaService =
                n8nMediaService;

        this.sharedSecret =
                sharedSecret;
    }

    @GetMapping(
            "/publications/{id}/context"
    )
    public ResponseEntity<?>
    getExecutionContext(
            @PathVariable
            Long id,

            @RequestHeader(
                    value = "X-N8N-SECRET",
                    required = false
            )
            String providedSecret) {

        if (
                !validSecret(
                        providedSecret
                )
        ) {
            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            new MessageResponse(
                                    "Secret n8n invalide."
                            )
                    );
        }

        N8nExecutionContext context =
                executionContextService
                        .getContext(id);

        return ResponseEntity.ok(
                context
        );
    }

    @GetMapping(
            "/media/{mediaId}"
    )
    public ResponseEntity<?>
    getMedia(
            @PathVariable
            Long mediaId,

            @RequestHeader(
                    value = "X-N8N-SECRET",
                    required = false
            )
            String providedSecret) {

        if (
                !validSecret(
                        providedSecret
                )
        ) {
            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            new MessageResponse(
                                    "Secret n8n invalide."
                            )
                    );
        }

        N8nMediaService.MediaDownload
                download =
                n8nMediaService
                        .download(
                                mediaId
                        );

        MediaType mediaType =
                MediaType.parseMediaType(
                        download
                                .contentType()
                );

        String disposition =
                ContentDisposition
                        .inline()
                        .filename(
                                download
                                        .originalFilename(),
                                StandardCharsets.UTF_8
                        )
                        .build()
                        .toString();

        return ResponseEntity
                .ok()
                .cacheControl(
                        CacheControl.noStore()
                )
                .contentType(
                        mediaType
                )
                .contentLength(
                        download.sizeBytes()
                )
                .header(
                        HttpHeaders
                                .CONTENT_DISPOSITION,
                        disposition
                )
                .body(
                        download.resource()
                );
    }

    private boolean validSecret(
            String providedSecret) {

        if (
                providedSecret == null ||
                        providedSecret.isBlank()
        ) {
            return false;
        }

        return MessageDigest
                .isEqual(
                        sharedSecret
                                .getBytes(
                                        StandardCharsets.UTF_8
                                ),

                        providedSecret
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );
    }
}