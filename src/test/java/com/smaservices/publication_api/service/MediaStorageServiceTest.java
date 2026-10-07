package com.smaservices.publication_api.service;

import com.smaservices.publication_api.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MediaStorageServiceTest {

    @TempDir
    Path tempDirectory;

    private MediaStorageService mediaStorageService;

    @BeforeEach
    void setUp() {

        mediaStorageService =
                new MediaStorageService(
                        tempDirectory.toString()
                );

        mediaStorageService.initialize();
    }

    @Test
    void store_shouldAcceptJpeg() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        "image/jpeg",
                        jpegBytes()
                );

        MediaStorageService.StoredMediaFile stored =
                mediaStorageService.store(file);

        assertEquals(
                "photo.jpg",
                stored.originalFilename()
        );

        assertEquals(
                "image/jpeg",
                stored.contentType()
        );

        assertTrue(
                stored.storageFilename()
                        .endsWith(".jpg")
        );

        assertEquals(
                file.getSize(),
                stored.sizeBytes()
        );

        assertTrue(
                Files.exists(
                        tempDirectory.resolve(
                                stored.storageFilename()
                        )
                )
        );
    }

    @Test
    void store_shouldAcceptPng() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "image.png",
                        "image/png",
                        pngBytes()
                );

        MediaStorageService.StoredMediaFile stored =
                mediaStorageService.store(file);

        assertEquals(
                "image/png",
                stored.contentType()
        );

        assertTrue(
                stored.storageFilename()
                        .endsWith(".png")
        );

        assertTrue(
                Files.exists(
                        tempDirectory.resolve(
                                stored.storageFilename()
                        )
                )
        );
    }

    @Test
    void store_shouldAcceptWebP() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "image.webp",
                        "image/webp",
                        webpBytes()
                );

        MediaStorageService.StoredMediaFile stored =
                mediaStorageService.store(file);

        assertEquals(
                "image/webp",
                stored.contentType()
        );

        assertTrue(
                stored.storageFilename()
                        .endsWith(".webp")
        );

        assertTrue(
                Files.exists(
                        tempDirectory.resolve(
                                stored.storageFilename()
                        )
                )
        );
    }

    @Test
    void store_shouldRejectUnsupportedFileType() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "document.pdf",
                        "application/pdf",
                        new byte[]{
                                0x25,
                                0x50,
                                0x44,
                                0x46
                        }
                );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                mediaStorageService
                                        .store(file)
                );

        assertEquals(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                exception.getStatus()
        );

        assertEquals(
                "MEDIA_TYPE_NOT_SUPPORTED",
                exception.getCode()
        );
    }

    @Test
    void store_shouldRejectFileDisguisedAsJpeg() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "fake.jpg",
                        "image/jpeg",
                        "not-an-image".getBytes()
                );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                mediaStorageService
                                        .store(file)
                );

        assertEquals(
                "MEDIA_TYPE_NOT_SUPPORTED",
                exception.getCode()
        );
    }

    @Test
    void store_shouldRejectEmptyFile() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "empty.jpg",
                        "image/jpeg",
                        new byte[0]
                );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                mediaStorageService
                                        .store(file)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatus()
        );

        assertEquals(
                "MEDIA_EMPTY",
                exception.getCode()
        );
    }

    @Test
    void store_shouldRejectFileLargerThanTenMegabytes() {

        byte[] largeFile =
                new byte[
                        10 * 1024 * 1024 + 1
                        ];

        largeFile[0] =
                (byte) 0xFF;

        largeFile[1] =
                (byte) 0xD8;

        largeFile[2] =
                (byte) 0xFF;

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "huge.jpg",
                        "image/jpeg",
                        largeFile
                );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                mediaStorageService
                                        .store(file)
                );

        assertEquals(
                HttpStatus.PAYLOAD_TOO_LARGE,
                exception.getStatus()
        );

        assertEquals(
                "MEDIA_TOO_LARGE",
                exception.getCode()
        );
    }

    @Test
    void loadAsResource_shouldReturnStoredFile() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        "image/jpeg",
                        jpegBytes()
                );

        MediaStorageService.StoredMediaFile stored =
                mediaStorageService.store(file);

        Resource resource =
                mediaStorageService
                        .loadAsResource(
                                stored.storageFilename()
                        );

        assertTrue(
                resource.exists()
        );

        assertTrue(
                resource.isReadable()
        );
    }

    @Test
    void loadAsResource_shouldRejectMissingFile() {

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                mediaStorageService
                                        .loadAsResource(
                                                "missing.jpg"
                                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatus()
        );

        assertEquals(
                "MEDIA_FILE_NOT_FOUND",
                exception.getCode()
        );
    }

    @Test
    void delete_shouldRemoveStoredFile()
            throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "photo.jpg",
                        "image/jpeg",
                        jpegBytes()
                );

        MediaStorageService.StoredMediaFile stored =
                mediaStorageService.store(file);

        Path storedPath =
                tempDirectory.resolve(
                        stored.storageFilename()
                );

        assertTrue(
                Files.exists(
                        storedPath
                )
        );

        mediaStorageService.delete(
                stored.storageFilename()
        );

        assertFalse(
                Files.exists(
                        storedPath
                )
        );
    }

    @Test
    void store_shouldRemoveDirectoryPartsFromOriginalFilename() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "../../secret/photo.jpg",
                        "image/jpeg",
                        jpegBytes()
                );

        MediaStorageService.StoredMediaFile stored =
                mediaStorageService.store(file);

        assertEquals(
                "photo.jpg",
                stored.originalFilename()
        );
    }

    private byte[] jpegBytes() {

        return new byte[]{
                (byte) 0xFF,
                (byte) 0xD8,
                (byte) 0xFF,
                (byte) 0xE0,
                0x00,
                0x10,
                0x4A,
                0x46,
                0x49,
                0x46
        };
    }

    private byte[] pngBytes() {

        return new byte[]{
                (byte) 0x89,
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A,
                0x00,
                0x00
        };
    }

    private byte[] webpBytes() {

        return new byte[]{
                'R',
                'I',
                'F',
                'F',
                0x00,
                0x00,
                0x00,
                0x00,
                'W',
                'E',
                'B',
                'P',
                'V',
                'P',
                '8',
                ' '
        };
    }
}