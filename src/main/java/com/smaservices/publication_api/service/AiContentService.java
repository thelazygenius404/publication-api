package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.ai.AiDraftResponse;
import com.smaservices.publication_api.dto.ai.AiGenerateRequest;
import com.smaservices.publication_api.exception.ApiException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.smaservices.publication_api.dto.ai.AiImproveRequest;

@Service
public class AiContentService {

    private final ChatClient chatClient;

    public AiContentService(
            ChatClient.Builder chatClientBuilder) {

        this.chatClient =
                chatClientBuilder.build();
    }

    public AiDraftResponse generate(
            AiGenerateRequest request) {

        String tone =
                normalize(
                        request.getTone(),
                        "professionnel"
                );

        String language =
                normalize(
                        request.getLanguage(),
                        "français"
                );

        try {

            AiDraftResponse response =
                    chatClient
                            .prompt()
                            .system("""
                                    Tu es un assistant éditorial spécialisé
                                    dans la rédaction de publications
                                    professionnelles pour les réseaux sociaux
                                    et les sites web.

                                    Génère uniquement du contenu publiable.

                                    Contraintes :
                                    - le titre doit être clair et concis ;
                                    - le titre doit contenir au maximum 255 caractères ;
                                    - le corps doit être naturel et professionnel ;
                                    - n'invente pas de faits précis non fournis ;
                                    - ne fournis aucune explication autour du contenu ;
                                    - le contenu généré sera obligatoirement validé
                                      par un humain avant publication.
                                    """)
                            .user(user ->
                                    user
                                            .text("""
                                                    Génère une publication.

                                                    Sujet :
                                                    {topic}

                                                    Ton :
                                                    {tone}

                                                    Langue :
                                                    {language}
                                                    """)
                                            .param(
                                                    "topic",
                                                    request.getTopic()
                                            )
                                            .param(
                                                    "tone",
                                                    tone
                                            )
                                            .param(
                                                    "language",
                                                    language
                                            )
                            )
                            .call()
                            .entity(
                                    AiDraftResponse.class,
                                    spec ->
                                            spec
                                                    .useProviderStructuredOutput()
                                                    .validateSchema()
                            );

            if (response == null
                    || response.title() == null
                    || response.title().isBlank()
                    || response.body() == null
                    || response.body().isBlank()) {

                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "AI_INVALID_RESPONSE",
                        "Le fournisseur d'IA a retourné une réponse invalide."
                );
            }

            return response;

        } catch (ApiException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_PROVIDER_UNAVAILABLE",
                    "Le service d'IA est temporairement indisponible."
            );
        }
    }

    private String normalize(
            String value,
            String defaultValue) {

        if (value == null
                || value.isBlank()) {

            return defaultValue;
        }

        return value.trim();
    }

    public AiDraftResponse improve(
            AiImproveRequest request) {

        String tone =
                normalize(
                        request.getTone(),
                        "professionnel"
                );

        String language =
                normalize(
                        request.getLanguage(),
                        "français"
                );

        String instruction =
                normalize(
                        request.getInstruction(),
                        "Améliore la clarté, la fluidité et l'impact du texte."
                );

        try {

            AiDraftResponse response =
                    chatClient
                            .prompt()
                            .system("""
                                Tu es un assistant éditorial spécialisé
                                dans l'amélioration de publications
                                professionnelles.

                                Ta mission est de réécrire un contenu
                                existant sans changer son intention.

                                Contraintes :
                                - conserve le sens du contenu original ;
                                - améliore la clarté et la qualité rédactionnelle ;
                                - n'invente aucun fait ;
                                - le titre doit contenir au maximum 255 caractères ;
                                - respecte la langue demandée ;
                                - respecte le ton demandé ;
                                - retourne uniquement le contenu amélioré ;
                                - ce contenu sera validé par un humain
                                  avant toute publication.
                                """)
                            .user(user ->
                                    user
                                            .text("""
                                                Contenu à améliorer.

                                                Titre actuel :
                                                {title}

                                                Corps actuel :
                                                {body}

                                                Instruction :
                                                {instruction}

                                                Ton :
                                                {tone}

                                                Langue :
                                                {language}
                                                """)
                                            .param(
                                                    "title",
                                                    request.getTitle()
                                            )
                                            .param(
                                                    "body",
                                                    request.getBody()
                                            )
                                            .param(
                                                    "instruction",
                                                    instruction
                                            )
                                            .param(
                                                    "tone",
                                                    tone
                                            )
                                            .param(
                                                    "language",
                                                    language
                                            )
                            )
                            .call()
                            .entity(
                                    AiDraftResponse.class,
                                    spec ->
                                            spec
                                                    .useProviderStructuredOutput()
                                                    .validateSchema()
                            );

            if (response == null
                    || response.title() == null
                    || response.title().isBlank()
                    || response.body() == null
                    || response.body().isBlank()) {

                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "AI_INVALID_RESPONSE",
                        "Le fournisseur d'IA a retourné une réponse invalide."
                );
            }

            return response;

        } catch (ApiException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_PROVIDER_UNAVAILABLE",
                    "Le service d'IA est temporairement indisponible."
            );
        }
    }
}