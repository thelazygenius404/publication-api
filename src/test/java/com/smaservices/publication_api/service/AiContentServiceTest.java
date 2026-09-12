package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.ai.AiDraftResponse;
import com.smaservices.publication_api.dto.ai.AiGenerateRequest;
import com.smaservices.publication_api.dto.ai.AiImproveRequest;
import com.smaservices.publication_api.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiContentServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private AiContentService aiContentService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {

        when(
                chatClientBuilder.build()
        ).thenReturn(
                chatClient
        );

        aiContentService =
                new AiContentService(
                        chatClientBuilder
                );

        when(
                chatClient.prompt()
        ).thenReturn(
                requestSpec
        );

        when(
                requestSpec.system(
                        anyString()
                )
        ).thenReturn(
                requestSpec
        );

        when(
                requestSpec.user(
                        any(Consumer.class)
                )
        ).thenReturn(
                requestSpec
        );

        when(
                requestSpec.call()
        ).thenReturn(
                callResponseSpec
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void generate_shouldReturnGeneratedDraft() {

        AiDraftResponse expected =
                new AiDraftResponse(
                        "Titre généré",
                        "Contenu généré"
                );

        when(
                callResponseSpec.entity(
                        eq(AiDraftResponse.class),
                        any(Consumer.class)
                )
        ).thenReturn(
                expected
        );

        AiGenerateRequest request =
                new AiGenerateRequest();

        request.setTopic(
                "Automatisation des publications"
        );

        request.setTone(
                "professionnel"
        );

        request.setLanguage(
                "français"
        );

        AiDraftResponse response =
                aiContentService.generate(
                        request
                );

        assertNotNull(response);

        assertEquals(
                "Titre généré",
                response.title()
        );

        assertEquals(
                "Contenu généré",
                response.body()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void improve_shouldReturnImprovedDraft() {

        AiDraftResponse expected =
                new AiDraftResponse(
                        "Titre amélioré",
                        "Contenu amélioré"
                );

        when(
                callResponseSpec.entity(
                        eq(AiDraftResponse.class),
                        any(Consumer.class)
                )
        ).thenReturn(
                expected
        );

        AiImproveRequest request =
                new AiImproveRequest();

        request.setTitle(
                "Titre initial"
        );

        request.setBody(
                "Contenu initial"
        );

        request.setInstruction(
                "Rends le contenu plus professionnel"
        );

        request.setTone(
                "professionnel"
        );

        request.setLanguage(
                "français"
        );

        AiDraftResponse response =
                aiContentService.improve(
                        request
                );

        assertNotNull(response);

        assertEquals(
                "Titre amélioré",
                response.title()
        );

        assertEquals(
                "Contenu amélioré",
                response.body()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void generate_shouldRejectInvalidAiResponse() {

        when(
                callResponseSpec.entity(
                        eq(AiDraftResponse.class),
                        any(Consumer.class)
                )
        ).thenReturn(
                new AiDraftResponse(
                        "",
                        ""
                )
        );

        AiGenerateRequest request =
                new AiGenerateRequest();

        request.setTopic(
                "Automatisation"
        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                aiContentService.generate(
                                        request
                                )
                );

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                exception.getStatus()
        );

        assertEquals(
                "AI_INVALID_RESPONSE",
                exception.getCode()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void generate_shouldHandleProviderFailure() {

        when(
                callResponseSpec.entity(
                        eq(AiDraftResponse.class),
                        any(Consumer.class)
                )
        ).thenThrow(
                new RuntimeException(
                        "Provider unavailable"
                )
        );

        AiGenerateRequest request =
                new AiGenerateRequest();

        request.setTopic(
                "Automatisation"
        );

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                aiContentService.generate(
                                        request
                                )
                );

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                exception.getStatus()
        );

        assertEquals(
                "AI_PROVIDER_UNAVAILABLE",
                exception.getCode()
        );
    }
}