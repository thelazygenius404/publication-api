package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.WordPressAccountRequest;
import com.smaservices.publication_api.service.ThirdPartyAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class ThirdPartyAccountController {

    private final ThirdPartyAccountService accountService;

    public ThirdPartyAccountController(ThirdPartyAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/wordpress")
    public ResponseEntity<String> linkWordPressAccount(
            @Valid
            @RequestBody
            WordPressAccountRequest request,
            Authentication authentication) {

        String userEmail =
                authentication.getName();

        accountService.linkWordPressAccount(
                userEmail,
                request.getSiteUrl(),
                request.getWpUsername(),
                request.getWpAppPassword()
        );

        return ResponseEntity.ok(
                "Compte WordPress lié avec succès."
        );
    }
}