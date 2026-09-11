package com.smaservices.publication_api.service;

import com.smaservices.publication_api.dto.linkedin.LinkedInConnectionResponse;
import com.smaservices.publication_api.dto.linkedin.LinkedInTokenResponse;
import com.smaservices.publication_api.dto.linkedin.LinkedInUserInfoResponse;
import com.smaservices.publication_api.entity.ThirdPartyAccount;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.AccountStatus;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import com.smaservices.publication_api.exception.ApiException;
import com.smaservices.publication_api.repository.ThirdPartyAccountRepository;
import com.smaservices.publication_api.repository.UserRepository;
import com.smaservices.publication_api.security.EncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Service
public class LinkedInOAuthService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    LinkedInOAuthService.class
            );

    private static final String AUTHORIZATION_URL =
            "https://www.linkedin.com/oauth/v2/authorization";

    private static final String ACCESS_TOKEN_URL =
            "https://www.linkedin.com/oauth/v2/accessToken";

    private static final String USER_INFO_URL =
            "https://api.linkedin.com/v2/userinfo";

    private final UserRepository userRepository;
    private final ThirdPartyAccountRepository accountRepository;
    private final EncryptionService encryptionService;
    private final LinkedInOAuthStateService stateService;

    private final RestClient restClient;

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public LinkedInOAuthService(
            UserRepository userRepository,
            ThirdPartyAccountRepository accountRepository,
            EncryptionService encryptionService,
            LinkedInOAuthStateService stateService,
            @Value("${app.linkedin.client-id}")
            String clientId,
            @Value("${app.linkedin.client-secret}")
            String clientSecret,
            @Value("${app.linkedin.redirect-uri}")
            String redirectUri) {

        this.userRepository =
                userRepository;

        this.accountRepository =
                accountRepository;

        this.encryptionService =
                encryptionService;

        this.stateService =
                stateService;

        this.clientId =
                clientId;

        this.clientSecret =
                clientSecret;

        this.redirectUri =
                redirectUri;

        this.restClient =
                RestClient.builder()
                        .build();
    }

    public String buildAuthorizationUrl(
            String userEmail) {

        User user =
                userRepository
                        .findByEmail(userEmail)
                        .orElseThrow(
                                () -> new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "Utilisateur non trouvé."
                                )
                        );

        String state =
                stateService.createState(
                        user.getId()
                );

        return UriComponentsBuilder
                .fromUriString(
                        AUTHORIZATION_URL
                )
                .queryParam(
                        "response_type",
                        "code"
                )
                .queryParam(
                        "client_id",
                        clientId
                )
                .queryParam(
                        "redirect_uri",
                        redirectUri
                )
                .queryParam(
                        "state",
                        state
                )
                .queryParam(
                        "scope",
                        "openid profile w_member_social"
                )
                .build()
                .encode()
                .toUriString();
    }

    public LinkedInConnectionResponse handleCallback(
            String code,
            String state,
            String error) {

        Long userId =
                stateService.validateAndGetUserId(
                        state
                );

        if (error != null
                && !error.isBlank()) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "LINKEDIN_AUTHORIZATION_DENIED",
                    "L'autorisation LinkedIn a été refusée ou annulée."
            );
        }

        if (code == null
                || code.isBlank()) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "LINKEDIN_AUTHORIZATION_CODE_MISSING",
                    "Le code d'autorisation LinkedIn est absent."
            );
        }

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "Utilisateur non trouvé."
                                )
                        );

        LinkedInTokenResponse token =
                exchangeCode(
                        code
                );

        LOGGER.info(
                "LinkedIn OAuth granted scopes: {}",
                token.scope()
        );

        LinkedInUserInfoResponse userInfo =
                fetchUserInfo(
                        token.accessToken()
                );

        Instant expiresAt =
                token.expiresIn() == null
                        ? null
                        : Instant.now()
                        .plusSeconds(
                                token.expiresIn()
                        );

        ThirdPartyAccount account =
                accountRepository
                        .findByUserIdAndType(
                                userId,
                                ThirdPartyType.LINKEDIN
                        )
                        .orElseGet(
                                ThirdPartyAccount::new
                        );

        account.setUser(user);

        account.setType(
                ThirdPartyType.LINKEDIN
        );

        account.setSiteUrl(null);

        account.setExternalAccountId(
                userInfo.sub()
        );

        account.setAccessTokenEnc(
                encryptionService.encrypt(
                        token.accessToken()
                )
        );

        if (token.refreshToken() == null
                || token.refreshToken().isBlank()) {

            account.setRefreshTokenEnc(null);

        } else {

            account.setRefreshTokenEnc(
                    encryptionService.encrypt(
                            token.refreshToken()
                    )
            );
        }

        account.setAccessTokenExpiresAt(
                expiresAt
        );

        account.setStatus(
                AccountStatus.CONNECTED
        );

        accountRepository.save(
                account
        );

        return new LinkedInConnectionResponse(
                true,
                "LINKEDIN",
                userInfo.sub(),
                expiresAt
        );
    }

    private LinkedInTokenResponse exchangeCode(
            String code) {

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add(
                "grant_type",
                "authorization_code"
        );

        form.add(
                "code",
                code
        );

        form.add(
                "client_id",
                clientId
        );

        form.add(
                "client_secret",
                clientSecret
        );

        form.add(
                "redirect_uri",
                redirectUri
        );

        try {

            LinkedInTokenResponse response =
                    restClient
                            .post()
                            .uri(
                                    ACCESS_TOKEN_URL
                            )
                            .contentType(
                                    MediaType.APPLICATION_FORM_URLENCODED
                            )
                            .accept(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(form)
                            .retrieve()
                            .body(
                                    LinkedInTokenResponse.class
                            );

            if (response == null
                    || response.accessToken() == null
                    || response.accessToken().isBlank()) {

                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "LINKEDIN_TOKEN_RESPONSE_INVALID",
                        "Réponse OAuth LinkedIn invalide."
                );
            }

            return response;

        } catch (ApiException exception) {
            throw exception;

        } catch (RestClientException exception) {

            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "LINKEDIN_TOKEN_EXCHANGE_FAILED",
                    "Impossible d'obtenir le jeton d'accès LinkedIn."
            );
        }
    }

    private LinkedInUserInfoResponse fetchUserInfo(
            String accessToken) {

        try {

            LinkedInUserInfoResponse response =
                    restClient
                            .get()
                            .uri(
                                    USER_INFO_URL
                            )
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + accessToken
                            )
                            .accept(
                                    MediaType.APPLICATION_JSON
                            )
                            .retrieve()
                            .body(
                                    LinkedInUserInfoResponse.class
                            );

            if (response == null
                    || response.sub() == null
                    || response.sub().isBlank()) {

                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "LINKEDIN_USER_INFO_INVALID",
                        "Impossible d'identifier le compte LinkedIn."
                );
            }

            return response;

        } catch (ApiException exception) {

            throw exception;

        } catch (RestClientResponseException exception) {

            LOGGER.warn(
                    "LinkedIn userinfo request failed with HTTP {}",
                    exception.getStatusCode().value()
            );

            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "LINKEDIN_USER_INFO_FAILED",
                    "Impossible de récupérer le profil LinkedIn."
            );

        } catch (RestClientException exception) {

            LOGGER.warn(
                    "LinkedIn userinfo response could not be processed: {}",
                    exception.getClass().getSimpleName()
            );

            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "LINKEDIN_USER_INFO_FAILED",
                    "Impossible de récupérer le profil LinkedIn."
            );
        }
    }
}