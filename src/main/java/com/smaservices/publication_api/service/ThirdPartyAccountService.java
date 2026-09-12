package com.smaservices.publication_api.service;

import com.smaservices.publication_api.entity.ThirdPartyAccount;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.AccountStatus;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import com.smaservices.publication_api.repository.ThirdPartyAccountRepository;
import com.smaservices.publication_api.repository.UserRepository;
import com.smaservices.publication_api.security.EncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smaservices.publication_api.dto.account.AccountConnectionState;
import com.smaservices.publication_api.dto.account.AccountProviderStatusResponse;
import com.smaservices.publication_api.dto.account.ThirdPartyAccountsResponse;

import java.time.Instant;
import java.util.Optional;

@Service
public class ThirdPartyAccountService {

    private final ThirdPartyAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public ThirdPartyAccountService(
            ThirdPartyAccountRepository accountRepository,
            UserRepository userRepository,
            EncryptionService encryptionService) {

        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public void linkWordPressAccount(
            String userEmail,
            String siteUrl,
            String wpUsername,
            String wpAppPassword) {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Utilisateur non trouvé."
                        )
                );

        String normalizedSiteUrl =
                normalizeSiteUrl(siteUrl);

        String normalizedUsername =
                wpUsername.trim();

        String normalizedAppPassword =
                wpAppPassword
                        .replace(" ", "")
                        .trim();

        if (normalizedUsername.isBlank()) {
            throw new IllegalArgumentException(
                    "Le nom d'utilisateur WordPress est obligatoire."
            );
        }

        if (normalizedAppPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Le mot de passe d'application WordPress est obligatoire."
            );
        }

        /*
         * Format utilisé plus tard pour Basic Authentication:
         *
         * username:applicationPassword
         */
        String credentials =
                normalizedUsername
                        + ":"
                        + normalizedAppPassword;

        String encryptedCredentials =
                encryptionService.encrypt(
                        credentials
                );

        ThirdPartyAccount account =
                accountRepository
                        .findByUserIdAndType(
                                user.getId(),
                                ThirdPartyType.WORDPRESS
                        )
                        .orElseGet(
                                ThirdPartyAccount::new
                        );

        account.setUser(user);

        account.setType(
                ThirdPartyType.WORDPRESS
        );

        account.setSiteUrl(
                normalizedSiteUrl
        );

        account.setAccessTokenEnc(
                encryptedCredentials
        );

        account.setRefreshTokenEnc(null);

        account.setStatus(
                AccountStatus.CONNECTED
        );

        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public ThirdPartyAccount getWordPressAccount(
            String userEmail) {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Utilisateur non trouvé."
                        )
                );

        return accountRepository
                .findByUserIdAndType(
                        user.getId(),
                        ThirdPartyType.WORDPRESS
                )
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Aucun compte WordPress connecté."
                        )
                );
    }

    @Transactional
    public void disconnectWordPressAccount(
            String userEmail) {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Utilisateur non trouvé."
                        )
                );

        accountRepository
                .findByUserIdAndType(
                        user.getId(),
                        ThirdPartyType.WORDPRESS
                )
                .ifPresent(
                        accountRepository::delete
                );
    }

    private String normalizeSiteUrl(
            String siteUrl) {

        if (siteUrl == null ||
                siteUrl.isBlank()) {

            throw new IllegalArgumentException(
                    "L'URL WordPress est obligatoire."
            );
        }

        String normalized =
                siteUrl.trim();

        if (!normalized.startsWith("http://") &&
                !normalized.startsWith("https://")) {

            throw new IllegalArgumentException(
                    "L'URL WordPress doit commencer par http:// ou https://"
            );
        }

        /*
         * http://localhost:8081/
         *
         * becomes:
         *
         * http://localhost:8081
         */
        return normalized.replaceAll(
                "/+$",
                ""
        );
    }

    @Transactional
    public ThirdPartyAccountsResponse getAccountsStatus(
            String userEmail) {

        User user =
                userRepository
                        .findByEmail(userEmail)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Utilisateur non trouvé."
                                )
                        );

        Optional<ThirdPartyAccount> wordpress =
                accountRepository
                        .findByUserIdAndType(
                                user.getId(),
                                ThirdPartyType.WORDPRESS
                        );

        Optional<ThirdPartyAccount> linkedin =
                accountRepository
                        .findByUserIdAndType(
                                user.getId(),
                                ThirdPartyType.LINKEDIN
                        );

        linkedin.ifPresent(
                this::refreshLinkedInExpiry
        );

        return new ThirdPartyAccountsResponse(
                toStatus(
                        wordpress,
                        ThirdPartyType.WORDPRESS
                ),
                toStatus(
                        linkedin,
                        ThirdPartyType.LINKEDIN
                )
        );
    }

    @Transactional
    public void disconnectLinkedInAccount(
            String userEmail) {

        User user =
                userRepository
                        .findByEmail(userEmail)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Utilisateur non trouvé."
                                )
                        );

        accountRepository
                .findByUserIdAndType(
                        user.getId(),
                        ThirdPartyType.LINKEDIN
                )
                .ifPresent(
                        accountRepository::delete
                );
    }

    private void refreshLinkedInExpiry(
            ThirdPartyAccount account) {

        if (account.getStatus()
                != AccountStatus.CONNECTED) {

            return;
        }

        Instant expiresAt =
                account.getAccessTokenExpiresAt();

        if (expiresAt != null
                && !expiresAt.isAfter(
                Instant.now()
        )) {

            account.setStatus(
                    AccountStatus.EXPIRED
            );

            accountRepository.save(
                    account
            );
        }
    }

    private AccountProviderStatusResponse toStatus(
            Optional<ThirdPartyAccount> optionalAccount,
            ThirdPartyType type) {

        if (optionalAccount.isEmpty()) {

            return new AccountProviderStatusResponse(
                    false,
                    AccountConnectionState.NOT_CONNECTED,
                    null,
                    null
            );
        }

        ThirdPartyAccount account =
                optionalAccount.get();

        boolean expired =
                account.getStatus()
                        == AccountStatus.EXPIRED;

        AccountConnectionState status =
                expired
                        ? AccountConnectionState.EXPIRED
                        : AccountConnectionState.CONNECTED;

        return new AccountProviderStatusResponse(
                !expired,
                status,

                type == ThirdPartyType.WORDPRESS
                        ? account.getSiteUrl()
                        : null,

                type == ThirdPartyType.LINKEDIN
                        ? account.getAccessTokenExpiresAt()
                        : null
        );
    }
}