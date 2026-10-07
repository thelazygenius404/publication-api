package com.smaservices.publication_api.service;

import com.smaservices.publication_api.entity.ContentMedia;
import com.smaservices.publication_api.exception.ApiException;
import com.smaservices.publication_api.repository.ContentMediaRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class N8nMediaService {

    private final ContentMediaRepository
            contentMediaRepository;

    private final MediaStorageService
            mediaStorageService;

    public N8nMediaService(
            ContentMediaRepository contentMediaRepository,
            MediaStorageService mediaStorageService) {

        this.contentMediaRepository =
                contentMediaRepository;

        this.mediaStorageService =
                mediaStorageService;
    }

    @Transactional(readOnly = true)
    public MediaDownload download(
            Long mediaId) {

        ContentMedia media =
                contentMediaRepository
                        .findById(mediaId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "MEDIA_NOT_FOUND",
                                                "Média introuvable."
                                        )
                        );

        Resource resource =
                mediaStorageService
                        .loadAsResource(
                                media.getStorageFilename()
                        );

        return new MediaDownload(
                resource,
                media.getOriginalFilename(),
                media.getContentType(),
                media.getSizeBytes()
        );
    }

    public record MediaDownload(
            Resource resource,
            String originalFilename,
            String contentType,
            long sizeBytes) {
    }
}