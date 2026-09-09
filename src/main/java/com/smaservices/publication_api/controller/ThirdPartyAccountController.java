package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.WordPressAccountRequest;
import com.smaservices.publication_api.service.ThirdPartyAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class ThirdPartyAccountController {

    private final ThirdPartyAccountService accountService;

    public ThirdPartyAccountController(ThirdPartyAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/wordpress")
    public ResponseEntity<String> linkWordPressAccount(
            @RequestBody WordPressAccountRequest request,
            Authentication authentication) {

        // L'email est extrait du SecurityContext (alimenté par le JwtAuthenticationFilter)
        String userEmail = authentication.getName();

        accountService.linkWordPressAccount(userEmail, request.getWpUsername(), request.getWpAppPassword());

        return ResponseEntity.ok("Compte WordPress lié avec succès.");
    }
}