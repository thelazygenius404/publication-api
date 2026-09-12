package com.smaservices.publication_api.controller;

import com.smaservices.publication_api.dto.WordPressAccountRequest;
import com.smaservices.publication_api.service.LinkedInOAuthService;
import com.smaservices.publication_api.service.ThirdPartyAccountService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.smaservices.publication_api.dto.linkedin.LinkedInAuthorizationUrlResponse;
import com.smaservices.publication_api.dto.linkedin.LinkedInConnectionResponse;
import com.smaservices.publication_api.dto.MessageResponse;
import com.smaservices.publication_api.dto.account.ThirdPartyAccountsResponse;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Third-party accounts")
@SecurityRequirement(name = "bearerAuth")
public class ThirdPartyAccountController {

    private final ThirdPartyAccountService accountService;
    private final LinkedInOAuthService linkedInOAuthService;

    public ThirdPartyAccountController(
            ThirdPartyAccountService accountService,
            LinkedInOAuthService linkedInOAuthService) {

        this.accountService =
                accountService;

        this.linkedInOAuthService =
                linkedInOAuthService;
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
    @GetMapping("/linkedin/authorization-url")
    public ResponseEntity<LinkedInAuthorizationUrlResponse>
    getLinkedInAuthorizationUrl(
            Authentication authentication) {

        String authorizationUrl =
                linkedInOAuthService
                        .buildAuthorizationUrl(
                                authentication.getName()
                        );

        return ResponseEntity.ok(
                new LinkedInAuthorizationUrlResponse(
                        authorizationUrl
                )
        );
    }

    @GetMapping("/linkedin/callback")
    public ResponseEntity<LinkedInConnectionResponse>
    linkedInCallback(
            @RequestParam(required = false)
            String code,
            @RequestParam(required = false)
            String state,
            @RequestParam(required = false)
            String error) {

        return ResponseEntity.ok(
                linkedInOAuthService.handleCallback(
                        code,
                        state,
                        error
                )
        );
    }

    @GetMapping
    public ResponseEntity<ThirdPartyAccountsResponse>
    getAccounts(
            Authentication authentication) {

        return ResponseEntity.ok(
                accountService.getAccountsStatus(
                        authentication.getName()
                )
        );
    }

    @DeleteMapping("/wordpress")
    public ResponseEntity<MessageResponse>
    disconnectWordPress(
            Authentication authentication) {

        accountService.disconnectWordPressAccount(
                authentication.getName()
        );

        return ResponseEntity.ok(
                new MessageResponse(
                        "Compte WordPress déconnecté."
                )
        );
    }

    @DeleteMapping("/linkedin")
    public ResponseEntity<MessageResponse>
    disconnectLinkedIn(
            Authentication authentication) {

        accountService.disconnectLinkedInAccount(
                authentication.getName()
        );

        return ResponseEntity.ok(
                new MessageResponse(
                        "Compte LinkedIn déconnecté."
                )
        );
    }
}