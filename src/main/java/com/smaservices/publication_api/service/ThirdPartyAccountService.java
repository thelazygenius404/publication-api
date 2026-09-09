package com.smaservices.publication_api.service;

import com.smaservices.publication_api.entity.ThirdPartyAccount;
import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.entity.enums.AccountStatus;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import com.smaservices.publication_api.repository.ThirdPartyAccountRepository;
import com.smaservices.publication_api.repository.UserRepository;
import com.smaservices.publication_api.security.EncryptionService;
import org.springframework.stereotype.Service;

@Service
public class ThirdPartyAccountService {

    private final ThirdPartyAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public ThirdPartyAccountService(ThirdPartyAccountRepository accountRepository,
                                    UserRepository userRepository,
                                    EncryptionService encryptionService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    public void linkWordPressAccount(String userEmail, String wpUsername, String wpAppPassword) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Format requis par l'API WordPress pour le Basic Auth
        String credentials = wpUsername + ":" + wpAppPassword.replace(" ", "");
        String encryptedCredentials = encryptionService.encrypt(credentials);

        ThirdPartyAccount account = accountRepository.findByUserIdAndType(user.getId(), ThirdPartyType.WORDPRESS)
                .orElse(new ThirdPartyAccount());

        account.setUser(user);
        account.setType(ThirdPartyType.WORDPRESS);
        account.setAccessTokenEnc(encryptedCredentials);
        account.setStatus(AccountStatus.CONNECTED);

        accountRepository.save(account);
    }
}