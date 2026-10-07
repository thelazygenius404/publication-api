package com.smaservices.publication_api.service;

import com.smaservices.publication_api.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class MediaStorageService {

    private static final long MAX_FILE_SIZE =
            10L * 1024L * 1024L;

    private final Path storageRoot;

    public MediaStorageService(
            @Value("${app.media.storage-path}")
            String storagePath) {

        this.storageRoot =
                Paths.get(storagePath)
                        .toAbsolutePath()
                        .normalize();
    }

    @PostConstruct
    public void initialize() {

        try {

            Files.createDirectories(
                    storageRoot
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Impossible d'initialiser le dossier de stockage des médias.",
                    exception
            );
        }
    }

    public StoredMediaFile store(
            MultipartFile file) {

        validateBasicFile(
                file
        );

        String originalFilename =
                sanitizeOriginalFilename(
                        file.getOriginalFilename()
                );

        String storageFilename =
                null;

        try (
                BufferedInputStream input =
                        new BufferedInputStream(
                                file.getInputStream()
                        )
        ) {

            input.mark(32);

            byte[] header =
                    input.readNBytes(16);

            input.reset();

            DetectedImageType detectedType =
                    detectImageType(
                            header
                    );

            storageFilename =
                    UUID.randomUUID()
                            + "."
                            + detectedType.extension();

            Path destination =
                    safeResolve(
                            storageFilename
                    );

            Files.copy(
                    input,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return new StoredMediaFile(
                    originalFilename,
                    storageFilename,
                    detectedType.contentType(),
                    file.getSize()
            );

        } catch (ApiException exception) {

            throw exception;

        } catch (IOException exception) {

            if (storageFilename != null) {
                deleteQuietly(
                        storageFilename
                );
            }

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MEDIA_STORAGE_ERROR",
                    "Impossible d'enregistrer le média."
            );
        }
    }

    public Resource loadAsResource(
            String storageFilename) {

        Path file =
                safeResolve(
                        storageFilename
                );

        if (!Files.exists(file)
                || !Files.isRegularFile(file)) {

            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "MEDIA_FILE_NOT_FOUND",
                    "Le fichier média est introuvable."
            );
        }

        try {

            Resource resource =
                    new UrlResource(
                            file.toUri()
                    );

            if (!resource.exists()
                    || !resource.isReadable()) {

                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "MEDIA_FILE_NOT_READABLE",
                        "Le fichier média est inaccessible."
                );
            }

            return resource;

        } catch (MalformedURLException exception) {

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MEDIA_FILE_URL_ERROR",
                    "Impossible d'accéder au fichier média."
            );
        }
    }

    public void delete(
            String storageFilename) {

        Path file =
                safeResolve(
                        storageFilename
                );

        try {

            Files.deleteIfExists(
                    file
            );

        } catch (IOException exception) {

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MEDIA_DELETE_ERROR",
                    "Impossible de supprimer le fichier média."
            );
        }
    }

    public void deleteQuietly(
            String storageFilename) {

        try {

            Path file =
                    safeResolve(
                            storageFilename
                    );

            Files.deleteIfExists(
                    file
            );

        } catch (Exception ignored) {

            // Cleanup best effort only.
        }
    }

    private void validateBasicFile(
            MultipartFile file) {

        if (file == null
                || file.isEmpty()
                || file.getSize() <= 0) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "MEDIA_EMPTY",
                    "Le fichier média est vide."
            );
        }

        if (file.getSize()
                > MAX_FILE_SIZE) {

            throw new ApiException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "MEDIA_TOO_LARGE",
                    "Le fichier média ne peut pas dépasser 10 Mo."
            );
        }
    }

    private DetectedImageType detectImageType(
            byte[] header) {

        if (isJpeg(header)) {

            return new DetectedImageType(
                    "image/jpeg",
                    "jpg"
            );
        }

        if (isPng(header)) {

            return new DetectedImageType(
                    "image/png",
                    "png"
            );
        }

        if (isWebP(header)) {

            return new DetectedImageType(
                    "image/webp",
                    "webp"
            );
        }

        throw new ApiException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "MEDIA_TYPE_NOT_SUPPORTED",
                "Seules les images JPEG, PNG et WebP sont autorisées."
        );
    }

    private boolean isJpeg(
            byte[] header) {

        return header.length >= 3
                && unsigned(header[0]) == 0xFF
                && unsigned(header[1]) == 0xD8
                && unsigned(header[2]) == 0xFF;
    }

    private boolean isPng(
            byte[] header) {

        int[] signature = {
                0x89,
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A
        };

        if (header.length
                < signature.length) {

            return false;
        }

        for (int index = 0;
             index < signature.length;
             index++) {

            if (unsigned(header[index])
                    != signature[index]) {

                return false;
            }
        }

        return true;
    }

    private boolean isWebP(
            byte[] header) {

        return header.length >= 12

                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'

                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }

    private int unsigned(
            byte value) {

        return value & 0xFF;
    }

    private String sanitizeOriginalFilename(
            String filename) {

        if (filename == null
                || filename.isBlank()) {

            return "image";
        }

        String normalized =
                filename
                        .replace('\\', '/');

        int separator =
                normalized.lastIndexOf('/');

        if (separator >= 0) {

            normalized =
                    normalized.substring(
                            separator + 1
                    );
        }

        normalized =
                normalized
                        .replace("\r", "")
                        .replace("\n", "")
                        .trim();

        if (normalized.isBlank()) {

            normalized =
                    "image";
        }

        if (normalized.length()
                > 255) {

            normalized =
                    normalized.substring(
                            0,
                            255
                    );
        }

        return normalized;
    }

    private Path safeResolve(
            String storageFilename) {

        Path destination =
                storageRoot
                        .resolve(
                                storageFilename
                        )
                        .normalize();

        if (!destination.startsWith(
                storageRoot
        )) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "MEDIA_INVALID_PATH",
                    "Chemin du média invalide."
            );
        }

        return destination;
    }

    public record StoredMediaFile(
            String originalFilename,
            String storageFilename,
            String contentType,
            long sizeBytes) {
    }

    private record DetectedImageType(
            String contentType,
            String extension) {
    }
}