package com.smaservices.publication_api.service;

import com.smaservices.publication_api.entity.ThirdPartyAccount;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import com.smaservices.publication_api.repository.ThirdPartyAccountRepository;
import com.smaservices.publication_api.repository.UserRepository;
import com.smaservices.publication_api.security.EncryptionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class WordPressPublishService {

    private final ThirdPartyAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final RestClient restClient;

    public WordPressPublishService(ThirdPartyAccountRepository accountRepository,
                                   UserRepository userRepository,
                                   EncryptionService encryptionService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.restClient = RestClient.create();
    }

    public String publishPost(String userEmail, String title, String content) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        ThirdPartyAccount account = accountRepository.findByUserIdAndType(user.getId(), ThirdPartyType.WORDPRESS)
                .orElseThrow(() -> new RuntimeException("Aucun compte WordPress lié à cet utilisateur"));

        // 1. Déchiffrement AES
        String decryptedCredentials = encryptionService.decrypt(account.getAccessTokenEnc());

        // 2. Encodage Base64 pour l'en-tête Basic Auth
        String base64Credentials = Base64.getEncoder().encodeToString(decryptedCredentials.getBytes(StandardCharsets.UTF_8));

        // 3. Appel à l'API REST de WordPress (Docker tourne sur le port 8081)
        String wpApiUrl = "http://localhost:8081/wp-json/wp/v2/posts";

        Map<String, String> body = Map.of(
                "title", title,
                "content", content,
                "status", "publish" // Publie directement (utilisez "draft" pour un brouillon)
        );

        return restClient.post()
                .uri(wpApiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Credentials)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }
}