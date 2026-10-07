package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.MessageResponse;
import com.smaservices.publication_api.dto.media.ContentMediaAltTextRequest;
import com.smaservices.publication_api.dto.media.ContentMediaResponse;
import com.smaservices.publication_api.service.ContentMediaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(
        "/api/contents/{contentId}/media"
)
@Tag(name = "Content Media")
@SecurityRequirement(name = "bearerAuth")
public class ContentMediaController {

    private final ContentMediaService
            contentMediaService;

    public ContentMediaController(
            ContentMediaService contentMediaService) {

        this.contentMediaService =
                contentMediaService;
    }

    @PostMapping(
            consumes =
                    MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ContentMediaResponse> upload(
            @PathVariable
            Long contentId,

            @RequestPart("file")
            MultipartFile file,

            @RequestParam(
                    value = "altText",
                    required = false
            )
            String altText,

            Authentication authentication) {

        ContentMediaResponse response =
                contentMediaService.upload(
                        authentication.getName(),
                        contentId,
                        file,
                        altText
                );

        return ResponseEntity
                .status(201)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ContentMediaResponse>>
    findAll(
            @PathVariable
            Long contentId,

            Authentication authentication) {

        return ResponseEntity.ok(
                contentMediaService
                        .findAll(
                                authentication.getName(),
                                contentId
                        )
        );
    }

    @PatchMapping("/{mediaId}")
    public ResponseEntity<ContentMediaResponse>
    updateAltText(
            @PathVariable
            Long contentId,

            @PathVariable
            Long mediaId,

            @Valid
            @RequestBody
            ContentMediaAltTextRequest request,

            Authentication authentication) {

        return ResponseEntity.ok(
                contentMediaService
                        .updateAltText(
                                authentication.getName(),
                                contentId,
                                mediaId,
                                request.getAltText()
                        )
        );
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable
            Long contentId,

            @PathVariable
            Long mediaId,

            Authentication authentication) {

        contentMediaService.delete(
                authentication.getName(),
                contentId,
                mediaId
        );

        return ResponseEntity.ok(
                new MessageResponse(
                        "Média supprimé."
                )
        );
    }

    @GetMapping("/{mediaId}/file")
    public ResponseEntity<Resource> download(
            @PathVariable
            Long contentId,

            @PathVariable
            Long mediaId,

            Authentication authentication) {

        ContentMediaService.MediaDownload
                download =
                contentMediaService.download(
                        authentication.getName(),
                        contentId,
                        mediaId
                );

        MediaType mediaType =
                MediaType.parseMediaType(
                        download.contentType()
                );

        String disposition =
                ContentDisposition
                        .inline()
                        .filename(
                                download.originalFilename(),
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
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition
                )
                .body(
                        download.resource()
                );
    }
}